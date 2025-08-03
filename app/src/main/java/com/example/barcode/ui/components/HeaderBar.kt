package com.example.barcode.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.barcode.R
import androidx.compose.material.icons.filled.Home

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeaderBar(
    onIconClick: () -> Unit = {}          // action facultative au clic sur l’icône
) {
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onIconClick) {
                Icon(
                    imageVector = Icons.Filled.Home,   // Icône intégrée
                    contentDescription = "Icône frigo",
                    tint = Color.White
                )
            }
        },
        title = {
            Text(
                text = "Mon Frigo",
                color = Color.White,
                fontSize = 20.sp
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color(0xFF4CAF50),   // vert « 600 » de la palette Material
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)   // hauteur standard d’une app-bar
    )
}
