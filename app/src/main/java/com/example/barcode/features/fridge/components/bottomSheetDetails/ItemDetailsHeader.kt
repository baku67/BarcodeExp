package com.example.barcode.features.fridge.components.bottomSheetDetails

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.barcode.R
import com.example.barcode.data.local.entities.ItemEntity
import com.example.barcode.features.fridge.formatRelativeDaysCompact
import com.example.barcode.features.fridge.isExpired
import com.example.barcode.features.fridge.isSoon


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
private data class ExpiryChipStyle(
    val container: Color,
    val label: Color,
    val border: Color
)



@Composable
private fun expiryChipStyle(expiry: Long?): ExpiryChipStyle {
    val cs = MaterialTheme.colorScheme

    if (expiry == null) {
        return ExpiryChipStyle(
            container = cs.surfaceVariant.copy(alpha = 0.55f),
            label = cs.onSurface.copy(alpha = 0.55f),
            border = cs.outlineVariant.copy(alpha = 0.55f)
        )
    }

    return when {
        isExpired(expiry) -> ExpiryChipStyle(
            container = cs.tertiary.copy(alpha = 0.12f),
            label = cs.tertiary.copy(alpha = 0.95f),
            border = cs.tertiary.copy(alpha = 0.35f)
        )

        isSoon(expiry) -> ExpiryChipStyle(
            container = Color(0xFFFFC107).copy(alpha = 0.16f),  // amber
            label = Color(0xFFFFC107).copy(alpha = 0.95f),
            border = Color(0xFFFFC107).copy(alpha = 0.40f)
        )

        else -> ExpiryChipStyle(
            container = cs.primary.copy(alpha = 0.10f),
            label = cs.primary.copy(alpha = 0.95f),
            border = cs.primary.copy(alpha = 0.30f)
        )
    }
}



@Composable
fun ItemDetailsHeader(
    itemEntity: ItemEntity,
    onClose: () -> Unit,
    onOpenViewer: (ViewerImageKind) -> Unit
) {
    val name = itemEntity.name?.takeIf { it.isNotBlank() } ?: "(sans nom)"
    val brand = itemEntity.brand?.takeIf { it.isNotBlank() } ?: "—"
    val nutriScore = itemEntity.nutriScore?.takeIf { it.isNotBlank() } ?: "—"
    val daysText = itemEntity.expiryDate?.let { formatRelativeDaysCompact(it) } ?: "—"

    val chip = expiryChipStyle(itemEntity.expiryDate)

    Box(Modifier.fillMaxWidth()) {

        // ✅ Contenu header normal
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Image
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable(enabled = !itemEntity.imageUrl.isNullOrBlank()) {
                        onOpenViewer(ViewerImageKind.Preview)
                    }
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (!itemEntity.imageUrl.isNullOrBlank()) {
                    Image(
                        painter = rememberAsyncImagePainter(itemEntity.imageUrl),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
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
                    text = name.replaceFirstChar { it.titlecase() }, // Majuscule
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