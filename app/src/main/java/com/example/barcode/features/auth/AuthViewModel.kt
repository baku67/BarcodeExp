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

data class AuthUiState(
    val loading: Boolean = false,
    val error: String? = null
)

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

    sealed interface AuthEvent {
        data object GoHome : AuthEvent
        data object GoHomeLocal : AuthEvent
    }

    private val _events = Channel<AuthEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val patchFlow = MutableSharedFlow<Map<String, String>>(extraBufferCapacity = 1)
    private var loginJob: Job? = null

    init {
        viewModelScope.launch {
            patchFlow
                .debounce(500)
                .distinctUntilChanged()
                .collectLatest { body ->
                    val token = session.token.first() ?: return@collectLatest
                    repo.patchPreferences(token, body)
                        .onFailure {
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
        if (uiState.value.loading) return

        loginJob?.cancel()
        loginJob = viewModelScope.launch {
            uiState.value = AuthUiState(loading = true, error = null)

            session.saveToken("")
            runCatching { session.saveRefreshToken("") }

            val result = repo.login(email.trim(), password)

            result
                .onSuccess { res ->
                    session.saveToken(res.token)
                    res.refreshToken?.let { session.saveRefreshToken(it) }

                    session.setAppMode(AppMode.AUTH)
                    uiState.value = AuthUiState(loading = false, error = null)
                    _events.trySend(AuthEvent.GoHome)

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
        if (password != confirmPassword) {
            uiState.value = uiState.value.copy(error = "Les mots de passe ne correspondent pas")
            return
        }

        viewModelScope.launch {
            uiState.value = uiState.value.copy(loading = true, error = null)

            repo.register(email, password, confirmPassword)
                .onSuccess { reg ->
                    repo.login(email.trim(), password)
                        .onSuccess { login ->
                            session.saveToken(login.token)
                            login.refreshToken?.let { session.saveRefreshToken(it) }
                            session.setAppMode(AppMode.AUTH)

                            _events.trySend(AuthEvent.GoHome)

                            viewModelScope.launch {
                                repo.me(login.token)
                                    .onSuccess { profile ->
                                        session.saveUser(profile)
                                        session.savePreferences(profile.toUserPreferences())
                                    }
                                    .onFailure {
                                        session.saveUser(
                                            UserProfile(
                                                id = reg.id,
                                                email = email,
                                                roles = emptyList(),
                                                isVerified = false,
                                                currentHomeId = null,
                                                preferences = null,
                                                preferencesUpdatedAt = null
                                            )
                                        )
                                    }
                            }
                        }
                        .onFailure { err ->
                            uiState.value = uiState.value.copy(
                                loading = false,
                                error = "Compte créé (id=${reg.id}) mais login auto KO: ${err.message ?: err}"
                            )
                        }
                }
                .onFailure { err ->
                    uiState.value = uiState.value.copy(
                        error = err.message ?: "Erreur",
                        loading = false
                    )
                }

            uiState.value = uiState.value.copy(loading = false)
        }
    }

    fun onUseLocalMode() {
        viewModelScope.launch {
            session.logout()
            _events.trySend(AuthEvent.GoHomeLocal)
        }
    }

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
            emitPrefsPatch(theme = newTheme)
        }
    }

    private suspend fun emitPrefsPatch(
        theme: ThemeMode? = null,
        frigoLayout: FrigoLayout? = null
    ) {
        val body = buildMap<String, String> {
            theme?.let { put("theme", it.name.lowercase()) }
            frigoLayout?.let { put("frigo_layout", it.name.lowercase()) }
        }

        if (body.isEmpty()) return

        if (session.appMode.first() == AppMode.AUTH) {
            patchFlow.emit(body)
        }
    }
}