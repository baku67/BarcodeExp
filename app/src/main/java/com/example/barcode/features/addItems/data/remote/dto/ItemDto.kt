package com.example.barcode.features.addItems.data.remote.dto

// DTO reçus
data class ItemDto(
    val id: Int,
    val clientId: String,        // ✅ important
    val barcode: String?,
    val name: String?,
    val brand: String?,
    val imageUrl: String?,
    val imageIngredientsUrl: String?,
    val imageNutritionUrl: String?,
    val nutriScore: String?,
    val addedAt: String?,        // ATOM
    val expiryDate: String?,     // yyyy-MM-dd
    val addMode: String?
)
