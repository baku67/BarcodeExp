package com.example.barcode.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.barcode.ui.components.HeaderBar
import com.example.barcode.ui.components.NavBar
import com.example.barcode.ui.components.NavBarItem
import kotlinx.coroutines.launch

// Écran d'accueil avec un bouton pour accéder à l'OCR
@Composable
fun HomeScreen(navController: NavHostController) {

    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }

    Scaffold(
        topBar = { HeaderBar(title = "FrigoZen", null, Icons.Filled.Home) },
        bottomBar = {
            NavBar(
                navController = navController,
                items = listOf(
                    NavBarItem("Home", Icons.Filled.Home, "home"),
                    NavBarItem("Items", Icons.Filled.List, "items"),
                    NavBarItem("Settings", Icons.Filled.Settings, "settings"),
                    // NavBarItem("Ajouter", Icons.Filled.AddCircle, "addItem/scan", matchPrefix = true)
                )
            )
        },
        snackbarHost = { SnackbarHost(snack) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    // ItemsScreen(navController = navController)

                    // 1) Bloc bienvenue
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Bienvenue 👋", style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Gère tes produits, surveille les DLC, et trouve des idées recettes.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    // 2) Bloc nombre de produits en DLC
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("DLC", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(6.dp))
                            val dlcProductsCount = 0
                            Text(
                                "Nombre de produits en DLC: $dlcProductsCount",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }

                    // 3) Bloc proposer une recette
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Recettes", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    scope.launch {
                                        snack.showSnackbar("Fonction “Proposer une recette” : à venir 🙂")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Proposer une recette")
                            }
                        }
                    }
                }

            }
        }

    }

}