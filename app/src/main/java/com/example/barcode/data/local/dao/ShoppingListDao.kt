package com.example.barcode.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.barcode.data.local.entities.ShoppingListItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingListDao {

    @Query(
        """
        SELECT * FROM shopping_list_items
        WHERE homeId = :homeId
          AND (
            scope = 'SHARED'
            OR (scope = 'PERSONAL' AND ownerUserId = :userId)
          )
        ORDER BY isChecked ASC, isImportant DESC, updatedAt DESC
        """
    )
    fun observeVisibleForUser(
        homeId: String,
        userId: String,
    ): Flow<List<ShoppingListItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ShoppingListItemEntity)

    @Query(
        """
        DELETE FROM shopping_list_items
        WHERE id = :id
          AND homeId = :homeId
          AND (
            scope = 'SHARED'
            OR (scope = 'PERSONAL' AND ownerUserId = :userId)
          )
        """
    )
    suspend fun deleteVisibleById(
        id: String,
        homeId: String,
        userId: String,
    )

    @Query(
        """
        DELETE FROM shopping_list_items
        WHERE homeId = :homeId
          AND scope = :scope
          AND isChecked = 1
          AND (
            :scope = 'SHARED'
            OR ownerUserId = :userId
          )
        """
    )
    suspend fun deleteCheckedByScope(
        homeId: String,
        scope: String,
        userId: String,
    )

    @Query(
        """
        UPDATE shopping_list_items
        SET isChecked = NOT isChecked,
            updatedAt = :updatedAt,
            updatedByUserId = :updatedByUserId
        WHERE id = :id
          AND homeId = :homeId
          AND (
            scope = 'SHARED'
            OR (scope = 'PERSONAL' AND ownerUserId = :userId)
          )
        """
    )
    suspend fun toggleChecked(
        id: String,
        homeId: String,
        userId: String,
        updatedByUserId: String,
        updatedAt: Long = System.currentTimeMillis(),
    )

    @Query(
        """
        UPDATE shopping_list_items
        SET isFavorite = NOT isFavorite,
            updatedAt = :updatedAt,
            updatedByUserId = :updatedByUserId
        WHERE id = :id
          AND homeId = :homeId
          AND (
            scope = 'SHARED'
            OR (scope = 'PERSONAL' AND ownerUserId = :userId)
          )
        """
    )
    suspend fun toggleFavorite(
        id: String,
        homeId: String,
        userId: String,
        updatedByUserId: String,
        updatedAt: Long = System.currentTimeMillis(),
    )
}
