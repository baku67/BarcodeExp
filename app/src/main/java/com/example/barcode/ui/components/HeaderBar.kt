package com.example.barcode.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.barcode.interfaces.AppIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeaderBar(
    title: String,
    subtitle: String? = null,
    icon: AppIcon? = AppIcon.Vector(Icons.Filled.Home),
    onIconClick: () -> Unit = {},
    titleTrailing: (@Composable () -> Unit)? = null,          // ✅ NEW
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        modifier = Modifier.fillMaxWidth(),
        windowInsets = TopAppBarDefaults.windowInsets,

        // (tu as commenté navigationIcon, je laisse pareil)
        /*navigationIcon = {
            if (icon != null) {
                IconButton(onClick = onIconClick) {
                    when (icon) {
                        is AppIcon.Vector -> Icon(icon.image, contentDescription = null)
                        is AppIcon.Drawable -> Icon(
                            painter = painterResource(icon.resId),
                            contentDescription = null
                        )
                    }
                }
            }
        },*/

        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 22.sp,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    maxLines = 1
                )

                // ✅ Help collé au titre
                if (titleTrailing != null) {
                    Spacer(Modifier.width(8.dp))
                    titleTrailing()
                }

                Spacer(Modifier.weight(1f)) // ✅ pousse le reste, évite l’effet collé
            }
        },

        actions = {
            actions()
            Spacer(Modifier.width(10.dp)) // ✅ marge droite
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}
