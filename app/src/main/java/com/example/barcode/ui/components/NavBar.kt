package com.example.barcode.ui.components

import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

data class NavBarItem(
    val label: String,
    val icon: ImageVector,
    val route: String,
    val matchPrefix: Boolean = false // utile si ta route est du style "addItem/scan"
)

@Composable
fun NavBar(
    navController: NavHostController,
    items: List<NavBarItem>,
    containerColor: Color = MaterialTheme.colorScheme.primary
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        containerColor = containerColor,
        contentColor = Color.White
    ) {
        items.forEach { item ->
            val selected = when {
                currentRoute == null -> false
                item.matchPrefix -> currentRoute.startsWith(item.route)
                else -> currentRoute == item.route
            }

            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        navController.navigate(item.route) {
                            launchSingleTop = true
                            restoreState = true
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                        }
                    }
                },
                icon = { Icon(imageVector = item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                alwaysShowLabel = true,
                colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    selectedTextColor = Color.White,
                    unselectedIconColor = Color.White.copy(alpha = 0.85f),
                    unselectedTextColor = Color.White.copy(alpha = 0.85f),
                    indicatorColor = Color.White.copy(alpha = 0.18f)
                )
            )
        }
    }
}
