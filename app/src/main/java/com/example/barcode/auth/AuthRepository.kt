package com.example.barcode.auth

class AuthRepository(private val api: AuthApi) {
    suspend fun login(email: String, password: String): Result<LoginResponse> {
        val response = api.login(LoginRequest(email, password))
        return if (response.isSuccessful) {
            response.body()?.let { Result.success(it) }
                ?: Result.failure(Exception("Réponse vide"))
        } else {
            Result.failure(Exception(response.message()))
        }
    }

    suspend fun register(email: String, password: String, confirm: String): Result<RegisterResponse> {
        val response = api.register(RegisterRequest(email, password, confirm))
        return if (response.isSuccessful) {
            response.body()?.let { Result.success(it) }
                ?: Result.failure(Exception("Réponse vide"))
        } else {
            Result.failure(Exception(response.message()))
        }
    }
}