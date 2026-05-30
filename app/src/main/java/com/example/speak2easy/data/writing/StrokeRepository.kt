package com.example.speak2easy.data.writing

import com.example.speak2easy.data.remote.WritingApi
import com.example.speak2easy.data.remote.apiCall
import com.example.speak2easy.data.remote.dto.EvaluationRequest
import com.example.speak2easy.data.remote.dto.EvaluationResponse
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

/**
 * In-memory cache for AnimCJK stroke SVGs (mirrors iOS `StrokeOrderCache`).
 * Process-lifetime — SVGs don't change once written, and a sign-out clears the map.
 */
class StrokeRepository(
    private val api: WritingApi,
    private val json: Json,
) {
    private val lock = Mutex()
    private val svgCache = mutableMapOf<String, String>()  // key = "$charset:$character"

    suspend fun getSvg(charset: String, character: String): String {
        val key = "$charset:$character"
        svgCache[key]?.let { return it }
        return lock.withLock {
            svgCache[key]
                ?: apiCall(json) { api.getStrokeSvg(charset, character).string() }
                    .also { svgCache[key] = it }
        }
    }

    suspend fun evaluate(charset: String, request: EvaluationRequest): EvaluationResponse =
        apiCall(json) { api.evaluate(charset, request) }

    suspend fun invalidateAll() = lock.withLock { svgCache.clear() }
}
