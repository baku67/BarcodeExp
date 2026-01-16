package com.example.barcode.features.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
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
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.app.NotificationManagerCompat

// ***********
//Pour les notifs, le check areNotificationsEnabled() est recommandé avant d’envoyer une notif.
// ***********
// Android recommande de ne demander les permissions que quand la feature est utilisée, sinon tu “grilles” tes chances si l’utilisateur refuse plusieurs fois.
// ***********

@Composable
fun PermissionsCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val lifecycleOwner = LocalLifecycleOwner.current

    fun refresh(): PermissionsState = PermissionsState(
        cameraGranted = context.hasPermission(Manifest.permission.CAMERA),
        notificationsGranted = context.notificationsAllowed()
    )

    var state by remember { mutableStateOf(refresh()) }

    // Refresh quand on revient des réglages
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) state = refresh()
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    val requestCamera = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        state = state.copy(cameraGranted = granted)
    }

    val requestNotif = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        // même si permission OK, l’utilisateur peut avoir désactivé les notifs dans les réglages
        state = state.copy(notificationsGranted = granted && context.notificationsAllowed())
    }

    val missing = buildList {
        if (!state.cameraGranted) add("Caméra")
        if (!state.notificationsGranted) add("Notifications")
    }

    if (missing.isEmpty()) return

    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Autorisations manquantes", style = MaterialTheme.typography.titleMedium)
            Text(
                "Certaines fonctionnalités peuvent être limitées : ${missing.joinToString(", ")}.",
                style = MaterialTheme.typography.bodyMedium
            )

            PermissionRow(
                label = "Caméra",
                granted = state.cameraGranted,
                onPrimaryAction = { requestCamera.launch(Manifest.permission.CAMERA) },
                onOpenSettings = { context.openAppSettings() },
                // Si refus permanent => on propose directement les réglages
                showSettingsHint = activity?.isPermanentlyDenied(Manifest.permission.CAMERA) == true
            )

            PermissionRow(
                label = "Notifications",
                granted = state.notificationsGranted,
                onPrimaryAction = {
                    if (Build.VERSION.SDK_INT >= 33) {
                        requestNotif.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        context.openAppSettings()
                    }
                },
                onOpenSettings = { context.openAppSettings() },
                showSettingsHint = if (Build.VERSION.SDK_INT >= 33)
                    activity?.isPermanentlyDenied(Manifest.permission.POST_NOTIFICATIONS) == true
                else true
            )
        }
    }
}

@Composable
private fun PermissionRow(
    label: String,
    granted: Boolean,
    onPrimaryAction: () -> Unit,
    onOpenSettings: () -> Unit,
    showSettingsHint: Boolean
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleSmall)
            Text(
                if (granted) "✅ Accordée" else "⚠️ Refusée",
                style = MaterialTheme.typography.bodySmall
            )
            if (!granted && showSettingsHint) {
                Text(
                    "Tu as probablement refusé définitivement → passe par les réglages.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!granted && !showSettingsHint) {
                OutlinedButton(onClick = onPrimaryAction) { Text("Autoriser") }
            }
            if (!granted && showSettingsHint) {
                OutlinedButton(onClick = onOpenSettings) { Text("Réglages") }
            }
        }
    }
}

private data class PermissionsState(
    val cameraGranted: Boolean,
    val notificationsGranted: Boolean
)

private fun Context.hasPermission(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PERMISSION_GRANTED

private fun Context.notificationsAllowed(): Boolean {
    // Android: même avec permission, l’utilisateur peut désactiver les notifs au niveau système
    val enabled = NotificationManagerCompat.from(this).areNotificationsEnabled()
    if (!enabled) return false

    return if (Build.VERSION.SDK_INT >= 33) {
        hasPermission(Manifest.permission.POST_NOTIFICATIONS)
    } else true
}

private fun Context.openAppSettings() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    startActivity(intent)
}

// ⚠️ shouldShowRequestPermissionRationale ne permet pas de distinguer "jamais demandé" vs "refus permanent"
// sans stocker "askedBefore". Ici on fait un heuristique simple (souvent suffisant).
private fun ComponentActivity.isPermanentlyDenied(permission: String): Boolean {
    val denied = !ContextCompat.checkSelfPermission(this, permission).equals(PERMISSION_GRANTED)
    val showRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, permission)
    return denied && !showRationale
}

private fun Context.findActivity(): ComponentActivity? {
    var ctx = this
    while (ctx is android.content.ContextWrapper) {
        if (ctx is ComponentActivity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
