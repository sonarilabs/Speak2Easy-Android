package com.example.speak2easy.data.practice

import com.example.speak2easy.data.remote.PracticeApi
import com.example.speak2easy.data.remote.apiCall
import com.example.speak2easy.data.remote.dto.CompleteSessionRequest
import com.example.speak2easy.data.remote.dto.PracticeAttemptResponse
import com.example.speak2easy.data.remote.dto.PracticeSessionResponse
import com.example.speak2easy.data.remote.dto.StartSessionRequest
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
) {
    suspend fun startSession(request: StartSessionRequest): PracticeSessionResponse =
        apiCall(json) { practiceApi.startSession(request) }

    suspend fun submitAttempt(sessionId: String, contentId: String, audioFile: File): PracticeAttemptResponse =
        apiCall(json) {
            val sid = sessionId.toRequestBody("text/plain".toMediaType())
            val cid = contentId.toRequestBody("text/plain".toMediaType())
            val audio = MultipartBody.Part.createFormData(
                name = "audio",
                filename = "recording.m4a",
                body = audioFile.asRequestBody("audio/m4a".toMediaType()),
            )
            practiceApi.submitAttempt(sid, cid, audio)
        }

    suspend fun completeSession(sessionId: String, skipped: List<String>) {
        apiCall(json) { practiceApi.completeSession(sessionId, CompleteSessionRequest(skipped)) }
        // Drop the cached progress so the Progress tab shows fresh numbers on next open.
        progressRepository.invalidateOnSessionComplete()
    }
}
