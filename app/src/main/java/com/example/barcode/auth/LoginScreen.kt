package com.example.barcode.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SyncDisabled
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.barcode.ui.components.HeaderBar
import kotlinx.coroutines.launch
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import android.app.Activity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navController: NavHostController,
    viewModel: AuthViewModel,
    onNavigateToRegister: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    // Proposition d'enregistrer le mdp si login réussi puis navigation /tabs
    val activity = LocalContext.current as? Activity
    var loginFromPasswordManager by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                AuthViewModel.AuthEvent.GoHome -> {
                    // Si tu veux garder ton "savePassword" avant de quitter :
                    if (!loginFromPasswordManager && activity != null) {
                        savePassword(activity, email.trim(), password)
                    }

                    navController.navigate("tabs") {
                        popUpTo("auth") { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
        }
    }

    // *** Focus tracking (pour credentials bottom sheet)
    val emailIS = remember { MutableInteractionSource() }
    val passIS  = remember { MutableInteractionSource() }
    val emailFocused by emailIS.collectIsFocusedAsState()
    val passFocused by passIS.collectIsFocusedAsState()
    // *** Credentials BottomSheet state
    var showSheet by rememberSaveable { mutableStateOf(false) }
    var sheetAlreadyShown by rememberSaveable { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // *** Ouvre le sheet à la 1ère entrée dans un champ
    LaunchedEffect(emailFocused, passFocused) {
        if (!sheetAlreadyShown && (emailFocused || passFocused)) {
            showSheet = true
            sheetAlreadyShown = true
        }
    }

    // Lancement du login auto après pré-remplissage Credentials
    suspend fun handlePickedCredential(pickedEmail: String, pickedPassword: String) {
        loginFromPasswordManager = true
        email = pickedEmail
        password = pickedPassword
        viewModel.onLogin(pickedEmail, pickedPassword) // ✅ auto-login
    }

    // BottomSheet Credentials
    val scope = rememberCoroutineScope()
    // Pour éviter d’ouvrir le sheet à chaque focus
    var credentialPromptShown by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(emailFocused) {
        if (!credentialPromptShown && emailFocused && activity != null && !state.loading) {
            credentialPromptShown = true

            scope.launch {
                val saved = getSavedPassword(activity)
                if (saved != null) {
                    handlePickedCredential(saved.first, saved.second)
                }
            }
        }
    }

    Scaffold(
        topBar = { HeaderBar(title = "FrigoZen", null, null) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
        ) {

            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Connexion",
                    fontSize = 30.sp,
                    modifier = Modifier.absoluteOffset(x = 0.dp, y = 0.dp)
                )
            }

            Spacer(Modifier.height(15.dp))

            TextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                interactionSource = emailIS,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.loading
            )

            Spacer(Modifier.height(12.dp))

            TextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Mot de passe") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                interactionSource = passIS,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.loading
            )

            Spacer(Modifier.height(16.dp))

            if (state.loading) {

                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CircularProgressIndicator()
                }

            } else {

                Button(
                    onClick = {
                        loginFromPasswordManager = false
                        viewModel.onLogin(email, password)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Se connecter")
                }

                TextButton(onClick = onNavigateToRegister) {
                    Text("Créer un compte")
                }

                Spacer(Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { viewModel.onUseLocalMode() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.SyncDisabled, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Mode local")
                }
            }

            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
