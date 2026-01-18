package com.example.barcode.features.addItems.data.remote.dto

data class ItemCreateDto(
    val clientId: String,      // ✅ UUID local (ItemEntity.id)
    val barcode: String?,
    val name: String?,
    val brand: String?,
    val imageUrl: String?,
    val imageIngredientsUrl: String?,
    val imageNutritionUrl: String?,
    val nutriScore: String?,
    val expiryDate: String?,
    val addMode: String,
)
