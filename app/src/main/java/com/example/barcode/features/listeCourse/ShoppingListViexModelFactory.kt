package com.example.barcode.features.listeCourse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.barcode.data.local.dao.ShoppingListDao

class ShoppingListViewModelFactory(
    private val dao: ShoppingListDao
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ShoppingListViewModel::class.java)) {
            return ShoppingListViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
