package com.example.barcode.features.fridge.components.bottomSheetDetails

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.barcode.features.addItems.manual.ManualTaxonomyRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoodToKnowScreen(
    itemName: String, // en réalité: code subtype/type (ex: "VEG_CARROT")
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val taxonomy = remember(context) { ManualTaxonomyRepository.get(context) }

    // ✅ subtype.title > type.title > fallback humain
    val resolvedTitle = remember(itemName, taxonomy) {
        val code = itemName.trim()
        when {
            code.isBlank() -> "Cet aliment"
            else -> taxonomy.subtypeMeta(code)?.title
                ?: taxonomy.typeMeta(code)?.title
                ?: prettifyTaxonomyCodeForUi(code)
        }
    }

    // ✅ "Carottes" -> "carottes" (et gère aussi les retours à la ligne)
    val insert = remember(resolvedTitle) { resolvedTitle.lowercaseFirstEachLine() }

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
                    text = "Conseils et repères pour $insert",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Contenu temporaire (faux texte) : plusieurs sections, listes, et recommandations pour $insert.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                SectionCard(
                    icon = { Icon(Icons.Outlined.Lightbulb, null) },
                    title = "Conservation",
                    paragraphs = listOf(
                        "Faux paragraphe : comment conserver $insert (température, emballage, durée…).",
                        "Deuxième faux paragraphe : astuces pour éviter le dessèchement / l’humidité / l’oxydation de $insert."
                    ),
                    bullets = listOf(
                        "Mettre $insert dans un contenant hermétique.",
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
                        "Faux paragraphe : quels signes indiquent que $insert n’est plus bon."
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
                        "Mini check-list pour $insert avant consommation."
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
                RecipesCard(insert = insert)
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
private fun RecipesCard(insert: String) {
    val ideas = listOf(
        "Idée 1 (fake) : poêlée rapide avec $insert",
        "Idée 2 (fake) : salade / bol froid avec $insert",
        "Idée 3 (fake) : version gratin / four avec $insert"
    )

    ElevatedCard(shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(16.dp)) {
            Text("Idées express", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
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

/** Fallback si jamais le code n'existe pas dans la taxonomie */
private fun prettifyTaxonomyCodeForUi(raw: String): String {
    val s = raw.trim()
    if (s.isBlank()) return "Cet aliment"
    val normalized = s.replace('-', '_').replace(Regex("_+"), "_").trim('_')
    return normalized.split('_')
        .filter { it.isNotBlank() }
        .joinToString(" ") { part ->
            part.lowercase().replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase() else c.toString() }
        }
}

private fun String.lowercaseFirstEachLine(): String =
    this.split('\n').joinToString("\n") { line ->
        val trimmed = line.trim()
        if (trimmed.isBlank()) line
        else trimmed.replaceFirstChar { ch -> ch.lowercase() }
    }
