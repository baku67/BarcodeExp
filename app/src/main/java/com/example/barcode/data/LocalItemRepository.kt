package com.example.barcode.data

import com.example.barcode.data.local.dao.ItemDao
import com.example.barcode.data.local.entities.ItemEntity
import com.example.barcode.data.local.entities.SyncStatus
import kotlinx.coroutines.flow.Flow

class LocalItemRepository(private val dao: ItemDao) {

    fun observeItems(): Flow<List<ItemEntity>> = dao.observeAll()

    suspend fun addOrUpdate(itemEntity: ItemEntity) {
        dao.upsert(itemEntity)
    }

    // Surcharge
    suspend fun addOrUpdate(
        name: String,
        brand: String,
        expiry: Long? = null,
        imageUrl: String? = null,
        id: String? = null
    ) {
        val itemEntity = if (id == null) {
            ItemEntity(name = name, brand = brand, expiryDate = expiry, imageUrl = imageUrl)
        } else {
            ItemEntity(id = id, name = name, brand = brand, expiryDate = expiry, imageUrl = imageUrl)
        }
        dao.upsert(itemEntity)
    }

    suspend fun delete(id: String) {
        val item = dao.getById(id) ?: return

        if (item.syncStatus == SyncStatus.PENDING_CREATE) {
            dao.hardDelete(id) // ✅ pas besoin de sync delete
        } else {
            dao.markDeleted(id) // ✅ tombstone + pending delete
        }
    }
}