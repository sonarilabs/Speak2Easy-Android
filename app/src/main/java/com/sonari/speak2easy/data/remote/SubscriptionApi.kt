package com.sonari.speak2easy.data.remote

import com.sonari.speak2easy.data.remote.dto.GoogleSubscriptionVerifyRequest
import com.sonari.speak2easy.data.remote.dto.SubscriptionStatusResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface SubscriptionApi {
    @GET("subscriptions/status")
    suspend fun getStatus(): SubscriptionStatusResponse

    @POST("subscriptions/verify-google")
    suspend fun verifyGoogle(@Body request: GoogleSubscriptionVerifyRequest): SubscriptionStatusResponse
}
