package com.sonari.speak2easy.data.practice

import com.sonari.speak2easy.data.lessons.LessonRepository
import com.sonari.speak2easy.data.remote.PracticeApi
import com.sonari.speak2easy.data.remote.apiCall
import com.sonari.speak2easy.data.remote.dto.CompleteSessionRequest
import com.sonari.speak2easy.data.remote.dto.PracticeAttemptResponse
import com.sonari.speak2easy.data.remote.dto.PracticeSessionResponse
import com.sonari.speak2easy.data.remote.dto.StartSessionRequest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class PracticeRepository(
    private val practiceApi: PracticeApi,
    private val json: Json,
    private val progressRepository: ProgressRepository,
    private val lessonRepository: LessonRepository,
) {
    suspend fun startSession(request: StartSessionRequest): PracticeSessionResponse =
        apiCall(json) { practiceApi.startSession(request) }

    suspend fun submitAttempt(sessionId: String, contentId: String, audioFile: File): PracticeAttemptResponse {
        val response = apiCall(json) {
            val sid = sessionId.toRequestBody("text/plain".toMediaType())
            val cid = contentId.toRequestBody("text/plain".toMediaType())
            val extension = audioFile.extension.ifEmpty { "wav" }
            val mediaType = when (extension.lowercase()) {
                "wav" -> "audio/wav"
                "m4a" -> "audio/m4a"
                else -> "application/octet-stream"
            }.toMediaType()
            val audio = MultipartBody.Part.createFormData(
                name = "audio",
                filename = "recording.$extension",
                body = audioFile.asRequestBody(mediaType),
            )
            practiceApi.submitAttempt(sid, cid, audio)
        }
        // A cancelled/incomplete session can still include saved attempts. Lessons uses the
        // progress endpoint, so clear that cache after each accepted attempt instead of waiting
        // for completeSession(), which never runs when the user exits midway.
        lessonRepository.invalidateLessonProgress()
        progressRepository.invalidateUserProgress()
        return response
    }

    suspend fun completeSession(sessionId: String, skipped: List<String>) {
        apiCall(json) { practiceApi.completeSession(sessionId, CompleteSessionRequest(skipped)) }
        // Drop the cached progress so the Progress tab shows fresh numbers on next open.
        progressRepository.invalidateOnSessionComplete()
        // Drop the per-charset lessons cache too — the just-completed session may have unlocked
        // the next lesson on the backend, and without this invalidation the ON_RESUME refresh on
        // Lessons would serve stale data and the new lesson would still appear locked.
        lessonRepository.invalidateLessonProgress()
    }
}
