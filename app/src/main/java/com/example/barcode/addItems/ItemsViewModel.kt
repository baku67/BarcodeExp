package com.example.barcode.addItems

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

    fun addItem(
        name: String,
        brand: String,
        expiry: Long?,
        imageUrl: String?,
        imageIngredientsUrl: String?,
        imageNutritionUrl: String?,
        nutriScore: String?
    ) =
        viewModelScope.launch {
            repo.addOrUpdate(
                Item(
                    name = name,
                    brand = brand,
                    expiryDate = expiry,
                    imageUrl = imageUrl,
                    imageIngredientsUrl = imageIngredientsUrl,
                    imageNutritionUrl = imageNutritionUrl,
                    nutriScore = nutriScore
                )
            )
        }

    fun deleteItem(id: String) = viewModelScope.launch { repo.delete(id) }
}