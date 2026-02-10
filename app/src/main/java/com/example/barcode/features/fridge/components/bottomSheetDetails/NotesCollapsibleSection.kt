package com.example.barcode.features.fridge.components.bottomSheetDetails

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.barcode.data.local.entities.ItemNoteEntity

@Composable
fun NotesCollapsibleSection(
    notes: List<ItemNoteEntity>,
    onAddNote: (String) -> Unit,
    onDeleteNote: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var draft by rememberSaveable { mutableStateOf("") }

    val shape = RoundedCornerShape(16.dp)

    val BadgeBlue = Color(0xFF1976D2)

    val headerTint by animateColorAsState(
        targetValue = if (notes.isNotEmpty()) BadgeBlue
        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
        label = "notesHeaderTint"
    )

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
                fontWeight = FontWeight.SemiBold,
                color = headerTint
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { if (it.length <= 800) draft = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text("Ajouter une note…") }
                    )
                    Button(
                        onClick = {
                            val text = draft.trim()
                            if (text.isNotBlank()) {
                                onAddNote(text)
                                draft = ""
                            }
                        }
                    ) {
                        Text("Ajouter")
                    }
                }

                if (notes.isEmpty()) {
                    Text(
                        text = "Astuce : “ouvert hier”, “à finir rapidement”, “ne pas congeler”…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                    )
                } else {
                    NotesGrid(notes = notes, onDeleteNote = onDeleteNote)
                }
            }
        }
    }
}

@Composable
private fun NotesGrid(
    notes: List<ItemNoteEntity>,
    onDeleteNote: (String) -> Unit,
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
                        note = note,
                        onDelete = { onDeleteNote(note.id) },
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
    note: ItemNoteEntity,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val text = note.body
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
            .padding(10.dp)
    ) {
        IconButton(
            onClick = onDelete,
            modifier = Modifier
                .size(28.dp)
                .align(Alignment.TopEnd)
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Supprimer",
                tint = Color(0xFF1F1F1F).copy(alpha = 0.65f)
            )
        }

        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF1F1F1F),
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .padding(end = 28.dp)
                .align(Alignment.CenterStart)
        )
    }
}
