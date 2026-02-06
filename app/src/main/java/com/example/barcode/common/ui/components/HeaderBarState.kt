package com.example.barcode.common.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.runtime.*
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.barcode.domain.models.AppIcon

class HeaderBarState {
    var title by mutableStateOf("")
    var subtitle by mutableStateOf<String?>(null)

    var icon by mutableStateOf<AppIcon?>(AppIcon.Vector(Icons.Filled.Home))

    // slot d’actions
    var ownerKey by mutableStateOf<String?>(null)
    var actions by mutableStateOf<(@Composable RowScope.() -> Unit)?>(null)

    // slot près du titre (ex: Help)
    var titleTrailing by mutableStateOf<(@Composable () -> Unit)?>(null)

    fun setTitleTrailing(owner: String, block: @Composable () -> Unit) {
        ownerKey = owner
        titleTrailing = block
    }

    fun clearTitleTrailing(owner: String) {
        if (ownerKey == owner) titleTrailing = null
    }

    fun setActions(owner: String, block: @Composable RowScope.() -> Unit) {
        ownerKey = owner
        actions = block
    }

    fun clearActions(owner: String) {
        if (ownerKey == owner) {
            ownerKey = null
            actions = null
            titleTrailing = null // ✅
        }
    }

    fun clearAll() {
        ownerKey = null
        actions = null
        titleTrailing = null // ✅
    }
}

val LocalAppTopBarState = staticCompositionLocalOf<HeaderBarState> {
    error("LocalAppTopBarState not provided")
}
