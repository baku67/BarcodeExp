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

    val items: StateFlow<List<ShoppingListItemUi>> =
        dao.observeAll()
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
                    name = name.trim(),
                    quantity = quantity?.trim()?.takeIf { it.isNotEmpty() },
                    note = note?.trim()?.takeIf { it.isNotEmpty() },
                    isImportant = isImportant,
                    scope = scope.name,
                    category = ShoppingCategory.OTHER.name,
                )
            )
        }
    }

    fun toggleChecked(id: String) = viewModelScope.launch { dao.toggleChecked(id) }
    fun toggleFavorite(id: String) = viewModelScope.launch { dao.toggleFavorite(id) }
    fun delete(id: String) = viewModelScope.launch { dao.deleteById(id) }
    fun clearChecked(scope: ShoppingListScope) =
        viewModelScope.launch { dao.deleteCheckedByScope(scope.name) }
}

private fun ShoppingListItemEntity.toUi(): ShoppingListItemUi {
    return ShoppingListItemUi(
        id = id,
        name = name,
        quantity = quantity,
        category = ShoppingCategory.valueOf(category),
        scope = ShoppingListScope.valueOf(scope),
        note = note,
        isImportant = isImportant,
        isFavorite = isFavorite,
        isChecked = isChecked,
    )
}