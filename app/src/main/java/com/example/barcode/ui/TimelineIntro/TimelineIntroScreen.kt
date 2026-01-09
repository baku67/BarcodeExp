package com.example.barcode.ui.TimelineIntro

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.delay
import kotlin.math.max

/**
 * Mini écran "timeline 3 jours" :
 * - données injectées depuis le GlobalLoader via SavedStateHandle (sinon fallback simulé)
 * - durée ~1.5s, tap pour skip
 */
@Composable
fun TimelineIntroScreen(nav: NavHostController) {

    // ---- 1) Récupère (optionnel) les données injectées par le loader
    // On lit dans l'entrée précédente (le "splash") qui a set() les valeurs.
    val prev = nav.previousBackStackEntry?.savedStateHandle

    // Target à atteindre ensuite (tabs ou auth/login)
    val targetRoute = prev?.get<String>("timeline_target") ?: "tabs"

    // Données: 3 jours (today, tomorrow, d+2)
    // On sépare bien de ce qui alimente le dashboard (ici c'est juste un "snapshot" dédié)
    val expired = prev?.get<IntArray>("timeline_expired") ?: intArrayOf(1, 0, 0)
    val soon = prev?.get<IntArray>("timeline_soon") ?: intArrayOf(2, 3, 1)

    // ---- 2) Animations (simple + propre)
    val reveal = remember { Animatable(0f) }   // 0 → 1
    val fill = remember { Animatable(0f) }     // 0 → 1

    fun goNext() {
        nav.navigate(targetRoute) {
            // Pop tout jusqu'à splash (inclu), donc splash + intro dégagent
            popUpTo("splash") { inclusive = true }
            launchSingleTop = true
        }
    }

    LaunchedEffect(Unit) {
        val fillEasing = CubicBezierEasing(0.18f, 0.90f, 0.18f, 1.00f) // démarre vif, termine smooth

        reveal.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing)
        )

        // Micro pause = “respiration” (fait très moderne)
        delay(180)

        // Fill plus lent + easing plus marqué
        // Fill stepper (DEV : exagéré + pauses)
        fill.snapTo(0f)

        fill.animateTo(
            targetValue = 1f / 3f,
            animationSpec = tween(durationMillis = 1600, easing = fillEasing)
        )
        delay(700)

        fill.animateTo(
            targetValue = 2f / 3f,
            animationSpec = tween(durationMillis = 1600, easing = fillEasing)
        )
        delay(700)

        fill.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1600, easing = fillEasing)
        )
    }

    // Tap n'importe où = skip
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .clickable { goNext() },
        color = Color.Transparent,
        tonalElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.alpha(reveal.value),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    text = "Revue quotidienne",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.height(14.dp))

                // ---- 3) Jauge chronologique (3 segments)
                TimelineSteps(
                    expired = expired,
                    soon = soon,
                    progress = fill.value // 0..1
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Tap pour passer",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
            }
        }
    }
}

@Composable
private fun TimelineSteps(
    expired: IntArray,
    soon: IntArray,
    progress: Float,
    lineHeight: Dp = 6.dp,
    dotRadius: Dp = 8.dp
) {
    val total0 = expired[0] + soon[0]
    val total1 = expired[1] + soon[1]
    val total2 = expired[2] + soon[2]

    val colorNone = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
    val colorExpired = MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
    val colorSoon = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.75f)

    // Couleur par étape
    fun stepColor(total: Int, ex: Int): Color = when {
        total <= 0 -> colorNone
        ex > 0 -> colorExpired
        else -> colorSoon
    }

    val c0 = stepColor(total0, expired[0])
    val c1 = stepColor(total1, expired[1])
    val c2 = stepColor(total2, expired[2])

    val track = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
    val fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)

    val density = LocalDensity.current
    val linePx = with(density) { lineHeight.toPx() }
    val rPx = with(density) { dotRadius.toPx() }

    Column(Modifier.fillMaxWidth()) {

        // --- Ligne + ronds (Canvas = parfait pour ça)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {

                val y = size.height / 2f

                // Ligne pleine largeur
                val linePadding = rPx * 1.2f
                val lineStartX = linePadding
                val lineEndX = size.width - linePadding

                // Points un peu rentrés (la ligne reste full width)
                val dotInset = size.width * 0.12f
                val x0 = lineStartX + dotInset
                val x2 = lineEndX - dotInset
                val x1 = (x0 + x2) / 2f


                // Step-by-step fill (pas un fill continu)
                val p = progress.coerceIn(0f, 1f)
                val t1 = 1f / 3f
                val t2 = 2f / 3f

                val xp = when {
                    p < t1 -> {
                        // segment 1 : start -> x1
                        val local = (p / t1).coerceIn(0f, 1f)
                        lineStartX + (x1 - lineStartX) * local
                    }
                    p < t2 -> {
                        // segment 2 : x1 -> x2
                        val local = ((p - t1) / (t2 - t1)).coerceIn(0f, 1f)
                        x1 + (x2 - x1) * local
                    }
                    else -> {
                        // fin : x2 -> end (petit bonus visuel)
                        val local = ((p - t2) / (1f - t2)).coerceIn(0f, 1f)
                        x2 + (lineEndX - x2) * local
                    }
                }

                // Track
                drawLine(
                    color = track,
                    start = Offset(lineStartX, y),
                    end = Offset(lineEndX, y),
                    strokeWidth = linePx,
                    cap = StrokeCap.Round
                )

                // Fill (segment par segment)
                drawLine(
                    color = fillColor,
                    start = Offset(lineStartX, y),
                    end = Offset(xp, y),
                    strokeWidth = linePx,
                    cap = StrokeCap.Round
                )

                // États : outlined (todo), current (outlined+halo), done (filled+check)
                val done0 = p >= t1
                val done1 = p >= t2
                val done2 = p >= 0.999f

                val currentIndex = when {
                    !done0 -> 0
                    !done1 -> 1
                    else -> 2
                }

                fun drawCheck(center: Offset, size: Float, color: Color) {
                    // check simple: 2 segments
                    val x = center.x
                    val y0 = center.y
                    val p1 = Offset(x - size * 0.35f, y0 + size * 0.02f)
                    val p2 = Offset(x - size * 0.10f, y0 + size * 0.28f)
                    val p3 = Offset(x + size * 0.40f, y0 - size * 0.25f)

                    drawLine(color, p1, p2, strokeWidth = size * 0.18f, cap = StrokeCap.Round)
                    drawLine(color, p2, p3, strokeWidth = size * 0.18f, cap = StrokeCap.Round)
                }

                fun drawStepperDot(
                    x: Float,
                    color: Color,
                    isDone: Boolean,
                    isCurrent: Boolean
                ) {
                    val center = Offset(x, y)

                    if (isCurrent) {
                        // halo léger (moderne)
                        drawCircle(
                            color = color.copy(alpha = 0.14f),
                            radius = rPx * 2.0f,
                            center = center
                        )
                    }

                    if (isDone) {
                        // filled
                        drawCircle(color = color, radius = rPx, center = center)
                        // check en blanc
                        drawCheck(center = center, size = rPx, color = Color.White.copy(alpha = 0.92f))
                    } else {
                        // outlined
                        drawCircle(
                            color = track,
                            radius = rPx,
                            center = center,
                            style = Stroke(width = max(2f, rPx * 0.22f))
                        )

                        // current : petit fill interne discret (sinon ça fait trop vide)
                        if (isCurrent) {
                            drawCircle(
                                color = color.copy(alpha = 0.25f),
                                radius = rPx * 0.55f,
                                center = center
                            )
                        }
                    }
                }

                drawStepperDot(x0, c0, isDone = done0, isCurrent = currentIndex == 0)
                drawStepperDot(x1, c1, isDone = done1, isCurrent = currentIndex == 1)
                drawStepperDot(x2, c2, isDone = done2, isCurrent = currentIndex == 2)
            }
        }

        Spacer(Modifier.height(8.dp))

        // Labels (tu m’as dit “plus tard”, mais je te les mets déjà en minimal)
        val p0 = total0
        val p1 = total1
        val p2 = total2

        fun productLabel(n: Int): String = if (n == 1) "1 produit" else "$n produits"

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(productLabel(p0), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f))
            Text(productLabel(p1), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f))
            Text(productLabel(p2), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f))
        }
    }
}



