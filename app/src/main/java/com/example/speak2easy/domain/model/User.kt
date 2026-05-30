package com.example.speak2easy.domain.model

import kotlinx.serialization.Serializable

/**
 * The authenticated user. Mirrors iOS `SonariUser`. Used as the API wire model,
 * the cached model, and the domain model (the JSON config tolerates snake_case,
 * unknown keys, and missing defaults). [authProvider] is local-only — set by the
 * repository after sign-in, never sent by the backend.
 */
@Serializable
data class User(
    val userId: String,
    val email: String? = null,
    val displayName: String? = null,
    val profilePictureUrl: String? = null,
    val emailVerified: Boolean = false,
    val onboardingCompleted: Boolean = false,
    val subscriptionTier: String? = null,
    val createdAt: String? = null,
    val preferredPracticeTime: String? = null,
    val dailyReminderEnabled: Boolean? = null,
    val dailyGoalMinutes: Int? = null,
    val authProvider: String? = null,
)
