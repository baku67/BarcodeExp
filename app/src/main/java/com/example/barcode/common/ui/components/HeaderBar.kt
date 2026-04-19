package com.example.barcode.common.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.barcode.domain.models.AppIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeaderBar(
    title: String,
    subtitle: String? = null,
    icon: AppIcon? = AppIcon.Vector(Icons.Filled.Home),
    onIconClick: () -> Unit = {},
    titleTrailing: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        modifier = Modifier.fillMaxWidth(),
        windowInsets = TopAppBarDefaults.windowInsets,

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
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 24.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            ),
                            maxLines = 1
                        )

                        if (titleTrailing != null) {
                            Spacer(Modifier.width(8.dp))
                            titleTrailing()
                        }
                    }

                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        },

        actions = {
            actions()
            Spacer(Modifier.width(10.dp))
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}