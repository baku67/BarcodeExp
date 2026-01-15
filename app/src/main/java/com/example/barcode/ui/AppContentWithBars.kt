package com.example.barcode.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import com.example.barcode.ui.components.HeaderBar
import com.example.barcode.ui.components.NavBar
import com.example.barcode.ui.components.NavBarItem
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import com.example.barcode.R
import com.example.barcode.interfaces.AppIcon
import com.example.barcode.ui.components.HeaderBarState
import com.example.barcode.ui.components.LocalAppTopBarState
import com.example.barcode.ui.components.SnackbarBus

@Composable
fun AppContentWithBars(
    navController: NavHostController,
    selectedRoute: String,
    onTabClick: (String) -> Unit,
    content: @Composable (PaddingValues, snackbarHostState: SnackbarHostState) -> Unit
) {
    // Icones du NavBar
    val items = listOf(
        NavBarItem(label = "Accueil", route = "home", icon = AppIcon.Vector(Icons.Filled.Home)),
        NavBarItem(label = "Courses", route = "listeCourses", icon = AppIcon.Vector(Icons.Filled.ReceiptLong)),
        NavBarItem(label = "Items", route = "items", icon = AppIcon.Drawable(R.drawable.ic_nav_fridge_icon_thicc)),
        NavBarItem(label = "Recettes", route = "recipes", icon = AppIcon.Vector(Icons.Filled.RestaurantMenu)), // ReceiptLong, MenuBook, RestaurantMenu(!), Restaurant
        NavBarItem(label = "Paramètres", route = "settings", icon = AppIcon.Vector(Icons.Filled.Settings))
    )

    // Titre et subtitle du HeaderBar
    val (title, subtitle) = when {
        selectedRoute.startsWith("home") -> "Tableau de bord" to "Tableau de bord"
        selectedRoute.startsWith("listeCourses") -> "Courses" to "Courses"
        selectedRoute.startsWith("items") -> "Frigo" to "Produits"
        selectedRoute.startsWith("recipes") -> "Recettes" to "Recettes"
        selectedRoute.startsWith("settings") -> "Réglages" to "Réglages"
        else -> "FrigoZen" to null
    }

    // Icones du HeaderBar (commentées)
    val iconForRoute: AppIcon? = when {
        selectedRoute.startsWith("home") -> AppIcon.Vector(Icons.Filled.Home)
        selectedRoute.startsWith("listeCourses") -> AppIcon.Vector(Icons.Filled.ReceiptLong)
        selectedRoute.startsWith("items") -> AppIcon.Drawable(R.drawable.ic_nav_fridge_icon_thicc)
        selectedRoute.startsWith("recipes") -> AppIcon.Vector(Icons.Filled.RestaurantMenu)
        selectedRoute.startsWith("settings") -> AppIcon.Vector(Icons.Filled.Settings)
        else -> null
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        SnackbarBus.messages.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val topBarState = remember { HeaderBarState() }
    topBarState.icon = iconForRoute

    topBarState.title = title
    topBarState.subtitle = subtitle

    CompositionLocalProvider(LocalAppTopBarState provides topBarState) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                HeaderBar(
                    title = topBarState.title,
                    subtitle = topBarState.subtitle,
                    icon = topBarState.icon, // ✅
                    onIconClick = { onTabClick("home") },
                    actions = { topBarState.actions?.invoke(this) ?: Unit }
                )
            },
            bottomBar = {
                NavBar(
                    navController = navController,
                    items = items,
                    activeRoute = selectedRoute,
                    onItemClick = { item -> onTabClick(item.route) }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            content(innerPadding, snackbarHostState)
        }
    }
}
