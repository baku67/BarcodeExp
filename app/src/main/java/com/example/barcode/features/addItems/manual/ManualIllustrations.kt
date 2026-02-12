package com.example.barcode.features.addItems.manual

import android.content.Context
import androidx.annotation.DrawableRes
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

@DrawableRes
fun drawableId(context: Context, name: String?): Int {
    if (name.isNullOrBlank()) return 0
    return context.resources.getIdentifier(name, "drawable", context.packageName)
}
