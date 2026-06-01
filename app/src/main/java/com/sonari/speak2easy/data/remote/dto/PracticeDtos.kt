package com.sonari.speak2easy.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class StartSessionRequest(
    val sessionType: String,
    val isHandsFree: Boolean,
    val lessonNumber: Int? = null,
    val unitNumber: Int? = null,
    val groupLabel: String? = null,
    val charset: String? = null,
)

@Serializable
data class PracticeSessionResponse(
    val sessionId: String = "",
    val sessionType: String = "",
    val startedAt: String? = null,
)

@Serializable
data class PracticeAnalysis(
    val transcription: String? = null,
    val confidence: Double = 0.0,
    val isCorrect: Boolean = false,
    val matchScore: Int? = null,
    val feedbackMessage: String? = null,
)

@Serializable
data class PracticeAttemptResponse(
    val success: Boolean? = null,
    val attemptId: String = "",
    val analysis: PracticeAnalysis = PracticeAnalysis(),
) {
    val isCorrect: Boolean get() = analysis.isCorrect
    val transcribedText: String? get() = analysis.transcription
    val feedback: String? get() = analysis.feedbackMessage
}

@Serializable
data class CompleteSessionRequest(
    val skippedContentIds: List<String> = emptyList(),
)

@Serializable
data class SessionAttemptsResponse(
    val attempts: List<PracticeAttemptDetail> = emptyList(),
    val skipped: List<SkippedItem>? = null,
)

/**
 * Per-attempt detail returned by GET /practice/session/{id}/attempts. Mirrors iOS
 * `PracticeAttemptDetail` (Services/APIClient.swift). [wasCorrect] is preferred over
 * [isCorrect] — iOS picks it first to match the backend's eventual rename.
 */
@Serializable
data class PracticeAttemptDetail(
    val attemptId: String = "",
    val contentId: String? = null,
    val contentText: String? = null,
    val romanization: String? = null,
    val isCorrect: Boolean? = null,
    val wasCorrect: Boolean? = null,
    val matchScore: Double? = null,
    val whisperConfidence: Double? = null,
    val feedback: String? = null,
    val attemptedAt: String? = null,
) {
    val correctness: Boolean? get() = wasCorrect ?: isCorrect
}

@Serializable
data class SkippedItem(
    val contentId: String = "",
    val contentText: String? = null,
    val romanization: String? = null,
)
