package com.example.barcode.features.listeCourse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.barcode.data.local.dao.ShoppingListDao
import com.example.barcode.data.local.entities.PendingOperation
import com.example.barcode.data.local.entities.ShoppingListItemEntity
import com.example.barcode.data.local.entities.SyncState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ShoppingListViewModel(
    private val dao: ShoppingListDao,
    private val currentHomeId: String,
    private val currentUserId: String,
) : ViewModel() {

    val items: StateFlow<List<ShoppingListItemUi>> =
        dao.observeVisible(currentHomeId, currentUserId)
            .map { list -> list.map { it.toUi() } }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    fun addCustomItem(
        scope: ShoppingListScope,
        name: String,
        quantity: String?,
        note: String?,
        isImportant: Boolean,
    ) {
        viewModelScope.launch {
            dao.upsert(
                ShoppingListItemEntity(
                    homeId = currentHomeId,
                    scope = scope.name,
                    ownerUserId = if (scope == ShoppingListScope.PERSONAL) currentUserId else null,
                    name = name.trim(),
                    quantity = quantity?.trim()?.takeIf { it.isNotEmpty() },
                    note = note?.trim()?.takeIf { it.isNotEmpty() },
                    isImportant = isImportant,
                    category = ShoppingCategory.OTHER.name,
                    pendingOperation = PendingOperation.CREATE,
                    syncState = SyncState.OK
                )
            )
        }
    }

    fun toggleChecked(id: String) = viewModelScope.launch {
        val local = dao.getById(id) ?: return@launch
        dao.setChecked(id, !local.isChecked)
    }

    fun toggleFavorite(id: String) = viewModelScope.launch {
        val local = dao.getById(id) ?: return@launch
        dao.setFavorite(id, !local.isFavorite)
    }

    fun delete(id: String) = viewModelScope.launch {
        val local = dao.getById(id) ?: return@launch
        if (local.pendingOperation == PendingOperation.CREATE) {
            dao.hardDelete(id)
        } else {
            dao.softDelete(id)
        }
    }

    fun clearChecked(scope: ShoppingListScope) = viewModelScope.launch {
        dao.softDeleteCheckedByScope(scope.name)
    }
}

private fun ShoppingListItemEntity.toUi(): ShoppingListItemUi {
    val safeScope = runCatching { ShoppingListScope.valueOf(scope) }
        .getOrDefault(ShoppingListScope.SHARED)

    val safeCategory = runCatching { ShoppingCategory.valueOf(category) }
        .getOrDefault(ShoppingCategory.OTHER)

    return ShoppingListItemUi(
        id = id,
        homeId = homeId,
        ownerUserId = ownerUserId,
        name = name,
        quantity = quantity,
        category = safeCategory,
        scope = safeScope,
        note = note,
        isImportant = isImportant,
        isFavorite = isFavorite,
        isChecked = isChecked,
        createdByUserId = createdByUserId,
        updatedByUserId = updatedByUserId,
    )
}
