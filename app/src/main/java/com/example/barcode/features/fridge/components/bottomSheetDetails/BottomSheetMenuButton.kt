package com.example.barcode.features.fridge.components.bottomSheetDetails

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
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
    val cs = MaterialTheme.colorScheme

    // Couleurs demandées
    val favorite = Color(0xFFE85B7B) // rose rougeâtre
    val shopping = Color(0xFFFFD77A) // jaune pâle
    val danger = cs.error

    var expanded by remember { mutableStateOf(false) }

    Box(modifier) {
        SmallFloatingActionButton(
            onClick = { expanded = !expanded },
            containerColor = cs.surface,
            contentColor = cs.onSurface.copy(alpha = 0.86f),
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
            offset = DpOffset(x = 0.dp, y = 8.dp)
        ) {
            DropdownMenuItem(
                text = { Text("Modifier") },
                leadingIcon = {
                    MenuLeadingIcon(
                        imageVector = Icons.Outlined.Edit,
                        fg = cs.onSurface.copy(alpha = 0.9f),
                        bg = cs.onSurface.copy(alpha = 0.08f)
                    )
                },
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                onClick = { expanded = false; onEdit() }
            )

            DropdownMenuItem(
                text = { Text("Ajouter aux favoris") },
                leadingIcon = {
                    MenuLeadingIcon(
                        imageVector = Icons.Outlined.FavoriteBorder,
                        fg = favorite,
                        bg = favorite.copy(alpha = 0.16f)
                    )
                },
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                onClick = { expanded = false; onAddToFavorites() }
            )

            DropdownMenuItem(
                text = { Text("Ajouter à la liste de courses") },
                leadingIcon = {
                    MenuLeadingIcon(
                        imageVector = Icons.Outlined.CheckCircleOutline,
                        fg = shopping,
                        bg = shopping.copy(alpha = 0.18f)
                    )
                },
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                onClick = { expanded = false; onAddToShoppingList() }
            )

            HorizontalDivider(color = cs.outlineVariant.copy(alpha = 0.6f))

            // Action destructive tout en bas + style “danger” (sans DropdownMenuItemDefaults)
            DropdownMenuItem(
                text = { Text("Retirer", color = danger, fontWeight = FontWeight.SemiBold) },
                leadingIcon = {
                    MenuLeadingIcon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        fg = danger,
                        bg = cs.errorContainer.copy(alpha = 0.65f)
                    )
                },
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                onClick = { expanded = false; onRemove() }
            )
        }
    }
}

@Composable
private fun MenuLeadingIcon(
    imageVector: ImageVector,
    fg: Color,
    bg: Color
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            tint = fg,
            modifier = Modifier.size(18.dp)
        )
    }
}