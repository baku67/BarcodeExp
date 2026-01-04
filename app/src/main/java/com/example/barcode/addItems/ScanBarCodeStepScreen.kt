package com.example.barcode.addItems

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.barcode.OpenFoodFacts.ProductInfo
import com.example.barcode.ui.ScanBarCodeScreen

// Demande d'autorisation Caméra puis -> ScanBarCodeScreen
@Composable
fun ScanBarCodeStepScreen(
    onValidated: (product: ProductInfo, barcode: String) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val activity = context.findActivity()

    var cameraGranted by remember {
        mutableStateOf(context.hasPermission(Manifest.permission.CAMERA))
    }

    val requestCamera = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        cameraGranted = granted
    }

    // Si OK → on affiche l’écran caméra
    if (cameraGranted) {
        ScanBarCodeScreen(onValidated = onValidated)
        return
    }

    // Sinon → UI “permission manquante”
    val permanentlyDenied = activity?.isPermanentlyDenied(Manifest.permission.CAMERA) == true

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Autorisation requise", style = MaterialTheme.typography.titleLarge)
        Text(
            "La caméra est nécessaire pour scanner les codes-barres. " +
                    "Tu peux aussi annuler et ajouter manuellement plus tard.",
            style = MaterialTheme.typography.bodyMedium
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!permanentlyDenied) {
                Button(onClick = { requestCamera.launch(Manifest.permission.CAMERA) }) {
                    Text("Autoriser la caméra")
                }
            } else {
                Button(onClick = { context.openAppSettings() }) {
                    Text("Ouvrir les réglages")
                }
            }

            OutlinedButton(onClick = onCancel) {
                Text("Annuler")
            }
        }
    }
}

/* -------- Helpers -------- */

private fun Context.hasPermission(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

private fun ComponentActivity.isPermanentlyDenied(permission: String): Boolean {
    val denied = ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
    val showRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, permission)
    return denied && !showRationale
}

private fun Context.openAppSettings() {
    startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    )
}

private fun Context.findActivity(): ComponentActivity? {
    var ctx = this
    while (ctx is android.content.ContextWrapper) {
        if (ctx is ComponentActivity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
