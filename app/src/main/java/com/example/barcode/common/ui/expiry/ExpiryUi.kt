package com.example.barcode.common.ui.expiry

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.barcode.common.expiry.ExpiryLevel
import com.example.barcode.common.expiry.ExpiryPolicy
import com.example.barcode.common.expiry.expiryLevel

val ExpiryWarning = Color(0xFFFFC107)

@Composable
fun expiryAccentColor(level: ExpiryLevel): Color {
    val cs = MaterialTheme.colorScheme
    return when (level) {
        ExpiryLevel.NONE -> cs.outlineVariant
        ExpiryLevel.EXPIRED -> cs.tertiary      // ✅ comme tu veux
        ExpiryLevel.SOON -> ExpiryWarning
        ExpiryLevel.OK -> cs.primary
    }
}

@Composable
fun expiryGlowColor(level: ExpiryLevel): Color? =
    when (level) {
        ExpiryLevel.EXPIRED, ExpiryLevel.SOON -> expiryAccentColor(level)
        else -> null
    }

@Composable
fun expirySelectionBorderColor(level: ExpiryLevel): Color {
    val cs = MaterialTheme.colorScheme
    val base = when (level) {
        ExpiryLevel.NONE -> cs.primary
        else -> expiryAccentColor(level)
    }
    return base.copy(alpha = 0.95f)
}

@Composable
fun expiryStrokeColor(expiryMillis: Long?, policy: ExpiryPolicy = ExpiryPolicy()): Color {
    val level = expiryLevel(expiryMillis, policy)
    val base = expiryAccentColor(level)

    return when (level) {
        ExpiryLevel.NONE -> base.copy(alpha = 0.35f)
        ExpiryLevel.EXPIRED -> base.copy(alpha = 0.65f)
        ExpiryLevel.SOON -> base.copy(alpha = 0.55f)
        ExpiryLevel.OK -> base.copy(alpha = 0.55f)
    }
}
