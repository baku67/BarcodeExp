package com.example.barcode.auth

// Etat réseau uniquement
data class AuthUiState(
    val loading: Boolean = false,
    val authenticated: Boolean = false,
    val error: String? = null
) {
}