package com.example.barcode

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.barcode.ui.CameraDateOcrScreen
import com.example.barcode.ui.CameraOcrBarCodeScreen
import com.example.barcode.ui.GlobalLoaderScreen
import com.example.barcode.ui.theme.AppPrimary
import androidx.navigation.compose.navigation
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.barcode.items.AddItemViewModel
import com.example.barcode.items.ScanStepScreen
import com.example.barcode.items.DetailsStepScreen
import com.example.barcode.items.DateStepScreen
import com.example.barcode.auth.LoginScreen
import com.example.barcode.items.ItemsViewModel
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.barcode.auth.*
import com.example.barcode.ui.MainTabsScreen
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import com.example.barcode.ui.components.SnackbarBus
import com.example.barcode.ui.theme.AppBackground

private val LightColors = lightColorScheme(
    primary = AppPrimary,
    // onPrimary = Color.White, // etc. si besoin
)

object DeepLinkBus {
    private val _links = MutableSharedFlow<Uri>(extraBufferCapacity = 1)
    val links = _links.asSharedFlow()

    fun emit(uri: Uri) {
        _links.tryEmit(uri)
    }
}

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

        handleDeepLink(intent)

        // Configuration de l'UI Compose avec Navigation
        setContent {

            AppBackground {
                MaterialTheme(
                    colorScheme = LightColors,
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Color.Transparent,
                        tonalElevation = 0.dp
                    ) {
                        val navController = rememberNavController()

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
                            composable("splash") { GlobalLoaderScreen(navController) }
                            composable("dateOCR") { CameraDateOcrScreen() }
                            composable("barCodeOCR") { CameraOcrBarCodeScreen() }
                            composable("tabs") { MainTabsScreen(navController) } // contient la navigation au Swipe (Home/Items/Liste/Settings)

                            navigation(startDestination = "auth/login", route = "auth") {

                                composable("auth/login") { backStackEntry ->
                                    val appContext = LocalContext.current.applicationContext

                                    // ✅ même owner pour login + register
                                    val authGraphEntry =
                                        remember(backStackEntry) { navController.getBackStackEntry("auth") }

                                    // ✅ objets partagés (liés au graph auth)
                                    val session =
                                        remember(authGraphEntry) { SessionManager(appContext) }
                                    val repo =
                                        remember(authGraphEntry) { AuthRepository(ApiClient.authApi) }

                                    val authVm: AuthViewModel = viewModel(
                                        authGraphEntry,
                                        factory = object : ViewModelProvider.Factory {
                                            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                                if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
                                                    @Suppress("UNCHECKED_CAST")
                                                    return AuthViewModel(repo, session) as T
                                                }
                                                throw IllegalArgumentException("Unknown ViewModel: $modelClass")
                                            }
                                        }
                                    )

                                    LoginScreen(
                                        navController = navController,
                                        viewModel = authVm,
                                        onNavigateToRegister = { navController.navigate("auth/register") },
                                    )
                                }

                                composable("auth/register") { backStackEntry ->
                                    val appContext = LocalContext.current.applicationContext
                                    val authGraphEntry =
                                        remember(backStackEntry) { navController.getBackStackEntry("auth") }

                                    val session =
                                        remember(authGraphEntry) { SessionManager(appContext) }
                                    val repo =
                                        remember(authGraphEntry) { AuthRepository(ApiClient.authApi) }

                                    val authVm: AuthViewModel = viewModel(
                                        authGraphEntry,
                                        factory = object : ViewModelProvider.Factory {
                                            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                                if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
                                                    @Suppress("UNCHECKED_CAST")
                                                    return AuthViewModel(repo, session) as T
                                                }
                                                throw IllegalArgumentException("Unknown ViewModel: $modelClass")
                                            }
                                        }
                                    )

                                    RegisterScreen(
                                        navController = navController,
                                        viewModel = authVm,
                                        onNavigateToLogin = {
                                            val popped = navController.popBackStack("auth/login", inclusive = false)
                                            if (!popped) {
                                                navController.navigate("auth/login") {
                                                    popUpTo("auth/register") { inclusive = true }
                                                    launchSingleTop = true
                                                }
                                            }
                                        }
                                    )
                                }
                            }

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
                                    val parentEntry =
                                        remember(backStackEntry) { navController.getBackStackEntry("addItem") }
                                    val addVm: AddItemViewModel = viewModel(parentEntry)
                                    val draft by addVm.draft.collectAsState()

                                    DateStepScreen(
                                        productName = draft.name,
                                        productBrand = draft.brand,
                                        productImageUrl = draft.imageUrl,
                                        onValidated = { expiryMs ->
                                            addVm.setExpiryDate(expiryMs)
                                            navController.navigate("addItem/details")
                                        },
                                        onCancel = { navController.popBackStack() }
                                    )
                                }

                                composable("addItem/details") { backStackEntry ->
                                    val parentEntry =
                                        remember(backStackEntry) { navController.getBackStackEntry("addItem") }
                                    val addVm: AddItemViewModel = viewModel(parentEntry)
                                    val draft by addVm.draft.collectAsState()

                                    // ✅ partage la même instance que ItemsScreen via l’entrée "home"
                                    val homeEntry =
                                        remember(backStackEntry) { navController.getBackStackEntry("tabs") }
                                    val itemsVm: ItemsViewModel = viewModel(homeEntry)

                                    DetailsStepScreen(
                                        draft = draft,
                                        onConfirm = { name, brand, expiry ->
                                            addVm.setDetails(name, brand)
                                            addVm.setExpiryDate(expiry)

                                            // ✅ commit direct
                                            itemsVm.addItem(
                                                name = (name ?: draft.name ?: "(sans nom)"),
                                                brand = (brand ?: draft.brand ?: "(sans brand)"),
                                                // ajoute expiry si ton ItemsViewModel le supporte
                                                expiry = (expiry ?: draft.expiryDate),
                                                imageUrl = draft.imageUrl
                                            )

                                            addVm.reset()
                                            navController.popBackStack("tabs", false)
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

