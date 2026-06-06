package com.sonari.speak2easy.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class SubscriptionStatusResponse(
    val isActive: Boolean = false,
    val isTrialPeriod: Boolean = false,
    val productId: String? = null,
    val expiresDate: String? = null,
    val subscriptionTier: String = "free",
)

@Serializable
data class GoogleSubscriptionVerifyRequest(
    val productId: String,
    val purchaseToken: String,
    val packageName: String,
    val orderId: String? = null,
)
