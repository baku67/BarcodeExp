package com.example.barcode.features.addItems

import android.os.Parcelable
import kotlinx.parcelize.Parcelize


@Parcelize
data class AddItemDraft(
    val addMode: ItemAddMode = ItemAddMode.BARCODE_SCAN,
    val expiryDate: Long? = null,
    val photoId: String? = null,

    val barcode: String? = null,
    val name: String? = null,
    val brand: String? = null,
    val imageUrl: String? = null,
    val imageIngredientsUrl: String? = null,
    val imageNutritionUrl: String? = null,
    val nutriScore: String? = null,
    // Pour tester les 4 images candidates récupérées
    val imageCandidates: List<String> = emptyList(),
    val imageCandidateIndex: Int = 0,

    // Propriété ajout produit manuel:
    val manualTypeCode: String? = null,
    val manualSubtypeCode: String? = null,

    // Propriétés ajout LEFTOVERS
    val manualMetaJson: String? = null
) : Parcelable
