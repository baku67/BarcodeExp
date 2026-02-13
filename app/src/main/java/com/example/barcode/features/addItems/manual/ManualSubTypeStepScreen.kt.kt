package com.example.barcode.features.addItems.manual

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.barcode.features.addItems.AddItemDraft
import com.example.barcode.features.addItems.AddItemStepScaffold
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.graphics.luminance

@Composable
fun ManualSubtypeStepScreen(
    draft: AddItemDraft,
    onPick: (ManualSubType) -> Unit,
    onBack: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val taxonomy = remember(context) { ManualTaxonomyRepository.get(context) }

    val type: ManualType? = draft.manualType
    val list = type?.let { taxonomy.subtypesOf(it) }.orEmpty()
    val typeMeta = type?.let { taxonomy.typeMeta(it) }

    AddItemStepScaffold(
        step = 2,
        onBack = onBack,
        onCancel = onCancel
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding) // ✅ collé sous la barre "Ajouter un produit (2/3)"
        ) {
            if (type != null) {
                ManualSubtypeFullBleedHeader(
                    typeTitle = typeMeta?.title ?: type.name,
                    typeImageResId = drawableId(context, typeMeta?.image),
                    palette = paletteForType(type.name)
                )
            }

            // ✅ tout le reste garde ton padding “page”
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (type == null) {
                    Text(
                        text = "Sous-types",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    AssistChip(onClick = onBack, label = { Text("Revenir au choix du type") })
                    return@AddItemStepScaffold
                }

                if (list.isEmpty()) {
                    Text(
                        "Aucun sous-type disponible pour ${typeMeta?.title ?: type.name}.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    return@AddItemStepScaffold
                }

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    val cols = if (maxWidth >= 340.dp) 3 else 2

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(cols),
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        contentPadding = PaddingValues(bottom = 2.dp)
                    ) {
                        items(list, key = { it.code }) { subMeta ->
                            val imageRes = drawableId(context, subMeta.image)

                            SubtypeTileCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1.05f),
                                title = subMeta.title,
                                parentTypeCode = type.name,
                                selected = draft.manualSubtype?.name == subMeta.code,
                                imageResId = imageRes,
                                onClick = {
                                    runCatching { ManualSubType.valueOf(subMeta.code) }
                                        .getOrNull()
                                        ?.let(onPick)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}





@Composable
private fun ManualSubtypeFullBleedHeader(
    typeTitle: String,
    typeImageResId: Int,
    palette: TypePalette
) {
    // ✅ Gradient plus “visible” en haut (plus saturé + alpha max)
    val topColor = lerp(palette.bg0, palette.accent, 0.35f)

    val gradient = Brush.verticalGradient(
        colorStops = arrayOf(
            0.00f to topColor.copy(alpha = 1.00f),
            0.45f to topColor.copy(alpha = 0.80f),
            1.00f to topColor.copy(alpha = 0.00f)
        )
    )

    val isLight = topColor.luminance() > 0.65f
    val titleColor = if (isLight) Color(0xFF0F172A) else Color.White

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(138.dp) // un peu plus compact vu qu’on n’a qu’un label
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(gradient)
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ✅ Label seul (ex: "Légumes")
            Text(
                text = typeTitle,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = titleColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // ✅ Image sans fond
            if (typeImageResId != 0) {
                Image(
                    painter = painterResource(typeImageResId),
                    contentDescription = null,
                    modifier = Modifier.size(92.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}





@Composable
private fun SubtypeTileCard(
    modifier: Modifier = Modifier,
    title: String,
    parentTypeCode: String,
    selected: Boolean,
    imageResId: Int,
    onClick: () -> Unit
) {
    val basePalette = paletteForType(parentTypeCode)

    val surface = MaterialTheme.colorScheme.surface
    val bg0 = lerp(surface, basePalette.bg0, if (selected) 0.22f else 0.14f)
    val bg1 = lerp(surface, basePalette.bg1, if (selected) 0.22f else 0.14f)

    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
    } else {
        basePalette.accent.copy(alpha = 0.30f)
    }

    val shape = RoundedCornerShape(22.dp)
    val gradient: Brush = Brush.linearGradient(listOf(bg0, bg1))
    val imageSize = 60.dp

    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = shape,
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 2.dp,
        border = BorderStroke(0.75.dp, borderColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 3.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
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

                Text(
                    text = title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 6.dp, end = 6.dp, bottom = 0.dp),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-6).dp, y = 6.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
