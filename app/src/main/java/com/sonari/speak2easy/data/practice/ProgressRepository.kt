package com.sonari.speak2easy.data.practice

import com.sonari.speak2easy.data.remote.PracticeApi
import com.sonari.speak2easy.data.remote.UserApi
import com.sonari.speak2easy.data.remote.apiCall
import com.sonari.speak2easy.data.remote.dto.PracticeSessionSummary
import com.sonari.speak2easy.data.remote.dto.SessionAttemptsResponse
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
    }
}
