package com.example.speak2easy.data.remote

import com.example.speak2easy.data.remote.dto.OnboardingRequest
import com.example.speak2easy.data.remote.dto.OnboardingResponse
import com.example.speak2easy.data.remote.dto.UserProgressResponse
import com.example.speak2easy.domain.model.User
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

interface UserApi {
    @GET("users/{userId}")
    suspend fun getUser(@Path("userId") userId: String): User

    @PUT("users/{userId}/onboarding")
    suspend fun completeOnboarding(
        @Path("userId") userId: String,
        @Body request: OnboardingRequest,
    ): OnboardingResponse

    @GET("users/{userId}/progress")
    suspend fun getProgress(@Path("userId") userId: String): UserProgressResponse

    @DELETE("users/{userId}")
    suspend fun deleteAccount(@Path("userId") userId: String): Response<Unit>
}
