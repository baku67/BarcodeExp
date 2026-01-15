package com.example.barcode.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import com.example.barcode.interfaces.AppIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeaderBar(
    title: String,
    subtitle: String? = null,
    icon: AppIcon? = AppIcon.Vector(Icons.Filled.Home),
    onIconClick: () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        modifier = Modifier.fillMaxWidth(),
        windowInsets = TopAppBarDefaults.windowInsets,
/*        navigationIcon = {
            if (icon != null) {
                IconButton(onClick = onIconClick) {
                    when (icon) {
                        is AppIcon.Vector -> Icon(
                            imageVector = icon.image,
                            contentDescription = null
                        )
                        is AppIcon.Drawable -> Icon(
                            painter = painterResource(icon.resId),
                            contentDescription = null
                        )
                    }
                }
            }
        },*/
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            )
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

