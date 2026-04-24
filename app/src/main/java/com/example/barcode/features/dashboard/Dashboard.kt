package com.example.barcode.features.dashboard

import android.annotation.SuppressLint
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.barcode.R
import kotlinx.coroutines.delay
import kotlin.math.min
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import com.example.barcode.common.utils.EuropeSeason
import com.example.barcode.common.utils.SeasonRegion
import com.example.barcode.common.utils.SeasonalityResolver
import com.example.barcode.core.SessionManager
import com.example.barcode.features.addItems.manual.rememberManualTaxonomy
import kotlinx.coroutines.launch
import com.example.barcode.data.local.entities.ItemEntity
import com.example.barcode.features.listeCourse.ShoppingListItemUi
import java.util.Calendar
import androidx.compose.material.icons.outlined.Widgets
import com.example.barcode.widgets.WidgetPinning


// ---- Produits (données réelles depuis Items) ----

private const val SOON_THRESHOLD_DAYS = 2

private enum class ExpiryKind { EXPIRED, SOON, FRESH }

private data class ExpiringLine(
    val name: String,
    val note: String,   // ex: "hier", "aujourd'hui", "1j", "6j"...
    val kind: ExpiryKind
)

private data class DashboardProductsUi(
    val total: Int,
    val fresh: Int,
    val soon: Int,
    val expired: Int,
    val nextExpiring: List<ExpiringLine>
)

private data class DashboardShoppingUi(
    val totalToBuy: Int,
    val preview: List<String>
)

private data class SeasonalItemUi(
    val title: String,
    val imageName: String
)

private const val DAY_MS: Long = 86_400_000L

private fun startOfDay(ms: Long): Long {
    val cal = Calendar.getInstance()
    cal.timeInMillis = ms
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

private fun diffDays(expiryMs: Long, nowMs: Long): Int {
    val e = startOfDay(expiryMs)
    val n = startOfDay(nowMs)
    return ((e - n) / DAY_MS).toInt()
}

private fun expiryNote(diffDays: Int): String = when {
    diffDays <= -2 -> "il y a ${-diffDays}j"
    diffDays == -1 -> "hier"
    diffDays == 0 -> "aujourd'hui"
    diffDays == 1 -> "1j"
    else -> "${diffDays}j"
}

private fun expiryKind(diffDays: Int): ExpiryKind = when {
    diffDays < 0 -> ExpiryKind.EXPIRED
    diffDays <= SOON_THRESHOLD_DAYS -> ExpiryKind.SOON
    else -> ExpiryKind.FRESH
}

private fun buildDashboardProductsUi(
    items: List<ItemEntity>,
    nowMs: Long = System.currentTimeMillis(),
): DashboardProductsUi {
    var fresh = 0
    var soon = 0
    var expired = 0

    items.forEach { item ->
        val expiryMs = item.expiryDate?.takeIf { it > 0L }
        if (expiryMs == null) {
            // Pas de date => on le compte dans "frais" pour l'instant (sinon ajoute une 4e catégorie "Sans date")
            fresh++
        } else {
            val d = diffDays(expiryMs, nowMs)
            when (expiryKind(d)) {
                ExpiryKind.EXPIRED -> expired++
                ExpiryKind.SOON -> soon++
                ExpiryKind.FRESH -> fresh++
            }
        }
    }

    val nextExpiring = items
        .asSequence()
        .mapNotNull { item ->
            item.expiryDate
                ?.takeIf { it > 0L }
                ?.let { expiry -> item to expiry }
        }
        .sortedBy { (_, expiry) -> expiry }
        .take(5)
        .map { (item, expiryMs) ->
            val d = diffDays(expiryMs, nowMs)
            ExpiringLine(
                name = item.name?.trim()?.takeIf { it.isNotBlank() } ?: "Produit",
                note = expiryNote(d),
                kind = expiryKind(d)
            )
        }
        .toList()

    return DashboardProductsUi(
        total = items.size,
        fresh = fresh,
        soon = soon,
        expired = expired,
        nextExpiring = nextExpiring
    )
}

/**
 * ✅ Version recommandée : tu passes directement la liste des items.
 * Le comptage + la mini-liste "à consommer" sont calculés ici.
 */
@Composable
fun Dashboard(
    items: List<ItemEntity>,
    shoppingItems: List<ShoppingListItemUi>,
    onNavigateToItems: () -> Unit,
    onNavigateToListeCourses: () -> Unit,
    onNavigateToRecipes: () -> Unit,
) {
    val productsUi = buildDashboardProductsUi(items)
    val shoppingUi = buildDashboardShoppingUi(shoppingItems)

    val context = LocalContext.current
    val sessionManager = remember(context) { SessionManager(context) }
    val coroutineScope = rememberCoroutineScope()
    val taxonomy = rememberManualTaxonomy()

    val seasonRegion by sessionManager.seasonRegion.collectAsState(
        initial = SeasonRegion.EU_TEMPERATE
    )

    val seasonalCardExpanded by sessionManager.dashboardSeasonalExpanded.collectAsState(
        initial = false
    )

    val seasonContext = remember(seasonRegion) {
        SeasonalityResolver.currentContext(seasonRegion)
    }

    val seasonalFruits = remember(taxonomy, seasonContext) {
        taxonomy
            ?.subtypesOf("FRUITS")
            .orEmpty()
            .filter {
                SeasonalityResolver.isInSeason(
                    subtype = it,
                    region = seasonContext.region,
                    month = seasonContext.currentMonth
                )
            }
            .sortedBy { it.title }
            .take(5)
            .map {
                SeasonalItemUi(
                    title = it.title,
                    imageName = it.image.orEmpty()
                )
            }
    }

    val seasonalVegetables = remember(taxonomy, seasonContext) {
        taxonomy
            ?.subtypesOf("VEGETABLES")
            .orEmpty()
            .filter {
                SeasonalityResolver.isInSeason(
                    subtype = it,
                    region = seasonContext.region,
                    month = seasonContext.currentMonth
                )
            }
            .sortedBy { it.title }
            .take(5)
            .map {
                SeasonalItemUi(
                    title = it.title,
                    imageName = it.image.orEmpty()
                )
            }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DashboardCardProductsWide(
            total = productsUi.total,
            fresh = productsUi.fresh,
            soon = productsUi.soon,
            expired = productsUi.expired,
            nextExpiring = productsUi.nextExpiring,
            onClick = onNavigateToItems
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DashboardCardShoppingList(
                total = shoppingUi.totalToBuy,
                preview = shoppingUi.preview,
                modifier = Modifier
                    .weight(1f)
                    .height(180.dp),
                onClick = onNavigateToListeCourses
            )

            DashboardCardRecipesFake(
                modifier = Modifier
                    .weight(1f)
                    .height(180.dp),
                onClick = onNavigateToRecipes
            )
        }

        DashboardCardSeasonal(
            modifier = Modifier.fillMaxWidth(),
            region = seasonContext.region,
            season = seasonContext.europeSeason,
            fruits = seasonalFruits,
            vegetables = seasonalVegetables,
            expanded = seasonalCardExpanded,
            onExpandedChange = { expanded ->
                coroutineScope.launch {
                    sessionManager.setDashboardSeasonalExpanded(expanded)
                }
            }
        )

        DashboardInstallWidgetCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                WidgetPinning.requestPinFridgeWidget(context)
            }
        )
    }
}



private data class SeasonalCardVisual(
    val illustrationRes: Int,
    val accent: Color
)

private fun seasonalCardVisualFor(season: EuropeSeason): SeasonalCardVisual {
    return when (season) {
        EuropeSeason.SPRING -> SeasonalCardVisual(
            illustrationRes = R.drawable.dashboard_season_spring,
            accent = Color(0xFF4CAF50)
        )

        EuropeSeason.SUMMER -> SeasonalCardVisual(
            illustrationRes = R.drawable.dashboard_season_summer,
            accent = Color(0xFFFF9800)
        )

        EuropeSeason.AUTUMN -> SeasonalCardVisual(
            illustrationRes = R.drawable.dashboard_season_automn,
            accent = Color(0xFFE67E22)
        )

        EuropeSeason.WINTER -> SeasonalCardVisual(
            illustrationRes = R.drawable.dashboard_season_winter,
            accent = Color(0xFF64B5F6)
        )
    }
}


@SuppressLint("RememberReturnType")
@Composable
private fun DashboardCardSeasonal(
    modifier: Modifier = Modifier,
    region: SeasonRegion,
    season: EuropeSeason,
    fruits: List<SeasonalItemUi>,
    vegetables: List<SeasonalItemUi>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit
) {
    val seasonVisual = remember(season) {
        seasonalCardVisualFor(season)
    }

    val subtitle = remember(region, season) {
        "${SeasonalityResolver.seasonLabel(season)} • ${SeasonalityResolver.regionLabel(region)}"
    }

    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(
            durationMillis = 220,
            easing = FastOutSlowInEasing
        ),
        label = "seasonCardArrowRotation"
    )

    val headerTitleColor = Color(0xFF111111)
    val headerSubtitleColor = Color(0xFF151515)

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        onClick = { onExpandedChange(!expanded) },
        interactionSource = remember { MutableInteractionSource() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f),
                    shape = RoundedCornerShape(18.dp)
                )
        ) {
            SeasonalIllustrationBackground(
                illustrationRes = seasonVisual.illustrationRes,
                accent = seasonVisual.accent,
                expanded = expanded,
                modifier = Modifier.matchParentSize()
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(
                        animationSpec = tween(
                            durationMillis = 260,
                            easing = FastOutSlowInEasing
                        )
                    )
                    .padding(
                        horizontal = 14.dp,
                        vertical = if (expanded) 14.dp else 18.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = if (expanded) 40.dp else 56.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "De saison",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = headerTitleColor
                    )

                    Box(
                        modifier = Modifier
                            .size(if (expanded) 34.dp else 38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                MaterialTheme.colorScheme.surface.copy(
                                    alpha = if (expanded) 0.46f else 0.36f
                                )
                            )
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ExpandMore,
                            contentDescription = if (expanded) "Réduire" else "Développer",
                            tint = headerTitleColor,
                            modifier = Modifier.graphicsLayer {
                                rotationZ = arrowRotation
                            }
                        )
                    }
                }

                if (expanded) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = headerSubtitleColor
                        )

                        CompactSeasonCategoryRow(
                            title = "Fruits",
                            items = fruits,
                            accent = seasonVisual.accent
                        )

                        CompactSeasonCategoryRow(
                            title = "Légumes",
                            items = vegetables,
                            accent = seasonVisual.accent
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun DashboardInstallWidgetCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        onClick = onClick,
        interactionSource = remember { MutableInteractionSource() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(18.dp)
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Widgets,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Widget d’accueil",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Gardez un œil sur vos produits à consommer bientôt sans ouvrir l’app.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Ajouter le widget →",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CompactSeasonCategoryRow(
    title: String,
    items: List<SeasonalItemUi>,
    accent: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = accent.copy(alpha = 0.96f)
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items.take(5).forEach { item ->
                TaxonomyDrawableThumb(
                    imageName = item.imageName,
                    contentDescription = item.title,
                    modifier = Modifier.size(42.dp)
                )
            }
        }
    }
}

@Composable
private fun SeasonalIllustrationBackground(
    illustrationRes: Int,
    accent: Color,
    expanded: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        if (!expanded) {
            // État collapsed :
            // image visible sur toute la hauteur / largeur, sans fade transparent vers le bas
            Image(
                painter = painterResource(id = illustrationRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )

            // légère teinte flashy globale
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                accent.copy(alpha = 0.22f),
                                accent.copy(alpha = 0.10f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // scrim léger pour garder le texte lisible
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.10f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.18f)
                            )
                        )
                    )
            )
        } else {
            // État expanded :
            // illustration forte en haut, puis fade sous le contenu
            Image(
                painter = painterResource(id = illustrationRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
                    .drawWithCache {
                        val imageMask = Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.00f to Color.White,
                                0.16f to Color.White,
                                0.34f to Color.White.copy(alpha = 0.90f),
                                0.52f to Color.White.copy(alpha = 0.45f),
                                0.70f to Color.White.copy(alpha = 0.14f),
                                0.86f to Color.White.copy(alpha = 0.03f),
                                1.00f to Color.Transparent
                            )
                        )

                        onDrawWithContent {
                            drawContent()
                            drawRect(
                                brush = imageMask,
                                blendMode = BlendMode.DstIn
                            )
                        }
                    }
            )

            // teinte flashy en haut
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.00f to accent.copy(alpha = 0.34f),
                                0.12f to accent.copy(alpha = 0.24f),
                                0.28f to accent.copy(alpha = 0.12f),
                                0.50f to Color.Transparent,
                                1.00f to Color.Transparent
                            )
                        )
                    )
            )

            // glow
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .drawWithCache {
                        val glowCenter = Offset(size.width * 0.82f, size.height * 0.10f)
                        val glowRadius = size.minDimension * 0.34f
                        val glowBrush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.22f),
                                Color.Transparent
                            ),
                            center = glowCenter,
                            radius = glowRadius
                        )

                        onDrawBehind {
                            drawRect(
                                brush = glowBrush,
                                blendMode = BlendMode.Screen
                            )
                        }
                    }
            )

            // scrim global
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.00f to MaterialTheme.colorScheme.surface.copy(alpha = 0.08f),
                                0.22f to MaterialTheme.colorScheme.surface.copy(alpha = 0.10f),
                                0.48f to MaterialTheme.colorScheme.surface.copy(alpha = 0.18f),
                                0.72f to MaterialTheme.colorScheme.surface.copy(alpha = 0.34f),
                                1.00f to MaterialTheme.colorScheme.surface.copy(alpha = 0.52f)
                            )
                        )
                    )
            )
        }
    }
}


@Composable
private fun TaxonomyDrawableThumb(
    imageName: String,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val resId = remember(imageName) {
        context.resources.getIdentifier(imageName, "drawable", context.packageName)
    }

    if (resId != 0) {
        Image(
            painter = painterResource(id = resId),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Fit
        )
    } else {
        Icon(
            imageVector = Icons.Outlined.Eco,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
            modifier = modifier
        )
    }
}


@Composable
private fun DashboardCardProductsWide(
    total: Int,
    fresh: Int,
    soon: Int,
    expired: Int,
    nextExpiring: List<ExpiringLine>,
    onClick: () -> Unit,
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        onClick = onClick,
        interactionSource = remember { MutableInteractionSource() }  // "ripple" anim au click
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 0.dp),
                verticalAlignment = Alignment.CenterVertically,     // ✅ centrage vertical
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {



                // 2) Milieu : header (nombre+label) + mini-sections en ligne
                Column(
                    modifier = Modifier
                        .weight(0.85f) // ✅ un peu plus large qu'avant (tu peux ajuster)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(0.dp)
                    ) {

                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_nav_fridge_icon_thicc),
                                contentDescription = "Produits",
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                modifier = Modifier.size(60.dp)
                            )
                        }


                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.Start
                        ) {
                            AnimatedCountText(
                                target = total,
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                durationMillis = 750
                            )
                            Text(
                                text = "Produits",
                                style = MaterialTheme.typography.bodyLarge, // au lieu de bodyMedium
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.80f)
                            )
                        }

                    }
                }

                // 3) Droite : mini-liste "À consommer"
                Column(
                    modifier = Modifier
                        .weight(0.85f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.18f))
                        .padding(12.dp)
                ) {
                    // Liste (max 5)
                    val lines = nextExpiring.take(5)

                    lines.forEachIndexed { index, item ->
                        val baseColor = when (item.kind) {
                            ExpiryKind.EXPIRED -> MaterialTheme.colorScheme.tertiary
                            ExpiryKind.SOON -> Color(0xFFF9A825)
                            ExpiryKind.FRESH -> MaterialTheme.colorScheme.onSurface
                        }

                        // ✅ Dégradé vertical vers le bas (sur le TEXT uniquement)
                        val alpha = when {
                            lines.size < 2 -> 1f
                            index == lines.lastIndex -> 0.22f        // dernier : très transparent
                            index == lines.lastIndex - 1 -> 0.55f    // avant-dernier : moins transparent
                            else -> 1f
                        }

                        Text(
                            text = "• ${item.name} — ${item.note}",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = baseColor.copy(alpha = alpha)
                        )
                    }


                    Spacer(modifier = Modifier.weight(1f)) // ✅ pousse le bouton en bas

                    Text(
                        text = "Voir tout →",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }


            ExpiryProgressBar(
                total = total,
                fresh = fresh,
                soon = soon,
                expired = expired,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}



@Composable
private fun DashboardCardShoppingList(
    total: Int,
    preview: List<String>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        onClick = onClick,
        interactionSource = remember { MutableInteractionSource() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.End
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ReceiptLong,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        modifier = Modifier.size(60.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    AnimatedCountText(
                        target = total,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        durationMillis = 650
                    )
                    Text(
                        text = "Courses",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.80f)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (preview.isEmpty()) {
                    Text(
                        text = "Rien à acheter",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    preview.forEach { item ->
                        Text(
                            text = "• $item",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "→",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}



@Composable
private fun DashboardCardRecipesFake(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val totalRecipes = 42
    val preview = listOf("Pâtes au thon", "Wrap poulet", "Salade quinoa")

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        onClick = onClick,
        interactionSource = remember { MutableInteractionSource() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.End
                ) {
                    Icon(
                        imageVector = Icons.Outlined.RestaurantMenu,
                        contentDescription = "Recettes",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        modifier = Modifier.size(60.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    AnimatedCountText(
                        target = totalRecipes,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        durationMillis = 650,
                        delayMillis = 60
                    )
                    Text(
                        text = "Recettes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.80f)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                preview.take(3).forEach { r ->
                    Text(
                        text = "• $r",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "→",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}


@Composable
private fun ExpiryProgressBar(
    total: Int,
    fresh: Int,
    soon: Int,
    expired: Int,
    modifier: Modifier = Modifier,
    animate: Boolean = true,
    barHeight: Dp = 3.dp,            // ✅ plus fin (au lieu de 4.dp)
    smoothFraction: Float = 0.03f,   // ✅ largeur de “fondu” autour des frontières (3% de la barre)
) {
    val t = total.coerceAtLeast(1)

    val freshW = (fresh.coerceAtLeast(0) / t.toFloat()).coerceIn(0f, 1f)
    val soonW = (soon.coerceAtLeast(0) / t.toFloat()).coerceIn(0f, 1f)
    val expiredW = (expired.coerceAtLeast(0) / t.toFloat()).coerceIn(0f, 1f)

    // Normalisation (si counts incohérents)
    val sum = (freshW + soonW + expiredW).takeIf { it > 0f } ?: 1f
    val wf = freshW / sum
    val ws = soonW / sum
    val we = expiredW / sum

    var target by remember { mutableStateOf(if (animate) 0f else 1f) }

    LaunchedEffect(total, fresh, soon, expired, animate) {
        if (!animate) { target = 1f; return@LaunchedEffect }
        target = 0f
        delay(80)
        target = 1f
    }

    val reveal by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "expiryBarReveal"
    )

    // ✅ Ordre plus “logique” pour une jauge de risque : EXPIRED -> SOON -> FRESH
    val cExpired = MaterialTheme.colorScheme.tertiary
    val cSoon = Color(0xFFF9A825)
    val cFresh = MaterialTheme.colorScheme.primary

    // Segments non nuls
    val segments = listOf(
        we to cExpired,
        ws to cSoon,
        wf to cFresh
    ).filter { it.first > 0f }

    val brush = when (segments.size) {
        0 -> Brush.horizontalGradient(
            colors = listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surfaceVariant)
        )
        1 -> Brush.horizontalGradient(colors = listOf(segments[0].second, segments[0].second))
        else -> {
            val stops = mutableListOf<Pair<Float, Color>>()

            var cum = 0f
            stops += 0f to segments.first().second

            for (i in 0 until segments.lastIndex) {
                val (wPrev, cPrev) = segments[i]
                val (wNext, cNext) = segments[i + 1]

                cum += wPrev

                // ✅ on réduit le fondu si un segment est minuscule
                val s = min(smoothFraction, min(wPrev, wNext) / 2f)

                val left = (cum - s).coerceIn(0f, 1f)
                val right = (cum + s).coerceIn(0f, 1f)

                stops += left to cPrev
                stops += right to cNext
            }

            stops += 1f to segments.last().second

            Brush.horizontalGradient(colorStops = stops.toTypedArray())
        }
    }

    // Track (fond)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(barHeight)
            .clip(RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
    ) {
        // ✅ un seul “draw” (plus clean + souvent plus optimisé que 3 Box)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = reveal
                    transformOrigin = TransformOrigin(0f, 0.5f)
                }
                .background(brush = brush)
        )
    }
}



@Composable
fun AnimatedCountText(
    target: Int,
    modifier: Modifier = Modifier,
    style: TextStyle,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    softWrap: Boolean = true,
    overflow: TextOverflow = TextOverflow.Clip,
    durationMillis: Int = 650,
    delayMillis: Int = 0,
    enabled: Boolean = true,
    format: (Int) -> String = { it.toString() }
) {
    val safeTarget = target.coerceAtLeast(0)
    val anim = remember { Animatable(0f) }

    LaunchedEffect(safeTarget, enabled) {
        if (!enabled) {
            anim.snapTo(safeTarget.toFloat())
            return@LaunchedEffect
        }
        if (delayMillis > 0) delay(delayMillis.toLong())
        anim.snapTo(0f)
        anim.animateTo(
            targetValue = safeTarget.toFloat(),
            animationSpec = tween(durationMillis = durationMillis)
        )
    }

    Text(
        text = format(anim.value.toInt()),
        modifier = modifier,
        style = style,
        color = color,
        fontWeight = fontWeight,
        textAlign = textAlign,
        maxLines = maxLines,
        softWrap = softWrap,
        overflow = overflow
    )
}


private fun buildDashboardShoppingUi(
    items: List<ShoppingListItemUi>
): DashboardShoppingUi {
    val toBuy = items
        .asSequence()
        .filter { !it.isChecked }
        .toList()

    val preview = toBuy
        .take(3)
        .map { item ->
            buildString {
                append(item.name.trim().ifBlank { "Produit" })
                item.quantity
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { qty -> append(" — $qty") }
            }
        }

    return DashboardShoppingUi(
        totalToBuy = toBuy.size,
        preview = preview
    )
}