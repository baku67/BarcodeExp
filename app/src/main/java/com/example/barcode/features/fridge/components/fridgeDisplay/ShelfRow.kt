package com.example.barcode.features.fridge.components.fridgeDisplay

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.TimerOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.barcode.data.local.entities.ItemEntity
import com.example.barcode.features.fridge.components.shared.ItemThumbnail
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalDensity
import com.example.barcode.common.expiry.ExpiryLevel
import com.example.barcode.common.expiry.ExpiryPolicy
import com.example.barcode.common.expiry.expiryLevel
import com.example.barcode.common.ui.expiry.expiryGlowColor
import com.example.barcode.common.ui.expiry.expirySelectionBorderColor
import com.example.barcode.common.ui.theme.ItemNote

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShelfRow(
    index: Int,
    itemEntities: List<ItemEntity>,
    notesCountByItemId: Map<String, Int> = emptyMap(),
    selectionMode: Boolean,
    selectedIds: Set<String>,
    onClickItem: (ItemEntity) -> Unit,
    onLongPressItem: (ItemEntity) -> Unit,
    dimAlpha: Float = 0f, // pour anim allumage frigo
    selectedSheetId: String? = null,
    emptyOpacity: Float = 1f,
    emptyCenterLabel: String? = null
) {
    // TODO: branche soonDays sur tes Settings
    val expiryPolicy = remember { ExpiryPolicy(soonDays = 2) }

    val preset = when (index) {
        0 -> ShelfPreset.TOP1
        1 -> ShelfPreset.TOP2
        2 -> ShelfPreset.MID
        3 -> ShelfPreset.BOTTOM1
        4 -> ShelfPreset.BOTTOM2
        else -> ShelfPreset.BOTTOM2 // ✅ étagères supplémentaires = la plus profonde BOTTOM2
    }
    val spec = shelfSpec(preset)

    val productDrop = when (preset) {
        ShelfPreset.TOP1 -> 7.dp
        ShelfPreset.TOP2 -> 5.dp
        ShelfPreset.MID -> 1.dp
        ShelfPreset.BOTTOM1 -> 5.dp
        ShelfPreset.BOTTOM2 -> 8.dp
    }

    // Hauteur de la rangée = produits + étagère (le drop ne change pas la hauteur)
    val productSize = 56.dp
    val rowHeight = productSize + spec.height

    val cs = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(rowHeight)
    ) {
        val productsOnTop = (spec.view == ShelfView.BOTTOM)

        // --- Produits
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = 12.dp)
                .offset(y = productDrop)
                .zIndex(if (productsOnTop) 1f else 0f),
            horizontalArrangement = Arrangement.spacedBy(3.dp), // espacement produits
            verticalAlignment = Alignment.Bottom
        ) {
            itemEntities.forEachIndexed { itemIndex, item ->

                val level = remember(item.id, item.expiryDate, expiryPolicy.soonDays) {
                    expiryLevel(item.expiryDate, expiryPolicy)
                }

                val isSheetSelected =
                    selectedSheetId != null && item.id == selectedSheetId // surbrillance pendant BottomSheetDetails

                val hasSheetSelection = selectedSheetId != null
                val dimOthers = hasSheetSelection && !isSheetSelected



                val isMultiSelected = selectionMode && selectedIds.contains(item.id)
                // ✅ Même rendu que la sélection “single” (BottomSheet) : bordure calée à l'image
                val isVisuallySelected = isSheetSelected || isMultiSelected

                val sheetOtherAlpha by animateFloatAsState(
                    targetValue = if (dimOthers) 0.55f else 1f,
                    animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                    label = "sheetOtherAlpha"
                )

                val sheetDimOverlay by animateFloatAsState(
                    targetValue = if (dimOthers) 0.50f else 0f,
                    animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                    label = "sheetDimOverlay"
                )
                val effectiveDim = maxOf(dimAlpha, sheetDimOverlay)

                val multiDimOverlay by animateFloatAsState(
                    targetValue = if (selectionMode && !isMultiSelected) 0.55f else 0f,
                    animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                    label = "multiDimOverlay"
                )

// ✅ dim global frigo (allumage) + dim “sheet” + dim “multi”
                val finalDimAlpha = maxOf(dimAlpha, sheetDimOverlay, multiDimOverlay)

                val dimForMultiSelect = selectionMode && !isMultiSelected
                val multiAlpha by animateFloatAsState(
                    targetValue = if (dimForMultiSelect) 0.45f else 1f,
                    animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                    label = "multiSelectAlpha"
                )

                val glowColor = expiryGlowColor(level)
                val selectionBorderColor = expirySelectionBorderColor(level)

                var imageLoaded by remember(item.id) { mutableStateOf(false) }

                val noteCount = notesCountByItemId[item.id] ?: 0

                Box(
                    modifier = Modifier
                        .size(productSize),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    val cornerIcon = when (level) {
                        ExpiryLevel.EXPIRED -> Icons.Filled.WarningAmber
                        ExpiryLevel.SOON -> Icons.Outlined.TimerOff
                        else -> null
                    }

                    val shouldGiggle =
                        !selectionMode &&
                                imageLoaded &&
                                (level == ExpiryLevel.EXPIRED || level == ExpiryLevel.SOON)

                    val selectedScale by animateFloatAsState(
                        targetValue = if (isSheetSelected) 1.07f else 1f,
                        animationSpec = tween(durationMillis = 260),
                        label = "selectedScale"
                    )

                    val itemAlpha =
                        when {
                            isSheetSelected -> 1f
                            selectionMode -> multiAlpha
                            else -> sheetOtherAlpha
                        }

                    val density = LocalDensity.current
                    val liftTargetPx = with(density) { 2.dp.toPx() } // décalage vers le haut lors selectItem (pour compenser MID border bottom)
                    val liftPx by animateFloatAsState(
                        targetValue = if (isVisuallySelected) liftTargetPx else 0f,
                        animationSpec = tween(durationMillis = 260),
                        label = "liftPx"
                    )

                    val wrapperModifier = Modifier
                        .fillMaxSize()
                        .zIndex(if (isSheetSelected) 2f else 0f)
                        .graphicsLayer {
                            transformOrigin = TransformOrigin(0.5f, 1f)
                            scaleX = selectedScale
                            scaleY = selectedScale
                            translationY = -liftPx
                        }
                        .giggleEvery(
                            enabled = shouldGiggle && !isSheetSelected,
                            intervalMs = 4_200L,
                            initialDelayMs = 500L + itemIndex * 90L
                        )
                        .combinedClickable(
                            onClick = { onClickItem(item) },
                            onLongClick = { onLongPressItem(item) }
                        )

                    Box(
                        modifier = Modifier.size(productSize),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(modifier = wrapperModifier) {

                            ItemThumbnail(
                                imageUrl = item.imageUrl,
                                alignBottom = true,
                                cornerIcon = cornerIcon,
                                cornerIconTint = glowColor,
                                onImageLoaded = { imageLoaded = it },
                                dimAlpha = if (isSheetSelected) 0f else finalDimAlpha,
                                showImageBorder = isVisuallySelected,
                                imageBorderColor = selectionBorderColor,
                                imageBorderWidth = 2.dp,
                                modifier = Modifier.fillMaxSize(),

                                // ✅ DogEar notes
                                topRightOverlayOnImage = if (noteCount > 0) {
                                    {
                                        NotesDogEarIndicator(
                                            modifier = Modifier
                                                // ✅ fait dépasser un peu à l'extérieur (droite + haut)
                                                .offset(x = 2.dp, y = (-2).dp)
                                                .zIndex(5f),
                                            showPenIcon = true
                                        )
                                    }
                                } else null
                            )
                        }
                    }
                }
            }

            repeat(5 - itemEntities.size) { Spacer(Modifier.size(productSize)) }
        }

        // --- Étagère
        ShelfRowTrapezoid(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .zIndex(if (productsOnTop) 0f else 1f)
                .alpha(emptyOpacity),
            height = spec.height,
            insetTop = spec.insetTop,
            lipHeight = spec.lipHeight,
            view = spec.view,
            lipAlpha = spec.lipAlpha,
            dimAlpha = dimAlpha
        )

        // ✅ Message "liste vide" intégré au design (uniquement étagère MID)
        if (emptyCenterLabel != null && itemEntities.isEmpty() && preset == ShelfPreset.MID) {
            Text(
                text = emptyCenterLabel,
                style = MaterialTheme.typography.titleMedium,
                color = cs.onSurface.copy(alpha = 0.72f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = (-16).dp)
                    .alpha((emptyOpacity * 1.15f).coerceIn(0f, 1f))
            )
        }
    }
}


private val NotesDogEarShape = GenericShape { size, _ ->
    // Triangle collé au coin haut-droit (effet "coin de page")
    moveTo(size.width, 0f)          // haut-droit
    lineTo(size.width, size.height) // bas-droit
    lineTo(0f, 0f)                  // haut-gauche
    close()
}


@Composable
private fun NotesDogEarIndicator(
    modifier: Modifier = Modifier,
    showPenIcon: Boolean = true,
) {
    val baseColor = ItemNote

    // Sur un fond clair, on passe en contrast “dark”
    val foldColor = Color(0xFF1F1F1F).copy(alpha = 0.06f)
    val lineColor = Color(0xFF1F1F1F).copy(alpha = 0.14f)

    Box(
        modifier = modifier
            .size(16.dp)
            .clip(NotesDogEarShape)
            .background(baseColor)
            .border(0.7.dp, lineColor, NotesDogEarShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val foldPath = Path().apply {
                moveTo(w, 0f)
                lineTo(w, h * 0.62f)
                lineTo(w * 0.38f, 0f)
                close()
            }
            drawPath(foldPath, color = foldColor)

            drawLine(
                color = lineColor.copy(alpha = 0.22f),
                start = Offset(0f, 0f),
                end = Offset(w, h),
                strokeWidth = 1.dp.toPx()
            )
        }

        if (showPenIcon) {
            Icon(
                imageVector = Icons.Outlined.StickyNote2,
                contentDescription = "Notes",
                tint = Color(0xFF1F1F1F).copy(alpha = 0.78f),
                modifier = Modifier
                    .offset(x = 3.dp, y = (-3).dp)
                    .size(10.dp)
            )
        }
    }
}




@Composable
private fun Modifier.giggleEvery(
    enabled: Boolean,
    intervalMs: Long = 5_000L,
    initialDelayMs: Long = 320L, // micro délai pour laisser l’onglet “se poser”
): Modifier {
    if (!enabled) return this

    val rotation = remember { Animatable(0f) }
    val tx = remember { Animatable(0f) }
    val ty = remember { Animatable(0f) }
    val scale = remember { Animatable(1f) }

    suspend fun burst() = coroutineScope {
        val d = 110 // ✅ plus long (au lieu de 70)

        // ✅ rotation plus intense
        launch {
            rotation.animateTo(3.4f, tween(d))
            rotation.animateTo(-3.0f, tween(d))
            rotation.animateTo(2.1f, tween(d))
            rotation.animateTo(-1.6f, tween(d))
            rotation.animateTo(0f, tween(d + 40))
        }

        // ✅ micro shake un peu plus visible (toujours discret)
        launch {
            tx.animateTo(2.4f, tween(d))
            tx.animateTo(-2.0f, tween(d))
            tx.animateTo(1.2f, tween(d))
            tx.animateTo(0f, tween(d + 20))
        }

        launch {
            ty.animateTo(-1.6f, tween(d))
            ty.animateTo(1.1f, tween(d))
            ty.animateTo(-0.7f, tween(d))
            ty.animateTo(0f, tween(d + 20))
        }

        // ✅ scale plus intense + plus long, en même temps
        launch {
            scale.animateTo(1.055f, tween(d + 10))
            scale.animateTo(1f, tween(d + 110))
        }
    }

    LaunchedEffect(enabled) {
        if (!enabled) return@LaunchedEffect

        // ✅ burst au spawn (après micro délai)
        delay(initialDelayMs)
        burst()

        // ✅ ensuite cadence fixe
        while (true) {
            delay(intervalMs)
            burst()
        }
    }

    return this.graphicsLayer {
        rotationZ = rotation.value
        translationX = tx.value
        translationY = ty.value
        scaleX = scale.value
        scaleY = scale.value
    }
}