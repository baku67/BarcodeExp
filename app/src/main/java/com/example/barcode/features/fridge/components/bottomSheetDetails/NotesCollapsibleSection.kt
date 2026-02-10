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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.barcode.data.local.entities.ItemNoteEntity

private const val NOTE_MAX_LEN = 150
private val BadgeBlue = Color(0xFF1976D2)

@Composable
fun NotesCollapsibleSection(
    notes: List<ItemNoteEntity>,
    onAddNote: (String) -> Unit,
    onDeleteNote: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var draft by rememberSaveable { mutableStateOf("") }

    val submit: () -> Unit = {
        val text = draft.trim().take(NOTE_MAX_LEN)
        if (text.isNotBlank()) {
            onAddNote(text)
            draft = ""
        }
    }

    val containerShape = RoundedCornerShape(16.dp)

    val headerTint by animateColorAsState(
        targetValue = if (notes.isNotEmpty()) BadgeBlue
        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
        label = "notesHeaderTint"
    )

    Column(
        modifier = modifier
            .clip(containerShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.85f),
                shape = containerShape
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
                AddNoteRow(
                    draft = draft,
                    onDraftChange = { draft = it.take(NOTE_MAX_LEN) },
                    onSubmit = submit
                )

                if (notes.isEmpty()) {
                    Text(
                        text = "Astuce : “ouvert hier”, “à finir rapidement”, “ne pas congeler”…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                    )
                } else {
                    NotesWrapLeft(
                        notes = notes,
                        onDeleteNote = onDeleteNote
                    )
                }
            }
        }
    }
}

@Composable
private fun AddNoteRow(
    draft: String,
    onDraftChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }

    val inputShape = RoundedCornerShape(12.dp)
    val borderColor =
        if (focused) BadgeBlue.copy(alpha = 0.55f)
        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .clip(inputShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f))
                    .border(1.dp, borderColor, inputShape)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (draft.isBlank()) {
                    Text(
                        text = "Ecrire quelque chose...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.70f)
                    )
                }

                BasicTextField(
                    value = draft,
                    onValueChange = onDraftChange,
                    singleLine = true,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focused = it.isFocused }
                )
            }

            Button(
                onClick = onSubmit,
                enabled = draft.isNotBlank()
            ) {
                Text("Ajouter")
            }
        }

        Text(
            text = "${draft.length}/$NOTE_MAX_LEN",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
            modifier = Modifier.padding(start = 2.dp)
        )
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NotesWrapLeft(
    notes: List<ItemNoteEntity>,
    onDeleteNote: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.Start),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        notes.forEach { note ->
            NotePostIt(
                note = note,
                onDelete = { onDeleteNote(note.id) },
                modifier = Modifier.widthIn(max = 260.dp)
            )
        }
    }
}

@Composable
private fun NotePostIt(
    note: ItemNoteEntity,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = Color(0xFFFFF4B0).copy(alpha = 0.96f)
    val border = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)

    // ✅ encore moins arrondi
    val shape = RoundedCornerShape(3.dp)

    Box(
        modifier = modifier
            // ✅ plus de rotation
            .clip(shape)
            .background(bg)
            .border(1.dp, border, shape)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        IconButton(
            onClick = onDelete,
            modifier = Modifier
                .size(20.dp)
                .align(Alignment.TopEnd)
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Supprimer",
                tint = Color(0xFF1F1F1F).copy(alpha = 0.65f),
                modifier = Modifier.size(15.dp)
            )
        }

        Text(
            text = note.body,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF1F1F1F),
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(end = 22.dp)
        )
    }
}
