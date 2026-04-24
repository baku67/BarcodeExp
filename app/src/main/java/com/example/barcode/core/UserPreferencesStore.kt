package com.example.barcode.core

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.barcode.common.utils.SeasonRegion
import com.example.barcode.common.utils.SeasonalityResolver
import com.example.barcode.domain.models.FrigoLayout
import com.example.barcode.domain.models.ThemeMode
import com.example.barcode.domain.models.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val PREF_THEME = stringPreferencesKey("pref_theme")
private val PREF_LANG = stringPreferencesKey("pref_lang")
private val PREF_FRIGO_LAYOUT = stringPreferencesKey("pref_frigo_layout")
private val PREF_COUNTRY_CODE = stringPreferencesKey("pref_country_code")
private val PREF_SEASON_REGION_OVERRIDE = stringPreferencesKey("pref_season_region_override")
private val LEGACY_PREF_SEASON_REGION = stringPreferencesKey("pref_season_region")
private val PREF_DASHBOARD_SEASONAL_EXPANDED =
    booleanPreferencesKey("pref_dashboard_seasonal_expanded")
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
            else -> FrigoLayout.DESIGN
        }

        val override = SeasonRegion.fromStorageOrNull(
            p[PREF_SEASON_REGION_OVERRIDE] ?: p[LEGACY_PREF_SEASON_REGION]
        )

        UserPreferences(
            theme = theme,
            lang = p[PREF_LANG] ?: "fr",
            frigoLayout = layout,
            countryCode = SeasonalityResolver.normalizeCountryCodeOrDefault(
                p[PREF_COUNTRY_CODE]
            ),
            seasonRegionOverride = override,
            updatedAtEpochSec = p[PREF_UPDATED_AT]
        )
    }

    val countryCode: Flow<String> = preferences.map { it.countryCode }

    val seasonRegionOverride: Flow<SeasonRegion?> = preferences.map { it.seasonRegionOverride }

    val seasonRegion: Flow<SeasonRegion> = preferences.map { prefs ->
        SeasonalityResolver.effectiveRegion(
            countryCode = prefs.countryCode,
            seasonRegionOverride = prefs.seasonRegionOverride
        )
    }

    val dashboardSeasonalExpanded: Flow<Boolean> = ds.data.map { p ->
        p[PREF_DASHBOARD_SEASONAL_EXPANDED] ?: false
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
            p[PREF_COUNTRY_CODE] =
                SeasonalityResolver.normalizeCountryCodeOrDefault(prefs.countryCode)

            prefs.seasonRegionOverride?.let { override ->
                p[PREF_SEASON_REGION_OVERRIDE] = override.name
            } ?: p.remove(PREF_SEASON_REGION_OVERRIDE)

            p.remove(LEGACY_PREF_SEASON_REGION)

            p[PREF_UPDATED_AT] = prefs.updatedAtEpochSec ?: (System.currentTimeMillis() / 1000)
        }
    }

    suspend fun clearPreferences() {
        ds.edit {
            it.remove(PREF_THEME)
            it.remove(PREF_LANG)
            it.remove(PREF_FRIGO_LAYOUT)
            it.remove(PREF_COUNTRY_CODE)
            it.remove(PREF_SEASON_REGION_OVERRIDE)
            it.remove(LEGACY_PREF_SEASON_REGION)
            it.remove(PREF_DASHBOARD_SEASONAL_EXPANDED)
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

    suspend fun setCountryCode(countryCode: String) {
        ds.edit {
            it[PREF_COUNTRY_CODE] =
                SeasonalityResolver.normalizeCountryCodeOrDefault(countryCode)
            it[PREF_UPDATED_AT] = System.currentTimeMillis() / 1000
        }
    }

    suspend fun setSeasonRegionOverride(region: SeasonRegion?) {
        ds.edit {
            region?.let { override ->
                it[PREF_SEASON_REGION_OVERRIDE] = override.name
            } ?: it.remove(PREF_SEASON_REGION_OVERRIDE)

            it.remove(LEGACY_PREF_SEASON_REGION)
            it[PREF_UPDATED_AT] = System.currentTimeMillis() / 1000
        }
    }

    suspend fun setDashboardSeasonalExpanded(expanded: Boolean) {
        ds.edit {
            it[PREF_DASHBOARD_SEASONAL_EXPANDED] = expanded
            it[PREF_UPDATED_AT] = System.currentTimeMillis() / 1000
        }
    }
}