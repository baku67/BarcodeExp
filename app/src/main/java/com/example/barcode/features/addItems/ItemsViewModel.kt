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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ItemsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo by lazy {
        val dao = AppDb.get(app).itemDao()
        LocalItemRepository(dao)
    }

    // La UI collecte ce Flow (Compose: collectAsState)
    val items: Flow<List<ItemEntity>> = repo.observeItems()

    private val session by lazy { SessionManager(app) }

    private val notesRepo by lazy {
        val dao = AppDb.get(app).itemNoteDao()
        LocalItemNoteRepository(dao)
    }

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
        photoId: String? = null,
    ) = viewModelScope.launch {

        // 1) Toujours écrire en local
        val itemId = java.util.UUID.randomUUID().toString()
        val resolvedPhotoId = photoId ?: itemId

        val entity = ItemEntity(
            id = itemId,
            photoId = resolvedPhotoId,

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



    // ✅ 1 seule source de vérité pour les badges (map itemId -> count)
    val notesCountByItemId: StateFlow<Map<String, Int>> =
        notesRepo.observeCountsMap()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun observeNotes(itemId: String): Flow<List<ItemNoteEntity>> =
        notesRepo.observeNotes(itemId)

    fun addNote(itemId: String, body: String, pinned: Boolean = false) = viewModelScope.launch {
        notesRepo.addNote(itemId, body, pinned)
        if (session.isAuthenticated()) SyncScheduler.enqueueSync(getApplication())
    }

    fun deleteNote(noteId: String) = viewModelScope.launch {
        notesRepo.deleteNote(noteId)
        if (session.isAuthenticated()) SyncScheduler.enqueueSync(getApplication())
    }


    fun addItemFromDraft(d: AddItemDraft) = viewModelScope.launch {
        val name = requireNotNull(d.name) { "name requis" }

        val itemId = java.util.UUID.randomUUID().toString()
        val resolvedPhotoId = d.photoId ?: itemId  // ✅ photoId commun scan/manual

        val entity = ItemEntity(
            id = itemId,
            photoId = resolvedPhotoId,

            name = name,
            expiryDate = d.expiryDate,
            addMode = d.addMode.value,

            // scan uniquement
            barcode = if (d.addMode == ItemAddMode.BARCODE_SCAN) d.barcode else null,
            brand = if (d.addMode == ItemAddMode.BARCODE_SCAN) d.brand else null,
            imageUrl = if (d.addMode == ItemAddMode.BARCODE_SCAN) d.imageUrl else null,
            imageIngredientsUrl = if (d.addMode == ItemAddMode.BARCODE_SCAN) d.imageIngredientsUrl else null,
            imageNutritionUrl = if (d.addMode == ItemAddMode.BARCODE_SCAN) d.imageNutritionUrl else null,
            nutriScore = if (d.addMode == ItemAddMode.BARCODE_SCAN) d.nutriScore else null,

            // manual uniquement
            manualType = if (d.addMode == ItemAddMode.MANUAL) d.manualType?.name else null,
            manualSubtype = if (d.addMode == ItemAddMode.MANUAL) d.manualSubtype?.name else null,

            pendingOperation = PendingOperation.CREATE,
            syncState = SyncState.OK
        )

        repo.addOrUpdate(entity)

        if (session.isAuthenticated()) {
            SyncScheduler.enqueueSync(getApplication())
        }
    }

}