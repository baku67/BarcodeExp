package com.example.barcode.sync

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.random.Random

private val Context.syncDataStore by preferencesDataStore(name = "sync_prefs")

data class WidgetForceSyncToken(
    val requestId: Long,
    val startedAt: Long
)

class SyncPreferences(context: Context) {

    private val appContext = context.applicationContext

    companion object {
        private val KEY_LAST_SUCCESS_AT = longPreferencesKey("last_success_sync_at")
        private val KEY_LAST_SYNC_FINISHED_AT = longPreferencesKey("last_sync_finished_at")
        private val KEY_AUTH_REQUIRED = booleanPreferencesKey("auth_required")

        private val KEY_WIDGET_FORCE_SYNC_RUNNING =
            booleanPreferencesKey("widget_force_sync_running")

        private val KEY_WIDGET_FORCE_SYNC_STARTED_AT =
            longPreferencesKey("widget_force_sync_started_at")

        private val KEY_WIDGET_FORCE_SYNC_REQUEST_ID =
            longPreferencesKey("widget_force_sync_request_id")

        private const val WIDGET_FORCE_SYNC_MAX_DURATION_MS = 10 * 60 * 1000L
    }

    val lastSuccessAt: Flow<Long> =
        appContext.syncDataStore.data.map { it[KEY_LAST_SUCCESS_AT] ?: 0L }

    val lastSyncFinishedAt: Flow<Long> =
        appContext.syncDataStore.data.map { it[KEY_LAST_SYNC_FINISHED_AT] ?: 0L }

    val authRequired: Flow<Boolean> =
        appContext.syncDataStore.data.map { it[KEY_AUTH_REQUIRED] ?: false }

    val widgetForceSyncStartedAt: Flow<Long> =
        appContext.syncDataStore.data.map { it[KEY_WIDGET_FORCE_SYNC_STARTED_AT] ?: 0L }

    val widgetForceSyncRequestId: Flow<Long> =
        appContext.syncDataStore.data.map { it[KEY_WIDGET_FORCE_SYNC_REQUEST_ID] ?: 0L }

    val isWidgetForceSyncRunning: Flow<Boolean> =
        appContext.syncDataStore.data.map { prefs ->
            val running = prefs[KEY_WIDGET_FORCE_SYNC_RUNNING] ?: false
            val startedAt = prefs[KEY_WIDGET_FORCE_SYNC_STARTED_AT] ?: 0L
            val requestId = prefs[KEY_WIDGET_FORCE_SYNC_REQUEST_ID] ?: 0L

            val isStale = startedAt > 0L &&
                    System.currentTimeMillis() - startedAt > WIDGET_FORCE_SYNC_MAX_DURATION_MS

            running && startedAt > 0L && requestId > 0L && !isStale
        }

    suspend fun markSyncSuccessAt(serverWatermarkMs: Long) {
        appContext.syncDataStore.edit {
            it[KEY_LAST_SUCCESS_AT] = serverWatermarkMs
            it[KEY_LAST_SYNC_FINISHED_AT] = System.currentTimeMillis()
            it[KEY_AUTH_REQUIRED] = false
        }
    }

    suspend fun markAuthRequired() {
        appContext.syncDataStore.edit {
            it[KEY_AUTH_REQUIRED] = true
        }
    }

    suspend fun clearAuthRequired() {
        appContext.syncDataStore.edit {
            it[KEY_AUTH_REQUIRED] = false
        }
    }

    suspend fun markWidgetForceSyncStarted(): WidgetForceSyncToken {
        val startedAt = System.currentTimeMillis()

        // ID assez stable et unique même en cas de clics très rapprochés.
        val requestId = startedAt * 1_000L + Random.nextLong(from = 0L, until = 1_000L)

        appContext.syncDataStore.edit {
            it[KEY_WIDGET_FORCE_SYNC_RUNNING] = true
            it[KEY_WIDGET_FORCE_SYNC_STARTED_AT] = startedAt
            it[KEY_WIDGET_FORCE_SYNC_REQUEST_ID] = requestId
        }

        return WidgetForceSyncToken(
            requestId = requestId,
            startedAt = startedAt
        )
    }

    suspend fun markWidgetForceSyncFinished(requestId: Long): Boolean {
        var didClear = false

        appContext.syncDataStore.edit { prefs ->
            val currentRequestId = prefs[KEY_WIDGET_FORCE_SYNC_REQUEST_ID] ?: 0L

            if (requestId > 0L && currentRequestId == requestId) {
                prefs[KEY_WIDGET_FORCE_SYNC_RUNNING] = false
                prefs.remove(KEY_WIDGET_FORCE_SYNC_STARTED_AT)
                prefs.remove(KEY_WIDGET_FORCE_SYNC_REQUEST_ID)

                didClear = true
            }
        }

        return didClear
    }

    suspend fun forceClearWidgetForceSyncRunning() {
        appContext.syncDataStore.edit {
            it[KEY_WIDGET_FORCE_SYNC_RUNNING] = false
            it.remove(KEY_WIDGET_FORCE_SYNC_STARTED_AT)
            it.remove(KEY_WIDGET_FORCE_SYNC_REQUEST_ID)
        }
    }

    suspend fun markLastSyncFinishedNow() {
        appContext.syncDataStore.edit {
            it[KEY_LAST_SYNC_FINISHED_AT] = System.currentTimeMillis()
        }
    }
}