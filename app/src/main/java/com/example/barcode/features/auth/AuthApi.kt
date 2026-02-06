package com.example.barcode.features.auth

import com.example.barcode.features.auth.data.remote.dto.RefreshRequestDto
import com.example.barcode.features.auth.data.remote.dto.RefreshResponseDto
import com.example.barcode.domain.models.LoginRequest
import com.example.barcode.domain.models.LoginResponse
import com.example.barcode.domain.models.RegisterRequest
import com.example.barcode.domain.models.RegisterResponse
import com.example.barcode.domain.models.UserProfile
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.DELETE
import retrofit2.http.PATCH

interface AuthApi {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @POST("auth/verify/resend")
    suspend fun resendEmailVerification(@Header("Authorization") token: String): Response<Unit>

    @POST("api/token/refresh")
    suspend fun refresh(@Body request: RefreshRequestDto): Response<RefreshResponseDto>


    // /me
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