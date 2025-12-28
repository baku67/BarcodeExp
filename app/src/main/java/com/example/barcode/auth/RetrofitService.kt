package com.example.barcode.auth

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.DELETE

interface AuthApi {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @GET("me")
    suspend fun me(@Header("Authorization") authorization: String): Response<UserProfile>

    @DELETE("me")
    suspend fun deleteMe(@Header("Authorization") token: String): Response<Unit>
}