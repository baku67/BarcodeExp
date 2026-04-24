package com.example.barcode.widgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context

object WidgetInstallState {

    fun isFridgeWidgetInstalled(context: Context): Boolean {
        val appWidgetManager = AppWidgetManager.getInstance(context)

        val widgetProvider = ComponentName(
            context,
            FridgeWidgetReceiver::class.java // ⚠️ Mets ici ta classe Receiver, pas FridgeWidget()
        )

        return appWidgetManager
            .getAppWidgetIds(widgetProvider)
            .isNotEmpty()
    }
}