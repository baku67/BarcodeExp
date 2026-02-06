package com.example.barcode.common.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.barcode.core.session.SessionManager
import com.example.barcode.domain.models.ThemeMode
import com.example.barcode.domain.models.UserPreferences

// Pour que AppBackground puisse connaître le thème RÉEL (y compris override user)
val LocalIsDarkTheme = staticCompositionLocalOf { true }

private val DarkColorScheme = darkColorScheme(
    primary = AppPrimary,
    tertiary = AppRed,
    onPrimary = Color(0xFF0B1220), // Text dans bouton primary
    background = Color(0xFF0B1220), // inutile car AppBackground au dessus ?
    surface = Color(0x9C121418), // Bleu anthracite surface/cards UN PEU TRANSPARENT
    // JAUNE/expiré: Color(0xFFF9A825)
    onBackground = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = AppPrimary,
    tertiary = AppRed,
    onPrimary = Color(0xFFF7FBFF), // Text dans bouton primary
    background = Color(0xFFF7FBFF), // inutile car AppBackground au dessus ?
    surface = Color.White, // itemsCard par exemple
    // JAUNE/expiré: Color(0xFFF9A825)
    onBackground = Color.Black
)

@Composable
fun Theme(
    session: SessionManager,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val prefs by session.preferences.collectAsState(initial = UserPreferences())

    val isDark = when (prefs.theme) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val baseScheme: ColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> DarkColorScheme
        else -> LightColorScheme
    }

    // ✅ Verrouille AppPrimary quoi qu’il arrive (même si dynamicColor=true)
    val scheme = baseScheme.copy(primary = AppPrimary)

    CompositionLocalProvider(LocalIsDarkTheme provides isDark) {
        MaterialTheme(
            colorScheme = scheme,
            typography = AppTypography,
            content = content
        )
    }
}
