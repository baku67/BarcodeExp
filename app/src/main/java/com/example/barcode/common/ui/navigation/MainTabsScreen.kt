package com.example.barcode.common.ui.navigation

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.barcode.core.SessionManager
import com.example.barcode.features.auth.AuthViewModel
import com.example.barcode.features.dashboard.HomeContent
import com.example.barcode.features.fridge.FridgePage
import com.example.barcode.features.listeCourse.ListeCoursesContent
import com.example.barcode.features.recipies.RecipesContent
import com.example.barcode.features.settings.SettingsContent
import com.example.barcode.sync.SyncPreferences
import com.example.barcode.sync.SyncScheduler
import com.example.barcode.sync.SyncUiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainTabsScreen(navController: NavHostController, authVm: AuthViewModel) {

    val tabs = listOf("home", "listeCourses", "items", "recipes", "settings")
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { tabs.size })
    val scope = rememberCoroutineScope()

    fun goToTab(route: String) {
        val idx = tabs.indexOf(route)
        if (idx >= 0) {
            scope.launch {
                pagerState.animateScrollToPage(
                    page = idx,
                    animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing)
                )
            }
        }
    }

    val selectedRoute = tabs[pagerState.currentPage]

    val context = LocalContext.current

    val prefs = remember { SyncPreferences(context) }
    val lastSuccessAt by prefs.lastSuccessAt.collectAsState(initial = null)
    val authRequired by prefs.authRequired.collectAsState(initial = false)

    val workInfos by WorkManager.getInstance(context)
        .getWorkInfosByTagLiveData(SyncScheduler.SYNC_TAG)
        .observeAsState(emptyList())

    val isSyncing = workInfos.any {
        it.state == WorkInfo.State.RUNNING
    }

    val isOnline = isOnline(context)
    val sessionManager = remember { SessionManager(context) }
    val isAuthenticated by produceState(initialValue = false, key1 = sessionManager) {
        value = sessionManager.isAuthenticated()
    }

    // Optionnel : renomme pour éviter la confusion visuelle
    val barSyncState: SyncUiState = when {
        authRequired || (isOnline && !isAuthenticated) -> SyncUiState.AuthRequired
        !isOnline -> SyncUiState.Offline
        isSyncing -> SyncUiState.Syncing
        else -> SyncUiState.UpToDate(lastSuccessAt)
    }

    AppContentWithBars(
        navController = navController,
        selectedRoute = selectedRoute,
        onTabClick = { route -> goToTab(route) },
        syncState = barSyncState, // ✅ TESTs (remplacer par SyncUiState.Syncing ou autre enum pour forcer)
        onSyncRetry = { SyncScheduler.enqueueSync(context) }
    ) { innerPadding, snackbarHostState  ->

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { page ->

            val isActive = pagerState.currentPage == page

            when (tabs[page]) {
                "home" -> HomeContent(
                    onNavigateToListeCourses = { goToTab("listeCourses") },
                    onNavigateToRecipes = { goToTab("recipes") },
                    onNavigateToItems = { goToTab("items") },
                    innerPadding = PaddingValues(),
                    14,
                    10,
                    3,
                    0
                ) // TODO Data Dashboard factices pour l'instant

                "listeCourses" -> ListeCoursesContent(
                    innerPadding = PaddingValues(),
                    isActive = isActive
                )

                "items" -> FridgePage(
                    innerPadding = PaddingValues(),
                    authVm = authVm,
                    onAddItem = {
                        navController.navigate("addItem/choose") {
                            launchSingleTop = true
                        }
                    },
                    isActive = isActive
                )

                "recipes" -> RecipesContent(
                    innerPadding = PaddingValues(),
                    isActive = isActive
                )

                "settings" -> SettingsContent(
                    innerPadding = PaddingValues(),
                    authVm = authVm,
                    onGoToLogin = {
                        navController.navigate("auth/login") {
                            popUpTo(0)
                            launchSingleTop = true
                        }
                    },
                    onGoToRegister = {
                        navController.navigate("auth/register") {
                            launchSingleTop = true
                        }
                    },
                    isActive = isActive
                )
            }
        }
    }
}


private fun isOnline(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}