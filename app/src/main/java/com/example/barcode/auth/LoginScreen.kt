package com.example.barcode.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SyncDisabled
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.barcode.ui.components.HeaderBar
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    navController: NavHostController,
    viewModel: AuthViewModel,
    onNavigateToRegister: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    Scaffold(
        topBar = { HeaderBar(title = "FrigoZen", null, null) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
        ) {

            TextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            TextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Mot de passe") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            if (state.loading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = { viewModel.onLogin(email, password) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Se connecter")
                }
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

            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
