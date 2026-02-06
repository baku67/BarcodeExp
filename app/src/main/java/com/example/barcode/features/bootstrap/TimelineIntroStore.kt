package com.example.barcode.features.bootstrap


import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import java.time.LocalDate

private val Context.dataStore by preferencesDataStore(name = "intro_prefs")

// La dernière fois que l'animation Intro a été jouée? Pour savoir si on ré-affiche ou pas l'anim (1max par jour)
object IntroPrefsKeys {
    val LAST_SEEN_DATE = stringPreferencesKey("timeline_intro_last_seen_date") // "2026-01-09"
}

class IntroStore(private val context: Context) {

    suspend fun getLastSeenDate(): String? {
        val prefs = context.dataStore.data.first()
        return prefs[IntroPrefsKeys.LAST_SEEN_DATE]
    }

    suspend fun setLastSeenToday() {
        val today = LocalDate.now().toString()
        context.dataStore.edit { it[IntroPrefsKeys.LAST_SEEN_DATE] = today }
    }
}
