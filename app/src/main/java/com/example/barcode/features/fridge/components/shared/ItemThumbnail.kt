package com.example.barcode.features.fridge.components.shared

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.TimerOff
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import com.example.barcode.common.expiry.ExpiryLevel
import com.example.barcode.common.expiry.ExpiryPolicy
import com.example.barcode.common.expiry.expiryLevel
import com.example.barcode.common.ui.expiry.expiryGlowColor
import com.example.barcode.common.ui.expiry.expirySelectionBorderColor
import com.example.barcode.data.local.entities.ItemEntity
import com.example.barcode.features.addItems.manual.MANUAL_TYPES_WITH_SUBTYPE_IMAGE
import com.example.barcode.features.addItems.manual.ManualTaxonomyImageResolver

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
    dimAlpha: Float = 0f,
    showImageBorder: Boolean = false,
    imageBorderColor: Color = MaterialTheme.colorScheme.primary,
    imageBorderWidth: Dp = 2.dp,
    topRightOverlayOnImage: (@Composable () -> Unit)? = null,
    topRightOverlaySize: Dp = 16.dp,

    // ✅ NEW (defaults = comportement actuel)
    cornerBadgeSize: Dp = 15.dp,
    cornerBadgeIconSize: Dp = 11.dp,
    cornerBadgeOffset: Dp = 4.dp,
) {
    var shape = RoundedCornerShape(3.dp)

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

            val imageModifier = Modifier
                .fillMaxSize()
                // ✅ clip + (si besoin) offscreen compositing pour que le BlendMode masque sur l'alpha
                .graphicsLayer {
                    shape = shape
                    clip = true
                    compositingStrategy =
                        if (cornerIconTint != null) CompositingStrategy.Offscreen
                        else CompositingStrategy.Auto
                }
                .then(
                    if (cornerIconTint != null) {
                        val fittedLocal = fitted
                        Modifier.drawWithCache {
                            val rect = fittedLocal
                            val startY = rect?.top ?: 0f
                            val endY = rect?.let { it.top + it.height } ?: size.height

                            val brush = Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0f to Color.Transparent,
                                    0.30f to Color.Transparent,
                                    1f to cornerIconTint.copy(alpha = overlayAlpha)
                                ),
                                startY = startY,
                                endY = endY
                            )

                            onDrawWithContent {
                                // 1) image
                                drawContent()

                                // 2) gradient masqué par l'alpha de l'image (fond transparent ignoré)
                                if (rect != null) {
                                    clipRect(
                                        left = rect.left,
                                        top = rect.top,
                                        right = rect.left + rect.width,
                                        bottom = rect.top + rect.height
                                    ) {
                                        drawRect(
                                            brush = brush,
                                            topLeft = Offset(rect.left, rect.top),
                                            size = Size(rect.width, rect.height),
                                            blendMode = BlendMode.SrcAtop
                                        )
                                    }
                                } else {
                                    drawRect(brush = brush, blendMode = BlendMode.SrcAtop)
                                }
                            }
                        }
                    } else Modifier
                )

            Image(
                painter = painter,
                contentDescription = null,
                modifier = imageModifier,
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
                    Text("\uD83C\uDF71", fontSize = 20.sp)
                }

                else -> Unit
            }

            if (fitted != null) {
                // ✅ border calé sur la zone FIT réelle
                Canvas(Modifier.matchParentSize()) {
                    val left = fitted.left
                    val top = fitted.top
                    val w = fitted.width
                    val h = fitted.height

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
                    val xDp = with(density) { fitted.left.toDp() } - cornerBadgeOffset
                    val yDp = with(density) { fitted.top.toDp() } - cornerBadgeOffset

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(x = xDp, y = yDp)
                            .size(cornerBadgeSize)
                            .clip(CircleShape)
                            .background(cornerIconTint.copy(alpha = overlayAlpha)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = cornerIcon,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = overlayAlpha),
                            modifier = Modifier.size(cornerBadgeIconSize)
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
            Text("\uD83C\uDF71", fontSize = 20.sp)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FridgeItemThumbnail(
    item: ItemEntity,
    size: Dp = 56.dp,
    modifier: Modifier = Modifier,
    selectionMode: Boolean = false,
    selected: Boolean = false,
    dimAlpha: Float = 0f,
    onClick: () -> Unit,
    onLongPress: () -> Unit = {},
    alignBottom: Boolean = true,
    soonDays: Int = 2,
    compact: Boolean = false,
) {
    val expiryPolicy = remember(soonDays) { ExpiryPolicy(soonDays = soonDays) }

    val level = remember(item.id, item.expiryDate, expiryPolicy.soonDays) {
        expiryLevel(item.expiryDate, expiryPolicy)
    }

    val glowColor = expiryGlowColor(level)
    val selectionBorderColor = expirySelectionBorderColor(level)

    val cornerIcon = when (level) {
        ExpiryLevel.EXPIRED -> Icons.Filled.WarningAmber
        ExpiryLevel.SOON -> Icons.Outlined.TimerOff
        else -> null
    }

    val multiDimOverlay = if (selectionMode && !selected) 0.55f else 0f
    val finalDimAlpha = maxOf(dimAlpha, multiDimOverlay)

    val effectiveImageUrl = rememberEffectiveItemImageUrl(item)

    val cornerBadgeSize = if (compact) 12.dp else 15.dp
    val cornerBadgeIconSize = if (compact) 9.dp else 11.dp
    val cornerBadgeOffset = if (compact) 3.dp else 4.dp
    val borderW = if (compact) 1.5.dp else 2.dp

    Box(
        modifier = modifier
            .size(size)
            .combinedClickable(onClick = onClick, onLongClick = onLongPress),
        contentAlignment = Alignment.Center
    ) {
        ItemThumbnail(
            imageUrl = effectiveImageUrl,
            modifier = Modifier.fillMaxSize(),
            alignBottom = alignBottom,
            // ✅ on ne passe un tint que si SOON/EXPIRED (sinon pas d'overlay inutile)
            cornerIconTint = if (cornerIcon != null) glowColor else null,
            cornerIcon = cornerIcon,
            dimAlpha = if (selected) 0f else finalDimAlpha,
            showImageBorder = selected,
            imageBorderColor = selectionBorderColor,
            imageBorderWidth = borderW,
            cornerBadgeSize = cornerBadgeSize,
            cornerBadgeIconSize = cornerBadgeIconSize,
            cornerBadgeOffset = cornerBadgeOffset,
            topRightOverlayOnImage = null
        )
    }
}

fun effectiveItemImageUrl(
    context: Context,
    item: ItemEntity,
): String? {
    val fallback = item.imageUrl

    // On ne touche qu'aux ajouts "manual"
    if (item.addMode != "manual") return fallback

    val type = item.manualType?.trim().orEmpty()
    val subtype = item.manualSubtype?.trim().orEmpty()
    val pkg = context.packageName

    // 1) Sous-type (si applicable)
    if (type in MANUAL_TYPES_WITH_SUBTYPE_IMAGE && subtype.isNotBlank()) {
        val resId = ManualTaxonomyImageResolver.resolveSubtypeDrawableResId(
            context = context,
            subtypeCode = subtype
        )
        if (resId != 0) return "android.resource://$pkg/$resId"
    }

    // 2) Type
    if (type.isNotBlank()) {
        val resId = ManualTaxonomyImageResolver.resolveTypeDrawableResId(
            context = context,
            typeCode = type
        )
        if (resId != 0) return "android.resource://$pkg/$resId"
    }

    // 3) Fallback
    return fallback
}

@Composable
fun rememberEffectiveItemImageUrl(item: ItemEntity): String? {
    val context = LocalContext.current
    val pkg = context.packageName

    // ✅ mêmes keys que tes 3 implémentations → mêmes triggers de recalcul
    return remember(item.addMode, item.manualType, item.manualSubtype, item.imageUrl, pkg) {
        effectiveItemImageUrl(context, item)
    }
}
