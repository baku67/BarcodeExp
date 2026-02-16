package com.example.barcode.features.fridge.components.bottomSheetDetails

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.barcode.common.expiry.ExpiryLevel
import com.example.barcode.common.expiry.ExpiryPolicy
import com.example.barcode.common.expiry.expiryLevel
import com.example.barcode.common.ui.expiry.expiryStrokeColor
import com.example.barcode.data.local.entities.ItemEntity
import com.example.barcode.data.local.entities.ItemNoteEntity
import com.example.barcode.features.addItems.manual.MANUAL_TYPES_WITH_SUBTYPE_IMAGE
import com.example.barcode.features.addItems.manual.ManualTaxonomyImageResolver
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId

@Composable
fun ItemDetailsBottomSheet(
    itemEntity: ItemEntity,
    notes: List<ItemNoteEntity> = emptyList(),
    onAddNote: (String, Boolean) -> Unit = { _, _ -> },
    onDeleteNote: (String) -> Unit = {},
    onClose: () -> Unit,
    onOpenViewer: (List<ViewerImage>, Int) -> Unit,
    onEdit: (ItemEntity) -> Unit = {},
    onRemove: (ItemEntity) -> Unit = {},
    onAddToFavorites: (ItemEntity) -> Unit = {},
    onAddToShoppingList: (ItemEntity) -> Unit = {},
) {
    // TODO: branche soonDays sur tes Settings
    val expiryPolicy = remember { ExpiryPolicy(soonDays = 2) }

    val level = remember(itemEntity.expiryDate) {
        expiryLevel(itemEntity.expiryDate, expiryPolicy)
    }
    val isWarning = level == ExpiryLevel.EXPIRED || level == ExpiryLevel.SOON

    val strokeColor by animateColorAsState(
        targetValue = expiryStrokeColor(itemEntity.expiryDate, expiryPolicy),
        label = "sheetStrokeColor"
    )

    val context = LocalContext.current
    val pkg = context.packageName

    val isManual = remember(itemEntity.addMode) {
        itemEntity.addMode.equals("manual", ignoreCase = true)
    }

    val isManualLeftovers = remember(isManual, itemEntity.manualType) {
        isManual && itemEntity.manualType?.equals("LEFTOVERS", ignoreCase = true) == true
    }

    val effectivePreviewUrl = remember(
        itemEntity.addMode,
        itemEntity.manualType,
        itemEntity.manualSubtype,
        itemEntity.imageUrl,
        pkg
    ) {
        if (!isManual) return@remember itemEntity.imageUrl

        val type = itemEntity.manualType?.trim().orEmpty()
        val subtype = itemEntity.manualSubtype?.trim().orEmpty()

        // 1) Sous-type (VEGETABLES/MEAT/FISH/DAIRY)
        if (type in MANUAL_TYPES_WITH_SUBTYPE_IMAGE && subtype.isNotBlank()) {
            val resId = ManualTaxonomyImageResolver.resolveSubtypeDrawableResId(context, subtype)
            if (resId != 0) return@remember "android.resource://$pkg/$resId"
        }

        // 2) Type (ex: LEFTOVERS n’a pas de sous-type)
        if (type.isNotBlank()) {
            val resId = ManualTaxonomyImageResolver.resolveTypeDrawableResId(context, type)
            if (resId != 0) return@remember "android.resource://$pkg/$resId"
        }

        itemEntity.imageUrl
    }

    val viewerImages = remember(
        itemEntity.imageUrl,
        effectivePreviewUrl,
        itemEntity.imageIngredientsUrl,
        itemEntity.imageNutritionUrl
    ) {
        buildViewerImages(
            previewUrl = effectivePreviewUrl,
            ingredientsUrl = itemEntity.imageIngredientsUrl,
            nutritionUrl = itemEntity.imageNutritionUrl
        )
    }

    val openViewerFromUrl: (String) -> Unit = open@{ clickedUrl ->
        if (viewerImages.isEmpty()) return@open
        val startIndex = viewerImages.indexOfFirst { it.url == clickedUrl }.let { idx ->
            if (idx >= 0) idx else 0
        }
        onOpenViewer(viewerImages, startIndex)
    }

    val openViewerFromKind: (ViewerImageKind) -> Unit = open@{ kind ->
        if (viewerImages.isEmpty()) return@open
        val startIndex = viewerImages.indexOfFirst { it.kind == kind }.let { idx ->
            if (idx >= 0) idx else 0
        }
        onOpenViewer(viewerImages, startIndex)
    }

    val listState = rememberLazyListState()
    val sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

    val cs = MaterialTheme.colorScheme
    val surface = cs.surface
    val elevated = cs.surfaceColorAtElevation(6.dp)

    // ✅ pas de “vert” au milieu quand warning : on n’utilise pas surfaceColorAtElevation
    val tint = strokeColor.copy(alpha = 1f)
    val topTint = if (isWarning) lerp(surface, tint, 0.26f) else lerp(surface, tint, 0.10f)
    val midTint = if (isWarning) lerp(surface, tint, 0.14f) else elevated

    val sheetBrush = remember(surface, topTint, midTint) {
        Brush.verticalGradient(
            colorStops = arrayOf(
                0f to topTint,
                0.55f to midTint,
                1f to surface
            )
        )
    }

    Box(
        Modifier
            .fillMaxWidth()
            .clip(sheetShape)
            .background(sheetBrush)
            .border(
                width = 1.dp,
                color = cs.outlineVariant.copy(alpha = 0.35f),
                shape = sheetShape
            )
    ) {
        Column(Modifier.fillMaxWidth()) {

            CornerRadiusEtPoignee(
                radius = 28.dp,
                strokeWidth = 2.dp,
                strokeColor = strokeColor,
                handleHeight = 4.dp,
                topEndContent = null
            )

            Spacer(Modifier.height(6.dp))

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 18.dp),
                contentPadding = PaddingValues(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item(key = "header") {
                    BottomSheetDetailsHeaderContent(
                        itemEntity = itemEntity,
                        previewImageUrl = effectivePreviewUrl,
                        onClose = onClose,
                        onOpenViewer = openViewerFromKind
                    )
                }

                // ✅ Barcode_scan (et autres) => boutons images
                if (!isManual) {
                    item(key = "open_images") {
                        DetailsOpenImageButtons(
                            ingredientsUrl = itemEntity.imageIngredientsUrl,
                            nutritionUrl = itemEntity.imageNutritionUrl,
                            onOpenViewer = openViewerFromUrl
                        )
                    }
                }

                // ✅ Manual + NOT leftovers => GoodToKnow
                if (isManual && !isManualLeftovers) {
                    item(key = "good_to_know") {
                        GoodToKnowCollapsibleSection(
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // ✅ Manual + leftovers => Infos plat (manualMetaJson)
                if (isManualLeftovers) {
                    item(key = "leftovers_meta") {
                        LeftoversMetaCollapsibleSection(
                            manualMetaJson = itemEntity.manualMetaJson,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }


                item(key = "notes") {
                    NotesCollapsibleSection(
                        notes = notes,
                        onAddNote = onAddNote,
                        onDeleteNote = onDeleteNote,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        BottomSheetMenuButton(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 10.dp, end = 12.dp)
                .zIndex(10f),
            onEdit = { onEdit(itemEntity) },
            onRemove = { onRemove(itemEntity) },
            onAddToFavorites = { onAddToFavorites(itemEntity) },
            onAddToShoppingList = { onAddToShoppingList(itemEntity) },
        )
    }
}

@Composable
private fun DetailsOpenImageButtons(
    ingredientsUrl: String?,
    nutritionUrl: String?,
    onOpenViewer: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        DetailsTabButton(
            text = "Ingrédients",
            icon = Icons.Outlined.Science,
            selected = false,
            enabled = !ingredientsUrl.isNullOrBlank(),
            onClick = { ingredientsUrl?.let(onOpenViewer) },
            modifier = Modifier.weight(1f)
        )

        DetailsTabButton(
            text = "Nutrition",
            icon = Icons.Outlined.FactCheck,
            selected = false,
            enabled = !nutritionUrl.isNullOrBlank(),
            onClick = { nutritionUrl?.let(onOpenViewer) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun TopRoundedStroke(
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 2.dp,
    radius: Dp = 28.dp,
    color: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
    edgeFadePct: Float = 0.05f // 5%
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(radius)
    ) {
        val w = size.width
        val sw = strokeWidth.toPx()
        val r = radius.toPx().coerceAtMost(w / 2f)
        val y = sw / 2f

        val leftRect = Rect(0f, y, 2 * r, 2 * r + y)
        val rightRect = Rect(w - 2 * r, y, w, 2 * r + y)

        val path = Path().apply {
            moveTo(0f, r + y)
            arcTo(leftRect, 180f, 90f, false)
            lineTo(w - r, y)
            arcTo(rightRect, 270f, 90f, false)
        }

        val fade = edgeFadePct.coerceIn(0f, 0.49f)

        val brush = Brush.horizontalGradient(
            colorStops = arrayOf(
                0f to color.copy(alpha = 0f),
                fade to color,
                (1f - fade) to color,
                1f to color.copy(alpha = 0f)
            ),
            startX = 0f,
            endX = w
        )

        drawPath(
            path = path,
            brush = brush,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = sw,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

@Composable
private fun CornerRadiusEtPoignee(
    modifier: Modifier = Modifier,
    radius: Dp = 28.dp,
    strokeWidth: Dp = 2.dp,
    strokeColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
    handleWidth: Dp = 44.dp,
    handleHeight: Dp = 4.dp,
    topEndContent: @Composable (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(radius)
    ) {
        TopRoundedStroke(
            modifier = Modifier.matchParentSize(),
            strokeWidth = strokeWidth,
            radius = radius,
            color = strokeColor
        )

        if (topEndContent != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 2.dp, end = 8.dp)
                    .zIndex(1f)
            ) {
                topEndContent()
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp)
                .width(handleWidth)
                .height(handleHeight)
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f))
        )
    }
}

@Composable
private fun DetailsTabButton(
    text: String,
    icon: ImageVector? = null,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme

    val bg = when {
        !enabled -> cs.surfaceVariant.copy(alpha = 0.35f)
        selected -> cs.primary.copy(alpha = 0.14f)
        else -> cs.surface
    }

    val border = when {
        !enabled -> cs.outlineVariant.copy(alpha = 0.25f)
        selected -> cs.primary.copy(alpha = 0.55f)
        else -> cs.outlineVariant.copy(alpha = 0.80f)
    }

    val content = when {
        !enabled -> cs.onSurface.copy(alpha = 0.22f)
        selected -> cs.primary
        else -> cs.onSurface.copy(alpha = 0.82f)
    }

    val shape = RoundedCornerShape(14.dp)

    Box(
        modifier = modifier
            .height(44.dp)
            .clip(shape)
            .background(bg)
            .border(1.dp, border, shape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = content,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
            }

            Text(
                text = text,
                fontWeight = FontWeight.SemiBold,
                color = content
            )
        }
    }
}

private fun buildViewerImages(
    previewUrl: String?,
    ingredientsUrl: String?,
    nutritionUrl: String?
): List<ViewerImage> {
    val out = mutableListOf<ViewerImage>()

    previewUrl?.trim()?.takeIf { it.isNotBlank() }?.let {
        out += ViewerImage(ViewerImageKind.Preview, it)
    }

    ingredientsUrl?.trim()?.takeIf { it.isNotBlank() }?.let {
        out += ViewerImage(ViewerImageKind.Ingredients, it)
    }

    nutritionUrl?.trim()?.takeIf { it.isNotBlank() }?.let {
        out += ViewerImage(ViewerImageKind.Nutrition, it)
    }

    return out
}


private const val DAY_MS = 24L * 60L * 60L * 1000L

private data class LeftoversMeta(
    val cookedAtMs: Long?,
    val storageDays: Int?,
    val containsMeat: Boolean,
    val containsCream: Boolean,
    val containsRice: Boolean,
    val containsFish: Boolean,
    val containsEggs: Boolean,
    val portions: Int?,
    val notes: String?
)

@Composable
private fun LeftoversMetaCollapsibleSection(
    manualMetaJson: String?,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    val meta = remember(manualMetaJson) { parseLeftoversMeta(manualMetaJson) }

    val shape = RoundedCornerShape(18.dp)
    val accent = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surface

    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        label = "goodToKnowChevronRotation"
    )

    val borderColor by animateColorAsState(
        targetValue = if (expanded) accent.copy(alpha = 0.55f) else accent.copy(alpha = 0.28f),
        label = "leftoversBorder"
    )

    val bgBrush = Brush.verticalGradient(
        0f to accent.copy(alpha = 0.12f),
        1f to surface
    )

    Column(
        modifier = modifier
            .clip(shape)
            .background(bgBrush)
            .border(1.dp, borderColor, shape)
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(Modifier.width(10.dp))

            Text(
                text = "Infos du plat",
                fontWeight = FontWeight.SemiBold,
                color = accent
            )

            Spacer(Modifier.weight(1f))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = accent.copy(alpha = 0.95f),
                modifier = Modifier
                    .size(22.dp)
                    .rotate(chevronRotation)
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(animationSpec = tween(220)) + fadeIn(animationSpec = tween(220)),
            exit = shrinkVertically(animationSpec = tween(180)) + fadeOut(animationSpec = tween(180))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .padding(bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (meta == null) {
                    Text(
                        text = "Aucune information enregistrée.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    return@Column
                }

                val cooked = meta.cookedAtMs
                val expiry = if (meta.cookedAtMs != null && meta.storageDays != null) {
                    meta.cookedAtMs + meta.storageDays.toLong() * DAY_MS
                } else null

                InfoLine("Cuisson", cooked?.let(::formatDate) ?: "—")
                InfoLine("DLC estimée", if (meta.storageDays != null && expiry != null) "J+${meta.storageDays} → ${formatDate(expiry)}" else "—")

                if (meta.portions != null) {
                    InfoLine("Portions", meta.portions.toString())
                }

                val composition = buildList {
                    if (meta.containsMeat) add("Viande")
                    if (meta.containsFish) add("Poisson")
                    if (meta.containsEggs) add("Œufs")
                    if (meta.containsCream) add("Crème")
                    if (meta.containsRice) add("Riz")
                }.joinToString(", ").ifBlank { "—" }

                InfoLine("Composition", composition)

                if (!meta.notes.isNullOrBlank()) {
                    InfoLine("Notes", meta.notes)
                }
            }
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "$label : ",
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f)
        )
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
            modifier = Modifier.weight(1f)
        )
    }
}

private fun parseLeftoversMeta(json: String?): LeftoversMeta? {
    val s = json?.trim().orEmpty()
    if (s.isBlank()) return null

    return runCatching {
        val o = JSONObject(s)

        // On tolère l'absence de "kind", mais si présent et pas leftovers => on ignore
        val kind = o.optString("kind", "")
        if (kind.isNotBlank() && kind != "leftovers") return@runCatching null

        val cookedAtMs = if (o.has("cookedAt")) o.optLong("cookedAt") else null
        val storageDays = if (o.has("storageDays")) o.optInt("storageDays") else null

        LeftoversMeta(
            cookedAtMs = cookedAtMs,
            storageDays = storageDays,
            containsMeat = o.optBoolean("containsMeat", false),
            containsCream = o.optBoolean("containsCream", false),
            containsRice = o.optBoolean("containsRice", false),
            containsFish = o.optBoolean("containsFish", false),
            containsEggs = o.optBoolean("containsEggs", false),
            portions = if (o.has("portions")) o.optInt("portions") else null,
            notes = o.optString("notes", "").takeIf { it.isNotBlank() }
        )
    }.getOrNull()
}

private fun formatDate(ms: Long): String {
    val zone = ZoneId.systemDefault()
    val d = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()
    return "%02d/%02d/%04d".format(d.dayOfMonth, d.monthValue, d.year)
}

