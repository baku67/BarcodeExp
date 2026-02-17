package com.example.barcode.features.fridge.components.bottomSheetDetails

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.example.barcode.features.addItems.manual.ManualContent
import com.example.barcode.features.addItems.manual.ManualTaxonomyImageResolver
import com.example.barcode.features.addItems.manual.ManualTaxonomyRepository

private const val ITEM_TOKEN = "{ITEM}"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoodToKnowScreen(
    itemName: String, // code subtype/type (ex: "VEG_CARROT")
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val taxonomy = remember(context) { ManualTaxonomyRepository.get(context) }

    val code = remember(itemName) { itemName.trim() }
    val subtype = remember(code, taxonomy) { taxonomy.subtypeMeta(code) }

    val resolvedTitle = remember(code, subtype, taxonomy) {
        when {
            code.isBlank() -> "Cet aliment"
            else -> subtype?.title
                ?: taxonomy.typeMeta(code)?.title
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

    val tokenSpan = remember(gradientColors) {
        SpanStyle(
            brush = Brush.linearGradient(gradientColors),
            fontWeight = FontWeight.SemiBold
        )
    }

    val bodySpan = remember(cs) { SpanStyle(color = cs.onSurfaceVariant) }
    val textSpan = remember(cs) { SpanStyle(color = cs.onSurface) }

    // ✅ image header du subtype (fallback type si besoin)
    val headerImageResId = remember(context, code) {
        val subRes = if (code.isNotBlank())
            ManualTaxonomyImageResolver.resolveSubtypeDrawableResId(context, code)
        else 0

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
            item {
                GoodToKnowHeader(
                    imageResId = headerImageResId,
                    gradientColors = gradientColors,
                    insert = insert,
                    baseTitleSpan = textSpan,
                    baseBodySpan = bodySpan,
                    tokenSpan = tokenSpan
                )
            }

            // ✅ FRIDGE ADVISE
            fridgeAdvise?.let {
                item {
                    DynamicSectionCard(
                        title = "Conseils frigo",
                        icon = { Icon(Icons.Outlined.Lightbulb, null) },
                        content = it,
                        insert = insert,
                        baseSpan = bodySpan,
                        tokenSpan = tokenSpan
                    )
                }
            }

            // ✅ HEALTH GOOD
            healthGood?.let {
                item {
                    DynamicSectionCard(
                        title = "Bon pour la santé",
                        icon = { Icon(Icons.Outlined.Info, null) },
                        content = it,
                        insert = insert,
                        baseSpan = bodySpan,
                        tokenSpan = tokenSpan
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
                        tokenSpan = tokenSpan
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
                        tokenSpan = tokenSpan
                    )
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

                Column(Modifier.weight(1f)) {
                    MarkdownInlineText(
                        template = "Conseils et repères pour “$ITEM_TOKEN”",
                        insert = insert,
                        baseSpan = baseTitleSpan,
                        tokenSpan = tokenSpan,
                        style = MaterialTheme.typography.titleLarge
                    )

                    Spacer(Modifier.height(6.dp))

                    MarkdownInlineText(
                        template = "Infos, listes et conseils pratiques pour “$ITEM_TOKEN”.",
                        insert = insert,
                        baseSpan = baseBodySpan,
                        tokenSpan = tokenSpan,
                        style = MaterialTheme.typography.bodyMedium
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
) {
    ElevatedCard(shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth()) {
                if (icon != null) {
                    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.primary) {
                        icon()
                    }
                    Spacer(Modifier.width(10.dp))
                }
                Text(title, style = MaterialTheme.typography.titleMedium)
            }

            Spacer(Modifier.height(10.dp))

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
                            style = MaterialTheme.typography.bodyMedium
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
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(Modifier.height(6.dp))
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
    modifier: Modifier = Modifier
) {
    val annotated = remember(template, insert, baseSpan, tokenSpan) {
        buildInlineMarkdownAnnotatedString(
            template = template,
            insert = insert,
            baseSpan = baseSpan,
            tokenSpan = tokenSpan
        )
    }

    Text(
        text = annotated,
        style = style,
        modifier = modifier
    )
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
    tokenSpan: SpanStyle
): AnnotatedString {
    var i = 0
    var bold = false
    var italic = false

    val out = AnnotatedString.Builder()
    val buf = StringBuilder()

    fun currentSpan(isToken: Boolean): SpanStyle {
        val base = if (isToken) tokenSpan else baseSpan

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
        // ** bold toggle
        if (template.startsWith("**", i)) {
            flush()
            bold = !bold
            i += 2
            continue
        }

        // * italic toggle
        if (template[i] == '*') {
            flush()
            italic = !italic
            i += 1
            continue
        }

        // {ITEM} injection
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
