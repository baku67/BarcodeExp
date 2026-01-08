package com.example.barcode.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.barcode.ui.components.Dashboard


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

        // 1) Bloc bienvenue
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
            ) {
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


        // 2) Dashboard (Card Items et Card ListeCourses)
        item {
            Dashboard(
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