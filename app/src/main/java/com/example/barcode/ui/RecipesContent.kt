package com.example.barcode.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.barcode.auth.SessionManager
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipesContent(innerPadding: PaddingValues, isActive: Boolean) {

    val appContext = LocalContext.current.applicationContext
    val session = remember { SessionManager(appContext) }
    val scope = rememberCoroutineScope()

    val mode = session.appMode.collectAsState(initial = AppMode.AUTH).value
    val token = session.token.collectAsState(initial = null).value

    var refreshing by rememberSaveable { mutableStateOf(false) }

    // vérifie si données déja fetch pour ce JWT, "1er chargement" todo:remplacer par 1ers chargements dans GloabLoaderScreen Splash
    var initialLoading by rememberSaveable { mutableStateOf(false) }   // ✅ loader initial dédié au premier chargement
    var loadedForToken by rememberSaveable { mutableStateOf<String?>(null) }

    // TODO: remplacer le delay par vrai refresh VM/API
    suspend fun refreshRecipesTokens() {
        if (mode == AppMode.AUTH && !token.isNullOrBlank()) {
            delay(3_000) // todo
        }
    }

    LaunchedEffect(isActive, mode, token) {
        val canLoad = isActive && mode == AppMode.AUTH && !token.isNullOrBlank()
        if (!canLoad) return@LaunchedEffect

        // auto-load 1 seule fois (par token)
        if (loadedForToken == token) return@LaunchedEffect

        initialLoading = true
        try {
            refreshRecipesTokens()
        } finally {
            initialLoading = false
            loadedForToken = token // ✅ même si échec => évite spam navigation (refresh manuel pour retenter)
        }
    }

    Box(Modifier.fillMaxSize()) {

        // Barre de chargement top
        if (initialLoading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter) // ✅ OK car on est dans BoxScope
            )
        }

        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = {
                scope.launch {
                    if (mode != AppMode.AUTH || token.isNullOrBlank()) {
                        SnackbarBus.show("Connecte-toi pour synchroniser.") // todo: bouton redirect login dans le snack ?
                        return@launch
                    }

                    refreshing = true
                    try {
                        refreshRecipesTokens()
                    } finally {
                        refreshing = false
                    }
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

                // Contenu todo
                item {
                    androidx.compose.material3.Text("Aucune recette pour le moment.")

                    // Btn Recette API
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Recettes", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(10.dp))
                            Text("(publicité)", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(10.dp))
                            Text("(pré-prompt) définir alergies, gouts etc...", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    SnackbarBus.show("Fonction “Proposer une recette” : à venir 🙂")
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Chercher une recette")
                            }
                        }
                    }
                }

                item {
                    // Btn Recette IA
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Recettes", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(10.dp))
                            Text("(publicité)", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(10.dp))
                            Text("(pré-prompt) définir alergies, gouts etc...", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    SnackbarBus.show("Fonction “Proposer une recette” : à venir 🙂")
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Générer une recette *IA*")
                            }
                        }
                    }
                }
            }

        }
    }
}
