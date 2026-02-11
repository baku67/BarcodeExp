package com.example.barcode.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.barcode.data.local.entities.ItemNoteEntity
import com.example.barcode.data.local.entities.PendingOperation
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemNoteDao {

    @Query("""
        SELECT * FROM item_notes
        WHERE itemId = :itemId
          AND deletedAt IS NULL
        ORDER BY pinned DESC, createdAt DESC
    """)
    fun observeForItem(itemId: String): Flow<List<ItemNoteEntity>>

    @Query("""
        SELECT COUNT(*) FROM item_notes
        WHERE itemId = :itemId
          AND deletedAt IS NULL
    """)
    fun observeCountForItem(itemId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: ItemNoteEntity)

    @Query("SELECT * FROM item_notes WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ItemNoteEntity?

    @Query("""
        UPDATE item_notes
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

    @Query("DELETE FROM item_notes WHERE id = :id")
    suspend fun hardDelete(id: String)

    @Query("""
        SELECT * FROM item_notes
        WHERE pendingOperation = :op
          AND syncState != 'FAILED'
        ORDER BY updatedAt ASC
    """)
    suspend fun getPending(op: PendingOperation): List<ItemNoteEntity>

    @Query("""
        UPDATE item_notes
        SET pendingOperation = 'NONE',
            syncState = 'OK',
            lastSyncError = NULL,
            failedAt = NULL,
            updatedAt = :updatedAt
        WHERE id = :id
    """)
    suspend fun clearPending(id: String, updatedAt: Long = System.currentTimeMillis())

    @Query("""
        UPDATE item_notes
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



    data class ItemNotesCountRow(
        val itemId: String,
        val count: Int
    )

    @Query("""
    SELECT itemId AS itemId, COUNT(*) AS count
    FROM item_notes
    WHERE deletedAt IS NULL
    GROUP BY itemId
    """)
        fun observeCountsByItemId(): Flow<List<ItemNotesCountRow>>

}
