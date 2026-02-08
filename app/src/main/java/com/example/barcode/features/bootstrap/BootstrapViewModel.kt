package com.example.barcode.features.bootstrap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.barcode.core.session.AppMode
import com.example.barcode.features.auth.AuthRepository
import com.example.barcode.core.session.SessionManager
import com.example.barcode.domain.models.toUserPreferences
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
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
    private val timelineRepo: TimelineRepository    // à créer
) : ViewModel() {

    val state = MutableStateFlow<BootState>(BootState.Loading)

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

    fun bootstrap() {
        viewModelScope.launch {
            val mode = session.appMode.first()

            if (mode == AppMode.LOCAL) {
                state.value = BootState.Go("tabs")
                return@launch
            }

            // AUTH
            val access = session.token.first()
            val refresh = session.refreshToken.first()

            // 1) Essayer /me avec access token (et mettre à jour le cache user)
            var meOk = false
            if (!access.isNullOrBlank()) {
                meOk = fetchAndCacheMe(access) // ✅ cache user/prefs même si token déjà OK
            }

            if (!meOk) {
                // 2) tenter refresh si possible
                if (refresh.isNullOrBlank()) {
                    session.logout()
                    state.value = BootState.Go("auth/login")
                    return@launch
                }

                val refreshed = repo.refresh(refresh)
                if (refreshed.isFailure) {
                    session.logout()
                    state.value = BootState.Go("auth/login")
                    return@launch
                }

                val rr = refreshed.getOrThrow()
                session.saveToken(rr.token)
                rr.refresh_token?.let { session.saveRefreshToken(it) } // rotation OK

                // re-check /me + cache
                meOk = fetchAndCacheMe(rr.token)
                if (!meOk) {
                    session.logout()
                    state.value = BootState.Go("auth/login")
                    return@launch
                }
            }

            // 3) Charger dashboard + timeline (avec timeout pour ne pas bloquer l’app)
            val tlDeferred = async {
                withTimeoutOrNull(3500) { timelineRepo.fetchTimelineIntro3Days() }
            }

            val timeline = tlDeferred.await()    // peut être null si timeout

            // 4) Décider timeline intro
            val target = "tabs"

            if (timeline == null) {
                state.value = BootState.Go(target)
                return@launch
            }

            val (expired, soon) = timeline
            val hasInteresting = (expired.sum() + soon.sum()) > 0

            val today = LocalDate.now().toString()
            val alreadySeenToday = introStore.getLastSeenDate() == today

            if (!hasInteresting || alreadySeenToday) {
                state.value = BootState.Go(target)
                return@launch
            }

            introStore.setLastSeenToday()
            state.value = BootState.ShowTimeline(target, expired, soon)
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
        return BootstrapViewModel(repo, session, introStore, timelineRepo) as T
    }
}
