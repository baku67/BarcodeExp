package com.example.barcode.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object SyncScheduler {

    /**
     * Sync ponctuelle :
     * - pull-to-refresh
     * - après login/register
     * - après ajout/modif/suppression locale si besoin
     */
    const val SYNC_UNIQUE_NAME = "sync_items_once"

    /**
     * Sync périodique :
     * - arrière-plan toutes les 2h environ
     */
    const val PERIODIC_SYNC_UNIQUE_NAME = "sync_items_periodic"

    /**
     * Tag observable côté UI.
     *
     * Important :
     * on met le même tag sur la sync ponctuelle ET périodique,
     * comme ça FridgePage peut continuer à observer SyncScheduler.SYNC_TAG.
     */
    const val SYNC_TAG = "SYNC_ITEMS"

    fun enqueueSync(context: Context) {
        val appContext = context.applicationContext

        val req = OneTimeWorkRequestBuilder<SyncWorker>()
            .addTag(SYNC_TAG)
            .setConstraints(syncConstraints())
            .build()

        WorkManager.getInstance(appContext)
            .enqueueUniqueWork(
                SYNC_UNIQUE_NAME,
                ExistingWorkPolicy.KEEP,
                req
            )
    }

    fun enqueuePeriodicSync(context: Context) {
        val appContext = context.applicationContext

        val req = PeriodicWorkRequestBuilder<SyncWorker>(
            2,
            TimeUnit.HOURS
        )
            .addTag(SYNC_TAG)
            .setConstraints(syncConstraints())
            .build()

        WorkManager.getInstance(appContext)
            .enqueueUniquePeriodicWork(
                PERIODIC_SYNC_UNIQUE_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                req
            )
    }

    fun cancelPeriodicSync(context: Context) {
        WorkManager.getInstance(context.applicationContext)
            .cancelUniqueWork(PERIODIC_SYNC_UNIQUE_NAME)
    }

    private fun syncConstraints(): Constraints {
        return Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    }
}