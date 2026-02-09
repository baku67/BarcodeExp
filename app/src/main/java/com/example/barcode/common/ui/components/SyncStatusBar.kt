package com.example.barcode.common.ui.components

import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.barcode.sync.SyncUiState
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.delay

private val BarHeight =28.dp              // ✅ moins épais (avant 44.dp)
private const val RotateDurationMs = 1800  // ✅ rotation plus lente (avant 900)

@Composable
fun SyncStatusBar(
    state: SyncUiState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    // ✅ Barre grisâtre
    val bg = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f) // ✅ surface + légère transparence

    // ✅ Bordure bottom un peu plus claire
    val bottomBorder =
        if (MaterialTheme.colorScheme.surface.luminance() < 0.5f)
            Color.White.copy(alpha = 0.10f)  // dark theme → bordure plus claire
        else
            Color.Black.copy(alpha = 0.08f)  // light theme → bordure légèrement marquée


    Column(modifier.fillMaxWidth()) {
        Surface(
            color = bg,
            shape = RoundedCornerShape(0.dp),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            when (state) {
                is SyncUiState.UpToDate -> UpToDateRow(state.lastSuccessAt)
                SyncUiState.Syncing -> SyncingRow() // tu peux garder ton glow/rotate ici
                SyncUiState.Offline -> OfflineRow()
                SyncUiState.AuthRequired -> AuthRequiredRow()
                is SyncUiState.Error -> ErrorRow(onRetry = onRetry)
            }
        }

        HorizontalDivider(thickness = 1.dp, color = bottomBorder)
    }
}




@Composable
private fun UpToDateRow(lastSuccessAt: Long?) {
    // tick pour mettre à jour "il y a X min"
    var tick by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            tick++
        }
    }

    val now = System.currentTimeMillis()

    val (icon, iconTint, label) = when {
        lastSuccessAt == null -> {
            Triple(
                Icons.Outlined.AccessTime,
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                "Dernière sync : jamais"
            )
        }

        (now - lastSuccessAt) < 60_000L -> {
            Triple(
                Icons.Outlined.CheckCircle,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                "Synchronisé : maintenant"
            )
        }

        else -> {
            val rel = DateUtils.getRelativeTimeSpanString(
                lastSuccessAt,
                now,
                DateUtils.MINUTE_IN_MILLIS
            ).toString()

            val isRecent = (now - lastSuccessAt) < 10 * 60_000L

            if (isRecent) {
                Triple(
                    Icons.Outlined.CheckCircle,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.72f),
                    "À jour — dernière sync : $rel"
                )
            } else {
                Triple(
                    Icons.Outlined.AccessTime,
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                    "Dernière sync : $rel"
                )
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(BarHeight)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.80f),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}




@Composable
private fun AuthRequiredRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(BarHeight)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Session expirée — Reconnexion nécessaire",
            color = MaterialTheme.colorScheme.error.copy(alpha = 0.90f),
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}








@Composable
private fun SyncingRow() {
    val accent = MaterialTheme.colorScheme.primary

    val infinite = rememberInfiniteTransition(label = "sync-anim")

    // ✅ rotation icône
    val rotation by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = RotateDurationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // ✅ glow lent (0 → 1 → 0)
    val glowT by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing), // ✅ plus rapide (avant 3200)
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    // Opacité un peu plus perceptible (mais pas “disco”)
    val textAlpha = lerp(0.60f, 1.00f, glowT) // ✅ plus de différence (avant ~0.75→1.00)

    // Glow shadow (léger)
    val shadowAlpha = lerp(0.10f, 0.40f, glowT) // ✅ plus visible
    val blur = lerp(2f, 16f, glowT)             // ✅ glow plus perceptible

    val glowShadow = Shadow(
        color = accent.copy(alpha = shadowAlpha),
        offset = Offset(0f, 0f),
        blurRadius = blur
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(BarHeight)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Icon(
            imageVector = Icons.Outlined.Sync,
            contentDescription = null,
            tint = accent,
            modifier = Modifier
                .size(18.dp)
                .graphicsLayer { rotationZ = rotation }
        )

        Spacer(Modifier.width(10.dp))

        // ✅ Texte animé : alpha + glow
        Text(
            text = "Synchronisation en cours…",
            color = accent.copy(alpha = textAlpha),
            style = MaterialTheme.typography.labelMedium.copy(
                shadow = Shadow(
                    color = accent.copy(alpha = shadowAlpha),
                    offset = Offset(0f, 0f),
                    blurRadius = blur
                )
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}



@Composable
private fun OfflineRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(BarHeight)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.CloudOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = "Hors connexion",
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ErrorRow(onRetry: () -> Unit) {
    val err = MaterialTheme.colorScheme.error

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(BarHeight)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(6.dp))
        Icon(
            imageVector = Icons.Outlined.ErrorOutline,
            contentDescription = null,
            tint = err,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = "Erreur de synchronisation",
            color = err,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        TextButton(onClick = onRetry) {
            Text("Réessayer", color = MaterialTheme.colorScheme.primary)
        }
    }
}
