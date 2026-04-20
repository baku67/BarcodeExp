package com.example.barcode.features.listeCourse

enum class ShoppingListScope(val routeValue: String, val label: String) {
    SHARED("shared", "Partagée"),
    PERSONAL("personal", "Personnelle");

    companion object {
        fun fromRoute(value: String?): ShoppingListScope {
            return entries.firstOrNull { it.routeValue == value } ?: SHARED
        }
    }
}

enum class ShoppingCategory(val label: String) {
    FRUITS_LEGUMES("Fruits & légumes"),
    FRAIS("Frais"),
    EPICERIE("Épicerie"),
    VIANDE("Viande"),
    POISSON("Poisson"),
    MAISON("Maison"),
    OTHER("Autre"),
}

enum class ShoppingFilter(val label: String) {
    ALL("Tout"),
    FRUITS_LEGUMES("Fruits & légumes"),
    FRAIS("Frais"),
    EPICERIE("Épicerie"),
    VIANDE("Viande"),
    POISSON("Poisson"),
    MAISON("Maison"),
    OTHER("Autre");

    fun matches(category: ShoppingCategory): Boolean {
        return this == ALL || this.name == category.name
    }
}

data class ShoppingListItemUi(
    val id: String,
    val name: String,
    val quantity: String?,
    val category: ShoppingCategory,
    val scope: ShoppingListScope,
    val note: String?,
    val isImportant: Boolean,
    val isFavorite: Boolean,
    val isChecked: Boolean,
)