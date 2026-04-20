package com.example.barcode.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.barcode.features.listeCourse.ShoppingCategory
import java.util.UUID

@Entity(tableName = "shopping_list_items")
data class ShoppingListItemEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val homeId: String,
    val scope: String,
    val ownerUserId: String?,
    val name: String,
    val quantity: String? = null,
    val note: String? = null,
    val isImportant: Boolean = false,
    val isFavorite: Boolean = false,
    val isChecked: Boolean = false,
    val category: String = ShoppingCategory.OTHER.name,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val createdByUserId: String,
    val updatedByUserId: String,
) {
    companion object {
        const val LOCAL_HOME_ID = "local_home"
        const val LOCAL_USER_ID = "local_user"
    }
}
