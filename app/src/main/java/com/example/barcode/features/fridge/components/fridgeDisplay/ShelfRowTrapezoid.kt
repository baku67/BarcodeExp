package com.example.barcode.features.fridge.components.fridgeDisplay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ---- ÉTAGÈRE selon index row (5 styles fixes)
enum class ShelfView { TOP, BOTTOM }

enum class ShelfPreset { TOP1, TOP2, MID, BOTTOM1, BOTTOM2 }

data class ShelfSpec(
    val height: Dp,
    val insetTop: Dp,
    val lipHeight: Dp,
    val view: ShelfView,
    val lipAlpha: Float = 1f
)

fun shelfSpec(preset: ShelfPreset): ShelfSpec = when (preset) {

    ShelfPreset.TOP1 -> ShelfSpec(
        height = 16.dp,
        insetTop = 16.dp,
        lipHeight = 1.dp,
        view = ShelfView.TOP,
        lipAlpha = 0.90f
    )

    ShelfPreset.TOP2 -> ShelfSpec(
        height = 11.dp,
        insetTop = 16.dp,
        lipHeight = 1.dp,
        view = ShelfView.TOP,
        lipAlpha = 0.90f
    )

    ShelfPreset.MID -> ShelfSpec(
        height = 2.dp,
        insetTop = 26.dp,
        lipHeight = 1.dp,
        view = ShelfView.TOP,
        lipAlpha = 0.90f
    )


    ShelfPreset.BOTTOM1 -> ShelfSpec(
        height = 10.dp,
        insetTop = 16.dp,
        lipHeight = 1.dp,
        view = ShelfView.BOTTOM,
        lipAlpha = 0.90f
    )

    ShelfPreset.BOTTOM2 -> ShelfSpec(
        height = 16.dp,
        insetTop = 16.dp,
        lipHeight = 1.dp,
        view = ShelfView.BOTTOM,
        lipAlpha = 0.90f
    )
}


@Composable
fun ShelfRowTrapezoid(
    modifier: Modifier = Modifier.Companion,
    height: Dp = 10.dp,
    insetTop: Dp = 18.dp,
    lipHeight: Dp = 2.dp,
    view: ShelfView = ShelfView.TOP,
    lipAlpha: Float = 1f,
    sideStrokeAlpha: Float = 0.28f,   // ✅ alpha des côtés
    sideStrokeWidth: Dp = 1.dp,        // ✅ épaisseur des côtés
    dimAlpha: Float = 0f
) {
    val cs = MaterialTheme.colorScheme

    val dimFactor = (dimAlpha / 0.55f).coerceIn(0f, 1f) // 0..1

    val baseShelf = lerp(cs.primary, cs.surface, 0.76f)
    val baseEdge = cs.primary
    // ✅ on assombrit uniquement les pixels dessinés (pas de rectangle overlay)
    val shelfColor = lerp(baseShelf, Color.Companion.Black, dimFactor * 0.65f)
    val edgeColor = lerp(baseEdge, Color.Companion.Black, dimFactor * 0.55f)


    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val w = size.width
        val h = size.height

        val inset = insetTop.toPx().coerceAtMost(w / 3f)
        val lip = lipHeight.toPx().coerceAtMost(h)
        val sw = sideStrokeWidth.toPx()

        // ✅ Coordonnées des 4 coins du trapèze (hors lèvre)
        val topLeft: Offset
        val topRight: Offset
        val bottomLeft: Offset
        val bottomRight: Offset

        if (view == ShelfView.BOTTOM) {
            // BOTTOM : haut court / bas long
            topLeft = Offset(inset, 0f)
            topRight = Offset(w - inset, 0f)
            bottomLeft = Offset(0f, h - lip)
            bottomRight = Offset(w, h - lip)
        } else {
            // TOP : haut long / bas court
            topLeft = Offset(0f, 0f)
            topRight = Offset(w, 0f)
            bottomLeft = Offset(inset, h - lip)
            bottomRight = Offset(w - inset, h - lip)
        }

        val path = Path().apply {
            moveTo(topLeft.x, topLeft.y)
            lineTo(topRight.x, topRight.y)
            lineTo(bottomRight.x, bottomRight.y)
            lineTo(bottomLeft.x, bottomLeft.y)
            close()
        }

        // Remplissage
        drawPath(path, color = shelfColor)

        // ✅ Bords latéraux (gauche + droite)
        val sideColor = edgeColor.copy(alpha = sideStrokeAlpha)
        drawLine(
            color = sideColor,
            start = topLeft,
            end = bottomLeft,
            strokeWidth = sw
        )
        drawLine(
            color = sideColor,
            start = topRight,
            end = bottomRight,
            strokeWidth = sw
        )

        // Lèvre : haut en TOP, bas en BOTTOM
        val lipY = if (view == ShelfView.BOTTOM) (h - lip) else 0f
        drawRect(
            color = edgeColor.copy(alpha = lipAlpha),
            topLeft = Offset(0f, lipY),
            size = Size(w, lip)
        )
    }
}