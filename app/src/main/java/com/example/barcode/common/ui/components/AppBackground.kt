package com.example.barcode.common.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.res.imageResource
import com.example.barcode.R
import com.example.barcode.common.ui.theme.AppPrimary
import com.example.barcode.common.ui.theme.LocalIsDarkTheme

@Composable
fun AppBackground(content: @Composable () -> Unit) {
    val isDark = LocalIsDarkTheme.current

    // tile pattern Webp/PNG (même image pour dark/light ou 2 fichiers différents)
    val tile = ImageBitmap.imageResource(
        if (isDark) R.drawable.app_background_pattern_tile else R.drawable.app_background_pattern_tile
        // si tu fais 2 tiles : R.drawable.pattern_tile_dark / _light
    )

    val patternBrush = remember(tile) {
        ShaderBrush(
            ImageShader(
                image = tile,
                tileModeX = TileMode.Repeated,
                tileModeY = TileMode.Repeated
            )
        )
    }

    Box(Modifier.fillMaxSize()) {

        // 1) Fond statique (gradients)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithCache {

                    if (isDark) {
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

                            // Ici si grid icones statique sans anim parallax
                        }
                    } else {
                        // 🌤️ LIGHT: gradient différent (tu peux ajuster ces couleurs)
                        val bg1 = Color(0xFFF7FBFF)
                        val bg2 = Color(0xFFEAF3FF)

                        val radialPrimary = Brush.radialGradient(
                            colorStops = arrayOf(
                                0.0f to AppPrimary.copy(alpha = 0.18f),
                                0.6f to Color.Transparent
                            ),
                            center = Offset(size.width * 0.20f, size.height * 0.15f),
                            radius = size.minDimension * 0.90f
                        )

                        val radialBlue = Brush.radialGradient(
                            colorStops = arrayOf(
                                0.0f to Color(0x1A3B82F6), // bleu très léger
                                0.6f to Color.Transparent
                            ),
                            center = Offset(size.width * 0.85f, size.height * 0.20f),
                            radius = size.minDimension * 0.85f
                        )

                        val linear = Brush.verticalGradient(listOf(bg1, bg2))

                        // Grid pattern Webp/PNG
                        onDrawBehind {
                            drawRect(linear)
                            drawRect(radialPrimary)
                            drawRect(radialBlue)

                            // Ici si grid icones statique sans anim parallax
                        }
                    }
                }
        )

        // 2) Overlay pattern (statique, 1 couche, +rotate, +scale)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithCache {
                    onDrawBehind {
                        withTransform({
                            // rotation globale pour casser la grille
                            val patternRotation = 9f // subtil: (-10 / 10)
                            rotate(degrees = patternRotation, pivot = center)
                            // scale globale des tiles
                            val patternScale = if (isDark) 3.1f else 3.1f // > 1 = icônes plus grosses, motif moins dense
                            scale(
                                scaleX = patternScale,
                                scaleY = patternScale,
                                pivot = Offset(size.width / 2f, size.height / 2f) // centré: laisser 2f meme si scale+-
                            )
                        }) {
                            // draw "overscan" pour éviter les coins vides après rotation (+opacité)
                            val overscan = 1.6f
                            val w = size.width * overscan
                            val h = size.height * overscan

                            drawRect(
                                brush = patternBrush,
                                topLeft = Offset(
                                    x = center.x - w / 2f,
                                    y = center.y - h / 2f
                                ),
                                size = Size(w, h),
                                alpha = if (isDark) 0.13f else 0.08f // Opacité du pattern (dark(0)=transp)
                            )
                        }
                    }
                }
        )

        // 3) Contenu au-dessus
        Box(Modifier.fillMaxSize()) { content() }
    }
}


