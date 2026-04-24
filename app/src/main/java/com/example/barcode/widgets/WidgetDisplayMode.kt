package com.example.barcode.widgets

import androidx.datastore.preferences.core.stringPreferencesKey

internal val WidgetDisplayModeKey = stringPreferencesKey("widget_display_mode")

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