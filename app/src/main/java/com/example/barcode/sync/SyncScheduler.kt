package com.example.barcode.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.barcode.widgets.FridgeWidgetRefreshWorker
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

    const val INPUT_TRIGGERED_BY_WIDGET = "triggered_by_widget"
    const val WIDGET_REFRESH_UNIQUE_NAME = "fridge_widget_refresh_once"
    const val INPUT_WIDGET_SYNC_REQUEST_ID = "widget_sync_request_id"

    // Sync utilisé lors des pull-to-refresh, ou modifs entités etc... (?)
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

    // Sync forcée depuis le widget (?)
    fun enqueueForceSync(
        context: Context,
        triggeredByWidget: Boolean = false,
        widgetSyncRequestId: Long = 0L
    ) {
        val appContext = context.applicationContext

        val builder = OneTimeWorkRequestBuilder<SyncWorker>()
            .addTag(SYNC_TAG)
            .setInputData(
                workDataOf(
                    INPUT_TRIGGERED_BY_WIDGET to triggeredByWidget,
                    INPUT_WIDGET_SYNC_REQUEST_ID to widgetSyncRequestId
                )
            )

        // Important :
        // Pour une sync forcée depuis le widget, je ne mets pas de contrainte réseau.
        // Sinon l'état UI peut passer à "Sync en cours" alors que le worker ne démarre pas.
        if (!triggeredByWidget) {
            builder.setConstraints(syncConstraints())
        }

        val req = builder.build()

        WorkManager.getInstance(appContext)
            .enqueueUniqueWork(
                SYNC_UNIQUE_NAME,
                ExistingWorkPolicy.REPLACE,
                req
            )
    }

    // Sync en arrière plan toutes les 2h (?)
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

    fun enqueueWidgetRefresh(context: Context) {
        val appContext = context.applicationContext

        val req = OneTimeWorkRequestBuilder<FridgeWidgetRefreshWorker>()
            .build()

        WorkManager.getInstance(appContext)
            .enqueueUniqueWork(
                WIDGET_REFRESH_UNIQUE_NAME,
                ExistingWorkPolicy.REPLACE,
                req
            )
    }

}