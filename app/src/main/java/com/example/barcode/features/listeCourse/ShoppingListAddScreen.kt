package com.example.barcode.features.listeCourse

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListAddScreen(
    initialScope: ShoppingListScope,
    onClose: () -> Unit,
    onSubmit: (
        name: String,
        quantity: String?,
        note: String?,
        isImportant: Boolean,
    ) -> Unit,
) {
    var step by rememberSaveable { mutableIntStateOf(1) }
    var name by rememberSaveable { mutableStateOf("") }
    var quantity by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var isImportant by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = step == 2) {
        step = 1
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(if (step == 1) "Ajouter un article" else "Détails")
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Rounded.Close, contentDescription = "Fermer")
                    }
                }
            )
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = step,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            label = "shoppingAddStep"
        ) { currentStep ->
            when (currentStep) {
                1 -> {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "Liste ${initialScope.label.lowercase()}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Nom / Recherche...") },
                            singleLine = true
                        )

                        Text(
                            text = if (initialScope == ShoppingListScope.SHARED) {
                                "Cet article ira dans la liste partagée du foyer. La recherche d’articles viendra ensuite ; pour l’instant, valide juste un nom libre."
                            } else {
                                "Cet article ira dans ta liste personnelle. La recherche d’articles viendra ensuite ; pour l’instant, valide juste un nom libre."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.weight(1f))

                        Button(
                            onClick = { step = 2 },
                            enabled = name.trim().isNotEmpty(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Suivant")
                        }
                    }
                }

                else -> {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = name.trim(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold
                        )

                        OutlinedTextField(
                            value = quantity,
                            onValueChange = { quantity = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Quantité (optionnel)") },
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Commentaire (optionnel)") },
                            minLines = 3
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Important")
                                Text(
                                    "Ajoute un indicateur visuel dans la liste",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Switch(
                                checked = isImportant,
                                onCheckedChange = { isImportant = it }
                            )
                        }

                        Spacer(Modifier.weight(1f))

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = { step = 1 },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Retour")
                            }

                            Button(
                                onClick = {
                                    onSubmit(
                                        name.trim(),
                                        quantity.trim().ifBlank { null },
                                        note.trim().ifBlank { null },
                                        isImportant
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Ajouter")
                            }
                        }
                    }
                }
            }
        }
    }
}