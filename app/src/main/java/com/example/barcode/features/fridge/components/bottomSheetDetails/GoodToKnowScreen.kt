package com.example.barcode.features.fridge.components.bottomSheetDetails

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoodToKnowScreen(
    itemName: String,
    onClose: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bon à savoir") },
                actions = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Outlined.Close, contentDescription = "Fermer")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Conseils et repères pour \"$itemName\"",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Contenu temporaire (faux texte) : ici tu pourras mettre plusieurs sections, listes, et recommandations.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                SectionCard(
                    icon = { Icon(Icons.Outlined.Lightbulb, null) },
                    title = "Conservation",
                    paragraphs = listOf(
                        "\"$itemName\" : faux paragraphe de conservation. Température, emballage, durée…",
                        "Deuxième faux paragraphe pour \"$itemName\". Astuces pour éviter le dessèchement / l’humidité / l’oxydation."
                    ),
                    bullets = listOf(
                        "Mettre \"$itemName\" dans un contenant hermétique.",
                        "Éviter le contact direct avec l’air (faux conseil).",
                        "Étiqueter la date d’ouverture / cuisson."
                    )
                )
            }

            item {
                SectionCard(
                    icon = { Icon(Icons.Outlined.WarningAmber, null) },
                    title = "Signes d’alerte",
                    paragraphs = listOf(
                        "Faux paragraphe : quels signes indiquent que \"$itemName\" n’est plus bon."
                    ),
                    bullets = listOf(
                        "Odeur anormale (faux exemple).",
                        "Texture visqueuse / changement de couleur.",
                        "Moisissure visible (à jeter)."
                    )
                )
            }

            item {
                SectionCard(
                    title = "Check-list rapide",
                    paragraphs = listOf(
                        "Mini check-list pour \"$itemName\" avant consommation."
                    ),
                    bullets = listOf(
                        "Aspect OK",
                        "Odeur OK",
                        "Date OK",
                        "Stockage OK"
                    )
                )
            }

            item {
                RecipesCard(itemName = itemName)
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    paragraphs: List<String>,
    bullets: List<String>,
    icon: (@Composable () -> Unit)? = null
) {
    ElevatedCard(shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth()) {
                if (icon != null) {
                    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.primary) {
                        icon()
                    }
                    Spacer(Modifier.width(10.dp))
                }
                Text(title, style = MaterialTheme.typography.titleMedium)
            }

            Spacer(Modifier.height(10.dp))

            paragraphs.forEach { p ->
                Text(
                    text = p,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
            }

            bullets.forEach { b ->
                Row(Modifier.fillMaxWidth()) {
                    Text("•  ", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = b,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun RecipesCard(itemName: String) {
    val ideas = listOf(
        "Idée 1 (fake) : poêlée rapide avec \"$itemName\"",
        "Idée 2 (fake) : salade / bol froid avec \"$itemName\"",
        "Idée 3 (fake) : version gratin / four avec \"$itemName\""
    )

    ElevatedCard(shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(16.dp)) {
            Text("Idées express", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))

            // ✅ C'est bien "ideas" (et pas "items")
            ideas.forEach { idea ->
                Row(Modifier.fillMaxWidth()) {
                    Text("•  ", style = MaterialTheme.typography.bodyMedium)
                    Text(idea, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}
