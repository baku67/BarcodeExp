package com.example.barcode.data.local.dao

import androidx.room.*
import com.example.barcode.data.local.entities.ItemEntity
import com.example.barcode.data.local.entities.SyncStatus
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

    // ✅ Soft delete : on garde une trace locale pour sync + éviter la résurrection au prochain PULL
    @Query("""
        UPDATE items
        SET deletedAt = :deletedAt,
            syncStatus = :status,
            updatedAt = :updatedAt
        WHERE id = :id
    """)
    suspend fun markDeleted(
        id: String,
        deletedAt: Long = System.currentTimeMillis(),
        status: SyncStatus = SyncStatus.PENDING_DELETE,
        updatedAt: Long = System.currentTimeMillis()
    )

    // ✅ Hard delete : purge définitive (après DELETE serveur OK, ou si jamais sync)
    @Query("DELETE FROM items WHERE id = :id")
    suspend fun hardDelete(id: String)

    // ⚠️ Gardé pour compat si tu as déjà du code qui appelle deleteById()
    @Query("DELETE FROM items WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM items WHERE syncStatus = :status")
    suspend fun getBySyncStatus(status: SyncStatus): List<ItemEntity>

    @Query("UPDATE items SET syncStatus = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateSyncStatus(
        id: String,
        status: SyncStatus,
        updatedAt: Long = System.currentTimeMillis()
    )
}