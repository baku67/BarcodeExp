package com.example.barcode.user

data class RefreshRequest(
    val refresh_token: String
)

data class RefreshResponse(
    val token: String,
    val refresh_token: String? = null // rotation => souvent renvoyé
)
