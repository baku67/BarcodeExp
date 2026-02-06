package com.example.barcode.features.addItems.data.remote.dto

// DTO reçus
data class ItemDto(
    val id: Int,
    val clientId: String,        // ✅ important
    val homeId: Int? = null,

    val barcode: String?,
    val name: String?,
    val brand: String?,
    val imageUrl: String?,
    val imageIngredientsUrl: String?,
    val imageNutritionUrl: String?,
    val nutriScore: String?,
    val expiryDate: String?,     // yyyy-MM-dd
    val addMode: String?,

    val addedAt: String?,        // ATOM
    val updatedAt: String,
    val deletedAt: String?,
    val isDeleted: Boolean
)
