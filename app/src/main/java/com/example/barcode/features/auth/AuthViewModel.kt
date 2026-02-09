package com.example.barcode.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.barcode.features.fridge.ViewMode
import com.example.barcode.common.bus.SnackbarBus
import com.example.barcode.core.AppMode
import com.example.barcode.core.SessionManager
import com.example.barcode.domain.models.FrigoLayout
import com.example.barcode.domain.models.ThemeMode
import com.example.barcode.domain.models.UserPreferences
import com.example.barcode.domain.models.UserProfile
import com.example.barcode.domain.models.toUserPreferences
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

// Etat réseau uniquement
data class AuthUiState(
    val loading: Boolean = false,
    val error: String? = null
) {}



class AuthViewModel(
    private val repo: AuthRepository,
    private val session: SessionManager
) : ViewModel() {

    val uiState = MutableStateFlow(AuthUiState())

    val appMode: Flow<AppMode> = session.appMode
    val token: Flow<String?> = session.token
    val userEmail: Flow<String?> = session.userEmail
    val userIsVerified: Flow<Boolean?> = session.userIsVerified
    val preferences: Flow<UserPreferences> = session.preferences

    // Pour auth auto après register
    sealed interface AuthEvent {
        data object GoHome : AuthEvent
    }
    private val _events = Channel<AuthEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()
    private val patchFlow = MutableSharedFlow<Map<String, String>>(extraBufferCapacity = 1)

    private var loginJob: Job? = null


    // Patch des préférences en BDD au lancement ?
    init {
        viewModelScope.launch {
            patchFlow
                .debounce(500)              // ✅ évite spam
                .distinctUntilChanged()     // ✅ évite PATCH identiques
                .collectLatest { body ->    // ✅ annule l’ancien si nouveau arrive
                    val token = session.token.first() ?: return@collectLatest
                    repo.patchPreferences(token, body)
                        .onFailure {
                            // IMPORTANT : tu ne reverts pas l'UI.
                            // Option : afficher un snackbar + marquer "pending sync"
                            SnackbarBus.show("Sync préférences impossible : ${it.message ?: it}")
                        }
                }
        }
    }

    suspend fun refreshProfile(): Result<Unit> {
        val mode = session.appMode.first()
        val t = session.token.first()
        if (mode != AppMode.AUTH || t.isNullOrBlank()) return Result.success(Unit)

        return repo.me(t).fold(
            onSuccess = { profile ->
                session.saveUser(profile)
                session.savePreferences(profile.toUserPreferences())
                Result.success(Unit)
            },
            onFailure = { Result.failure(it) }
        )
    }

    suspend fun resendVerifyEmail(): Result<Unit> {
        val t = session.token.first()
        if (t.isNullOrBlank()) return Result.failure(Exception("Token manquant"))
        return repo.resendVerifyEmail(t)
    }

    suspend fun deleteAccount(): Result<Unit> {
        val t = session.token.first()
        if (t.isNullOrBlank()) return Result.failure(Exception("Token manquant"))
        return repo.deleteMe(t)
    }

    suspend fun logout() {
        session.logout()
    }

    fun onLogin(email: String, password: String) {
        // évite double taps / auto-login + bouton
        if (uiState.value.loading) return

        loginJob?.cancel()
        loginJob = viewModelScope.launch {
            uiState.value = AuthUiState(loading = true, error = null)

            // IMPORTANT : évite de te faire croire qu’un vieux token = login OK
            session.saveToken("")
            runCatching { session.saveRefreshToken("") }

            val result = repo.login(email.trim(), password)

            result
                .onSuccess { res ->
                    session.saveToken(res.token)
                    res.refreshToken?.let { session.saveRefreshToken(it) }

                    // ✅ mets le mode + navigation tout de suite
                    session.setAppMode(AppMode.AUTH)
                    uiState.value = AuthUiState(loading = false, error = null)
                    _events.trySend(AuthEvent.GoHome)

                    // ✅ /me en background (ne bloque pas l’UI)
                    viewModelScope.launch {
                        repo.me(res.token)
                            .onSuccess { profile ->
                                session.saveUser(profile)
                                session.savePreferences(profile.toUserPreferences())
                            }
                            .onFailure {
                                SnackbarBus.show("Profil non chargé : ${it.message ?: it}")
                            }
                    }
                }
                .onFailure { err ->
                    val msg = buildString {
                        append(err::class.simpleName ?: "Erreur")
                        err.message?.let { append(": $it") }
                    }
                    uiState.value = AuthUiState(loading = false, error = msg)
                }
        }
    }


    fun onRegister(email: String, password: String, confirmPassword: String) {
        // petit garde-fou front (évite un call inutile)
        if (password != confirmPassword) {
            uiState.value = uiState.value.copy(error = "Les mots de passe ne correspondent pas")
            return
        }

        viewModelScope.launch {
            uiState.value = uiState.value.copy(loading = true, error = null)

            repo.register(email, password, confirmPassword)
                .onSuccess { res ->
                    // res.token vient de /auth/register
                    session.saveToken(res.token)
                    res.refreshToken?.let { session.saveRefreshToken(it) }
                    session.saveUser(UserProfile(id = res.id, email = email, isVerified = false))
                    session.setAppMode(AppMode.AUTH)

                    _events.send(AuthEvent.GoHome)
                }
                .onFailure { err ->
                    uiState.value = uiState.value.copy(error = err.message ?: "Erreur", loading = false)
                }

            uiState.value = uiState.value.copy(loading = false)
        }
    }


    fun onUseLocalMode() {
        viewModelScope.launch {
            session.setAppMode(AppMode.LOCAL)
            session.clear() // au cas où un ancien token traîne
        }
    }













    // CACHES DES PREFERENCES UTILISATEUR (Theme, displayFridge, lang)

    fun onFridgeDisplaySelected(mode: ViewMode) {
        viewModelScope.launch {
            val current = session.preferences.first().frigoLayout
            val next = when (mode) {
                ViewMode.List -> FrigoLayout.LIST
                ViewMode.Fridge -> FrigoLayout.DESIGN
            }
            if (current == next) return@launch

            session.setFrigoLayout(next)
            emitPrefsPatch(frigoLayout = next)
        }
    }

    fun onThemeToggled(isDark: Boolean) {
        viewModelScope.launch {
            val newTheme = if (isDark) ThemeMode.DARK else ThemeMode.LIGHT
            session.setTheme(newTheme)

            // ✅ en AUTH ça partira dans patchFlow (debounce), en LOCAL ça ne fera rien
            emitPrefsPatch(theme = newTheme)
        }
    }

    private suspend fun emitPrefsPatch(
        theme: ThemeMode? = null,
        frigoLayout: FrigoLayout? = null
    ) {
        val mode = session.appMode.first()
        val token = session.token.first()

        if (mode != AppMode.AUTH || token.isNullOrBlank()) return

        val body = mutableMapOf<String, String>()

        theme?.let {
            body["theme"] = when (it) {
                ThemeMode.DARK -> "dark"
                ThemeMode.LIGHT -> "light"
                ThemeMode.SYSTEM -> "system"
            }
        }

        frigoLayout?.let {
            body["frigo_layout"] = when (it) {
                FrigoLayout.LIST -> "list"
                FrigoLayout.DESIGN -> "design"
            }
        }

        if (body.isNotEmpty()) patchFlow.tryEmit(body)
    }
}