package com.sonari.speak2easy.data.remote.dto

import com.sonari.speak2easy.domain.model.User
import kotlinx.serialization.Serializable

// --- Requests (camelCase fields serialize to snake_case; nulls are omitted) ---

@Serializable
data class SignInRequest(
    val provider: String,
    val email: String? = null,
    val displayName: String? = null,
    val password: String? = null,
    val isSignup: Boolean? = null,
    // Verified server-side against the provider's JWKS — required for google/apple.
    val idToken: String? = null,
    val identityToken: String? = null,
)

@Serializable
data class RefreshTokenRequest(val refreshToken: String)

@Serializable
data class ForgotPasswordRequest(val email: String)

@Serializable
data class ResetPasswordRequest(val token: String, val newPassword: String)

@Serializable
data class ResendVerificationRequest(val email: String)

// --- Responses ---

@Serializable
data class AuthResponse(
    val token: String,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val expiresIn: Int? = null,
    val user: User,
    val isNewUser: Boolean? = null,
)

@Serializable
data class MessageResponse(val status: String? = null, val message: String = "")

@Serializable
data class TokenVerifyResponse(
    val valid: Boolean = false,
    val userId: String? = null,
    val email: String? = null,
)

@Serializable
data class TokenRefreshResponse(
    val token: String,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val expiresIn: Int? = null,
    val userId: String? = null,
)

@Serializable
data class OnboardingResponse(
    val status: String? = null,
    val message: String = "",
    val user: User,
)

@Serializable
data class OnboardingRequest(
    val displayName: String,
    val birthYear: Int,
    val birthMonth: Int,
    val gender: String,
    val countryCode: String,
    val timezone: String,
    val nativeLanguage: String,
    val targetLanguages: List<String>,
    val japaneseLevel: String,
    val learningGoal: String,
    val dailyGoalMinutes: Int,
    val preferredPracticeTime: String,
    val dailyReminderEnabled: Boolean,
    val dailyReminderTime: String,
    val city: String? = null,
    val state: String? = null,
    val learningGoalDetails: String? = null,
    val referredByCode: String? = null,
)
