package com.example.barcode.features.listeCourse

data class ShoppingItemDto(
    val clientId: String,
    val homeId: String,
    val scope: String,
    val ownerUserId: String?,
    val name: String,
    val quantity: String?,
    val note: String?,
    val category: String,
    val isImportant: Boolean,
    val isFavorite: Boolean,
    val isChecked: Boolean,
    val createdAt: String?
)

data class ShoppingItemDeletedDto(
    val clientId: String,
    val deletedAt: String,
)