package com.example.barcode.domain.models

data class ProductInfo(
    val name: String,
    val brand: String,
    val imageUrl: String,
    val nutriScore: String,
    val imageCandidates: List<String> = emptyList(),
    val imageIngredientsUrl: String?,
    val imageNutritionUrl: String?
)