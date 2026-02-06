package com.example.barcode.features.addItems.manual

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.barcode.features.addItems.AddItemStepScaffold

@Composable
fun ManualExpiryStepScreen(
    title: String,
    onPickExpiry: (Long?) -> Unit,
    onSkip: () -> Unit,
    onBack: () -> Unit,
    onCancel: () -> Unit
) {
    AddItemStepScaffold(
        step = 3,
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
            Text("Date limite", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(
                "Optionnel pour \"$title\".",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f)
            )

            // ✅ Placeholder : tu pourras remplacer par un vrai date picker Material (ou custom bottomsheet)
            ElevatedButton(
                onClick = {
                    // TODO: ouvrir un date picker et convertir en ms
                    // onPickExpiry(expiryMs)
                    onPickExpiry(null)
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Choisir une date (à brancher)") }

            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Ignorer") }
        }
    }
}
