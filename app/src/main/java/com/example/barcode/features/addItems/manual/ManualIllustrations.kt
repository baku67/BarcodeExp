package com.example.barcode.features.addItems.manual

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.LunchDining
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.ui.graphics.vector.ImageVector

fun illustrationFor(key: String?, fallback: ImageVector): ImageVector {
    return when (key) {
        "eco" -> Icons.Outlined.Eco
        "restaurant" -> Icons.Outlined.Restaurant
        "spa" -> Icons.Outlined.Spa
        "lunch" -> Icons.Outlined.LunchDining
        else -> fallback
    }
}
