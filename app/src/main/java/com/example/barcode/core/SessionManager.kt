package com.example.barcode.core

import android.content.Context
import com.example.barcode.domain.models.FrigoLayout
import com.example.barcode.domain.models.ThemeMode
import com.example.barcode.domain.models.UserPreferences
import com.example.barcode.domain.models.UserProfile
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

internal const val SESSION_STORE_NAME = "session_store"
internal val Context.sessionDataStore by preferencesDataStore(name = SESSION_STORE_NAME)

class SessionManager(context: Context) {

    private val auth = AuthStore(context)
    private val prefs = UserPreferencesStore(context)

    // --- Auth ---
    val token = auth.token
    val refreshToken = auth.refreshToken
    val appMode = auth.appMode
    val userId = auth.userId
    val userEmail = auth.userEmail
    val userIsVerified = auth.userIsVerified

    suspend fun setAppMode(mode: AppMode) = auth.setAppMode(mode)
    suspend fun saveToken(token: String) = auth.saveToken(token)
    suspend fun saveRefreshToken(token: String) = auth.saveRefreshToken(token)
    suspend fun saveUser(profile: UserProfile) = auth.saveUser(profile)

    suspend fun clear() = auth.clearTokensOnly()
    suspend fun logout() = withContext(NonCancellable) {
        auth.logout()
    }

    // --- Preferences ---
    val preferences = prefs.preferences
    suspend fun savePreferences(p: UserPreferences) = prefs.savePreferences(p)
    suspend fun clearPreferences() = prefs.clearPreferences()
    suspend fun setTheme(theme: ThemeMode) = prefs.setTheme(theme)
    suspend fun setLang(lang: String) = prefs.setLang(lang)
    suspend fun setFrigoLayout(layout: FrigoLayout) = prefs.setFrigoLayout(layout)
    suspend fun isAuthenticated(): Boolean {
        val modeOk = appMode.first() == AppMode.AUTH
        val tokenOk = !token.first().isNullOrBlank()
        return modeOk && tokenOk
    }
}
