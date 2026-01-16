package com.example.barcode.features.addItems

import android.os.Parcelable
import com.example.barcode.features.addItems.manual.ManualSubType
import com.example.barcode.features.addItems.manual.ManualType
import kotlinx.parcelize.Parcelize


@Parcelize
data class AddItemDraft(
    val barcode: String? = null,
    val name: String? = null,
    val brand: String? = null,
    val expiryDate: Long? = null,

    val imageUrl: String? = null,
    val imageIngredientsUrl: String? = null,
    val imageNutritionUrl: String? = null,

    val nutriScore: String? = null,

    // Pour tester les 4 images candidates récupérées
    val imageCandidates: List<String> = emptyList(),
    val imageCandidateIndex: Int = 0,

    val addMode: ItemAddMode = ItemAddMode.BARCODE_SCAN,

    // Propriété ajout produit manuel:
    val manualType: ManualType? = null,
    val manualSubtype: ManualSubType? = null
) : Parcelable



// Helpers
fun ManualType.subtypes(): List<ManualSubType> =
    ManualSubType.entries.filter { it.parentType == this }
fun requireSubtypeCompatible(type: ManualType?, sub: ManualSubType?): Boolean {
    if (type == null || sub == null) return false
    return sub.parentType == type
}