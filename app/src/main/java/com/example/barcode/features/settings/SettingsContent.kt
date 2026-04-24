package com.example.barcode.features.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import com.example.barcode.core.AppMode
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import com.example.barcode.common.bus.SnackbarBus
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import com.example.barcode.common.utils.SeasonRegion
import com.example.barcode.common.utils.SeasonalityResolver
import com.example.barcode.features.auth.AuthViewModel
import com.example.barcode.domain.models.UserPreferences
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    innerPadding: PaddingValues,
    authVm: AuthViewModel,
    onGoToLogin: () -> Unit,
    onGoToRegister: () -> Unit,
    isActive: Boolean
) {

    val scope = rememberCoroutineScope()

    val mode = authVm.appMode.collectAsState(initial = AppMode.AUTH).value
    val token = authVm.token.collectAsState(initial = null).value
    val cachedEmail = authVm.userEmail.collectAsState(initial = null).value
    val cachedIsVerified = authVm.userIsVerified.collectAsState(initial = null).value
    val prefs = authVm.preferences.collectAsState(initial = UserPreferences()).value

    var refreshing by rememberSaveable { mutableStateOf(false) }

    // vérifie si données déja fetch pour ce JWT, "1er chargement" todo:remplacer par 1ers chargements dans GloabLoaderScreen Splash
    var initialLoading by rememberSaveable { mutableStateOf(false) }   // ✅ loader initial dédié au premier chargement
    var loadedForToken by rememberSaveable { mutableStateOf<String?>(null) }

    var resending by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var deleting by rememberSaveable { mutableStateOf(false) }

    // TODO: remplacer le delay par vrai refresh VM/API
    suspend fun refreshProfile() {
        authVm.refreshProfile()
            .onFailure { SnackbarBus.show("Impossible de charger le profil : ${it.message ?: it}") }
    }


    // On ne refresh plus le UserProfil au premier affichage car déja fait dans le GlobalLoaderScreen
    /*    LaunchedEffect(isActive, mode, token) {
        val canLoad = isActive && mode == AppMode.AUTH && !token.isNullOrBlank()
        if (!canLoad) return@LaunchedEffect

        // auto-load 1 seule fois (par token)
        if (loadedForToken == token) return@LaunchedEffect

        initialLoading = true
        try {
            refreshProfile()
        } finally {
            initialLoading = false
            loadedForToken = token // ✅ même si échec => évite spam navigation (refresh manuel pour retenter)
        }
    }*/




    Box(Modifier.fillMaxSize()) {

        // Barre de chargement top (plus besoin car plus de refresh au premier affichage)
/*        if (initialLoading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .align(Alignment.TopCenter),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            )
        }*/

        // Contenu + PullToRefreshBox
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
                        refreshProfile()
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
                item {
                    // Bloc : Affichage du Mode actuel (LOCAL ou AUTH)
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Mode d’utilisation",
                                style = MaterialTheme.typography.titleMedium
                            )
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

                        // Si MODE AUTH (User en cache) on affiche ses infos + btn deco + btn suppr compte
                        if (mode == AppMode.AUTH) {

                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Compte", style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(6.dp))

                                Text(
                                    "Email : ${cachedEmail ?: "Chargement..."}",
                                    style = MaterialTheme.typography.bodyLarge
                                )

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
                                            authVm.logout()     // ✅ d’abord : purge DataStore
                                            onGoToLogin()       // ✅ ensuite : navigation
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
                                        onDismissRequest = {
                                            if (!deleting) showDeleteDialog = false
                                        },
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
                                                                onGoToLogin()
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

                        // Si MODE LOCAL proposition création de compte
                        if (mode == AppMode.LOCAL) {

                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Synchroniser & partager",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "Passe en mode compte pour sauvegarder en base, synchroniser entre appareils " +
                                            "et partager le frigo avec d’autres utilisateurs.",
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                Spacer(Modifier.height(12.dp))

                                OutlinedButton(
                                    onClick = onGoToRegister,
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

                // Gestion du foyer
                item {
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Foyer", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }

                // 2) Alerte si autorisations manquantes -> redirection vers section /Settings
                // Attention: utiliser areNotificationsEnabled() avant envoie de notif,
                // ET demander permissions au moment où besoin sinon peut bloquer après plusieurs refus si User voit pas l'intéret au lancement de  l'app
                item {
                    PermissionsCard()
                }

                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Saisonnalité", style = MaterialTheme.typography.titleLarge)

                            CountrySelectorRow(
                                selectedCountryCode = prefs.countryCode,
                                onCountrySelected = { code ->
                                    authVm.onCountryCodeSelected(code)
                                }
                            )

                            Text(
                                text = "Ce pays sert à déduire automatiquement la zone climatique utilisée pour les fruits et légumes de saison.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            val regionFromCountry = SeasonalityResolver.regionFromCountryCode(prefs.countryCode)
                            val effectiveRegion = SeasonalityResolver.effectiveRegion(
                                countryCode = prefs.countryCode,
                                seasonRegionOverride = prefs.seasonRegionOverride
                            )
                            val regionLabel = SeasonalityResolver.regionLabel(effectiveRegion)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Zone utilisée :",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Text(
                                    text = regionLabel,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                SeasonRegionOverrideButton(
                                    countryCode = prefs.countryCode,
                                    currentOverride = prefs.seasonRegionOverride,
                                    onOverrideSelected = { region ->
                                        authVm.onSeasonRegionOverrideSelected(region)
                                    }
                                )
                            }

                            Text(
                                text = if (prefs.seasonRegionOverride == null) {
                                    "Mode automatique : ${SeasonalityResolver.regionLabel(regionFromCountry)} est déduite du pays sélectionné."
                                } else {
                                    "Correction manuelle active."
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Toggle Theme Light/Dark
                item {
                    ElevatedCard {
                        Column(
                            Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Apparence", style = MaterialTheme.typography.titleLarge)

                            ThemeToggleRow(
                                prefs = prefs,
                                onToggleDark = { checked -> authVm.onThemeToggled(checked) }
                            )
                        }
                    }
                }

                item {
                    //  Bloc paramétrages DLC et notifs
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Paramètres de DLC",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.height(6.dp))
                            val dlcProductsCount = 0
                            Text(
                                "Nombre de produits en DLC: $dlcProductsCount",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                "Recevoir des alertes (toggle)",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                "Si toggle: Combien de jours avant (options)",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                "Si pas autorisation: need autorisation notifs",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun CountrySelectorRow(
    selectedCountryCode: String,
    onCountrySelected: (String) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    val countries = remember { SeasonalityResolver.europeanCountries() }
    val selectedLabel = remember(selectedCountryCode, countries) {
        SeasonalityResolver.countryLabel(selectedCountryCode)
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            CountryOptionRow(
                countryCode = selectedCountryCode,
                label = selectedLabel,
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodyLarge
            )

            Spacer(Modifier.width(8.dp))

            Text("Changer")
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            Column(
                modifier = Modifier
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                countries.forEach { country ->
                    DropdownMenuItem(
                        text = {
                            CountryOptionRow(
                                countryCode = country.code,
                                label = country.label,
                                textStyle = MaterialTheme.typography.bodyMedium
                            )
                        },
                        onClick = {
                            expanded = false
                            onCountrySelected(country.code)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SeasonRegionOverrideButton(
    countryCode: String,
    currentOverride: SeasonRegion?,
    onOverrideSelected: (SeasonRegion?) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    val automaticRegion = remember(countryCode) {
        SeasonalityResolver.regionFromCountryCode(countryCode)
    }

    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = "Modifier la zone",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        "Automatique (${SeasonalityResolver.regionLabel(automaticRegion)})" +
                                if (currentOverride == null) " ✓" else ""
                    )
                },
                onClick = {
                    expanded = false
                    onOverrideSelected(null)
                }
            )

            SeasonRegion.entries.forEach { region ->
                DropdownMenuItem(
                    text = {
                        Text(
                            SeasonalityResolver.regionLabel(region) +
                                    if (currentOverride == region) " ✓" else ""
                        )
                    },
                    onClick = {
                        expanded = false
                        onOverrideSelected(region)
                    }
                )
            }
        }
    }
}


@Composable
private fun CountryOptionRow(
    countryCode: String,
    label: String,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = countryCodeToFlagEmoji(countryCode),
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = label,
            style = textStyle,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun countryCodeToFlagEmoji(countryCode: String): String {
    val normalized = countryCode
        .trim()
        .uppercase(Locale.ROOT)

    if (normalized.length != 2 || normalized.any { !it.isLetter() }) {
        return "🏳️"
    }

    val firstCodePoint = normalized[0].code - 'A'.code + 0x1F1E6
    val secondCodePoint = normalized[1].code - 'A'.code + 0x1F1E6

    return String(Character.toChars(firstCodePoint)) +
            String(Character.toChars(secondCodePoint))
}
