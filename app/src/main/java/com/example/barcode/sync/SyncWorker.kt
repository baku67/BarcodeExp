package com.example.barcode.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.barcode.core.network.ApiClient
import com.example.barcode.core.session.SessionManager
import com.example.barcode.data.local.AppDb
import com.example.barcode.data.local.entities.SyncStatus
import com.example.barcode.features.addItems.data.remote.api.ItemsApi
import com.example.barcode.features.addItems.data.remote.dto.ItemCreateDto
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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

        val pending = dao.getBySyncStatus(SyncStatus.PENDING_CREATE)
        if (pending.isEmpty()) return Result.success()

        pending.forEach { item ->
            try {
                val dto = ItemCreateDto(
                    barcode = item.barcode,
                    name = item.name,
                    brand = item.brand,
                    imageUrl = item.imageUrl,
                    imageIngredientsUrl = item.imageIngredientsUrl,
                    imageNutritionUrl = item.imageNutritionUrl,
                    nutriScore = item.nutriScore,
                    expiryDate = item.expiryDate?.let { epochMsToYyyyMmDd(it) },
                    addMode = item.addMode,
                )

                val res = api.createItem(
                    authorization = "Bearer $token",
                    body = dto
                )

                // DEBUG (TODO remove)
                if (!res.isSuccessful) {
                    val err = res.errorBody()?.string()
                    android.util.Log.e("SyncWorker", "422 body=$err")
                }

                if (res.isSuccessful) dao.updateSyncStatus(item.id, SyncStatus.SYNCED)
                else dao.updateSyncStatus(item.id, SyncStatus.FAILED)

            } catch (e: Exception) {
                dao.updateSyncStatus(item.id, SyncStatus.FAILED)
                // On continue les autres; tu pourras plus tard faire Result.retry()
            }
        }

        return Result.success()
    }


    private val yyyyMMdd = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private fun epochMsToYyyyMmDd(ms: Long): String =
        Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate().format(yyyyMMdd)
}
