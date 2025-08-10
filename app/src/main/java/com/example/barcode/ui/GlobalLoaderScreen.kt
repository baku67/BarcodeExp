package com.example.barcode.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.delay
import com.example.barcode.R


@Composable
fun GlobalLoaderScreen(nav: NavHostController) {

    // ⏳ Simule le chargement sans bloquer l’UI
    LaunchedEffect(Unit) {
        // TODO: remplace ce delay par tes vrais chargements (préfetch, DB, etc.)
        delay(3000)
        // exemple : pré-initialiser MLKit, charger prefs, réhydrater cache, ping API…
        // BarcodeScanning.getClient() // warm-up éventuel
        // repository.prefetch()
        // database.warmup()

        nav.navigate("home") {
            popUpTo("splash") { inclusive = true } // retire l’écran de la back stack
        }
    }

    // UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 🖼 Grande image (remplace par ton visuel)
            Image(
                painter = painterResource(id = R.drawable.frigozen_title),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .aspectRatio(1f)
                    .padding(bottom = 24.dp),
                contentScale = ContentScale.Fit
            )

            Text(
                text = "Chargement…",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 4.dp
            )
        }
    }
}