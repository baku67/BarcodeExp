package com.example.barcode.widgets

import android.content.Context
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.example.barcode.sync.SyncPreferences
import com.example.barcode.sync.SyncScheduler

class ForceWidgetSyncActionCallback : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val appContext = context.applicationContext
        val prefs = SyncPreferences(appContext)

        Log.d("FridgeWidget", "Force sync clicked from widget")

        val token = prefs.markWidgetForceSyncStarted()

        // Feedback immédiat côté widget.
        // Feedback immédiat sur le widget cliqué.
        FridgeWidget().update(appContext, glanceId)

        // On garde updateFridgeWidgets() pour synchroniser toutes les instances du widget.
        updateFridgeWidgets(appContext)

        SyncScheduler.enqueueForceSync(
            context = appContext,
            triggeredByWidget = true,
            widgetSyncRequestId = token.requestId
        )
    }
}