package com.example.barcode.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.barcode.core.network.ApiClient
import com.example.barcode.core.SessionManager
import com.example.barcode.data.local.AppDb
import com.example.barcode.data.local.entities.ItemEntity
import com.example.barcode.data.local.entities.PendingOperation
import com.example.barcode.data.local.entities.SyncState
import com.example.barcode.features.addItems.data.remote.api.ItemsApi
import com.example.barcode.features.addItems.data.remote.dto.ItemCreateDto
import com.example.barcode.util.sanitizeNutriScore
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.max


class SyncWorker(
    ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val session = SessionManager(applicationContext)

        if (!session.isAuthenticated()) return Result.success()

        val token = session.token.first().orEmpty()
        if (token.isBlank()) return Result.success()

        val dao = AppDb.get(applicationContext).itemDao()
        val api = ApiClient.createApi(ItemsApi::class.java)
        val prefs = SyncPreferences(applicationContext)

        // ✅ watermark serveur stocké en local (epoch ms)
        val sinceMs = prefs.lastSuccessAt.first()
        val sinceIso = DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(sinceMs))

        var newWatermarkMs = sinceMs

        // 1) PUSH DELETE
        val pendingDeletes = dao.getPending(PendingOperation.DELETE)
        pendingDeletes.forEach { item ->
            try {
                val res = api.deleteItemByClientId(
                    authorization = "Bearer $token",
                    clientId = item.id
                )

                if (res.isSuccessful || res.code() == 404) {
                    dao.hardDelete(item.id)
                } else {
                    dao.markFailed(item.id, error = "delete failed code=${res.code()}")
                }
            } catch (e: Exception) {
                dao.markFailed(item.id, error = "delete exception=${e.message}")
            }
        }

        // 2) PUSH CREATE
        val pendingCreates = dao.getPending(PendingOperation.CREATE)
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

                if (res.isSuccessful) dao.clearPending(item.id)
                else dao.markFailed(item.id, error = "create failed code=${res.code()}")

            } catch (e: Exception) {
                dao.markFailed(item.id, error = "create exception=${e.message}")
            }
        }

        // 3) PUSH UPDATE (upsert)
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

        // 4) PULL UPSERTS (items actifs)
        try {
            val pullUpserts = api.getItems(
                authorization = "Bearer $token",
                updatedSince = sinceIso
            )

            if (pullUpserts.code() == 401) {
                prefs.markAuthRequired()
                return Result.success()
            }

            if (!pullUpserts.isSuccessful) {
                val err = pullUpserts.errorBody()?.string()
                android.util.Log.e("SyncWorker", "pull upserts failed code=${pullUpserts.code()} body=$err")
                return Result.success()
            }

            val remoteItems = pullUpserts.body().orEmpty()

            remoteItems.forEach { dto ->
                val clientId = dto.clientId

                val local = dao.getById(clientId)
                if (local != null && (local.pendingOperation != PendingOperation.NONE || local.syncState == SyncState.FAILED)) {
                    return@forEach
                }

                val serverUpdatedAt = parseAtomToEpochMs(dto.updatedAt)
                newWatermarkMs = max(newWatermarkMs, serverUpdatedAt)

                // Ici on ne s'attend pas à des deleted, mais on garde ton parsing safe
                val serverDeletedAt = dto.deletedAt?.let { parseAtomToEpochMs(it) }
                    ?: if (dto.isDeleted) serverUpdatedAt else null
                if (serverDeletedAt != null) newWatermarkMs = max(newWatermarkMs, serverDeletedAt)

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
                    localUpdatedAt = System.currentTimeMillis(),
                    serverUpdatedAt = serverUpdatedAt,
                    deletedAt = serverDeletedAt
                )

                dao.upsert(entity)
            }

        } catch (e: Exception) {
            android.util.Log.e("SyncWorker", "pull upserts exception=${e.message}")
            return Result.success()
        }

        // 5) PULL DELETES (tombstones)
        try {
            val pullDeletes = api.getDeletedItems(
                authorization = "Bearer $token",
                since = sinceIso
            )

            if (pullDeletes.code() == 401) {
                prefs.markAuthRequired()
                return Result.success()
            }

            if (!pullDeletes.isSuccessful) {
                val err = pullDeletes.errorBody()?.string()
                android.util.Log.e("SyncWorker", "pull deletes failed code=${pullDeletes.code()} body=$err")
                return Result.success()
            }

            val deleted = pullDeletes.body().orEmpty()
            deleted.forEach { t ->
                val clientId = t.clientId
                val deletedAtMs = parseAtomToEpochMs(t.deletedAt)
                newWatermarkMs = max(newWatermarkMs, deletedAtMs)

                // ✅ ne pas écraser les items locaux qui ont un pending/FAILED
                val local = dao.getById(clientId)
                if (local != null && (local.pendingOperation != PendingOperation.NONE || local.syncState == SyncState.FAILED)) {
                    return@forEach
                }

                // ✅ choix simple: purge locale (comme après un delete push)
                dao.hardDelete(clientId)
            }

        } catch (e: Exception) {
            android.util.Log.e("SyncWorker", "pull deletes exception=${e.message}")
            return Result.success()
        }

        // ✅ Sync complète OK => on stocke un watermark serveur (pas now())
        prefs.markSyncSuccessAt(newWatermarkMs)

        return Result.success()
    }

    private val yyyyMMdd = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private fun epochMsToYyyyMmDd(ms: Long): String =
        Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate().format(yyyyMMdd)

    private fun parseAtomToEpochMs(s: String): Long =
        java.time.OffsetDateTime.parse(s).toInstant().toEpochMilli()

    private fun parseYyyyMmDdToEpochMs(s: String): Long {
        val d = java.time.LocalDate.parse(s)
        return d.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
