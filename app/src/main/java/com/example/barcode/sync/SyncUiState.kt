package com.example.barcode.sync

sealed class SyncUiState {
    data class UpToDate(val lastSuccessAt: Long?) : SyncUiState()
    data object Syncing : SyncUiState()
    data object Offline : SyncUiState() // Pas de connexion internet
    data object AuthRequired : SyncUiState() // Unauthorized / SessionExpired / AuthRequired (à la place de logout direct?)
    data class Error(val message: String) : SyncUiState() // Réseau OK mais timeoutou erreuyr 500x: Afficher le message d'erreur dans la barre ?
}