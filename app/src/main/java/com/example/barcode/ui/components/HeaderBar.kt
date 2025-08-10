package com.example.barcode.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeaderBar(
    title: String,                         // titre principal obligatoire
    subtitle: String? = null,              // sous-titre facultatif
    icon: ImageVector = Icons.Filled.Home, // icône de navigation par défaut
    onIconClick: () -> Unit = {}           // action au clic
) {
    val primary = MaterialTheme.colorScheme.primary

    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onIconClick) {
                Icon(
                    imageVector = icon,
                    contentDescription = "Icone header",
                    tint = Color.White
                )
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.fillMaxHeight()
            ) {
                Text(
                    text = title,
                    fontSize = 20.sp,
                    color = Color.White,
                    modifier = Modifier.alignByBaseline()
                )
                // Sous-titre plus petit si fourni
                subtitle?.let {
                    // Spacer(Modifier.width(4.dp))
                    Text(
                        text = " - $subtitle",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = .85f),
                        modifier = Modifier.alignByBaseline()
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = primary,   // vert « 600 » de la palette Material
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(66.dp)   // hauteur standard d’une app-bar
    )
}
