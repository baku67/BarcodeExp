package com.example.barcode.add

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AddItemDraft(
    val barcode: String? = null,
    val name: String? = null,
    val brand: String? = null,
    val expiryDate: Long? = null,
    val imageUrl: String? = null
) : Parcelable