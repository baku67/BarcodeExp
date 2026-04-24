package com.example.barcode.widgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.widget.RemoteViews
import android.widget.Toast
import com.example.barcode.R

object WidgetPinning {

    fun requestPinFridgeWidget(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Toast.makeText(
                context,
                "Ajoutez le widget depuis l’écran d’accueil : appui long > Widgets > FrigoZen",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val appWidgetManager = context.getSystemService(AppWidgetManager::class.java)

        if (!appWidgetManager.isRequestPinAppWidgetSupported) {
            Toast.makeText(
                context,
                "Votre lanceur ne permet pas l’ajout direct. Ajoutez le widget depuis l’écran d’accueil.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val provider = ComponentName(
            context,
            FridgeWidgetReceiver::class.java
        )

        val preview = RemoteViews(
            context.packageName,
            R.layout.fridge_fake_widget_preview
        )

        val extras = Bundle().apply {
            putParcelable(
                AppWidgetManager.EXTRA_APPWIDGET_PREVIEW,
                preview
            )
        }

        appWidgetManager.requestPinAppWidget(
            provider,
            extras,
            null
        )
    }
}