package com.sonari.speak2easy.data.remote

import com.sonari.speak2easy.data.remote.dto.AuthResponse
import com.sonari.speak2easy.data.remote.dto.ForgotPasswordRequest
import com.sonari.speak2easy.data.remote.dto.MessageResponse
import com.sonari.speak2easy.data.remote.dto.RefreshTokenRequest
import com.sonari.speak2easy.data.remote.dto.ResendVerificationRequest
import com.sonari.speak2easy.data.remote.dto.ResetPasswordRequest
import com.sonari.speak2easy.data.remote.dto.SignInRequest
import com.sonari.speak2easy.data.remote.dto.TokenRefreshResponse
import com.sonari.speak2easy.data.remote.dto.TokenVerifyResponse
import com.sonari.speak2easy.domain.model.User
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/signin")
    suspend fun signIn(@Body request: SignInRequest): AuthResponse

    @GET("auth/token/verify")
    suspend fun verifyToken(): TokenVerifyResponse

    @POST("auth/token/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): TokenRefreshResponse

    @POST("auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): MessageResponse

    @POST("auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): MessageResponse

    @POST("auth/resend-verification")
    suspend fun resendVerification(@Body request: ResendVerificationRequest): MessageResponse

    @GET("auth/verification-status")
    suspend fun verificationStatus(): User
}
