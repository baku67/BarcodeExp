package com.example.barcode.features.fridge.components.bottomSheetDetails

import android.graphics.Color as AndroidColor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.barcode.R
import com.example.barcode.features.addItems.manual.ManualContent
import com.example.barcode.features.addItems.manual.ManualTaxonomyImageResolver
import com.example.barcode.features.addItems.manual.ManualTaxonomyRepository
import com.example.barcode.features.addItems.manual.rememberManualTaxonomy
import kotlin.math.abs

private const val ITEM_TOKEN = "{ITEM}"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoodToKnowScreen(
    itemName: String, // code subtype/type (ex: "VEG_CARROT")
    onClose: () -> Unit
) {
    val context = LocalContext.current

    // ✅ async + cache (repo gère Dispatchers.IO)
    val taxonomy = rememberManualTaxonomy()

    val code = remember(itemName) { itemName.trim() }
    val subtype = remember(code, taxonomy) { taxonomy?.subtypeMeta(code) }
    val typeMeta = remember(code, taxonomy) { taxonomy?.typeMeta(code) }

    val resolvedTitle = remember(code, subtype, typeMeta) {
        when {
            code.isBlank() -> "Cet aliment"
            else -> subtype?.title
                ?: typeMeta?.title
                ?: prettifyTaxonomyCodeForUi(code)
        }
    }

    // "Carottes" -> "carottes"
    val insert = remember(resolvedTitle) { resolvedTitle.lowercaseFirstEachLine() }

    // ✅ tri-color du JSON (fallback si absent)
    val cs = MaterialTheme.colorScheme
    val gradientColors: List<Color> = remember(subtype, cs) {
        val hexes = subtype?.gradient?.colors?.take(3).orEmpty()
        val parsed = hexes.mapNotNull { hex ->
            runCatching { Color(AndroidColor.parseColor(hex)) }.getOrNull()
        }
        if (parsed.size >= 3) parsed else listOf(cs.primary, cs.tertiary, cs.secondary)
    }

    val markdownBoldColor = remember(gradientColors, cs.surface) {
        gradientColors.maxByOrNull { abs(it.luminance() - cs.surface.luminance()) }
            ?: cs.primary
    }

    val tokenSpan = remember(gradientColors) {
        SpanStyle(
            brush = Brush.linearGradient(gradientColors),
            fontWeight = FontWeight.SemiBold
        )
    }

    val bodySpan = remember(cs) { SpanStyle(color = cs.onSurfaceVariant) }
    val textSpan = remember(cs) { SpanStyle(color = cs.onSurface) }

    // ✅ image header : recalcul quand taxonomy arrive (sinon reste sur fallback)
    val headerImageResId = remember(context, code, taxonomy) {
        if (taxonomy == null || code.isBlank()) return@remember 0

        val subRes = ManualTaxonomyImageResolver.resolveSubtypeDrawableResId(context, code)
        if (subRes != 0) subRes
        else ManualTaxonomyImageResolver.resolveTypeDrawableResId(context, code)
    }.takeIf { it != 0 }

    // ✅ sections depuis JSON
    val fridgeAdvise = subtype?.fridgeAdvise
    val healthGood = subtype?.healthGood
    val healthWarning = subtype?.healthWarning
    val goodToKnow = subtype?.goodToKnow

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bon à savoir") },
                actions = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Outlined.Close, contentDescription = "Fermer")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ✅ Loader tant que la taxonomy n'est pas prête (évite écran vide)
            if (taxonomy == null) {
                item {
                    Spacer(Modifier.height(24.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else {
                item {
                    GoodToKnowHeader(
                        imageResId = headerImageResId,
                        gradientColors = gradientColors,
                        insert = insert,
                        baseTitleSpan = textSpan,
                        baseBodySpan = bodySpan,
                        tokenSpan = tokenSpan,
                        boldColor = markdownBoldColor,
                    )
                }

                // ✅ FRIDGE ADVISE
                fridgeAdvise?.let {
                    item {
                        DynamicSectionCard(
                            title = "Stockage",
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_nav_fridge_icon_thicc),
                                    contentDescription = "Frigo",
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            content = it,
                            insert = insert,
                            baseSpan = bodySpan,
                            tokenSpan = tokenSpan,
                            boldColor = markdownBoldColor,
                        )
                    }
                }

                // ✅ HEALTH GOOD
                healthGood?.let {
                    item {
                        DynamicSectionCard(
                            title = "Bienfaits",
                            icon = { Icon(Icons.Outlined.HealthAndSafety, null) },
                            content = it,
                            insert = insert,
                            baseSpan = bodySpan,
                            tokenSpan = tokenSpan,
                            boldColor = markdownBoldColor,
                        )
                    }
                }

                // ✅ HEALTH WARNING
                healthWarning?.let {
                    item {
                        DynamicSectionCard(
                            title = "À surveiller",
                            icon = { Icon(Icons.Outlined.WarningAmber, null) },
                            content = it,
                            insert = insert,
                            baseSpan = bodySpan,
                            tokenSpan = tokenSpan,
                            boldColor = markdownBoldColor,
                        )
                    }
                }

                // ✅ GOOD TO KNOW
                goodToKnow?.let {
                    item {
                        DynamicSectionCard(
                            title = "Bon à savoir",
                            icon = { Icon(Icons.Outlined.Info, null) },
                            content = it,
                            insert = insert,
                            baseSpan = bodySpan,
                            tokenSpan = tokenSpan,
                            boldColor = markdownBoldColor,
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun GoodToKnowHeader(
    imageResId: Int?,
    gradientColors: List<Color>,
    insert: String,
    baseTitleSpan: SpanStyle,
    baseBodySpan: SpanStyle,
    tokenSpan: SpanStyle,
    boldColor: Color,
) {
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(22.dp)

    val bgBrush = remember(gradientColors) {
        Brush.linearGradient(gradientColors.map { it.copy(alpha = 0.18f) })
    }

    ElevatedCard(shape = shape) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(bgBrush)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (imageResId != null) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = cs.surface.copy(alpha = 0.70f),
                        tonalElevation = 2.dp
                    ) {
                        Image(
                            painter = painterResource(imageResId),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .size(76.dp)
                                .padding(10.dp)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                }

                val insertCap = remember(insert) {
                    insert.replaceFirstChar { ch ->
                        if (ch.isLowerCase()) ch.titlecase() else ch.toString()
                    }
                }

                Column(Modifier.weight(1f)) {
                    MarkdownInlineText(
                        template = "Conseils et infos\n$ITEM_TOKEN",
                        insert = insertCap,
                        baseSpan = baseTitleSpan,
                        tokenSpan = tokenSpan,
                        style = MaterialTheme.typography.titleLarge,
                        boldColor = boldColor,
                    )
                }
            }
        }
    }
}

@Composable
private fun DynamicSectionCard(
    title: String,
    icon: (@Composable () -> Unit)?,
    content: ManualContent,
    insert: String,
    baseSpan: SpanStyle,
    tokenSpan: SpanStyle,
    boldColor: Color? = null,
    defaultExpanded: Boolean = false,
) {
    var expanded by rememberSaveable(title) { mutableStateOf(defaultExpanded) }
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "sectionArrowRotation"
    )

    ElevatedCard(shape = MaterialTheme.shapes.large) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.primary) {
                        icon()
                    }
                    Spacer(Modifier.width(10.dp))
                }

                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    imageVector = Icons.Outlined.ExpandMore,
                    contentDescription = if (expanded) "Réduire" else "Développer",
                    modifier = Modifier.rotate(rotation),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                                MarkdownInlineText(
                                    template = p,
                                    insert = insert,
                                    baseSpan = baseSpan,
                                    tokenSpan = tokenSpan,
                                    style = MaterialTheme.typography.bodyMedium,
                                    boldColor = boldColor,
                                )
                                if (idx != paragraphs.lastIndex) Spacer(Modifier.height(10.dp))
                            }
                        }

                        is ManualContent.Bullets -> {
                            content.items.forEach { b ->
                                Row(Modifier.fillMaxWidth()) {
                                    Text(
                                        "•  ",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    MarkdownInlineText(
                                        template = b,
                                        insert = insert,
                                        baseSpan = baseSpan,
                                        tokenSpan = tokenSpan,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f),
                                        boldColor = boldColor,
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
private fun MarkdownInlineText(
    template: String,
    insert: String,
    baseSpan: SpanStyle,
    tokenSpan: SpanStyle,
    style: androidx.compose.ui.text.TextStyle,
    modifier: Modifier = Modifier,
    boldColor: Color? = null, // ✅
) {
    val effectiveBoldColor = boldColor ?: MaterialTheme.colorScheme.primary

    val boldSpan = remember(baseSpan, effectiveBoldColor) {
        baseSpan.copy(color = effectiveBoldColor)
    }

    val annotated = remember(template, insert, baseSpan, tokenSpan, effectiveBoldColor) {
        buildInlineMarkdownAnnotatedString(
            template = template,
            insert = insert,
            baseSpan = baseSpan,
            tokenSpan = tokenSpan,
            boldSpan = boldSpan // ✅
        )
    }

    Text(text = annotated, style = style, modifier = modifier)
}

/**
 * Markdown inline (subset) :
 * - **gras**
 * - *italique*
 * + support du placeholder {ITEM} injecté en gradient (et combinable avec gras/italique)
 */
private fun buildInlineMarkdownAnnotatedString(
    template: String,
    insert: String,
    baseSpan: SpanStyle,
    tokenSpan: SpanStyle,
    boldSpan: SpanStyle? = null, // ✅
): AnnotatedString {
    var i = 0
    var bold = false
    var italic = false

    val out = AnnotatedString.Builder()
    val buf = StringBuilder()

    fun currentSpan(isToken: Boolean): SpanStyle {
        val base = when {
            isToken -> tokenSpan
            bold && boldSpan != null -> boldSpan      // ✅ gras coloré
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
            flush(isToken = true) // ✅ garde le gradient sur {ITEM}
            i += ITEM_TOKEN.length
            continue
        }

        buf.append(template[i])
        i += 1
    }

    flush()
    return out.toAnnotatedString()
}


/** Fallback si jamais le code n'existe pas dans la taxonomie */
private fun prettifyTaxonomyCodeForUi(raw: String): String {
    val s = raw.trim()
    if (s.isBlank()) return "Cet aliment"
    val normalized = s.replace('-', '_').replace(Regex("_+"), "_").trim('_')
    return normalized.split('_')
        .filter { it.isNotBlank() }
        .joinToString(" ") { part ->
            part.lowercase().replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase() else c.toString() }
        }
}

private fun String.lowercaseFirstEachLine(): String =
    split('\n').joinToString("\n") { line ->
        val trimmed = line.trim()
        if (trimmed.isBlank()) line
        else trimmed.replaceFirstChar { ch -> ch.lowercase() }
    }
