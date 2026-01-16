package com.example.barcode.features.addItems

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun AddItemChooseScreen(
    onPickScan: () -> Unit,
    onPickManual: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Ajouter un aliment",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onCancel) { Text("Annuler") }
        }

        Text(
            text = "Comment veux-tu l’ajouter ?",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(6.dp))

        // Option 1: Scan
        ChooseBigCard(
            title = "Scanner un code-barres",
            subtitle = "Rapide pour les produits emballés",
            icon = { Icon(Icons.Outlined.QrCodeScanner, contentDescription = null) },
            onClick = onPickScan
        )


        // Option 2: Manual

        ChooseBigCard(
            title = "Ajouter sans code-barres",
            subtitle = "Légumes, viande, restes, vrac…",
            icon = { Icon(Icons.Outlined.EditNote, contentDescription = null) },
            onClick = onPickManual
        )

        Spacer(Modifier.weight(1f))

        // Petit hint (facultatif)
        Text(
            text = "Astuce : tu pourras définir une méthode par défaut plus tard.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
        )
    }
}

@Composable
private fun ChooseBigCard(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .clickable(onClick = onClick),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                tonalElevation = 0.dp,
                modifier = Modifier.size(54.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    ProvideTextStyle(MaterialTheme.typography.titleLarge) {
                        icon()
                    }
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f)
                )
            }
        }
    }
}
