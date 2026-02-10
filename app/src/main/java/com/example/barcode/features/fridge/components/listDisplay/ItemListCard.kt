package com.example.barcode.features.fridge.components.listDisplay

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.MarqueeSpacing
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
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
import com.example.barcode.features.fridge.components.shared.ItemThumbnail
import com.example.barcode.features.fridge.formatRelativeDaysCompact
import com.example.barcode.features.fridge.isExpired
import com.example.barcode.features.fridge.isSoon
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalFoundationApi::class)
@Composable
public fun ItemListCard(
    name: String,
    brand: String?,
    expiry: Long?,
    imageUrl: String?,
    notesCount: Int = 0,
    hasNotes: Boolean = false, // legacy (si tu n'as pas encore le count)
    selected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onDelete: () -> Unit
) {
    val surface = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface
    val relativeCompact = remember(expiry) { expiry?.let { formatRelativeDaysCompact(it) } ?: "—" }
    val cardShape = MaterialTheme.shapes.medium
    val effectiveNotesCount = when {
        notesCount > 0 -> notesCount
        hasNotes -> 1
        else -> 0
    }

    Box {
        Card(
            modifier = Modifier.Companion.combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            ),
            shape = cardShape,
            colors = CardDefaults.cardColors(
                containerColor = if (selected)
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                else surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            border = when {
                selected -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                expiry == null -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                expiry != null && isSoon(expiry) -> BorderStroke(1.dp, Color.Companion.Yellow)
                expiry != null && isExpired(expiry) -> BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.tertiary
                )

                else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            }
        ) {
            Row(
                Modifier.Companion
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.Companion.CenterVertically
            ) {
                // TODO: removeBG natif
                ItemThumbnail(imageUrl)

                Spacer(Modifier.Companion.width(12.dp))

                Column(Modifier.Companion.weight(1f)) {

                    Text(
                        text = name,
                        fontWeight = FontWeight.Companion.SemiBold,
                        color = onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Companion.Clip, // important: sinon l’ellipsis masque l’intérêt du marquee
                        modifier = Modifier.Companion
                            .fillMaxWidth() // important: il faut une contrainte de largeur
                            .basicMarquee(
                                animationMode = MarqueeAnimationMode.Companion.Immediately,
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
                        overflow = TextOverflow.Companion.Ellipsis,
                        softWrap = false
                    )

                    // “dans 3j.” / “aujourd’hui” / “hier” / “il y a 2j.”
                    Text(
                        relativeCompact,
                        color = when {
                            expiry == null -> onSurface.copy(alpha = 0.6f)
                            isSoon(expiry) -> Color.Companion.Yellow
                            isExpired(expiry) -> MaterialTheme.colorScheme.tertiary
                            else -> onSurface.copy(alpha = 0.8f)
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        if (effectiveNotesCount > 0) {
            // ✅ Carré collé au coin, et le coin haut-droit est arrondi par le clip de la Card
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(cardShape)
            ) {
                NotesCornerCountBadge(
                    count = effectiveNotesCount,
                    modifier = Modifier.align(Alignment.TopEnd)
                )
            }
        }
    }
}





@Composable
private fun NotesCornerCountBadge(
    count: Int,
    modifier: Modifier = Modifier,
) {
    val baseColor = Color(0xFF1976D2)
    val foldColor = Color.White.copy(alpha = 0.18f)
    val lineColor = Color.White.copy(alpha = 0.30f)

    val display = when {
            count > 9 -> "9+"
            else -> count.toString()
        }

    Box(
            modifier = modifier.size(18.dp),
            contentAlignment = Alignment.Center
    ) {
        // carré (le radius topEnd vient du clip(cardShape) au-dessus)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(baseColor)
                .border(0.7.dp, lineColor)
        )

        // petit "pli" interne (effet dog-ear sans changer la forme carrée)
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val foldPath = Path().apply {
                moveTo(w, 0f)
                lineTo(w, h * 0.60f)
                lineTo(w * 0.60f, 0f)
                close()
            }
            drawPath(foldPath, color = foldColor)
        }

        Text(
            text = display,
            color = Color.White.copy(alpha = 0.96f),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                lineHeight = 9.sp,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1
        )
    }
}