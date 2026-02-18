package com.example.barcode.features.fridge.components.bottomSheetDetails

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.barcode.R
import com.example.barcode.features.addItems.manual.ManualContent
import com.example.barcode.features.addItems.manual.ManualTaxonomyImageResolver
import com.example.barcode.features.addItems.manual.rememberManualTaxonomy
import java.util.Calendar
import kotlin.math.abs

private const val ITEM_TOKEN = "{ITEM}"

private val MONTH_LABELS_FR = listOf(
    "J", "F", "M", "A", "M", "J",
    "J", "A", "S", "O", "N", "D"
)

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoodToKnowScreen(
    itemName: String, // code subtype/type (ex: "VEG_CARROT")
    onClose: () -> Unit
) {
    val context = LocalContext.current
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

    val cs = MaterialTheme.colorScheme

    // tri-color du JSON (fallback si absent)
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

    // ✅ Accent discret basé sur la couleur item (pour halo / chevron / puces)
    val accentColor = remember(gradientColors, markdownBoldColor) {
        gradientColors.getOrNull(1) ?: markdownBoldColor
    }

    val tokenSpan = remember(gradientColors) {
        SpanStyle(
            brush = Brush.linearGradient(gradientColors),
            fontWeight = FontWeight.SemiBold
        )
    }

    val bodySpan = remember(cs) { SpanStyle(color = cs.onSurfaceVariant) }
    val textSpan = remember(cs) { SpanStyle(color = cs.onSurface) }

    // image header : recalcul quand taxonomy arrive
    val headerImageResId = remember(context, code, taxonomy) {
        if (taxonomy == null || code.isBlank()) return@remember 0

        val subRes = ManualTaxonomyImageResolver.resolveSubtypeDrawableResId(context, code)
        if (subRes != 0) subRes
        else ManualTaxonomyImageResolver.resolveTypeDrawableResId(context, code)
    }.takeIf { it != 0 }

    val fridgeAdvise = subtype?.fridgeAdvise
    val healthGood = subtype?.healthGood
    val healthWarning = subtype?.healthWarning
    val goodToKnow = subtype?.goodToKnow

    // ✅ Saison EU_TEMPERATE (mois 1..12)
    val temperateMonths = remember(subtype) {
        subtype?.seasons
            ?.get("EU_TEMPERATE")
            .orEmpty()
            .filter { it in 1..12 }
            .distinct()
            .sorted()
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        contentWindowInsets = WindowInsets(0)
    ) {
        Column(Modifier.fillMaxSize()) {

            GoodToKnowHeroHeader(
                imageResId = headerImageResId,
                gradientColors = gradientColors,
                insert = insert,
                baseTitleSpan = textSpan,
                tokenSpan = tokenSpan,
                boldColor = markdownBoldColor,
                onClose = onClose,
            )

            val navBottom = WindowInsets.navigationBars
                .asPaddingValues()
                .calculateBottomPadding()

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .offset(y = (-18).dp),
                contentPadding = PaddingValues(
                    top = 6.dp,
                    bottom = 24.dp + navBottom
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (taxonomy == null) {
                    item {
                        Spacer(Modifier.height(18.dp))
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                } else {

                    // ✅ Nouveau composant Saison (sous header, au-dessus des volets)
                    if (temperateMonths.isNotEmpty()) {
                        item {
                            SeasonalityGaugeCard(
                                months = temperateMonths,
                                accentColor = accentColor,
                            )
                            Spacer(Modifier.height(15.dp))
                        }
                    }

                    fridgeAdvise?.let {
                        item {
                            DynamicSectionCard(
                                title = "Conseils frigo",
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
                                accentColor = accentColor,
                            )
                        }
                    }

                    healthGood?.let {
                        item {
                            DynamicSectionCard(
                                title = "Bon pour la santé",
                                icon = { Icon(Icons.Outlined.HealthAndSafety, null) },
                                content = it,
                                insert = insert,
                                baseSpan = bodySpan,
                                tokenSpan = tokenSpan,
                                boldColor = markdownBoldColor,
                                accentColor = accentColor,
                            )
                        }
                    }

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
                                accentColor = accentColor,
                            )
                        }
                    }

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
                                accentColor = accentColor,
                            )
                        }
                    }
                }
            }
        }
    }
}




@Composable
private fun GoodToKnowHeroHeader(
    imageResId: Int?,
    gradientColors: List<Color>,
    insert: String,
    baseTitleSpan: SpanStyle,
    tokenSpan: SpanStyle,
    boldColor: Color,
    onClose: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme

    val insertCap = remember(insert) {
        insert.replaceFirstChar { ch ->
            if (ch.isLowerCase()) ch.titlecase() else ch.toString()
        }
    }

    val heroBrush = remember(gradientColors) {
        Brush.verticalGradient(
            colors = listOf(
                gradientColors[0].copy(alpha = 0.55f),
                gradientColors.getOrNull(1)?.copy(alpha = 0.30f) ?: gradientColors[0].copy(alpha = 0.30f),
                gradientColors.getOrNull(2)?.copy(alpha = 0.14f) ?: gradientColors[0].copy(alpha = 0.14f),
                Color.Transparent
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .background(heroBrush)
            .statusBarsPadding()
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp),
            shape = CircleShape,
            color = cs.surface.copy(alpha = 0.55f),
            tonalElevation = 0.dp
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Outlined.Close, contentDescription = "Fermer")
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp)
                .offset(y = (-8).dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (imageResId != null) {
                Image(
                    painter = painterResource(imageResId),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(92.dp)
                )

                Spacer(Modifier.width(14.dp))
            }

            Column(Modifier.weight(1f)) {
                MarkdownInlineText(
                    template = "**$ITEM_TOKEN**",
                    insert = insertCap,
                    baseSpan = baseTitleSpan,
                    tokenSpan = tokenSpan.copy(
                        fontSize = MaterialTheme.typography.headlineMedium.fontSize // ou headlineMedium
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    boldColor = boldColor,
                )
            }
        }
    }
}


@Composable
private fun SeasonalityGaugeCard(
    months: List<Int>,
    accentColor: Color,
) {
    val cs = MaterialTheme.colorScheme

    val currentMonth = remember {
        Calendar.getInstance().get(Calendar.MONTH) + 1 // 1..12
    }

    val monthsSet = remember(months) { months.filter { it in 1..12 }.toSet() }
    val ranges = remember(monthsSet) { buildMonthRanges(monthsSet) }

    val isInSeasonNow = remember(monthsSet, currentMonth) { currentMonth in monthsSet }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 0.dp)
    ) {
        Text(
            text = if (isInSeasonNow) "C'est la saison !" else "Hors saison",
            color = if (isInSeasonNow) cs.primary else cs.onSurfaceVariant.copy(alpha = 0.85f),
            style = if (isInSeasonNow) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
            fontWeight = if (isInSeasonNow) FontWeight.ExtraBold else FontWeight.Medium,
        )

        Spacer(Modifier.height(10.dp))

        val gaugeShape = RoundedCornerShape(999.dp)
        val innerPad = 8.dp
        val highlightVPad = 6.dp

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp),
            shape = gaugeShape,
            color = cs.surface.copy(alpha = 0.65f),
            border = BorderStroke(1.dp, cs.outlineVariant.copy(alpha = 0.55f)),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = innerPad)
            ) {
                val cellW = maxWidth / 12f

                // Fond de la surbrillance = teinte de l'item (mélangée à la surface pour rester douce)
                val highlightBg = lerp(cs.surface, accentColor, 0.32f)

                // Texte sur la surbrillance = noir/blanc auto selon luminance
                val onHighlight = if (highlightBg.luminance() < 0.45f) Color.White else Color.Black

                // ✅ Surbrillance en blocs (ranges contigus)
                ranges.forEach { r ->
                    val startIndex = (r.first - 1).coerceIn(0, 11)
                    val endIndex = (r.last - 1).coerceIn(0, 11)
                    val x = cellW * startIndex
                    val w = cellW * (endIndex - startIndex + 1)

                    Box(
                        modifier = Modifier
                            .offset(x = x)
                            .width(w)
                            .fillMaxHeight()
                            .padding(vertical = highlightVPad)
                            .background(highlightBg, RoundedCornerShape(999.dp))
                    )
                }

                // ✅ Labels au-dessus
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MONTH_LABELS_FR.forEachIndexed { index, label ->
                        val month = index + 1

                        val inSeasonLabelColor = onHighlight.copy(alpha = 0.92f)

                        val isInSeason = month in monthsSet
                        val isCurrent = month == currentMonth

                        // Pastille mois courant
                        val currentPillBg = when {
                            isCurrent && isInSeason -> cs.primary
                            isCurrent -> cs.surfaceVariant.copy(alpha = 0.95f)
                            else -> Color.Transparent
                        }

                        val currentPillBorder = when {
                            isCurrent && isInSeason -> cs.onPrimary.copy(alpha = 0.18f)
                            isCurrent -> cs.outlineVariant.copy(alpha = 0.60f)
                            else -> Color.Transparent
                        }

                        val labelColor = when {
                            isCurrent && isInSeason -> cs.onPrimary
                            isCurrent -> cs.onSurface
                            isInSeason -> inSeasonLabelColor // (déjà calculée avec onHighlight)
                            else -> cs.onSurfaceVariant
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCurrent) {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .padding(vertical = highlightVPad) // ✅ même hauteur que l'inner-gauge
                                        .background(currentPillBg, RoundedCornerShape(999.dp))
                                        .border(1.dp, currentPillBorder, RoundedCornerShape(999.dp))
                                )
                            }

                            Text(
                                text = label,
                                color = labelColor,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = when {
                                    isCurrent -> FontWeight.ExtraBold
                                    else -> FontWeight.Medium
                                },
                                maxLines = 1
                            )
                        }

                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = "Europe tempérée",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.End,
            color = cs.onSurfaceVariant.copy(alpha = 0.65f),
            style = MaterialTheme.typography.labelSmall
        )
    }
}

private fun buildMonthRanges(months: Set<Int>): List<IntRange> {
    if (months.isEmpty()) return emptyList()

    val ranges = mutableListOf<IntRange>()
    var start: Int? = null

    for (m in 1..12) {
        if (m in months) {
            if (start == null) start = m
        } else if (start != null) {
            ranges += start!!..(m - 1)
            start = null
        }
    }
    if (start != null) ranges += start!!..12

    return ranges
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
    accentColor: Color,
) {
    var expanded by rememberSaveable(title) { mutableStateOf(defaultExpanded) }
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "sectionArrowRotation"
    )

    val cs = MaterialTheme.colorScheme

    // ✅ Fond très léger pour détacher du background (un poil plus présent quand ouvert)
    val sectionBgAlpha = if (expanded) 0.24f else 0.18f
    val sectionBg = cs.surfaceVariant.copy(alpha = sectionBgAlpha)

    // ✅ Border fine + discrète (teintée item)
    val borderColor = remember(accentColor) { accentColor.copy(alpha = 0.28f) }

    // ✅ Chevron teinté, reste neutre
    val chevronTint = remember(accentColor) { accentColor.copy(alpha = 0.60f) }

    // ✅ Puces teintées
    val bulletTint = remember(accentColor) { accentColor.copy(alpha = 0.75f) }

    // ✅ Halo smooth (radial) derrière l’icône
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
        colors = CardDefaults.outlinedCardColors(
            containerColor = sectionBg
        ),
        elevation = CardDefaults.outlinedCardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(haloBrush, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        CompositionLocalProvider(LocalContentColor provides cs.onSurface) {
                            icon()
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                }

                Text(
                    title,
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
                                        color = bulletTint
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
    boldColor: Color? = null,
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
            boldSpan = boldSpan
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
