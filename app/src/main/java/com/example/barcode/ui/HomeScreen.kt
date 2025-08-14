package com.example.barcode.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
                    Button(onClick = { navController.navigate("barCodeOCR") }) {
                        Text(text = "Scanner code-barres")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { navController.navigate("dateOCR") }) {
                        Text(text = "OCR Dates")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { navController.navigate("items") }) {
                        Text(text = "Frigo")
                    }
                }
            }
        }
    }

}