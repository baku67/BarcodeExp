package com.example.barcode.common.bus

import androidx.compose.material3.SnackbarDuration
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object SnackbarBus {

    private val _events = MutableSharedFlow<AppSnackbarEvent>(
        extraBufferCapacity = 1
    )
    val events = _events.asSharedFlow()

    fun show(message: String) {
        _events.tryEmit(AppSnackbarEvent(message = message))
    }

    fun show(event: AppSnackbarEvent) {
        _events.tryEmit(event)
    }
}

data class AppSnackbarEvent(
    val message: String,
    val actionLabel: String? = null,
    val withDismissAction: Boolean = false,
    val duration: SnackbarDuration = SnackbarDuration.Short,
    val onAction: (() -> Unit)? = null,
)
