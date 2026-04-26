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

enum class ShoppingCategory(
    val key: String,
    val label: String,
    val emoji: String,
) {
    FRESH("FRESH", "Frais", "🥛"),
    FRUITS_VEGE("FRUITS/VEGE", "Fruits / légumes", "🥦"),
    MEAT("MEAT", "Viande", "🥩"),
    FISH("FISH", "Poisson", "🐟"),
    SWEET("SWEET", "Sucré", "🍫"),
    SALTY("SALTY", "Salé", "🥨"),
    FROZEN("FROZEN", "Surgelé", "❄️"),
    HOME("HOME", "Maison", "🏠"),
    OTHER("OTHER", "Autre", "🛒");

    val displayLabel: String
        get() = "$emoji $label"

    companion object {
        fun fromKey(value: String?): ShoppingCategory {
            if (value.isNullOrBlank()) return OTHER

            return entries.firstOrNull {
                it.key == value || it.name == value
            } ?: OTHER
        }
    }
}

enum class ShoppingFilter(val label: String) {
    ALL("Tout"),
    FRESH("Frais"),
    FRUITS_VEGE("Fruits / légumes"),
    MEAT("Viande"),
    FISH("Poisson"),
    SWEET("Sucré"),
    SALTY("Salé"),
    FROZEN("Surgelé"),
    HOME("Maison"),
    OTHER("Autre");

    fun matches(category: ShoppingCategory): Boolean {
        return when (this) {
            ALL -> true
            FRESH -> category == ShoppingCategory.FRESH
            FRUITS_VEGE -> category == ShoppingCategory.FRUITS_VEGE
            MEAT -> category == ShoppingCategory.MEAT
            FISH -> category == ShoppingCategory.FISH
            SWEET -> category == ShoppingCategory.SWEET
            SALTY -> category == ShoppingCategory.SALTY
            FROZEN -> category == ShoppingCategory.FROZEN
            HOME -> category == ShoppingCategory.HOME
            OTHER -> category == ShoppingCategory.OTHER
        }
    }
}

data class ShoppingListItemUi(
    val id: String,
    val homeId: String,
    val ownerUserId: String?,
    val name: String,
    val quantity: String?,
    val category: ShoppingCategory,
    val scope: ShoppingListScope,
    val note: String?,
    val isImportant: Boolean,
    val isFavorite: Boolean,
    val isChecked: Boolean,
    val createdByUserId: String,
    val updatedByUserId: String,
)
