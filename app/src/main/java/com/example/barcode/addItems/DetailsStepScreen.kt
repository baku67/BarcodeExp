package com.example.barcode.addItems

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.*

import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.example.barcode.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsStepScreen(
    draft: AddItemDraft,
    onConfirm: (name: String?, brand: String?, expiry: Long?) -> Unit,
    onBack: () -> Unit,
    onCancel: () -> Unit,
    // Nécessaire pour le choix entres les 4 images candidates
    onCycleImage: () -> Unit
) {
    var name by remember { mutableStateOf(draft.name.orEmpty()) }
    var brand by remember { mutableStateOf(draft.brand.orEmpty()) }
    var expiry by remember { mutableStateOf(draft.expiryDate) }
    var showPicker by remember { mutableStateOf(false) }

    val relative = remember(expiry) { expiry?.let { formatRelativeDaysAnyDistance(it) } ?: "—" }
    val absolute = remember(expiry) { expiry?.let { formatAbsoluteDate(it) } ?: "—" }
    val scrollState = rememberScrollState()

    AddItemStepScaffold(
        step = 3,
        onBack = onBack,
        onCancel = onCancel
    ) { innerPadding ->

        val footerHeight = 84.dp // zone bouton + gradient
        val footerPadding = 14.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // ✅ Zone scrollable (ne chevauche jamais le bouton)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // --- Garde TOUT ton contenu actuel ici SAUF le Button("Confirmer") ---

                // Image et Bouton pour switcher entres les 4 images candidates
                val canCycleImage = draft.imageCandidates.size > 1

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                0.7f to Color.Black.copy(alpha = 0.25f),
                                1f to Color.Black.copy(alpha = 0.45f)
                            )
                        )
                ) {
                    val url = draft.imageUrl

                    if (!url.isNullOrBlank()) {
                        AsyncImage(
                            model = url,
                            contentDescription = "Image produit",
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable(enabled = canCycleImage) { onCycleImage() },
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Image,
                            contentDescription = null,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    if (canCycleImage) {
                        FilledIconButton(
                            onClick = onCycleImage,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(10.dp)
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Changer l'image")
                        }

                        Text(
                            text = "Image ${draft.imageCandidateIndex + 1}/${draft.imageCandidates.size}",
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(10.dp),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = brand,
                        onValueChange = { brand = it },
                        label = { Text("Marque") },
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(Modifier.width(12.dp))

                    val nutriRes = nutriScoreRes(draft.nutriScore)

                    if (nutriRes != null) {
                        Image(
                            painter = painterResource(nutriRes),
                            contentDescription = "Nutri-Score ${draft.nutriScore}",
                            modifier = Modifier.height(22.dp)
                        )
                    } else {
                        Image(
                            painter = painterResource(R.drawable.nutri_score_neutre),
                            contentDescription = "Nutri-Score indisponible",
                            modifier = Modifier
                                .height(22.dp)
                                .alpha(0.35f)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Expiration",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                            )
                            Text(
                                text = if (expiry != null) "$relative • $absolute" else "Non définie",
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        FilledTonalIconButton(onClick = { showPicker = true }) {
                            Icon(Icons.Filled.EditCalendar, contentDescription = "Modifier la date")
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                if (showPicker) {
                    val initial = expiry ?: System.currentTimeMillis()
                    val state = rememberDatePickerState(initialSelectedDateMillis = initial)
                    DatePickerDialog(
                        onDismissRequest = { showPicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                val selectedUtc = state.selectedDateMillis
                                expiry = selectedUtc?.let { utcMillisToLocalMidnight(it) }
                                showPicker = false
                            }) { Text("OK") }
                        },
                        dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Annuler") } }
                    ) {
                        DatePicker(state = state, showModeToggle = false)
                    }
                }
            }

            // Footer fixe : bouton confirmé
            Spacer(Modifier.height(12.dp))

            // ✅ Bouton en bas (dans le flux, comme ItemsContent)
            Button(
                onClick = { onConfirm(name.ifBlank { null }, brand.ifBlank { null }, expiry) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = (name.isNotBlank() || (draft.name?.isNotBlank() == true))
            ) {
                Text("Confirmer")
            }
            }
        }

    }



// ——— Utils ——————————————————————————————————————————————————————————————

private fun utcMillisToLocalMidnight(utcMillis: Long): Long {
    val localDate = Instant.ofEpochMilli(utcMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    return localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

private fun formatAbsoluteDate(ms: Long): String =
    Instant.ofEpochMilli(ms)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))

private fun formatRelativeDaysAnyDistance(targetMillis: Long): String {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val target = Instant.ofEpochMilli(targetMillis).atZone(zone).toLocalDate()
    val days = ChronoUnit.DAYS.between(today, target).toInt()
    return when {
        days == 0 -> "aujourd'hui"
        days == 1 -> "demain"
        days == -1 -> "hier"
        days > 1 -> "dans $days j"
        else -> "il y a ${-days} j"
    }
}

@DrawableRes
private fun nutriScoreRes(score: String?): Int? = when (score?.trim()?.uppercase()) {
    "A" -> R.drawable.nutri_score_a
    "B" -> R.drawable.nutri_score_b
    "C" -> R.drawable.nutri_score_c
    "D" -> R.drawable.nutri_score_d
    "E" -> R.drawable.nutri_score_e
    else -> null
}