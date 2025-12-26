package com.example.barcode.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
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
                    uiState.value = uiState.value.copy(authenticated = true, loading = false)
                }
                .onFailure { err ->
                    uiState.value = uiState.value.copy(error = err.message, loading = false)
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
                    session.saveToken(res.token)
                    uiState.value = uiState.value.copy(authenticated = true, loading = false)
                }
                .onFailure { err ->
                    uiState.value = uiState.value.copy(error = err.message ?: "Erreur", loading = false)
                }
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