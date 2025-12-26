package com.example.barcode.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// ✅ TOP-LEVEL: un seul DataStore par process
private val Context.dataStore by preferencesDataStore(name = "session")

private val TOKEN_KEY = stringPreferencesKey("token")

private val APP_MODE_KEY = stringPreferencesKey("app_mode")
enum class AppMode { LOCAL, AUTH }


class SessionManager(private val context: Context) {

    val token: Flow<String?> = context.dataStore.data.map { prefs -> prefs[TOKEN_KEY] }

    val appMode: Flow<AppMode> = context.dataStore.data.map { prefs ->
        when (prefs[APP_MODE_KEY]) {
            AppMode.LOCAL.name -> AppMode.LOCAL
            else -> AppMode.AUTH // default
        }
    }

    suspend fun setAppMode(mode: AppMode) {
        context.dataStore.edit { prefs -> prefs[APP_MODE_KEY] = mode.name }
    }

    suspend fun saveToken(token: String) {
        context.dataStore.edit { prefs -> prefs[TOKEN_KEY] = token }
    }

    suspend fun clear() {
        context.dataStore.edit { prefs -> prefs.remove(TOKEN_KEY) }
    }
    suspend fun logout() {
        context.dataStore.edit { prefs ->
            prefs.remove(TOKEN_KEY)
            // en général, tu repasses en AUTH pour forcer login
            prefs[APP_MODE_KEY] = AppMode.AUTH.name
        }
    }
}
