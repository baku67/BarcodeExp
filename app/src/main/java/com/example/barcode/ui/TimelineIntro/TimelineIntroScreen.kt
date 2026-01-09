package com.example.barcode.ui.TimelineIntro

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
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

        reveal.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing)
        )

        // Micro pause = “respiration” (fait très moderne)
        delay(150)

        fill.snapTo(0f)


        fill.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 1500,
                easing = CubicBezierEasing(0.05f, 0.05f, 0.95f, 0.95f) // filling “linéaire” mais enlève un peu l’aspect “robot”
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
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                // + d’air entre titre et timeline
                Spacer(Modifier.height(32.dp))

                TimelineSteps(
                    expired = expired,
                    soon = soon,
                    progress = fill.value
                )
            }

            Spacer(Modifier.weight(1f))

            // Footer bas d’écran
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

enum class StepKind { TODAY, TOMORROW, SOON }

@Composable
private fun TimelineSteps(
    expired: IntArray,
    soon: IntArray,
    progress: Float,
    lineHeight: Dp = 6.dp,
    dotRadius: Dp = 12.dp // taille du rond
) {
    val stepRed = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.95f)
    val stepYellow = Color(0xFFF9A825) // ou secondary/tertiary selon ton thème
    val stepGreen = MaterialTheme.colorScheme.primary.copy(alpha = 0.90f)   // si tu as un vrai green dans ton thème, remplace


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

    val todayPainter = rememberVectorPainter(Icons.Rounded.Today)
    val tomorrowPainter = rememberVectorPainter(Icons.Rounded.Schedule)
    val soonPainter = rememberVectorPainter(Icons.Rounded.CalendarMonth)

    Column(Modifier.fillMaxWidth()) {

        // --- Ligne + ronds (Canvas =
        var timelineSize by remember { mutableStateOf(IntSize.Zero) }

        // --- Géométrie (hors Canvas) pour réutiliser xp/done aussi pour les labels
        val widthPx = with(density) { timelineSize.width.toFloat() }.takeIf { it > 0f } ?: 0f
        val linePaddingPx = rPx * 1.2f
        val lineStartXPx = linePaddingPx
        val lineEndXPx = (widthPx - linePaddingPx).coerceAtLeast(lineStartXPx)
        val dotInsetPx = widthPx * 0.12f

        val x0Px = (lineStartXPx + dotInsetPx).coerceIn(lineStartXPx, lineEndXPx)
        val x2Px = (lineEndXPx - dotInsetPx).coerceIn(lineStartXPx, lineEndXPx)
        val x1Px = (x0Px + x2Px) / 2f

        val p = progress.coerceIn(0f, 1f)
        val xp = lineStartXPx + (lineEndXPx - lineStartXPx) * p

        val eps = 0.5f
        val done0 = widthPx > 0f && xp >= (x0Px - eps)
        val done1 = widthPx > 0f && xp >= (x1Px - eps)
        val done2 = widthPx > 0f && xp >= (x2Px - eps)

        val snap = rPx * 0.6f
        val haloIndex = when {
            widthPx <= 0f -> -1
            kotlin.math.abs(xp - x0Px) <= snap -> 0
            kotlin.math.abs(xp - x1Px) <= snap -> 1
            kotlin.math.abs(xp - x2Px) <= snap -> 2
            else -> -1
        }

        val alpha0 = remember { Animatable(0f) }
        val alpha1 = remember { Animatable(0f) }
        val alpha2 = remember { Animatable(0f) }

        val darkFill = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)

        // Labels du haut (jour) - alignés sur les ronds
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(22.dp)
                .padding(horizontal = 6.dp)
        ) {
            @Composable
            fun placeTopLabelMeasured(xPx: Float, alpha: Float, text: String, color: Color) {
                var w by remember(text) { mutableStateOf(0) }

                Text(
                    text = text,
                    modifier = Modifier
                        .alpha(alpha)
                        .onSizeChanged { w = it.width }
                        .offset {
                            val left = (xPx - w / 2f).toInt()
                            val maxLeft = (timelineSize.width - w).coerceAtLeast(0)
                            IntOffset(left.coerceIn(0, maxLeft), 0)
                        },
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = color
                )
            }

            if (timelineSize.width > 0) {
                placeTopLabelMeasured(
                    xPx = x0Px,
                    alpha = alpha0.value,
                    text = "Aujourd’hui",
                    color = stepRed
                )
                placeTopLabelMeasured(
                    xPx = x1Px,
                    alpha = alpha1.value,
                    text = "Demain",
                    color = stepYellow
                )
                placeTopLabelMeasured(
                    xPx = x2Px,
                    alpha = alpha2.value,
                    text = "J+2",
                    color = stepGreen
                )
            }
        }









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
                    color = fillColor,
                    start = Offset(lineStartXPx, y),
                    end = Offset(xp, y),
                    strokeWidth = linePx,
                    cap = StrokeCap.Round
                )


                fun drawStepIcon(kind: StepKind, center: Offset, r: Float) {
                    val stroke = max(2f, r * 0.14f)
                    val white = Color.White.copy(alpha = 0.92f)

                    when (kind) {
                        StepKind.TODAY -> {
                            // "Alerte" simple : triangle + point
                            val top = Offset(center.x, center.y - r * 0.55f)
                            val left = Offset(center.x - r * 0.45f, center.y + r * 0.40f)
                            val right = Offset(center.x + r * 0.45f, center.y + r * 0.40f)

                            val path = androidx.compose.ui.graphics.Path().apply {
                                moveTo(top.x, top.y)
                                lineTo(left.x, left.y)
                                lineTo(right.x, right.y)
                                close()
                            }
                            drawPath(path, color = white.copy(alpha = 0.90f))

                            // petit "!" en overlay (2 traits)
                            drawLine(
                                color = Color.Black.copy(alpha = 0.35f),
                                start = Offset(center.x, center.y - r * 0.18f),
                                end = Offset(center.x, center.y + r * 0.18f),
                                strokeWidth = stroke,
                                cap = StrokeCap.Round
                            )
                            drawCircle(
                                color = Color.Black.copy(alpha = 0.35f),
                                radius = stroke * 0.55f,
                                center = Offset(center.x, center.y + r * 0.30f)
                            )
                        }

                        StepKind.TOMORROW -> {
                            // "Horloge" : cercle + aiguilles
                            drawCircle(
                                color = white,
                                radius = r * 0.52f,
                                center = center,
                                style = Stroke(width = stroke)
                            )
                            drawLine(
                                color = white,
                                start = center,
                                end = Offset(center.x, center.y - r * 0.22f),
                                strokeWidth = stroke,
                                cap = StrokeCap.Round
                            )
                            drawLine(
                                color = white,
                                start = center,
                                end = Offset(center.x + r * 0.18f, center.y + r * 0.10f),
                                strokeWidth = stroke,
                                cap = StrokeCap.Round
                            )
                        }

                        StepKind.SOON -> {
                            // "Calendrier" : rectangle arrondi + barre
                            val w = r * 0.95f
                            val h = r * 0.80f
                            val topLeft = Offset(center.x - w / 2f, center.y - h / 2f)
                            val bottomRight = Offset(center.x + w / 2f, center.y + h / 2f)

                            // contour
                            drawRoundRect(
                                color = white,
                                topLeft = topLeft,
                                size = androidx.compose.ui.geometry.Size(w, h),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(r * 0.18f, r * 0.18f),
                                style = Stroke(width = stroke)
                            )
                            // barre haute
                            drawLine(
                                color = white,
                                start = Offset(topLeft.x, topLeft.y + h * 0.28f),
                                end = Offset(bottomRight.x, topLeft.y + h * 0.28f),
                                strokeWidth = stroke,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }

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
                            StepKind.SOON -> soonPainter
                        }
                        drawCenteredIcon(
                            painter = painter,
                            center = center,
                            sizePx = iconSize,
                            tint = color.copy(alpha = 0.95f) // icône colorée
                        )
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


                drawStepperDot(x0Px, stepRed,    isDone = done0, isCurrent = haloIndex == 0, kind = StepKind.TODAY)
                drawStepperDot(x1Px, stepYellow, isDone = done1, isCurrent = haloIndex == 1, kind = StepKind.TOMORROW)
                drawStepperDot(x2Px, stepGreen,  isDone = done2, isCurrent = haloIndex == 2, kind = StepKind.SOON)
            }
        }









        Spacer(Modifier.height(8.dp))









        // ----------------------------------  "x Produits"
        val p0 = total0
        val p1 = total1
        val p2 = total2

        fun productLabel(n: Int): String = if (n == 1) "1 produit" else "$n produits"

        var shown0 by remember { mutableStateOf(false) }
        var shown1 by remember { mutableStateOf(false) }
        var shown2 by remember { mutableStateOf(false) }

        LaunchedEffect(done0, done1, done2) {
            if (!shown0 && done0) {
                shown0 = true
                delay(120)
                alpha0.animateTo(1f, tween(220, easing = FastOutSlowInEasing))
            }
            if (!shown1 && done1) {
                shown1 = true
                delay(120)
                alpha1.animateTo(1f, tween(220, easing = FastOutSlowInEasing))
            }
            if (!shown2 && done2) {
                shown2 = true
                delay(120)
                alpha2.animateTo(1f, tween(220, easing = FastOutSlowInEasing))
            }
        }

        // petit offset vertical sous la ligne
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .padding(horizontal = 6.dp)
        ) {
            @Composable
            fun placeLabel(xPx: Float, alpha: Float, text: String) {
                var w by remember(text) { mutableStateOf(0) }

                Text(
                    text = text,
                    modifier = Modifier
                        .alpha(alpha)
                        .onSizeChanged { w = it.width }
                        .offset {
                            val left = (xPx - w / 2f).toInt()
                            val maxLeft = (timelineSize.width - w).coerceAtLeast(0)
                            IntOffset(left.coerceIn(0, maxLeft), 0)
                        },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f)
                )
            }

            // Variante plus propre : on centre avec WrapContent + align, mais offset suffit ici
            if (timelineSize.width > 0) {
                placeLabel(x0Px, alpha0.value, productLabel(p0))
                placeLabel(x1Px, alpha1.value, productLabel(p1))
                placeLabel(x2Px, alpha2.value, productLabel(p2))
            }
        }
    }
    // ----------------------------------  FIN  "x Produits"
}



