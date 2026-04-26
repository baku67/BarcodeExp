package com.example.barcode.features.listeCourse

data class ShoppingItemDto(
    val id: Long? = null,
    val clientId: String,
    val homeId: String? = null,
    val scope: String,
    val ownerUserId: String? = null,
    val name: String,
    val quantity: String? = null,
    val note: String? = null,
    val category: String? = null,
    val isImportant: Boolean,
    val isFavorite: Boolean,
    val isChecked: Boolean,
    val isDeleted: Boolean? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val deletedAt: String? = null,
)

data class ShoppingItemDeletedDto(
    val id: Long? = null,
    val clientId: String,
    val deletedAt: String,
)
