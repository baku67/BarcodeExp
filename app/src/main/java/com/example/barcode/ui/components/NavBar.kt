package com.example.barcode.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.barcode.interfaces.AppIcon

data class NavBarItem(
    val label: String,
    val route: String,
    val icon: AppIcon,
    val matchPrefix: Boolean = false // utile si ta route est du style "addItem/scan"
)

@Composable
fun NavBar(
    navController: NavHostController,
    items: List<NavBarItem>,
    activeRoute: String? = null,
    containerColor: Color = Color.Transparent,
    onItemClick: ((NavBarItem) -> Unit)? = null, // si fourni, on ne navigate pas ici
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val destination = navBackStackEntry?.destination

    val dividerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)

    NavigationBar(
        modifier = Modifier.drawBehind {
            val stroke = 1.dp.toPx()
            drawLine(
                color = dividerColor,
                start = Offset(0f, 0f),
                end = Offset(size.width, 0f),
                strokeWidth = stroke
            )
        },
        containerColor = containerColor,
        contentColor = Color.White
    ) {
        items.forEach { item ->

            val selected = when {
                // ✅ Mode pager: on se base sur activeRoute (pas sur NavController)
                activeRoute != null -> routeMatches(activeRoute, item)

                // ✅ Mode NavController normal: gère aussi les nested graphs
                else -> destination?.hierarchy?.any { dest ->
                    val r = dest.route ?: return@any false
                    if (item.matchPrefix) r.startsWith(item.route) else r == item.route
                } == true
            }

            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (selected) return@NavigationBarItem

                    // ✅ Mode "pager": AppScaffoldWithBars gère le click
                    if (onItemClick != null) {
                        onItemClick(item)
                        return@NavigationBarItem
                    }

                    // ✅ Mode normal: NavController navigate
                    navController.navigate(item.route) {
                        launchSingleTop = true
                        restoreState = true
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                    }
                },
                icon = {
                    when (val ic = item.icon) {
                        is AppIcon.Vector -> Icon(
                            imageVector = ic.image,
                            contentDescription = item.label
                        )

                        is AppIcon.Drawable -> Icon(
                            painter = painterResource(id = ic.resId),
                            contentDescription = item.label,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                label = { Text(item.label) },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                )
            )
        }
    }
}

private fun routeMatches(activeRoute: String, item: NavBarItem): Boolean {
    val r = activeRoute.substringBefore("?")
    return if (item.matchPrefix) r.startsWith(item.route) else r == item.route
}
