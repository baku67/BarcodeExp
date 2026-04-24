package com.example.barcode.features.auth.data.remote.dto

import com.google.gson.annotations.SerializedName

data class RefreshRequestDto(
    @SerializedName("refresh_token") val refreshToken: String
)