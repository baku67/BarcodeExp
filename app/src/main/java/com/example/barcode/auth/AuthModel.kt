package com.example.barcode.auth

data class LoginRequest(val email: String, val password: String)
data class LoginResponse(val token: String)

data class RegisterRequest(val email: String, val password: String, val confirmPassword: String)
data class RegisterResponse(val id: String, val token: String)