package com.example.barcode.features.fridge.components.listDisplay

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.MarqueeSpacing
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.barcode.common.expiry.ExpiryLevel
import com.example.barcode.common.expiry.ExpiryPolicy
import com.example.barcode.common.expiry.expiryLevel
import com.example.barcode.common.expiry.formatRelativeDaysCompact
import com.example.barcode.common.ui.expiry.expiryAccentColor
import com.example.barcode.common.ui.expiry.expiryStrokeColor
import com.example.barcode.common.ui.theme.ItemNote
import com.example.barcode.features.fridge.components.shared.ItemThumbnail

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ItemListCard(
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
    val cs = MaterialTheme.colorScheme
    val surface = cs.surface
    val onSurface = cs.onSurface

    // TODO: branche soonDays sur tes Settings
    val expiryPolicy = remember { ExpiryPolicy(soonDays = 2) }

    val level = remember(expiry) { expiryLevel(expiry, expiryPolicy) }

    val relativeCompact = remember(expiry) {
        expiry?.let { formatRelativeDaysCompact(it, expiryPolicy) } ?: "—"
    }

    val cardShape = MaterialTheme.shapes.medium

    val effectiveNotesCount = when {
        notesCount > 0 -> notesCount
        hasNotes -> 1
        else -> 0
    }

    val warning = androidx.compose.ui.graphics.Color(0xFFFFC107)

    val expiryColor = when (level) {
        ExpiryLevel.NONE -> cs.outlineVariant.copy(alpha = 0.90f)
        ExpiryLevel.EXPIRED -> cs.error
        ExpiryLevel.SOON -> warning
        ExpiryLevel.OK -> cs.primary
    }

    val cardBorder = when {
        selected -> BorderStroke(2.dp, cs.primary)
        level == ExpiryLevel.EXPIRED || level == ExpiryLevel.SOON ->
            BorderStroke(1.dp, expiryAccentColor(level))
        else -> BorderStroke(1.dp, cs.outlineVariant)
    }

    Box {
        Card(
            modifier = Modifier.combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            ),
            shape = cardShape,
            colors = CardDefaults.cardColors(
                containerColor = if (selected) cs.primary.copy(alpha = 0.08f) else surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            border = cardBorder
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Thumbnail
                ItemThumbnail(imageUrl)

                Spacer(Modifier.width(12.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        text = name,
                        fontWeight = FontWeight.SemiBold,
                        color = onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Clip, // sinon l’ellipsis casse l’intérêt du marquee
                        modifier = Modifier
                            .fillMaxWidth()
                            .basicMarquee(
                                animationMode = MarqueeAnimationMode.Immediately,
                                iterations = Int.MAX_VALUE,
                                initialDelayMillis = 1200,
                                repeatDelayMillis = 2000,
                                velocity = 28.dp,
                                spacing = MarqueeSpacing(24.dp)
                            )
                    )

                    val brandText = brand?.takeIf { it.isNotBlank() } ?: "—"
                    Text(
                        text = brandText,
                        color = onSurface.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false
                    )

                    Text(
                        text = relativeCompact,
                        color = when (level) {
                            ExpiryLevel.EXPIRED, ExpiryLevel.SOON -> expiryAccentColor(level)
                            ExpiryLevel.NONE -> onSurface.copy(alpha = 0.6f)
                            else -> onSurface.copy(alpha = 0.8f)
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // Notes badge (coin haut-droit)
        if (effectiveNotesCount > 0) {
            val density = LocalDensity.current
            val badgeSize = 18.dp
            val badgeSizePx = with(density) { badgeSize.toPx() }

            // radius topEnd de la Card, mais évalué à l’échelle du badge
            val topEndRadiusPx = (cardShape as? CornerBasedShape)
                ?.topEnd
                ?.toPx(Size(badgeSizePx, badgeSizePx), density)
                ?: with(density) { 10.dp.toPx() }

            val topEndRadiusDp = with(density) { topEndRadiusPx.toDp() }
            val innerTopEndRadius = (topEndRadiusDp - cardBorder.width).coerceAtLeast(0.dp)

            NotesCornerCountBadge(
                count = effectiveNotesCount,
                badgeSize = badgeSize,
                topEndRadius = innerTopEndRadius,
                bottomStartRadius = 6.dp,
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }

        // Si tu veux un border par-dessus TOUT (badge inclus) : décommente
        /*
        Box(
            modifier = Modifier
                .matchParentSize()
                .border(cardBorder, cardShape)
        )
        */
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
    val stroke = ItemNote.copy(alpha = 0.96f)
    val fold = ItemNote.copy(alpha = 0.18f)

    val display = if (count > 9) "9+" else count.toString()

    val badgeShape = RoundedCornerShape(
        topStart = 0.dp,
        topEnd = topEndRadius,
        bottomEnd = 0.dp,
        bottomStart = bottomStartRadius
    )

    Box(
        modifier = modifier
            .size(badgeSize)
            .clip(badgeShape)
            .border(1.dp, stroke, badgeShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
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
