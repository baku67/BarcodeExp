package com.example.barcode.addItems

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