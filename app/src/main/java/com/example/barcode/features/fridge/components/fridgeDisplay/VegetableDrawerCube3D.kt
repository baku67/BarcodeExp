package com.example.barcode.features.fridge.components.fridgeDisplay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun VegetableDrawerCube3D(
    modifier: Modifier = Modifier.Companion,
    height: Dp = 92.dp,
    depth: Dp = 16.dp,                 // profondeur du "toit"
    corner: Dp = 14.dp,                // arrondi bas uniquement (face avant)
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
    dimAlpha: Float = 0f, // ✅ NEW : dim de l’allumage frigo
    isGhost: Boolean = false,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val cs = MaterialTheme.colorScheme

    val ghostFactor = if (isGhost) 0.55f else 0f

    val dimFactor = (dimAlpha / 0.55f).coerceIn(0f, 1f) // 0..1 (même échelle que les étagères)

    val baseFace = lerp(cs.primary, cs.surface, 0.76f)
    val baseStroke = cs.primary.copy(alpha = 0.75f)

    // ✅ 1) GHOST : on rapproche de la surface (matière moins présente), sans transparence
    val ghostFace = lerp(baseFace, cs.surface, ghostFactor)
    val ghostStroke = lerp(baseStroke, cs.surface, ghostFactor * 0.65f)

    // ✅ 2) DIM : frigo éteint -> on assombrit (comme les étagères)
    val faceColor = lerp(ghostFace, Color.Companion.Black, dimFactor * 0.65f)
    val stroke = lerp(ghostStroke, Color.Companion.Black, dimFactor * 0.55f)

    // dessus plus clair que la face avant
    val front = faceColor.copy(alpha = 0.3f)
    val top = faceColor.copy(alpha = 0.15f)

    Box(modifier = modifier.height(height)) {
        Canvas(Modifier.Companion.matchParentSize()) {
            val w = size.width
            val h = size.height

            val d = minOf(depth.toPx(), h * 0.35f).coerceAtLeast(8f)

            // ✅ épaisseurs séparées
            val frontSw = 1.4.dp.toPx()   // epaisseur rectangle face front
            val topSw = 0.3.dp.toPx()     // épaisseur trait stroke trapeze profondeur

            // --- Face avant : commence sous le toit
            val frontTop = d
            val frontRectTopLeft = Offset(0f, frontTop)
            val frontRectSize = Size(w, h - frontTop)

            // --- Arrondi UNIQUEMENT en bas
            val rWanted = corner.toPx()
            val rb = rWanted
                .coerceAtMost(frontRectSize.height / 2f)
                .coerceAtMost(frontRectSize.width / 2f)

            // --- Toit (parallélogramme)
            val a = Offset(0f, frontTop)     // avant-gauche
            val b = Offset(w, frontTop)      // avant-droit
            val c = Offset(w - d, 0f)        // arrière-droit
            val dPt = Offset(d, 0f)          // arrière-gauche

            val topPath = Path().apply {
                moveTo(a.x, a.y)
                lineTo(b.x, b.y)
                lineTo(c.x, c.y)
                lineTo(dPt.x, dPt.y)
                close()
            }

            // --- Path face avant : haut carré, bas arrondi
            val x0 = frontRectTopLeft.x
            val y0 = frontRectTopLeft.y
            val x1 = x0 + frontRectSize.width
            val y1 = y0 + frontRectSize.height

            val frontPath = Path().apply {
                moveTo(x0, y0)        // haut carré
                lineTo(x1, y0)

                lineTo(x1, y1 - rb)   // descente droite

                arcTo(
                    rect = Rect(
                        left = x1 - 2 * rb,
                        top = y1 - 2 * rb,
                        right = x1,
                        bottom = y1
                    ),
                    startAngleDegrees = 0f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )

                lineTo(x0 + rb, y1)

                arcTo(
                    rect = Rect(
                        left = x0,
                        top = y1 - 2 * rb,
                        right = x0 + 2 * rb,
                        bottom = y1
                    ),
                    startAngleDegrees = 90f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )

                lineTo(x0, y0)
                close()
            }

            // ====== FILL ======
            drawPath(topPath, color = top)
            if (front.alpha > 0f) drawPath(frontPath, color = front)

            // ====== STROKES ======

            // ✅ contour toit (plus fin)
            drawPath(
                path = topPath,
                color = stroke,
                style = Stroke(width = topSw)
            )

            // ✅ arêtes du toit (plus fines)
            // drawLine(stroke, start = dPt, end = c, strokeWidth = topSw) // arrière top
            drawLine(stroke, start = dPt, end = a, strokeWidth = topSw) // diagonale gauche
            drawLine(stroke, start = c, end = b, strokeWidth = topSw)   // diagonale droite

            // ✅ contour face avant (plus épais)
            drawPath(
                path = frontPath,
                color = stroke,
                style = Stroke(width = frontSw)
            )

            // petite ligne de relief sous le toit (optionnel)
            drawLine(
                color = stroke.copy(alpha = 0.35f),
                start = Offset(10f, frontTop + 1f),
                end = Offset(w - 10f, frontTop + 1f),
                strokeWidth = 1f
            )
        }

        // Contenu au-dessus (on évite le toit)
        Box(
            modifier = Modifier.Companion
                .matchParentSize()
                .padding(top = depth)
                .padding(contentPadding)
        ) {
            content()
        }
    }
}