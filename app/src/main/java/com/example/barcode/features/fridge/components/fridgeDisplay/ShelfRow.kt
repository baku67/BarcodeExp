package com.example.barcode.features.fridge.components.fridgeDisplay

import androidx.compose.animation.core.Animatable
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShelfRow(
    index: Int,
    itemEntities: List<ItemEntity>,
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
        else -> ShelfPreset.MID
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

                val isSelected = selectionMode && selectedIds.contains(item.id)

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

                val mt = MaterialTheme.colorScheme

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

                    ItemThumbnail(
                        imageUrl = item.imageUrl,
                        alignBottom = true,
                        cornerIcon = cornerIcon,
                        cornerIconTint = glowColor,
                        onImageLoaded = { imageLoaded = it },
                        dimAlpha = when {
                            isSheetSelected -> 0f           // le sélectionné reste full bright
                            dimOthers -> 0.5f              // assombrit les autres (AJUSTE ICI)
                            else -> dimAlpha                // sinon: anim globale d’allumage frigo
                        },
                        showImageBorder = isSheetSelected, // ✅ NEW
                        imageBorderColor = selectionBorderColor,
                        imageBorderWidth = 2.dp, // ✅ NEW
                        modifier = Modifier.Companion
                            .fillMaxSize()
                            .alpha(if (dimOthers) 0.92f else 1f)
                            .zIndex(if (isSheetSelected) 2f else 0f)
                            .graphicsLayer {
                                scaleX = selectedScale
                                scaleY = selectedScale
                            }
                            .giggleEvery(
                                enabled = shouldGiggle && !isSheetSelected, // évite 2 anims en même temps
                                intervalMs = 4_200L,
                                initialDelayMs = 500L + itemIndex * 90L
                            )
                            .combinedClickable(
                                onClick = { onClickItem(item) },
                                onLongClick = { onLongPressItem(item) }
                            )
                            .border(
                                width = if (isSelected) 2.dp else 0.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Companion.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .background(
                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Companion.Transparent,
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                            )
                    )
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