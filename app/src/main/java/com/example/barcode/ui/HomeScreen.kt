package com.example.barcode.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.example.barcode.ui.components.HeaderBar

// Écran d'accueil avec un bouton pour accéder à l'OCR
@Composable
fun HomeScreen(navController: NavHostController) {
    Scaffold(
        topBar = { HeaderBar(title = "FrigoZen", null, Icons.Filled.Home) }
    ) { innerPadding ->
        Column(Modifier.padding(innerPadding)) {
            // Contenu de ton écran
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ItemsScreen(navController = navController)
                }

            }
        }

    }

}