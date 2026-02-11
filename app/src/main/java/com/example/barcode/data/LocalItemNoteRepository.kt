package com.example.barcode.data

import com.example.barcode.data.local.dao.ItemNoteDao
import com.example.barcode.data.local.entities.ItemNoteEntity
import com.example.barcode.data.local.entities.PendingOperation
import com.example.barcode.data.local.entities.SyncState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LocalItemNoteRepository(
    private val dao: ItemNoteDao
) {
    fun observeNotes(itemId: String): Flow<List<ItemNoteEntity>> = dao.observeForItem(itemId)

    suspend fun addNote(itemId: String, body: String, pinned: Boolean = false) {
        val trimmed = body.trim()
        if (trimmed.isBlank()) return
        if (trimmed.length > 100) return // max char 100

        dao.upsert(
            ItemNoteEntity(
                itemId = itemId,
                body = trimmed,
                pinned = pinned,
                pendingOperation = PendingOperation.CREATE,
                syncState = SyncState.OK
            )
        )
    }


    suspend fun deleteNote(noteId: String) {
        val local = dao.getById(noteId) ?: return

        // ✅ si la note n’a jamais été sync → on la supprime direct local (pas besoin de DELETE serveur)
        if (local.pendingOperation == PendingOperation.CREATE) {
            dao.hardDelete(noteId)
            return
        }

        dao.markDeleted(noteId)
    }



    fun observeCountsMap(): Flow<Map<String, Int>> =
        dao.observeCountsByItemId()
            .map { rows -> rows.associate { it.itemId to it.count } }

}
