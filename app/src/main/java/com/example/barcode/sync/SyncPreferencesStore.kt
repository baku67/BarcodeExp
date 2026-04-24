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
        private val KEY_LAST_SUCCESS_AT = longPreferencesKey("last_success_sync_at")
        private val KEY_LAST_SYNC_FINISHED_AT = longPreferencesKey("last_sync_finished_at")
        private val KEY_AUTH_REQUIRED = booleanPreferencesKey("auth_required")

        private val KEY_WIDGET_FORCE_SYNC_RUNNING =
            booleanPreferencesKey("widget_force_sync_running")

        private val KEY_WIDGET_FORCE_SYNC_STARTED_AT =
            longPreferencesKey("widget_force_sync_started_at")

        private const val WIDGET_FORCE_SYNC_MAX_DURATION_MS = 10 * 60 * 1000L
    }

    val lastSuccessAt: Flow<Long> =
        context.syncDataStore.data.map { it[KEY_LAST_SUCCESS_AT] ?: 0L }

    val lastSyncFinishedAt: Flow<Long> =
        context.syncDataStore.data.map { it[KEY_LAST_SYNC_FINISHED_AT] ?: 0L }

    val authRequired: Flow<Boolean> =
        context.syncDataStore.data.map { it[KEY_AUTH_REQUIRED] ?: false }

    val isWidgetForceSyncRunning: Flow<Boolean> =
        context.syncDataStore.data.map { prefs ->
            val running = prefs[KEY_WIDGET_FORCE_SYNC_RUNNING] ?: false
            val startedAt = prefs[KEY_WIDGET_FORCE_SYNC_STARTED_AT] ?: 0L

            val isStale = startedAt > 0L &&
                    System.currentTimeMillis() - startedAt > WIDGET_FORCE_SYNC_MAX_DURATION_MS

            running && !isStale
        }

    suspend fun markSyncSuccessAt(serverWatermarkMs: Long) {
        context.syncDataStore.edit {
            it[KEY_LAST_SUCCESS_AT] = serverWatermarkMs
            it[KEY_LAST_SYNC_FINISHED_AT] = System.currentTimeMillis()
            it[KEY_AUTH_REQUIRED] = false
        }
    }

    suspend fun markAuthRequired() {
        context.syncDataStore.edit { it[KEY_AUTH_REQUIRED] = true }
    }

    suspend fun clearAuthRequired() {
        context.syncDataStore.edit { it[KEY_AUTH_REQUIRED] = false }
    }

    suspend fun markWidgetForceSyncStarted() {
        context.syncDataStore.edit {
            it[KEY_WIDGET_FORCE_SYNC_RUNNING] = true
            it[KEY_WIDGET_FORCE_SYNC_STARTED_AT] = System.currentTimeMillis()
        }
    }

    suspend fun markWidgetForceSyncFinished() {
        context.syncDataStore.edit {
            it[KEY_WIDGET_FORCE_SYNC_RUNNING] = false
            it.remove(KEY_WIDGET_FORCE_SYNC_STARTED_AT)
        }
    }

    suspend fun markLastSyncFinishedNow() {
        context.syncDataStore.edit {
            it[KEY_LAST_SYNC_FINISHED_AT] = System.currentTimeMillis()
        }
    }
}