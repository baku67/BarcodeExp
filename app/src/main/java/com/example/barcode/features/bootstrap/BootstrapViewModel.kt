package com.example.barcode.features.bootstrap

import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.barcode.core.AppMode
import com.example.barcode.core.SessionManager
import com.example.barcode.domain.models.toUserPreferences
import com.example.barcode.features.auth.AuthRepository
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.time.LocalDate

sealed interface BootState {
    data object Loading : BootState
    data class Go(val route: String) : BootState
    data class ShowTimeline(
        val targetRoute: String,
        val expired: IntArray,
        val soon: IntArray
    ) : BootState
    data class Error(val message: String) : BootState
}

class BootstrapViewModel(
    private val repo: AuthRepository,
    private val session: SessionManager,
    private val introStore: IntroStore,
    private val timelineRepo: TimelineRepository
) : ViewModel() {

    private val _state = MutableStateFlow<BootState>(BootState.Loading)
    val state: StateFlow<BootState> = _state

    // anti double-run (recompositions / double LaunchedEffect)
    private var started = false

    fun bootstrap() {
        if (started) return
        started = true

        viewModelScope.launch {
            _state.value = BootState.Loading

            // 1) Charge timeline en parallèle (ne bloque pas le boot)
            val timelineDeferred: Deferred<Pair<IntArray, IntArray>?> = async {
                withTimeoutOrNull(3500) { timelineRepo.fetchTimelineIntro3Days() }
            }

            val mode = session.appMode.first()
            val targetRoute = when (mode) {
                AppMode.LOCAL -> "tabs"
                AppMode.AUTH -> bootstrapAuthOrGoLogin()
            }

            // Si on doit aller au login, inutile de faire la timeline
            if (targetRoute != "tabs") {
                _state.value = BootState.Go(targetRoute)
                return@launch
            }

            // 2) Timeline intro (si pertinente et pas déjà vue aujourd'hui)
            val timeline = timelineDeferred.await()
            if (timeline == null) {
                _state.value = BootState.Go(targetRoute)
                return@launch
            }

            val (expired, soon) = timeline
            val hasInteresting = (expired.sum() + soon.sum()) > 0

            val today = LocalDate.now().toString()
            val alreadySeenToday = introStore.getLastSeenDate() == today

            if (!hasInteresting || alreadySeenToday) {
                _state.value = BootState.Go(targetRoute)
                return@launch
            }

            introStore.setLastSeenToday()
            _state.value = BootState.ShowTimeline(targetRoute, expired, soon)
        }
    }

    /**
     * Retourne "tabs" si l’auth auto + refresh initial a réussi,
     * sinon retourne "auth/login".
     */
    private suspend fun bootstrapAuthOrGoLogin(): String {
        val access = session.token.first()
        val refresh = session.refreshToken.first()

        // Pas de refresh => impossible d’auto-auth (tu es en AUTH mode mais plus de crédentials)
        if (refresh.isNullOrBlank()) {
            session.clear() // garde AppMode.AUTH, mais enlève tokens
            return "auth/login"
        }

        // 0) Refresh initial (mais sans casser inutilement la rotation)
        // => on refresh si access absent OU access expire très bientôt
        val shouldRefreshNow = access.isNullOrBlank() || (access.expiresInSec()?.let { it <= 30L } == true)

        var tokenToUse: String? = access

        if (shouldRefreshNow) {
            val refreshed = withTimeoutOrNull(3000) { repo.refresh(refresh) }
            if (refreshed != null && refreshed.isSuccess) {
                val rr = refreshed.getOrThrow()

                session.saveToken(rr.token)
                // rotation support (single_use = true)
                rr.refreshToken?.let { session.saveRefreshToken(it) }

                tokenToUse = rr.token
            } else {
                // refresh KO :
                // - si on a encore un access token, on tente /me (OkHttp Authenticator peut aussi refresh sur 401)
                // - sinon => login obligatoire
                if (tokenToUse.isNullOrBlank()) {
                    session.clear()
                    return "auth/login"
                }
            }
        }

        // 1) Tente /me (valide l’access + remplit cache user/prefs)
        val meOk = tokenToUse?.let { withTimeoutOrNull(3000) { fetchAndCacheMe(it) } } == true
        if (meOk) return "tabs"

        // 2) /me KO -> on tente une dernière fois un refresh (si l’access était expiré ou invalide)
        val refreshed2 = withTimeoutOrNull(3000) { repo.refresh(refresh) }
        if (refreshed2 == null || refreshed2.isFailure) {
            session.clear()
            return "auth/login"
        }

        val rr2 = refreshed2.getOrThrow()
        session.saveToken(rr2.token)
        rr2.refreshToken?.let { session.saveRefreshToken(it) }

        val meOk2 = withTimeoutOrNull(3000) { fetchAndCacheMe(rr2.token) } == true
        if (!meOk2) {
            session.clear()
            return "auth/login"
        }

        return "tabs"
    }

    private suspend fun fetchAndCacheMe(token: String): Boolean {
        return repo.me(token).fold(
            onSuccess = { profile ->
                session.saveUser(profile)
                session.savePreferences(profile.toUserPreferences())
                true
            },
            onFailure = { false }
        )
    }

    /**
     * Retourne le nombre de secondes restantes avant expiration JWT (claim exp),
     * ou null si parsing impossible.
     */
    private fun String.expiresInSec(): Long? {
        return try {
            val parts = split(".")
            if (parts.size < 2) return null

            val payloadB64 = parts[1]
            val decoded = Base64.decode(
                payloadB64,
                Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
            )

            val json = JSONObject(String(decoded, Charsets.UTF_8))
            val exp = json.optLong("exp", -1L)
            if (exp <= 0L) return null

            val nowSec = System.currentTimeMillis() / 1000L
            exp - nowSec
        } catch (_: Exception) {
            null
        }
    }
}

class BootstrapViewModelFactory(
    private val repo: AuthRepository,
    private val session: SessionManager,
    private val introStore: IntroStore,
    private val timelineRepo: TimelineRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return BootstrapViewModel(repo, session, introStore, timelineRepo) as T
    }
}