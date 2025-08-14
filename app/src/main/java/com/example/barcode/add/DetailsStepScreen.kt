package com.example.barcode.add

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DetailsStepScreen(
    draft: AddItemDraft,
    onNext: (name: String?, brand: String?, expiry: Long?) -> Unit,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf(draft.name.orEmpty()) }
    var brand by remember { mutableStateOf(draft.brand.orEmpty()) }
    var expiry by remember { mutableStateOf(draft.expiryDate) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(name, { name = it }, label = { Text("Nom") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(brand, { brand = it }, label = { Text("Marque") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Button(onClick = { /* TODO: ouvrir date picker → expiry = ... */ }) { Text("Choisir une date") }

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBack) { Text("Retour") }
            Button(onClick = { onNext(name.ifBlank { null }, brand.ifBlank { null }, expiry) }) { Text("Suivant") }
        }
    }
}