package com.example.barcode.features.addItems

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.barcode.R

@Composable
fun AddItemChooseScreen(
    onPickScan: () -> Unit,
    onPickManual: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
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

        val scanAccent = MaterialTheme.colorScheme.primary
        val manualAccent = MaterialTheme.colorScheme.tertiary

        // Option 1: Scan (avec Lottie)
        ChooseBigCard(
            title = "Scanner un code-barres",
            subtitle = "Rapide pour les produits emballés",
            accent = scanAccent,
            backgroundBrush = Brush.verticalGradient(
                listOf(
                    scanAccent.copy(alpha = 0.30f),
                    scanAccent.copy(alpha = 0.12f),
                    Color.Transparent
                )
            ),
            leading = {
                BarcodeScannerLottie(
                    modifier = Modifier
                        .size(44.dp)
                )
            },
            onClick = onPickScan
        )

        // Option 2: Manual
        ChooseBigCard(
            title = "Ajouter sans code-barres",
            subtitle = "Légumes, viande, restes, vrac…",
            accent = manualAccent,
            backgroundBrush = Brush.verticalGradient(
                listOf(
                    manualAccent.copy(alpha = 0.28f),
                    manualAccent.copy(alpha = 0.10f),
                    Color.Transparent
                )
            ),
            leading = {
                Icon(
                    imageVector = Icons.Outlined.EditNote,
                    contentDescription = null,
                    tint = manualAccent
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
    accent: Color,
    backgroundBrush: Brush,
    leading: @Composable () -> Unit,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)

    // Rend la card lisible même si ton theme.surface est semi-transparent
    val baseSurface = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceSub = MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(118.dp),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = baseSurface,
            contentColor = onSurface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.85f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                // Gradient PAR-DESSUS une base opaque (sinon tu “vois rien”)
                .background(backgroundBrush)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = accent.copy(alpha = 0.16f),                 // + visible
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.22f)),
                    tonalElevation = 0.dp,
                    modifier = Modifier.size(62.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) { leading() }
                }

                Spacer(Modifier.width(14.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.SemiBold,
                        color = onSurface,                               // ✅ plus de titre noir
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = onSurfaceSub.copy(alpha = 0.92f),         // ✅ cohérent & lisible
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}



@Composable
private fun BarcodeScannerLottie(modifier: Modifier = Modifier) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.barcode_scanner))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )

    LottieAnimation(
        composition = composition,
        progress = progress,
        modifier = modifier
    )
}
