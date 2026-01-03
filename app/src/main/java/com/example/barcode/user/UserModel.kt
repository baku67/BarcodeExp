package com.example.barcode.user

import com.google.gson.annotations.SerializedName

data class LoginRequest(val email: String, val password: String)
data class LoginResponse(val token: String)

data class RegisterRequest(val email: String, val password: String, val confirmPassword: String)
data class RegisterResponse(val id: String, val token: String)

data class UserProfile(
    val id: String,
    val email: String,
    val roles: List<String> = emptyList(),
    val isVerified: Boolean,
    val preferences: UserPreferencesDto? = null,
    val preferencesUpdatedAt: Long? = null
)
data class UserPreferencesDto(
    val theme: String? = null,
    val lang: String? = null,
    @SerializedName("frigo_layout") val frigoLayout: String? = null
)

data class UserPreferences(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val lang: String = "fr",
    val frigoLayout: FrigoLayout = FrigoLayout.LIST,
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
        updatedAtEpochSec = preferencesUpdatedAt
    )
}