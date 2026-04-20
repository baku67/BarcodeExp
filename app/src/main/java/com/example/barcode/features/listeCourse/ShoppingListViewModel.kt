package com.example.barcode.features.listeCourse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.barcode.data.local.dao.ShoppingListDao
import com.example.barcode.data.local.entities.ShoppingListItemEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ShoppingListViewModel(
    private val dao: ShoppingListDao
) : ViewModel() {

    private val currentHomeId = ShoppingListItemEntity.LOCAL_HOME_ID
    private val currentUserId = ShoppingListItemEntity.LOCAL_USER_ID

    val items: StateFlow<List<ShoppingListItemUi>> =
        dao.observeVisibleForUser(currentHomeId, currentUserId)
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
        val now = System.currentTimeMillis()
        val normalizedName = name.trim()
        val normalizedQuantity = quantity?.trim()?.takeIf { it.isNotEmpty() }
        val normalizedNote = note?.trim()?.takeIf { it.isNotEmpty() }
        val ownerUserId = if (scope == ShoppingListScope.PERSONAL) currentUserId else null

        viewModelScope.launch {
            dao.upsert(
                ShoppingListItemEntity(
                    homeId = currentHomeId,
                    scope = scope.name,
                    ownerUserId = ownerUserId,
                    name = normalizedName,
                    quantity = normalizedQuantity,
                    note = normalizedNote,
                    isImportant = isImportant,
                    category = ShoppingCategory.OTHER.name,
                    createdAt = now,
                    updatedAt = now,
                    createdByUserId = currentUserId,
                    updatedByUserId = currentUserId,
                )
            )
        }
    }

    fun toggleChecked(id: String) = viewModelScope.launch {
        dao.toggleChecked(
            id = id,
            homeId = currentHomeId,
            userId = currentUserId,
            updatedByUserId = currentUserId,
        )
    }

    fun toggleFavorite(id: String) = viewModelScope.launch {
        dao.toggleFavorite(
            id = id,
            homeId = currentHomeId,
            userId = currentUserId,
            updatedByUserId = currentUserId,
        )
    }

    fun delete(id: String) = viewModelScope.launch {
        dao.deleteVisibleById(
            id = id,
            homeId = currentHomeId,
            userId = currentUserId,
        )
    }

    fun clearChecked(scope: ShoppingListScope) = viewModelScope.launch {
        dao.deleteCheckedByScope(
            homeId = currentHomeId,
            scope = scope.name,
            userId = currentUserId,
        )
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
