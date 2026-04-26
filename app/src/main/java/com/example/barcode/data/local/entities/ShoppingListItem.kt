package com.example.barcode.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "shopping_list_items",
    indices = [
        Index(value = ["deletedAt"], name = "index_shopping_list_items_deletedAt"),
        Index(value = ["pendingOperation"], name = "index_shopping_list_items_pendingOperation"),
        Index(value = ["syncState"], name = "index_shopping_list_items_syncState"),
        Index(value = ["serverUpdatedAt"], name = "index_shopping_list_items_serverUpdatedAt"),
        Index(value = ["homeId"], name = "index_shopping_list_items_homeId"),
        Index(value = ["scope"], name = "index_shopping_list_items_scope"),
        Index(value = ["ownerUserId"], name = "index_shopping_list_items_ownerUserId"),
        Index(value = ["isChecked"], name = "index_shopping_list_items_isChecked"),
    ]
)
data class ShoppingListItemEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(defaultValue = "NONE")
    val pendingOperation: PendingOperation = PendingOperation.NONE,

    @ColumnInfo(defaultValue = "OK")
    val syncState: SyncState = SyncState.OK,

    val lastSyncError: String? = null,
    val failedAt: Long? = null,

    @ColumnInfo(name = "updatedAt", defaultValue = "0")
    val localUpdatedAt: Long = System.currentTimeMillis(),

    val serverUpdatedAt: Long? = null,

    val homeId: String = LOCAL_HOME_ID,
    val scope: String,
    val ownerUserId: String? = null,

    val name: String,
    val quantity: String? = null,
    val note: String? = null,
    val isImportant: Boolean = false,
    val isFavorite: Boolean = false,
    val isChecked: Boolean = false,

    @ColumnInfo(defaultValue = "'OTHER'")
    val category: String = "OTHER",

    val createdAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,

    val createdByUserId: String = LOCAL_USER_ID,
    val updatedByUserId: String = LOCAL_USER_ID,
) {
    companion object {
        const val LOCAL_HOME_ID = "local_home"
        const val LOCAL_USER_ID = "local_user"
    }
}