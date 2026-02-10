package com.example.barcode.data

import com.example.barcode.data.local.dao.ItemNoteDao
import com.example.barcode.data.local.entities.ItemNoteEntity
import com.example.barcode.data.local.entities.PendingOperation
import com.example.barcode.data.local.entities.SyncState
import kotlinx.coroutines.flow.Flow

class LocalItemNoteRepository(
    private val dao: ItemNoteDao
) {
    fun observeNotes(itemId: String): Flow<List<ItemNoteEntity>> = dao.observeForItem(itemId)

    suspend fun addNote(itemId: String, body: String) {
        val trimmed = body.trim()
        if (trimmed.isBlank()) return
        if (trimmed.length > 800) return

        dao.upsert(
            ItemNoteEntity(
                itemId = itemId,
                body = trimmed,
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
}
