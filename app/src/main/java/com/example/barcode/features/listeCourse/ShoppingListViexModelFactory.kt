package com.example.barcode.features.listeCourse

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.barcode.data.local.dao.ShoppingListDao

class ShoppingListViewModelFactory(
    private val app: Application,
    private val dao: ShoppingListDao,
    private val currentHomeId: String,
    private val currentUserId: String,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ShoppingListViewModel::class.java)) {
            return ShoppingListViewModel(
                app = app,
                dao = dao,
                currentHomeId = currentHomeId,
                currentUserId = currentUserId
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
