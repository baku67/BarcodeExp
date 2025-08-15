package com.example.barcode.add

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.*

import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import androidx.compose.material3.rememberDatePickerState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsStepScreen(
    draft: AddItemDraft,
    onConfirm: (name: String?, brand: String?, expiry: Long?) -> Unit,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf(draft.name.orEmpty()) }
    var brand by remember { mutableStateOf(draft.brand.orEmpty()) }
    var expiry by remember { mutableStateOf(draft.expiryDate) } // epoch ms (minuit local)
    var showPicker by remember { mutableStateOf(false) }

    val relative = remember(expiry) { expiry?.let { formatRelativeDaysAnyDistance(it) } ?: "—" }
    val absolute = remember(expiry) { expiry?.let { formatAbsoluteDate(it) } ?: "—" }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Ajouter le produit", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))

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

        // —— DatePicker ——
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

        // —— Récap rapide (optionnel) ——
        Text("Récapitulatif", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text("Nom : ${name.ifBlank { draft.name ?: "—" }}")
        Text("Marque : ${brand.ifBlank { draft.brand ?: "—" }}")
        Text("Code-barres : ${draft.barcode ?: "—"}")
        Text("Date : ${if (expiry != null) absolute else "—"}")

        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBack) { Text("Retour") }
            Button(
                onClick = { onConfirm(name.ifBlank { null }, brand.ifBlank { null }, expiry) },
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