package com.example.barcode.widgets

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState

class ToggleWidgetFridgeContentModeActionCallback : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val appContext = context.applicationContext

        updateAppWidgetState(appContext, glanceId) { prefs ->
            val currentMode = WidgetFridgeContentMode.fromStoredValue(
                prefs[WidgetFridgeContentModeKey]
            )

            prefs[WidgetFridgeContentModeKey] = currentMode
                .toggled()
                .storedValue
        }

        FridgeWidget().update(appContext, glanceId)
    }
}