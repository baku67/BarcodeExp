package com.example.barcode.addItems

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AddItemViewModel(
    private val savedState: SavedStateHandle
) : ViewModel() {

    private val _draft = MutableStateFlow(savedState.get<AddItemDraft>("draft") ?: AddItemDraft())
    val draft = _draft.asStateFlow()

    init {
        viewModelScope.launch {
            draft.collect { savedState["draft"] = it }
        }
    }

    fun setBarcode(code: String) = _draft.update { it.copy(barcode = code) }
    fun setDetails(name: String?, brand: String?) = _draft.update { it.copy(name = name, brand = brand) }
    fun setExpiryDate(ts: Long?) = _draft.update { it.copy(expiryDate = ts) }
    fun setImage(url: String?) = _draft.update { it.copy(imageUrl = url) }
    fun reset() { _draft.value = AddItemDraft() }
}
