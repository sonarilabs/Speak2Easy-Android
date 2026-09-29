package com.sonari.speak2easy.data.practice

import com.sonari.speak2easy.BuildConfig
import com.sonari.speak2easy.data.remote.ApiException
import com.sonari.speak2easy.data.remote.PracticeApi
import com.sonari.speak2easy.data.remote.UserApi
import com.sonari.speak2easy.data.remote.apiCall
import com.sonari.speak2easy.data.remote.dto.AttemptRatingRequest
import com.sonari.speak2easy.data.remote.dto.FeedbackRating
import com.sonari.speak2easy.data.remote.dto.PracticeSessionSummary
import com.sonari.speak2easy.data.remote.dto.SessionAttemptsResponse
import com.sonari.speak2easy.data.remote.dto.SessionFeedback
import com.sonari.speak2easy.data.remote.dto.SessionFeedbackRequest
import com.sonari.speak2easy.data.remote.dto.UserProgressResponse
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

/**
 * Short-TTL cache so flipping into the Progress tab doesn't re-hit the backend every time.
 * Cleared on session-completion via [invalidateOnSessionComplete] and on sign-out.
 */
class ProgressRepository(
    private val userApi: UserApi,
    private val practiceApi: PracticeApi,
    private val json: Json,
) {
    private val lock = Mutex()
    private var cachedProgress: Cached<UserProgressResponse>? = null
    private var cachedSessions: Cached<List<PracticeSessionSummary>>? = null
    private val cachedAttempts = mutableMapOf<String, SessionAttemptsResponse>()

    suspend fun getProgress(userId: String, forceRefresh: Boolean = false): UserProgressResponse {
        val now = System.currentTimeMillis()
        cachedProgress?.takeIf { !forceRefresh && it.userId == userId && now - it.fetchedAt < TTL_MS }
            ?.let { return it.value }
        return lock.withLock {
            val fresh = apiCall(json) { userApi.getProgress(userId) }
            cachedProgress = Cached(userId, fresh, now)
            fresh
        }
    }

    suspend fun getSessions(userId: String, limit: Int = 20, forceRefresh: Boolean = false): List<PracticeSessionSummary> {
        val now = System.currentTimeMillis()
        val cacheKey = "$userId:$limit"
        cachedSessions?.takeIf { !forceRefresh && it.userId == cacheKey && now - it.fetchedAt < TTL_MS }
            ?.let { return it.value }
        return lock.withLock {
            val fresh = apiCall(json) { practiceApi.getSessions(limit = limit) }
            cachedSessions = Cached(cacheKey, fresh, now)
            fresh
        }
    }

    /** Session attempts are immutable once recorded — cache for the lifetime of the app. */
    suspend fun getSessionAttempts(sessionId: String): SessionAttemptsResponse {
        cachedAttempts[sessionId]?.let { return it }
        return lock.withLock {
            cachedAttempts[sessionId]
                ?: apiCall(json) { practiceApi.getSessionAttempts(sessionId) }.also { cachedAttempts[sessionId] = it }
        }
    }

    /** "Was this feedback helpful?" for one attempt. Returns the rating as saved. */
    suspend fun rateAttempt(
        sessionId: String,
        attemptId: String,
        isHelpful: Boolean,
        reason: String? = null,
        comment: String? = null,
    ): FeedbackRating {
        val request = AttemptRatingRequest(isHelpful, reason, comment, PLATFORM, BuildConfig.VERSION_NAME)
        val saved = apiCall(json) { practiceApi.rateAttempt(attemptId, request) }.rating
        updateCachedRating(sessionId, attemptId, saved)
        return saved
    }

    /** Withdraws a rating (tapping the selected thumb again). */
    suspend fun clearRating(sessionId: String, attemptId: String) {
        val response = apiCall(json) { practiceApi.clearAttemptRating(attemptId) }
        if (!response.isSuccessful) throw ApiException(response.code(), "Couldn't clear the rating")
        updateCachedRating(sessionId, attemptId, null)
    }

    suspend fun submitSessionFeedback(sessionId: String, comment: String): SessionFeedback {
        val request = SessionFeedbackRequest(comment, PLATFORM, BuildConfig.VERSION_NAME)
        val saved = apiCall(json) { practiceApi.submitSessionFeedback(sessionId, request) }.sessionFeedback
        lock.withLock {
            cachedAttempts[sessionId]?.let { cachedAttempts[sessionId] = it.copy(sessionFeedback = saved) }
        }
        return saved
    }

    /** Keeps the cached attempts in step so revisiting a session shows the latest vote. */
    private suspend fun updateCachedRating(sessionId: String, attemptId: String, rating: FeedbackRating?) = lock.withLock {
        val cached = cachedAttempts[sessionId] ?: return@withLock
        cachedAttempts[sessionId] = cached.copy(
            attempts = cached.attempts.map { if (it.attemptId == attemptId) it.copy(rating = rating) else it },
        )
    }

    /** Called when practice activity changes user-visible progress. */
    suspend fun invalidateUserProgress() = lock.withLock {
        cachedProgress = null
        cachedSessions = null
    }

    /** Called from the practice flow when a session finishes so the Progress tab refreshes. */
    suspend fun invalidateOnSessionComplete() = invalidateUserProgress()

    suspend fun invalidateAll() = lock.withLock {
        cachedProgress = null
        cachedSessions = null
        cachedAttempts.clear()
    }

    private data class Cached<V>(val userId: String, val value: V, val fetchedAt: Long)

    private companion object {
        // 30 seconds is long enough to coalesce tab-switch fetches but short enough that
        // a return-from-background sees current-ish numbers.
        const val TTL_MS = 30_000L
        const val PLATFORM = "android"
    }
}
