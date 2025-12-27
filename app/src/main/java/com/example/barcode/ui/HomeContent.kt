package com.example.barcode.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch

// Écran d'accueil avec un bouton pour accéder à l'OCR
@Composable
fun HomeContent(navController: NavHostController, innerPadding: PaddingValues, snackbarHostState: SnackbarHostState) {

    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(16.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

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
                        Text(
                            "Recevoir des alertes (toggle)",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "Si toggle: Combien de jours avant (options)",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "Si pas autorisation: need autorisation notifs",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                // 3) Bloc proposer une recette
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Recettes", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(10.dp))
                        Text("(publicité)", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(10.dp))
                        Text("(pré-prompt) définir alergies, gouts etc...", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Fonction “Proposer une recette” : à venir 🙂")
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