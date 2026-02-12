package com.example.barcode.features.fridge.components.bottomSheetDetails

import android.os.Build
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.barcode.R
import com.example.barcode.common.expiry.ExpiryLevel
import com.example.barcode.common.expiry.ExpiryPolicy
import com.example.barcode.common.expiry.expiryLevel
import com.example.barcode.common.expiry.formatRelativeDaysCompact
import com.example.barcode.common.ui.expiry.expiryAccentColor
import com.example.barcode.data.local.entities.ItemEntity

@DrawableRes
private fun nutriScoreRes(score: String?): Int? = when (score?.trim()?.uppercase()) {
    "A" -> R.drawable.nutri_score_a
    "B" -> R.drawable.nutri_score_b
    "C" -> R.drawable.nutri_score_c
    "D" -> R.drawable.nutri_score_d
    "E" -> R.drawable.nutri_score_e
    else -> null
}

@Immutable
private data class HeaderExpiryChipStyle(
    val container: Color,
    val label: Color,
    val border: Color
)

@Composable
private fun headerExpiryChipStyle(expiryMillis: Long?, policy: ExpiryPolicy): HeaderExpiryChipStyle {
    val cs = MaterialTheme.colorScheme
    val level = expiryLevel(expiryMillis, policy)

    // ✅ neutre si pas de date
    if (level == ExpiryLevel.NONE) {
        return HeaderExpiryChipStyle(
            container = cs.surfaceVariant.copy(alpha = 0.55f),
            label = cs.onSurface.copy(alpha = 0.55f),
            border = cs.outlineVariant.copy(alpha = 0.55f)
        )
    }

    // ✅ couleur centralisée dans ExpiryUi.kt
    // (et donc EXPIRED => tertiary si c’est ton choix global)
    val accent = expiryAccentColor(level)

    return HeaderExpiryChipStyle(
        container = accent.copy(alpha = 0.14f),
        label = accent.copy(alpha = 0.95f),
        border = accent.copy(alpha = 0.38f)
    )
}

/**
 * ✅ Nom volontairement différent pour éviter le conflit si tu as une autre fonction
 * BottomSheetDetailsHeader(...) ailleurs.
 */
@Composable
fun BottomSheetDetailsHeaderContent(
    itemEntity: ItemEntity,
    onClose: () -> Unit,
    onOpenViewer: (ViewerImageKind) -> Unit
) {
    // TODO: branche soonDays sur tes Settings
    val expiryPolicy = remember { ExpiryPolicy(soonDays = 2) }

    val name = itemEntity.name?.takeIf { it.isNotBlank() } ?: "(sans nom)"
    val brand = itemEntity.brand?.takeIf { it.isNotBlank() } ?: "—"
    val daysText = itemEntity.expiryDate?.let { formatRelativeDaysCompact(it, expiryPolicy) } ?: "—"

    val chip = headerExpiryChipStyle(itemEntity.expiryDate, expiryPolicy)

    Box(Modifier.fillMaxWidth()) {
        val context = LocalContext.current
        val imageUrl = itemEntity.imageUrl?.trim()?.takeIf { it.isNotBlank() }

        val imageRequest = remember(imageUrl) {
            ImageRequest.Builder(context)
                .data(imageUrl)
                .crossfade(true)
                .build()
        }

        val blurBg = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Modifier.blur(26.dp)
        } else {
            Modifier
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable(enabled = !imageUrl.isNullOrBlank()) {
                        onOpenViewer(ViewerImageKind.Preview)
                    }
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (imageUrl != null) {
                    val bgPainter = rememberAsyncImagePainter(imageRequest)
                    val fgPainter = rememberAsyncImagePainter(imageRequest)

                    Image(
                        painter = bgPainter,
                        contentDescription = null,
                        modifier = Modifier
                            .matchParentSize()
                            .graphicsLayer {
                                scaleX = 1.55f
                                scaleY = 1.55f
                                alpha = 0.55f
                            }
                            .then(blurBg),
                        contentScale = ContentScale.Crop
                    )

                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.35f)
                                    )
                                )
                            )
                    )

                    Image(
                        painter = fgPainter,
                        contentDescription = "Image produit",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp),
                        contentScale = ContentScale.Fit,
                        alignment = Alignment.Center
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
                    text = name.replaceFirstChar { it.titlecase() },
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
