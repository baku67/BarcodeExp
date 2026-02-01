package com.example.barcode.features.fridge.components.bottomSheetDetails

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.barcode.data.local.entities.ItemEntity
import com.example.barcode.features.fridge.isExpired
import com.example.barcode.features.fridge.isSoon




// BOTTOM SHEET 1/2:
@Composable
public fun ItemDetailsBottomSheet(
    itemEntity: ItemEntity,
    onClose: () -> Unit,
    onOpenViewer: (String) -> Unit,
    onEdit: (ItemEntity) -> Unit = {},
    onRemove: (ItemEntity) -> Unit = {},
    onAddToFavorites: (ItemEntity) -> Unit = {},
    onAddToShoppingList: (ItemEntity) -> Unit = {},
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomSheetMenuButton(
                    onEdit = { onEdit(itemEntity) },
                    onRemove = { onRemove(itemEntity) },
                    onAddToFavorites = { onAddToFavorites(itemEntity) },
                    onAddToShoppingList = { onAddToShoppingList(itemEntity) },
                )
            }

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


