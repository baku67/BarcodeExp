package com.example.barcode.features.fridge.components.shared

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter

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
    imgH: Float,
    alignBottom: Boolean
): FittedRectPx {
    val scale = minOf(boxW / imgW, boxH / imgH)
    val w = imgW * scale
    val h = imgH * scale
    val left = (boxW - w) / 2f
    val top = if (alignBottom) (boxH - h) else (boxH - h) / 2f
    return FittedRectPx(left, top, w, h)
}

@Composable
fun ItemThumbnail(
    imageUrl: String?,
    modifier: Modifier = Modifier.size(56.dp),
    alignBottom: Boolean = false,
    cornerIconTint: Color? = null,
    cornerIcon: ImageVector? = null,
    onImageLoaded: (Boolean) -> Unit = {},
    dimAlpha: Float = 0f, // assombrissement uniquement sur l'image
    showImageBorder: Boolean = false,
    imageBorderColor: Color = MaterialTheme.colorScheme.primary,
    imageBorderWidth: Dp = 2.dp,
    topRightOverlayOnImage: (@Composable () -> Unit)? = null,
    topRightOverlaySize: Dp = 16.dp,
) {
    val shape = RoundedCornerShape(3.dp)

    var boxW by remember { mutableFloatStateOf(0f) }
    var boxH by remember { mutableFloatStateOf(0f) }

    val painter = rememberAsyncImagePainter(imageUrl)
    val state = painter.state

    val isLoaded = state is AsyncImagePainter.State.Success || state is AsyncImagePainter.State.Error
    LaunchedEffect(isLoaded) { onImageLoaded(isLoaded) }

    val dimFactor = (dimAlpha / 0.55f).coerceIn(0f, 1f)  // 0..1
    val brightness = 1f - (0.70f * dimFactor)            // 1 -> ~0.30

    // ✅ dim overlays (icônes / badges)
    val overlayAlpha = 1f - (0.55f * dimFactor)          // 1 -> ~0.45

    val dimFilter = remember(brightness) {
        ColorFilter.colorMatrix(
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

    val drawable = (state as? AsyncImagePainter.State.Success)?.result?.drawable
    val imgW = drawable?.intrinsicWidth?.takeIf { it > 0 }?.toFloat()
    val imgH = drawable?.intrinsicHeight?.takeIf { it > 0 }?.toFloat()

    val fitted = if (imgW != null && imgH != null && boxW > 0f && boxH > 0f) {
        fittedRectForFit(boxW, boxH, imgW, imgH, alignBottom)
    } else null

    val density = LocalDensity.current

    Box(
        modifier = modifier.onSizeChanged {
            boxW = it.width.toFloat()
            boxH = it.height.toFloat()
        },
        contentAlignment = Alignment.Center
    ) {
        if (!imageUrl.isNullOrBlank()) {
            Image(
                painter = painter,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().clip(shape),
                contentScale = ContentScale.Fit,
                alignment = if (alignBottom) Alignment.BottomCenter else Alignment.Center,
                colorFilter = if (dimAlpha > 0f) dimFilter else null
            )

            when (state) {
                is AsyncImagePainter.State.Loading -> {
                    Box(
                        Modifier
                            .matchParentSize()
                            .background(Color.Black.copy(alpha = 0.10f))
                    )
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                    )
                }

                is AsyncImagePainter.State.Error -> {
                    Text("🧴", fontSize = 20.sp)
                }

                else -> Unit
            }

            if (fitted != null) {
                // ✅ overlay tint + border calés sur la zone FIT réelle
                Canvas(Modifier.matchParentSize()) {
                    val left = fitted.left
                    val top = fitted.top
                    val w = fitted.width
                    val h = fitted.height

                    if (cornerIconTint != null) {
                        drawRect(
                            brush = Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0f to Color.Transparent,
                                    0.30f to Color.Transparent,
                                    1f to cornerIconTint.copy(alpha = overlayAlpha)
                                ),
                                startY = top,
                                endY = top + h
                            ),
                            topLeft = Offset(left, top),
                            size = Size(w, h)
                        )
                    }

                    if (showImageBorder) {
                        drawRoundRect(
                            color = imageBorderColor,
                            topLeft = Offset(left, top),
                            size = Size(w, h),
                            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                            style = Stroke(width = imageBorderWidth.toPx())
                        )
                    }
                }

                // ✅ bulle icône (coin haut-gauche de l'image rendue)
                if (cornerIconTint != null && cornerIcon != null) {
                    val xDp = with(density) { fitted.left.toDp() } - 4.dp
                    val yDp = with(density) { fitted.top.toDp() } - 4.dp

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(x = xDp, y = yDp)
                            .size(15.dp)
                            .clip(CircleShape)
                            .background(cornerIconTint.copy(alpha = overlayAlpha)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = cornerIcon,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = overlayAlpha),
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }

                // ✅ overlay top-right calé sur l'image rendue (Fit)
                if (topRightOverlayOnImage != null) {
                    val inset = 1.dp
                    val leftDp = with(density) { fitted.left.toDp() }
                    val topDp = with(density) { fitted.top.toDp() }
                    val wDp = with(density) { fitted.width.toDp() }

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(
                                x = leftDp + wDp - topRightOverlaySize - inset,
                                y = topDp + inset
                            )
                            .graphicsLayer { alpha = overlayAlpha }
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
