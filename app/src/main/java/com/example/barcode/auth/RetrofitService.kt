package com.example.barcode.auth

import com.example.barcode.user.LoginRequest
import com.example.barcode.user.LoginResponse
import com.example.barcode.user.RegisterRequest
import com.example.barcode.user.RegisterResponse
import com.example.barcode.user.UserProfile
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

    @GET("me")
    suspend fun me(@Header("Authorization") authorization: String): Response<UserProfile>

    @PATCH("me/preferences")
    suspend fun patchPreferences(
        @Header("Authorization") token: String,
        @Body body: Map<String, String>
    ): Response<Unit>
    // Exemple patch preferences
    //api.patchPreferences("Bearer $token", mapOf(
    //    "theme" to "dark",
    //    "lang" to "fr",
    //    "frigo_layout" to "list"
    //))

    @DELETE("me")
    suspend fun deleteMe(@Header("Authorization") token: String): Response<Unit>

    @POST("auth/verify/resend")
    suspend fun resendEmailVerification(@Header("Authorization") token: String): Response<Unit>
}