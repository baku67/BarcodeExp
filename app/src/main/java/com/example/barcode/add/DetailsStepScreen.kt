package com.example.barcode.add

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import androidx.compose.material3.rememberDatePickerState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsStepScreen(
    draft: AddItemDraft,
    onNext: (name: String?, brand: String?, expiry: Long?) -> Unit,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf(draft.name.orEmpty()) }
    var brand by remember { mutableStateOf(draft.brand.orEmpty()) }
    var expiry by remember { mutableStateOf(draft.expiryDate) } // epoch ms
    var showPicker by remember { mutableStateOf(false) }

    // Texte “dans 2 j / il y a 3 j / …”
    val relativeExpiry by remember(expiry) {
        mutableStateOf(expiry?.let { formatRelativeDays(it) } ?: "—")
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
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
        // Bloc Date d’expiration
        Column {
            Text("Date d’expiration : $relativeExpiry")
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { showPicker = true }) {
                Text(if (expiry == null) "Choisir une date" else "Modifier la date")
            }
        }

        // DatePicker Material 3
        if (showPicker) {
            val initial = expiry ?: System.currentTimeMillis()
            val state = rememberDatePickerState(initialSelectedDateMillis = initial)

            DatePickerDialog(
                onDismissRequest = { showPicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        val selectedUtc = state.selectedDateMillis
                        expiry = selectedUtc?.let { utcMillisToLocalMidnight(it) } // normalise à minuit local
                        showPicker = false
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showPicker = false }) { Text("Annuler") }
                }
            ) {
                DatePicker(state = state, showModeToggle = false)
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBack) { Text("Retour") }
            Button(onClick = { onNext(name.ifBlank { null }, brand.ifBlank { null }, expiry) }) {
                Text("Suivant")
            }
        }
    }
}

// ——— Utils ——————————————————————————————————————————————————————————————

private fun utcMillisToLocalMidnight(utcMillis: Long): Long {
    val localDate = Instant.ofEpochMilli(utcMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    return localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

private fun formatRelativeDays(targetMillis: Long): String {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val target = Instant.ofEpochMilli(targetMillis).atZone(zone).toLocalDate()
    val days = ChronoUnit.DAYS.between(today, target).toInt()
    return when (days) {
        0 -> "aujourd'hui"
        1 -> "demain"
        2 -> "après-demain"
        -1 -> "hier"
        -2 -> "avant-hier"
        in 2..60 -> "dans ${days} j"
        in -30..-2 -> "il y a ${-days} j"
        else -> target.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    }
}