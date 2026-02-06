package com.example.barcode.features.fridge.components.bottomSheetDetails

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

@Composable
fun BottomSheetMenuButton(
    modifier: Modifier = Modifier,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
    onAddToFavorites: () -> Unit,
    onAddToShoppingList: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier) {
        SmallFloatingActionButton(
            onClick = { expanded = true },
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.86f),
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 6.dp,
                pressedElevation = 10.dp,
                hoveredElevation = 8.dp,
                focusedElevation = 8.dp
            )
        ) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = "Menu"
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            offset = DpOffset(x = 0.dp, y = 8.dp) // petit gap sous le FAB
        ) {
            DropdownMenuItem(
                text = { Text("Modifier") },
                leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                onClick = { expanded = false; onEdit() }
            )

            DropdownMenuItem(
                text = { Text("Retirer") },
                leadingIcon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null) },
                onClick = { expanded = false; onRemove() }
            )

            HorizontalDivider()

            DropdownMenuItem(
                text = { Text("Ajouter aux favoris") },
                leadingIcon = { Icon(Icons.Outlined.FavoriteBorder, contentDescription = null) },
                onClick = { expanded = false; onAddToFavorites() }
            )

            DropdownMenuItem(
                text = { Text("Ajouter à la liste de courses") },
                leadingIcon = { Icon(Icons.Outlined.CheckCircleOutline, contentDescription = null) },
                onClick = { expanded = false; onAddToShoppingList() }
            )
        }
    }
}
