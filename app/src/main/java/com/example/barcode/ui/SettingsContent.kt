package com.example.barcode.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.barcode.auth.SessionManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import com.example.barcode.auth.AppMode
import kotlinx.coroutines.launch
import com.example.barcode.auth.ApiClient
import com.example.barcode.auth.AuthRepository

@Composable
fun SettingsContent(navController: NavHostController, innerPadding: PaddingValues, snackbarHostState: SnackbarHostState) {

    val appContext = LocalContext.current.applicationContext
    val session = remember { SessionManager(appContext) }
    val scope = rememberCoroutineScope()

    val mode = session.appMode.collectAsState(initial = AppMode.AUTH).value

    // Si User en cache, on affiche ses infos
    val repo = remember { AuthRepository(ApiClient.authApi) }
    val token = session.token.collectAsState(initial = null).value
    val cachedEmail = session.userEmail.collectAsState(initial = null).value
    val cachedId = session.userId.collectAsState(initial = null).value
    LaunchedEffect(mode, token) {
        if (mode == AppMode.AUTH && !token.isNullOrBlank()) {
            repo.me(token)
                .onSuccess { session.saveUser(it) }
                .onFailure { snackbarHostState.showSnackbar("Impossible de charger le profil : ${it.message ?: it}") }
        }
    }


    Column(
        modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()
            .padding(16.dp)
    ) {

        // Bloc : Affichage du Mode actuel (LOCAL ou AUTH)
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Mode d’utilisation", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))

                val modeLabel = when (mode) {
                    AppMode.LOCAL -> "Local (sur ce téléphone)"
                    AppMode.AUTH -> "Connecté (synchronisation cloud)"
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

        // Si User en cache on affiche ses infos
        if (mode == AppMode.AUTH) {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Compte", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))

                    Text("Email : ${cachedEmail ?: "Chargement..."}", style = MaterialTheme.typography.bodyLarge)
                    // Text("Id : ${cachedId ?: "Chargement..."}", style = MaterialTheme.typography.bodyMedium)

                    Spacer(Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                if (!token.isNullOrBlank()) {
                                    repo.me(token)
                                        .onSuccess { session.saveUser(it); snackbarHostState.showSnackbar("Profil mis à jour") }
                                        .onFailure { snackbarHostState.showSnackbar("Erreur : ${it.message ?: it}") }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Rafraîchir le profil")
                    }
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
                                snackbarHostState.showSnackbar("À venir : création de compte + partage du frigo")
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
