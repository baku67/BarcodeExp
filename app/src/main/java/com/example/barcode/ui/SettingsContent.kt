package com.example.barcode.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import com.example.barcode.auth.AppMode
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import com.example.barcode.ui.components.SnackbarBus
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import com.example.barcode.auth.AuthViewModel
import com.example.barcode.ui.components.ThemeToggleRow
import com.example.barcode.user.UserPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    navController: NavHostController,
    innerPadding: PaddingValues,
    authVm: AuthViewModel
) {

    val scope = rememberCoroutineScope()

    val mode = authVm.appMode.collectAsState(initial = AppMode.AUTH).value
    val token = authVm.token.collectAsState(initial = null).value
    val cachedEmail = authVm.userEmail.collectAsState(initial = null).value
    val cachedIsVerified = authVm.userIsVerified.collectAsState(initial = null).value
    val prefs = authVm.preferences.collectAsState(initial = UserPreferences()).value

    var refreshing by rememberSaveable { mutableStateOf(false) }
    var resending by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var deleting by rememberSaveable { mutableStateOf(false) }

    suspend fun refreshProfile() {
        authVm.refreshProfile()
            .onFailure { SnackbarBus.show("Impossible de charger le profil : ${it.message ?: it}") }
    }

    LaunchedEffect(mode, token) {
        refreshProfile()
    }

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = {
            scope.launch {
                refreshing = true
                refreshProfile()
                refreshing = false
            }
        },
        modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
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
            }

            item {
                ElevatedCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Apparence", style = MaterialTheme.typography.titleLarge)

                        ThemeToggleRow(
                            prefs = prefs,
                            onToggleDark = { checked -> authVm.onThemeToggled(checked) }
                        )
                    }
                }
            }

            if (mode == AppMode.AUTH) {
                item {
                    // Si MODE AUTH (User en cache) on affiche ses infos
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Compte", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(6.dp))

                            Text("Email : ${cachedEmail ?: "Chargement..."}", style = MaterialTheme.typography.bodyLarge)

                            val verifiedLabel = when (cachedIsVerified) {
                                true -> "Oui ✅"
                                false -> "Non ❌"
                                null -> "Chargement..."
                            }

                            Text(
                                "Email vérifié : $verifiedLabel",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            if (cachedIsVerified == false) {
                                Spacer(Modifier.height(10.dp))

                                Text(
                                    "Tu n’as pas encore confirmé ton email. Vérifie tes spams si besoin.",
                                    style = MaterialTheme.typography.bodySmall
                                )

                                Spacer(Modifier.height(8.dp))

                                OutlinedButton(
                                    enabled = !resending,
                                    onClick = {
                                        scope.launch {
                                            resending = true
                                            authVm.resendVerifyEmail()
                                                .onSuccess { SnackbarBus.show("Email renvoyé ✅") }
                                                .onFailure { SnackbarBus.show("Impossible : ${it.message ?: it}") }
                                            resending = false
                                        }
                                    }
                                ) {
                                    if (resending) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(Modifier.width(10.dp))
                                            Text("Envoi…")
                                        }
                                    } else {
                                        Text("Renvoyer l’email de confirmation")
                                    }
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            // Déconnexion
                            Button(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    scope.launch {
                                        authVm.logout()
                                        navController.navigate("auth/login") {
                                            popUpTo(0)
                                            launchSingleTop = true
                                        }
                                    }
                                }
                            ) {
                                Text("Se déconnecter")
                            }

                            // Suppression compte
                            Spacer(Modifier.height(12.dp))

                            OutlinedButton(
                                onClick = { showDeleteDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !deleting,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text(if (deleting) "Suppression..." else "Supprimer mon compte")
                            }

                            if (showDeleteDialog) {
                                AlertDialog(
                                    onDismissRequest = { if (!deleting) showDeleteDialog = false },
                                    title = { Text("Supprimer le compte ?") },
                                    text = {
                                        Text("Cette action est définitive. Tes données cloud seront supprimées.")
                                    },
                                    confirmButton = {
                                        Button(
                                            enabled = !deleting,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.error,
                                                contentColor = MaterialTheme.colorScheme.onError
                                            ),
                                            onClick = {
                                                scope.launch {
                                                    if (token.isNullOrBlank()) {
                                                        SnackbarBus.show("Token manquant")
                                                        return@launch
                                                    }

                                                    deleting = true
                                                    authVm.deleteAccount()
                                                        .onSuccess {
                                                            SnackbarBus.show("Compte supprimé")
                                                            authVm.logout()
                                                            navController.navigate("auth/login") { popUpTo(0); launchSingleTop = true }
                                                        }
                                                        .onFailure { SnackbarBus.show("Suppression impossible : ${it.message ?: it}") }
                                                    deleting = false
                                                    showDeleteDialog = false
                                                }
                                            }
                                        ) {
                                            if (deleting) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(18.dp),
                                                        strokeWidth = 2.dp,
                                                        color = MaterialTheme.colorScheme.onError
                                                    )
                                                    Spacer(Modifier.width(10.dp))
                                                    Text("Suppression…")
                                                }
                                            } else {
                                                Text("Supprimer définitivement")
                                            }
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(
                                            enabled = !deleting,
                                            onClick = { showDeleteDialog = false }
                                        ) { Text("Annuler") }
                                    }
                                )
                            }
                        }
                    }

                }
            }

            if (mode == AppMode.LOCAL) {
                item {
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
                                    // SnackbarBus.show("À venir : création de compte + partage du frigo")

                                    navController.navigate("auth/register") {
                                        launchSingleTop = true
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
            }
        }
    }
}
