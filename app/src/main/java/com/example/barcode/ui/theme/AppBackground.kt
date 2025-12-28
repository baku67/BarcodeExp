package com.example.barcode.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun AppBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawWithCache {
                val bg1 = Color(0xFF0B1220)
                val bg2 = Color(0xFF0A2A3A)

                val radialGreen = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f to Color(0x4022C55E), // rgba(34,197,94,.25) approx
                        0.6f to Color.Transparent
                    ),
                    center = Offset(size.width * 0.20f, size.height * 0.15f),
                    radius = size.minDimension * 0.90f
                )

                val radialBlue = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f to Color(0x333B82F6), // rgba(59,130,246,.20) approx
                        0.6f to Color.Transparent
                    ),
                    center = Offset(size.width * 0.85f, size.height * 0.20f),
                    radius = size.minDimension * 0.85f
                )

                val linear = Brush.verticalGradient(listOf(bg1, bg2))

                onDrawBehind {
                    drawRect(linear)
                    drawRect(radialGreen)
                    drawRect(radialBlue)
                }
            }
    ) { content() }
}
