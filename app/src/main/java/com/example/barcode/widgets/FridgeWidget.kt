package com.example.barcode.widgets

import android.content.Context
import android.content.res.Configuration
import android.text.format.DateUtils
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.barcode.common.ui.theme.AppMutedDark
import com.example.barcode.common.ui.theme.AppMutedLight
import com.example.barcode.common.ui.theme.AppOnSurfaceDark
import com.example.barcode.common.ui.theme.AppOnSurfaceLight
import com.example.barcode.common.ui.theme.AppPrimary
import com.example.barcode.common.ui.theme.AppWidgetBackgroundDark
import com.example.barcode.common.ui.theme.AppWidgetBackgroundLight
import com.example.barcode.common.ui.theme.AppWidgetSurfaceDark
import com.example.barcode.common.ui.theme.AppWidgetSurfaceLight
import com.example.barcode.sync.SyncPreferences
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.size
import com.example.barcode.R

class FridgeWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Single

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId
    ) {
        val colors = WidgetPalette.fromContext(context)

        val syncPrefs = SyncPreferences(context)

        val lastSyncFinishedAt = syncPrefs
            .lastSyncFinishedAt
            .first()

        val isWidgetForceSyncRunning = syncPrefs
            .isWidgetForceSyncRunning
            .first()

        val lastSyncText = lastSyncFinishedAt.toLastSyncTimeLabel()

        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(color = colors.background)
                    .padding(10.dp),
                verticalAlignment = Alignment.Vertical.Top,
                horizontalAlignment = Alignment.Horizontal.Start
            ) {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(color = colors.surface)
                        .cornerRadius(22.dp)
                        .padding(14.dp),
                    verticalAlignment = Alignment.Vertical.Top,
                    horizontalAlignment = Alignment.Horizontal.Start
                ) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Vertical.CenterVertically,
                        horizontalAlignment = Alignment.Horizontal.Start
                    ) {
                        Text(
                            text = "FrigoZen",
                            modifier = GlanceModifier.defaultWeight(),
                            style = TextStyle(
                                color = colors.primary.toColorProvider(),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )

                        if (isWidgetForceSyncRunning) {
                            Text(
                                text = "Sync en cours",
                                style = TextStyle(
                                    color = colors.primary.toColorProvider(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        } else {
                            Image(
                                provider = ImageProvider(R.drawable.ic_widget_refresh),
                                contentDescription = "Forcer la synchronisation",
                                modifier = GlanceModifier
                                    .size(22.dp)
                                    .clickable(
                                        actionRunCallback<ForceWidgetSyncActionCallback>()
                                    )
                            )
                        }
                    }

                    Spacer(modifier = GlanceModifier.height(4.dp))

                    Text(
                        text = lastSyncText,
                        style = TextStyle(
                            color = colors.muted.toColorProvider(),
                            fontSize = 12.sp
                        )
                    )

                    Spacer(modifier = GlanceModifier.height(10.dp))

                    Text(
                        text = "À surveiller",
                        style = TextStyle(
                            color = colors.muted.toColorProvider(),
                            fontSize = 13.sp
                        )
                    )

                    Spacer(modifier = GlanceModifier.height(12.dp))

                    WidgetItemText(
                        text = "🥕 Carottes — demain",
                        colors = colors
                    )

                    WidgetItemText(
                        text = "🥛 Lait — dans 2 jours",
                        colors = colors
                    )

                    WidgetItemText(
                        text = "🍓 Fraises — aujourd’hui",
                        colors = colors
                    )
                }
            }
        }
    }
}

private data class WidgetPalette(
    val background: Color,
    val surface: Color,
    val primary: Color,
    val text: Color,
    val muted: Color
) {
    companion object {
        fun fromContext(context: Context): WidgetPalette {
            val isDark = context.resources.configuration.uiMode
                .and(Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

            return if (isDark) {
                WidgetPalette(
                    background = AppWidgetBackgroundDark,
                    surface = AppWidgetSurfaceDark,
                    primary = AppPrimary,
                    text = AppOnSurfaceDark,
                    muted = AppMutedDark
                )
            } else {
                WidgetPalette(
                    background = AppWidgetBackgroundLight,
                    surface = AppWidgetSurfaceLight,
                    primary = AppPrimary,
                    text = AppOnSurfaceLight,
                    muted = AppMutedLight
                )
            }
        }
    }
}

@Composable
private fun WidgetItemText(
    text: String,
    colors: WidgetPalette
) {
    Text(
        text = text,
        style = TextStyle(
            color = colors.text.toColorProvider(),
            fontSize = 14.sp
        )
    )

    Spacer(modifier = GlanceModifier.height(5.dp))
}

private fun Color.toColorProvider(): ColorProvider {
    return ColorProvider(this)
}

private fun Long.toLastSyncTimeLabel(): String {
    if (this <= 0L) {
        return "Dernière sync : jamais"
    }

    val time = SimpleDateFormat("HH:mm", Locale.getDefault())
        .format(Date(this))

    return if (DateUtils.isToday(this)) {
        "Dernière sync : $time"
    } else {
        val date = SimpleDateFormat("dd/MM", Locale.getDefault())
            .format(Date(this))

        "Dernière sync : $date à $time"
    }
}

suspend fun updateFridgeWidgets(context: Context) {
    val appContext = context.applicationContext
    val manager = GlanceAppWidgetManager(appContext)
    val glanceIds = manager.getGlanceIds(FridgeWidget::class.java)

    Log.d("FridgeWidget", "updateFridgeWidgets count=${glanceIds.size}")

    glanceIds.forEach { glanceId ->
        FridgeWidget().update(appContext, glanceId)
    }
}