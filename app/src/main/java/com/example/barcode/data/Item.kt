package com.example.barcode.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "items")
data class Item(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val barcode: String? = null,
    val name: String? = null,
    val brand: String? = null,
    val imageUrl: String? = null,
    val nutriScore: String? = null,

    val addedAt: Long? = System.currentTimeMillis(),
    val expiryDate: Long? = null // date d’expiration en epoch millis
)