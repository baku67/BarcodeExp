package com.example.barcode.features.addItems.manual

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
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
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
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
                    palette = paletteForType(typeCode),
                    // ✅ Revenir à un header plus petit UNIQUEMENT ici
                    // (laisse ManualDetails sur le default 188.dp)
                    height = 118.dp
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

                    val gridState = rememberSaveable(typeCode, saver = LazyGridState.Saver) {
                        LazyGridState()
                    }

                    // évite le scroll-to-top au 1er affichage (et donc au retour écran)
                    var lastQ by rememberSaveable(typeCode) { mutableStateOf(q) }

                    val showTopScrim by remember {
                        derivedStateOf {
                            gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 0
                        }
                    }

                    LaunchedEffect(q) {
                        if (q != lastQ) {
                            runCatching { gridState.scrollToItem(0) }
                            lastQ = q
                        }
                    }

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
                                    gradientMeta = subMeta.gradient, // ✅ gradient du produit dans la card
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
    gradientColors: List<Color>? = null,
    isLarge: Boolean = false,
    height: Dp = if (isLarge) 220.dp else 188.dp,

    // ✅ mode “compact centré” (ManualDetails)
    centerContent: Boolean = false,

    // ✅ NEW: réglages optionnels pour ManualDetails (sans impacter ailleurs)
    // - centerGap: espace fixe entre titre et image en mode compact-centre
    // - centerEdgePadding: si > 0 => layout “semi-centré” (pas aux extrêmes) avec padding latéral interne
    centerGap: Dp = 12.dp,
    centerEdgePadding: Dp = 0.dp,

    // ✅ overrides (ManualDetails)
    titleFontWeight: FontWeight = FontWeight.Black,
    titleFontSize: TextUnit? = null,
    titleLineHeight: TextUnit? = null,
    titleShadow: Shadow? = null,
) {
    val fallbackTopColor = lerp(palette.bg0, palette.accent, 0.28f)

    val c0 = gradientColors?.getOrNull(0)
    val c1 = gradientColors?.getOrNull(1) ?: c0
    val c2 = gradientColors?.getOrNull(2) ?: c1 ?: c0

    val hPad = if (isLarge) 18.dp else 16.dp
    val vPad = if (isLarge) 14.dp else 8.dp
    val imageSize = if (isLarge) 104.dp else 96.dp
    val imageGapIfLarge = if (isLarge) 10.dp else 0.dp

    val bg = MaterialTheme.colorScheme.background
    val endY = with(LocalDensity.current) { height.toPx() * 1.35f }

    val heroBrush = remember(c0, c1, c2, fallbackTopColor, isLarge, bg, endY) {
        if (!isLarge) {
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
        } else {
            if (c0 != null) {
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.00f to c0.copy(alpha = 0.62f),
                        0.28f to (c1 ?: c0).copy(alpha = 0.38f),
                        0.56f to (c2 ?: c0).copy(alpha = 0.20f),
                        0.82f to (c2 ?: c0).copy(alpha = 0.08f),
                        1.00f to bg
                    ),
                    startY = 0f,
                    endY = endY
                )
            } else {
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.00f to fallbackTopColor.copy(alpha = 0.92f),
                        0.30f to fallbackTopColor.copy(alpha = 0.72f),
                        0.60f to fallbackTopColor.copy(alpha = 0.38f),
                        0.84f to fallbackTopColor.copy(alpha = 0.14f),
                        1.00f to bg
                    ),
                    startY = 0f,
                    endY = endY
                )
            }
        }
    }

    val titleBrush = remember(gradientColors) {
        gradientColors?.takeIf { it.size >= 2 }?.let { Brush.linearGradient(it) }
    }

    val luminanceBase = (c0 ?: fallbackTopColor).luminance()
    val isLight = luminanceBase > 0.65f
    val fallbackTitleColor = if (isLight) Color(0xFF0F172A) else Color.White

    val baseTitleStyle = if (titleBrush != null) {
        MaterialTheme.typography.headlineSmall.copy(brush = titleBrush)
    } else {
        MaterialTheme.typography.headlineSmall
    }

    val finalTitleStyle = remember(baseTitleStyle, titleFontSize, titleLineHeight, titleShadow) {
        baseTitleStyle.copy(
            fontSize = titleFontSize ?: baseTitleStyle.fontSize,
            lineHeight = titleLineHeight ?: baseTitleStyle.lineHeight,
            shadow = titleShadow ?: baseTitleStyle.shadow
        )
    }

    val compactCenter = centerContent && centerEdgePadding == 0.dp
    val titleTextAlign = if (compactCenter) TextAlign.Center else TextAlign.Start

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(heroBrush)
        )

        if (!centerContent) {
            // ✅ rendu actuel (Type/Subtype screens)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = hPad, vertical = vPad),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = typeTitle,
                    modifier = Modifier.weight(1f),
                    style = finalTitleStyle,
                    fontWeight = titleFontWeight,
                    color = if (titleBrush != null) Color.Unspecified else fallbackTitleColor,
                    textAlign = titleTextAlign,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (typeImageResId != 0) {
                    if (imageGapIfLarge > 0.dp) Spacer(Modifier.width(imageGapIfLarge))
                    Image(
                        painter = painterResource(typeImageResId),
                        contentDescription = null,
                        modifier = Modifier.size(imageSize),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        } else {
            // ✅ mode centré (ManualDetails) : compact OU “semi-centré” si centerEdgePadding > 0
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = hPad, vertical = vPad)
            ) {
                val hasImage = typeImageResId != 0

                if (!hasImage) {
                    Text(
                        text = typeTitle,
                        modifier = Modifier.align(Alignment.Center),
                        style = finalTitleStyle,
                        fontWeight = titleFontWeight,
                        color = if (titleBrush != null) Color.Unspecified else fallbackTitleColor,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                } else if (centerEdgePadding > 0.dp) {
                    // ✅ “semi-centré” : pas collé au centre, pas aux extrêmes (padding interne)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = centerEdgePadding)
                            .align(Alignment.Center),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = typeTitle,
                            modifier = Modifier.weight(1f),
                            style = finalTitleStyle,
                            fontWeight = titleFontWeight,
                            color = if (titleBrush != null) Color.Unspecified else fallbackTitleColor,
                            textAlign = TextAlign.Start,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(Modifier.width(12.dp))

                        Image(
                            painter = painterResource(typeImageResId),
                            contentDescription = null,
                            modifier = Modifier.size(imageSize),
                            contentScale = ContentScale.Fit
                        )
                    }
                } else {
                    // ✅ compact centré : le duo (titre + image) reste groupé et centré, avec un gap réglable
                    val maxTextWidth =
                        (maxWidth - imageSize - centerGap).coerceAtLeast(0.dp)

                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = typeTitle,
                            modifier = Modifier.widthIn(max = maxTextWidth),
                            style = finalTitleStyle,
                            fontWeight = titleFontWeight,
                            color = if (titleBrush != null) Color.Unspecified else fallbackTitleColor,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(Modifier.width(centerGap))

                        Image(
                            painter = painterResource(typeImageResId),
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
