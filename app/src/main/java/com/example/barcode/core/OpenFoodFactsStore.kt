package com.example.barcode.core

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

object OpenFoodFactsStore {

    private const val STORE_NAME = "openfoodfacts_stats"
    private val FETCH_COUNT = intPreferencesKey("fetch_count")

    private val Context.openFoodFactsDataStore by preferencesDataStore(name = STORE_NAME)

    private fun ds(context: Context) = context.applicationContext.openFoodFactsDataStore

    fun countFlow(context: Context): Flow<Int> =
        ds(context).data.map { it[FETCH_COUNT] ?: 0 }

    suspend fun increment(context: Context) {
        ds(context).edit { prefs ->
            prefs[FETCH_COUNT] = (prefs[FETCH_COUNT] ?: 0) + 1
        }
    }
}