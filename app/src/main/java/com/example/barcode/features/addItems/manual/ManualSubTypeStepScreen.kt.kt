package com.example.barcode.features.addItems.manual

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.barcode.features.addItems.AddItemDraft
import com.example.barcode.features.addItems.AddItemStepScaffold

@Composable
fun ManualSubtypeStepScreen(
    draft: AddItemDraft,
    onPick: (String) -> Unit,
    onBack: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current

    val taxonomy = rememberManualTaxonomy()

    val typeCode: String? = draft.manualTypeCode
    val list = typeCode?.let { taxonomy?.subtypesOf(it) }.orEmpty()
    val typeMeta = typeCode?.let { taxonomy?.typeMeta(it) }

    var query by rememberSaveable(typeCode) { mutableStateOf("") }
    val q = remember(query) { normalizeForSearch(query) }
    val filteredList = remember(q, list) {
        if (q.isBlank()) list
        else list.filter { normalizeForSearch(it.title).contains(q) }
    }

    AddItemStepScaffold(
        step = 2,
        onBack = onBack,
        onCancel = onCancel
    ) { innerPadding ->

        if (draft.manualTypeCode != null && taxonomy == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@AddItemStepScaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (typeCode != null) {
                ManualSubtypeFullBleedHeader(
                    typeTitle = typeMeta?.title ?: typeCode,
                    typeImageResId = drawableId(context, typeMeta?.image),
                    palette = paletteForType(typeCode)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = ManualTaxonomyUiSpec.screenHPad,
                        vertical = ManualTaxonomyUiSpec.screenHPad
                    ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (typeCode == null) {
                    Text(
                        text = "Sous-types",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    AssistChip(onClick = onBack, label = { Text("Revenir au choix du type") })
                    return@AddItemStepScaffold
                }

                ManualSubtypeSearchField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth()
                )

                if (list.isEmpty()) {
                    Text(
                        "Aucun sous-type disponible pour ${typeMeta?.title ?: typeCode}.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    return@AddItemStepScaffold
                }

                if (q.isNotBlank() && filteredList.isEmpty()) {
                    Text(
                        text = "Aucun résultat",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    return@AddItemStepScaffold
                }

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    val cols = ManualTaxonomyUiSpec.colsFor(maxWidth)
                    val palette = remember(typeCode) { paletteForType(typeCode ?: "") }

                    val gridState = rememberLazyGridState()
                    val showTopScrim by remember {
                        derivedStateOf {
                            gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 0
                        }
                    }

                    LaunchedEffect(q) { runCatching { gridState.scrollToItem(0) } }

                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyVerticalGrid(
                            state = gridState,
                            columns = GridCells.Fixed(cols),
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(ManualTaxonomyUiSpec.gridGap),
                            horizontalArrangement = Arrangement.spacedBy(ManualTaxonomyUiSpec.gridGap),
                            contentPadding = PaddingValues(bottom = ManualTaxonomyUiSpec.gridBottomPad)
                        ) {
                            items(filteredList, key = { it.code }) { subMeta ->
                                val imageRes = drawableId(context, subMeta.image)

                                ManualTaxonomyTileCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(ManualTaxonomyUiSpec.tileAspect),
                                    title = subMeta.title,
                                    palette = palette,
                                    imageResId = imageRes,
                                    selected = draft.manualSubtypeCode == subMeta.code,
                                    onClick = { onPick(subMeta.code) }
                                )
                            }
                        }

                        if (showTopScrim) {
                            TopEdgeFadeScrim(
                                modifier = Modifier.align(Alignment.TopCenter),
                                height = 18.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ManualSubtypeFullBleedHeader(
    typeTitle: String,
    typeImageResId: Int,
    palette: TypePalette,
    gradientColors: List<Color>? = null, // ✅ AJOUT : permet au ManualDetailsStepScreen de passer le tri-color SUBTYPE
) {
    // Fallback historique (TYPE)
    val fallbackTopColor = lerp(palette.bg0, palette.accent, 0.28f)

    // ✅ Si on a un tri-color (SUBTYPE), on s'en sert pour le fond + le titre
    val c0 = gradientColors?.getOrNull(0)
    val c1 = gradientColors?.getOrNull(1) ?: c0
    val c2 = gradientColors?.getOrNull(2) ?: c1 ?: c0

    val heroBrush = remember(c0, c1, c2, fallbackTopColor) {
        if (c0 != null) {
            Brush.verticalGradient(
                colors = listOf(
                    c0.copy(alpha = 0.55f),
                    (c1 ?: c0).copy(alpha = 0.30f),
                    (c2 ?: c0).copy(alpha = 0.14f),
                    Color.Transparent
                )
            )
        } else {
            Brush.verticalGradient(
                colorStops = arrayOf(
                    0.00f to fallbackTopColor.copy(alpha = 0.92f),
                    0.42f to fallbackTopColor.copy(alpha = 0.62f),
                    1.00f to fallbackTopColor.copy(alpha = 0.00f)
                )
            )
        }
    }

    val titleBrush = remember(gradientColors) {
        gradientColors?.takeIf { it.size >= 2 }?.let { Brush.linearGradient(it) }
    }

    val luminanceBase = (c0 ?: fallbackTopColor).luminance()
    val isLight = luminanceBase > 0.65f
    val fallbackTitleColor = if (isLight) Color(0xFF0F172A) else Color.White

    val titleStyle = if (titleBrush != null) {
        MaterialTheme.typography.headlineSmall.copy(brush = titleBrush)
    } else {
        MaterialTheme.typography.headlineSmall
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(118.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(heroBrush)
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = typeTitle,
                modifier = Modifier.weight(1f),
                style = titleStyle,
                fontWeight = FontWeight.Black,
                color = if (titleBrush != null) Color.Unspecified else fallbackTitleColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (typeImageResId != 0) {
                Image(
                    painter = painterResource(typeImageResId),
                    contentDescription = null,
                    modifier = Modifier.size(96.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}
