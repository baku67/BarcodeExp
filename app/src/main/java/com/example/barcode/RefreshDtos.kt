package com.example.barcode

data class RefreshRequest(
    val refresh_token: String
)

data class RefreshResponse(
    val token: String,
    val refresh_token: String? = null // rotation => souvent renvoyé
)
