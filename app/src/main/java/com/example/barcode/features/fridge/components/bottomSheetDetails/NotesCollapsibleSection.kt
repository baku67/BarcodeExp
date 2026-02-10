package com.example.barcode.features.fridge.components.bottomSheetDetails

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun NotesCollapsibleSection(
    notes: List<String>,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val shape = RoundedCornerShape(16.dp)

    Column(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.85f),
                shape = shape
            )
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Notes (${notes.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.width(8.dp))

            Text(
                text = if (notes.isEmpty()) "Aucune" else "Voir",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
            )

            Spacer(Modifier.weight(1f))

            Icon(
                imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.90f)
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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (notes.isEmpty()) {
                    Text(
                        text = "Ajoute une note pour retrouver une info (ex: ouvert le 03/02, à consommer vite, etc.)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                    )
                } else {
                    NotesGrid(notes = notes)
                }
            }
        }
    }
}

@Composable
private fun NotesGrid(
    notes: List<String>,
    columns: Int = 2,
    modifier: Modifier = Modifier
) {
    val rows = remember(notes, columns) { notes.chunked(columns.coerceAtLeast(1)) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { note ->
                    NotePostIt(
                        text = note,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (row.size < columns) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun NotePostIt(
    text: String,
    modifier: Modifier = Modifier
) {
    // Tilt déterministe (pas de random à chaque recomposition)
    val tiltDeg = remember(text) { (((text.hashCode() % 9) - 4) * 0.55f).coerceIn(-3f, 3f) }

    val bg = Color(0xFFFFF4B0).copy(alpha = 0.96f)
    val border = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val shape = RoundedCornerShape(14.dp)

    Box(
        modifier = modifier
            .graphicsLayer { rotationZ = tiltDeg }
            .clip(shape)
            .background(bg)
            .border(1.dp, border, shape)
            .padding(12.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF1F1F1F),
            fontWeight = FontWeight.Medium
        )
    }
}
