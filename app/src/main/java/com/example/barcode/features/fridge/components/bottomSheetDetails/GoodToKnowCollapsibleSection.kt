package com.example.barcode.features.fridge.components.bottomSheetDetails

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun GoodToKnowCollapsibleSection(
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    val containerShape = RoundedCornerShape(16.dp)

    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        label = "goodToKnowChevronRotation"
    )

    val contentTint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
    val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.85f)

    Column(
        modifier = modifier
            .clip(containerShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(width = 1.dp, color = borderColor, shape = containerShape)
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = contentTint,
                modifier = Modifier.size(18.dp)
            )

            Spacer(Modifier.width(6.dp))

            Text(
                text = "Bon à savoir",
                fontWeight = FontWeight.SemiBold,
                color = contentTint
            )

            Spacer(Modifier.weight(1f))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = contentTint,
                modifier = Modifier
                    .size(20.dp)
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
                    .padding(horizontal = 14.dp)
                    .padding(bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GoodToKnowBullet("Riche en oligo-éléments : participe au bon fonctionnement du cœur et du système immunitaire (selon les apports journaliers).")
                GoodToKnowBullet("Source de protéines : utile après une séance de sport, mais évite d’en abuser si tu surveilles tes apports.")
                GoodToKnowBullet("Peut être salé : attention si tu as tendance à faire de la rétention d’eau ou si tu limites le sel.")
                GoodToKnowBullet("Le soir : si le produit est stimulant (caféine / théine / épices), privilégie une petite portion pour ne pas gêner le sommeil.")
                GoodToKnowBullet("Après ouverture : conserve au frais, referme bien l’emballage et consomme rapidement pour limiter l’oxydation et la perte d’arômes.")

                Text(
                    text = "(Infos indicatives — à personnaliser plus tard selon le produit.)",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    fontStyle = FontStyle.Italic
                )
            }
        }
    }
}

@Composable
private fun GoodToKnowBullet(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "•",
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(end = 8.dp)
        )

        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
