package com.example.barcode.core

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.barcode.domain.models.FrigoLayout
import com.example.barcode.domain.models.ThemeMode
import com.example.barcode.domain.models.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val PREF_THEME = stringPreferencesKey("pref_theme")

private val PREF_LANG = stringPreferencesKey("pref_lang")
private val PREF_FRIGO_LAYOUT = stringPreferencesKey("pref_frigo_layout")
private val PREF_UPDATED_AT = longPreferencesKey("pref_updated_at")


class UserPreferencesStore(private val context: Context) {

    private val ds get() = context.applicationContext.sessionDataStore

    val preferences: Flow<UserPreferences> = ds.data.map { p ->
        val theme = when (p[PREF_THEME]) {
            "light" -> ThemeMode.LIGHT
            "dark" -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
        val layout = when (p[PREF_FRIGO_LAYOUT]) {
            "list" -> FrigoLayout.LIST
            "design" -> FrigoLayout.DESIGN
            else -> FrigoLayout.DESIGN // ✅ défaut: Frigo (design)
        }

        UserPreferences(
            theme = theme,
            lang = p[PREF_LANG] ?: "fr",
            frigoLayout = layout,
            updatedAtEpochSec = p[PREF_UPDATED_AT]
        )
    }

    suspend fun savePreferences(prefs: UserPreferences) {
        ds.edit { p ->
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
            p[PREF_UPDATED_AT] = prefs.updatedAtEpochSec ?: (System.currentTimeMillis() / 1000)
        }
    }

    suspend fun clearPreferences() {
        ds.edit {
            it.remove(PREF_THEME)
            it.remove(PREF_LANG)
            it.remove(PREF_FRIGO_LAYOUT)
            it.remove(PREF_UPDATED_AT)
        }
    }

    suspend fun setTheme(theme: ThemeMode) {
        ds.edit {
            it[PREF_THEME] = when (theme) {
                ThemeMode.LIGHT -> "light"
                ThemeMode.DARK -> "dark"
                ThemeMode.SYSTEM -> "system"
            }
            it[PREF_UPDATED_AT] = System.currentTimeMillis() / 1000
        }
    }

    suspend fun setLang(lang: String) {
        ds.edit {
            it[PREF_LANG] = lang
            it[PREF_UPDATED_AT] = System.currentTimeMillis() / 1000
        }
    }

    suspend fun setFrigoLayout(layout: FrigoLayout) {
        ds.edit {
            it[PREF_FRIGO_LAYOUT] = when (layout) {
                FrigoLayout.LIST -> "list"
                FrigoLayout.DESIGN -> "design"
            }
            it[PREF_UPDATED_AT] = System.currentTimeMillis() / 1000
        }
    }
}