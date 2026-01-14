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
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.barcode.OpenFoodFacts.ProductInfo
import com.example.barcode.R
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

    // ✅ Permission OK -> vraie étape 1/3 avec HeaderBar "Ajouter un produit"
    if (cameraGranted) {
        AddItemStepScaffold(
            step = 1,
            onBack = null,          // pas de retour à l’étape précédente
            onCancel = onCancel
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                ScanBarCodeScreen(onValidated = onValidated)
            }
        }
        return
    }

    // ❌ Permission KO -> page dédiée centrée + branding en haut (sans navigation)
    val permanentlyDenied = activity?.isPermanentlyDenied(Manifest.permission.CAMERA) == true

    // Lorsqu'on revient depuis settings du phone, on actualise l'état permission
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                cameraGranted = context.hasPermission(Manifest.permission.CAMERA)
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {

        BrandHeader(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp, bottom = 6.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Autorisation caméra", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "La caméra est nécessaire pour scanner les codes-barres.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!permanentlyDenied) {
                            Button(onClick = { requestCamera.launch(Manifest.permission.CAMERA) }) {
                                Text("Autoriser")
                            }
                        } else {
                            Button(onClick = { context.openAppSettings() }) {
                                Text("Ouvrir les réglages")
                            }
                        }
                        OutlinedButton(onClick = onCancel) { Text("Annuler") }
                    }
                }
            }
        }
    }
}

@Composable
private fun BrandHeader(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.size(34.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = "FrigoZen",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
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
