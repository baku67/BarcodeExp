package com.example.barcode.OpenFoodFacts

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "stats")
private val FETCH_COUNT = intPreferencesKey("fetch_count")

object OpenFoodFactsStore {
    fun countFlow(ctx: Context) = ctx.dataStore.data.map { it[FETCH_COUNT] ?: 0 }

    suspend fun counterIncrement(ctx: Context) {
        ctx.dataStore.edit { prefs ->
            prefs[FETCH_COUNT] = (prefs[FETCH_COUNT] ?: 0) + 1
        }
    }
}