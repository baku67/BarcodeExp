package com.example.barcode.features.addItems.data.remote.dto

data class ItemDeletedDto(
    val id: Int? = null,
    val clientId: String,
    val deletedAt: String // ATOM/ISO 8601 (ex: 2026-02-10T09:32:08Z)
)