package com.example.barcode

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.barcode.features.bootstrap.GlobalLoaderScreen
import androidx.navigation.compose.navigation
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.barcode.features.addItems.AddItemViewModel
import com.example.barcode.features.addItems.ScanBarCodeStepScreen
import com.example.barcode.features.addItems.ConfirmStepScreen
import com.example.barcode.features.addItems.ScanDlcStepScreen
import com.example.barcode.features.auth.LoginScreen
import com.example.barcode.features.addItems.ItemsViewModel
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.barcode.features.addItems.AddItemChooseScreen
import com.example.barcode.features.addItems.ItemAddMode
import com.example.barcode.features.addItems.manual.ManualDetailsStepScreen
import com.example.barcode.features.addItems.manual.ManualSubtypeStepScreen
import com.example.barcode.features.addItems.manual.ManualTypeStepScreen
import com.example.barcode.features.auth.RegisterScreen
import com.example.barcode.common.ui.navigation.MainTabsScreen
import com.example.barcode.features.bootstrap.TimelineIntroScreen
import com.example.barcode.features.auth.AuthRepository
import com.example.barcode.features.auth.AuthViewModel
import com.example.barcode.core.SessionManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import com.example.barcode.common.bus.SnackbarBus
import com.example.barcode.common.ui.components.AppBackground
import com.example.barcode.common.ui.theme.Theme
import com.example.barcode.features.addItems.manual.ManualLeftoversDetailsStepScreen
import com.example.barcode.features.addItems.manual.ManualTaxonomy
import com.example.barcode.features.addItems.manual.ManualTaxonomyRepository
import com.example.barcode.features.addItems.manual.rememberManualTaxonomy
import com.example.barcode.features.fridge.components.bottomSheetDetails.GoodToKnowScreen
import com.example.barcode.sync.SyncScheduler


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

            val repo = remember { AuthRepository() }
            val session = remember(appContext) { SessionManager(appContext) }
            val authVm = remember { AuthViewModel(repo, session) }

            val navController = rememberNavController()

            Theme(session = session) {
                AppBackground {

                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Color.Transparent,
                        tonalElevation = 0.dp
                    ) {

                        // ✅ charge la taxonomie dès le démarrage pour que le frigo ait les images SUBTYPES
                        val taxonomyBoot = rememberManualTaxonomy()

                        // ✅ optionnel (mais OK) : assure le chargement même si taxonomyBoot n’est pas “utilisé”
                        LaunchedEffect(Unit) {
                            ManualTaxonomyRepository.preload(appContext)
                        }

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

                        // ✅ déclenche une sync WorkManager après login/register pour pousser les items ajoutés en mode LOCAL (PENDING_CREATE) vers le backend
                        LaunchedEffect(Unit) {
                            authVm.events.collect { event ->
                                when (event) {

                                    // ✅ Login/Register : sync OK + sortir du graph auth
                                    AuthViewModel.AuthEvent.GoHome -> {
                                        SyncScheduler.enqueueSync(appContext)
                                        navController.navigate("tabs") {
                                            popUpTo("auth") { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    }

                                    // ✅ Mode LOCAL : pas de sync + sortir du graph auth
                                    AuthViewModel.AuthEvent.GoHomeLocal -> {
                                        navController.navigate("tabs") {
                                            popUpTo("auth") { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    }

                                    else -> Unit
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

                            composable("good_to_know/{itemName}") { backStackEntry ->
                                val encoded = backStackEntry.arguments?.getString("itemName").orEmpty()
                                val itemName = Uri.decode(encoded)

                                GoodToKnowScreen(
                                    itemName = itemName,
                                    onClose = { navController.popBackStack() }
                                )
                            }

                            // Parcours addItem (choix -> ScanBarCode -> ScanDate -> Confirm) ou (choix -> type -> selection/remplissage)
                            navigation(startDestination = "addItem/choose", route = "addItem") {

                                fun close() {
                                    // ✅ Si "tabs" est déjà dans la backstack (cas normal), on pop directement.
                                    val popped = navController.popBackStack("tabs", inclusive = false)

                                    // ✅ Fallback si pour une raison quelconque tabs n'est pas dans la stack
                                    if (!popped) {
                                        navController.navigate("tabs") {
                                            popUpTo("addItem") { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    }
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
                                        onCancel = { close() }
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
                                        onCancel = { close() }
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
                                        onCancel = { close() }
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

                                    ConfirmStepScreen(
                                        draft = draft,
                                        onConfirm = { name, brand, expiry ->
                                            addVm.setDetails(name, brand)
                                            addVm.setExpiryDate(expiry)

                                            itemsVm.addItem(
                                                barcode = (draft.barcode ?: "(sans barcode)"),
                                                name = (name ?: draft.name ?: "(sans nom)"),
                                                brand = (brand ?: draft.brand ?: "(sans brand)"),
                                                expiry = (expiry ?: draft.expiryDate),
                                                imageUrl = draft.imageUrl,
                                                imageIngredientsUrl = draft.imageIngredientsUrl,
                                                imageNutritionUrl = draft.imageNutritionUrl,
                                                nutriScore = draft.nutriScore,
                                                addMode = draft.addMode.value
                                            )

                                            close()
                                        },
                                        onBack = { navController.popBackStack() },
                                        onCancel = { close() },
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

                                    val taxonomy = rememberManualTaxonomy()

                                    if (taxonomy == null) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator()
                                        }
                                        return@composable
                                    }

                                    ManualTypeStepScreen(
                                        taxonomy = taxonomy,
                                        onPick = { typeCode: String ->
                                            addVm.setManualType(typeCode)

                                            val hasSubtypes = taxonomy.subtypesOf(typeCode).isNotEmpty()

                                            when {
                                                typeCode == "LEFTOVERS" ->
                                                    navController.navigate("addItem/manual/leftovers/details")

                                                hasSubtypes ->
                                                    navController.navigate("addItem/manual/subtype")

                                                else ->
                                                    navController.navigate("addItem/manual/details")
                                            }
                                        },
                                        onPickSubtype = { typeCode: String, subtypeCode: String ->
                                            addVm.setManualType(typeCode)
                                            addVm.setManualSubtype(subtypeCode)

                                            if (typeCode == "LEFTOVERS") {
                                                navController.navigate("addItem/manual/leftovers/details")
                                            } else {
                                                navController.navigate("addItem/manual/details")
                                            }
                                        },
                                        onCancel = { close() },
                                        onBack = { navController.popBackStack() }
                                    )

                                }


                                // ✅ Manuel - Étape 2 : subtype (vegetables, viande, laitiers)
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
                                        onCancel = { close() }
                                    )
                                }

                                /* TODO: Branch “Restes / Tupperware” : parcours différent
                                Tu as demandé un parcours différent : c’est une bonne idée (sinon ça va devenir bancal avec “marque”, “nutri-score”, etc.).
                                Option recommandée (simple & clean)
                                Dès ManualTypeStepScreen, si type == LEFTOVERS, navigate vers un autre flow :
                                addItem/manual/leftovers/details (nom = “Restes …”, notes, portion, date de cuisson)
                                addItem/manual/leftovers/expiry (proposition auto J+2 / J+3)
                                confirm*/

                                // ✅ Manuel - Étape 3 : détails + confirm
                                composable("addItem/manual/details") { backStackEntry ->
                                    val parentEntry = remember(backStackEntry) {
                                        navController.getBackStackEntry("addItem")
                                    }
                                    val addVm: AddItemViewModel = viewModel(parentEntry)
                                    val draft by addVm.draft.collectAsState()

                                    val homeEntry = remember(backStackEntry) {
                                        navController.getBackStackEntry("tabs")
                                    }
                                    val itemsVm: ItemsViewModel = viewModel(homeEntry)

                                    ManualDetailsStepScreen(
                                        draft = draft,
                                        onNext = { name, brandOrNull, expiryMs ->
                                            val finalDraft = draft.copy(
                                                name = name,
                                                brand = brandOrNull,
                                                expiryDate = expiryMs
                                            )

                                            itemsVm.addItemFromDraft(finalDraft)
                                            close()
                                        },
                                        onBack = { navController.popBackStack() },
                                        onCancel = { close() }
                                    )
                                }


                                // ✅ Manuel - Branch LEFTOVERS : détails spécifiques + confirm
                                composable("addItem/manual/leftovers/details") { backStackEntry ->
                                    val parentEntry = remember(backStackEntry) {
                                        navController.getBackStackEntry("addItem")
                                    }
                                    val addVm: AddItemViewModel = viewModel(parentEntry)
                                    val draft by addVm.draft.collectAsState()

                                    val homeEntry = remember(backStackEntry) {
                                        navController.getBackStackEntry("tabs")
                                    }
                                    val itemsVm: ItemsViewModel = viewModel(homeEntry)

                                    ManualLeftoversDetailsStepScreen(
                                        draft = draft,
                                        onMetaChange = { addVm.setManualMetaJson(it) },
                                        onConfirm = { dishName, expiryMs, metaJson ->
                                            val finalDraft = draft.copy(
                                                name = dishName,
                                                brand = null,
                                                expiryDate = expiryMs,
                                                manualSubtypeCode = null,     // ✅ pas de subtype pour LEFTOVERS
                                                manualMetaJson = metaJson     // ✅ on stocke les toggles ici
                                            )

                                            itemsVm.addItemFromDraft(finalDraft)
                                            close()
                                        },
                                        onBack = { navController.popBackStack() },
                                        onCancel = { close() }
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

