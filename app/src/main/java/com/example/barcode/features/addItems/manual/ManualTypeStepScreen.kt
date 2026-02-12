package com.example.barcode.features.addItems.manual

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Ajout manuel") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Filled.Close, contentDescription = "Annuler")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Choisis une catégorie",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Les couleurs des cartes sont calées sur tes illustrations WebP (512×512).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
            )

            Spacer(Modifier.height(6.dp))

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 172.dp),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(types, key = { it.code }) { meta ->
                    val imageResId = drawableId(context, meta.image)
                    val palette = paletteForType(meta.code)

                    BigTypeCard(
                        modifier = Modifier.fillMaxWidth(),
                        title = meta.title,
                        imageResId = imageResId,
                        palette = palette,
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

private data class TypePalette(
    val bg0: Color,
    val bg1: Color,
    val accent: Color
) {
    val gradient: Brush = Brush.linearGradient(listOf(bg0, bg1))
}

@Composable
private fun BigTypeCard(
    modifier: Modifier = Modifier,
    title: String,
    @DrawableRes imageResId: Int,
    palette: TypePalette,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(26.dp)

    Surface(
        onClick = onClick,
        modifier = modifier
            .aspectRatio(1.10f),
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 4.dp,
        border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.42f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(palette.gradient)
        ) {
            // léger "glow" pour donner du volume (sans casser les perfs)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0.14f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // bloc texte lisible même si la carte finit claire (ex: leftovers / dairy)
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(14.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.Black.copy(alpha = 0.18f))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Appuie pour choisir",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.82f)
                )
            }

            if (imageResId != 0) {
                Image(
                    painter = painterResource(imageResId),
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 10.dp, bottom = 8.dp)
                        // "big illustration" : plus présente, légèrement hors-centre
                        .size(132.dp)
                        .offset(x = 10.dp, y = 6.dp),
                    contentScale = ContentScale.Fit
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
 * Palettes inspirées directement des couleurs dominantes visibles sur tes WebP:
 * - légumes: verts profonds + accent tomate
 * - viandes: bordeaux/rouge + accent rosé
 * - poisson: bleu profond + accent saumon
 * - laitages: bleu "frais" + accent crème
 * - restes: brun chaud + accent vert (brocoli)
 */
private fun paletteForType(code: String): TypePalette {
    return when (code) {
        "VEGETABLES" -> TypePalette(
            bg0 = Color(0xFF0F2A12),
            bg1 = Color(0xFF2E7D32),
            accent = Color(0xFFE53935)
        )

        "MEAT" -> TypePalette(
            bg0 = Color(0xFF3B1418),
            bg1 = Color(0xFFC62828),
            accent = Color(0xFFFFB4A8)
        )

        "FISH" -> TypePalette(
            bg0 = Color(0xFF0B1E3A),
            bg1 = Color(0xFF1565C0),
            accent = Color(0xFFFF7043)
        )

        "DAIRY" -> TypePalette(
            bg0 = Color(0xFF123A63),
            bg1 = Color(0xFF4FC3F7),
            accent = Color(0xFFFFF1D6)
        )

        "LEFTOVERS" -> TypePalette(
            bg0 = Color(0xFF3E2723),
            bg1 = Color(0xFFD7A86E),
            accent = Color(0xFF43A047)
        )

        else -> TypePalette(
            bg0 = Color(0xFF263238),
            bg1 = Color(0xFF607D8B),
            accent = Color(0xFFB0BEC5)
        )
    }
}
