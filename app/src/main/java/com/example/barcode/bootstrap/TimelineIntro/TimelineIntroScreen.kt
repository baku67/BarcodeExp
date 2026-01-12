package com.example.barcode.bootstrap.TimelineIntro

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.delay
import kotlin.math.max
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntOffset

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
    val expired = prev?.get<IntArray>("timeline_expired") ?: intArrayOf(1, 0)
    val soon = prev?.get<IntArray>("timeline_soon") ?: intArrayOf(2, 3)

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

        reveal.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing)
        )

        // Micro pause = "respiration" (fait très moderne)
        delay(150)

        fill.snapTo(0f)


        fill.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 1300,
                easing = CubicBezierEasing(0.05f, 0.05f, 0.95f, 0.95f) // filling "linéaire" mais enlève un peu l'aspect "robot"
            )
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp, vertical = 24.dp)
        ) {

            Spacer(Modifier.weight(1f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(reveal.value),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Revue quotidienne",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                // + d'air entre titre et timeline
                Spacer(Modifier.height(42.dp))

                TimelineSteps(
                    expired = expired,
                    soon = soon,
                    progress = fill.value
                )
            }

            Spacer(Modifier.weight(1f))

            // Footer bas d'écran
            Text(
                text = "Tap pour passer",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

enum class StepKind { TODAY, TOMORROW }

@Composable
private fun TimelineSteps(
    expired: IntArray,
    soon: IntArray,
    progress: Float,
    lineHeight: Dp = 6.dp,
    dotRadius: Dp = 12.dp // taille du rond
) {
    val total0 = expired.getOrNull(0)?.let { it + (soon.getOrNull(0) ?: 0) } ?: 0
    val total1 = expired.getOrNull(1)?.let { it + (soon.getOrNull(1) ?: 0) } ?: 0

    val track = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)

    val density = LocalDensity.current
    val linePx = with(density) { lineHeight.toPx() }
    val rPx = with(density) { dotRadius.toPx() }

    val todayPainter = rememberVectorPainter(Icons.Rounded.Today)
    val tomorrowPainter = rememberVectorPainter(Icons.Rounded.Schedule)

    Column(Modifier.fillMaxWidth()) {

        // --- Ligne + ronds (Canvas =
        var timelineSize by remember { mutableStateOf(IntSize.Zero) }

        // --- Géométrie (hors Canvas) pour réutiliser xp/done aussi pour les labels
        val widthPx = with(density) { timelineSize.width.toFloat() }.takeIf { it > 0f } ?: 0f
        val linePaddingPx = rPx * 1.2f
        val lineStartXPx = linePaddingPx
        val lineEndXPx = (widthPx - linePaddingPx).coerceAtLeast(lineStartXPx)
        val dotInsetPx = widthPx * 0.25f

        val stepRed = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.95f)
        val stepYellow = Color(0xFFF9A825) // ou secondary/tertiary selon ton thème

        val fillBrush = remember(stepRed, stepYellow, lineStartXPx, lineEndXPx) {
            Brush.linearGradient(
                colors = listOf(stepRed, stepYellow),
                start = Offset(lineStartXPx, 0f),
                end = Offset(lineEndXPx, 0f)
            )
        }

        val x0Px = (lineStartXPx + dotInsetPx).coerceIn(lineStartXPx, lineEndXPx)
        val x1Px = (lineEndXPx - dotInsetPx).coerceIn(lineStartXPx, lineEndXPx)

        val p = progress.coerceIn(0f, 1f)
        val xp = lineStartXPx + (lineEndXPx - lineStartXPx) * p

        val eps = 0.5f
        val done0 = widthPx > 0f && xp >= (x0Px - eps)
        val done1 = widthPx > 0f && xp >= (x1Px - eps)

        val snap = rPx * 0.6f

        val haloIndex = when {
            widthPx <= 0f -> -1
            kotlin.math.abs(xp - x0Px) <= snap -> 0
            kotlin.math.abs(xp - x1Px) <= snap -> 1
            else -> -1
        }

        val alpha0 = remember { Animatable(0f) }
        val alpha1 = remember { Animatable(0f) }

        val darkFill = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)

        val placeholderFill = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)


        // CANVA JAUGE
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .onSizeChanged { timelineSize = it }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {

                fun DrawScope.drawCenteredIcon(
                    painter: Painter,
                    center: Offset,
                    sizePx: Float,
                    tint: Color
                ) {
                    with(painter) {
                        val topLeft = Offset(center.x - sizePx / 2f, center.y - sizePx / 2f)
                        translate(topLeft.x, topLeft.y) {
                            draw(
                                size = androidx.compose.ui.geometry.Size(sizePx, sizePx),
                                colorFilter = ColorFilter.tint(tint)
                            )
                        }
                    }
                }

                val y = size.height / 2f

                // Track
                drawLine(
                    color = track,
                    start = Offset(lineStartXPx, y),
                    end = Offset(lineEndXPx, y),
                    strokeWidth = linePx,
                    cap = StrokeCap.Round
                )

                // Fill
                drawLine(
                    brush = fillBrush,
                    start = Offset(lineStartXPx, y),
                    end = Offset(xp, y),
                    strokeWidth = linePx,
                    cap = StrokeCap.Round
                )


                fun drawStepperDot(
                    x: Float,
                    color: Color,
                    isDone: Boolean,
                    isCurrent: Boolean,
                    kind: StepKind
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
                        // fond sombre (constant)

                        drawCircle(color = darkFill, radius = rPx, center = center)

                        // fine bordure colorée (optionnel mais très joli)
                        drawCircle(
                            color = color.copy(alpha = 0.95f),
                            radius = rPx,
                            center = center,
                            style = Stroke(width = max(2f, rPx * 0.16f))
                        )

                        val iconSize = rPx * 1.25f
                        val painter = when (kind) {
                            StepKind.TODAY -> todayPainter
                            StepKind.TOMORROW -> tomorrowPainter
                        }
                        drawCenteredIcon(
                            painter = painter,
                            center = center,
                            sizePx = iconSize,
                            tint = color.copy(alpha = 0.95f) // icône colorée
                        )
                    } else {
                        // placeholder plein opaque (couvre la ligne derrière)
                        drawCircle(
                            color = placeholderFill,
                            radius = rPx,
                            center = center
                        )

                        // bordure grise (celle que tu avais)
                        drawCircle(
                            color = track.copy(alpha = 0.9f),
                            radius = rPx,
                            center = center,
                            style = Stroke(width = max(2f, rPx * 0.22f))
                        )
                    }
                }


                drawStepperDot(
                    x0Px,
                    stepRed,
                    isDone = done0,
                    isCurrent = haloIndex == 0,
                    kind = StepKind.TODAY
                )
                drawStepperDot(
                    x1Px,
                    stepYellow,
                    isDone = done1,
                    isCurrent = haloIndex == 1,
                    kind = StepKind.TOMORROW
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // ----------------------------------  "x Produits"
        val p0 = total0
        val p1 = total1

        fun productLabel(n: Int): String = if (n == 1) "1 produit" else "$n produits"

        // couleurs labels "jour"
        val dayTodayColor = stepRed
        val dayTomorrowColor = stepYellow

        var shown0 by remember { mutableStateOf(false) }
        var shown1 by remember { mutableStateOf(false) }

        LaunchedEffect(done0, done1) {
            if (!shown0 && done0) {
                shown0 = true
                alpha0.animateTo(1f, tween(durationMillis = 260, easing = FastOutSlowInEasing))
            }
            if (!shown1 && done1) {
                shown1 = true
                alpha1.animateTo(1f, tween(durationMillis = 260, easing = FastOutSlowInEasing))
            }
        }

        // Spacer(Modifier.height(5.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
        ) {
            @Composable
            fun placeStepLabels(
                xPx: Float,
                alpha: Float,
                dayText: String,
                dayColor: Color,
                bottomText: String
            ) {
                var w by remember(dayText, bottomText) { mutableStateOf(0) }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .onSizeChanged { w = it.width }
                        .offset {
                            if (w == 0 || timelineSize.width == 0) {
                                IntOffset(0, 0)
                            } else {
                                val left = (xPx - w / 2f).toInt()
                                val maxLeft = (timelineSize.width - w).coerceAtLeast(0)
                                IntOffset(left.coerceIn(0, maxLeft), 0)
                            }
                        }
                        .alpha(alpha)
                ) {

                    fun tintedDark(dayColor: Color): Color {
                        val base = Color.Black.copy(alpha = 0.85f)          // noir de base
                        val tint = dayColor.copy(alpha = 1f)
                        return lerp(base, tint, 0.22f).copy(alpha = 0.55f)  // 0.15..0.30 selon ton goût
                    }
                    val cardBg = tintedDark(dayColor)


                    // Flèche pointant vers le haut
                    Canvas(
                        modifier = Modifier
                            .width(12.dp)
                            .height(6.dp)
                    ) {
                        val path = Path().apply {
                            moveTo(size.width / 2f, 0f)
                            lineTo(size.width, size.height)
                            lineTo(0f, size.height)
                            close()
                        }
                        drawPath(
                            path = path,
                            color = cardBg // SAME
                        )
                    }

                    // Card avec contenu
                    Surface(
                        modifier = Modifier,
                        shape = RoundedCornerShape(10.dp),
                        color = cardBg, // SAME
                        tonalElevation = 0.dp,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = dayText,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = dayColor
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = bottomText,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }

            if (timelineSize.width > 0) {
                placeStepLabels(
                    xPx = x0Px,
                    alpha = alpha0.value,
                    dayText = "Aujourd'hui",
                    dayColor = dayTodayColor,
                    bottomText = productLabel(p0)
                )
                placeStepLabels(
                    xPx = x1Px,
                    alpha = alpha1.value,
                    dayText = "Demain",
                    dayColor = dayTomorrowColor,
                    bottomText = productLabel(p1)
                )
            }
        }
    }
}