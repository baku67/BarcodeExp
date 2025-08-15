package com.example.barcode.data

import kotlinx.coroutines.flow.Flow

class LocalItemRepository(private val dao: ItemDao) {

    fun observeItems(): Flow<List<Item>> = dao.observeAll()

    suspend fun addOrUpdate(item: Item) {
        dao.upsert(item)
    }

    // Surcharge
    suspend fun addOrUpdate(
        name: String,
        brand: String,
        expiry: Long? = null,
        imageUrl: String? = null,
        id: String? = null
    ) {
        val item = if (id == null) {
            Item(name = name, brand = brand, expiryDate = expiry, imageUrl = imageUrl)
        } else {
            Item(id = id, name = name, brand = brand, expiryDate = expiry, imageUrl = imageUrl)
        }
        dao.upsert(item)
    }

    suspend fun delete(id: String) = dao.deleteById(id)
}