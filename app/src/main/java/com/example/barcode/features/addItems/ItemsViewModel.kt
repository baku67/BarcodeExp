package com.example.barcode.features.addItems

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.barcode.core.SessionManager
import com.example.barcode.data.LocalItemNoteRepository
import com.example.barcode.data.local.AppDb
import com.example.barcode.data.local.entities.ItemEntity
import com.example.barcode.data.LocalItemRepository
import com.example.barcode.data.local.entities.ItemNoteEntity
import com.example.barcode.data.local.entities.PendingOperation
import com.example.barcode.sync.SyncScheduler
import com.example.barcode.data.local.entities.SyncState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class ItemsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo by lazy {
        val dao = AppDb.get(app).itemDao()
        LocalItemRepository(dao)
    }

    // La UI collecte ce Flow (Compose: collectAsState)
    val items: Flow<List<ItemEntity>> = repo.observeItems()

    private val session by lazy { SessionManager(app) }

    fun addItem(
        barcode: String?,
        name: String,
        brand: String,
        expiry: Long?,
        imageUrl: String?,
        imageIngredientsUrl: String?,
        imageNutritionUrl: String?,
        nutriScore: String?,
        addMode: String,
    ) = viewModelScope.launch {

        // 1) Toujours écrire en local
        val entity = ItemEntity(
            barcode = barcode,
            name = name,
            brand = brand,
            expiryDate = expiry,
            imageUrl = imageUrl,
            imageIngredientsUrl = imageIngredientsUrl,
            imageNutritionUrl = imageNutritionUrl,
            nutriScore = nutriScore,
            addMode = addMode,
            pendingOperation = PendingOperation.CREATE,
            syncState = SyncState.OK
        )

        repo.addOrUpdate(entity)

        // 2) Si connecté (AUTH + token), déclenche la sync distante en background
        if (session.isAuthenticated()) {
            SyncScheduler.enqueueSync(getApplication())
        }
    }


    fun updateItem(
        id: String,
        name: String?,
        brand: String?,
        expiry: Long?,
        imageUrl: String?,
        imageIngredientsUrl: String?,
        imageNutritionUrl: String?,
        nutriScore: String?,
    ) = viewModelScope.launch {

        repo.updateItem(
            id = id,
            name = name,
            brand = brand,
            expiry = expiry,
            imageUrl = imageUrl,
            imageIngredientsUrl = imageIngredientsUrl,
            imageNutritionUrl = imageNutritionUrl,
            nutriScore = nutriScore
        )

        if (session.isAuthenticated()) {
            SyncScheduler.enqueueSync(getApplication())
        }
    }


    fun deleteItem(id: String) = viewModelScope.launch {
        repo.delete(id) // ✅ devient un soft delete

        // ✅ Si authentifié, on push la suppression
        if (session.isAuthenticated()) {
            SyncScheduler.enqueueSync(getApplication())
        }
    }



    // ITEM NOTES
    private val notesRepo by lazy {
        val dao = AppDb.get(app).itemNoteDao()
        LocalItemNoteRepository(dao)
    }

    fun observeNotes(itemId: String): Flow<List<ItemNoteEntity>> =
        notesRepo.observeNotes(itemId)

    fun addNote(itemId: String, body: String) = viewModelScope.launch {
        notesRepo.addNote(itemId, body)
        if (session.isAuthenticated()) {
            SyncScheduler.enqueueSync(getApplication())
        }
    }

    fun deleteNote(noteId: String) = viewModelScope.launch {
        notesRepo.deleteNote(noteId)
        if (session.isAuthenticated()) {
            SyncScheduler.enqueueSync(getApplication())
        }
    }
}