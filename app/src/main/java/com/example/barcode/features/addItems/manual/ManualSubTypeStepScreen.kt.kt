package com.example.barcode.features.addItems.manual

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Sous-types",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = typeMeta?.title ?: type?.name.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (type == null) {
                Spacer(Modifier.height(8.dp))
                AssistChip(
                    onClick = onBack,
                    label = { Text("Revenir au choix du type") }
                )
                return@AddItemStepScaffold
            }

            if (list.isEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Aucun sous-type disponible pour ${typeMeta?.title ?: type.name}.",
                    style = MaterialTheme.typography.bodyMedium
                )
                return@AddItemStepScaffold
            }

            Spacer(Modifier.height(6.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(list, key = { it.code }) { subMeta ->
                    val imageRes = drawableId(context, subMeta.image)

                    SmallSubtypeCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(84.dp),
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

@Composable
private fun SmallSubtypeCard(
    modifier: Modifier = Modifier,
    title: String,
    parentTypeCode: String,
    selected: Boolean,
    imageResId: Int,
    onClick: () -> Unit
) {
    // Même layout que ManualTypeStepScreen, mais visiblement “sous-niveau” :
    // - bordure plus fine
    // - image plus petite
    // - label plus petit
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

    val imageSlot = 88.dp
    val imageSize = 62.dp

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
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Texte: aligné à gauche avec un “effet centré”
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
                            .padding(horizontal = 10.dp),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start
                    )
                }

                // Slot image fixe -> image exactement au même endroit pour toutes les lignes
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

                    // Check overlay (ne décale rien)
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
    }
}
