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
            icon = Icons.Filled.ViewList,
            onClick = { onSelect(ViewMode.List) },
            shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
        )

        SegIcon(
            active = selected == ViewMode.Grid,
            icon = Icons.Filled.GridView,
            onClick = { onSelect(ViewMode.Grid) },
            shape = RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp)
        )
    }
}


@Composable
fun SegIcon(
    active: Boolean,
    icon: ImageVector,
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
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint
        )
    }
}

