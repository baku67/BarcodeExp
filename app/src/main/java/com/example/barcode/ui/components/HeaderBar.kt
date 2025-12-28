package com.example.barcode.ui.components

import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import com.example.barcode.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeaderBar(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = Icons.Filled.Home, // icône de navigation par défaut
    onIconClick: () -> Unit = {}            // action au clic
) {

    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onIconClick) {
                Image(
                    painter = painterResource(id = R.drawable.frigozen_icon),
                    contentDescription = "Logo",
                    modifier = Modifier.size(35.dp),
                    contentScale = ContentScale.Fit
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
                    fontSize = 25.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.absoluteOffset(x = 0.dp, y = -4.dp)
                )
                subtitle?.let {
                    Text(
                        text = "- $subtitle",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = .85f),
                        modifier = Modifier.absoluteOffset(x = 15.dp, y = -2.dp)
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,   // ou "MaterialTheme.colorScheme.primary"
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(73.dp)   // hauteur standard d’une app-bar
    )
}
