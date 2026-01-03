package com.example.barcode.auth.ui

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


// Permet de centrer verticalement le contenu tout en gérant l'apparition du clavier
@Composable
fun AuthCenteredScreen(
    innerPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val scroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)      // si tu es dans un Scaffold/NavHost
            .imePadding()               // ✅ pousse le contenu au-dessus du clavier
            .navigationBarsPadding()    // ✅ évite les barres système
            .verticalScroll(scroll)     // ✅ petit écran => scroll
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center // ✅ centre si ça tient
    ) {
        // Optionnel : limite la largeur sur tablette
        Box(modifier = Modifier.widthIn(max = 420.dp)) {
            Column(content = content)
        }
    }
}
