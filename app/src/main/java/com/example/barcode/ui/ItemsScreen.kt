package com.example.barcode.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.barcode.ui.components.ItemsViewModel
import java.util.Date
import androidx.compose.runtime.getValue
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController


@Composable
fun ItemsScreen(navController: NavHostController, vm: ItemsViewModel = viewModel()) {
    val list by vm.items.collectAsState(initial = emptyList())
    val primary = MaterialTheme.colorScheme.primary

    Column {
        Text("Frigo", fontSize = 20.sp, color = primary, fontWeight = FontWeight.SemiBold,)
        LazyColumn {
            items(list) { it ->
                Row (Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${it.name ?: "(sans nom)"} • ${it.expiryDate?.let { d -> Date(d) } ?: "—"}")
                    TextButton(onClick = { vm.deleteItem(it.id) }) { Text("Suppr") }
                }
            }
        }

        // Bouton d’exemple pour ajouter un item
        Button(onClick = {
            val in7days = System.currentTimeMillis() + 7L*24*60*60*1000
            vm.addItem(name = "Chipssss", brand = "Nutella")
        }) { Text("test") }

        // Bouton pour lancer le parcours d'ajout
        Button(onClick = { navController.navigate("addItem") }) {
            Text("+ Ajouter un produit")
        }
    }
}