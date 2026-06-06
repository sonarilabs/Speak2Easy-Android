package com.sonari.speak2easy.ui.paywall

import android.app.Activity
import android.content.Context
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
    private var connecting = false

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

    private val billingClient = BillingClient.newBuilder(appContext)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build(),
        )
        .enableAutoServiceReconnection()
        .build()

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
                        errorMessage = null,
                    )
                }
            } catch (e: Exception) {
                _ui.update {
                    it.copy(
                        isLoadingStatus = false,
                        errorMessage = if (silent) null else (e.message ?: "Could not check subscription"),
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
                    _ui.update { it.copy(isBillingReady = true, errorMessage = null) }
                    queryProductDetails()
                    queryExistingPurchases()
                } else {
                    _ui.update {
                        it.copy(
                            isBillingReady = false,
                            purchaseAvailable = false,
                            errorMessage = billingResult.debugMessage.ifBlank { "Billing is unavailable" },
                        )
                    }
                }
            }

            override fun onBillingServiceDisconnected() {
                connecting = false
                _ui.update { it.copy(isBillingReady = false) }
            }
        })
    }

    private fun queryProductDetails() {
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
                return@queryProductDetailsAsync
            }

            val details = result.productDetailsList.firstOrNull()
            val offer = details?.subscriptionOfferDetails
                ?.firstOrNull { offer ->
                    offer.pricingPhases.pricingPhaseList.any { it.priceAmountMicros == 0L && it.billingPeriod == "P3D" }
                }
                ?: details?.subscriptionOfferDetails?.firstOrNull()

            productDetails = details
            offerToken = offer?.offerToken

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
        const val PREMIUM_MONTHLY_PRODUCT_ID = "speak2easy_premium_monthly"
    }
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
        "P1M" -> "1 Month Free Trial"
        else -> "3 Days Free Trial"
    }
}
