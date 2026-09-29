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
    val matchScore: Double? = null,
    val feedbackMessage: String? = null,
    val diagnosticReason: String? = null,
    val feedback: PronunciationFeedback? = null,
)

/**
 * Personalised pronunciation feedback (backend `feedback`, version 2). Every field is
 * optional: older attempts and older backends only carry [messageEn].
 */
@Serializable
data class PronunciationFeedback(
    val version: Int? = null,
    /** "high" states the finding, "moderate" hedges it, "low" names no specific sound. */
    val confidenceLevel: String? = null,
    val headline: String? = null,
    val didWell: String? = null,
    val issue: PronunciationFeedbackIssue? = null,
    val nextStep: String? = null,
    val messageEn: String? = null,
)

@Serializable
data class PronunciationFeedbackIssue(
    val category: String? = null,
    val expectedMora: String? = null,
    val heardMora: String? = null,
    val moraIndex: Int? = null,
    val moraCount: Int? = null,
    val position: String? = null,
    val text: String? = null,
)

/** A learner's "Was this feedback helpful?" answer for one attempt. */
@Serializable
data class FeedbackRating(
    val isHelpful: Boolean = false,
    val reason: String? = null,
    val comment: String? = null,
)

/** No defaults on [platform]: kotlinx.serialization leaves default values out of the body. */
@Serializable
data class AttemptRatingRequest(
    val isHelpful: Boolean,
    val reason: String? = null,
    val comment: String? = null,
    val platform: String,
    val appVersion: String? = null,
)

@Serializable
data class AttemptRatingResponse(
    val success: Boolean? = null,
    val rating: FeedbackRating = FeedbackRating(),
)

/** Free-text feedback about a whole session's pronunciation feedback. */
@Serializable
data class SessionFeedback(
    val comment: String = "",
    val updatedAt: String? = null,
)

@Serializable
data class SessionFeedbackRequest(
    val comment: String,
    val platform: String,
    val appVersion: String? = null,
)

@Serializable
data class SessionFeedbackResponse(
    val success: Boolean? = null,
    val sessionFeedback: SessionFeedback = SessionFeedback(),
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
    val sessionFeedback: SessionFeedback? = null,
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
    val feedbackDetails: PronunciationFeedback? = null,
    val confidenceLevel: String? = null,
    val rating: FeedbackRating? = null,
    val attemptedAt: String? = null,
) {
    val correctness: Boolean? get() = wasCorrect ?: isCorrect

    /** The feedback text shown for this attempt, if any. */
    val feedbackText: String? get() = (feedback ?: feedbackDetails?.messageEn)?.takeIf { it.isNotBlank() }
}

@Serializable
data class SkippedItem(
    val contentId: String = "",
    val contentText: String? = null,
    val romanization: String? = null,
)
