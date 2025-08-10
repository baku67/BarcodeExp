package com.example.barcode

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.barcode.ui.HomeScreen
import com.example.barcode.ui.CameraDateOcrScreen
import com.example.barcode.ui.CameraOcrBarCodeScreen
import com.example.barcode.ui.GlobalLoaderScreen
import com.example.barcode.ui.theme.AppPrimary

private val LightColors = lightColorScheme(
    primary = AppPrimary,
    // onPrimary = Color.White, // etc. si besoin
)

// MainActivity configure NavController et gère la permission caméra au démarrage
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Demande de la permission CAMERA dès le démarrage
        val requestPerm = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (!granted) finish() // Termine si refus
        }
        requestPerm.launch(Manifest.permission.CAMERA)

        // Configuration de l'UI Compose avec Navigation
        setContent {
            MaterialTheme(
                colorScheme = LightColors,
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    // Définition des routes
                    NavHost(navController, startDestination = "splash") {
                        composable("splash") { GlobalLoaderScreen(navController) }
                        composable("home") { HomeScreen(navController) }
                        composable("dateOCR") { CameraDateOcrScreen() }
                        composable("barCodeOCR") { CameraOcrBarCodeScreen() }
                    }
                }
            }
        }
    }
}
