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

        Log.d("FridgeWidget", "Force sync clicked from widget")

        SyncPreferences(appContext).markWidgetForceSyncStarted()

        updateFridgeWidgets(appContext)

        SyncScheduler.enqueueForceSync(
            context = appContext,
            triggeredByWidget = true
        )
    }
}