package com.example.barcode.features.addItems.data.remote.dto

// DTO reçus
data class ItemDto(
    val id: Int,
    val clientId: String,
    val homeId: Int? = null,

    val photoId: String? = null, // photoId == clientId

    val name: String?,
    val expiryDate: String?,     // yyyy-MM-dd
    val addMode: String?,        // "barcode_scan" | "manual"

    // ✅ Nouveau (backend v2)
    val scan: ScanDto? = null,
    val manual: ManualDto? = null,

    // ✅ Ancien (backend v1) - on garde pour compat
        val barcode: String? = null,
    val brand: String? = null,
    val imageUrl: String? = null,
    val imageIngredientsUrl: String? = null,
    val imageNutritionUrl: String? = null,
    val nutriScore: String? = null,

    val addedAt: String?,        // ATOM
    val updatedAt: String,
    val deletedAt: String?,
    val isDeleted: Boolean
)

data class ScanDto(
    val barcode: String,
    val brand: String? = null,
    val imageUrl: String? = null,
    val imageIngredientsUrl: String? = null,
    val imageNutritionUrl: String? = null,
    val nutriScore: String? = null
)

data class ManualDto(
    val type: String,
    val subtype: String? = null,
    // Si tu n'en as pas besoin tout de suite, tu peux laisser null/ignorer
    val meta: Map<String, Any?>? = null
    // Photo custom unique
)

