package com.example.barcode.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.barcode.core.SessionManager

class AuthViewModelFactory(
    private val repo: AuthRepository,
    private val session: SessionManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(repo, session) as T
        }
        throw IllegalArgumentException("Unknown ViewModel: $modelClass")
    }
}
