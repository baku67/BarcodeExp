package com.example.barcode.core.session

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.barcode.domain.models.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.example.barcode.core.session.sessionDataStore


private val TOKEN_KEY = stringPreferencesKey("token")
private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")

private val APP_MODE_KEY = stringPreferencesKey("app_mode")
enum class AppMode { LOCAL, AUTH }

private val USER_ID_KEY = stringPreferencesKey("user_id")
private val USER_EMAIL_KEY = stringPreferencesKey("user_email")
private val USER_IS_VERIFIED_KEY = booleanPreferencesKey("user_is_verified")

class AuthStore(private val context: Context) {

    private val ds get() = context.applicationContext.sessionDataStore

    val token: Flow<String?> = ds.data.map { it[TOKEN_KEY] }
    val refreshToken: Flow<String?> = ds.data.map { it[REFRESH_TOKEN_KEY] }

    val appMode: Flow<AppMode> = ds.data.map { prefs ->
        when (prefs[APP_MODE_KEY]) {
            AppMode.AUTH.name -> AppMode.AUTH
            else -> AppMode.LOCAL
        }
    }

    val userId: Flow<String?> = ds.data.map { it[USER_ID_KEY] }
    val userEmail: Flow<String?> = ds.data.map { it[USER_EMAIL_KEY] }
    val userIsVerified: Flow<Boolean?> = ds.data.map { it[USER_IS_VERIFIED_KEY] }

    suspend fun setAppMode(mode: AppMode) {
        ds.edit { it[APP_MODE_KEY] = mode.name }
    }

    suspend fun saveToken(token: String) {
        ds.edit { it[TOKEN_KEY] = token }
    }

    suspend fun saveRefreshToken(token: String) {
        ds.edit { it[REFRESH_TOKEN_KEY] = token }
    }

    suspend fun saveUser(profile: UserProfile) {
        ds.edit {
            it[USER_ID_KEY] = profile.id
            it[USER_EMAIL_KEY] = profile.email
            it[USER_IS_VERIFIED_KEY] = profile.isVerified
        }
    }

    suspend fun clearTokensOnly() {
        ds.edit {
            it.remove(TOKEN_KEY)
            it.remove(REFRESH_TOKEN_KEY)
        }
    }

    suspend fun logout() {
        ds.edit {
            it.remove(TOKEN_KEY)
            it.remove(REFRESH_TOKEN_KEY)
            it.remove(USER_ID_KEY)
            it.remove(USER_EMAIL_KEY)
            it.remove(USER_IS_VERIFIED_KEY)
            it[APP_MODE_KEY] = AppMode.LOCAL.name
        }
    }
}
