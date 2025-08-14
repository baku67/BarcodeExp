package com.example.barcode.ui.components

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.barcode.data.AppDb
import com.example.barcode.data.Item
import com.example.barcode.data.LocalItemRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class ItemsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo by lazy {
        val dao = AppDb.get(app).itemDao()
        LocalItemRepository(dao)
    }

    // La UI collecte ce Flow (Compose: collectAsState)
    val items: Flow<List<Item>> = repo.observeItems()

    fun addItem(name: String, brand: String) =
        viewModelScope.launch {
            repo.addOrUpdate(name, brand)
        }

    fun deleteItem(id: String) = viewModelScope.launch { repo.delete(id) }
}