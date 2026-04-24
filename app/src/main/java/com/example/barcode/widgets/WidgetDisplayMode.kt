package com.example.barcode.widgets

import androidx.datastore.preferences.core.stringPreferencesKey

internal val WidgetDisplayModeKey = stringPreferencesKey("widget_display_mode")
internal val WidgetShoppingScopeKey = stringPreferencesKey("widget_shopping_scope")

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
            return entries.firstOrNull { it.storedValue == value } ?: FRIDGE
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
            return entries.firstOrNull { it.storedValue == value } ?: SHARED
        }
    }
}