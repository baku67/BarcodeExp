package com.example.barcode.data

import com.example.barcode.data.local.dao.ItemDao
import com.example.barcode.data.local.entities.ItemEntity
import com.example.barcode.data.local.entities.PendingOperation
import com.example.barcode.data.local.entities.SyncState
import kotlinx.coroutines.flow.Flow

class LocalItemRepository(private val dao: ItemDao) {

    fun observeItems(): Flow<List<ItemEntity>> = dao.observeAll()

    suspend fun addOrUpdate(itemEntity: ItemEntity) {
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

        val nextOp = when (current.pendingOperation) {
                PendingOperation.CREATE -> PendingOperation.CREATE // ✅ pas encore push => reste CREATE
                PendingOperation.DELETE -> PendingOperation.DELETE // (ne devrait pas arriver via l’UI)
                else -> PendingOperation.UPDATE // ✅ item déjà existant => UPDATE à push
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
                pendingOperation = nextOp,
                syncState = SyncState.OK,    // ✅ action utilisateur => débloque un éventuel FAILED
                lastSyncError = null,
                failedAt = null,
                localUpdatedAt = System.currentTimeMillis()
            )
        )
    }


    suspend fun delete(id: String) {
        val item = dao.getById(id) ?: return

        if (item.pendingOperation == PendingOperation.CREATE) {
            dao.hardDelete(id) // ✅ pas besoin de sync delete
        } else {
            dao.markDeleted(id) // ✅ tombstone + pending DELETE + débloque FAILED (si ton DAO le fait)
        }
    }
}