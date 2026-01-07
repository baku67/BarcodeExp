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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.barcode.ui.components.DashboardRow
import com.example.barcode.ui.components.SnackbarBus


@Composable
fun HomeContent(
    onNavigateToListeCourses: () -> Unit,
    onNavigateToRecipes: () -> Unit,
    onNavigateToItems: () -> Unit,
    innerPadding: PaddingValues,
    totalProducts: Int,
    freshCount: Int,
    expiringSoonCount: Int,
    expiredCount: Int,
) {

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        item {
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
        }


        // 2) Alerte si autorisations manquantes -> redirection vers section /Settings
        // Attention: utiliser areNotificationsEnabled() avant envoie de notif,
        // ET demander permissions au moment où besoin sinon peut bloquer après plusieurs refus si User voit pas l'intéret au lancement de  l'app
        // PermissionsCard()
        item {
            Text(
                "Attention: autorisations requises ?",
                style = MaterialTheme.typography.titleMedium
            )

        }

        // 3) Dashboard (Card Items et Card ListeCourses)
        item {
            DashboardRow(
                totalProducts = totalProducts,
                freshCount = freshCount,
                expiringSoonCount = expiringSoonCount,
                expiredCount = expiredCount,
                onNavigateToItems = onNavigateToItems,
                onNavigateToListeCourses = onNavigateToListeCourses,
                onNavigateToRecipes = onNavigateToRecipes
            )
        }
    }

}