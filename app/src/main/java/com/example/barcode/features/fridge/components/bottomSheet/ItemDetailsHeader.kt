package com.example.barcode.features.fridge.components.bottomSheet

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import com.example.barcode.R
import com.example.barcode.data.local.entities.ItemEntity
import com.example.barcode.features.fridge.formatRelativeDaysCompact
import com.example.barcode.features.fridge.isExpired
import com.example.barcode.features.fridge.isSoon
import kotlin.math.abs


@Immutable
private data class ExpiryChipStyle(
    val container: Color,
    val label: Color,
    val border: Color
)

@DrawableRes
private fun nutriScoreRes(score: String?): Int? = when (score?.trim()?.uppercase()) {
    "A" -> R.drawable.nutri_score_a
    "B" -> R.drawable.nutri_score_b
    "C" -> R.drawable.nutri_score_c
    "D" -> R.drawable.nutri_score_d
    "E" -> R.drawable.nutri_score_e
    else -> null
}

@Composable
private fun expiryChipStyle(expiry: Long?): ExpiryChipStyle {
    val cs = MaterialTheme.colorScheme

    if (expiry == null) {
        return ExpiryChipStyle(
            container = cs.surfaceVariant.copy(alpha = 0.55f),
            label = cs.onSurface.copy(alpha = 0.55f),
            border = cs.outlineVariant.copy(alpha = 0.55f)
        )
    }

    return when {
        isExpired(expiry) -> ExpiryChipStyle(
            container = cs.tertiary.copy(alpha = 0.12f),
            label = cs.tertiary.copy(alpha = 0.95f),
            border = cs.tertiary.copy(alpha = 0.35f)
        )

        isSoon(expiry) -> ExpiryChipStyle(
            container = Color(0xFFFFC107).copy(alpha = 0.16f),  // amber
            label = Color(0xFFFFC107).copy(alpha = 0.95f),
            border = Color(0xFFFFC107).copy(alpha = 0.40f)
        )

        else -> ExpiryChipStyle(
            container = cs.primary.copy(alpha = 0.10f),
            label = cs.primary.copy(alpha = 0.95f),
            border = cs.primary.copy(alpha = 0.30f)
        )
    }
}






// BOTTOM SHEET 1/2:
@Composable
public fun ItemDetailsBottomSheet(
    itemEntity: ItemEntity,
    onClose: () -> Unit,
    onOpenViewer: (String) -> Unit
) {
    val strokeColor by animateColorAsState(
        targetValue = expiryStrokeColor(itemEntity.expiryDate),
        label = "sheetStrokeColor"
    )

    Column(Modifier.fillMaxWidth()) {

        CornerRadiusEtPoignee(
            radius = 28.dp,
            strokeWidth = 2.dp,
            strokeColor = strokeColor, // couleur calculée en fonction exp Item
            handleHeight = 4.dp
        )

        Spacer(Modifier.height(5.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ItemDetailsHeader(
                itemEntity = itemEntity,
                onClose = onClose,
                onOpenViewer = onOpenViewer
            )

            DetailsOpenImageButtons(
                ingredientsUrl = itemEntity.imageIngredientsUrl,
                nutritionUrl = itemEntity.imageNutritionUrl,
                onOpenViewer = onOpenViewer
            )

        }
    }
}



@Composable
private fun DetailsOpenImageButtons(
    ingredientsUrl: String?,
    nutritionUrl: String?,
    onOpenViewer: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        DetailsTabButton(
            text = "Ingrédients",
            icon = Icons.Outlined.Science,
            selected = false,
            enabled = !ingredientsUrl.isNullOrBlank(),
            onClick = { ingredientsUrl?.let(onOpenViewer) },
            modifier = Modifier.weight(1f)
        )

        DetailsTabButton(
            text = "Nutrition",
            icon = Icons.Outlined.FactCheck,
            selected = false,
            enabled = !nutritionUrl.isNullOrBlank(),
            onClick = { nutritionUrl?.let(onOpenViewer) },
            modifier = Modifier.weight(1f)
        )
    }
}



// Couleur du border top BottomSheet
@Composable
private fun expiryStrokeColor(expiry: Long?): Color {
    val base = MaterialTheme.colorScheme.primary

    if (expiry == null) return base.copy(alpha = 0.35f)

    return when {
        isExpired(expiry) -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.65f) // expiré
        isSoon(expiry) -> Color(0xFFFFC107).copy(alpha = 0.45f) // bientôt (jaune)
        else -> base.copy(alpha = 0.55f) // ok
    }
}




@Composable
fun TopRoundedStroke(
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 2.dp,
    radius: Dp = 28.dp,
    color: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
    edgeFadePct: Float = 0.05f // 5%
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(radius)
    ) {
        val w = size.width
        val sw = strokeWidth.toPx()
        val r = radius.toPx().coerceAtMost(w / 2f)
        val y = sw / 2f

        val leftRect = androidx.compose.ui.geometry.Rect(0f, y, 2 * r, 2 * r + y)
        val rightRect = androidx.compose.ui.geometry.Rect(w - 2 * r, y, w, 2 * r + y)

        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(0f, r + y)
            arcTo(leftRect, 180f, 90f, false)
            lineTo(w - r, y)
            arcTo(rightRect, 270f, 90f, false)
        }

        val fade = edgeFadePct.coerceIn(0f, 0.49f)

        // ✅ Brush: transparent -> color -> transparent
        val brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
            colorStops = arrayOf(
                0f to color.copy(alpha = 0f),
                fade to color,
                (1f - fade) to color,
                1f to color.copy(alpha = 0f)
            ),
            startX = 0f,
            endX = w
        )

        drawPath(
            path = path,
            brush = brush,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = sw,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round
            )
        )
    }
}





@Composable
private fun CornerRadiusEtPoignee(
    modifier: Modifier = Modifier,
    radius: Dp = 28.dp,
    strokeWidth: Dp = 2.dp,
    strokeColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
    handleWidth: Dp = 44.dp,
    handleHeight: Dp = 4.dp
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(radius) // ✅ la zone de rayon sert à la fois au trait ET à la poignée
    ) {
        // ✅ Trait arrondi
        TopRoundedStroke(
            modifier = Modifier.matchParentSize(),
            strokeWidth = strokeWidth,
            radius = radius,
            color = strokeColor
        )

        // ✅ Poignée DANS la zone (pas en dessous)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp) // ajuste visuellement
                .width(handleWidth)
                .height(handleHeight)
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f))
        )
    }
}










@Composable
private fun DetailsTabButton(
    text: String,
    icon: ImageVector? = null,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = when {
        !enabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        else -> MaterialTheme.colorScheme.surface
    }

    val border = when {
        !enabled -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
        selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.80f)
    }

    val content = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f)
        selected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f)
    }

    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = content,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
            }

            Text(text, fontWeight = FontWeight.SemiBold, color = content)
        }
    }
}









// Viewer d'image du BottomSheet:
@Composable
public fun ImageViewerDialog(
    url: String,
    onDismiss: () -> Unit
) {
    // zoom/pan/rotation
    var scale by remember(url) { mutableStateOf(1f) }
    var rotation by remember(url) { mutableStateOf(0f) }
    var offset by remember(url) { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

    val state = rememberTransformableState { zoomChange, panChange, rotationChange ->
        scale = (scale * zoomChange).coerceIn(1f, 8f)
        rotation += rotationChange
        offset += panChange

        // si on revient proche de 1x, on “recentre” (évite de perdre l’image)
        if (abs(scale - 1f) < 0.03f) {
            scale = 1f
            rotation = 0f
            offset = androidx.compose.ui.geometry.Offset.Zero
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            val painter = rememberAsyncImagePainter(url)
            val pState = painter.state

            // scrim cliquable pour fermer (optionnel, mais UX top)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.92f)
                    .clickable(enabled = pState !is AsyncImagePainter.State.Loading) { onDismiss() }
            )

            // image interactive (affichée seulement si pas en erreur)
            if (pState !is AsyncImagePainter.State.Error) {
                Image(
                    painter = painter,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp)
                        .transformable(state = state, lockRotationOnZoomPan = false)
                        .graphicsLayer {
                            translationX = offset.x
                            translationY = offset.y
                            scaleX = scale
                            scaleY = scale
                            rotationZ = rotation
                        }
                )
            }

            // ✅ Loader overlay (gris/blanc, discret)
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

            // ✅ Fallback (si erreur)
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

            // bouton fermer
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Fermer", tint = Color.White)
            }
        }
    }
}







@Composable
fun ItemDetailsHeader(
    itemEntity: ItemEntity,
    onClose: () -> Unit,
    onOpenViewer: (String) -> Unit
) {
    val name = itemEntity.name?.takeIf { it.isNotBlank() } ?: "(sans nom)"
    val brand = itemEntity.brand?.takeIf { it.isNotBlank() } ?: "—"
    val nutriScore = itemEntity.nutriScore?.takeIf { it.isNotBlank() } ?: "—"
    val daysText = itemEntity.expiryDate?.let { formatRelativeDaysCompact(it) } ?: "—"

    val chip = expiryChipStyle(itemEntity.expiryDate)

    Box(Modifier.fillMaxWidth()) {

        // ✅ Contenu header normal
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Image
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable(enabled = !itemEntity.imageUrl.isNullOrBlank()) {
                        onOpenViewer(itemEntity.imageUrl!!)
                    }
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (!itemEntity.imageUrl.isNullOrBlank()) {
                    Image(
                        painter = rememberAsyncImagePainter(itemEntity.imageUrl),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text("🧺", fontSize = 22.sp)
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = name.replaceFirstChar { it.titlecase() }, // Majuscule
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = brand,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val nutriRes = nutriScoreRes(itemEntity.nutriScore)

                    if (nutriRes != null) {
                        Image(
                            painter = painterResource(nutriRes),
                            contentDescription = "Nutri-Score ${itemEntity.nutriScore}",
                            modifier = Modifier.height(22.dp)
                        )
                    } else {
                        Image(
                            painter = painterResource(R.drawable.nutri_score_neutre),
                            contentDescription = "Nutri-Score indisponible",
                            modifier = Modifier
                                .height(22.dp)
                                .alpha(0.35f)
                        )
                    }

                    Spacer(Modifier.weight(1f))

                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = { Text(daysText, fontWeight = FontWeight.SemiBold) },
                        colors = AssistChipDefaults.assistChipColors(
                            disabledContainerColor = chip.container,
                            disabledLabelColor = chip.label
                        ),
                        border = BorderStroke(1.dp, chip.border)
                    )
                }
            }
        }
    }
}