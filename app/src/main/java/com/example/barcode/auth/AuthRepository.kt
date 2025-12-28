package com.example.barcode.auth

import retrofit2.Response
import kotlinx.coroutines.CancellationException

class AuthRepository(private val api: AuthApi) {

    suspend fun login(email: String, password: String): Result<LoginResponse> {
        return try {
            val response = api.login(LoginRequest(email, password))

            if (response.isSuccessful) {
                response.body()?.let { Result.success(it) }
                    ?: Result.failure(Exception("Réponse vide"))
            } else {
                Result.failure(Exception("HTTP ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e) // <-- empêche le crash, et tu affiches e.message dans l'UI
        }
    }

    suspend fun register(email: String, password: String, confirmPassword: String): Result<RegisterResponse> {
        return try {
            val response = api.register(RegisterRequest(email, password, confirmPassword))

            if (response.isSuccessful) {
                response.body()?.let { Result.success(it) }
                    ?: Result.failure(Exception("Réponse vide"))
            } else {
                Result.failure(Exception("HTTP ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun me(token: String): Result<UserProfile> {
        return try {
            val res: Response<UserProfile> = api.me("Bearer $token")
            if (res.isSuccessful && res.body() != null) Result.success(res.body()!!)
            else Result.failure(Exception("HTTP ${res.code()} - ${res.message()}"))
        } catch (e: CancellationException) {
            throw e // ✅ super important
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteMe(token: String): Result<Unit> {
        return try {
            val response = api.deleteMe("Bearer $token")
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("HTTP ${response.code()} - ${response.message()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resendVerifyEmail(token: String): Result<Unit> = try {
        val res = api.resendEmailVerification("Bearer $token") // POST auth/verify/resend
        if (res.isSuccessful) Result.success(Unit)
        else Result.failure(Exception("HTTP ${res.code()} - ${res.message()}"))
    } catch (e: Exception) {
        Result.failure(e)
    }

}