package com.example.barcode.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.*
import androidx.compose.runtime.staticCompositionLocalOf

class HeaderBarState {
    var title by mutableStateOf("")
    var subtitle by mutableStateOf<String?>(null)

    // slot d’actions
    var ownerKey by mutableStateOf<String?>(null)
    var actions by mutableStateOf<(@Composable RowScope.() -> Unit)?>(null)

    fun setActions(owner: String, block: @Composable RowScope.() -> Unit) {
        ownerKey = owner
        actions = block
    }

    fun clearActions(owner: String) {
        if (ownerKey == owner) {
            ownerKey = null
            actions = null
        }
    }

    fun clearAll() {
        ownerKey = null
        actions = null
    }
}

val LocalAppTopBarState = staticCompositionLocalOf<HeaderBarState> {
    error("LocalAppTopBarState not provided")
}
