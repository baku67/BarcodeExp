package com.example.barcode.widgets

import android.content.Context
import android.content.res.Configuration
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

class FridgeWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Single

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId
    ) {
        val colors = WidgetPalette.fromContext(context)

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
                    Text(
                        text = "FrigoZen",
                        style = TextStyle(
                            color = colors.primary.toColorProvider(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    Spacer(modifier = GlanceModifier.height(4.dp))

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