package com.example.barcode.features.addItems.data.remote.dto

data class CreateItemRequestDto(
    val clientId: String,
    val name: String,
    val expiryDate: String?, // yyyy-MM-dd
    val addMode: String,
    val scan: ScanPayload? = null,
    val manual: ManualPayload? = null
)

data class ScanPayload(
    val barcode: String,
    val brand: String? = null,
    val imageUrl: String? = null,
    val imageIngredientsUrl: String? = null,
    val imageNutritionUrl: String? = null,
    val nutriScore: String? = null
)

data class ManualPayload(
    val type: String,
    val subtype: String? = null,
    val meta: Map<String, Any?>? = null
)
