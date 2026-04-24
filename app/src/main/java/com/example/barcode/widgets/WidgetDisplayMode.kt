package com.example.barcode.widgets

import androidx.datastore.preferences.core.stringPreferencesKey

internal val WidgetDisplayModeKey = stringPreferencesKey("widget_display_mode")
internal val WidgetShoppingScopeKey = stringPreferencesKey("widget_shopping_scope")
internal val WidgetFridgeContentModeKey = stringPreferencesKey("widget_fridge_content_mode")

internal enum class WidgetDisplayMode(
    val storedValue: String,
    val label: String
) {
    FRIDGE(
        storedValue = "fridge",
        label = "Frigo"
    ),

    SHOPPING(
        storedValue = "shopping",
        label = "Courses"
    );

    fun toggled(): WidgetDisplayMode {
        return when (this) {
            FRIDGE -> SHOPPING
            SHOPPING -> FRIDGE
        }
    }

    companion object {
        fun fromStoredValue(value: String?): WidgetDisplayMode {
            return values().firstOrNull { it.storedValue == value } ?: FRIDGE
        }
    }
}

internal enum class WidgetShoppingScope(
    val storedValue: String,
    val daoValue: String,
    val label: String
) {
    SHARED(
        storedValue = "shared",
        daoValue = "SHARED",
        label = "Partagée"
    ),

    PERSONAL(
        storedValue = "personal",
        daoValue = "PERSONAL",
        label = "Perso"
    );

    fun toggled(): WidgetShoppingScope {
        return when (this) {
            SHARED -> PERSONAL
            PERSONAL -> SHARED
        }
    }

    companion object {
        fun fromStoredValue(value: String?): WidgetShoppingScope {
            return values().firstOrNull { it.storedValue == value } ?: SHARED
        }
    }
}

internal enum class WidgetFridgeContentMode(
    val storedValue: String
) {
    LIST(
        storedValue = "list"
    ),

    GRID(
        storedValue = "grid"
    );

    fun toggled(): WidgetFridgeContentMode {
        return when (this) {
            LIST -> GRID
            GRID -> LIST
        }
    }

    companion object {
        fun fromStoredValue(value: String?): WidgetFridgeContentMode {
            return values().firstOrNull { it.storedValue == value } ?: LIST
        }
    }
}