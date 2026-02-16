package com.example.barcode.features.fridge.components.bottomSheetDetails

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun GoodToKnowCollapsibleSection(
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    val shape = RoundedCornerShape(18.dp)
    val accent = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surface

    val borderColor by animateColorAsState(
        targetValue = if (expanded) accent.copy(alpha = 0.55f) else accent.copy(alpha = 0.28f),
        label = "goodToKnowBorder"
    )

    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        label = "goodToKnowChevronRotation"
    )

    val bgBrush = Brush.verticalGradient(
        0f to accent.copy(alpha = 0.12f),
        1f to surface
    )

    Column(
        modifier = modifier
            .clip(shape)
            .background(bgBrush)
            .border(1.dp, borderColor, shape)
            .animateContentSize()
            .clickable(enabled = enabled) { expanded = !expanded }
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(Modifier.width(10.dp))

            Text(
                text = "Bon à savoir",
                fontWeight = FontWeight.SemiBold,
                color = accent
            )

            Spacer(Modifier.weight(1f))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = accent.copy(alpha = if (enabled) 0.95f else 0.35f),
                modifier = Modifier
                    .size(22.dp)
                    .rotate(chevronRotation)
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(animationSpec = tween(220)) + fadeIn(animationSpec = tween(220)),
            exit = shrinkVertically(animationSpec = tween(180)) + fadeOut(animationSpec = tween(180))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GoodToKnowBulletModern("Riche en oligo-éléments : participe au bon fonctionnement du cœur et du système immunitaire (selon les apports journaliers).")
                GoodToKnowBulletModern("Source de protéines : utile après une séance de sport, mais évite d’en abuser si tu surveilles tes apports.")
                GoodToKnowBulletModern("Peut être salé : attention si tu limites le sel.")
                GoodToKnowBulletModern("Après ouverture : conserve au frais, referme bien l’emballage et consomme rapidement.")

                Text(
                    text = "(Infos indicatives — à personnaliser plus tard.)",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                    fontStyle = FontStyle.Italic
                )
            }
        }
    }
}

@Composable
private fun GoodToKnowBulletModern(text: String) {
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "•",
            color = accent,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(end = 10.dp)
        )
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.84f),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
