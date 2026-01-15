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
import com.example.barcode.bootstrap.GlobalLoaderScreen
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
import com.example.barcode.addItems.ItemAddMode
import com.example.barcode.addItems.manual.ManualDetailsStepScreen
import com.example.barcode.addItems.manual.ManualExpiryStepScreen
import com.example.barcode.addItems.manual.ManualSubtypeStepScreen
import com.example.barcode.addItems.manual.ManualType
import com.example.barcode.addItems.manual.ManualTypeStepScreen
import com.example.barcode.auth.*
import com.example.barcode.auth.ui.RegisterScreen
import com.example.barcode.ui.MainTabsScreen
import com.example.barcode.bootstrap.TimelineIntro.TimelineIntroScreen
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
                                        onPickScan = {
                                            addVm.setAddMode(ItemAddMode.BARCODE_SCAN)
                                            navController.navigate("addItem/scan/barcode")
                                        },
                                        onPickManual = {
                                            addVm.setAddMode(ItemAddMode.MANUAL)
                                            navController.navigate("addItem/manual/type")
                                        },
                                        onCancel = { close(addVm) }
                                    )
                                }



                                // ✅ 1) Scan - barcode
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
                                            addVm.setNutriScore(product.nutriScore)
                                            navController.navigate("addItem/scan/expiry")
                                        },
                                        onCancel = { close(addVm) }
                                    )
                                }

                                // ✅ 2) Scan - DLC
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

                                // ✅ 3) Scan - Confirm (ex-details)
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
                                                nutriScore = draft.nutriScore,
                                                addMode = draft.addMode.value
                                            )

                                            close(addVm)
                                        },
                                        onBack = { navController.popBackStack() },
                                        onCancel = { close(addVm) },
                                        onCycleImage = { addVm.cycleNextImage() },
                                        onNutriScoreChange = { addVm.setNutriScore(it)}
                                    )
                                }



                                // ✅ Manuel - Étape 1 : type
                                composable("addItem/manual/type") { backStackEntry ->
                                    val parentEntry = remember(backStackEntry) {
                                        navController.getBackStackEntry("addItem")
                                    }
                                    val addVm: AddItemViewModel = viewModel(parentEntry)

                                    ManualTypeStepScreen(
                                        onPick = { type ->
                                            addVm.setManualType(type)

                                            when (type) {

                                                ManualType.LEFTOVERS -> navController.navigate("addItem/manual/leftovers/details") // TODO

                                                ManualType.VEGETABLES,
                                                ManualType.MEAT,
                                                ManualType.DAIRY
                                                    -> navController.navigate("addItem/manual/subtype")

                                                else -> navController.navigate("addItem/manual/details")
                                            }
                                        },
                                        onCancel = { close(addVm) },
                                        onBack = { navController.popBackStack() }
                                    )
                                }

                                // ✅ Manuel - Étape 1.1 : subtype (vegetables, viande, laitiers)
                                composable("addItem/manual/subtype") { backStackEntry ->
                                    val parentEntry = remember(backStackEntry) {
                                        navController.getBackStackEntry("addItem")
                                    }
                                    val addVm: AddItemViewModel = viewModel(parentEntry)
                                    val draft by addVm.draft.collectAsState()

                                    ManualSubtypeStepScreen(
                                        draft = draft,
                                        onPick = { sub ->
                                            addVm.setManualSubtype(sub)
                                            navController.navigate("addItem/manual/details")
                                        },
                                        onBack = { navController.popBackStack() },
                                        onCancel = { close(addVm) }
                                    )
                                }

/*                              TODO: Branch “Restes / Tupperware” : parcours différent
                                Tu as demandé un parcours différent : c’est une bonne idée (sinon ça va devenir bancal avec “marque”, “nutri-score”, etc.).
                                Option recommandée (simple & clean)
                                Dès ManualTypeStepScreen, si type == LEFTOVERS, navigate vers un autre flow :
                                addItem/manual/leftovers/details (nom = “Restes …”, notes, portion, date de cuisson)
                                addItem/manual/leftovers/expiry (proposition auto J+2 / J+3)
                                confirm*/

                                // ✅ Manuel - Étape 2 : détails
                                composable("addItem/manual/details") { backStackEntry ->
                                    val parentEntry = remember(backStackEntry) {
                                        navController.getBackStackEntry("addItem")
                                    }
                                    val addVm: AddItemViewModel = viewModel(parentEntry)
                                    val draft by addVm.draft.collectAsState()

                                    ManualDetailsStepScreen(
                                        draft = draft,
                                        onNext = { name, brandOrNull ->
                                            addVm.setDetails(name, brandOrNull)
                                            navController.navigate("addItem/manual/expiry")
                                        },
                                        onBack = { navController.popBackStack() },
                                        onCancel = { close(addVm) }
                                    )
                                }

                                // ✅ Manuel - Étape 3 : DLC optionnelle
                                composable("addItem/manual/expiry") { backStackEntry ->
                                    val parentEntry = remember(backStackEntry) {
                                        navController.getBackStackEntry("addItem")
                                    }
                                    val addVm: AddItemViewModel = viewModel(parentEntry)
                                    val draft by addVm.draft.collectAsState()

                                    ManualExpiryStepScreen(
                                        title = draft.name ?: "Produit",
                                        onPickExpiry = { expiryMs ->
                                            addVm.setExpiryDate(expiryMs)
                                            navController.navigate("addItem/manual/confirm")
                                        },
                                        onSkip = {
                                            addVm.setExpiryDate(null)
                                            navController.navigate("addItem/manual/confirm")
                                        },
                                        onBack = { navController.popBackStack() },
                                        onCancel = { close(addVm) }
                                    )
                                }

                                // ✅ Manuel - Étape 4 : confirm (réutilise ton écran)
                                composable("addItem/manual/confirm") { backStackEntry ->
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
                                                imageUrl = draft.imageUrl, // souvent null en manuel
                                                imageIngredientsUrl = draft.imageIngredientsUrl,
                                                imageNutritionUrl = draft.imageNutritionUrl,
                                                nutriScore = draft.nutriScore,
                                                addMode = draft.addMode.value
                                            )

                                            close(addVm)
                                        },
                                        onBack = { navController.popBackStack() },
                                        onCancel = { close(addVm) },
                                        onCycleImage = { addVm.cycleNextImage() },
                                        onNutriScoreChange = { addVm.setNutriScore(it) }
                                    )
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

