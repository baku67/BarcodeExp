package com.example.barcode.addItems.manual

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.barcode.addItems.AddItemDraft
import com.example.barcode.addItems.AddItemStepScaffold

@Composable
fun ManualDetailsStepScreen(
    draft: AddItemDraft,
    onNext: (name: String, brand: String?) -> Unit,
    onBack: () -> Unit,
    onCancel: () -> Unit
) {
    var name by remember(draft.name, draft.manualType) {
        mutableStateOf(draft.name ?: draft.manualType?.label.orEmpty())
    }
    var brand by remember(draft.brand) { mutableStateOf(draft.brand.orEmpty()) }

    AddItemStepScaffold(
        step = 2,
        onBack = onBack,
        onCancel = onCancel
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Détails",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nom") },
                placeholder = { Text("ex: Blanc de poulet, Carottes, Omelette...") },
                singleLine = true
            )

            OutlinedTextField(
                value = brand,
                onValueChange = { brand = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Marque (optionnel)") },
                singleLine = true
            )

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    val cleanedName = name.trim()
                    if (cleanedName.isNotEmpty()) {
                        onNext(cleanedName, brand.trim().ifBlank { null })
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.trim().isNotEmpty()
            ) {
                Text("Continuer", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
