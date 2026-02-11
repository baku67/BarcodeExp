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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.barcode.common.ui.theme.ItemNote
import com.example.barcode.data.local.entities.ItemNoteEntity

private const val NOTE_MAX_LEN = 100
private val BadgeBlue = Color(0xFF1976D2)

@Composable
fun NotesCollapsibleSection(
    notes: List<ItemNoteEntity>,
    onAddNote: (text: String, pinned: Boolean) -> Unit,
    onDeleteNote: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var draft by rememberSaveable { mutableStateOf("") }
    var pinnedDraft by rememberSaveable { mutableStateOf(false) }

    val submit: () -> Unit = {
        val text = draft.trim().take(NOTE_MAX_LEN)
        if (text.isNotBlank()) {
            onAddNote(text, pinnedDraft)
            draft = ""
            pinnedDraft = false
        }
    }

    // ✅ Tri : épinglées d’abord, puis par date (récent -> ancien)
    val sortedNotes = remember(notes) {
        notes.sortedWith(
            compareByDescending<ItemNoteEntity> { it.pinned }
                .thenByDescending { it.createdAt }
        )
    }

    val containerShape = RoundedCornerShape(16.dp)

    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        label = "notesChevronRotation"
    )

    val labelTint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)

    val accentTint by animateColorAsState(
        targetValue = if (notes.isNotEmpty()) ItemNote else labelTint,
        label = "notesAccentTint"
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
            Icon(
                imageVector = Icons.Outlined.StickyNote2,
                contentDescription = null,
                tint = labelTint,
                modifier = Modifier.size(18.dp)
            )

            Spacer(Modifier.width(6.dp))

            Text(
                text = "Notes",
                fontWeight = FontWeight.SemiBold,
                color = labelTint
            )

            Spacer(Modifier.width(6.dp))

            Text(
                text = buildAnnotatedString {
                    append("(")
                    withStyle(SpanStyle(color = accentTint)) { append(notes.size.toString()) }
                    append(")")
                },
                fontWeight = FontWeight.SemiBold,
                color = labelTint
            )

            Spacer(Modifier.weight(1f))
            Spacer(Modifier.weight(1f))

            if (notes.isEmpty()) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Ajouter une note",
                    tint = labelTint,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = accentTint,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(chevronRotation)
                )
            }
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
                    pinned = pinnedDraft,
                    onTogglePinned = { pinnedDraft = !pinnedDraft },
                    onSubmit = submit
                )

                if (notes.isEmpty()) {
                    Text(
                        text = "Exemple : “ouvert hier”, “à finir rapidement”...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                    )
                } else {
                    NotesWrapLeft(
                        notes = sortedNotes,
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
    pinned: Boolean,
    onTogglePinned: () -> Unit,
    onSubmit: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }

    val inputShape = RoundedCornerShape(12.dp)
    val borderColor =
        if (focused) BadgeBlue.copy(alpha = 0.55f)
        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)

    val pinBg by animateColorAsState(
        targetValue = if (pinned) MaterialTheme.colorScheme.error.copy(alpha = 0.10f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f),
        label = "notePinBg"
    )

    val pinBorder by animateColorAsState(
        targetValue = if (pinned) MaterialTheme.colorScheme.error.copy(alpha = 0.55f)
        else borderColor,
        label = "notePinBorder"
    )

    val pinTint by animateColorAsState(
        targetValue = if (pinned) MaterialTheme.colorScheme.error.copy(alpha = 0.90f)
        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
        label = "notePinTint"
    )

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

            IconToggleButton(
                checked = pinned,
                onCheckedChange = { onTogglePinned() },
                modifier = Modifier
                    .size(44.dp)
                    .clip(inputShape)
                    .background(pinBg)
                    .border(1.dp, pinBorder, inputShape)
            ) {
                Icon(
                    imageVector = Icons.Outlined.PushPin,
                    contentDescription = "Épingler",
                    tint = pinTint,
                    modifier = Modifier.size(18.dp)
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

    val shape = RoundedCornerShape(3.dp)

    Box(
        modifier = modifier
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

        Row(
            modifier = Modifier.padding(end = 22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (note.pinned) {
                Icon(
                    imageVector = Icons.Outlined.PushPin,
                    contentDescription = "Note épinglée",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(6.dp))
            }

            Text(
                text = note.body,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF1F1F1F),
                fontWeight = FontWeight.Medium
            )
        }
    }
}
