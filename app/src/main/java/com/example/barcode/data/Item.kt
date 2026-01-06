package com.example.barcode.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

// Table SQLite (stockage LOCAL, pas Cache)
// Nécessite migration si changement de schema
@Entity(tableName = "items")
data class Item(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val barcode: String? = null,
    val name: String? = null,
    val brand: String? = null,

    val imageUrl: String? = null, // image principale (thumbnail)
    val imageIngredientsUrl: String? = null,
    val imageNutritionUrl: String? = null,

    val nutriScore: String? = null,

    val addedAt: Long? = System.currentTimeMillis(),
    val expiryDate: Long? = null // en epoch millis
)