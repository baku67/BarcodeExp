package com.example.barcode.widgets

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.example.barcode.sync.SyncScheduler

class ForceWidgetSyncActionCallback : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        SyncScheduler.enqueueForceSync(context)

        // Optionnel, mais utile pour forcer un refresh visuel immédiat.
        // La vraie heure de dernière sync sera surtout mise à jour après la fin du SyncWorker.
        SyncScheduler.enqueueWidgetRefresh(context)
    }
}