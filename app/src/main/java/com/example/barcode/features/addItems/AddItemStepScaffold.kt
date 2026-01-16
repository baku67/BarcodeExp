package com.example.barcode.features.addItems

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemStepScaffold(
    step: Int,
    totalSteps: Int = 3,
    onBack: (() -> Unit)?,
    onCancel: () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            AddItemStepTopBar(
                stepLabel = "$step/$totalSteps",
                onBack = onBack,
                onCancel = onCancel
            )
        },
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddItemStepTopBar(
    stepLabel: String,
    onBack: (() -> Unit)?,
    onCancel: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = "Ajouter un produit",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.primary
            )
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Retour"
                    )
                }
            }
        },
        actions = {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Text(
                    text = stepLabel,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
            Spacer(Modifier.width(6.dp))
            IconButton(onClick = onCancel) {
                Icon(imageVector = Icons.Filled.Close, contentDescription = "Annuler")
            }
        }
    )
}
