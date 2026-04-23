package com.example.barcode.features.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.barcode.data.local.entities.ItemEntity
import com.example.barcode.features.listeCourse.ShoppingListItemUi

@Composable
fun HomeContent(
    onNavigateToListeCourses: () -> Unit,
    onNavigateToRecipes: () -> Unit,
    onNavigateToItems: () -> Unit,
    innerPadding: PaddingValues,
    items: List<ItemEntity>,
    shoppingItems: List<ShoppingListItemUi>,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Dashboard(
                items = items,
                shoppingItems = shoppingItems,
                onNavigateToItems = onNavigateToItems,
                onNavigateToListeCourses = onNavigateToListeCourses,
                onNavigateToRecipes = onNavigateToRecipes
            )
        }
    }
}