package com.example.barcode.addItems

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AddItemDraft(
    val barcode: String? = null,
    val name: String? = null,
    val brand: String? = null,
    val expiryDate: Long? = null,
    val imageUrl: String? = null,

    // Pour tester les 4 images candidates récupérées
    val imageCandidates: List<String> = emptyList(),
    val imageCandidateIndex: Int = 0
) : Parcelable