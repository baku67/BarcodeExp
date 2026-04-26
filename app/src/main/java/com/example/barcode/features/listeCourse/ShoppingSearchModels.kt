package com.example.barcode.features.listeCourse

import android.content.Context
import com.example.barcode.features.addItems.manual.ManualTaxonomy
import com.example.barcode.features.addItems.manual.ManualTaxonomyImageResolver
import java.text.Normalizer
import java.util.Locale

enum class ShoppingSuggestionSource {
    TAXONOMY,
    CATALOG,
}

data class ShoppingCatalogItem(
    val id: String,
    val label: String,
    val image: String?,
    val categoryId: String,
    val searchText: String,
)

data class ShoppingSearchSuggestion(
    val stableId: String,
    val label: String,
    val source: ShoppingSuggestionSource,
    val sourceId: String,
    val categoryId: String?,
    val categoryLabel: String?,
    val taxonomySubtypeCode: String? = null,
    val imageResId: Int = 0,
    val imagePath: String? = null,
)

data class ShoppingListAddDraft(
    val name: String,
    val quantity: String?,
    val note: String?,
    val isImportant: Boolean,
    val category: ShoppingCategory,
    val selectedSuggestion: ShoppingSearchSuggestion?,
)

private data class SearchIndexEntry(
    val suggestion: ShoppingSearchSuggestion,
    val normalizedLabel: String,
    val normalizedSearchText: String,
)

class ShoppingSearchIndex private constructor(
    private val entries: List<SearchIndexEntry>,
) {
    fun search(rawQuery: String, limit: Int = 12): List<ShoppingSearchSuggestion> {
        val query = normalizeForSearch(rawQuery)
        if (query.length < 3) return emptyList()

        return entries
            .asSequence()
            .mapNotNull { entry ->
                val score = entry.score(query) ?: return@mapNotNull null
                score to entry
            }
            .sortedWith(
                compareByDescending<Pair<Int, SearchIndexEntry>> { it.first }
                    .thenBy {
                        if (it.second.suggestion.source == ShoppingSuggestionSource.TAXONOMY) 0 else 1
                    }
                    .thenBy { it.second.suggestion.label.length }
            )
            .map { it.second }
            .distinctBy { it.normalizedLabel }
            .take(limit)
            .map { it.suggestion }
            .toList()
    }

    companion object {
        fun from(
            context: Context,
            taxonomy: ManualTaxonomy,
            catalog: List<ShoppingCatalogItem>,
        ): ShoppingSearchIndex {
            val taxonomyEntries = taxonomy.subtypes
                .asSequence()
                .filter { it.parentCode == "FRUITS" || it.parentCode == "VEGETABLES" }
                .map { subtype ->
                    val categoryLabel = when (subtype.parentCode) {
                        "FRUITS" -> "Fruit générique"
                        "VEGETABLES" -> "Légume générique"
                        else -> "Générique"
                    }

                    val imageResId = ManualTaxonomyImageResolver
                        .resolveSubtypeDrawableResId(context, subtype.code)

                    val label = subtype.title.trim()

                    SearchIndexEntry(
                        suggestion = ShoppingSearchSuggestion(
                            stableId = "taxonomy:${subtype.code}",
                            label = label,
                            source = ShoppingSuggestionSource.TAXONOMY,
                            sourceId = subtype.code,
                            categoryId = subtype.parentCode,
                            categoryLabel = categoryLabel,
                            taxonomySubtypeCode = subtype.code,
                            imageResId = imageResId,
                        ),
                        normalizedLabel = normalizeForSearch(label),
                        normalizedSearchText = normalizeForSearch(
                            buildString {
                                append(label)
                                append(' ')
                                if (subtype.parentCode == "FRUITS") {
                                    append("fruit fruits")
                                } else {
                                    append("legume legumes légumes")
                                }
                            }
                        )
                    )
                }

            val catalogEntries = catalog.asSequence().map { item ->
                SearchIndexEntry(
                    suggestion = ShoppingSearchSuggestion(
                        stableId = "catalog:${item.id}",
                        label = item.label.trim(),
                        source = ShoppingSuggestionSource.CATALOG,
                        sourceId = item.id,
                        categoryId = item.categoryId,
                        categoryLabel = categoryLabelFromId(item.categoryId),
                        imagePath = item.image,
                    ),
                    normalizedLabel = normalizeForSearch(item.label),
                    normalizedSearchText = normalizeForSearch(
                        buildString {
                            append(item.label)
                            append(' ')
                            append(item.searchText)
                            append(' ')
                            append(item.categoryId)
                        }
                    )
                )
            }

            return ShoppingSearchIndex((taxonomyEntries + catalogEntries).toList())
        }
    }
}

private fun SearchIndexEntry.score(query: String): Int? {
    return when {
        normalizedLabel == query -> 1000
        normalizedLabel.startsWith(query) -> 800
        normalizedLabel.split(' ').any { it.startsWith(query) } -> 700
        normalizedSearchText.contains(query) -> 500
        else -> null
    }
}

private fun categoryLabelFromId(categoryId: String?): String? {
    return when (categoryId) {
        "fruits_legumes" -> "Rayon fruits & légumes"
        "boucherie_poissonnerie" -> "Boucherie / poissonnerie"
        "produits_laitiers_oeufs_fromages" -> "Produits laitiers / œufs / fromages"
        "boulangerie_patisserie" -> "Boulangerie / pâtisserie"
        "epicerie_salee" -> "Épicerie salée"
        else -> categoryId?.replace('_', ' ')
    }
}

fun normalizeForSearch(value: String): String {
    return Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace("\\p{Mn}+".toRegex(), "")
        .lowercase(Locale.ROOT)
        .replace("œ", "oe")
        .replace("&", " ")
        .replace("'", " ")
        .replace("-", " ")
        .replace("/", " ")
        .replace("[^a-z0-9 ]".toRegex(), " ")
        .replace("\\s+".toRegex(), " ")
        .trim()
}