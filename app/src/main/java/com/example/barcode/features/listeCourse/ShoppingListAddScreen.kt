package com.example.barcode.features.listeCourse

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.barcode.features.addItems.manual.ManualTaxonomyRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListAddScreen(
    initialScope: ShoppingListScope,
    onClose: () -> Unit,
    onSubmit: (ShoppingListAddDraft) -> Unit,
) {
    val context = LocalContext.current

    var step by rememberSaveable { mutableIntStateOf(1) }
    var name by rememberSaveable { mutableStateOf("") }
    var quantity by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var isImportant by rememberSaveable { mutableStateOf(false) }

    var searchIndex by remember { mutableStateOf<ShoppingSearchIndex?>(null) }
    var selectedSuggestion by remember { mutableStateOf<ShoppingSearchSuggestion?>(null) }

    var selectedCategoryKey by rememberSaveable {
        mutableStateOf(ShoppingCategory.OTHER.key)
    }

    val trimmedName = name.trim()
    val canSearch = trimmedName.length >= 3

    val selectedCategory = remember(selectedCategoryKey) {
        ShoppingCategory.fromKey(selectedCategoryKey)
    }

    LaunchedEffect(Unit) {
        val taxonomy = ManualTaxonomyRepository.load(context)
        val catalog = ShoppingCatalogRepository.load(context)
        searchIndex = ShoppingSearchIndex.from(context, taxonomy, catalog)
    }

    val suggestions = remember(trimmedName, searchIndex) {
        if (!canSearch) emptyList()
        else searchIndex?.search(trimmedName, limit = 14).orEmpty()
    }

    BackHandler(enabled = step == 2) {
        step = 1
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (step == 1) "Ajouter un article" else "Détails") },
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
                            onValueChange = {
                                name = it
                                selectedSuggestion = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Nom / Recherche...") },
                            singleLine = true
                        )

                        Text(
                            text = if (initialScope == ShoppingListScope.SHARED) {
                                "Recherche locale dans les fruits/légumes génériques et dans le catalogue supermarché."
                            } else {
                                "Recherche locale dans les fruits/légumes génériques et dans le catalogue supermarché."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            when {
                                trimmedName.isBlank() -> {
                                    Text(
                                        text = "Tape au moins 3 caractères pour afficher des suggestions.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                !canSearch -> {
                                    Text(
                                        text = "Minimum 3 caractères.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                searchIndex == null -> {
                                    Text(
                                        text = "Chargement du catalogue...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                suggestions.isEmpty() -> {
                                    Text(
                                        text = "Aucun résultat. Tu peux garder ce nom libre si tu veux.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                else -> {
                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 420.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        items(
                                            items = suggestions,
                                            key = { it.stableId }
                                        ) { suggestion ->
                                            ShoppingSuggestionRow(
                                                suggestion = suggestion,
                                                onClick = {
                                                    selectedSuggestion = suggestion
                                                    name = suggestion.label

                                                    ShoppingCategory.fromTechnicalValue(suggestion.categoryLabel)?.let { category ->
                                                        selectedCategoryKey = category.key
                                                    }

                                                    step = 2
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = { step = 2 },
                            enabled = trimmedName.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Suivant")
                        }
                    }
                }

                else -> {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = {
                                name = it
                                selectedSuggestion = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Nom de l'article") },
                            singleLine = true
                        )

                        selectedSuggestion?.let { suggestion ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            ) {
                                Column(Modifier.padding(14.dp)) {
                                    Text(
                                        text = when (suggestion.source) {
                                            ShoppingSuggestionSource.TAXONOMY -> "Source : taxonomy"
                                            ShoppingSuggestionSource.CATALOG -> "Source : catalogue"
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    ShoppingCategory.displayLabelFromTechnicalValueOrRaw(suggestion.categoryLabel)?.let {
                                        Text(
                                            text = it,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        ShoppingCategorySelect(
                            selectedCategory = selectedCategory,
                            onCategorySelected = { category ->
                                selectedCategoryKey = category.key
                            },
                            modifier = Modifier.fillMaxWidth()
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
                                        ShoppingListAddDraft(
                                            name = trimmedName,
                                            quantity = quantity.trim().ifBlank { null },
                                            note = note.trim().ifBlank { null },
                                            isImportant = isImportant,
                                            category = selectedCategory,
                                            selectedSuggestion = selectedSuggestion,
                                        )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShoppingSuggestionRow(
    suggestion: ShoppingSearchSuggestion,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = suggestion.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                val readableCategoryLabel =
                    ShoppingCategory.displayLabelFromTechnicalValueOrRaw(suggestion.categoryLabel)

                val subtitle = when (suggestion.source) {
                    ShoppingSuggestionSource.TAXONOMY ->
                        readableCategoryLabel ?: "Générique"

                    ShoppingSuggestionSource.CATALOG ->
                        readableCategoryLabel ?: "Catalogue"
                }

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = RoundedCornerShape(999.dp),
                color = if (suggestion.source == ShoppingSuggestionSource.TAXONOMY) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                } else {
                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f)
                }
            ) {
                Text(
                    text = if (suggestion.source == ShoppingSuggestionSource.TAXONOMY) {
                        "Générique"
                    } else {
                        "Magasin"
                    },
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (suggestion.source == ShoppingSuggestionSource.TAXONOMY) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.secondary
                    }
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShoppingCategorySelect(
    selectedCategory: ShoppingCategory,
    onCategorySelected: (ShoppingCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedCategory.displayLabel,
            onValueChange = {},
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            readOnly = true,
            label = { Text("Catégorie") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            singleLine = true
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            ShoppingCategory.entries.forEach { category ->
                DropdownMenuItem(
                    text = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(category.emoji)
                            Text(category.label)
                        }
                    },
                    onClick = {
                        onCategorySelected(category)
                        expanded = false
                    }
                )
            }
        }
    }
}