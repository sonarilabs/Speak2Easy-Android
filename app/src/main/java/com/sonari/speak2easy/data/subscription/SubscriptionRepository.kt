package com.sonari.speak2easy.data.subscription

import com.sonari.speak2easy.data.auth.AuthRepository
import com.sonari.speak2easy.data.remote.SubscriptionApi
import com.sonari.speak2easy.data.remote.apiCall
import com.sonari.speak2easy.data.remote.dto.GoogleSubscriptionVerifyRequest
import com.sonari.speak2easy.data.remote.dto.SubscriptionStatusResponse
import kotlinx.serialization.json.Json

class SubscriptionRepository(
    private val api: SubscriptionApi,
    private val authRepository: AuthRepository,
    private val json: Json,
) {
    suspend fun getStatus(): SubscriptionStatusResponse {
        val status = apiCall(json) { api.getStatus() }
        authRepository.updateSubscriptionTier(status.subscriptionTier.takeIf { status.isActive })
        return status
    }

    suspend fun verifyGooglePurchase(
        productId: String,
        purchaseToken: String,
        packageName: String,
        orderId: String?,
    ): SubscriptionStatusResponse {
        val status = apiCall(json) {
            api.verifyGoogle(
                GoogleSubscriptionVerifyRequest(
                    productId = productId,
                    purchaseToken = purchaseToken,
                    packageName = packageName,
                    orderId = orderId,
                ),
            )
        }
        authRepository.updateSubscriptionTier(status.subscriptionTier.takeIf { status.isActive })
        return status
    }
}
