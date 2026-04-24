package com.example.barcode.features.auth.data.remote.dto

import com.google.gson.annotations.SerializedName

data class RefreshResponseDto(
    val token: String,
    @SerializedName("refresh_token") val refreshToken: String? = null
)