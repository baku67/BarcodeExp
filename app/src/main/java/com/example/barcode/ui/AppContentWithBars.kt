package com.example.barcode.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.SetMeal
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import com.example.barcode.ui.components.HeaderBar
import com.example.barcode.ui.components.NavBar
import com.example.barcode.ui.components.NavBarItem
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import com.example.barcode.R
import com.example.barcode.interfaces.AppIcon
import com.example.barcode.ui.components.SnackbarBus

@Composable
fun AppContentWithBars(
    navController: NavHostController,
    selectedRoute: String,
    onTabClick: (String) -> Unit,
    content: @Composable (PaddingValues, snackbarHostState: SnackbarHostState) -> Unit
) {
    val items = listOf(
        NavBarItem(label = "Accueil", route = "home", icon = AppIcon.Vector(Icons.Filled.Home)),
        NavBarItem(label = "Courses", route = "listeCourses", icon = AppIcon.Vector(Icons.Filled.ReceiptLong)),
        NavBarItem(label = "Items", route = "items", icon = AppIcon.Drawable(R.drawable.ic_nav_fridge_icon)),
        NavBarItem(label = "Recettes", route = "recipes", icon = AppIcon.Vector(Icons.Filled.RestaurantMenu)), // ReceiptLong, MenuBook, RestaurantMenu(!), Restaurant
        NavBarItem(label = "Paramètres", route = "settings", icon = AppIcon.Vector(Icons.Filled.Settings))
    )

    val (title, subtitle) = when {
        selectedRoute.startsWith("home") -> "FrigoZen" to "Accueil"
        selectedRoute.startsWith("listeCourses") -> "FrigoZen" to "Courses"
        selectedRoute.startsWith("items") -> "FrigoZen" to "Produits"
        selectedRoute.startsWith("recipes") -> "FrigoZen" to "Recettes"
        selectedRoute.startsWith("settings") -> "FrigoZen" to "Réglages"
        else -> "FrigoZen" to null
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        SnackbarBus.messages.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            HeaderBar(
                title = title,
                onIconClick = {
                    // logo cliqué => retour Home
                    onTabClick("home")
                }
            )
        },
        bottomBar = {
            // ✅ ici, on ne navigate PAS: on laisse le pager gérer
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
