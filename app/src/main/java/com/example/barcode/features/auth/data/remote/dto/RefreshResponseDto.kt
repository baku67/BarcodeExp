package com.example.barcode.features.auth.data.remote.dto

data class RefreshResponseDto(
    val token: String,
    val refresh_token: String? = null // rotation => souvent renvoyé
)