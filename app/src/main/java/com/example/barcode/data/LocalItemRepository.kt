package com.example.barcode.data

import kotlinx.coroutines.flow.Flow

class LocalItemRepository(private val dao: ItemDao) {

    fun observeItems(): Flow<List<Item>> = dao.observeAll()

    suspend fun addOrUpdate(name: String, brand: String, id: String? = null) {
        val item = if (id == null) {
            Item(name = name, brand = brand)
        } else {
            Item(id = id, name = name, brand = brand)
        }
        dao.upsert(item)
    }

    suspend fun delete(id: String) = dao.deleteById(id)
}