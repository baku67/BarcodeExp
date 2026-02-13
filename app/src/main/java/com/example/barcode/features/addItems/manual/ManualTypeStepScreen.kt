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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.barcode.features.addItems.AddItemStepScaffold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualTypeStepScreen(
    onPick: (String) -> Unit,
    onBack: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val taxonomy = remember(context) { ManualTaxonomyRepository.get(context) }
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
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Choisis une catégorie",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(Modifier.height(6.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(types, key = { it.code }) { meta ->
                    val imageResId = drawableId(context, meta.image)
                    val palette = paletteForType(meta.code)

                    BigTypeCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(112.dp),
                        title = meta.title,
                        imageResId = imageResId,
                        palette = palette,
                        onClick = { onPick(meta.code) }
                    )
                }
            }
        }
    }
}

internal data class TypePalette(
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
        modifier = modifier,
        shape = shape,
        color = Color.Transparent,
        tonalElevation = 1.dp,
        shadowElevation = 4.dp,
        border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.42f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.gradient)
        ) {
            // Dans BigTypeCard(...) -> à l'intérieur du Box(...) qui est déjà .fillMaxSize().background(...)

            val imageSlot = 104.dp
            val imageSize = 92.dp

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Zone texte : centrée dans l'espace restant, mais texte aligné à gauche
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        modifier = Modifier
                            .widthIn(max = 280.dp) // <-- donne l'effet "centré" (ajuste 260-320 selon ton feeling)
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start // ✅ left align (même sur 2 lignes)
                    )
                }

                // Slot image fixe : position identique pour tous les boutons
                Box(
                    modifier = Modifier
                        .width(imageSlot)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    if (imageResId != 0) {
                        Image(
                            painter = painterResource(imageResId),
                            contentDescription = null,
                            modifier = Modifier.size(imageSize),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
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
internal fun paletteForType(code: String): TypePalette {
    return when (code) {

        // 🌿 Légumes — vert forêt -> vert feuille, accent menthe
        "VEGETABLES" -> TypePalette(
            bg0 = Color(0xFF0B241A), // forêt profonde (plus doux que noir/vert pur)
            bg1 = Color(0xFF2E8B57), // vert feuille nuancé
            accent = Color(0xFFBFE7D3) // menthe légère (bordure propre)
        )

        // 🥩 Viandes — bordeaux -> rouge profond, accent rosé “chair”
        "MEAT" -> TypePalette(
            bg0 = Color(0xFF2A0A0D), // bordeaux sombre
            bg1 = Color(0xFFB11B2B), // rouge profond (moins “flashy”)
            accent = Color(0xFFF2B8B5) // rosé doux
        )

        // 🐟 Poissons / Fruits de mer — bleu nuit -> bleu océan, accent bleu-vert frais
        "FISH" -> TypePalette(
            bg0 = Color(0xFF061A2D), // bleu nuit (très profond)
            bg1 = Color(0xFF0F4C81), // bleu océan (nuancé)
            accent = Color(0xFF8BE4D6) // aqua doux (rappelle “frais” sans orange)
        )

        // 🥛 Produits laitiers — blanc bleuté -> bleu poudre, accent blanc/ice
        "DAIRY" -> TypePalette(
            bg0 = Color(0xFF16436E),
            bg1 = Color(0xFF9FE3FF),
            accent = Color(0xFFFFF1D6)
        )

        // 🍱 Restes / Tupperware — proposition la plus jolie (à mon avis) :
        // “plastique teal” + “vert sauge” (propre, moderne, et distinct des autres catégories)
        "LEFTOVERS" -> TypePalette(
            bg0 = Color(0xFF0E3B43), // teal sombre
            bg1 = Color(0xFF2C6E77), // teal moyen
            accent = Color(0xFFCFE8D6) // sauge / menthe claire
        )

        else -> TypePalette(
            bg0 = Color(0xFF243038),
            bg1 = Color(0xFF516773),
            accent = Color(0xFFB6C2CA)
        )
    }
}

