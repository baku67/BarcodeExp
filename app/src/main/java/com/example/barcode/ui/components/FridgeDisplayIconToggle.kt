package com.example.barcode.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.barcode.ui.ViewMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import com.example.barcode.R
import com.example.barcode.interfaces.AppIcon

@Composable
fun FridgeDisplayIconToggle(
    selected: ViewMode,
    onSelect: (ViewMode) -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    val borderColor = MaterialTheme.colorScheme.outlineVariant

    Row(
        modifier = Modifier
            .clip(shape)
            .border(1.dp, borderColor, shape)
            .padding(2.dp)
    ) {
        SegIcon(
            active = selected == ViewMode.List,
            icon = AppIcon.Vector(Icons.Filled.ViewList),
            onClick = { onSelect(ViewMode.List) },
            shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
        )

        SegIcon(
            active = selected == ViewMode.Fridge,
            // icon = AppIcon.Drawable(R.drawable.display_fridge_grid), // icon custom fridge bof bof
            icon = AppIcon.Vector(Icons.Filled.GridView),
            onClick = { onSelect(ViewMode.Fridge) },
            shape = RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp)
        )
    }
}


@Composable
fun SegIcon(
    active: Boolean,
    icon: AppIcon, // ImageVector (Icones Material) ou @DrawableRes Int (Pour Icones SVG->XML customs)
    onClick: () -> Unit,
    shape: RoundedCornerShape
) {
    val bg = if (active) MaterialTheme.colorScheme.primary else Color.Transparent
    val tint = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary

    IconButton(
        onClick = onClick,
        modifier = Modifier
            .clip(shape)
            .background(bg)
            .size(40.dp)
    ) {
        when (icon) {
            is AppIcon.Vector -> Icon(
                imageVector = icon.image,
                contentDescription = null,
                tint = tint
            )
            is AppIcon.Drawable -> Icon(
                painter = painterResource(icon.resId),
                contentDescription = null,
                tint = tint
            )
        }
    }
}

