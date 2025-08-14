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
import com.example.barcode.ui.ItemsScreen
import com.example.barcode.ui.theme.AppPrimary

import androidx.navigation.compose.navigation
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.barcode.add.AddItemViewModel
import com.example.barcode.add.AddItemDraft
import com.example.barcode.add.ScanStepScreen
import com.example.barcode.add.DetailsStepScreen
import com.example.barcode.add.ConfirmStepScreen
import com.example.barcode.add.DateStepScreen
import com.example.barcode.ui.components.ItemsViewModel

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
                        composable("items") { ItemsScreen(navController) }

                        navigation(startDestination = "addItem/scan", route = "addItem") {

                            composable("addItem/scan") { backStackEntry ->
                                val parentEntry = remember(backStackEntry) {
                                    navController.getBackStackEntry("addItem")
                                }
                                val addVm: AddItemViewModel = viewModel(parentEntry)

                                ScanStepScreen(
                                    onValidated = { product, code ->
                                        addVm.setBarcode(code)
                                        addVm.setDetails(product.name, product.brand)
                                        addVm.setImage(product.imageUrl)
                                        navController.navigate("addItem/date")
                                    },
                                    onCancel = { navController.popBackStack() }
                                )
                            }

                            // ⬇️ NOUVEL ÉCRAN DATE
                            composable("addItem/date") { backStackEntry ->
                                val parentEntry = remember(backStackEntry) { navController.getBackStackEntry("addItem") }
                                val addVm: AddItemViewModel = viewModel(parentEntry)

                                DateStepScreen(
                                    onValidated = { expiryMs ->
                                        addVm.setExpiryDate(expiryMs)   // ⬅️ on remplit le draft ici
                                        navController.navigate("addItem/details")
                                    },
                                    onCancel = { navController.popBackStack() }
                                )
                            }

                            composable("addItem/details") { backStackEntry ->
                                val parentEntry = remember(backStackEntry) {
                                    navController.getBackStackEntry("addItem")
                                }
                                val addVm: AddItemViewModel = viewModel(parentEntry)
                                val draft by addVm.draft.collectAsState()

                                DetailsStepScreen(
                                    draft = draft,
                                    onNext = { name, brand, expiry ->
                                        addVm.setDetails(name, brand)
                                        addVm.setExpiryDate(expiry)
                                        navController.navigate("addItem/confirm")
                                    },
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable("addItem/confirm") { backStackEntry ->
                                val parentEntry = remember(backStackEntry) {
                                    navController.getBackStackEntry("addItem")
                                }
                                val addVm: AddItemViewModel = viewModel(parentEntry)

                                val itemsVm: ItemsViewModel = viewModel(LocalContext.current as ComponentActivity)
                                val draft by addVm.draft.collectAsState()

                                ConfirmStepScreen(
                                    draft = draft,
                                    onConfirm = {
                                        itemsVm.addItem(
                                            name = draft.name ?: "(sans nom)",
                                            brand = draft.brand ?: "(sans marque)"
                                        )
                                        addVm.reset()
                                        navController.popBackStack("home", false)
                                    },
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
