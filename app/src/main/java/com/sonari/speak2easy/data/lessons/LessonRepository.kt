package com.sonari.speak2easy.data.lessons

import com.sonari.speak2easy.data.remote.ContentApi
import com.sonari.speak2easy.data.remote.apiCall
import com.sonari.speak2easy.data.remote.dto.ContentGroupsResponse
import com.sonari.speak2easy.data.remote.dto.ContentItem
import com.sonari.speak2easy.data.remote.dto.ContentItemsResponse
import com.sonari.speak2easy.data.remote.dto.LessonContentResponse
import com.sonari.speak2easy.data.remote.dto.LessonsResponse
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

/**
 * In-memory cache for the lesson/content endpoints. Chart contents and content-group lists are
 * effectively static, so we never expire them in-process. Per-lesson and per-group item payloads
 * are also cached for the lifetime of the app — they only change when the user advances the curriculum,
 * and a process restart picks up any backend updates anyway. Progress (which IS user-driven) lives
 * in [com.sonari.speak2easy.data.practice.ProgressRepository] with a short TTL.
 *
 * Mirrors the iOS pattern in `APIClient.swift` where chart/content responses are cached against
 * UserDefaults; here we only do in-memory caching for now (chart data ships in the APK as a
 * fallback via [com.sonari.speak2easy.ui.lessons.LessonData]).
 */
class LessonRepository(
    private val contentApi: ContentApi,
    private val json: Json,
) {
    private val lock = Mutex()

    private var hiraganaChart: List<ContentItem>? = null
    private var katakanaChart: List<ContentItem>? = null
    private val lessonsByCharset = mutableMapOf<String, LessonsResponse>()
    private val contentGroupsByType = mutableMapOf<String, ContentGroupsResponse>()
    private val lessonContentByKey = mutableMapOf<String, LessonContentResponse>()
    private val contentItemsByKey = mutableMapOf<String, ContentItemsResponse>()

    /** Force a refresh on next call — used when sign-out clears user-scoped state. */
    suspend fun invalidateAll() = lock.withLock {
        hiraganaChart = null
        katakanaChart = null
        lessonsByCharset.clear()
        contentGroupsByType.clear()
        lessonContentByKey.clear()
        contentItemsByKey.clear()
    }

    /**
     * Drops only the per-charset lesson progress/unlock cache so the next [getLessons] call
     * round-trips to the API. Called from [com.sonari.speak2easy.data.practice.PracticeRepository]
     * right after a session completes — otherwise the cached lessonsByCharset entry returns the
     * pre-session unlock state and the next lesson appears locked even after the backend just
     * unlocked it. Chart/content/group caches stay (those are immutable for the user).
     */
    suspend fun invalidateLessonProgress() = lock.withLock {
        lessonsByCharset.clear()
    }

    /** Progress-tracking endpoint — bypasses the lessons cache because the unlocked map changes per session. */
    suspend fun getLessons(charset: String, forceRefresh: Boolean = false): LessonsResponse =
        cached(lessonsByCharset, charset, forceRefresh) { apiCall(json) { contentApi.getLessons(charset = charset) } }

    suspend fun getContentGroups(contentType: String = "word", forceRefresh: Boolean = false): ContentGroupsResponse =
        cached(contentGroupsByType, contentType, forceRefresh) {
            apiCall(json) { contentApi.getContentGroups(contentType = contentType) }
        }

    suspend fun getLessonContent(
        lessonNumber: Int,
        charset: String,
        contentType: String?,
        unitNumber: Int?,
    ): LessonContentResponse {
        val key = "$lessonNumber|$charset|${contentType ?: ""}|${unitNumber ?: ""}"
        return cached(lessonContentByKey, key) {
            apiCall(json) { contentApi.getLessonContent(lessonNumber, charset, contentType, unitNumber) }
        }
    }

    suspend fun getContentItems(
        contentType: String?,
        charset: String?,
        groupLabel: String?,
        limit: Int = 200,
    ): ContentItemsResponse {
        val key = "${contentType ?: ""}|${charset ?: ""}|${groupLabel ?: ""}|$limit"
        return cached(contentItemsByKey, key) {
            apiCall(json) { contentApi.getContentItems(contentType, charset, groupLabel, limit) }
        }
    }

    suspend fun getHiraganaChart(): List<ContentItem> {
        hiraganaChart?.let { return it }
        return lock.withLock {
            hiraganaChart ?: apiCall(json) { contentApi.getHiraganaChart() }.also { hiraganaChart = it }
        }
    }

    suspend fun getKatakanaChart(): List<ContentItem> {
        katakanaChart?.let { return it }
        return lock.withLock {
            katakanaChart ?: apiCall(json) { contentApi.getKatakanaChart() }.also { katakanaChart = it }
        }
    }

    private suspend inline fun <K, V> cached(
        map: MutableMap<K, V>,
        key: K,
        forceRefresh: Boolean = false,
        load: () -> V,
    ): V {
        if (!forceRefresh) map[key]?.let { return it }
        return lock.withLock {
            if (!forceRefresh) map[key]?.let { return it }
            load().also { map[key] = it }
        }
    }
}
