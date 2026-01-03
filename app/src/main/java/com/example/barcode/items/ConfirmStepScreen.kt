package com.example.barcode.items

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ConfirmStepScreen(
    draft: AddItemDraft,
    onConfirm: () -> Unit,
    onBack: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Confirmer l’ajout", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        Text("Nom : ${draft.name ?: "—"}")
        Text("Marque : ${draft.brand ?: "—"}")
        Text("Code-barres : ${draft.barcode ?: "—"}")
        Text("Date : ${draft.expiryDate ?: "—"}")

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBack) { Text("Retour") }
            Button(onClick = onConfirm) { Text("Confirmer") }
        }
    }
}