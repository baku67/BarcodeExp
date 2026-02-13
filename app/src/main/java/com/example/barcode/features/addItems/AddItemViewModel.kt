package com.example.barcode.features.addItems

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class AddItemViewModel(
    private val savedState: SavedStateHandle
) : ViewModel() {

    private val _draft = MutableStateFlow(
        savedState.get<AddItemDraft>("draft")
            ?: AddItemDraft(photoId = UUID.randomUUID().toString())
    )

    val draft = _draft.asStateFlow()

    init {
        if (_draft.value.photoId.isNullOrBlank()) {
            _draft.update { it.copy(photoId = UUID.randomUUID().toString()) }
        }

        viewModelScope.launch {
            draft.collect { savedState["draft"] = it }
        }
    }

    // *** Propriétés (scan / commun)
    fun setBarcode(code: String) = _draft.update { it.copy(barcode = code) }
    fun setDetails(name: String?, brand: String?) = _draft.update { it.copy(name = name, brand = brand) }
    fun setExpiryDate(ts: Long?) = _draft.update { it.copy(expiryDate = ts) }
    fun setImage(url: String?) = _draft.update { it.copy(imageUrl = url) }
    fun setIngredientsImage(url: String?) = _draft.update { it.copy(imageIngredientsUrl = url) }
    fun setNutritionImage(url: String?) = _draft.update { it.copy(imageNutritionUrl = url) }
    fun setNutriScore(v: String?) = _draft.update { it.copy(nutriScore = v) }
    fun setAddMode(mode: ItemAddMode) = _draft.update { it.copy(addMode = mode) }

    // *** Propriétés ajout manuel (JSON = source de vérité)
    fun setManualType(typeCode: String) {
        _draft.update {
            // ✅ si on change le type, on reset le subtype (sinon incohérent)
            it.copy(manualTypeCode = typeCode, manualSubtypeCode = null)
        }
    }

    fun setManualSubtype(subtypeCode: String) {
        _draft.update { d ->
            // garde-fou simple : pas de subtype si aucun type choisi
            if (d.manualTypeCode.isNullOrBlank()) d
            else d.copy(manualSubtypeCode = subtypeCode)
        }
    }

    fun reset() {
        _draft.value = AddItemDraft(photoId = UUID.randomUUID().toString())
    }

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
