package com.example.barcode.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.graphics.vector.ImageVector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeaderBar(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = Icons.Filled.Home,
    onIconClick: () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {} // ✅ NOUVEAU
) {
    TopAppBar(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp), // ✅ au lieu de 86.dp
        navigationIcon = {
            if (icon != null) {
                IconButton(onClick = onIconClick) {
                    Icon(imageVector = icon, contentDescription = null)
                }
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(start = 6.dp, bottom = 6.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 22.sp, // ✅ plus compact
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }
        },
        actions = actions, // ✅ NOUVEAU
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}
