package com.example.barcode.widgets

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.example.barcode.sync.SyncScheduler

class FridgeWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = FridgeWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)

        SyncScheduler.enqueueWidgetRefresh(context)
        SyncScheduler.enqueueSync(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
    }
}