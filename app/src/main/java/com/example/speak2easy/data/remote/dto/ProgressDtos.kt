package com.example.speak2easy.data.remote.dto

import com.example.speak2easy.data.remote.FlexibleDoubleSerializer
import com.example.speak2easy.data.remote.FlexibleIntSerializer
import kotlinx.serialization.Serializable

@Serializable
data class UserProgressResponse(
    val stats: UserProgressStats = UserProgressStats(),
    val streak: StreakInfo? = null,
)

@Serializable
data class UserProgressStats(
    @Serializable(with = FlexibleIntSerializer::class) val totalItemsPracticed: Int? = null,
    @Serializable(with = FlexibleIntSerializer::class) val totalAttempts: Int? = null,
    @Serializable(with = FlexibleIntSerializer::class) val totalCorrect: Int? = null,
    @Serializable(with = FlexibleDoubleSerializer::class) val avgAccuracy: Double? = null,
    @Serializable(with = FlexibleIntSerializer::class) val masteredCount: Int? = null,
    @Serializable(with = FlexibleIntSerializer::class) val practicingCount: Int? = null,
    @Serializable(with = FlexibleIntSerializer::class) val learningCount: Int? = null,
)

@Serializable
data class StreakInfo(
    @Serializable(with = FlexibleIntSerializer::class) val streakDays: Int? = null,
    val lastPracticeDate: String? = null,
)

@Serializable
data class PracticeSessionSummary(
    val sessionId: String = "",
    val sessionType: String? = null,
    @Serializable(with = FlexibleIntSerializer::class) val totalItems: Int? = null,
    @Serializable(with = FlexibleIntSerializer::class) val correctCount: Int? = null,
    @Serializable(with = FlexibleDoubleSerializer::class) val accuracyRate: Double? = null,
    val startedAt: String? = null,
    val endedAt: String? = null,
    @Serializable(with = FlexibleIntSerializer::class) val durationSeconds: Int? = null,
    @Serializable(with = FlexibleIntSerializer::class) val lessonNumber: Int? = null,
    val lessonCharsetDisplayName: String? = null,
    val groupLabel: String? = null,
    @Serializable(with = FlexibleIntSerializer::class) val uniqueCorrect: Int? = null,
    @Serializable(with = FlexibleIntSerializer::class) val uniqueTotal: Int? = null,
)
