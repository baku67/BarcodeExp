package com.example.barcode.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {

    @Query("SELECT * FROM items ORDER BY expiryDate ASC")
    fun observeAll(): Flow<List<Item>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: Item)

    @Query("DELETE FROM items WHERE id = :id")
    suspend fun deleteById(id: String)
}