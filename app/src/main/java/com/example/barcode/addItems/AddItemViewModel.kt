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




    // Pour tester les 4 images candidates récupérées
    fun setImageCandidates(urls: List<String>) = _draft.update { d ->
        val unique = urls.map { it.trim() }.filter { it.isNotBlank() }.distinct()

        // si l'image actuelle existe dans la liste, on garde son index
        val current = d.imageUrl
        val idx = current?.let { unique.indexOf(it) }?.takeIf { it >= 0 } ?: 0
        val resolvedUrl = current ?: unique.getOrNull(idx)

        d.copy(
            imageCandidates = unique,
            imageCandidateIndex = idx,
            imageUrl = resolvedUrl
        )
    }

    fun cycleNextImage() = _draft.update { d ->
        val list = d.imageCandidates
        if (list.size <= 1) return@update d

        val next = (d.imageCandidateIndex + 1) % list.size
        d.copy(
            imageCandidateIndex = next,
            imageUrl = list[next]
        )
    }
}
