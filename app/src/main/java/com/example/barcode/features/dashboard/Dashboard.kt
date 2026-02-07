package com.example.barcode.features.dashboard

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material.icons.outlined.TimerOff
import androidx.compose.material.icons.outlined.WarningAmber
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy


// TODO: vraies données route dashboard (counts et 5 nextExpiring)
private enum class ExpiryKind { EXPIRED, SOON, FRESH }
private data class ExpiringLine(
    val name: String,
    val note: String,   // ex: "hier", "1j", "5j", ya une fonction pour ça dans ItemsContent
    val kind: ExpiryKind
)
private data class FakeDashboardProducts(
    val fresh: Int,
    val soon: Int,
    val expired: Int,
    val nextExpiring: List<ExpiringLine>
) {
    val total: Int get() = fresh + soon + expired
}
@Composable
private fun rememberFakeDashboardProducts(): FakeDashboardProducts {
    // Stable entre recompositions (pas de flicker)
    return remember {
        val fresh = 18
        val soon = 3
        val expired = 1

        // ✅ cohérent avec les counts (1 EXPIRED, 2 SOON, 2 FRESH = 5 lignes)
        val nextExpiring = listOf(
            ExpiringLine("Jambon", "hier", ExpiryKind.EXPIRED),
            ExpiringLine("Yaourt nature", "1j", ExpiryKind.SOON),
            ExpiringLine("Salade", "2j", ExpiryKind.SOON),
            ExpiringLine("Poulet", "3j", ExpiryKind.FRESH),
            ExpiringLine("Fromage râpé", "6j", ExpiryKind.FRESH),
        )

        FakeDashboardProducts(
            fresh = fresh,
            soon = soon,
            expired = expired,
            nextExpiring = nextExpiring
        )
    }
}
// Fin TODO






@Composable
fun Dashboard(
    totalProducts: Int,
    freshCount: Int,
    expiringSoonCount: Int,
    expiredCount: Int,
    onNavigateToItems: () -> Unit,
    onNavigateToListeCourses: () -> Unit,
    onNavigateToRecipes: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        DashboardCardProductsWide(
            total = totalProducts,
            fresh = freshCount,
            soon = expiringSoonCount,
            expired = expiredCount,
            onClick = onNavigateToItems
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DashboardCardShoppingListFake(
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
    }
}





@Composable
private fun DashboardCardProductsWide(
    total: Int,
    fresh: Int,
    soon: Int,
    expired: Int,
    onClick: () -> Unit,
) {
    // TODO Fake “x prochains” (à remplacer plus tard par tes vrais items triés par expiryDate)
    // TODO: n'afficher que les items qui ont -3/-4 jours de conserv (parmis les 4-5 max)
    val fake = rememberFakeDashboardProducts()

    val total = fake.total
    val fresh = fake.fresh
    val soon = fake.soon
    val expired = fake.expired
    val nextExpiring = fake.nextExpiring

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
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.20f),
                                modifier = Modifier
                                    .size(72.dp)
                                    .textFadeToRight(fadeWidthFraction = 0.65f)
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

                    // PILLS
                    /*
                    Spacer(Modifier.height(15.dp))

                    // Mini-sections en ligne (ordre cohérent avec la barre : rouge → jaune → vert)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatIconPill(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Outlined.Eco,
                            label = "Sains",
                            value = fresh,
                            color = MaterialTheme.colorScheme.primary
                        )
                        StatIconPill(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Outlined.WarningAmber,
                            label = "Bientôt",
                            value = soon,
                            color = Color(0xFFF9A825)
                        )
                        StatIconPill(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Outlined.TimerOff,
                            label = "Périmés",
                            value = expired,
                            color = MaterialTheme.colorScheme.tertiary,
                            iconAlpha = 0.80f
                        )
                    }*/
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
private fun StatIconPill(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: Int,
    color: Color,
    iconAlpha: Float = 0.55f
) {
    val hasValue = value > 0

    val borderColor = if (hasValue) {
        color.copy(alpha = 0.22f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)
    }

    val numberColor =
        if (hasValue) color else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)

    val iconTint =
        if (hasValue) color.copy(alpha = iconAlpha)
        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)

    // ✅ valeur affichée (tu peux garder — quand 0, ou afficher 0)
    val safeValue = value.coerceIn(0, 99)

    Column(
        modifier = modifier
            .height(56.dp) // ✅ hauteur fixe => pills identiques
            .clip(RoundedCornerShape(12.dp))
            .border(0.75.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(vertical = 6.dp, horizontal = 6.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconTint,
            modifier = Modifier.size(18.dp)
        )

        Spacer(Modifier.height(4.dp))

        if (hasValue) {
            AnimatedCountText(
                target = safeValue,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = numberColor,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
                durationMillis = 450
            )
        } else {
            Text(
                text = "—",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = numberColor,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip
            )
        }

        // Optionnel : si tu veux le label (ça peut devenir chargé)
        /*
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
        */
    }
}


@Composable
private fun DashboardCardShoppingListFake(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    // Fake data
    val total = 12
    val preview = listOf("Lait", "Tomates", "Riz basmati")

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
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header (nombre + label) centré
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // Gauche : icône alignée à droite
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.End
                ) {
                    Icon(
                        imageVector = Icons.Filled.ReceiptLong,
                        contentDescription = "Courses",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.20f),
                        modifier = Modifier
                            .size(60.dp)
                            .textFadeToRight(fadeWidthFraction = 0.65f)
                    )
                }

                // Droite : nombre + label alignés à gauche
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

            // Mini-liste
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                preview.take(3).forEach { item ->
                    Text(
                        text = "• $item",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
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
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header (centré)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // Gauche : icône alignée à droite
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.End
                ) {
                    Icon(
                        imageVector = Icons.Outlined.RestaurantMenu,
                        contentDescription = "Recettes",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.20f),
                        modifier = Modifier
                            .size(60.dp)
                            .textFadeToRight(fadeWidthFraction = 0.65f)
                    )

                }

                // Droite : nombre + label alignés à gauche
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

            // Mini-liste (full width pour align End)
            Column(
                modifier = Modifier.fillMaxWidth(),
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



private fun Modifier.textFadeToRight(
    fadeWidthFraction: Float = 0.33f // 33% de la largeur à la fin du texte
): Modifier = this
    // ✅ indispensable pour que BlendMode.DstIn fonctionne proprement
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()

        val start = (1f - fadeWidthFraction).coerceIn(0f, 1f)

        // ✅ Masque alpha : opaque -> transparent (uniquement sur le contenu dessiné = le texte)
        drawRect(
            brush = Brush.horizontalGradient(
                colorStops = arrayOf(
                    0f to Color.White,
                    start to Color.White,
                    1f to Color.Transparent
                )
            ),
            blendMode = BlendMode.DstIn
        )
    }

