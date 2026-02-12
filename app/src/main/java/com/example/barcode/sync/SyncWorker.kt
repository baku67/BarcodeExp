package com.example.barcode.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.barcode.core.network.ApiClient
import com.example.barcode.core.SessionManager
import com.example.barcode.data.local.AppDb
import com.example.barcode.data.local.entities.ItemEntity
import com.example.barcode.data.local.entities.ItemNoteEntity
import com.example.barcode.data.local.entities.PendingOperation
import com.example.barcode.data.local.entities.SyncState
import com.example.barcode.features.addItems.data.remote.api.ItemNotesApi
import com.example.barcode.features.addItems.data.remote.api.ItemsApi
import com.example.barcode.features.addItems.data.remote.dto.CreateItemRequestDto
import com.example.barcode.features.addItems.data.remote.dto.ScanPayload
import com.example.barcode.features.addItems.data.remote.dto.ManualPayload
import com.example.barcode.features.addItems.data.remote.dto.ItemNoteCreateDto
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

        val db = AppDb.get(applicationContext)
        val itemDao = db.itemDao()
        val noteDao = db.itemNoteDao()

        val itemsApi = ApiClient.createApi(ItemsApi::class.java)
        val notesApi = ApiClient.createApi(ItemNotesApi::class.java)


        val prefs = SyncPreferences(applicationContext)

        // ✅ watermark serveur stocké en local (epoch ms)
        val sinceMs = prefs.lastSuccessAt.first()
        val sinceIso = DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(sinceMs))

        var newWatermarkMs = sinceMs


        // 0) PUSH ItemNtes DELETE
        val pendingNoteDeletes = noteDao.getPending(PendingOperation.DELETE)
        pendingNoteDeletes.forEach { note ->
            try {
                val res = notesApi.deleteNote(
                    authorization = "Bearer $token",
                    clientId = note.id
                )

                if (res.isSuccessful || res.code() == 404) {
                    noteDao.hardDelete(note.id)
                } else {
                    noteDao.markFailed(note.id, error = "note delete failed code=${res.code()}")
                }
            } catch (e: Exception) {
                noteDao.markFailed(note.id, error = "note delete exception=${e.message}")
            }
        }

        // 0bis) PUSH itemNotes CREATE
        val pendingNoteCreates = noteDao.getPending(PendingOperation.CREATE)
        pendingNoteCreates.forEach { note ->
            try {
                val res = notesApi.createNote(
                    authorization = "Bearer $token",
                    itemClientId = note.itemId,
                    body = ItemNoteCreateDto(
                        clientId = note.id,
                        body = note.body,
                        pinned = note.pinned
                    )
                )

                if (res.isSuccessful) {
                    noteDao.clearPending(note.id)
                } else {
                    noteDao.markFailed(note.id, error = "note create failed code=${res.code()}")
                }
            } catch (e: Exception) {
                noteDao.markFailed(note.id, error = "note create exception=${e.message}")
            }
        }



        // 1) PUSH items DELETE
        val pendingDeletes = itemDao.getPending(PendingOperation.DELETE)
        pendingDeletes.forEach { item ->
            try {
                val res = itemsApi.deleteItemByClientId(
                    authorization = "Bearer $token",
                    clientId = item.id
                )

                if (res.isSuccessful || res.code() == 404) {
                    itemDao.hardDelete(item.id)
                } else {
                    itemDao.markFailed(item.id, error = "delete failed code=${res.code()}")
                }
            } catch (e: Exception) {
                itemDao.markFailed(item.id, error = "delete exception=${e.message}")
            }
        }

        // 2) PUSH items CREATE
        val pendingCreates = itemDao.getPending(PendingOperation.CREATE)
        pendingCreates.forEach { item ->
            try {
                val name = item.name?.trim().orEmpty()
                if (name.isBlank()) {
                    itemDao.markFailed(item.id, error = "create/update: name is blank")
                    return@forEach
                }

                val expiry = item.expiryDate?.let { epochMsToYyyyMmDd(it) }
                val addMode = item.addMode // "barcode_scan" | "manual"

                val dto = when (addMode) {
                    "manual" -> {
                        val type = item.manualType?.trim().orEmpty()
                        if (type.isBlank()) {
                            itemDao.markFailed(item.id, error = "manual item missing manualType")
                            return@forEach
                        }

                        CreateItemRequestDto(
                            clientId = item.id,
                            name = name,
                            expiryDate = expiry,
                            addMode = "manual",
                            scan = null,
                            manual = ManualPayload(
                                type = type,
                                subtype = item.manualSubtype
                            )
                        )
                    }

                    else -> {
                        val barcode = item.barcode?.trim().orEmpty()
                        if (barcode.isBlank()) {
                            itemDao.markFailed(item.id, error = "scanned item missing barcode")
                            return@forEach
                        }

                        CreateItemRequestDto(
                            clientId = item.id,
                            name = name,
                            expiryDate = expiry,
                            addMode = "barcode_scan",
                            manual = null,
                            scan = ScanPayload(
                                barcode = barcode,
                                brand = item.brand,
                                imageUrl = item.imageUrl,
                                imageIngredientsUrl = item.imageIngredientsUrl,
                                imageNutritionUrl = item.imageNutritionUrl,
                                nutriScore = sanitizeNutriScore(item.nutriScore)
                            )
                        )
                    }
                }

                val res = itemsApi.createItem(
                    authorization = "Bearer $token",
                    body = dto
                )

                if (res.isSuccessful) itemDao.clearPending(item.id)
                else itemDao.markFailed(item.id, error = "create failed code=${res.code()}")

            } catch (e: Exception) {
                itemDao.markFailed(item.id, error = "create exception=${e.message}")
            }
        }

        // 3) PUSH items UPDATE (upsert)
        val pendingUpdates = itemDao.getPending(PendingOperation.UPDATE)
        pendingUpdates.forEach { item ->
            try {
                val name = item.name?.trim().orEmpty()
                if (name.isBlank()) {
                    itemDao.markFailed(item.id, error = "create/update: name is blank")
                    return@forEach
                }

                val expiry = item.expiryDate?.let { epochMsToYyyyMmDd(it) }
                val addMode = item.addMode // "barcode_scan" | "manual"

                val dto = when (addMode) {
                    "manual" -> {
                        val type = item.manualType?.trim().orEmpty()
                        if (type.isBlank()) {
                            itemDao.markFailed(item.id, error = "manual item missing manualType")
                            return@forEach
                        }

                        CreateItemRequestDto(
                            clientId = item.id,
                            name = name,
                            expiryDate = expiry,
                            addMode = "manual",
                            scan = null,
                            manual = ManualPayload(
                                type = type,
                                subtype = item.manualSubtype
                            )
                        )
                    }

                    else -> {
                        val barcode = item.barcode?.trim().orEmpty()
                        if (barcode.isBlank()) {
                            itemDao.markFailed(item.id, error = "scanned item missing barcode")
                            return@forEach
                        }

                        CreateItemRequestDto(
                            clientId = item.id,
                            name = name,
                            expiryDate = expiry,
                            addMode = "barcode_scan",
                            manual = null,
                            scan = ScanPayload(
                                barcode = barcode,
                                brand = item.brand,
                                imageUrl = item.imageUrl,
                                imageIngredientsUrl = item.imageIngredientsUrl,
                                imageNutritionUrl = item.imageNutritionUrl,
                                nutriScore = sanitizeNutriScore(item.nutriScore)
                            )
                        )
                    }
                }

                val res = itemsApi.createItem(
                    authorization = "Bearer $token",
                    body = dto
                )

                if (res.isSuccessful) itemDao.clearPending(item.id)
                else itemDao.markFailed(item.id, error = "update(upsert) failed code=${res.code()}")

            } catch (e: Exception) {
                itemDao.markFailed(item.id, error = "update(upsert) exception=${e.message}")
            }
        }

        // 4) PULL items UPSERTS (items actifs)
        try {
            val pullUpserts = itemsApi.getItems(
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

                val local = itemDao.getById(clientId)
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

                itemDao.upsert(entity)
            }

        } catch (e: Exception) {
            android.util.Log.e("SyncWorker", "pull upserts exception=${e.message}")
            return Result.success()
        }

        // 5) PULL items DELETES (tombstones)
        try {
            val pullDeletes = itemsApi.getDeletedItems(
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
                val local = itemDao.getById(clientId)
                if (local != null && (local.pendingOperation != PendingOperation.NONE || local.syncState == SyncState.FAILED)) {
                    return@forEach
                }

                // ✅ choix simple: purge locale (comme après un delete push)
                itemDao.hardDelete(clientId)
            }

        } catch (e: Exception) {
            android.util.Log.e("SyncWorker", "pull deletes exception=${e.message}")
            return Result.success()
        }

        // 6) PULL ItemNotes DELTA (créées/supprimées)
        try {
            val pullNotes = notesApi.getNotesDelta(
                authorization = "Bearer $token",
                since = sinceIso
            )

            if (pullNotes.code() == 401) {
                prefs.markAuthRequired()
                return Result.success()
            }

            if (!pullNotes.isSuccessful) {
                val err = pullNotes.errorBody()?.string()
                android.util.Log.e("SyncWorker", "pull notes failed code=${pullNotes.code()} body=$err")
                return Result.success()
            }

            val body = pullNotes.body()
            if (body != null) {
                newWatermarkMs = max(newWatermarkMs, parseAtomToEpochMs(body.serverTime))

                body.notes.forEach { dto ->
                    val clientId = dto.clientId

                    val local = noteDao.getById(clientId)
                    if (local != null && (local.pendingOperation != PendingOperation.NONE || local.syncState == SyncState.FAILED)) {
                        return@forEach
                    }

                    val serverUpdatedAt = parseAtomToEpochMs(dto.updatedAt)
                    newWatermarkMs = max(newWatermarkMs, serverUpdatedAt)

                    val serverDeletedAt = dto.deletedAt?.let { parseAtomToEpochMs(it) }
                    if (serverDeletedAt != null) {
                        newWatermarkMs = max(newWatermarkMs, serverDeletedAt)
                        noteDao.hardDelete(clientId)
                        return@forEach
                    }

                    val createdAt = dto.createdAt?.let { parseAtomToEpochMs(it) } ?: serverUpdatedAt

                    noteDao.upsert(
                        ItemNoteEntity(
                            id = clientId,
                            itemId = dto.itemClientId,
                            body = dto.body,
                            pinned = dto.pinned,
                            createdAt = createdAt,
                            deletedAt = null,
                            pendingOperation = PendingOperation.NONE,
                            syncState = SyncState.OK,
                            lastSyncError = null,
                            failedAt = null,
                            localUpdatedAt = System.currentTimeMillis(),
                            serverUpdatedAt = serverUpdatedAt
                        )
                    )
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SyncWorker", "pull notes exception=${e.message}")
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
