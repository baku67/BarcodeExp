package com.example.barcode.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.barcode.data.local.entities.PendingOperation
import com.example.barcode.data.local.entities.ShoppingListItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingListDao {

    @Query("""
        SELECT * FROM shopping_list_items
        WHERE deletedAt IS NULL
          AND homeId = :homeId
          AND (
              scope = 'SHARED'
              OR (scope = 'PERSONAL' AND ownerUserId = :userId)
          )
        ORDER BY isChecked ASC, isImportant DESC, updatedAt DESC
    """)
    fun observeVisible(homeId: String, userId: String): Flow<List<ShoppingListItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ShoppingListItemEntity)

    @Query("SELECT * FROM shopping_list_items WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ShoppingListItemEntity?

    @Query("""
        SELECT * FROM shopping_list_items
        WHERE pendingOperation = :pending
        ORDER BY updatedAt ASC
    """)
    suspend fun getPending(pending: PendingOperation): List<ShoppingListItemEntity>

    @Query("""
        UPDATE shopping_list_items
        SET pendingOperation = 'NONE',
            syncState = 'OK',
            lastSyncError = NULL,
            failedAt = NULL
        WHERE id = :id
    """)
    suspend fun clearPending(id: String)

    @Query("""
        UPDATE shopping_list_items
        SET syncState = 'FAILED',
            lastSyncError = :error,
            failedAt = :failedAt
        WHERE id = :id
    """)
    suspend fun markFailed(
        id: String,
        error: String,
        failedAt: Long = System.currentTimeMillis()
    )

    @Query("DELETE FROM shopping_list_items WHERE id = :id")
    suspend fun hardDelete(id: String)

    @Query("""
        UPDATE shopping_list_items
        SET isChecked = :checked,
            updatedAt = :updatedAt,
            pendingOperation = CASE
                WHEN pendingOperation = 'CREATE' THEN 'CREATE'
                ELSE 'UPDATE'
            END,
            syncState = 'OK',
            lastSyncError = NULL,
            failedAt = NULL
        WHERE id = :id
    """)
    suspend fun setChecked(
        id: String,
        checked: Boolean,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("""
        UPDATE shopping_list_items
        SET isFavorite = :favorite,
            updatedAt = :updatedAt,
            pendingOperation = CASE
                WHEN pendingOperation = 'CREATE' THEN 'CREATE'
                ELSE 'UPDATE'
            END,
            syncState = 'OK',
            lastSyncError = NULL,
            failedAt = NULL
        WHERE id = :id
    """)
    suspend fun setFavorite(
        id: String,
        favorite: Boolean,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("""
        UPDATE shopping_list_items
        SET deletedAt = :deletedAt,
            updatedAt = :deletedAt,
            pendingOperation = CASE
                WHEN pendingOperation = 'CREATE' THEN 'DELETE'
                ELSE 'DELETE'
            END,
            syncState = 'OK',
            lastSyncError = NULL,
            failedAt = NULL
        WHERE id = :id
    """)
    suspend fun softDelete(
        id: String,
        deletedAt: Long = System.currentTimeMillis()
    )

    @Query("""
        UPDATE shopping_list_items
        SET deletedAt = :deletedAt,
            updatedAt = :deletedAt,
            pendingOperation = CASE
                WHEN pendingOperation = 'CREATE' THEN 'DELETE'
                ELSE 'DELETE'
            END,
            syncState = 'OK',
            lastSyncError = NULL,
            failedAt = NULL
        WHERE scope = :scope
          AND isChecked = 1
          AND deletedAt IS NULL
    """)
    suspend fun softDeleteCheckedByScope(
        scope: String,
        deletedAt: Long = System.currentTimeMillis()
    )
}
