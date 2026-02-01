package com.example.barcode.features.addItems

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.barcode.data.local.AppDb
import com.example.barcode.data.local.entities.ItemEntity
import com.example.barcode.data.LocalItemRepository
import com.example.barcode.data.local.entities.SyncStatus
import com.example.barcode.sync.SyncScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class ItemsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo by lazy {
        val dao = AppDb.get(app).itemDao()
        LocalItemRepository(dao)
    }

    // La UI collecte ce Flow (Compose: collectAsState)
    val items: Flow<List<ItemEntity>> = repo.observeItems()

    private val session by lazy { com.example.barcode.core.session.SessionManager(app) }

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
            syncStatus = SyncStatus.PENDING_CREATE
        )

        repo.addOrUpdate(entity)

        // 2) Si connecté (AUTH + token), déclenche la sync distante en background
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
}