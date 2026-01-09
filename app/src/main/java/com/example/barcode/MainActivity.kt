package com.example.barcode

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.barcode.ui.ScanDlcScreen
import com.example.barcode.ui.ScanBarCodeScreen
import com.example.barcode.ui.GlobalLoaderScreen
import androidx.navigation.compose.navigation
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.barcode.addItems.AddItemViewModel
import com.example.barcode.addItems.ScanBarCodeStepScreen
import com.example.barcode.addItems.DetailsStepScreen
import com.example.barcode.addItems.ScanDlcStepScreen
import com.example.barcode.auth.ui.LoginScreen
import com.example.barcode.addItems.ItemsViewModel
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.barcode.addItems.AddItemChooseScreen
import com.example.barcode.auth.*
import com.example.barcode.auth.ui.RegisterScreen
import com.example.barcode.ui.MainTabsScreen
import com.example.barcode.ui.TimelineIntro.TimelineIntroScreen
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import com.example.barcode.ui.components.SnackbarBus
import com.example.barcode.ui.theme.AppBackground
import com.example.barcode.ui.theme.Theme


object DeepLinkBus {
    private val _links = MutableSharedFlow<Uri>(extraBufferCapacity = 1)
    val links = _links.asSharedFlow()

    fun emit(uri: Uri) {
        _links.tryEmit(uri)
    }
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleDeepLink(intent)

        setContent {

            val appContext = LocalContext.current.applicationContext
            val session = remember { SessionManager(appContext) }
            val repo = remember { AuthRepository(ApiClient.authApi) }
            val authVm: AuthViewModel = viewModel(
                factory = AuthViewModelFactory(repo, session)
            )
            val navController = rememberNavController()

            Theme(session = session) {
                AppBackground {

                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Color.Transparent,
                        tonalElevation = 0.dp
                    ) {

                        // Si ouverture app suite a click lien email verification (page dédiée ou ici juste snack):
                        LaunchedEffect(Unit) {
                            DeepLinkBus.links.collect { uri ->
                                if (uri.scheme == "frigozen" && uri.host == "email-verified") {
                                    val userId = uri.getQueryParameter("id") // optionnel

                                    // ✅ 1) UX: naviguer vers l'écran principal (ou settings)
                                    navController.navigate("tabs") {
                                        launchSingleTop = true
                                    }

                                    // ✅ 2) UX: afficher un message (todo: centraliser le Scaffold SnackBar ici ?)
                                    SnackbarBus.show("Email vérifié ✅")

                                    // ✅ 3) (optionnel) rafraîchir /me pour récupérer isVerified=true
                                    // scope.launch { repo.me(token)... }
                                }
                            }
                        }

                        // Définition des routes
                        NavHost(
                            navController,
                            startDestination = "splash",
                        ) {

                            // GloablLoaderScreen
                            composable("splash") { GlobalLoaderScreen(navController) }

                            // Ecran anim chronologie
                            composable("introTimeline") { TimelineIntroScreen(navController) }

                            navigation(startDestination = "auth/login", route = "auth") {

                                composable("auth/login") {
                                    LoginScreen(
                                        navController = navController,
                                        viewModel = authVm,
                                        onNavigateToRegister = { navController.navigate("auth/register") },
                                    )
                                }

                                composable("auth/register") {
                                    RegisterScreen(
                                        navController = navController,
                                        viewModel = authVm,
                                        onNavigateToLogin = {
                                            val popped = navController.popBackStack(
                                                "auth/login",
                                                inclusive = false
                                            )
                                            if (!popped) {
                                                navController.navigate("auth/login") {
                                                    popUpTo("auth/register") {
                                                        inclusive = true
                                                    }
                                                    launchSingleTop = true
                                                }
                                            }
                                        }
                                    )
                                }
                            }

                            composable("tabs") {
                                MainTabsScreen(
                                    navController,
                                    authVm
                                )
                            } // contient la navigation au Swipe (Home/Items/Liste/Settings)


                            // Parcours addItem (choix -> ScanBarCode -> ScanDate -> Confirm) ou (choix -> type -> selection/remplissage)
                            navigation(startDestination = "addItem/choose", route = "addItem") {

                                fun close(addVm: AddItemViewModel) {
                                    addVm.reset()
                                    val popped = navController.popBackStack("tabs", false)
                                    if (!popped) navController.navigate("tabs") { launchSingleTop = true }
                                }

                                // ✅ 0) Choix méthode
                                composable("addItem/choose") { backStackEntry ->
                                    val parentEntry = remember(backStackEntry) {
                                        navController.getBackStackEntry("addItem")
                                    }
                                    val addVm: AddItemViewModel = viewModel(parentEntry)

                                    AddItemChooseScreen(
                                        onPickScan = { navController.navigate("addItem/scan/barcode") },
                                        onPickManual = { navController.navigate("addItem/manual") }, // placeholder
                                        onCancel = { close(addVm) }
                                    )
                                }

                                // ✅ 1) Scan barcode
                                composable("addItem/scan/barcode") { backStackEntry ->
                                    val parentEntry = remember(backStackEntry) {
                                        navController.getBackStackEntry("addItem")
                                    }
                                    val addVm: AddItemViewModel = viewModel(parentEntry)

                                    ScanBarCodeStepScreen(
                                        onValidated = { product, code ->
                                            addVm.setBarcode(code)
                                            addVm.setDetails(product.name, product.brand)
                                            addVm.setImage(product.imageUrl)
                                            addVm.setImageCandidates(product.imageCandidates)
                                            addVm.setIngredientsImage(product.imageIngredientsUrl)
                                            addVm.setNutritionImage(product.imageNutritionUrl)
                                            navController.navigate("addItem/scan/expiry")
                                        },
                                        onCancel = { close(addVm) }
                                    )
                                }

                                // ✅ 2) Scan DLC
                                composable("addItem/scan/expiry") { backStackEntry ->
                                    val parentEntry = remember(backStackEntry) {
                                        navController.getBackStackEntry("addItem")
                                    }
                                    val addVm: AddItemViewModel = viewModel(parentEntry)
                                    val draft by addVm.draft.collectAsState()

                                    ScanDlcStepScreen(
                                        productName = draft.name,
                                        productBrand = draft.brand,
                                        productImageUrl = draft.imageUrl,
                                        onValidated = { expiryMs ->
                                            addVm.setExpiryDate(expiryMs)
                                            navController.navigate("addItem/scan/confirm")
                                        },
                                        onBack = { navController.popBackStack() },
                                        onCancel = { close(addVm) }
                                    )
                                }

                                // ✅ 3) Confirm (ex-details)
                                composable("addItem/scan/confirm") { backStackEntry ->
                                    val parentEntry = remember(backStackEntry) {
                                        navController.getBackStackEntry("addItem")
                                    }
                                    val addVm: AddItemViewModel = viewModel(parentEntry)
                                    val draft by addVm.draft.collectAsState()

                                    val homeEntry = remember(backStackEntry) {
                                        navController.getBackStackEntry("tabs")
                                    }
                                    val itemsVm: ItemsViewModel = viewModel(homeEntry)

                                    DetailsStepScreen(
                                        draft = draft,
                                        onConfirm = { name, brand, expiry ->
                                            addVm.setDetails(name, brand)
                                            addVm.setExpiryDate(expiry)

                                            itemsVm.addItem(
                                                name = (name ?: draft.name ?: "(sans nom)"),
                                                brand = (brand ?: draft.brand ?: "(sans brand)"),
                                                expiry = (expiry ?: draft.expiryDate),
                                                imageUrl = draft.imageUrl,
                                                imageIngredientsUrl = draft.imageIngredientsUrl,
                                                imageNutritionUrl = draft.imageNutritionUrl,
                                                nutriScore = draft.nutriScore
                                            )

                                            close(addVm)
                                        },
                                        onBack = { navController.popBackStack() },
                                        onCancel = { close(addVm) },
                                        onCycleImage = { addVm.cycleNextImage() }
                                    )
                                }

                                // ✅ placeholder manuel (le temps de faire le vrai flow)
                                composable("addItem/manual") { backStackEntry ->
                                    val parentEntry = remember(backStackEntry) {
                                        navController.getBackStackEntry("addItem")
                                    }
                                    val addVm: AddItemViewModel = viewModel(parentEntry)

                                    Column(
                                        modifier = Modifier.fillMaxSize().padding(18.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text("Ajout manuel", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                                        Text("À venir : sélection type (légume/viande/restes), détails, date optionnelle…")
                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            OutlinedButton(onClick = { navController.popBackStack() }) { Text("Retour") }
                                            Button(onClick = { close(addVm) }) { Text("Annuler") }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme != "frigozen") return
        DeepLinkBus.emit(uri)
    }

}

