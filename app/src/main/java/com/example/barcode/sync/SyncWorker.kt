package com.example.barcode.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.barcode.core.network.ApiClient
import com.example.barcode.core.session.SessionManager
import com.example.barcode.data.local.AppDb
import com.example.barcode.data.local.entities.ItemEntity
import com.example.barcode.data.local.entities.PendingOperation
import com.example.barcode.data.local.entities.SyncState
import com.example.barcode.features.addItems.data.remote.api.ItemsApi
import com.example.barcode.features.addItems.data.remote.dto.ItemCreateDto
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class SyncWorker(
    ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val session = SessionManager(applicationContext)

        // Pas connecté => rien à faire
        if (!session.isAuthenticated()) return Result.success()

        val token = session.token.first().orEmpty()
        if (token.isBlank()) return Result.success()

        val dao = AppDb.get(applicationContext).itemDao()
        val api = ApiClient.createApi(ItemsApi::class.java)


        // 1) PUSH DELETE : envoyer les suppressions en attente
        // ✅ getPending() exclut les FAILED => pas de retry automatique
        val pendingDeletes = dao.getPending(PendingOperation.DELETE)

        // TODO: Batch plutot que 1 requete par Item ?
        pendingDeletes.forEach { item ->
            try {
                val res = api.deleteItemByClientId(
                    authorization = "Bearer $token",
                    clientId = item.id
                )

                // ✅ 204 = OK, ✅ 404 = déjà supprimé côté serveur => on purge local pareil
                if (res.isSuccessful || res.code() == 404) {
                    dao.hardDelete(item.id) // ✅ on nettoie la DB locale
                } else {
                    dao.markFailed(item.id, error = "delete failed code=${res.code()}")
                }
            } catch (e: Exception) {
                dao.markFailed(item.id, error = "delete exception=${e.message}")
            }
        }


        // 2) PUSH CREATE: envoyer les items en attente
        val pendingCreates = dao.getPending(PendingOperation.CREATE)

        // TODO: Batch plutot que 1 requete par Item ?
        pendingCreates.forEach { item ->
            try {
                val dto = ItemCreateDto(
                    clientId = item.id,
                    barcode = item.barcode,
                    name = item.name,
                    brand = item.brand,
                    imageUrl = item.imageUrl,
                    imageIngredientsUrl = item.imageIngredientsUrl,
                    imageNutritionUrl = item.imageNutritionUrl,
                    nutriScore = sanitizeNutriScore(item.nutriScore),
                    expiryDate = item.expiryDate?.let { epochMsToYyyyMmDd(it) },
                    addMode = item.addMode,
                )

                val res = api.createItem(
                    authorization = "Bearer $token",
                    body = dto
                )

                // DEBUG si error (TODO remove)
                if (!res.isSuccessful) {
                    val err = res.errorBody()?.string()
                    android.util.Log.e("SyncWorker", "422 body=$err")
                }

                if (res.isSuccessful) dao.clearPending(item.id)
                else dao.markFailed(item.id, error = "create failed code=${res.code()}")

            } catch (e: Exception) {
                dao.markFailed(item.id, error = "create exception=${e.message}")
                // On continue les autres; tu pourras plus tard faire Result.retry()
            }
        }


        // 3) PUSH UPDATE : envoyer les edits en attente
        // ⚠️ Ici on réutilise createItem comme "upsert" (clientId stable).
        // Si ton backend a un vrai PATCH/PUT, remplace par api.updateItem(...)
        val pendingUpdates = dao.getPending(PendingOperation.UPDATE)
        pendingUpdates.forEach { item ->
                try {
                        val dto = ItemCreateDto(
                                clientId = item.id,
                                barcode = item.barcode,
                                name = item.name,
                                brand = item.brand,
                                imageUrl = item.imageUrl,
                                imageIngredientsUrl = item.imageIngredientsUrl,
                                imageNutritionUrl = item.imageNutritionUrl,
                                nutriScore = sanitizeNutriScore(item.nutriScore),
                                expiryDate = item.expiryDate?.let { epochMsToYyyyMmDd(it) },
                                addMode = item.addMode,
                            )

                        val res = api.createItem(
                                authorization = "Bearer $token",
                                body = dto
                                    )

                        if (res.isSuccessful) dao.clearPending(item.id)
                        else dao.markFailed(item.id, error = "update(upsert) failed code=${res.code()}")

                    } catch (e: Exception) {
                        dao.markFailed(item.id, error = "update(upsert) exception=${e.message}")
                    }
            }


        // 4) PULL : récupérer l'état serveur et upsert local (Room) par clientId
        try {
            val pull = api.getItems("Bearer $token")
            if (!pull.isSuccessful) {
                val err = pull.errorBody()?.string()
                android.util.Log.e("SyncWorker", "pull failed code=${pull.code()} body=$err")
                return Result.success()
            }

            val remoteItems = pull.body().orEmpty()

            remoteItems.forEach { dto ->
                val clientId = dto.clientId ?: return@forEach

                // ✅ Ne pas écraser un item local qui a des changements non push (pending)
                // ou un item bloqué FAILED (pas de retry auto)
                val local = dao.getById(clientId)
                if (local != null && (local.pendingOperation != PendingOperation.NONE || local.syncState == SyncState.FAILED)) {
                    return@forEach
                }

                val serverUpdatedAt = dto.updatedAt.let(::parseAtomToEpochMs)

                val serverDeletedAt = dto.deletedAt?.let { parseAtomToEpochMs(it) }
                    ?: if (dto.isDeleted) serverUpdatedAt else null // fallback safe

                val entity = ItemEntity(
                    id = clientId,
                    barcode = dto.barcode,
                    name = dto.name,
                    brand = dto.brand,
                    imageUrl = dto.imageUrl,
                    imageIngredientsUrl = dto.imageIngredientsUrl,
                    imageNutritionUrl = dto.imageNutritionUrl,
                    nutriScore = dto.nutriScore,
                    addedAt = dto.addedAt?.let { parseAtomToEpochMs(it) },
                    expiryDate = dto.expiryDate?.let { parseYyyyMmDdToEpochMs(it) },
                    addMode = dto.addMode ?: "barcode_scan",
                    pendingOperation = PendingOperation.NONE,
                    syncState = SyncState.OK,
                    lastSyncError = null,
                    failedAt = null,
                    // ✅ timestamp local (debug / tri / etc)
                    localUpdatedAt = System.currentTimeMillis(),
                    // ✅ timestamp serveur (delta sync / merge)
                    serverUpdatedAt = serverUpdatedAt,
                    // ✅ tombstone
                    deletedAt = serverDeletedAt
                )

                dao.upsert(entity)
            }

        } catch (e: Exception) {
            // Pull qui échoue : on ne casse pas tout, on garde juste le local
            android.util.Log.e("SyncWorker", "pull exception=${e.message}")
        }


        return Result.success()
    }



    fun sanitizeNutriScore(raw: String?): String? {
        val v = raw?.trim()?.uppercase(Locale.ROOT) ?: return null
        return if (v in setOf("A","B","C","D","E")) v else null
    }

    private val yyyyMMdd = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private fun epochMsToYyyyMmDd(ms: Long): String =
        Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate().format(yyyyMMdd)

    private fun parseAtomToEpochMs(s: String): Long =
        java.time.OffsetDateTime.parse(s).toInstant().toEpochMilli()


    private fun parseYyyyMmDdToEpochMs(s: String): Long {
        val d = java.time.LocalDate.parse(s) // yyyy-MM-dd
        return d.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
