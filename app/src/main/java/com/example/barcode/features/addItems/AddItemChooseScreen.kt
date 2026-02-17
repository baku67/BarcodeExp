package com.example.barcode.features.addItems

import androidx.annotation.RawRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.barcode.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private val LeadingBoxSize = 86.dp
private val LottieSize = 74.dp

@Composable
fun AddItemChooseScreen(
    onPickScan: () -> Unit,
    onPickManual: () -> Unit,
    onCancel: () -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val sheen = if (isDark) Color.White else Color.Black

    // ✅ Reflet neutre (pas de couleur “verte/primary”)
    val sharedGradient = Brush.linearGradient(
        colors = listOf(
            sheen.copy(alpha = if (isDark) 0.14f else 0.07f),
            sheen.copy(alpha = if (isDark) 0.06f else 0.03f),
            sheen.copy(alpha = if (isDark) 0.02f else 0.01f) // garde un léger “sheen”, pas 0f
        )
    )

    // ✅ Base un peu plus opaque (même si ton theme.surface est translucide)
    val cardBase = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Ajouter un aliment",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onCancel) { Text("Annuler") }
        }

        Text(
            text = "Comment veux-tu l’ajouter ?",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(6.dp))

        // Card 1 : Scan (boucle normale)
        ChooseBigCard(
            title = "Scanner un code-barres",
            subtitle = "Rapide pour les produits emballés",
            containerColor = cardBase,
            backgroundBrush = sharedGradient,
            leading = {
                LottieIconRaw(
                    resId = R.raw.lottie_barcode_scanner_light,
                    modifier = Modifier.size(LottieSize)
                )
            },
            onClick = onPickScan
        )

        // Card 2 : Manuel (ralenti + pause en fin)
        ChooseBigCard(
            title = "Ajouter sans code-barres",
            subtitle = "Légumes, viande, restes, vrac…",
            containerColor = cardBase,
            backgroundBrush = sharedGradient,
            leading = {
                LottieIconRawWithPause(
                    resId = R.raw.lottie_select_light,
                    modifier = Modifier.size(LottieSize),
                    pauseEndMs = 1200L,
                    pauseStartMs = 150L,
                    slowFactor = 1.3f,
                    pingPong = true
                )
            },
            onClick = onPickManual
        )

        Spacer(Modifier.weight(1f))

        Text(
            text = "Astuce : tu pourras définir une méthode par défaut plus tard.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
        )
    }
}

@Composable
private fun ChooseBigCard(
    title: String,
    subtitle: String,
    containerColor: Color,
    backgroundBrush: Brush,
    leading: @Composable () -> Unit,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(118.dp),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.85f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(Modifier.fillMaxSize()) {



            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ✅ PLUS de fond / border : juste une zone de layout plus grande
                Box(
                    modifier = Modifier.size(LeadingBoxSize),
                    contentAlignment = Alignment.Center
                ) {
                    leading()
                }

                Spacer(Modifier.width(12.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.92f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun LottieIconRaw(
    @RawRes resId: Int,
    modifier: Modifier = Modifier
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(resId))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )

    if (composition != null) {
        LottieAnimation(
            composition = composition,
            progress = progress,
            modifier = modifier
        )
    }
}

/**
 * Lottie : lecture 0->1, puis pause en gardant la dernière frame.
 * slowFactor > 1 = plus lent (ex: 1.7f)
 */
@Composable
private fun LottieIconRawWithPause(
    @RawRes resId: Int,
    modifier: Modifier = Modifier,
    pauseEndMs: Long = 1500L,   // pause sur la dernière frame (progress=1f)
    pauseStartMs: Long = 250L,  // pause sur la première frame (progress=0f) (optionnel)
    slowFactor: Float = 1.4f,   // > 1 = plus lent
    pingPong: Boolean = true    // ✅ va-et-vient
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(resId))
    val anim = remember { Animatable(0f) }

    LaunchedEffect(composition, pauseEndMs, pauseStartMs, slowFactor, pingPong) {
        val c = composition ?: return@LaunchedEffect

        val base = c.duration.toInt().coerceAtLeast(300)
        val durationMs = (base * slowFactor).toInt().coerceAtLeast(300)

        while (isActive) {
            // 0 -> 1
            anim.snapTo(0f)
            anim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = durationMs, easing = LinearEasing)
            )
            delay(pauseEndMs) // hold dernière frame

            if (pingPong) {
                // 1 -> 0 (retour smooth au départ)
                anim.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = durationMs, easing = LinearEasing)
                )
                delay(pauseStartMs) // hold première frame (optionnel)
            }
        }
    }

    if (composition != null) {
        LottieAnimation(
            composition = composition,
            progress = { anim.value },
            modifier = modifier
        )
    }
}

