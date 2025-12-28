package com.example.barcode.stores

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// private val Context.dataStore by preferencesDataStore(name = "app_settings")
// private val KEY_AUTO_LOCK_ENABLED = booleanPreferencesKey("auto_lock_enabled")

object AppSettingsStore {
    // fun autoLockEnabledFlow(ctx: Context): Flow<Boolean> =
    //     ctx.dataStore.data.map { it[KEY_AUTO_LOCK_ENABLED] ?: true } // ON par défaut

    // suspend fun setAutoLockEnabled(ctx: Context, enabled: Boolean) {
    //     ctx.dataStore.edit { it[KEY_AUTO_LOCK_ENABLED] = enabled }
    // }
}