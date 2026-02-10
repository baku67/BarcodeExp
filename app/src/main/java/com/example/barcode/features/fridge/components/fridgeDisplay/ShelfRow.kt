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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.barcode.features.fridge.isExpired
import com.example.barcode.features.fridge.isSoon
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path

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

    Box(
        modifier = Modifier.Companion
            .fillMaxWidth()
            .height(rowHeight)
    ) {
        val productsOnTop = (spec.view == ShelfView.BOTTOM)

        // --- Produits
        Row(
            modifier = Modifier.Companion
                .align(Alignment.Companion.TopCenter)
                .padding(horizontal = 12.dp)
                .offset(y = productDrop)
                .zIndex(if (productsOnTop) 1f else 0f),
            horizontalArrangement = Arrangement.spacedBy(3.dp), // espacement produits
            verticalAlignment = Alignment.Companion.Bottom
        ) {
            itemEntities.forEachIndexed { itemIndex, item ->

                val isSheetSelected =
                    selectedSheetId != null && item.id == selectedSheetId // pour mettre l'item en surbrillance pendant le BottomSheetDetails

                val hasSheetSelection = selectedSheetId != null
                val dimOthers = hasSheetSelection && !isSheetSelected

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

                val isMultiSelected = selectionMode && selectedIds.contains(item.id)
                // ✅ Même rendu que la sélection “single” (BottomSheet) : bordure calée à l'image
                val isVisuallySelected = isSheetSelected || isMultiSelected

                val dimForMultiSelect = selectionMode && !isMultiSelected
                val multiAlpha by animateFloatAsState(
                    targetValue = if (dimForMultiSelect) 0.45f else 1f,
                    animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                    label = "multiSelectAlpha"
                )

                val glowColor = when {
                    item.expiryDate == null -> null
                    isExpired(item.expiryDate) -> MaterialTheme.colorScheme.tertiary
                    isSoon(item.expiryDate) -> Color(0xFFF9A825)
                    else -> null
                }

                val selectionBorderColor = when {
                    item.expiryDate != null && isExpired(item.expiryDate) -> MaterialTheme.colorScheme.tertiary
                    item.expiryDate != null && isSoon(item.expiryDate) -> Color(0xFFFFC107) // jaune
                    else -> Color(0xFF2ECC71) // vert (ou utilise cs.tertiary si tu préfères un vert "theme")
                }.copy(alpha = 0.95f)

                var imageLoaded by remember(item.id) { mutableStateOf(false) }

                val noteCount = notesCountByItemId[item.id] ?: 0

                Box(
                    modifier = Modifier.Companion
                        .size(productSize),
                    contentAlignment = Alignment.Companion.BottomCenter
                ) {
                    val cornerIcon = when {
                        item.expiryDate == null -> null
                        isExpired(item.expiryDate) -> Icons.Filled.WarningAmber
                        isSoon(item.expiryDate) -> Icons.Outlined.TimerOff
                        else -> null
                    }

                    val shouldGiggle =
                        !selectionMode && // évite que ça bouge pendant la sélection et anim que quand image chargée
                                imageLoaded &&
                                item.expiryDate != null &&
                                (isExpired(item.expiryDate) || isSoon(item.expiryDate))

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

                    val wrapperModifier = Modifier
                        .fillMaxSize()
                        .alpha(itemAlpha)
                        .zIndex(if (isSheetSelected) 2f else 0f)
                        .graphicsLayer {
                            scaleX = selectedScale
                            scaleY = selectedScale
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
                                dimAlpha = when {
                                    isSheetSelected -> 0f
                                    dimForMultiSelect -> 0.55f
                                    else -> effectiveDim
                                },
                                showImageBorder = isVisuallySelected,
                                imageBorderColor = selectionBorderColor,
                                imageBorderWidth = 2.dp,
                                modifier = Modifier.fillMaxSize()
                            )

                            if (noteCount > 0) {
                                NotesDogEarIndicator(
                                    modifier = Modifier.align(Alignment.TopEnd),
                                    // showPenIcon = false // ✅ si tu veux SANS icône
                                )
                            }
                        }
                    }
                }
            }

            repeat(5 - itemEntities.size) { Spacer(Modifier.Companion.size(productSize)) }
        }

        // --- Étagère
        ShelfRowTrapezoid(
            modifier = Modifier.Companion
                .align(Alignment.Companion.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .zIndex(if (productsOnTop) 0f else 1f)
                .alpha(emptyOpacity),
            height = spec.height,
            insetTop = spec.insetTop,
            lipHeight = spec.lipHeight,
            view = spec.view,
            lipAlpha = spec.lipAlpha,
            dimAlpha = dimAlpha // ✅ NEW
        )


        // ✅ Message "liste vide" intégré au design (uniquement étagère MID)
        if (emptyCenterLabel != null && itemEntities.isEmpty() && preset == ShelfPreset.MID) {
            Text(
                text = emptyCenterLabel,
                style = MaterialTheme.typography.titleMedium, // ✅ plus gros que bodyMedium
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f), // ✅ plus visible
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = (-16).dp) // légèrement moins haut
                    .alpha((emptyOpacity * 1.15f).coerceIn(0f, 1f)) // ✅ garde l'effet "ghost", mais moins fade
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
    val baseColor = Color(0xFF1976D2)              // bleu "notes"
    val foldColor = Color.White.copy(alpha = 0.18f) // pli plus clair
    val lineColor = Color.White.copy(alpha = 0.30f) // liseré

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

            // petit "pli" plus clair dans le coin
            val foldPath = Path().apply {
                moveTo(w, 0f)
                lineTo(w, h * 0.62f)
                lineTo(w * 0.38f, 0f)
                close()
            }
            drawPath(foldPath, color = foldColor)

            // diagonale subtile (premium + lisible)
            drawLine(
                color = lineColor.copy(alpha = 0.22f),
                start = Offset(0f, 0f),
                end = Offset(w, h),
                strokeWidth = 1.dp.toPx()
            )
        }

        if (showPenIcon) {
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = "Notes",
                tint = Color.White.copy(alpha = 0.92f),
                modifier = Modifier
                    .offset(x = 1.dp, y = (-1).dp)
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