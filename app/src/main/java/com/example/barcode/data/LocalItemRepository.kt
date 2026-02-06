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

    suspend fun updateItem(
        id: String,
        name: String?,
        brand: String?,
        expiry: Long?,
        imageUrl: String?,
        imageIngredientsUrl: String?,
        imageNutritionUrl: String?,
        nutriScore: String?,
    ) {
        val current = dao.getById(id) ?: return

        val nextStatus = when (current.syncStatus) {
            SyncStatus.PENDING_CREATE -> SyncStatus.PENDING_CREATE // ✅ pas encore push => reste "create"
            SyncStatus.PENDING_DELETE -> SyncStatus.PENDING_DELETE // (ne devrait pas arriver via l’UI)
            else -> SyncStatus.PENDING_EDIT // ✅ item déjà existant => update à push
        }

        dao.upsert(
            current.copy(
                name = name,
                brand = brand,
                expiryDate = expiry,
                imageUrl = imageUrl,
                imageIngredientsUrl = imageIngredientsUrl,
                imageNutritionUrl = imageNutritionUrl,
                nutriScore = nutriScore,
                syncStatus = nextStatus,
                localUpdatedAt = System.currentTimeMillis()
            )
        )
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