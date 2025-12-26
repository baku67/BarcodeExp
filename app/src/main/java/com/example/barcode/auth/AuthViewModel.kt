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
}