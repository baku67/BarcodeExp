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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.barcode.common.ui.theme.ItemNote
import com.example.barcode.data.local.entities.ItemNoteEntity
import kotlinx.coroutines.delay
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.key
import androidx.compose.ui.text.font.FontStyle

const val NOTE_MAX_LEN = 100
private val BadgeBlue = Color(0xFF1976D2)

// ✅ rouge “dense” + fond blanc => lisible sur Post-it jaune (et en dark theme aussi)
private val PinRed = Color(0xFFB00020)
private val PinBadgeShape = RoundedCornerShape(999.dp)

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

    // ✅ vibration uniquement au submit success
    val haptics = LocalHapticFeedback.current

    val submit: () -> Unit = {
        val text = draft.trim().take(NOTE_MAX_LEN)
        if (text.isNotBlank()) {
            onAddNote(text, pinnedDraft)

            // ✅ pas de vibration sur chaque frappe -> uniquement ici
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)

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

    var armedNoteId by rememberSaveable { mutableStateOf<String?>(null) }

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

                // ✅ Séparateur discret (mais visible)
                if (sortedNotes.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 2.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                            .heightIn(min = 1.dp)
                    )
                }

                if (notes.isEmpty()) {
                    Text(
                        text = "Exemple : “ouvert hier”, “à finir rapidement”...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                    )
                } else {
                    NotesWrapLeft(
                        notes = sortedNotes,
                        armedNoteId = armedNoteId,
                        onArm = { id -> armedNoteId = id },
                        onDisarm = { armedNoteId = null },
                        onConfirmDelete = { id ->
                            onDeleteNote(id)
                            armedNoteId = null
                        }
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

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {

        // ✅ Ligne 1 : champ pleine largeur
        Box(
            modifier = Modifier
                .fillMaxWidth()
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

        // ✅ Ligne 2 : compteur collé au champ (en haut), + épingle + ajouter
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = "${draft.length}/$NOTE_MAX_LEN",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                modifier = Modifier.padding(top = 2.dp, start = 10.dp) // ✅ collé au champ + en haut de la ligne
            )

            Spacer(Modifier.weight(1f))

            val pinContainer =
                if (pinned) PinRed.copy(alpha = 0.10f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f)

            val pinBorderColor =
                if (pinned) PinRed.copy(alpha = 0.75f)
                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)

            val pinContent =
                if (pinned) PinRed.copy(alpha = 0.95f)
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)

            OutlinedButton(
                onClick = onTogglePinned,
                shape = ButtonDefaults.shape, // ✅ même arrondi que “Ajouter”
                border = BorderStroke(1.dp, pinBorderColor),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = pinContainer,
                    contentColor = pinContent
                ),
                contentPadding = PaddingValues(horizontal = 12.dp),
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .height(40.dp)                 // ✅ aligné au bouton Ajouter
                    .defaultMinSize(minWidth = 40.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.PushPin,
                    contentDescription = "Épingler",
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(Modifier.width(10.dp))

            Button(
                onClick = onSubmit,
                enabled = draft.isNotBlank(),
                modifier = Modifier.align(Alignment.CenterVertically)
            ) {
                Text("Ajouter")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NotesWrapLeft(
    notes: List<ItemNoteEntity>,
    armedNoteId: String?,
    onArm: (String) -> Unit,
    onDisarm: () -> Unit,
    onConfirmDelete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // ✅ IDs connus au moment où la section est rendue => pas d’anim “initiale”
    var knownIds by remember { mutableStateOf(notes.map { it.id }.toSet()) }

    // ✅ on garde l’item le temps de l’anim de sortie, puis on supprime vraiment
    var deletingIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    val exitMs = 170

    fun requestDelete(id: String) {
        onDisarm() // ✅ UX: pas besoin de 2 taps pour armer un autre post-it pendant l’anim
        deletingIds = deletingIds + id
    }

    // ✅ tap dans les zones vides = reset
    Box(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(armedNoteId) {
                if (armedNoteId == null) return@pointerInput
                detectTapGestures(onTap = { onDisarm() })
            }
    ) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)      // ✅ “marge” gauche/droite
                .animateContentSize(tween(220)),
            horizontalArrangement = Arrangement.spacedBy(14.dp), // ✅ espace entre post-its
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            notes.forEach { note ->
                key(note.id) {
                    val isDeleting = deletingIds.contains(note.id)
                    val isArmed = (armedNoteId == note.id)
                    val isNew = !knownIds.contains(note.id)

                    // ✅ entrée animée uniquement pour les nouveaux
                    val visibleState = remember(note.id) {
                        MutableTransitionState(!isNew)
                    }

                    LaunchedEffect(note.id, isNew) {
                        if (isNew) {
                            visibleState.targetState = true
                            knownIds = knownIds + note.id
                        }
                    }

                    // ✅ sortie animée + suppression réelle après la sortie
                    LaunchedEffect(note.id, isDeleting) {
                        if (isDeleting) {
                            visibleState.targetState = false
                            delay(exitMs.toLong())
                            onConfirmDelete(note.id)
                            deletingIds = deletingIds - note.id
                        }
                    }

                    AnimatedVisibility(
                        visibleState = visibleState,
                        enter =
                        fadeIn(tween(160)) +
                                slideInVertically(tween(200)) { (it * -0.20f).toInt() } +
                                scaleIn(tween(200), initialScale = 0.97f),
                        exit =
                        fadeOut(tween(exitMs)) +
                                slideOutVertically(tween(exitMs)) { (it * 0.20f).toInt() } +
                                scaleOut(tween(exitMs), targetScale = 0.97f)
                    ) {
                        NotePostIt(
                            note = note,
                            // ✅ garde le style “armed” pendant la sortie même si on a disarm
                            deleteArmed = isArmed || isDeleting,
                            onArm = { onArm(note.id) },
                            onDisarm = onDisarm,
                            // ✅ ici on demande l’anim de suppression, pas la suppression directe
                            onConfirmDelete = { requestDelete(note.id) },
                            modifier = Modifier
                                .widthIn(max = 260.dp)
                                .disarmIfOtherArmed(
                                    armedNoteId = armedNoteId,
                                    noteId = note.id,
                                    onDisarm = onDisarm
                                )
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun NotePostIt(
    note: ItemNoteEntity,
    deleteArmed: Boolean,
    onArm: () -> Unit,
    onDisarm: () -> Unit,
    onConfirmDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = Color(0xFFFFF4B0).copy(alpha = 0.96f)
    val baseBorder = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val shape = RoundedCornerShape(3.dp)

    Box(
        modifier = modifier
            .clip(shape)
            .background(bg)
            // ✅ border constante => pas de shift
            .border(1.dp, baseBorder, shape)
            // ✅ surbrillance “interne” (n’affecte pas le layout)
            .drawWithContent {
                drawContent()
                if (deleteArmed) {
                    val stroke = 1.dp.toPx()
                    val inset = stroke / 2f
                    val r = 2.dp.toPx()

                    drawRect(PinRed.copy(alpha = 0.05f))
                    drawRoundRect(
                        color = PinRed.copy(alpha = 0.95f),
                        topLeft = Offset(inset, inset),
                        size = Size(size.width - stroke, size.height - stroke),
                        cornerRadius = CornerRadius(r, r),
                        style = Stroke(width = stroke)
                    )
                }
            }
            // ✅ clic sur le post-it armé => reset
            .clickable(
                enabled = deleteArmed,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onDisarm() }
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        // ✅ Action (toujours la même taille => aucun reflow du FlowRow)
        val actionSize = 24.dp
        val actionBg = if (deleteArmed) PinRed.copy(alpha = 0.16f) else Color.Transparent
        val actionTint = if (deleteArmed) PinRed.copy(alpha = 0.95f) else Color(0xFF1F1F1F).copy(alpha = 0.65f)

        Box(
            modifier = Modifier
                .size(actionSize)                 // ✅ taille FIXE
                .align(Alignment.TopEnd)
                .clip(RoundedCornerShape(999.dp))
                .background(actionBg)             // ✅ fond uniquement en armed
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    if (deleteArmed) onConfirmDelete() else onArm()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = if (deleteArmed) "Confirmer suppression" else "Supprimer",
                tint = actionTint,
                modifier = Modifier.size(16.dp)
            )
        }

        Row(
            modifier = Modifier.padding(end = 28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // (optionnel) badge pinned si tu veux le garder visible
            if (note.pinned) {
                Box(
                    modifier = Modifier
                        .clip(PinBadgeShape)
                        .background(Color.White.copy(alpha = 0.85f))
                        .border(1.dp, PinRed.copy(alpha = 0.90f), PinBadgeShape)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PushPin,
                        contentDescription = "Note épinglée",
                        tint = PinRed.copy(alpha = 0.95f),
                        modifier = Modifier.size(12.dp)
                    )
                }
                Spacer(Modifier.width(6.dp))
            }

            Text(
                text = note.body,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontStyle = FontStyle.Italic
                ),
                color = Color(0xFF1F1F1F).copy(alpha = 0.88f), // ✅ opacity “contenu utilisateur”
                fontWeight = FontWeight.Normal                // ✅ plus naturel en italique
            )
        }
    }
}



private fun Modifier.disarmIfOtherArmed(
    armedNoteId: String?,
    noteId: String,
    onDisarm: () -> Unit
): Modifier = pointerInput(armedNoteId, noteId) {
    if (armedNoteId == null || armedNoteId == noteId) return@pointerInput

    awaitPointerEventScope {
        // ✅ capture avant les enfants (IconButton etc.)
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        down.consume() // ✅ empêche que le tap arme/supprime autre chose
        onDisarm()
        waitForUpOrCancellation(pass = PointerEventPass.Initial)
    }
}


