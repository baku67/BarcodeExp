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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.barcode.R
import com.example.barcode.common.ui.components.MonthWheelFormat
import com.example.barcode.common.ui.components.WheelDatePickerDialog
import com.example.barcode.core.UserPreferencesStore
import com.example.barcode.domain.models.ThemeMode
import com.example.barcode.features.addItems.AddItemDraft
import com.example.barcode.features.addItems.AddItemStepScaffold
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ManualDetailsStepScreen(
    draft: AddItemDraft,
    onNext: (name: String, brand: String?, expiryMs: Long?) -> Unit,
    onBack: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current

    val taxonomy = rememberManualTaxonomy()

    val prefsStore = remember(context) { UserPreferencesStore(context) }

    val themeMode by prefsStore.preferences
        .map { it.theme }
        .collectAsState(initial = ThemeMode.SYSTEM)

    val useDarkTheme = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val typeCode = draft.manualTypeCode
    val subtypeCode = draft.manualSubtypeCode

    val typeMeta = typeCode?.let { taxonomy?.typeMeta(it) }
    val subtypeMeta = subtypeCode?.let { taxonomy?.subtypeMeta(it) }

    // Header = SubType (fallback = Type si pas de subtype)
    val headerTitle = subtypeMeta?.title ?: typeMeta?.title ?: ""
    val headerImageResId = drawableId(context, subtypeMeta?.image ?: typeMeta?.image)

    // Palette fallback (si pas de gradient sur le subtype)
    val headerPaletteCode = typeCode ?: subtypeMeta?.parentCode ?: ""

    // ✅ Couleurs item (SUBTYPE) pour gradient background + gradient titre (comme GoodToKnowScreen)
    // (si absent => le header continue à utiliser le paletteForType() comme avant)
    val headerGradientColors: List<Color>? = remember(subtypeMeta) {
        val hexes = subtypeMeta?.gradient?.colors?.take(3) ?: return@remember null
        val parsed = hexes.mapNotNull { hex ->
            runCatching { Color(AndroidColor.parseColor(hex)) }.getOrNull()
        }
        parsed.takeIf { it.size >= 3 }
    }

    // ✅ "{ITEM}" pour le contenu taxonomy (comme GoodToKnowScreen)
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
        // ✅ ne remplit que si l'user n’a rien tapé et que draft.name est vide
        if (draft.name.isNullOrBlank() && name.isBlank() && autoName.isNotBlank()) {
            name = autoName
        }
    }

    var brand by remember(draft.brand) { mutableStateOf(draft.brand.orEmpty()) }

    var expiryMs by remember(draft.expiryDate) { mutableStateOf(draft.expiryDate) }
    var showWheel by remember { mutableStateOf(false) }

    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val expiryLabel = remember(expiryMs) {
        expiryMs?.let { dateFormatter.format(Date(it)) } ?: "Choisir une date"
    }

    AddItemStepScaffold(
        step = 3,
        onBack = onBack,
        onCancel = onCancel
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding) // header collé sous topbar
        ) {
            if (headerTitle.isNotBlank() && headerPaletteCode.isNotBlank()) {
                ManualSubtypeFullBleedHeader(
                    typeTitle = headerTitle,
                    typeImageResId = headerImageResId,
                    palette = paletteForType(headerPaletteCode),
                    gradientColors = headerGradientColors,

                    // ✅ centre un peu plus le titre + l'image
                    centerContent = true,

                    titleFontWeight = FontWeight.Light,
                    titleFontSize = 30.sp,
                    titleLineHeight = 28.sp,
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val scrollState = rememberScrollState()
                val showTopScrim by remember { derivedStateOf { scrollState.value > 0 } }

                Box(modifier = Modifier.weight(1f)) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Nom") },
                            placeholder = { Text("ex: Blanc de poulet, Carottes, Omelette...") },
                            singleLine = true
                        )

                        val storageLine = remember(
                            subtypeMeta?.title,
                            subtypeMeta?.storageDaysMin,
                            subtypeMeta?.storageDaysMax
                        ) {
                            val label = subtypeMeta?.title?.lowercase(Locale.getDefault())
                            val min = subtypeMeta?.storageDaysMin
                            val max = subtypeMeta?.storageDaysMax

                            if (label == null || min == null) return@remember null

                            val daysText = when {
                                max != null && max != min -> "$min–$max jours"
                                else -> "$min jours"
                            }

                            "Temps de conservation conseillé pour $label : $daysText"
                        }

                        if (storageLine != null) {
                            Text(
                                text = storageLine!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // ✅ Date limite via WheelDatePicker
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Date limite (optionnel)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { showWheel = true },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = expiryLabel,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (expiryMs != null) {
                                    TextButton(onClick = { expiryMs = null }) {
                                        Text("Effacer")
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

                    if (showTopScrim) {
                        TopEdgeFadeScrim(
                            modifier = Modifier.align(Alignment.TopCenter),
                            height = 18.dp
                        )
                    }
                }

                Button(
                    onClick = {
                        val cleanedName = name.trim()
                        if (cleanedName.isNotEmpty()) {
                            onNext(cleanedName, brand.trim().ifBlank { null }, expiryMs)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = name.trim().isNotEmpty()
                ) {
                    Text("Ajouter")
                }
            }
        }

        if (showWheel) {
            WheelDatePickerDialog(
                initialMillis = expiryMs,
                onConfirm = { pickedMillis ->
                    expiryMs = pickedMillis
                    showWheel = false
                },
                onDismiss = { showWheel = false },
                title = "Date limite",
                monthFormat = MonthWheelFormat.ShortText,
                showExpiredHint = true,
                useDarkTheme = useDarkTheme,
            )
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
    val density = LocalDensity.current
    val haloBrush = remember(accentColor, density) {
        val r = with(density) { 18.dp.toPx() }
        Brush.radialGradient(
            colors = listOf(
                accentColor.copy(alpha = 0.18f),
                Color.Transparent
            ),
            radius = r
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
                    text = "Astuces de stockage",
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
    style: androidx.compose.ui.text.TextStyle,
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
        if (template.startsWith("**", i)) { flush(); bold = !bold; i += 2; continue }
        if (template[i] == '*') { flush(); italic = !italic; i += 1; continue }

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
