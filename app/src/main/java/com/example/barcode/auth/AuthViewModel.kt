package com.example.barcode.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
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
                    session.saveUser(UserProfile(id = res.id, email = email))
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
}