package com.sonari.speak2easy.ui.progress

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sonari.speak2easy.data.practice.ProgressRepository
import com.sonari.speak2easy.data.remote.dto.FeedbackRating
import com.sonari.speak2easy.data.remote.dto.PracticeAttemptDetail
import com.sonari.speak2easy.data.remote.dto.SessionFeedback
import com.sonari.speak2easy.data.remote.dto.SkippedItem
import com.sonari.speak2easy.util.TextSanitizer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class SessionDetailUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val correct: List<PracticeAttemptDetail> = emptyList(),
    val incorrect: List<PracticeAttemptDetail> = emptyList(),
    val skipped: List<SkippedItem> = emptyList(),
    /** "Was this feedback helpful?" votes, keyed by attempt id. */
    val ratings: Map<String, FeedbackRating> = emptyMap(),
    val sessionFeedback: SessionFeedback? = null,
    /** Attempt whose thumbs-down opened the "Help us improve" sheet. */
    val reportTarget: PracticeAttemptDetail? = null,
    val thankedAttemptId: String? = null,
    val ratingError: String? = null,
) {
    val total: Int get() = correct.size + incorrect.size
    val accuracyPercent: Int
        get() = if (total == 0) 0 else (correct.size * 100) / total

    /** The general-feedback card appears once the learner has rated anything here. */
    val showsSessionFeedback: Boolean get() = ratings.isNotEmpty() || sessionFeedback != null
}

class SessionDetailViewModel(
    private val repo: ProgressRepository,
    private val sessionId: String,
) : ViewModel() {

    var state by mutableStateOf(SessionDetailUiState())
        private set

    private val ratingJobs = mutableMapOf<String, Job>()

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)
            try {
                val response = repo.getSessionAttempts(sessionId)
                // iOS parity (Views/Progress/SessionDetailView.swift:18-35): dedup by contentId,
                // first attempt wins. Retries don't reclassify.
                val firstByContent = LinkedHashMap<String, PracticeAttemptDetail>()
                response.attempts.forEach { a ->
                    val key = a.contentId ?: return@forEach
                    if (!firstByContent.containsKey(key)) firstByContent[key] = a
                }
                val first = firstByContent.values.toList()
                state = SessionDetailUiState(
                    isLoading = false,
                    correct = first.filter { it.correctness == true },
                    incorrect = first.filter { it.correctness == false },
                    skipped = response.skipped.orEmpty(),
                    ratings = response.attempts.mapNotNull { a -> a.rating?.let { a.attemptId to it } }.toMap(),
                    sessionFeedback = response.sessionFeedback,
                )
            } catch (e: Exception) {
                state = state.copy(isLoading = false, error = e.message ?: "Couldn't load the session details")
            }
        }
    }

    fun rate(attempt: PracticeAttemptDetail, isHelpful: Boolean) {
        val attemptId = attempt.attemptId
        val previous = state.ratings[attemptId]

        // Tapping the thumb that's already selected withdraws the vote.
        if (previous?.isHelpful == isHelpful) {
            setRating(attemptId, null)
            send(attemptId, previous, optimistic = null) { repo.clearRating(sessionId, attemptId) }
            return
        }

        val optimistic = FeedbackRating(isHelpful = isHelpful)
        setRating(attemptId, optimistic)
        if (isHelpful) flashThanks(attemptId) else state = state.copy(reportTarget = attempt)
        send(attemptId, previous, optimistic) { repo.rateAttempt(sessionId, attemptId, isHelpful) }
    }

    fun dismissReport() {
        state = state.copy(reportTarget = null)
    }

    /** Sends the "Help us improve" answer for the current [SessionDetailUiState.reportTarget]. */
    fun submitReport(reason: FeedbackReportReason, note: String, onResult: (Boolean) -> Unit) {
        val target = state.reportTarget ?: return onResult(false)
        viewModelScope.launch {
            // Let the plain thumbs-down land first so it can't overwrite the reason.
            ratingJobs[target.attemptId]?.join()
            val comment = TextSanitizer.cleanMultilineText(note, MAX_NOTE_CHARS).trim().ifEmpty { null }
            val saved = runCatching {
                repo.rateAttempt(sessionId, target.attemptId, isHelpful = false, reason = reason.apiValue, comment = comment)
            }.onSuccess { setRating(target.attemptId, it) }
            onResult(saved.isSuccess)
        }
    }

    fun submitSessionFeedback(comment: String, onResult: (Boolean) -> Unit) {
        val cleaned = TextSanitizer.cleanMultilineText(comment, MAX_NOTE_CHARS).trim()
        if (cleaned.isEmpty()) return onResult(false)
        viewModelScope.launch {
            val saved = runCatching { repo.submitSessionFeedback(sessionId, cleaned) }
                .onSuccess { state = state.copy(sessionFeedback = it) }
            onResult(saved.isSuccess)
        }
    }

    /** Runs a rating request, putting the old vote back if it fails and nothing newer replaced it. */
    private fun send(
        attemptId: String,
        previous: FeedbackRating?,
        optimistic: FeedbackRating?,
        request: suspend () -> Unit,
    ) {
        val earlier = ratingJobs[attemptId]
        ratingJobs[attemptId] = viewModelScope.launch {
            earlier?.join()
            try {
                request()
            } catch (e: Exception) {
                if (state.ratings[attemptId] == optimistic) {
                    setRating(attemptId, previous)
                    showRatingError()
                }
            }
        }
    }

    private fun setRating(attemptId: String, rating: FeedbackRating?) {
        val updated = if (rating == null) state.ratings - attemptId else state.ratings + (attemptId to rating)
        state = state.copy(ratings = updated)
    }

    private fun flashThanks(attemptId: String) {
        state = state.copy(thankedAttemptId = attemptId)
        viewModelScope.launch {
            delay(THANKS_MS)
            if (state.thankedAttemptId == attemptId) state = state.copy(thankedAttemptId = null)
        }
    }

    private fun showRatingError() {
        state = state.copy(ratingError = "Couldn't save your rating. Please try again.")
        viewModelScope.launch {
            delay(ERROR_MS)
            state = state.copy(ratingError = null)
        }
    }

    class Factory(
        private val repo: ProgressRepository,
        private val sessionId: String,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SessionDetailViewModel(repo, sessionId) as T
    }

    companion object {
        const val MAX_NOTE_CHARS = 300
        private const val THANKS_MS = 2_000L
        private const val ERROR_MS = 3_000L
    }
}
