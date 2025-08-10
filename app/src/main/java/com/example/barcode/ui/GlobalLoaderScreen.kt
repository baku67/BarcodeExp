package com.example.barcode.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import kotlinx.coroutines.delay
import com.example.barcode.R


@Composable
fun GlobalLoaderScreen(nav: NavHostController) {

    val gradient = remember {
        Brush.horizontalGradient(
            listOf(
                Color(0xFF2196F3), // bleu
                Color(0xFF4CAF50)  // vert
            )
        )
    }

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
            .background(Color(0xFFFAFAFA))
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
                    .fillMaxWidth(0.9f)
                    .aspectRatio(1f)
                    .padding(bottom = 0.dp),
                contentScale = ContentScale.Fit
            )

            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(brush = gradient)) { // dégradé uniquement ici
                        append("Piece of food, Peace of mind")
                    }
                },
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.SemiBold, // ou FontWeight.Bold
                    fontSize = 18.sp                  // ajuste (ex: 20.sp)
                ),
                modifier = Modifier.padding(bottom = 70.dp).absoluteOffset(x = 0.dp, y = -20.dp)
            )


            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 4.dp
            )

            //Text(
            //    text = "Chargement…",
            //    style = MaterialTheme.typography.bodySmall,
            //    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            //    modifier = Modifier.padding(top = 12.dp)
            //)


        }
    }
}