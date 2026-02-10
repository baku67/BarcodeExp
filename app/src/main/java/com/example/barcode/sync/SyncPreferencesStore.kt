package com.example.barcode.sync


import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.syncDataStore by preferencesDataStore(name = "sync_prefs")

class SyncPreferences(private val context: Context) {

    companion object {
        // ✅ maintenant c’est un watermark serveur (epoch ms), pas juste "now()"
        private val KEY_LAST_SUCCESS_AT = longPreferencesKey("last_success_sync_at")
        private val KEY_AUTH_REQUIRED = booleanPreferencesKey("auth_required")
    }

    // ✅ plus de Long? => plus d'erreur
    val lastSuccessAt: Flow<Long> =
        context.syncDataStore.data.map { it[KEY_LAST_SUCCESS_AT] ?: 0L }

    val authRequired: Flow<Boolean> =
        context.syncDataStore.data.map { it[KEY_AUTH_REQUIRED] ?: false }

    suspend fun markSyncSuccessAt(serverWatermarkMs: Long) {
        context.syncDataStore.edit {
            it[KEY_LAST_SUCCESS_AT] = serverWatermarkMs
            it[KEY_AUTH_REQUIRED] = false
        }
    }

    // (optionnel) garde ton ancienne API
    suspend fun markSyncSuccessNow() {
        markSyncSuccessAt(System.currentTimeMillis())
    }

    suspend fun markAuthRequired() {
        context.syncDataStore.edit { it[KEY_AUTH_REQUIRED] = true }
    }

    suspend fun clearAuthRequired() {
        context.syncDataStore.edit { it[KEY_AUTH_REQUIRED] = false }
    }
}