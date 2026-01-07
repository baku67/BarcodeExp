package com.example.barcode.ui.components

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AvTimer
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.TimerOff
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

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
        "Jambon — J+1",
        "Yaourt — J+2",
        "Salade — J+3",
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        shape = RoundedCornerShape(18.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // Gauche : chiffres + stats
            Column(
                modifier = Modifier
                    .weight(1.15f)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = total.toString(),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Produits",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatIconColumn(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.Eco,
                        value = fresh,
                        color = MaterialTheme.colorScheme.primary,
                        contentDescription = "Frais"
                    )
                    StatIconColumn(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.WarningAmber,
                        value = soon,
                        color = Color(0xFFF9A825),
                        contentDescription = "Expire bientôt"
                    )
                    StatIconColumn(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.BugReport,
                        value = expired,
                        color = MaterialTheme.colorScheme.tertiary,
                        contentDescription = "Périmé"
                    )
                }
            }

            // Droite : Top 3 prochainement à expirer
            Column(
                modifier = Modifier
                    .weight(0.85f)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = "Prochaines expirations",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = "À consommer",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

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
        onClick = onClick
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
                    Text(
                        text = total.toString(),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
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
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = totalRecipes.toString(),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
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
private fun StatIconColumn(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    value: Int,
    color: Color,
    contentDescription: String,
    iconAlpha: Float = 0.55f
) {
    val bg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)

    Box(
        modifier = modifier
            .padding(horizontal = 2.dp)              // espace entre “sections”
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StatValueText(value = value, color = color)
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                // tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f), // TODO neutre mieux ?
                tint = color.copy(alpha = iconAlpha),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun StatValueText(value: Int, color: Color) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val txt = value.toString()
        val style = if (txt.length >= 2) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge

        Text(
            text = txt,
            style = style,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
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
