package com.example.barcode.data.local.dao

import androidx.room.*
import com.example.barcode.data.local.entities.ItemEntity
import com.example.barcode.data.local.entities.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {

    @Query("SELECT * FROM items ORDER BY expiryDate ASC")
    fun observeAll(): Flow<List<ItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(itemEntity: ItemEntity)

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