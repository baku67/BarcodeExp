package com.example.barcode.addItems

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage

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

    AddItemStepScaffold(
        step = 3,
        onBack = onBack,
        onCancel = onCancel
    ) { innerPadding ->

        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {

            // Image et Bouton pour switcher entres les 4 images candidates
            val canCycleImage = draft.imageCandidates.size > 1

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
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



            // —— Édition ——
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nom") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = brand,
                onValueChange = { brand = it },
                label = { Text("Marque") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Text("Date d’expiration : $relative${if (absolute != "—") " ($absolute)" else ""}")

            Spacer(Modifier.height(8.dp))

            OutlinedButton(onClick = { showPicker = true }) {
                Text(if (expiry == null) "Choisir une date" else "Modifier la date")
            }

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

            Spacer(Modifier.height(24.dp))

            Text("Récapitulatif", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

            Spacer(Modifier.height(6.dp))

            Text("Nom : ${name.ifBlank { draft.name ?: "—" }}")
            Text("Marque : ${brand.ifBlank { draft.brand ?: "—" }}")
            Text("Code-barres : ${draft.barcode ?: "—"}")
            Text("Date : ${if (expiry != null) absolute else "—"}")

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { onConfirm(name.ifBlank { null }, brand.ifBlank { null }, expiry) },
                modifier = Modifier.fillMaxWidth(),
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