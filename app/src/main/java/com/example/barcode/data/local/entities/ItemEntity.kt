package com.example.barcode.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

// Table SQLite (stockage LOCAL, pas Cache)
// Nécessite migration si changement de schema
@Entity(
    tableName = "items",
    indices = [
        Index(value = ["deletedAt"], name = "index_items_deletedAt"),
        Index(value = ["syncStatus"], name = "index_items_syncStatus"),
        Index(value = ["serverUpdatedAt"], name = "index_items_serverUpdatedAt"),
    ]
)
data class ItemEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),

    // 🔹 Sync
    @ColumnInfo(defaultValue = "PENDING_CREATE")
    val syncStatus: SyncStatus = SyncStatus.PENDING_CREATE,

    // ⚠️ On garde le NOM de colonne "updatedAt" car ta DB l’a déjà
    @ColumnInfo(name = "updatedAt", defaultValue = "0")
    val localUpdatedAt: Long = System.currentTimeMillis(),

    // ✅ Timestamp serveur (pour delta sync / debug / merge):
    val serverUpdatedAt: Long? = null,


    // 🔹 Métier
    val barcode: String? = null,
    val name: String? = null,
    val brand: String? = null,

    val imageUrl: String? = null, // image principale (thumbnail)
    val imageIngredientsUrl: String? = null,
    val imageNutritionUrl: String? = null,
    val nutriScore: String? = null,

    val addedAt: Long? = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val expiryDate: Long? = null, // en epoch millis

    @ColumnInfo(defaultValue = "barcode_scan")
    val addMode: String = "barcode_scan",
)


enum class SyncStatus {
    PENDING_CREATE,
    PENDING_DELETE,
    SYNCED,
    FAILED
}