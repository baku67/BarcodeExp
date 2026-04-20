package com.example.barcode.data.local.dao;

import androidx.room.*;
import com.example.barcode.data.local.entities.ShoppingListItemEntity;
import kotlinx.coroutines.flow.Flow;

@Dao
interface ShoppingListDao {

    @Query("""
        SELECT * FROM shopping_list_items
        ORDER BY isChecked ASC, isImportant DESC, updatedAt DESC
    """)
    fun observeAll(): Flow<List<ShoppingListItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ShoppingListItemEntity)

    @Query("DELETE FROM shopping_list_items WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM shopping_list_items WHERE scope = :scope AND isChecked = 1")
    suspend fun deleteCheckedByScope(scope: String)

    @Query("UPDATE shopping_list_items SET isChecked = NOT isChecked, updatedAt = :updatedAt WHERE id = :id")
    suspend fun toggleChecked(id: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE shopping_list_items SET isFavorite = NOT isFavorite, updatedAt = :updatedAt WHERE id = :id")
    suspend fun toggleFavorite(id: String, updatedAt: Long = System.currentTimeMillis())
}