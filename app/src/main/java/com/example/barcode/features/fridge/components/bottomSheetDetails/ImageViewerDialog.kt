package com.example.barcode.features.fridge.components.bottomSheetDetails

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import kotlin.math.abs
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import kotlinx.coroutines.launch
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize

public enum class ViewerImageKind {
    Preview,
    Ingredients,
    Nutrition
}

public data class ViewerImage(
    val kind: ViewerImageKind,
    val url: String
) {
    val title: String
        get() = when (kind) {
            ViewerImageKind.Preview -> "Aperçu"
            ViewerImageKind.Ingredients -> "Ingrédients"
            ViewerImageKind.Nutrition -> "Nutrition"
        }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
public fun ImageViewerDialog(
    images: List<ViewerImage>,
    startIndex: Int = 0,
    onDismiss: () -> Unit
) {
    if (images.isEmpty()) return

    val pagerState = rememberPagerState(
        initialPage = startIndex.coerceIn(0, images.lastIndex),
        pageCount = { images.size }
    )

    // ✅ Désactive le swipe quand on est zoomé (sinon c'est vite relou)
    val pageScales = remember { mutableStateMapOf<Int, Float>() }
    val currentScale = pageScales[pagerState.currentPage] ?: 1f
    val canSwipe = currentScale <= 1.02f
    val scope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = canSwipe
            ) { page ->
                ZoomableImagePage(
                    url = images[page].url,
                    onScaleChanged = { pageScales[page] = it }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ScrollableTabRow(
                        selectedTabIndex = pagerState.currentPage,
                        modifier = Modifier.weight(1f),
                        containerColor = Color.Transparent,
                        contentColor = Color.White,
                        edgePadding = 0.dp,
                        divider = {}
                    ) {
                        images.forEachIndexed { index, img ->
                            val selected = pagerState.currentPage == index

                            Tab(
                                selected = selected,
                                onClick = {
                                    scope.launch { pagerState.animateScrollToPage(index) }
                                },
                                text = {
                                    Text(
                                        text = img.title,
                                        maxLines = 1,
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                        color = Color.White.copy(alpha = if (selected) 0.95f else 0.65f),
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                }
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Fermer", tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun ZoomableImagePage(
    url: String,
    onScaleChanged: (Float) -> Unit
) {
    var scale by remember(url) { mutableStateOf(1f) }
    var rotation by remember(url) { mutableStateOf(0f) }
    var offset by remember(url) { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

    val state = rememberTransformableState { zoomChange, panChange, rotationChange ->
        scale = (scale * zoomChange).coerceIn(1f, 8f)
        rotation += rotationChange
        offset += panChange

        if (abs(scale - 1f) < 0.03f) {
            scale = 1f
            rotation = 0f
            offset = androidx.compose.ui.geometry.Offset.Zero
        }

        onScaleChanged(scale)
    }

    // pour pas moove infini dans le vide quand zoomed
    var containerSize by remember(url) { mutableStateOf(IntSize.Zero) }
    var imageSize by remember(url) { mutableStateOf(IntSize.Zero) }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged { containerSize = it }
    ) {
        val painter = rememberAsyncImagePainter(url)
        val pState = painter.state

        if (pState !is AsyncImagePainter.State.Error) {
            Image(
                painter = painter,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
                    .onGloballyPositioned { coords ->
                        imageSize = coords.size
                    }
                    .pointerInput(url) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)

                            var transforming = false

                            do {
                                val event = awaitPointerEvent()

                                val pressedCount = event.changes.count { it.pressed }
                                val zoom = event.calculateZoom()
                                val pan = event.calculatePan()
                                val rot = 0f

                                // ✅ On ne "prend la main" QUE si:
                                // - multi-touch (pinch), ou
                                // - déjà zoomé (permet pan)
                                if (!transforming) {
                                    transforming = pressedCount >= 2 || scale > 1f
                                }

                                if (transforming) {
                                    val newScale = (scale * zoom).coerceIn(1f, 8f)
                                    scale = newScale
                                    rotation += rot
                                    offset = (offset + pan).clampToBounds(
                                        containerSize = containerSize,
                                        contentSize = imageSize,
                                        scale = scale
                                    )

                                    if (kotlin.math.abs(scale - 1f) < 0.03f) {
                                        scale = 1f
                                        rotation = 0f
                                        offset = androidx.compose.ui.geometry.Offset.Zero
                                    }

                                    onScaleChanged(scale)

                                    // ✅ Ici on consomme, sinon le Pager swipe en même temps
                                    event.changes.forEach { it.consume() }
                                }

                            } while (event.changes.any { it.pressed })
                        }
                    }
                    .graphicsLayer {
                        translationX = offset.x
                        translationY = offset.y
                        scaleX = scale
                        scaleY = scale
                        rotationZ = rotation
                    }
            )
        }

        if (pState is AsyncImagePainter.State.Loading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(36.dp),
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }

        if (pState is AsyncImagePainter.State.Error) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Image,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.65f),
                    modifier = Modifier.size(64.dp)
                )
            }
        }
    }
}

private fun androidx.compose.ui.geometry.Offset.clampToBounds(
    containerSize: IntSize,
    contentSize: IntSize,
    scale: Float
): androidx.compose.ui.geometry.Offset {
    if (containerSize.width == 0 || containerSize.height == 0) return this
    if (contentSize.width == 0 || contentSize.height == 0) return this

    val scaledW = contentSize.width * scale
    val scaledH = contentSize.height * scale

    val maxX = ((scaledW - containerSize.width) / 2f).coerceAtLeast(0f)
    val maxY = ((scaledH - containerSize.height) / 2f).coerceAtLeast(0f)

    val clampedX = x.coerceIn(-maxX, maxX)
    val clampedY = y.coerceIn(-maxY, maxY)

    return androidx.compose.ui.geometry.Offset(clampedX, clampedY)
}

