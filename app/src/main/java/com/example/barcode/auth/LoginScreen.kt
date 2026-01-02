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
import kotlinx.coroutines.launch
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import com.example.barcode.R

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
        containerColor = Color.Transparent
    ) { innerPadding ->

        AuthCenteredScreen(innerPadding) {

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Box(
                    modifier = Modifier
                        .size(104.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.frigozen_icon),
                        contentDescription = "FrigoZen",
                        modifier = Modifier.size(72.dp)
                    )
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    text = "FrigoZen",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = "Connexion",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(Modifier.height(15.dp))

                TextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    interactionSource = emailIS,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.loading,
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
}
