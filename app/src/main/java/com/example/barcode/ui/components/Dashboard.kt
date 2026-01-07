package com.example.barcode.ui.components

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.TimerOff
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun DashboardRow(
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

        DashboardCardShoppingListFake(
            modifier = Modifier.fillMaxWidth(),
            onClick = onNavigateToListeCourses
        )

        DashboardCardRecipesFake(
            modifier = Modifier.fillMaxWidth(),
            onClick = onNavigateToRecipes
        )
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
    // Fake “3 prochains” (à remplacer plus tard par tes vrais items triés par expiryDate)
    val nextExpiring = listOf(
        "Jambon — J+1fzfefze",
        "Yaourt — J+2",
        "Salade — J+3",
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp),
        shape = RoundedCornerShape(18.dp),
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
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,     // ✅ centrage vertical
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {

                // 1) Gauche : nombre + label (colonne)
                Column(
                    modifier = Modifier
                        .weight(0.55f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AnimatedCountText(
                        target = total,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        durationMillis = 750
                    )
                    Text(
                        text = "Produits",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 2) Milieu : 3 mini-sections en colonne (Périmés -> Bientôt -> Sains)
                Column(
                    modifier = Modifier
                        .weight(0.55f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatIconRow(
                        icon = Icons.Outlined.TimerOff,
                        label = "Périmés",
                        value = expired,
                        color = MaterialTheme.colorScheme.tertiary,
                        iconAlpha = 0.80f // on boust un peu l'opacité de l'icone périmés (rouge)
                    )
                    StatIconRow(
                        icon = Icons.Outlined.WarningAmber,
                        label = "Bientôt",
                        value = soon,
                        color = Color(0xFFF9A825)
                    )
                    StatIconRow(
                        icon = Icons.Outlined.Eco,
                        label = "Sains",
                        value = fresh,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // 3) Droite : mini-liste "À consommer" (inchangée)
                Column(
                    modifier = Modifier
                        .weight(0.90f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    nextExpiring.forEach { line ->
                        Text(
                            text = "• $line",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
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
private fun DashboardCardShoppingListFake(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    // TODO Fake data
    val total = 7
    val lastAdded = "Lait demi-écrémé"
    val lastBy = "Basile"
    val notifCount = 3

    Card(
        modifier = modifier.height(150.dp),
        shape = RoundedCornerShape(18.dp),
        onClick = onClick,
        interactionSource = remember { MutableInteractionSource() }  // "ripple" anim au click
    ) {
        Box(Modifier.fillMaxSize()) {

            // Contenu
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    AnimatedCountText(
                        target = total,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        durationMillis = 750
                    )
                    Text(
                        text = "Liste de courses",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column {
                    Text(
                        text = "Dernier ajout : $lastAdded",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Par : $lastBy",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Bulle notification (coin haut droit)
            if (notifCount > 0) {
                NotificationBubble(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp),
                    count = notifCount
                )
            }
        }
    }
}



@Composable
private fun DashboardCardRecipesFake(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    // TODO Fake data
    val totalRecipes = 42
    val aiRecipes = 8
    val sharedRecipes = 5
    val lastGenerated = "Pâtes au thon"

    Card(
        modifier = modifier.height(150.dp),
        shape = RoundedCornerShape(18.dp),
        onClick = onClick,
        interactionSource = remember { MutableInteractionSource() } // "ripple" anim au click
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                AnimatedCountText(
                    target = totalRecipes,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    durationMillis = 750
                )
                Text(
                    text = "Recettes",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("IA : $aiRecipes • Partagées : $sharedRecipes", style = MaterialTheme.typography.bodySmall)
                Text(
                    text = "Dernière : $lastGenerated",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}





@Composable
private fun StatIconRow(
    icon: ImageVector,
    label: String,
    value: Int,
    color: Color,
    iconAlpha: Float = 0.55f
) {
    val hasValue = value > 0

    val baseAlpha = when (label) {
        "Périmés" -> 0.40f
        "Bientôt" -> 0.18f
        else -> 0.20f
    }

    val borderAlpha = if (hasValue) baseAlpha.coerceAtLeast(0.22f) else 0.06f
    val borderColor = if (hasValue) {
        // plus visible quand > 0
        color.copy(alpha = borderAlpha)
    } else {
        // neutre/grisé quand 0
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)
    }

    val numberColor = if (hasValue) color else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
    val numberDisplay = if (hasValue) value.coerceIn(0, 99).toString() else "—"

    val iconTint = if (hasValue) {
        color.copy(alpha = iconAlpha)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(0.75.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(top = 6.dp, bottom = 6.dp, start = 10.dp, end = 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // ✅ Icône reste colorée (mais tu peux la calmer un peu si value == 0)
        val effectiveIconAlpha = if (hasValue) iconAlpha else (iconAlpha * 0.75f)

        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconTint,
            modifier = Modifier.size(18.dp)
        )

        if (hasValue) {
            AnimatedCountText(
                target = value.coerceIn(0, 99),
                modifier = Modifier.width(22.dp),
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = numberColor,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
                durationMillis = 550
            )
        } else {
            Text(
                text = "—",
                modifier = Modifier.width(22.dp),
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = numberColor,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}



@Composable
private fun NotificationBubble(
    modifier: Modifier = Modifier,
    count: Int
) {
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(Color(0xFFC62828)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (count > 99) "99+" else count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
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

    // Track (fond)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
    ) {
        // Contenu (segments) -> reveal via scaleX depuis la gauche
        Row(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = reveal
                    transformOrigin = TransformOrigin(0f, 0.5f) // ✅ reveal depuis la gauche
                }
        ) {
            if (we > 0f) Box(Modifier.weight(we).fillMaxHeight().background(MaterialTheme.colorScheme.tertiary))
            if (ws > 0f) Box(Modifier.weight(ws).fillMaxHeight().background(Color(0xFFF9A825)))
            if (wf > 0f) Box(Modifier.weight(wf).fillMaxHeight().background(MaterialTheme.colorScheme.primary))
        }
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