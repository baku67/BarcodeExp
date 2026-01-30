package com.example.barcode.features.fridge.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.MarqueeSpacing
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.barcode.features.fridge.formatRelativeDaysCompact
import com.example.barcode.features.fridge.isExpired
import com.example.barcode.features.fridge.isSoon

@OptIn(ExperimentalFoundationApi::class)
@Composable
public fun ItemCard(
    name: String,
    brand: String?,
    expiry: Long?,
    imageUrl: String?,
    selected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onDelete: () -> Unit
) {
    val surface = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface
    val relativeCompact = remember(expiry) { expiry?.let { formatRelativeDaysCompact(it) } ?: "—" }

    Card(
        modifier = Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongPress
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (selected)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            else surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = when {
            selected -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            expiry == null -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            expiry != null && isSoon(expiry) -> BorderStroke(1.dp, Color.Yellow)
            expiry != null && isExpired(expiry) -> BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary)
            else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        }
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // TODO: removeBG natif
            ItemThumbnail(imageUrl)

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {

                Text(
                    text = name,
                    fontWeight = FontWeight.SemiBold,
                    color = onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Clip, // important: sinon l’ellipsis masque l’intérêt du marquee
                    modifier = Modifier
                        .fillMaxWidth() // important: il faut une contrainte de largeur
                        .basicMarquee(
                            animationMode = MarqueeAnimationMode.Immediately,
                            iterations = Int.MAX_VALUE,
                            initialDelayMillis = 1200,   // pause avant le 1er défilement
                            repeatDelayMillis = 2000,    // pause entre chaque boucle (ton “interval régulier”)
                            velocity = 28.dp,            // vitesse (dp/sec environ selon version)
                            spacing = MarqueeSpacing(24.dp) // espace avant de “re-boucler”
                        )
                )

                val brandText = brand?.takeIf { it.isNotBlank() } ?: "—"
                Text(
                    brandText,
                    color = onSurface.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false
                )

                // “dans 3j.” / “aujourd’hui” / “hier” / “il y a 2j.”
                Text(
                    relativeCompact,
                    color = when {
                        expiry == null -> onSurface.copy(alpha = 0.6f)
                        isSoon(expiry) -> Color.Yellow
                        isExpired(expiry) -> MaterialTheme.colorScheme.tertiary
                        else -> onSurface.copy(alpha = 0.8f)
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}