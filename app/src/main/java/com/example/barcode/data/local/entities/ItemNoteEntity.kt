package com.example.barcode.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "item_notes",
    indices = [
        Index(value = ["itemId"], name = "index_item_notes_itemId"),
        Index(value = ["deletedAt"], name = "index_item_notes_deletedAt"),
        Index(value = ["pendingOperation"], name = "index_item_notes_pendingOperation"),
        Index(value = ["syncState"], name = "index_item_notes_syncState"),
        Index(value = ["serverUpdatedAt"], name = "index_item_notes_serverUpdatedAt"),
    ]
)
data class ItemNoteEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(), // = clientId

    val itemId: String, // = itemClientId (ItemEntity.id)
    val body: String,

    val createdAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,

    // Sync
    @ColumnInfo(defaultValue = "NONE")
    val pendingOperation: PendingOperation = PendingOperation.NONE,

    @ColumnInfo(defaultValue = "OK")
    val syncState: SyncState = SyncState.OK,

    val lastSyncError: String? = null,
    val failedAt: Long? = null,

    @ColumnInfo(name = "updatedAt", defaultValue = "0")
    val localUpdatedAt: Long = System.currentTimeMillis(),

    val serverUpdatedAt: Long? = null,
)
