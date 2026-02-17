package com.example.barcode.features.fridge.components.bottomSheetDetails

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.barcode.features.addItems.manual.ManualTaxonomyImageResolver
import com.example.barcode.features.addItems.manual.ManualTaxonomyRepository

private const val ITEM_TOKEN = "{ITEM}"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoodToKnowScreen(
    itemName: String, // = code subtype/type (ex: "VEG_CARROT")
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

    val headerImageResId = remember(context, code) {
        // 1) image subtype (ex: VEG_CARROT)
        val subRes = if (code.isNotBlank())
            ManualTaxonomyImageResolver.resolveSubtypeDrawableResId(context, code)
        else 0

        if (subRes != 0) subRes
        else {
            // 2) fallback type (ex: VEGETABLES) si jamais tu ouvres la page sur un type
            ManualTaxonomyImageResolver.resolveTypeDrawableResId(context, code)
        }
    }.takeIf { it != 0 }

    // "Carottes" -> "carottes"
    val insert = remember(resolvedTitle) { resolvedTitle.lowercaseFirstEachLine() }

    val cs = MaterialTheme.colorScheme

    // ✅ 3 couleurs depuis subtypes.gradient.colors[] (sinon fallback thème)
    val gradientColors: List<Color> = remember(subtype, cs) {
        val hexes = subtype?.gradient?.colors?.take(3).orEmpty()
        val parsed = hexes.mapNotNull { hex ->
            runCatching { Color(AndroidColor.parseColor(hex)) }.getOrNull()
        }
        if (parsed.size >= 3) parsed
        else listOf(cs.primary, cs.tertiary, cs.secondary)
    }

    val gradientSpan = remember(gradientColors) {
        SpanStyle(
            brush = Brush.linearGradient(gradientColors),
            fontWeight = FontWeight.SemiBold
        )
    }

    val baseBodySpan = remember(cs) { SpanStyle(color = cs.onSurfaceVariant) }
    val baseTextSpan = remember(cs) { SpanStyle(color = cs.onSurface) }

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
                Spacer(Modifier.height(6.dp))

                GoodToKnowHeader(
                    imageResId = headerImageResId,
                    gradientColors = gradientColors,
                    insert = insert,
                    baseTitleSpan = baseTextSpan,
                    baseBodySpan = baseBodySpan,
                    tokenSpan = gradientSpan
                )
            }

            item {
                SectionCard(
                    icon = { Icon(Icons.Outlined.Lightbulb, null) },
                    title = "Conservation",
                    tokenText = insert,
                    baseSpan = baseBodySpan,
                    tokenSpan = gradientSpan,
                    paragraphs = listOf(
                        "Faux paragraphe : comment conserver $ITEM_TOKEN (température, emballage, durée…).",
                        "Deuxième faux paragraphe : astuces pour éviter le dessèchement / l’humidité / l’oxydation de $ITEM_TOKEN."
                    ),
                    bullets = listOf(
                        "Mettre $ITEM_TOKEN dans un contenant hermétique.",
                        "Éviter le contact direct avec l’air (faux conseil).",
                        "Étiqueter la date d’ouverture / cuisson."
                    )
                )
            }

            item {
                SectionCard(
                    icon = { Icon(Icons.Outlined.WarningAmber, null) },
                    title = "Signes d’alerte",
                    tokenText = insert,
                    baseSpan = baseBodySpan,
                    tokenSpan = gradientSpan,
                    paragraphs = listOf(
                        "Faux paragraphe : quels signes indiquent que $ITEM_TOKEN n’est plus bon."
                    ),
                    bullets = listOf(
                        "Odeur anormale (faux exemple).",
                        "Texture visqueuse / changement de couleur.",
                        "Moisissure visible (à jeter)."
                    )
                )
            }

            item {
                SectionCard(
                    title = "Check-list rapide",
                    tokenText = insert,
                    baseSpan = baseBodySpan,
                    tokenSpan = gradientSpan,
                    paragraphs = listOf(
                        "Mini check-list pour $ITEM_TOKEN avant consommation."
                    ),
                    bullets = listOf(
                        "Aspect OK",
                        "Odeur OK",
                        "Date OK",
                        "Stockage OK"
                    )
                )
            }

            item {
                RecipesCard(
                    tokenText = insert,
                    baseSpan = baseBodySpan,
                    tokenSpan = gradientSpan
                )
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    tokenText: String,
    baseSpan: SpanStyle,
    tokenSpan: SpanStyle,
    paragraphs: List<String>,
    bullets: List<String>,
    icon: (@Composable () -> Unit)? = null
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

            paragraphs.forEach { p ->
                GradientInjectedText(
                    template = p,
                    tokenText = tokenText,
                    baseSpan = baseSpan,
                    tokenSpan = tokenSpan,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
            }

            bullets.forEach { b ->
                Row(Modifier.fillMaxWidth()) {
                    Text("•  ", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    GradientInjectedText(
                        template = b,
                        tokenText = tokenText,
                        baseSpan = baseSpan,
                        tokenSpan = tokenSpan,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun RecipesCard(
    tokenText: String,
    baseSpan: SpanStyle,
    tokenSpan: SpanStyle
) {
    val ideas = listOf(
        "Idée 1 (fake) : poêlée rapide avec $ITEM_TOKEN",
        "Idée 2 (fake) : salade / bol froid avec $ITEM_TOKEN",
        "Idée 3 (fake) : version gratin / four avec $ITEM_TOKEN"
    )

    ElevatedCard(shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(16.dp)) {
            Text("Idées express", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))

            ideas.forEach { idea ->
                Row(Modifier.fillMaxWidth()) {
                    Text("•  ", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    GradientInjectedText(
                        template = idea,
                        tokenText = tokenText,
                        baseSpan = baseSpan,
                        tokenSpan = tokenSpan,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(4.dp))
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

    // Fond légèrement teinté par les 3 couleurs du subtype (smooth et discret)
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
                        color = cs.surface.copy(alpha = 0.65f),
                        tonalElevation = 2.dp
                    ) {
                        Image(
                            painter = painterResource(imageResId),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .size(74.dp)
                                .padding(10.dp)
                        )
                    }
                }

                if (imageResId != null) Spacer(Modifier.width(14.dp))

                Column(Modifier.weight(1f)) {
                    GradientInjectedText(
                        template = "Conseils et infos sur les $ITEM_TOKEN",
                        tokenText = insert,
                        baseSpan = baseTitleSpan,
                        tokenSpan = tokenSpan,
                        style = MaterialTheme.typography.titleLarge
                    )

                    Spacer(Modifier.height(6.dp))

/*                    GradientInjectedText(
                        template = "Conservation, signaux d’alerte, check-list… (contenu temporaire) pour “$ITEM_TOKEN”.",
                        tokenText = insert,
                        baseSpan = baseBodySpan,
                        tokenSpan = tokenSpan,
                        style = MaterialTheme.typography.bodyMedium
                    )*/
                }
            }
        }
    }
}




@Composable
private fun GradientInjectedText(
    template: String,
    tokenText: String,
    baseSpan: SpanStyle,
    tokenSpan: SpanStyle,
    style: androidx.compose.ui.text.TextStyle,
    modifier: Modifier = Modifier
) {
    val annotated = remember(template, tokenText, baseSpan, tokenSpan) {
        buildAnnotatedString {
            val parts = template.split(ITEM_TOKEN)
            parts.forEachIndexed { idx, part ->
                if (part.isNotEmpty()) {
                    withStyle(baseSpan) { append(part) }
                }
                if (idx != parts.lastIndex) {
                    withStyle(tokenSpan) { append(tokenText) }
                }
            }
        }
    }

    Text(
        text = annotated,
        style = style,
        modifier = modifier
    )
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
    this.split('\n').joinToString("\n") { line ->
        val trimmed = line.trim()
        if (trimmed.isBlank()) line
        else trimmed.replaceFirstChar { ch -> ch.lowercase() }
    }
