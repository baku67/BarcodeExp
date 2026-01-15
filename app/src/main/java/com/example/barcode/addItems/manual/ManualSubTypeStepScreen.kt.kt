package com.example.barcode.addItems.manual

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.barcode.addItems.AddItemDraft
import com.example.barcode.addItems.AddItemStepScaffold
import com.example.barcode.addItems.subtypes

@Composable
fun ManualSubtypeStepScreen(
    draft: AddItemDraft,
    onPick: (ManualSubType) -> Unit,
    onBack: () -> Unit,
    onCancel: () -> Unit
) {
    val type: ManualType? = draft.manualType
    val list = type?.subtypes().orEmpty()

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
                text = "Choisir un sous-type",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = when (type) {
                    ManualType.VEGETABLES -> "Quel légume ? (utile pour proposer une DLC)"
                    ManualType.MEAT -> "Quel type de viande ? (utile pour proposer une DLC)"
                    ManualType.DAIRY -> "Quel produit laitier ?"
                    ManualType.FISH -> "Quel type de poisson ?"
                    ManualType.EGGS -> "Quel type d'œufs ?"
                    null -> "Type manquant. Reviens en arrière."
                    else -> "Sélectionner un sous-type."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f)
            )

            if (type == null) {
                Spacer(Modifier.height(8.dp))
                AssistChip(
                    onClick = onBack,
                    label = { Text("Revenir au choix du type") }
                )
                return@AddItemStepScaffold
            }

            if (list.isEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Aucun sous-type disponible pour ${type.label}.",
                    style = MaterialTheme.typography.bodyMedium
                )
                return@AddItemStepScaffold
            }

            Spacer(Modifier.height(6.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(list) { sub ->
                    SubtypeRow(
                        title = sub.label,
                        selected = draft.manualSubtype == sub,
                        onClick = { onPick(sub) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SubtypeRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = MaterialTheme.shapes.large

    Surface(
        onClick = onClick,
        shape = shape,
        tonalElevation = 0.dp,
        color = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        } else {
            MaterialTheme.colorScheme.surface
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            if (selected) {
                Text(
                    text = "✓",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
