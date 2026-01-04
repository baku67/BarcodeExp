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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.barcode.ui.components.DashboardRow
import com.example.barcode.ui.components.SnackbarBus

// Écran d'accueil avec un bouton pour accéder à l'OCR
@Composable
fun HomeContent(
    onNavigateToItems: () -> Unit,
    onNavigateToListeCourses: () -> Unit,
    innerPadding: PaddingValues,
    totalProducts: Int,
    freshCount: Int,
    expiringSoonCount: Int,
    expiredCount: Int,
) {

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

                DashboardRow(
                    totalProducts = totalProducts,
                    freshCount = freshCount,
                    expiringSoonCount = expiringSoonCount,
                    expiredCount = expiredCount,
                    onNavigateToItems = onNavigateToItems,
                    onNavigateToListeCourses = onNavigateToListeCourses,
                )

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
                                SnackbarBus.show("Fonction “Proposer une recette” : à venir 🙂")
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