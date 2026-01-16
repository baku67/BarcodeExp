package com.example.barcode.core

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.barcode.domain.models.FrigoLayout
import com.example.barcode.domain.models.ThemeMode
import com.example.barcode.domain.models.UserPreferences
import com.example.barcode.domain.models.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// ----------------------------------------------------------
// APP_MODE, TOKEN, User(id, mail, isVerified, preferences)

// ✅ TOP-LEVEL: un seul DataStore par process
private const val STORE_NAME = "session_store"
private val Context.sessionDataStore by preferencesDataStore(name = STORE_NAME)


private val TOKEN_KEY = stringPreferencesKey("token")
private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")

private val APP_MODE_KEY = stringPreferencesKey("app_mode")
enum class AppMode { LOCAL, AUTH }

// Mise en cache de l'utilisateur en cours
private val USER_ID_KEY = stringPreferencesKey("user_id")
private val USER_EMAIL_KEY = stringPreferencesKey("user_email")
private val USER_IS_VERIFIED_KEY = booleanPreferencesKey("user_is_verified")

// Cache des préférences user
private val PREF_THEME = stringPreferencesKey("pref_theme")               // "system|light|dark"
private val PREF_LANG = stringPreferencesKey("pref_lang")                 // "fr"
private val PREF_FRIGO_LAYOUT = stringPreferencesKey("pref_frigo_layout") // "list|design"
private val PREF_UPDATED_AT = longPreferencesKey("pref_updated_at")       // epoch seconds


class SessionManager(private val context: Context) {

    val token: Flow<String?> = context.sessionDataStore.data.map { prefs -> prefs[TOKEN_KEY] }
    val refreshToken: Flow<String?> = context.sessionDataStore.data.map { prefs -> prefs[REFRESH_TOKEN_KEY] }

    val appMode: Flow<AppMode> = context.sessionDataStore.data.map { prefs ->
        when (prefs[APP_MODE_KEY]) {
            AppMode.LOCAL.name -> AppMode.LOCAL
            else -> AppMode.AUTH // default
        }
    }

    val userId: Flow<String?> = context.sessionDataStore.data.map { it[USER_ID_KEY] }
    val userEmail: Flow<String?> = context.sessionDataStore.data.map { it[USER_EMAIL_KEY] }
    val userIsVerified: Flow<Boolean?> = context.sessionDataStore.data.map { it[USER_IS_VERIFIED_KEY] }

    suspend fun setAppMode(mode: AppMode) {
        context.sessionDataStore.edit { prefs -> prefs[APP_MODE_KEY] = mode.name }
    }

    suspend fun saveToken(token: String) {
        context.sessionDataStore.edit { prefs -> prefs[TOKEN_KEY] = token }
    }

    suspend fun saveRefreshToken(token: String) { // ✅
        context.sessionDataStore.edit { prefs -> prefs[REFRESH_TOKEN_KEY] = token }
    }

    suspend fun saveUser(profile: UserProfile) {
        context.sessionDataStore.edit { prefs ->
            prefs[USER_ID_KEY] = profile.id
            prefs[USER_EMAIL_KEY] = profile.email
            prefs[USER_IS_VERIFIED_KEY] = profile.isVerified
        }
    }

    suspend fun clear() {
        context.sessionDataStore.edit {
            prefs -> prefs.remove(TOKEN_KEY)
            prefs.remove(REFRESH_TOKEN_KEY)
        }

    }
    suspend fun logout() {
        context.sessionDataStore.edit { prefs ->
            prefs.remove(TOKEN_KEY)
            prefs.remove(REFRESH_TOKEN_KEY)
            prefs.remove(USER_ID_KEY)
            prefs.remove(USER_EMAIL_KEY)
            prefs.remove(USER_IS_VERIFIED_KEY)
            // en général, tu repasses en AUTH pour forcer login
            prefs[APP_MODE_KEY] = AppMode.AUTH.name
        }
    }

    val preferences: Flow<UserPreferences> = context.sessionDataStore.data.map { p ->
        val theme = when (p[PREF_THEME]) {
            "light" -> ThemeMode.LIGHT
            "dark" -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
        val layout = when (p[PREF_FRIGO_LAYOUT]) {
            "list" -> FrigoLayout.LIST
            "design" -> FrigoLayout.DESIGN
            else -> FrigoLayout.LIST
        }
        UserPreferences(
            theme = theme,
            lang = p[PREF_LANG] ?: "fr",
            frigoLayout = layout,
            updatedAtEpochSec = p[PREF_UPDATED_AT]
        )
    }

    // Pour sync backend
    suspend fun savePreferences(prefs: UserPreferences) {
        context.sessionDataStore.edit { p ->
            p[PREF_THEME] = when (prefs.theme) {
                ThemeMode.LIGHT -> "light"
                ThemeMode.DARK -> "dark"
                ThemeMode.SYSTEM -> "system"
            }

            p[PREF_LANG] = prefs.lang

            p[PREF_FRIGO_LAYOUT] = when (prefs.frigoLayout) {
                FrigoLayout.LIST -> "list"
                FrigoLayout.DESIGN -> "design"
            }

            // si ça vient du serveur, garde la date serveur ; sinon maintenant
            p[PREF_UPDATED_AT] = prefs.updatedAtEpochSec ?: (System.currentTimeMillis() / 1000)
        }
    }

    suspend fun clearPreferences() {
        context.sessionDataStore.edit { p ->
            p.remove(PREF_THEME)
            p.remove(PREF_LANG)
            p.remove(PREF_FRIGO_LAYOUT)
            p.remove(PREF_UPDATED_AT)
        }
    }

    // Au niveau UI
    suspend fun setTheme(theme: ThemeMode) {
        context.sessionDataStore.edit {
            it[PREF_THEME] = when (theme) {
                ThemeMode.LIGHT -> "light"
                ThemeMode.DARK -> "dark"
                ThemeMode.SYSTEM -> "system"
            }
            it[PREF_UPDATED_AT] = System.currentTimeMillis() / 1000
        }
    }
    suspend fun setLang(lang: String) {
        context.sessionDataStore.edit {
            it[PREF_LANG] = lang
            it[PREF_UPDATED_AT] = System.currentTimeMillis() / 1000
        }
    }
    suspend fun setFrigoLayout(layout: FrigoLayout) {
        context.sessionDataStore.edit {
            it[PREF_FRIGO_LAYOUT] = when (layout) {
                FrigoLayout.LIST -> "list"
                FrigoLayout.DESIGN -> "design"
            }
            it[PREF_UPDATED_AT] = System.currentTimeMillis() / 1000
        }
    }
}
