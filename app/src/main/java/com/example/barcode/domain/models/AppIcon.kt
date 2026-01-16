package com.example.barcode.domain.models

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.vector.ImageVector

// ImageVector (Icones Material) ou @DrawableRes Int (Pour Icones SVG->XML customs)
// Utilisation:
// SegIcon(
//      icon = AppIcon.Vector(Icons.Filled.ViewList),
// )
sealed interface AppIcon {
    data class Vector(val image: ImageVector) : AppIcon
    data class Drawable(@DrawableRes val resId: Int) : AppIcon
}
