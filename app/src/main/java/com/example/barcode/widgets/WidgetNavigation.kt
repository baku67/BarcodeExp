package com.example.barcode.widgets

object WidgetNavigation {
    const val EXTRA_DESTINATION = "com.example.barcode.widgets.EXTRA_DESTINATION"

    const val DESTINATION_FRIDGE = "fridge"

    // Fallback générique
    const val DESTINATION_SHOPPING = "shopping"

    // Destinations précises depuis le widget
    const val DESTINATION_SHOPPING_SHARED = "shopping_shared"
    const val DESTINATION_SHOPPING_PERSONAL = "shopping_personal"

    // Utilisé pour transmettre la destination à MainTabsScreen
    const val SAVED_STATE_DESTINATION = "widget_destination"
}