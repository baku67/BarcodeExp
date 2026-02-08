package com.example.barcode.sync

sealed class SyncUiState {
    data object Idle : SyncUiState()
    data object Syncing : SyncUiState()
    data object Offline : SyncUiState()
    data class Error(val message: String) : SyncUiState()
}