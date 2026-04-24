package com.example.barcode.sync

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.barcode.BarcodeApp
import com.example.barcode.core.SessionManager
import com.example.barcode.data.local.AppDb
import com.example.barcode.data.local.entities.ItemEntity
import com.example.barcode.data.local.entities.ItemNoteEntity
import com.example.barcode.data.local.entities.PendingOperation
import com.example.barcode.data.local.entities.ShoppingListItemEntity
import com.example.barcode.data.local.entities.SyncState
import com.example.barcode.features.addItems.data.remote.api.ItemNotesApi
import com.example.barcode.features.addItems.data.remote.api.ItemsApi
import com.example.barcode.features.addItems.data.remote.dto.CreateItemRequestDto
import com.example.barcode.features.addItems.data.remote.dto.ItemNoteCreateDto
import com.example.barcode.features.addItems.data.remote.dto.ManualPayload
import com.example.barcode.features.addItems.data.remote.dto.ScanPayload
import com.example.barcode.features.listeCourse.ShoppingItemDto
import com.example.barcode.features.listeCourse.ShoppingListApi
import com.example.barcode.common.utils.sanitizeNutriScore
import com.example.barcode.widgets.FridgeWidget
import com.example.barcode.widgets.updateFridgeWidgets
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.max

class SyncWorker(
    ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    companion object {
        private const val MIN_WIDGET_SYNC_VISIBLE_MS = 800L
    }

    override suspend fun doWork(): Result {
        val triggeredByWidget = inputData.getBoolean(
            SyncScheduler.INPUT_TRIGGERED_BY_WIDGET,
            false
        )

        var widgetSyncRequestId = inputData.getLong(
            SyncScheduler.INPUT_WIDGET_SYNC_REQUEST_ID,
            0L
        )

        val prefs = SyncPreferences(applicationContext)

        // Sécurité si un appel widget est fait sans passer par ForceWidgetSyncActionCallback.
        // Dans le flux normal, l'état est déjà démarré dans le callback pour avoir un feedback immédiat.
        if (triggeredByWidget && widgetSyncRequestId <= 0L) {
            val token = prefs.markWidgetForceSyncStarted()
            widgetSyncRequestId = token.requestId
            updateFridgeWidgets(applicationContext)
        }

        return try {
            doSync()
        } finally {
            if (triggeredByWidget && widgetSyncRequestId > 0L) {
                withContext(NonCancellable) {
                    val currentRequestId = prefs.widgetForceSyncRequestId.first()

                    // Si un clic plus récent existe déjà, ce worker n'a plus le droit
                    // de modifier l'état UI du widget.
                    if (currentRequestId == widgetSyncRequestId) {
                        val startedAt = prefs.widgetForceSyncStartedAt.first()

                        val elapsed = if (startedAt > 0L) {
                            System.currentTimeMillis() - startedAt
                        } else {
                            MIN_WIDGET_SYNC_VISIBLE_MS
                        }

                        val remainingVisibleMs = MIN_WIDGET_SYNC_VISIBLE_MS - elapsed

                        // Évite le cas où la sync est tellement rapide que le launcher
                        // ne rend jamais visuellement "Sync en cours".
                        if (remainingVisibleMs > 0L) {
                            delay(remainingVisibleMs)
                        }

                        val didClear = prefs.markWidgetForceSyncFinished(widgetSyncRequestId)

                        if (didClear) {
                            prefs.markLastSyncFinishedNow()
                            updateFridgeWidgets(applicationContext)
                        }
                    }
                }
            }
        }
    }

    private suspend fun doSync(): Result {
        val session = SessionManager(applicationContext)

        if (!session.isAuthenticated()) return Result.success()

        val token = session.token.first().orEmpty()
        if (token.isBlank()) return Result.success()

        val db = AppDb.get(applicationContext)
        val itemDao = db.itemDao()
        val noteDao = db.itemNoteDao()
        val shoppingDao = db.shoppingListDao()

        val app = applicationContext as BarcodeApp
        val itemsApi = app.apiClient.createApi(ItemsApi::class.java)
        val notesApi = app.apiClient.createApi(ItemNotesApi::class.java)
        val shoppingApi = app.apiClient.createApi(ShoppingListApi::class.java)

        val prefs = SyncPreferences(applicationContext)

        val sinceMs = prefs.lastSuccessAt.first()
        val sinceIso = DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(sinceMs))

        var newWatermarkMs = sinceMs

        // 0) PUSH ItemNotes DELETE
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

        // 0bis) PUSH ItemNotes CREATE
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
                    itemDao.markFailed(item.id, error = "name is blank")
                    return@forEach
                }

                val expiry = item.expiryDate?.let { epochMsToYyyyMmDd(it) }

                val dto = when (item.addMode) {
                    "manual" -> {
                        val type = item.manualType?.trim().orEmpty()
                        if (type.isBlank()) {
                            itemDao.markFailed(item.id, error = "manualType missing")
                            return@forEach
                        }

                        CreateItemRequestDto(
                            clientId = item.id,
                            name = name,
                            expiryDate = expiry,
                            addMode = "manual",
                            photoId = item.photoId,
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
                            itemDao.markFailed(item.id, error = "barcode missing for barcode_scan")
                            return@forEach
                        }

                        CreateItemRequestDto(
                            clientId = item.id,
                            name = name,
                            expiryDate = expiry,
                            addMode = "barcode_scan",
                            photoId = item.photoId,
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
                else itemDao.markFailed(item.id, error = "create failed code=${res.code()} body=${res.errorBody()?.string()}")
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
                val addMode = item.addMode

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

        // 4) PULL items UPSERTS
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

                val serverDeletedAt = dto.deletedAt?.let { parseAtomToEpochMs(it) }
                    ?: if (dto.isDeleted) serverUpdatedAt else null
                if (serverDeletedAt != null) newWatermarkMs = max(newWatermarkMs, serverDeletedAt)

                val scan = dto.scan
                val manual = dto.manual

                val remoteBarcode = scan?.barcode
                val remoteBrand = scan?.brand
                val remoteImageUrl = scan?.imageUrl
                val remoteImgIng = scan?.imageIngredientsUrl
                val remoteImgNut = scan?.imageNutritionUrl
                val remoteNutri = scan?.nutriScore

                val inferredAddMode = dto.addMode ?: when {
                    manual != null -> "manual"
                    scan != null -> "barcode_scan"
                    else -> local?.addMode ?: "barcode_scan"
                }

                val merged = (local ?: ItemEntity(id = clientId)).copy(
                    id = clientId,
                    name = dto.name ?: local?.name,
                    addedAt = dto.addedAt?.let { parseAtomToEpochMs(it) } ?: local?.addedAt,
                    expiryDate = dto.expiryDate?.let { parseYyyyMmDdToEpochMs(it) } ?: local?.expiryDate,
                    addMode = inferredAddMode,
                    barcode = remoteBarcode ?: local?.barcode,
                    brand = remoteBrand ?: local?.brand,
                    imageUrl = remoteImageUrl ?: local?.imageUrl,
                    imageIngredientsUrl = remoteImgIng ?: local?.imageIngredientsUrl,
                    imageNutritionUrl = remoteImgNut ?: local?.imageNutritionUrl,
                    nutriScore = remoteNutri ?: local?.nutriScore,
                    manualType = if (inferredAddMode == "manual") (manual?.type ?: local?.manualType) else null,
                    manualSubtype = if (inferredAddMode == "manual") (manual?.subtype ?: local?.manualSubtype) else null,
                    manualMetaJson = if (inferredAddMode == "manual") local?.manualMetaJson else null,
                    photoId = dto.photoId ?: local?.photoId,
                    pendingOperation = PendingOperation.NONE,
                    syncState = SyncState.OK,
                    lastSyncError = null,
                    failedAt = null,
                    localUpdatedAt = System.currentTimeMillis(),
                    serverUpdatedAt = serverUpdatedAt,
                    deletedAt = serverDeletedAt
                )

                itemDao.upsert(merged)
            }
        } catch (e: Exception) {
            android.util.Log.e("SyncWorker", "pull upserts exception=${e.message}")
            return Result.success()
        }

        // 5) PULL items DELETES
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

                val local = itemDao.getById(clientId)
                if (local != null && (local.pendingOperation != PendingOperation.NONE || local.syncState == SyncState.FAILED)) {
                    return@forEach
                }

                itemDao.hardDelete(clientId)
            }
        } catch (e: Exception) {
            android.util.Log.e("SyncWorker", "pull deletes exception=${e.message}")
            return Result.success()
        }

        // 6) PULL ItemNotes DELTA
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

        // 7) PUSH shopping DELETE
        val pendingShoppingDeletes = shoppingDao.getPending(PendingOperation.DELETE)
        pendingShoppingDeletes.forEach { item ->
            try {
                val res = shoppingApi.deleteItemByClientId(
                    authorization = "Bearer $token",
                    clientId = item.id
                )

                if (res.isSuccessful || res.code() == 404) {
                    shoppingDao.hardDelete(item.id)
                } else {
                    shoppingDao.markFailed(item.id, error = "shopping delete failed code=${res.code()}")
                }
            } catch (e: Exception) {
                shoppingDao.markFailed(item.id, error = "shopping delete exception=${e.message}")
            }
        }

        // 8) PUSH shopping CREATE
        val pendingShoppingCreates = shoppingDao.getPending(PendingOperation.CREATE)
        pendingShoppingCreates.forEach { item ->
            try {
                val name = item.name.trim()
                if (name.isBlank()) {
                    shoppingDao.markFailed(item.id, error = "shopping name is blank")
                    return@forEach
                }

                val dto = ShoppingItemDto(
                    clientId = item.id,
                    homeId = item.homeId,
                    scope = item.scope,
                    ownerUserId = item.ownerUserId,
                    name = name,
                    quantity = item.quantity,
                    note = item.note,
                    category = item.category,
                    isImportant = item.isImportant,
                    isFavorite = item.isFavorite,
                    isChecked = item.isChecked,
                    createdAt = epochMsToPhpAtom(item.createdAt)
                )

                val res = shoppingApi.createOrUpdateItem(
                    authorization = "Bearer $token",
                    body = dto
                )

                if (res.isSuccessful) {
                    shoppingDao.clearPending(item.id)
                } else {
                    shoppingDao.markFailed(item.id, error = "shopping create failed code=${res.code()} body=${res.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                shoppingDao.markFailed(item.id, error = "shopping create exception=${e.message}")
            }
        }

        // 9) PUSH shopping UPDATE (upsert)
        val pendingShoppingUpdates = shoppingDao.getPending(PendingOperation.UPDATE)
        pendingShoppingUpdates.forEach { item ->
            try {
                val name = item.name.trim()
                if (name.isBlank()) {
                    shoppingDao.markFailed(item.id, error = "shopping update: name is blank")
                    return@forEach
                }

                val dto = ShoppingItemDto(
                    clientId = item.id,
                    homeId = item.homeId,
                    scope = item.scope,
                    ownerUserId = item.ownerUserId,
                    name = name,
                    quantity = item.quantity,
                    note = item.note,
                    category = item.category,
                    isImportant = item.isImportant,
                    isFavorite = item.isFavorite,
                    isChecked = item.isChecked,
                    createdAt = epochMsToPhpAtom(item.createdAt)
                )

                val res = shoppingApi.createOrUpdateItem(
                    authorization = "Bearer $token",
                    body = dto
                )

                if (res.isSuccessful) {
                    shoppingDao.clearPending(item.id)
                } else {
                    shoppingDao.markFailed(item.id, error = "shopping update failed code=${res.code()} body=${res.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                shoppingDao.markFailed(item.id, error = "shopping update exception=${e.message}")
            }
        }

        // 10) PULL shopping UPSERTS
        try {
            val pullShopping = shoppingApi.getItems(
                authorization = "Bearer $token",
                updatedSince = sinceIso
            )

            if (pullShopping.code() == 401) {
                prefs.markAuthRequired()
                return Result.success()
            }

            if (!pullShopping.isSuccessful) {
                val err = pullShopping.errorBody()?.string()
                android.util.Log.e("SyncWorker", "pull shopping failed code=${pullShopping.code()} body=$err")
                return Result.success()
            }

            val remoteItems = pullShopping.body().orEmpty()
            remoteItems.forEach { dto ->
                val clientId = dto.clientId

                val local = shoppingDao.getById(clientId)
                if (local != null && (local.pendingOperation != PendingOperation.NONE || local.syncState == SyncState.FAILED)) {
                    return@forEach
                }

                val updatedAtRaw = dto.updatedAt ?: dto.createdAt
                if (updatedAtRaw == null) {
                    android.util.Log.w("SyncWorker", "shopping dto sans updatedAt/createdAt pour clientId=$clientId")
                    return@forEach
                }

                val serverUpdatedAt = parseAtomToEpochMs(updatedAtRaw)
                newWatermarkMs = max(newWatermarkMs, serverUpdatedAt)

                val serverDeletedAt = dto.deletedAt?.let { parseAtomToEpochMs(it) }
                if (serverDeletedAt != null) {
                    newWatermarkMs = max(newWatermarkMs, serverDeletedAt)
                }

                val createdAt = dto.createdAt?.let { parseAtomToEpochMs(it) } ?: serverUpdatedAt

                val merged = (local ?: ShoppingListItemEntity(
                    id = clientId,
                    homeId = dto.homeId ?: ShoppingListItemEntity.LOCAL_HOME_ID,
                    scope = dto.scope,
                    ownerUserId = dto.ownerUserId,
                    name = dto.name,
                    quantity = dto.quantity,
                    note = dto.note,
                    isImportant = dto.isImportant,
                    isFavorite = dto.isFavorite,
                    isChecked = dto.isChecked,
                    category = dto.category,
                    createdAt = createdAt,
                    pendingOperation = PendingOperation.NONE,
                    syncState = SyncState.OK,
                    lastSyncError = null,
                    failedAt = null,
                    localUpdatedAt = System.currentTimeMillis(),
                    serverUpdatedAt = serverUpdatedAt,
                    deletedAt = serverDeletedAt,
                    createdByUserId = dto.ownerUserId ?: ShoppingListItemEntity.LOCAL_USER_ID,
                    updatedByUserId = dto.ownerUserId ?: ShoppingListItemEntity.LOCAL_USER_ID,
                )).copy(
                    id = clientId,
                    homeId = dto.homeId ?: local?.homeId ?: ShoppingListItemEntity.LOCAL_HOME_ID,
                    scope = dto.scope,
                    ownerUserId = dto.ownerUserId,
                    name = dto.name,
                    quantity = dto.quantity,
                    note = dto.note,
                    isImportant = dto.isImportant,
                    isFavorite = dto.isFavorite,
                    isChecked = dto.isChecked,
                    category = dto.category,
                    createdAt = createdAt,
                    pendingOperation = PendingOperation.NONE,
                    syncState = SyncState.OK,
                    lastSyncError = null,
                    failedAt = null,
                    localUpdatedAt = System.currentTimeMillis(),
                    serverUpdatedAt = serverUpdatedAt,
                    deletedAt = serverDeletedAt,
                    createdByUserId = local?.createdByUserId ?: (dto.ownerUserId ?: ShoppingListItemEntity.LOCAL_USER_ID),
                    updatedByUserId = local?.updatedByUserId ?: (dto.ownerUserId ?: ShoppingListItemEntity.LOCAL_USER_ID),
                )

                shoppingDao.upsert(merged)
            }
        } catch (e: Exception) {
            android.util.Log.e("SyncWorker", "pull shopping exception=${e.message}")
            return Result.success()
        }

        // 11) PULL shopping DELETES
        try {
            val pullShoppingDeletes = shoppingApi.getDeletedItems(
                authorization = "Bearer $token",
                since = sinceIso
            )

            if (pullShoppingDeletes.code() == 401) {
                prefs.markAuthRequired()
                return Result.success()
            }

            if (!pullShoppingDeletes.isSuccessful) {
                val err = pullShoppingDeletes.errorBody()?.string()
                android.util.Log.e("SyncWorker", "pull shopping deletes failed code=${pullShoppingDeletes.code()} body=$err")
                return Result.success()
            }

            val deleted = pullShoppingDeletes.body().orEmpty()
            deleted.forEach { t ->
                val clientId = t.clientId
                val deletedAtMs = parseAtomToEpochMs(t.deletedAt)
                newWatermarkMs = max(newWatermarkMs, deletedAtMs)

                val local = shoppingDao.getById(clientId)
                if (local != null && (local.pendingOperation != PendingOperation.NONE || local.syncState == SyncState.FAILED)) {
                    return@forEach
                }

                shoppingDao.hardDelete(clientId)
            }
        } catch (e: Exception) {
            android.util.Log.e("SyncWorker", "pull shopping deletes exception=${e.message}")
            return Result.success()
        }

        prefs.markSyncSuccessAt(newWatermarkMs)

        updateFridgeWidgets(applicationContext)

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

    private val phpAtomFormatter: DateTimeFormatter =
        java.time.format.DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
            .appendOffset("+HH:MM", "+00:00")
            .toFormatter()

    private fun epochMsToPhpAtom(ms: Long): String =
        Instant.ofEpochMilli(ms)
            .atOffset(java.time.ZoneOffset.UTC)
            .withNano(0)
            .format(phpAtomFormatter)
}