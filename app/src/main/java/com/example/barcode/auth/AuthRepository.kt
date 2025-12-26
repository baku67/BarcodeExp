package com.example.barcode.auth

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