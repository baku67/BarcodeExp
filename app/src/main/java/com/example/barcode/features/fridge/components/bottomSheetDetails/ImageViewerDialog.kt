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

            // ✅ Titre au-dessus : "Aperçu" / "Ingrédients" / "Nutrition"
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = images.getOrNull(pagerState.currentPage)?.title.orEmpty(),
                    color = Color.White.copy(alpha = 0.92f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.Center)
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Fermer", tint = Color.White)
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
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
                    .pointerInput(url) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)

                            var transforming = false

                            do {
                                val event = awaitPointerEvent()

                                val pressedCount = event.changes.count { it.pressed }
                                val zoom = event.calculateZoom()
                                val pan = event.calculatePan()
                                val rot = event.calculateRotation()

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
                                    offset += pan

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
