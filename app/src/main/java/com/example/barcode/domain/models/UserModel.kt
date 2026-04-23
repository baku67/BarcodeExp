package com.example.barcode.domain.models

import com.example.barcode.common.utils.SeasonalityResolver
import com.google.gson.annotations.SerializedName

data class LoginRequest(val email: String, val password: String)
data class RegisterRequest(val email: String, val password: String, val confirmPassword: String)

data class LoginResponse(
    val token: String,
    @SerializedName("refresh_token") val refreshToken: String? = null
)

data class RegisterResponse(
    val id: String
)

data class UserProfile(
    val id: String,
    val email: String,
    val roles: List<String> = emptyList(),
    val isVerified: Boolean,
    val currentHomeId: String?,
    val preferences: UserPreferencesDto? = null,
    val preferencesUpdatedAt: Long? = null
)

data class UserPreferencesDto(
    val theme: String? = null,
    val lang: String? = null,
    @SerializedName("frigo_layout") val frigoLayout: String? = null,
    @SerializedName("country_code") val countryCode: String? = null
)

data class UserPreferences(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val lang: String = "fr",
    val frigoLayout: FrigoLayout = FrigoLayout.LIST,
    val countryCode: String = SeasonalityResolver.defaultCountryCode(),
    val updatedAtEpochSec: Long? = null
)

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class FrigoLayout { LIST, DESIGN }

// ---------------------------------------------------------

fun UserProfile.toUserPreferences(): UserPreferences {
    val dto = preferences

    val theme = when (dto?.theme?.lowercase()) {
        "light" -> ThemeMode.LIGHT
        "dark" -> ThemeMode.DARK
        else -> ThemeMode.SYSTEM
    }

    val layout = when (dto?.frigoLayout?.lowercase()) {
        "design" -> FrigoLayout.DESIGN
        else -> FrigoLayout.LIST
    }

    return UserPreferences(
        theme = theme,
        lang = dto?.lang ?: "fr",
        frigoLayout = layout,
        countryCode = SeasonalityResolver.normalizeCountryCodeOrDefault(dto?.countryCode),
        updatedAtEpochSec = preferencesUpdatedAt
    )
}