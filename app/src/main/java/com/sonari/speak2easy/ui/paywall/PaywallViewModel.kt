package com.sonari.speak2easy.ui.paywall

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.sonari.speak2easy.data.auth.AuthRepository
import com.sonari.speak2easy.data.remote.ApiException
import com.sonari.speak2easy.data.subscription.SubscriptionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.security.MessageDigest

data class PaywallUiState(
    val productTitle: String = "Speak2Easy Premium Monthly",
    val priceLabel: String = "$4.99/month",
    val trialLabel: String = "3 Days Free Trial",
    val isLoadingStatus: Boolean = true,
    val isBillingReady: Boolean = false,
    val isPurchasing: Boolean = false,
    val isRestoring: Boolean = false,
    val purchaseAvailable: Boolean = false,
    val promoCode: String = "",
    val isApplyingPromo: Boolean = false,
    val promoApplied: Boolean = false,
    val infoMessage: String? = null,
    val errorMessage: String? = null,
) {
    val primaryButtonEnabled: Boolean
        get() = isBillingReady && purchaseAvailable && !isPurchasing && !isRestoring
}

class PaywallViewModel(
    context: Context,
    private val authRepository: AuthRepository,
    private val subscriptionRepository: SubscriptionRepository,
) : ViewModel() {
    private val appContext = context.applicationContext
    private val processedTokens = mutableSetOf<String>()
    private var productDetails: ProductDetails? = null
    private var offerToken: String? = null
    private var selectedOfferId: String? = null
    private var selectedOfferHasOneMonthTrial = false
    private var connecting = false

    // Offer id unlocked by a validated promo code (developer-determined offer in Play
    // Console). Null until the backend accepts a code; never selected for the public.
    private var promoOfferId: String? = null

    private val _ui = MutableStateFlow(PaywallUiState())
    val ui: StateFlow<PaywallUiState> = _ui.asStateFlow()

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        when (billingResult.responseCode) {
            BillingResponseCode.OK -> {
                if (purchases.isNullOrEmpty()) {
                    _ui.update { it.copy(isPurchasing = false) }
                } else {
                    processPurchases(purchases)
                }
            }
            BillingResponseCode.USER_CANCELED -> {
                _ui.update { it.copy(isPurchasing = false, infoMessage = "Purchase cancelled", errorMessage = null) }
            }
            else -> {
                _ui.update {
                    it.copy(
                        isPurchasing = false,
                        errorMessage = billingResult.debugMessage.ifBlank { "Purchase failed" },
                        infoMessage = null,
                    )
                }
            }
        }
    }

    // Reconnection is handled manually (see scheduleReconnect) instead of
    // enableAutoServiceReconnection: the library's internal reconnect races our own
    // startConnection calls and fails with DEVELOPER_ERROR ("already connecting"),
    // leaving the client permanently not-ready and the purchase button dead.
    private val billingClient = BillingClient.newBuilder(appContext)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build(),
        )
        .build()

    private var reconnectAttempts = 0

    init {
        refreshStatus(silent = true)
        connectBilling()
    }

    fun refreshStatus(silent: Boolean = false) {
        _ui.update { it.copy(isLoadingStatus = true, errorMessage = if (silent) null else it.errorMessage) }
        viewModelScope.launch {
            try {
                val status = subscriptionRepository.getStatus()
                _ui.update {
                    it.copy(
                        isLoadingStatus = false,
                        infoMessage = if (status.isActive) "Subscription active" else it.infoMessage,
                    )
                }
            } catch (e: Exception) {
                _ui.update {
                    it.copy(
                        isLoadingStatus = false,
                        // Leave any billing error in place on silent refreshes; overwriting
                        // it here hid the real failure reason from the paywall.
                        errorMessage = if (silent) it.errorMessage else (e.message ?: "Could not check subscription"),
                    )
                }
            }
        }
    }

    fun startPurchase(activity: Activity) {
        val details = productDetails
        val token = offerToken
        if (!billingClient.isReady || details == null || token == null) {
            _ui.update {
                it.copy(
                    errorMessage = "Subscription is not available yet. Try again in a moment.",
                    infoMessage = null,
                )
            }
            connectBilling()
            return
        }

        _ui.update { it.copy(isPurchasing = true, errorMessage = null, infoMessage = null) }
        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .setOfferToken(token)
            .build()

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .setObfuscatedAccountId(obfuscatedAccountId())
            .build()

        val result = billingClient.launchBillingFlow(activity, flowParams)
        if (result.responseCode != BillingResponseCode.OK) {
            _ui.update {
                it.copy(
                    isPurchasing = false,
                    errorMessage = result.debugMessage.ifBlank { "Could not start purchase" },
                )
            }
        }
    }

    fun restorePurchases() {
        _ui.update { it.copy(isRestoring = true, errorMessage = null, infoMessage = null) }
        refreshStatus(silent = true)
        if (!billingClient.isReady) {
            // Doubles as the user-visible retry path: a dead purchase button can't
            // reconnect itself, but tapping Restore can.
            connectBilling()
            _ui.update { it.copy(isRestoring = false) }
            return
        }
        queryProductDetails()
        queryExistingPurchases {
            _ui.update {
                it.copy(
                    isRestoring = false,
                    infoMessage = it.infoMessage ?: "Restore checked",
                )
            }
        }
    }

    private fun connectBilling() {
        if (billingClient.isReady || connecting) {
            if (billingClient.isReady) {
                queryProductDetails()
                queryExistingPurchases()
            }
            return
        }
        connecting = true
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                connecting = false
                if (billingResult.responseCode == BillingResponseCode.OK) {
                    reconnectAttempts = 0
                    _ui.update { it.copy(isBillingReady = true, errorMessage = null) }
                    queryProductDetails()
                    queryExistingPurchases()
                } else {
                    _ui.update { it.copy(isBillingReady = false, purchaseAvailable = false) }
                    scheduleReconnect(billingResult.debugMessage.ifBlank { "Billing is unavailable" })
                }
            }

            override fun onBillingServiceDisconnected() {
                connecting = false
                _ui.update { it.copy(isBillingReady = false) }
                scheduleReconnect("Billing is unavailable")
            }
        })
    }

    /**
     * Retries the billing connection with capped exponential backoff. The Play billing
     * service disconnecting mid-session (common under aggressive OEM process management)
     * previously left the paywall dead with no recovery path.
     */
    private fun scheduleReconnect(failureMessage: String) {
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            _ui.update { it.copy(errorMessage = failureMessage) }
            return
        }
        val delayMs = (1000L shl reconnectAttempts).coerceAtMost(15_000L)
        reconnectAttempts++
        viewModelScope.launch {
            kotlinx.coroutines.delay(delayMs)
            connectBilling()
        }
    }

    private fun queryProductDetails(onLoaded: (() -> Unit)? = null) {
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PREMIUM_MONTHLY_PRODUCT_ID)
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(product))
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, result ->
            if (billingResult.responseCode != BillingResponseCode.OK) {
                _ui.update {
                    it.copy(
                        purchaseAvailable = false,
                        errorMessage = billingResult.debugMessage.ifBlank { "Could not load subscription" },
                    )
                }
                onLoaded?.invoke()
                return@queryProductDetailsAsync
            }

            productDetails = result.productDetailsList.firstOrNull()
            logProductDetails(productDetails)
            applySelectedOffer()
            onLoaded?.invoke()
        }
    }

    /**
     * Picks which offer the billing flow will use and syncs it into the UI state.
     *
     * Selection order:
     *  1. The promo-unlocked (developer determined) offer, once a code has validated.
     *  2. Any public offer with a free phase — trial length is controlled entirely
     *     from Play Console, so changing it needs no app update.
     *  3. The base plan.
     * The promo offer is excluded from 2 and 3: Play returns developer-determined
     * offers to every device, and gating them is this app's responsibility.
     */
    private fun applySelectedOffer() {
        val details = productDetails
        val offers = details?.subscriptionOfferDetails.orEmpty()
        val oneMonthTrialOffers = offers.filter { it.hasOneMonthFreeTrial() }
        val offer = promoOfferId?.let { id ->
            offers.firstOrNull { it.offerId == id }
                ?: oneMonthTrialOffers.singleOrNull()
        }
            ?: offers.firstOrNull { o -> o.offerId != PROMO_OFFER_ID && o.pricingPhases.pricingPhaseList.any { it.priceAmountMicros == 0L } }
            ?: offers.firstOrNull { it.offerId != PROMO_OFFER_ID }

        offerToken = offer?.offerToken
        selectedOfferId = offer?.offerId
        selectedOfferHasOneMonthTrial = offer?.hasOneMonthFreeTrial() == true
        if (promoOfferId != null && offer?.offerId != promoOfferId) {
            Log.w(
                TAG,
                "Promo offer id not matched exactly. requestedOfferId=$promoOfferId " +
                    "selectedOfferId=${offer?.offerId} availableOfferIds=${offers.map { it.offerId }}",
            )
        }

        _ui.update {
            it.copy(
                productTitle = details?.title?.removeSuffix(" (Speak2Easy)") ?: it.productTitle,
                priceLabel = details?.monthlyPriceLabel() ?: it.priceLabel,
                trialLabel = offer?.trialLabel() ?: it.trialLabel,
                purchaseAvailable = details != null && offer != null,
                errorMessage = if (details == null || offer == null) {
                    "Monthly subscription is not configured in Google Play yet."
                } else {
                    null
                },
            )
        }
    }

    fun onPromoCodeChange(value: String) {
        _ui.update { it.copy(promoCode = value.uppercase().take(20)) }
    }

    fun applyPromoCode() {
        val code = _ui.value.promoCode.trim()
        if (code.isEmpty() || _ui.value.isApplyingPromo || _ui.value.promoApplied) return

        _ui.update { it.copy(isApplyingPromo = true, errorMessage = null, infoMessage = null) }
        viewModelScope.launch {
            try {
                val response = subscriptionRepository.validatePromoCode(code)
                promoOfferId = response.offerId
                if (billingClient.isReady) {
                    queryProductDetails {
                        finishPromoApply(response.offerId)
                    }
                } else {
                    connectBilling()
                    applySelectedOffer()
                    finishPromoApply(response.offerId)
                }
            } catch (e: ApiException) {
                _ui.update {
                    it.copy(isApplyingPromo = false, errorMessage = e.message ?: "Invalid promo code")
                }
            } catch (e: Exception) {
                _ui.update {
                    it.copy(isApplyingPromo = false, errorMessage = "Could not check promo code. Try again.")
                }
            }
        }
    }

    private fun finishPromoApply(offerId: String?) {
        val unlocked = offerId != null &&
            offerToken != null &&
            (selectedOfferId == offerId || selectedOfferHasOneMonthTrial)
        _ui.update {
            it.copy(
                isApplyingPromo = false,
                promoApplied = unlocked,
                infoMessage = if (unlocked) "Promo applied!" else it.infoMessage,
                errorMessage = if (unlocked) {
                    null
                } else {
                    "Promo code is valid, but the Google Play offer is not available on this device yet."
                },
            )
        }
    }

    private fun queryExistingPurchases(onDone: (() -> Unit)? = null) {
        if (!billingClient.isReady) {
            onDone?.invoke()
            return
        }
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingResponseCode.OK) {
                processPurchases(purchases, onDone)
            } else {
                onDone?.invoke()
            }
        }
    }

    private fun processPurchases(purchases: List<Purchase>, onDone: (() -> Unit)? = null) {
        val premiumPurchases = purchases.filter { purchase ->
            PREMIUM_MONTHLY_PRODUCT_ID in purchase.products
        }
        if (premiumPurchases.isEmpty()) {
            onDone?.invoke()
            return
        }

        premiumPurchases.forEachIndexed { index, purchase ->
            processPurchase(purchase, onDone?.takeIf { index == premiumPurchases.lastIndex })
        }
    }

    private fun processPurchase(purchase: Purchase, onDone: (() -> Unit)? = null) {
        if (purchase.purchaseToken in processedTokens) {
            onDone?.invoke()
            return
        }
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) {
            _ui.update {
                it.copy(
                    isPurchasing = false,
                    infoMessage = "Purchase is pending",
                    errorMessage = null,
                )
            }
            onDone?.invoke()
            return
        }

        processedTokens += purchase.purchaseToken
        _ui.update { it.copy(isPurchasing = true, errorMessage = null, infoMessage = null) }

        viewModelScope.launch {
            try {
                val status = subscriptionRepository.verifyGooglePurchase(
                    productId = PREMIUM_MONTHLY_PRODUCT_ID,
                    purchaseToken = purchase.purchaseToken,
                    packageName = appContext.packageName,
                    orderId = purchase.orderId,
                )
                if (status.isActive) {
                    acknowledgePurchase(purchase)
                    _ui.update {
                        it.copy(
                            isPurchasing = false,
                            infoMessage = "Premium unlocked",
                            errorMessage = null,
                        )
                    }
                } else {
                    _ui.update {
                        it.copy(
                            isPurchasing = false,
                            errorMessage = "Subscription is not active yet.",
                        )
                    }
                }
            } catch (e: ApiException) {
                processedTokens -= purchase.purchaseToken
                _ui.update {
                    it.copy(
                        isPurchasing = false,
                        errorMessage = e.message ?: "Could not verify purchase",
                        infoMessage = null,
                    )
                }
            } catch (e: Exception) {
                processedTokens -= purchase.purchaseToken
                _ui.update {
                    it.copy(
                        isPurchasing = false,
                        errorMessage = e.message ?: "Could not verify purchase",
                        infoMessage = null,
                    )
                }
            } finally {
                onDone?.invoke()
            }
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        if (purchase.isAcknowledged) return
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient.acknowledgePurchase(params) { result ->
            if (result.responseCode != BillingResponseCode.OK) {
                _ui.update {
                    it.copy(errorMessage = result.debugMessage.ifBlank { "Could not acknowledge purchase" })
                }
            }
        }
    }

    private fun obfuscatedAccountId(): String {
        val userId = authRepository.currentUser?.userId ?: appContext.packageName
        val digest = MessageDigest.getInstance("SHA-256").digest(userId.toByteArray())
        return digest.joinToString(separator = "") { "%02x".format(it) }.take(64)
    }

    override fun onCleared() {
        billingClient.endConnection()
        super.onCleared()
    }

    class Factory(
        private val context: Context,
        private val authRepository: AuthRepository,
        private val subscriptionRepository: SubscriptionRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PaywallViewModel(context, authRepository, subscriptionRepository) as T
    }

    companion object {
        private const val TAG = "PaywallViewModel"
        const val PREMIUM_MONTHLY_PRODUCT_ID = "speak2easy_premium_monthly"
        private const val MAX_RECONNECT_ATTEMPTS = 6

        // Developer-determined offer in Play Console; must match the backend's
        // PROMO_UNLOCKED_OFFER_ID so unvalidated users never see it.
        private const val PROMO_OFFER_ID = "free-trial-1month"
    }
}

private fun logProductDetails(details: ProductDetails?) {
    if (details == null) {
        Log.w("PaywallViewModel", "Billing product not returned for speak2easy_premium_monthly")
        return
    }
    val offers = details.subscriptionOfferDetails.orEmpty()
    Log.i(
        "PaywallViewModel",
        "Billing product loaded. productId=${details.productId} offerCount=${offers.size} " +
            "offers=${offers.map { offer -> "${offer.basePlanId}:${offer.offerId}:${offer.offerTags}" }}",
    )
}

private fun ProductDetails.monthlyPriceLabel(): String {
    val price = subscriptionOfferDetails
        ?.flatMap { it.pricingPhases.pricingPhaseList }
        ?.lastOrNull { it.priceAmountMicros > 0L }
        ?.formattedPrice
        ?: return "$4.99/month"
    return "$price/month"
}

private fun ProductDetails.SubscriptionOfferDetails.trialLabel(): String {
    val freeTrial = pricingPhases.pricingPhaseList.firstOrNull { it.priceAmountMicros == 0L }
    return when (freeTrial?.billingPeriod) {
        "P3D" -> "3 Days Free Trial"
        "P7D" -> "7 Days Free Trial"
        "P2W" -> "2 Weeks Free Trial"
        "P1M" -> "1 Month Free Trial"
        else -> "1 Month Free Trial"
    }
}

private fun ProductDetails.SubscriptionOfferDetails.hasOneMonthFreeTrial(): Boolean =
    pricingPhases.pricingPhaseList.any { it.priceAmountMicros == 0L && it.billingPeriod == "P1M" }
