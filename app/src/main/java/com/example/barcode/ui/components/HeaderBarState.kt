package com.example.barcode.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.*
import androidx.compose.runtime.staticCompositionLocalOf

class HeaderBarState {
    var title by mutableStateOf("")
    var subtitle by mutableStateOf<String?>(null)

    // slot d’actions
    var actions by mutableStateOf<(@Composable RowScope.() -> Unit)?>(null)

    fun clearActions() {
        actions = null
    }
}

val LocalAppTopBarState = staticCompositionLocalOf<HeaderBarState> {
    error("LocalAppTopBarState not provided")
}
