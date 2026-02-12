package com.example.barcode.features.addItems.manual

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.barcode.features.addItems.AddItemStepScaffold
import androidx.compose.foundation.lazy.grid.items



@Composable
fun ManualTypeStepScreen(
    onPick: (ManualType) -> Unit,
    onBack: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val taxonomy = remember { ManualTaxonomyRepository.get(context) }
    val types = taxonomy.types

    AddItemStepScaffold(
        step = 1,
        onBack = onBack,
        onCancel = onCancel
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "Choisir un type",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Pour les produits sans code-barres (marché, boucher, restes, etc.).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f)
            )

            Spacer(Modifier.height(6.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(types) { meta ->
                    BigGradientTypeCard(
                        modifier = Modifier.fillMaxWidth(),
                        title = meta.title,
                        subtitle = meta.subtitle ?: "",
                        icon = illustrationFor(meta.iconKey, Icons.Outlined.Restaurant),
                        gradient = gradientForType(meta.code),
                        onClick = { onPick(ManualType.valueOf(meta.code)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun gradientForType(code: String): Brush {
    return when (code) {
        "VEGETABLES" -> Brush.linearGradient(listOf(Color(0xFF1B5E20), Color(0xFF43A047), Color(0xFFB2FF59)))
        "MEAT" -> Brush.linearGradient(listOf(Color(0xFF4E342E), Color(0xFFB71C1C), Color(0xFFFF7043)))
        "DAIRY" -> Brush.linearGradient(listOf(Color(0xFF0D47A1), Color(0xFF1976D2), Color(0xFF80DEEA)))
        "LEFTOVERS" -> Brush.linearGradient(listOf(Color(0xFF311B92), Color(0xFF7C4DFF), Color(0xFFFFD54F)))
        else -> Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))
    }
}

@Composable
private fun BigGradientTypeCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradient: Brush,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(22.dp)

    Surface(
        onClick = onClick,
        modifier = modifier.height(122.dp),
        shape = shape,
        color = Color.Transparent,
        tonalElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(gradient)
                .padding(14.dp)
        ) {
            // Icône “illustration”
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.92f),
                modifier = Modifier
                    .size(34.dp)
                    .align(Alignment.TopEnd)
            )

            // Texte
            Column(
                modifier = Modifier.align(Alignment.BottomStart),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
