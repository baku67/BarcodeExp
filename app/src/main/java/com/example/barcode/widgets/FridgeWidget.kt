package com.example.barcode.widgets

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
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

class FridgeWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Single

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId
    ) {
        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(Color(0xFFF8F5EF))
                    .padding(16.dp),
                verticalAlignment = Alignment.Vertical.Top,
                horizontalAlignment = Alignment.Horizontal.Start
            ) {
                Text(
                    text = "FrigoZen",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF2E7D32)),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = GlanceModifier.height(4.dp))

                Text(
                    text = "Widget factice",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF6D6A63)),
                        fontSize = 13.sp
                    )
                )

                Spacer(modifier = GlanceModifier.height(12.dp))

                Text(
                    text = "🥕 Carottes — demain",
                    style = TextStyle(fontSize = 14.sp)
                )

                Text(
                    text = "🥛 Lait — dans 2 jours",
                    style = TextStyle(fontSize = 14.sp)
                )

                Text(
                    text = "🍓 Fraises — aujourd’hui",
                    style = TextStyle(fontSize = 14.sp)
                )
            }
        }
    }
}