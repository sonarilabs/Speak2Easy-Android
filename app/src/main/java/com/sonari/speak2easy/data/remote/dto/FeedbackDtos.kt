package com.sonari.speak2easy.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Mirrors the backend `feedbackSchema` at `/api/v1/users/{userId}/feedback`. SonariJson applies
 * snake_case naming, so `imagesBase64` ⇄ `images_base64` etc. happens automatically.
 *
 * - [category] capped at 50 chars server-side
 * - [message] required, 1..5000 chars (trimmed). Client enforces 1000 (matches iOS).
 * - [imagesBase64] up to 5 base64-encoded JPEGs, each up to ~2,000,000 chars
 *   (≈1.5 MB binary). We cap at 3 client-side per iOS parity.
 * - [deviceInfo] / [appVersion] / [currentScreen] capped at 500 / 20 / 100 chars
 */
@Serializable
data class FeedbackRequest(
    val category: String,
    val message: String,
    val imagesBase64: List<String> = emptyList(),
    val deviceInfo: String = "",
    val appVersion: String = "",
    val currentScreen: String = "",
)

/** Backend returns `{ status: "ok", message: "Feedback sent" }`. */
@Serializable
data class FeedbackResponse(
    val status: String = "",
    val message: String = "",
)
