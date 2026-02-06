package com.example.barcode.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

object SyncScheduler {
    /** Nom unique pour éviter d'empiler des syncs */
    const val SYNC_UNIQUE_NAME = "sync_items"

    /** Tag observable côté UI */
    const val SYNC_TAG = "SYNC_ITEMS"

    fun enqueueSync(context: Context) {
        val req = OneTimeWorkRequestBuilder<SyncWorker>()
            .addTag(SYNC_TAG) // ✅ IMPORTANT : sans ça l'UI ne peut pas "voir" la sync
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(SYNC_UNIQUE_NAME, ExistingWorkPolicy.KEEP, req)
    }
}
