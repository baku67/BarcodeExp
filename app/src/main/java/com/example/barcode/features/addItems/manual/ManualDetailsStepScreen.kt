package com.example.barcode.features.addItems.manual

import android.graphics.Color as AndroidColor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.barcode.R
import com.example.barcode.features.addItems.AddItemDraft
import com.example.barcode.features.addItems.AddItemStepScaffold
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun ManualDetailsStepScreen(
    draft: AddItemDraft,
    onNext: (name: String, brand: String?, expiryMs: Long?) -> Unit,
    onBack: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current

    val taxonomy = rememberManualTaxonomy()

    val typeCode = draft.manualTypeCode
    val subtypeCode = draft.manualSubtypeCode

    val typeMeta = typeCode?.let { taxonomy?.typeMeta(it) }
    val subtypeMeta = subtypeCode?.let { taxonomy?.subtypeMeta(it) }

    // Header = SubType (fallback = Type si pas de subtype)
    val headerTitle = subtypeMeta?.title ?: typeMeta?.title ?: ""
    val headerImageResId = drawableId(context, subtypeMeta?.image ?: typeMeta?.image)

    // Palette fallback (si pas de gradient sur le subtype)
    val headerPaletteCode = typeCode ?: subtypeMeta?.parentCode ?: ""

    // Couleurs item (SUBTYPE) pour gradient background header + gradient titre
    val headerGradientColors: List<Color>? = remember(subtypeMeta) {
        val hexes = subtypeMeta?.gradient?.colors?.take(3) ?: return@remember null
        val parsed = hexes.mapNotNull { hex ->
            runCatching { Color(AndroidColor.parseColor(hex)) }.getOrNull()
        }
        parsed.takeIf { it.size >= 3 }
    }

    // "{ITEM}" pour le contenu taxonomy
    val itemTitleForContent = remember(subtypeMeta?.title, typeMeta?.title) {
        (subtypeMeta?.title ?: typeMeta?.title).orEmpty().ifBlank { "Cet aliment" }
    }
    val insert = remember(itemTitleForContent) { itemTitleForContent.lowercaseFirstEachLine() }

    val cs = MaterialTheme.colorScheme
    val gradientColorsForContent = remember(headerGradientColors, cs) {
        headerGradientColors ?: listOf(cs.primary, cs.tertiary, cs.secondary)
    }
    val tokenSpan = remember(gradientColorsForContent) {
        SpanStyle(
            brush = Brush.linearGradient(gradientColorsForContent),
            fontWeight = FontWeight.SemiBold
        )
    }
    val baseSpan = remember(cs) { SpanStyle(color = cs.onSurfaceVariant) }
    val accentColor = remember(gradientColorsForContent, cs) {
        gradientColorsForContent.getOrNull(1) ?: cs.primary
    }

    var name by rememberSaveable(draft.name, typeCode, subtypeCode) {
        mutableStateOf(draft.name.orEmpty())
    }

    val autoName = (subtypeMeta?.title ?: typeMeta?.title).orEmpty()

    LaunchedEffect(taxonomy, typeCode, subtypeCode) {
        // Remplit uniquement si l'user n’a rien tapé et que draft.name est vide
        if (draft.name.isNullOrBlank() && name.isBlank() && autoName.isNotBlank()) {
            name = autoName
        }
    }

    var brand by remember(draft.brand) { mutableStateOf(draft.brand.orEmpty()) }

    // ---- Date (délai moyen) ----
    val zoneId = remember { ZoneId.systemDefault() }

    val storageMinDays = subtypeMeta?.storageDaysMin
    val storageMaxDays = subtypeMeta?.storageDaysMax

    val avgDays: Int? = remember(storageMinDays, storageMaxDays) {
        val min = storageMinDays ?: return@remember null
        val max = storageMaxDays ?: min
        ((min + max) / 2f).roundToInt()
    }

    val recommendedExpiryMs: Long? = remember(avgDays, zoneId) {
        val d = avgDays ?: return@remember null
        localDateToEpochMillis(LocalDate.now(zoneId).plusDays(d.toLong()), zoneId)
    }

    val fallbackTodayMs = remember(zoneId) {
        localDateToEpochMillis(LocalDate.now(zoneId), zoneId)
    }

    var expiryMs by rememberSaveable(draft.expiryDate, recommendedExpiryMs) {
        mutableStateOf(draft.expiryDate ?: recommendedExpiryMs ?: fallbackTodayMs)
    }

    val plusJText by remember(zoneId) {
        derivedStateOf {
            val today = LocalDate.now(zoneId)
            val selected = epochMillisToLocalDate(expiryMs, zoneId)
            val delta = ChronoUnit.DAYS.between(today, selected).toInt()

            "(${if (delta >= 0) "+" else "-"} ${abs(delta)}j.)"
        }
    }

    val displayDateFormatter = remember {
        // Exemple attendu: "22 Jan. 2026"
        SimpleDateFormat("dd MMM. yyyy", Locale.ENGLISH)
    }

    val expiryDisplay = remember(expiryMs) {
        displayDateFormatter.format(Date(expiryMs))
    }

    val storageLineText = remember(storageMinDays, storageMaxDays) {
        val min = storageMinDays ?: return@remember null
        val max = storageMaxDays ?: min
        val rangeText = if (max != min) "$min-$max" else "$min"
        "Conservation approximative: $rangeText jours"
    }

    val isShortHeaderTitle = remember(headerTitle) {
        headerTitle.count { it.isLetterOrDigit() } <= 10
    }

    val detailsCenterGap = remember(isShortHeaderTitle) {
        if (isShortHeaderTitle) 28.dp else 12.dp
    }

    AddItemStepScaffold(
        step = 3,
        onBack = onBack,
        onCancel = onCancel
    ) { innerPadding ->
        // ✅ Header scrollable uniquement sur cet écran
        val scrollState = rememberScrollState()
        val showTopScrim by remember { derivedStateOf { scrollState.value > 0 } }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding) // topbar fixe
        ) {
            Box(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                ) {
                    // ✅ header DANS le scroll => il bouge avec la page
                    if (headerTitle.isNotBlank() && headerPaletteCode.isNotBlank()) {
                        ManualSubtypeFullBleedHeader(
                            typeTitle = headerTitle,
                            typeImageResId = headerImageResId,
                            palette = paletteForType(headerPaletteCode),
                            gradientColors = headerGradientColors,

                            centerContent = true,
                            centerGap = detailsCenterGap,

                            titleFontWeight = FontWeight.Light,
                            titleFontSize = 30.sp,
                            titleLineHeight = 28.sp,
                            titleShadow = Shadow(
                                color = Color.White,
                                offset = Offset(0f, 2f),
                                blurRadius = 1f
                            )
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 0.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TextField(
                            value = name,
                            onValueChange = { name = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Nom") },
                            placeholder = {
                                Text(
                                    text = if (autoName.isNotBlank()) "ex: $autoName…" else "ex: Carottes…"
                                )
                            },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,

                                // ✅ label “Nom” en couleur produit
                                focusedLabelColor = accentColor.copy(alpha = 0.90f),
                                unfocusedLabelColor = accentColor.copy(alpha = 0.70f),

                                // ✅ bottom line en couleur produit
                                focusedIndicatorColor = accentColor.copy(alpha = 0.70f),
                                unfocusedIndicatorColor = accentColor.copy(alpha = 0.40f),
                                disabledIndicatorColor = accentColor.copy(alpha = 0.25f),

                                cursorColor = accentColor,

                                // (optionnel) garde le texte neutre
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            )
                        )

                        Spacer(Modifier.height(8.dp))

                        // ---- Section Date (même fond + outline que "Conseils frigo") ----
                        val dateSectionBg = cs.surfaceVariant.copy(alpha = 0.18f)
                        val dateBorderColor = remember(accentColor) { accentColor.copy(alpha = 0.28f) }

                        OutlinedCard(
                            shape = MaterialTheme.shapes.large,
                            border = BorderStroke(1.dp, dateBorderColor),
                            colors = CardDefaults.outlinedCardColors(containerColor = dateSectionBg),
                            elevation = CardDefaults.outlinedCardElevation(defaultElevation = 0.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Date limite",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = accentColor.copy(alpha = 0.70f),
                                    modifier = Modifier.padding(start = 4.dp)
                                )

                                if (storageLineText != null) {
                                    Text(
                                        text = storageLineText!!,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            val d = epochMillisToLocalDate(expiryMs, zoneId).minusDays(1)
                                            expiryMs = localDateToEpochMillis(d, zoneId)
                                        },
                                        modifier = Modifier.size(44.dp),
                                        shape = CircleShape,
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                                    ) {
                                        Text("-")
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = expiryDisplay,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )

                                            if (plusJText.isNotEmpty()) {
                                                Text(
                                                    text = plusJText,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = accentColor,
                                                    modifier = Modifier.padding(start = 4.dp) // collé
                                                )
                                            }
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            val d = epochMillisToLocalDate(expiryMs, zoneId).plusDays(1)
                                            expiryMs = localDateToEpochMillis(d, zoneId)
                                        },
                                        modifier = Modifier.size(44.dp),
                                        shape = CircleShape,
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                                    ) {
                                        Text("+")
                                    }
                                }
                            }
                        }

                        // ✅ Volet "Conseils frigo" (taxonomy) — uniquement ici
                        subtypeMeta?.fridgeAdvise?.let { content ->
                            Spacer(Modifier.height(6.dp))
                            FridgeAdviseSectionCard(
                                content = content,
                                insert = insert,
                                baseSpan = baseSpan,
                                tokenSpan = tokenSpan,
                                accentColor = accentColor,
                            )
                        }

                        Spacer(Modifier.height(12.dp))
                    }
                }

                if (showTopScrim) {
                    TopEdgeFadeScrim(
                        modifier = Modifier.align(Alignment.TopCenter),
                        height = 18.dp
                    )
                }
            }

            // ✅ CTA reste fixe en bas
            Button(
                onClick = {
                    val cleanedName = name.trim()
                    if (cleanedName.isNotEmpty()) {
                        onNext(cleanedName, brand.trim().ifBlank { null }, expiryMs)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                enabled = name.trim().isNotEmpty()
            ) {
                Text("Ajouter")
            }
        }
    }
}

private const val ITEM_TOKEN = "{ITEM}"

@Composable
private fun FridgeAdviseSectionCard(
    content: ManualContent,
    insert: String,
    baseSpan: SpanStyle,
    tokenSpan: SpanStyle,
    accentColor: Color,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "fridgeAdviseArrowRotation"
    )

    val cs = MaterialTheme.colorScheme

    // Fond très léger (un poil plus présent quand ouvert)
    val sectionBgAlpha = if (expanded) 0.24f else 0.18f
    val sectionBg = cs.surfaceVariant.copy(alpha = sectionBgAlpha)

    val borderColor = remember(accentColor) { accentColor.copy(alpha = 0.28f) }
    val chevronTint = remember(accentColor) { accentColor.copy(alpha = 0.60f) }
    val bulletTint = remember(accentColor) { accentColor.copy(alpha = 0.75f) }

    // Halo smooth derrière l’icône
    val haloBrush = remember(accentColor) {
        Brush.radialGradient(
            colors = listOf(
                accentColor.copy(alpha = 0.18f),
                Color.Transparent
            )
        )
    }

    OutlinedCard(
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, borderColor),
        colors = CardDefaults.outlinedCardColors(containerColor = sectionBg),
        elevation = CardDefaults.outlinedCardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(haloBrush, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_nav_fridge_icon_thicc),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = cs.onSurface
                    )
                }

                Spacer(Modifier.width(10.dp))

                Text(
                    text = "Conseils frigo",
                    style = MaterialTheme.typography.titleMedium,
                    color = cs.onSurface,
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    imageVector = Icons.Outlined.ExpandMore,
                    contentDescription = if (expanded) "Réduire" else "Développer",
                    modifier = Modifier.rotate(rotation),
                    tint = chevronTint
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    Spacer(Modifier.height(2.dp))

                    when (content) {
                        is ManualContent.Markdown -> {
                            val paragraphs = remember(content.text) {
                                content.text
                                    .split("\n\n")
                                    .map { it.trim() }
                                    .filter { it.isNotBlank() }
                            }

                            paragraphs.forEachIndexed { idx, p ->
                                ManualInlineMarkdownText(
                                    template = p,
                                    insert = insert,
                                    baseSpan = baseSpan,
                                    tokenSpan = tokenSpan,
                                    boldColor = accentColor,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                if (idx != paragraphs.lastIndex) Spacer(Modifier.height(10.dp))
                            }
                        }

                        is ManualContent.Bullets -> {
                            content.items.forEach { b ->
                                Row(Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "•  ",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = bulletTint
                                    )

                                    ManualInlineMarkdownText(
                                        template = b,
                                        insert = insert,
                                        baseSpan = baseSpan,
                                        tokenSpan = tokenSpan,
                                        boldColor = accentColor,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ManualInlineMarkdownText(
    template: String,
    insert: String,
    baseSpan: SpanStyle,
    tokenSpan: SpanStyle,
    boldColor: Color,
    style: TextStyle,
    modifier: Modifier = Modifier,
) {
    val boldSpan = remember(baseSpan, boldColor) { baseSpan.copy(color = boldColor) }

    val annotated = remember(template, insert, baseSpan, tokenSpan, boldSpan) {
        buildInlineMarkdownAnnotatedString(
            template = template,
            insert = insert,
            baseSpan = baseSpan,
            tokenSpan = tokenSpan,
            boldSpan = boldSpan,
        )
    }

    Text(text = annotated, style = style, modifier = modifier)
}

/**
 * Markdown inline (subset) :
 * - **gras**
 * - *italique*
 * + support du placeholder {ITEM} injecté en gradient
 */
private fun buildInlineMarkdownAnnotatedString(
    template: String,
    insert: String,
    baseSpan: SpanStyle,
    tokenSpan: SpanStyle,
    boldSpan: SpanStyle? = null,
): AnnotatedString {
    var i = 0
    var bold = false
    var italic = false

    val out = AnnotatedString.Builder()
    val buf = StringBuilder()

    fun currentSpan(isToken: Boolean): SpanStyle {
        val base = when {
            isToken -> tokenSpan
            bold && boldSpan != null -> boldSpan
            else -> baseSpan
        }

        val weight = when {
            isToken && bold -> FontWeight.Bold
            bold -> FontWeight.SemiBold
            else -> base.fontWeight
        }

        val fStyle = if (italic) FontStyle.Italic else base.fontStyle
        return base.copy(fontWeight = weight, fontStyle = fStyle)
    }

    fun flush(isToken: Boolean = false) {
        if (buf.isEmpty()) return
        out.withStyle(currentSpan(isToken)) { append(buf.toString()) }
        buf.setLength(0)
    }

    while (i < template.length) {
        if (template.startsWith("**", i)) {
            flush()
            bold = !bold
            i += 2
            continue
        }
        if (template[i] == '*') {
            flush()
            italic = !italic
            i += 1
            continue
        }

        if (template.startsWith(ITEM_TOKEN, i)) {
            flush()
            buf.append(insert)
            flush(isToken = true)
            i += ITEM_TOKEN.length
            continue
        }

        buf.append(template[i])
        i += 1
    }

    flush()
    return out.toAnnotatedString()
}

private fun String.lowercaseFirstEachLine(): String =
    split('\n').joinToString("\n") { line ->
        val trimmed = line.trim()
        if (trimmed.isBlank()) line
        else trimmed.replaceFirstChar { ch -> ch.lowercase() }
    }

private fun epochMillisToLocalDate(epochMs: Long, zoneId: ZoneId): LocalDate {
    return Instant.ofEpochMilli(epochMs).atZone(zoneId).toLocalDate()
}

private fun localDateToEpochMillis(date: LocalDate, zoneId: ZoneId): Long {
    return date.atStartOfDay(zoneId).toInstant().toEpochMilli()
}
