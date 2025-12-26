package com.example.barcode.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.barcode.auth.SessionManager
import com.example.barcode.ui.components.HeaderBar
import com.example.barcode.ui.components.NavBar
import com.example.barcode.ui.components.NavBarItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import com.example.barcode.auth.AppMode
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue

@Composable
fun SettingsScreen(navController: NavHostController) {

    val appContext = LocalContext.current.applicationContext
    val session = remember { SessionManager(appContext) }
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }

    val mode = session.appMode.collectAsState(initial = AppMode.AUTH).value

    Scaffold(
        topBar = { HeaderBar(title = "FrigoZen", null, Icons.Filled.Home) },
        bottomBar = {
            NavBar(
                navController = navController,
                items = listOf(
                    NavBarItem("Home", Icons.Filled.Home, "home"),
                    NavBarItem("Items", Icons.Filled.List, "items"),
                    NavBarItem("Settings", Icons.Filled.Settings, "settings"),
                    // NavBarItem("Ajouter", Icons.Filled.AddCircle, "addItem/scan", matchPrefix = true)
                )
            )
        },
        snackbarHost = { SnackbarHost(snack) }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
        ) {

            // Bloc : Mode actuel

            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Mode d’utilisation", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))

                    val modeLabel = when (mode) {
                        AppMode.LOCAL -> "Local (sur ce téléphone)"
                        AppMode.AUTH -> "Connecté (API + synchronisation)"
                    }

                    Text(modeLabel, style = MaterialTheme.typography.bodyLarge)

                    if (mode == AppMode.LOCAL) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Tes données restent uniquement sur ton appareil. Pas de partage, pas de sync multi-appareils.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Proposition sync données (uniquement en local)
            if (mode == AppMode.LOCAL) {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Synchroniser & partager", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Passe en mode compte pour sauvegarder en base, synchroniser entre appareils " +
                                    "et partager le frigo avec d’autres utilisateurs.",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    snack.showSnackbar("À venir : création de compte + partage du frigo")
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Sync, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Créer un compte pour synchroniser & partager")
                        }
                    }
                }
            }

            // Déconnexion
            Button(
                onClick = {
                    scope.launch {
                        session.logout()
                        navController.navigate("auth/login") {
                            popUpTo(0)
                            launchSingleTop = true
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Se déconnecter")
            }
        }
    }
}
