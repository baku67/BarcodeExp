package com.example.barcode.features.auth

import com.example.barcode.features.auth.data.remote.dto.RefreshResponseDto
import com.example.barcode.domain.models.LoginRequest
import com.example.barcode.domain.models.LoginResponse
import com.example.barcode.domain.models.RegisterRequest
import com.example.barcode.domain.models.RegisterResponse
import com.example.barcode.domain.models.UserProfile
import retrofit2.Response
import retrofit2.http.*

interface AuthApi {

    @POST("auth/login_check")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @POST("auth/verify/resend")
    suspend fun resendEmailVerification(@Header("Authorization") token: String): Response<Unit>

    @FormUrlEncoded
    @POST("auth/token/refresh")
    suspend fun refresh(@Field("refresh_token") refreshToken: String): Response<RefreshResponseDto>

    @GET("api/me")
    suspend fun me(@Header("Authorization") authorization: String): Response<UserProfile>

    @PATCH("api/me/preferences")
    suspend fun patchPreferences(
        @Header("Authorization") token: String,
        @Body body: Map<String, String>
    ): Response<Unit>

    @DELETE("api/me")
    suspend fun deleteMe(@Header("Authorization") token: String): Response<Unit>
}