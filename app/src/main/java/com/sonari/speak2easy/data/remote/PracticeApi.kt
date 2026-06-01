package com.sonari.speak2easy.data.remote

import com.sonari.speak2easy.data.remote.dto.CompleteSessionRequest
import com.sonari.speak2easy.data.remote.dto.PracticeAttemptResponse
import com.sonari.speak2easy.data.remote.dto.PracticeSessionResponse
import com.sonari.speak2easy.data.remote.dto.PracticeSessionSummary
import com.sonari.speak2easy.data.remote.dto.SessionAttemptsResponse
import com.sonari.speak2easy.data.remote.dto.StartSessionRequest
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface PracticeApi {
    @POST("practice/session/start")
    suspend fun startSession(@Body request: StartSessionRequest): PracticeSessionResponse

    @Multipart
    @POST("practice/attempt")
    suspend fun submitAttempt(
        @Part("session_id") sessionId: RequestBody,
        @Part("content_id") contentId: RequestBody,
        @Part audio: MultipartBody.Part,
    ): PracticeAttemptResponse

    @PUT("practice/session/{sessionId}/complete")
    suspend fun completeSession(
        @Path("sessionId") sessionId: String,
        @Body request: CompleteSessionRequest,
    ): Response<Unit>

    @GET("practice/sessions")
    suspend fun getSessions(
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
    ): List<PracticeSessionSummary>

    @GET("practice/session/{sessionId}/attempts")
    suspend fun getSessionAttempts(@Path("sessionId") sessionId: String): SessionAttemptsResponse
}
