package com.example.barcode.features.addItems.manual

import android.content.Context
import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.barcode.features.addItems.AddItemStepScaffold
import java.text.Normalizer

private data class SubtypeHit(
    val typeCode: String,
    val typeTitle: String,
    val typeImage: String?,
    val subtypeCode: String,
    val subtypeTitle: String,
    val subtypeImage: String?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualTypeStepScreen(
    onPick: (String) -> Unit,
    onBack: () -> Unit,
    onCancel: () -> Unit,
    onPickSubtype: ((typeCode: String, subtypeCode: String) -> Unit)? = null
) {
    val context = LocalContext.current
    val taxonomy = remember(context) { ManualTaxonomyRepository.get(context) }
    val types = taxonomy.types

    var query by rememberSaveable { mutableStateOf("") }
    val q = remember(query) { normalizeForSearch(query) }

    val allSubtypeHits = remember(types, taxonomy) {
        types.flatMap { t ->
            taxonomy.subtypesOf(t.code).map { s ->
                SubtypeHit(
                    typeCode = t.code,
                    typeTitle = t.title,
                    typeImage = t.image,
                    subtypeCode = s.code,
                    subtypeTitle = s.title,
                    subtypeImage = s.image
                )
            }
        }
    }

    val filteredHits = remember(q, allSubtypeHits) {
        if (q.isBlank()) emptyList()
        else allSubtypeHits
            .filter { normalizeForSearch(it.subtypeTitle).contains(q) }
            .sortedWith(compareBy({ it.typeTitle }, { it.subtypeTitle }))
    }

    val groupedHits = remember(filteredHits) { filteredHits.groupBy { it.typeCode } }

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

            ManualSubtypeSearchField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = "Rechercher un produit..."
            )

            Spacer(Modifier.height(6.dp))

            if (q.isBlank()) {
                val listState = rememberLazyListState()
                val showTopScrim by remember {
                    derivedStateOf {
                        listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
                    }
                }

                LaunchedEffect(q) { runCatching { listState.scrollToItem(0) } }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
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

                    if (showTopScrim) {
                        TopEdgeFadeScrim(
                            modifier = Modifier.align(Alignment.TopCenter),
                            height = 18.dp
                        )
                    }
                }
            } else {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    val cols = if (maxWidth >= 340.dp) 3 else 2
                    val gridState = rememberLazyGridState()
                    val showTopScrim by remember {
                        derivedStateOf {
                            gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 0
                        }
                    }

                    LaunchedEffect(q) { runCatching { gridState.scrollToItem(0) } }

                    Box(modifier = Modifier.fillMaxSize()) {
                        if (filteredHits.isEmpty()) {
                            Text(
                                text = "Aucun résultat",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            LazyVerticalGrid(
                                state = gridState,
                                columns = GridCells.Fixed(cols),
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                contentPadding = PaddingValues(bottom = 18.dp)
                            ) {
                                val orderedTypeCodes = types
                                    .map { it.code }
                                    .filter { groupedHits.containsKey(it) }

                                orderedTypeCodes.forEach { typeCode ->
                                    val group = groupedHits[typeCode] ?: emptyList()
                                    if (group.isEmpty()) return@forEach

                                    val any = group.first()
                                    val palette = paletteForType(typeCode)
                                    val typeImgRes = drawableId(context, any.typeImage)

                                    item(
                                        key = "header:$typeCode",
                                        span = { GridItemSpan(cols) }
                                    ) {
                                        TypeResultHeaderRow(
                                            title = any.typeTitle,
                                            imageResId = typeImgRes,
                                            palette = palette
                                        )
                                    }

                                    items(
                                        items = group,
                                        key = { hit -> "sub:${hit.typeCode}:${hit.subtypeCode}" }
                                    ) { hit ->
                                        val subImgRes = drawableId(context, hit.subtypeImage)

                                        ManualTaxonomyTileCard(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(1.05f),
                                            title = hit.subtypeTitle,
                                            palette = palette,
                                            imageResId = subImgRes,
                                            selected = false,
                                            onClick = {
                                                onPickSubtype?.invoke(hit.typeCode, hit.subtypeCode)
                                                    ?: onPick(hit.typeCode)
                                            }
                                        )
                                    }
                                }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ManualSubtypeSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Rechercher..."
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        singleLine = true,
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        placeholder = {
            Text(
                text = placeholder,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.70f)
            )
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            errorContainerColor = Color.Transparent,
            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
            unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f),
            disabledIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
            cursorColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
private fun TypeResultHeaderRow(
    title: String,
    @DrawableRes imageResId: Int,
    palette: TypePalette
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface
        )

        if (imageResId != 0) {
            Image(
                painter = painterResource(imageResId),
                contentDescription = null,
                modifier = Modifier.size(26.dp),
                contentScale = ContentScale.Fit
            )
        }

        Spacer(Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .height(1.dp)
                .weight(1f)
                .background(palette.accent.copy(alpha = 0.35f))
        )
    }
}

internal fun normalizeForSearch(raw: String): String {
    val s = raw.trim().lowercase()
    if (s.isEmpty()) return ""
    val n = Normalizer.normalize(s, Normalizer.Form.NFD)
    return n.replace(Regex("\\p{Mn}+"), "")
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
            val imageSlot = 104.dp
            val imageSize = 92.dp

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        modifier = Modifier
                            .widthIn(max = 280.dp)
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start
                    )
                }

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

internal fun paletteForType(code: String): TypePalette {
    return when (code) {
        "VEGETABLES" -> TypePalette(Color(0xFF0B241A), Color(0xFF2E8B57), Color(0xFFBFE7D3))
        "FRUITS" -> TypePalette(Color(0xFF2A0F3C), Color(0xFFC02A6B), Color(0xFFFFE1C7))
        "MEAT" -> TypePalette(Color(0xFF2A0A0D), Color(0xFFB11B2B), Color(0xFFF2B8B5))
        "FISH" -> TypePalette(Color(0xFF061A2D), Color(0xFF0F4C81), Color(0xFF8BE4D6))
        "DAIRY" -> TypePalette(Color(0xFF16436E), Color(0xFF9FE3FF), Color(0xFFFFF1D6))
        "LEFTOVERS" -> TypePalette(Color(0xFF0E3B43), Color(0xFF2C6E77), Color(0xFFCFE8D6))
        else -> TypePalette(Color(0xFF243038), Color(0xFF516773), Color(0xFFB6C2CA))
    }
}
