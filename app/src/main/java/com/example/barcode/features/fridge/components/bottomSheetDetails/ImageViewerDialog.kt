package com.example.barcode.features.fridge.components.bottomSheetDetails

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

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

    // Désactive le swipe quand on est zoomé
    val pageScales = remember { mutableStateMapOf<Int, Float>() }
    val currentScale = pageScales[pagerState.currentPage] ?: 1f
    val canSwipe = currentScale <= 1.02f

    val scope = rememberCoroutineScope()

    // Palette “premium” (soft, pas noir pur)
    val bgBrush = remember {
        Brush.verticalGradient(
            listOf(
                Color(0xFF0B0D12),
                Color(0xFF000000)
            )
        )
    }
    val headerColor = Color(0xFF101218) // opaque
    val dividerColor = Color.White.copy(alpha = 0.08f)
    val indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.95f)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(bgBrush)
        ) {
            // ✅ Header opaque, prend sa place (réduit l’image)
            Surface(
                color = headerColor,
                tonalElevation = 0.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(start = 12.dp, end = 6.dp), // ✅ pas de padding vertical => indicateur collé en bas
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ScrollableTabRow(
                            selectedTabIndex = pagerState.currentPage,
                            modifier = Modifier.weight(1f),
                            containerColor = Color.Transparent,
                            contentColor = Color.White,
                            edgePadding = 0.dp,
                            divider = {},
                            indicator = { tabPositions ->
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier
                                        .tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                                    height = 3.dp,
                                    color = indicatorColor
                                )
                            }
                        ) {
                            images.forEachIndexed { index, img ->
                                val selected = pagerState.currentPage == index
                                Tab(
                                    selected = selected,
                                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                                    text = {
                                        Text(
                                            text = img.title,
                                            maxLines = 1,
                                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                            color = Color.White.copy(alpha = if (selected) 0.95f else 0.62f),
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                    }
                                )
                            }
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Fermer",
                                tint = Color.White
                            )
                        }
                    }

                    HorizontalDivider(color = dividerColor, thickness = 1.dp)
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                userScrollEnabled = canSwipe
            ) { page ->
                ZoomableImagePage(
                    url = images[page].url,
                    onScaleChanged = { pageScales[page] = it }
                )
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
    var offset by remember(url) { mutableStateOf(Offset.Zero) }

    // ✅ Source de vérité pour le toggle double-tap (sinon “re-double-tap pendant anim” peut rater)
    var isZoomed by remember(url) { mutableStateOf(false) }

    var containerSize by remember(url) { mutableStateOf(IntSize.Zero) }
    var imageSize by remember(url) { mutableStateOf(IntSize.Zero) }

    val scope = rememberCoroutineScope()
    var zoomJob by remember(url) { mutableStateOf<Job?>(null) }

    fun animateZoom(targetScale: Float, targetOffset: Offset) {
        zoomJob?.cancel()
        zoomJob = scope.launch {
            val specScale = tween<Float>(durationMillis = 220, easing = FastOutSlowInEasing)
            val specOffset = tween<Offset>(durationMillis = 220, easing = FastOutSlowInEasing)

            val scaleAnim = Animatable(scale)
            val offsetAnim = Animatable(offset, Offset.VectorConverter)

            launch {
                scaleAnim.animateTo(targetScale, animationSpec = specScale) {
                    scale = value
                    onScaleChanged(scale)
                }
            }
            launch {
                offsetAnim.animateTo(targetOffset, animationSpec = specOffset) {
                    offset = value
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
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
                    .onGloballyPositioned { coords -> imageSize = coords.size }

                    // ✅ Double-tap animé : zoom <-> reset (et re-double-tap pendant anim = OK)
                    .pointerInput(url) {
                        detectTapGestures(
                            onDoubleTap = {
                                val targetScale = if (!isZoomed) 2.5f else 1f
                                val targetOffset = Offset.Zero

                                // toggle immédiat
                                isZoomed = !isZoomed

                                animateZoom(targetScale, targetOffset)
                            }
                        )
                    }

                    // ✅ Pinch/drag immédiat (ne casse pas les taps)
                    .pointerInput(url) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)

                            var transforming = false
                            val panThreshold = viewConfiguration.touchSlop / 3f

                            do {
                                val event = awaitPointerEvent()

                                val pressedCount = event.changes.count { it.pressed }
                                val zoom = event.calculateZoom()
                                val pan = event.calculatePan()
                                val panDistance = pan.getDistance()

                                // ✅ Pan 1 doigt seulement si on bouge vraiment, sinon ça laisse passer les double-taps
                                val wantsOneFingerPan = scale > 1.02f && panDistance > panThreshold

                                if (!transforming) {
                                    transforming = pressedCount >= 2 || wantsOneFingerPan
                                }

                                if (transforming) {
                                    // Interaction > animation
                                    zoomJob?.cancel()

                                    val newScale = (scale * zoom).coerceIn(1f, 8f)
                                    scale = newScale

                                    offset = (offset + pan).clampToBounds(
                                        containerSize = containerSize,
                                        contentSize = imageSize,
                                        scale = scale
                                    )

                                    if (abs(scale - 1f) < 0.03f) {
                                        scale = 1f
                                        offset = Offset.Zero
                                    }

                                    isZoomed = scale > 1.02f

                                    onScaleChanged(scale)
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

private fun Offset.clampToBounds(
    containerSize: IntSize,
    contentSize: IntSize,
    scale: Float
): Offset {
    if (containerSize.width == 0 || containerSize.height == 0) return this
    if (contentSize.width == 0 || contentSize.height == 0) return this

    val scaledW = contentSize.width * scale
    val scaledH = contentSize.height * scale

    val maxX = ((scaledW - containerSize.width) / 2f).coerceAtLeast(0f)
    val maxY = ((scaledH - containerSize.height) / 2f).coerceAtLeast(0f)

    return Offset(
        x = x.coerceIn(-maxX, maxX),
        y = y.coerceIn(-maxY, maxY)
    )
}
