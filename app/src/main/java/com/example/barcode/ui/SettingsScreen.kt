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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(navController: NavHostController) {
    val appContext = LocalContext.current.applicationContext
    val session = remember { SessionManager(appContext) }
    val scope = rememberCoroutineScope()

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
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            session.clear()
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
}
