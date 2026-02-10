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
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
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

    val cardBorder = when {
        selected -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        expiry == null -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        expiry != null && isSoon(expiry) -> BorderStroke(1.dp, Color.Yellow)
        expiry != null && isExpired(expiry) -> BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary)
        else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
            border = cardBorder
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
                    val density = LocalDensity.current
                    val badgeSize = 18.dp
                    val badgeSizePx = with(density) { badgeSize.toPx() }

                    // radius topEnd de la Card, mais évalué à l’échelle du badge (gère aussi les %)
                    val topEndRadiusPx = (cardShape as? CornerBasedShape)
                        ?.topEnd
                                ?.toPx(Size(badgeSizePx, badgeSizePx), density)
                        ?: with(density) { 10.dp.toPx() }

                    val topEndRadiusDp = with(density) { topEndRadiusPx.toDp() }
                    val innerTopEndRadius = (topEndRadiusDp - cardBorder.width).coerceAtLeast(0.dp)

                    NotesCornerCountBadge(
                            count = effectiveNotesCount,
                            badgeSize = badgeSize,
                            topEndRadius = innerTopEndRadius,     // ✅ coin haut-droit “comme la Card”
                            bottomStartRadius = 6.dp,          // ✅ ton radius bas-gauche
                            modifier = Modifier.align(Alignment.TopEnd)
                                )
                }

            // ✅ border de la Card DESSINÉ AU-DESSUS de tout (badge inclus) => plus de triangle gris
/*            Box(
                    modifier = Modifier
                                .matchParentSize()
                                .border(cardBorder, cardShape)
            )*/
    }
}



@Composable
private fun NotesCornerCountBadge(
    count: Int,
    badgeSize: Dp,
    topEndRadius: Dp,
    bottomStartRadius: Dp,
    modifier: Modifier = Modifier,
) {
    val blue = Color(0xFF1976D2) // ou MaterialTheme.colorScheme.primary
    val stroke = blue.copy(alpha = 0.90f)
    val fold = blue.copy(alpha = 0.12f)

    val display = if (count > 9) "9+" else count.toString()

    val badgeShape = RoundedCornerShape(
        topStart = 0.dp,
        topEnd = topEndRadius,          // épouse le coin de la card (inner)
        bottomEnd = 0.dp,
        bottomStart = bottomStartRadius // ✅ demandé
    )

    Box(
        modifier = modifier
            .size(badgeSize)
            .clip(badgeShape)
            // ✅ pas de background => on voit la card dessous
            .border(1.dp, stroke, badgeShape),
        contentAlignment = Alignment.Center
    ) {
        // petit “pli” interne, très léger (optionnel)
        Canvas(Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height

            val foldPath = Path().apply {
                moveTo(w, 0f)
                lineTo(w, h * 0.60f)
                lineTo(w * 0.60f, 0f)
                close()
            }
            drawPath(foldPath, color = fold)
        }

        Text(
            text = display,
            color = stroke,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                lineHeight = 9.sp,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1
        )
    }
}

