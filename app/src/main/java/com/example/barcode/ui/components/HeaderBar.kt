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
import com.example.barcode.ui.theme.Manrope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeaderBar(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = Icons.Filled.Home, // icône de navigation par défaut
    onIconClick: () -> Unit = {}            // action au clic
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp) // ✅ padding
    ) {
        TopAppBar(
            navigationIcon = {
                IconButton(
                    onClick = onIconClick,
                    modifier = Modifier.padding(start = 6.dp, top = 6.dp, bottom = 6.dp) // ✅ padding
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.frigozen_icon),
                        contentDescription = "Logo",
                        modifier = Modifier.size(38.dp), // plus grand
                        contentScale = ContentScale.Fit
                    )
                }
            },
            title = {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(start = 6.dp, bottom = 6.dp) // ✅ + padding autour du titre
                ) {
                    Text(
                        text = title,
                        modifier = Modifier.absoluteOffset(x = 0.dp, y = -2.dp),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = Manrope,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 28.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = Color.White,
                navigationIconContentColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(86.dp) // ✅ + hauteur (avant 73.dp)
                .padding(horizontal = 3.dp) // ✅ un peu plus “spacieux”
        )
    }
}
