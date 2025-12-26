package com.example.barcode.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SessionManager(private val context: Context) {
    private val Context.dataStore by preferencesDataStore("session")
    private val TOKEN_KEY = stringPreferencesKey("token")

    val token: Flow<String?> = context.dataStore.data.map { prefs -> prefs[TOKEN_KEY] }

    suspend fun saveToken(token: String) {
        context.dataStore.edit { prefs -> prefs[TOKEN_KEY] = token }
    }

    suspend fun clear() {
        context.dataStore.edit { prefs -> prefs.remove(TOKEN_KEY) }
    }
}