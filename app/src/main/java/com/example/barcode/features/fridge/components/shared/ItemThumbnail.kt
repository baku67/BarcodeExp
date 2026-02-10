package com.example.barcode.features.fridge.components.shared

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter

@Composable
fun ItemThumbnail(
    imageUrl: String?,
    modifier: Modifier = Modifier.Companion,
    alignBottom: Boolean = false,
    cornerIconTint: Color? = null,
    cornerIcon: ImageVector? = null,
    onImageLoaded: (Boolean) -> Unit = {},
    dimAlpha: Float = 0f, // ✅ NEW : assombrissement uniquement sur l'image
    showImageBorder: Boolean = false,                 // ✅ NEW
    imageBorderColor: Color = MaterialTheme.colorScheme.primary, // ✅ NEW
    imageBorderWidth: Dp = 2.dp,                       // ✅ NEW
    topRightOverlayOnImage: (@Composable () -> Unit)? = null,
    topRightOverlaySize: Dp = 16.dp,
) {
    val shape = RoundedCornerShape(3.dp)

    val dimFactor = (dimAlpha / 0.55f).coerceIn(0f, 1f) // 0..1
    val brightness = 1f - (0.70f * dimFactor)           // 1 -> ~0.30 (plus sombre)

    val dimFilter = remember(brightness) {
        // Multiplie R,G,B par "brightness" sans toucher A (alpha)
        ColorFilter.Companion.colorMatrix(
            ColorMatrix(
                floatArrayOf(
                    brightness, 0f, 0f, 0f, 0f,
                    0f, brightness, 0f, 0f, 0f,
                    0f, 0f, brightness, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )
    }

    var boxW by remember { mutableStateOf(0f) }
    var boxH by remember { mutableStateOf(0f) }
    var imgW by remember(imageUrl) { mutableStateOf<Float?>(null) }
    var imgH by remember(imageUrl) { mutableStateOf<Float?>(null) }

    Box(
        modifier = modifier
            .size(56.dp)
            .onSizeChanged {
                boxW = it.width.toFloat()
                boxH = it.height.toFloat()
            },
        contentAlignment = if (alignBottom) Alignment.Companion.BottomCenter else Alignment.Companion.Center
    ) {
        if (!imageUrl.isNullOrBlank()) {
            val painter = rememberAsyncImagePainter(imageUrl)
            val state = painter.state  // ✅ Lecture simple (se met à jour à chaque recompo)

            if (alignBottom) {
                // ✅ boîte de placement : l'image est alignée en bas
                Box(
                    Modifier.Companion.matchParentSize(),
                    contentAlignment = Alignment.Companion.BottomCenter
                ) {
                    Image(
                        painter = painter,
                        contentDescription = null,
                        modifier = Modifier.Companion
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .clip(shape),
                        contentScale = ContentScale.Companion.Fit,
                        colorFilter = if (dimAlpha > 0f) dimFilter else null
                    )
                }
            } else {
                Image(
                    painter = painter,
                    contentDescription = null,
                    modifier = Modifier.Companion
                        .matchParentSize()
                        .clip(shape),
                    contentScale = ContentScale.Companion.Fit,
                    colorFilter = if (dimAlpha > 0f) dimFilter else null
                )
            }

            when (state) {
                is AsyncImagePainter.State.Loading -> {
                    onImageLoaded(false)
                    Box(
                        modifier = Modifier.Companion
                            .matchParentSize()
                            .background(Color.Companion.Black.copy(alpha = 0.10f))
                    )
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.Companion.size(18.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                    )
                }

                is AsyncImagePainter.State.Error -> {
                    onImageLoaded(true)
                    Text("🧴", fontSize = 20.sp)
                }

                is AsyncImagePainter.State.Success -> {
                    onImageLoaded(true)
                    val d = (state as AsyncImagePainter.State.Success).result.drawable
                    val iw = d.intrinsicWidth
                    val ih = d.intrinsicHeight
                    imgW = iw.takeIf { it > 0 }?.toFloat()
                    imgH = ih.takeIf { it > 0 }?.toFloat()
                }

                else -> Unit
            }

            val isImageReady = state is AsyncImagePainter.State.Success

            if (isImageReady && imgW != null && imgH != null && boxW > 0f && boxH > 0f) {
                // Fit: l'image affichée est centrée dans le conteneur (ou collée en bas si alignBottom)
                val scale = minOf(boxW / imgW!!, boxH / imgH!!)
                val dw = imgW!! * scale
                val dh = imgH!! * scale

                val dx = (boxW - dw) / 2f
                val dy = if (alignBottom) (boxH - dh) else (boxH - dh) / 2f


                // ✅ overlay dégradé : color (bas) -> transparent (haut), limité à la zone FIT
                Canvas(Modifier.Companion.matchParentSize()) {
                    val left = dx
                    val top = dy
                    val right = dx + dw
                    val bottom = dy + dh

                    if (cornerIconTint != null) {
                        drawRect(
                            brush = Brush.Companion.verticalGradient(
                                colorStops = arrayOf(
                                    0f to Color.Companion.Transparent,
                                    0.3f to Color.Companion.Transparent,
                                    1f to cornerIconTint.copy(alpha = 1f)
                                ),
                                startY = top,
                                endY = bottom
                            ),
                            topLeft = Offset(left, top),
                            size = Size(dw, dh)
                        )
                    }

                    // border des images produits lors slection BottomSheet par exemple
                    if (showImageBorder) {
                        drawRoundRect(
                            color = imageBorderColor,
                            topLeft = Offset(left, top),
                            size = Size(dw, dh),
                            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                            style = Stroke(width = imageBorderWidth.toPx())
                        )
                    }
                }

                // position icône dans le coin haut-gauche de l'image affichée
                if (cornerIconTint != null && cornerIcon != null) {
                    Box(
                        modifier = Modifier.Companion
                            .align(Alignment.Companion.TopStart)
                            .offset(
                                x = (dx / LocalDensity.current.density).dp - 4.dp,
                                y = (dy / LocalDensity.current.density).dp - 4.dp
                            )
                            .size(15.dp) // ✅ un peu plus grand qu’avant pour une bulle lisible
                            .clip(CircleShape)
                            .background(cornerIconTint),
                        contentAlignment = Alignment.Companion.Center
                    ) {
                        Icon(
                            imageVector = cornerIcon,
                            contentDescription = null,
                            tint = Color.Companion.White,
                            modifier = Modifier.Companion.size(11.dp) // ✅ icône plus petite dans la bulle
                        )
                    }
                }


                // ✅ overlay calé sur le coin HAUT-DROIT de l'image rendue (Fit)
                if (topRightOverlayOnImage != null) {
                        val density = LocalDensity.current.density
                        val inset = 1.dp

                        Box(
                                modifier = Modifier.Companion
                                            .align(Alignment.Companion.TopStart)
                                            .offset(
                                                x = ((dx + dw) / density).dp - topRightOverlaySize - inset,
                                        y = (dy / density).dp + inset
                                            )
                        ) {
                                topRightOverlayOnImage()
                            }
                    }


            }


        } else {
            Text("🧴", fontSize = 20.sp)
        }
    }
}



private data class FittedRectPx(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float
)

private fun fittedRectForFit(
    boxW: Float,
    boxH: Float,
    imgW: Float,
    imgH: Float
): FittedRectPx {
    val scale = minOf(boxW / imgW, boxH / imgH)
    val w = imgW * scale
    val h = imgH * scale
    val left = (boxW - w) / 2f
    val top = (boxH - h) / 2f
    return FittedRectPx(left, top, w, h)
}