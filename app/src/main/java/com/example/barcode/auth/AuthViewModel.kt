package com.example.barcode.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.barcode.ui.components.SnackbarBus
import com.example.barcode.user.ThemeMode
import com.example.barcode.user.UserProfile
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AuthViewModel(
    private val repo: AuthRepository,
    private val session: SessionManager
) : ViewModel() {

    val uiState = MutableStateFlow(AuthUiState())

    fun onLogin(email: String, password: String) {
        viewModelScope.launch {
            uiState.value = uiState.value.copy(loading = true)
            repo.login(email, password)
                .onSuccess { res ->
                    session.saveToken(res.token)
                    repo.me(res.token).onSuccess { profile ->
                        session.saveUser(profile)
                    }
                    session.setAppMode(AppMode.AUTH)
                    _events.emit(AuthEvent.GoHome)
                }
                .onFailure { err ->
                    uiState.value = uiState.value.copy(error = err.message, loading = false)
                }
        }
    }

    // Pour auth auto après register
    sealed interface AuthEvent {
        data object GoHome : AuthEvent
    }
    private val _events = MutableSharedFlow<AuthEvent>()
    val events = _events.asSharedFlow()

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
                    session.saveUser(UserProfile(id = res.id, email = email, isVerified = false))
                    session.setAppMode(AppMode.AUTH)

                    _events.emit(AuthEvent.GoHome)
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
            uiState.value = uiState.value.copy(authenticated = true, loading = false, error = null)
        }
    }



    fun onThemeToggled(isDark: Boolean) {
        val newTheme = if (isDark) ThemeMode.DARK else ThemeMode.LIGHT

        viewModelScope.launch {
            // 1) Toujours appliquer localement (UI instant)
            session.setTheme(newTheme)

            // 2) Sync backend seulement en AUTH
            val mode = session.appMode.first()
            val token = session.token.first()

            if (mode != AppMode.AUTH || token.isNullOrBlank()) return@launch

            repo.patchPreferences(
                token = token,
                body = mapOf("theme" to if (isDark) "dark" else "light")
            )
            // Option UX : snackbar si échec
            // .onFailure { ... }
        }
    }


    private val patchFlow = MutableSharedFlow<Map<String, String>>(extraBufferCapacity = 1)

    private suspend fun emitPrefsPatch(theme: ThemeMode? = null) {
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

        if (body.isNotEmpty()) patchFlow.tryEmit(body)
    }

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
}