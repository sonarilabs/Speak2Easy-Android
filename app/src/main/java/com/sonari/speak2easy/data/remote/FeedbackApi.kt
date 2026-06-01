package com.sonari.speak2easy.data.remote

import com.sonari.speak2easy.data.remote.dto.FeedbackRequest
import com.sonari.speak2easy.data.remote.dto.FeedbackResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

/** Bug / feature / general feedback submission. Auth header is added by [AuthInterceptor]. */
interface FeedbackApi {
    @POST("users/{userId}/feedback")
    suspend fun sendFeedback(
        @Path("userId") userId: String,
        @Body request: FeedbackRequest,
    ): FeedbackResponse
}
