package com.example.barcode.features.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.barcode.domain.models.ThemeMode
import com.example.barcode.domain.models.UserPreferences


@Composable
fun ThemeToggleRow(
    prefs: UserPreferences,
    onToggleDark: (Boolean) -> Unit
) {
    val isDark = prefs.theme == ThemeMode.DARK

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isDark) Icons.Filled.DarkMode else Icons.Filled.LightMode,
            contentDescription = null
        )

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text("Thème", style = MaterialTheme.typography.titleMedium)
            Text(
                if (isDark) "Sombre" else "Clair",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = isDark,
            onCheckedChange = onToggleDark
        )
    }
}

