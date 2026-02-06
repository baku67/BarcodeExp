package com.example.barcode.data.local.dao

import androidx.room.*
import com.example.barcode.data.local.entities.ItemEntity
import com.example.barcode.data.local.entities.PendingOperation
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {

    // ✅ Vue “normale” : on masque les items supprimés (tombstones)
    @Query("SELECT * FROM items WHERE deletedAt IS NULL ORDER BY expiryDate ASC")
    fun observeAll(): Flow<List<ItemEntity>>

    // (Optionnel) Debug / admin : voir aussi les tombstones
    @Query("SELECT * FROM items ORDER BY expiryDate ASC")
    fun observeAllIncludingDeleted(): Flow<List<ItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(itemEntity: ItemEntity)

    @Query("SELECT * FROM items WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ItemEntity?

    // ✅ Soft delete : tombstone + pending delete + débloque FAILED
    @Query("""
      UPDATE items
      SET deletedAt = :deletedAt,
          pendingOperation = 'DELETE',
          syncState = 'OK',
          lastSyncError = NULL,
          failedAt = NULL,
          updatedAt = :updatedAt
      WHERE id = :id
    """)
    suspend fun markDeleted(
        id: String,
        deletedAt: Long = System.currentTimeMillis(),
        updatedAt: Long = System.currentTimeMillis()
    )

    // ✅ Hard delete : purge définitive (après DELETE serveur OK, ou si jamais sync)
    @Query("DELETE FROM items WHERE id = :id")
    suspend fun hardDelete(id: String)

    // ⚠️ Gardé pour compat si tu as déjà du code qui appelle deleteById()
    @Query("DELETE FROM items WHERE id = :id")
    suspend fun deleteById(id: String)

    // ✅ Items à pousser (FAILED exclus => pas de retry automatique)
    @Query("""
      SELECT * FROM items
      WHERE pendingOperation = :op
        AND syncState != 'FAILED'
      ORDER BY updatedAt ASC
    """)
    suspend fun getPending(op: PendingOperation): List<ItemEntity>

    // ✅ Succès push => reset pending + reset FAILED
    @Query("""
      UPDATE items
      SET pendingOperation = 'NONE',
          syncState = 'OK',
          lastSyncError = NULL,
          failedAt = NULL,
          updatedAt = :updatedAt
      WHERE id = :id
    """)
    suspend fun clearPending(id: String, updatedAt: Long = System.currentTimeMillis())

    // ✅ Échec push => FAILED (bloqué, non retenté)
    @Query("""
      UPDATE items
      SET syncState = 'FAILED',
          lastSyncError = :error,
          failedAt = :failedAt,
          updatedAt = :updatedAt
      WHERE id = :id
    """)
    suspend fun markFailed(
        id: String,
        error: String? = null,
        failedAt: Long = System.currentTimeMillis(),
        updatedAt: Long = System.currentTimeMillis()
    )
}
