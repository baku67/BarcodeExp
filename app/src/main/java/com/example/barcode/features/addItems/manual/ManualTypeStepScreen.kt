package com.example.barcode.features.addItems.manual

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualTypeStepScreen(
    onPick: (ManualType) -> Unit,
    onBack: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val taxonomy = remember(context) { ManualTaxonomyRepository.get(context) }
    val types = taxonomy.types

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Choisir un type") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Filled.Close, contentDescription = "Annuler")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Ajout manuel",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Sélectionne une catégorie (images WebP dans drawable).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f)
            )

            Spacer(Modifier.height(6.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(types, key = { it.code }) { meta ->
                    val imageResId = drawableId(context, meta.image)

                    BigGradientTypeCard(
                        modifier = Modifier.fillMaxWidth(),
                        title = meta.title,
                        imageResId = imageResId,
                        gradient = gradientForType(meta.code),
                        onClick = {
                            runCatching { ManualType.valueOf(meta.code) }
                                .getOrNull()
                                ?.let(onPick)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BigGradientTypeCard(
    modifier: Modifier = Modifier,
    title: String,
    @DrawableRes imageResId: Int,
    gradient: Brush,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(22.dp)

    Surface(
        onClick = onClick,
        modifier = modifier.height(118.dp),
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
            if (imageResId != 0) {
                Image(
                    painter = painterResource(imageResId),
                    contentDescription = null,
                    modifier = Modifier
                        .size(44.dp)
                        .align(Alignment.TopEnd),
                    contentScale = ContentScale.Fit
                )
            }

            Row(
                modifier = Modifier.align(Alignment.BottomStart),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
        }
    }
}

@DrawableRes
fun drawableId(context: Context, name: String?): Int {
    if (name.isNullOrBlank()) return 0
    return context.resources.getIdentifier(name, "drawable", context.packageName)
}

/**
 * Optionnel : tu peux changer ces gradients, ou remplacer par un gradient unique.
 */
private fun gradientForType(code: String): Brush {
    return when (code) {
        "VEGETABLES" -> Brush.linearGradient(listOf(Color(0xFF1B5E20), Color(0xFF43A047)))
        "MEAT" -> Brush.linearGradient(listOf(Color(0xFF6D2C2C), Color(0xFFC62828)))
        "FISH" -> Brush.linearGradient(listOf(Color(0xFF0D47A1), Color(0xFF1976D2)))
        "EGGS" -> Brush.linearGradient(listOf(Color(0xFF6A1B9A), Color(0xFF9C27B0)))
        "DAIRY" -> Brush.linearGradient(listOf(Color(0xFF006064), Color(0xFF26C6DA)))
        "LEFTOVERS" -> Brush.linearGradient(listOf(Color(0xFF3E2723), Color(0xFF8D6E63)))
        else -> Brush.linearGradient(listOf(Color(0xFF455A64), Color(0xFF78909C)))
    }
}
