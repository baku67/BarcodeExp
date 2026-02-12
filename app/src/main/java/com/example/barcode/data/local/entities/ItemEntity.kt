package com.example.barcode.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.barcode.data.local.entities.PendingOperation
import com.example.barcode.data.local.entities.SyncState
import java.util.UUID

// Table SQLite (stockage LOCAL, pas Cache)
// Nécessite migration si changement de schema
@Entity(
    tableName = "items",
    indices = [
        Index(value = ["deletedAt"], name = "index_items_deletedAt"),
        Index(value = ["pendingOperation"], name = "index_items_pendingOperation"),
        Index(value = ["syncState"], name = "index_items_syncState"),
        Index(value = ["serverUpdatedAt"], name = "index_items_serverUpdatedAt"),
    ]
)
data class ItemEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),

    // 🔹 Sync
    @ColumnInfo(defaultValue = "NONE")
    val pendingOperation: PendingOperation = PendingOperation.NONE,

    @ColumnInfo(defaultValue = "OK")
    val syncState: SyncState = SyncState.OK,

    val lastSyncError: String? = null,
    val failedAt: Long? = null,

    @ColumnInfo(name = "updatedAt", defaultValue = "0")
    val localUpdatedAt: Long = System.currentTimeMillis(),

    // Timestamp serveur (pour delta sync / debug / merge):
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

    // ✅ Manual
    val manualType: String? = null,
    val manualSubtype: String? = null,
    val manualMetaJson: String? = null,
)
