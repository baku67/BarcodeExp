package com.example.barcode.common.ui.navigation

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.barcode.BarcodeApp
import com.example.barcode.core.SessionManager
import com.example.barcode.features.addItems.ItemsViewModel
import com.example.barcode.features.auth.AuthViewModel
import com.example.barcode.features.dashboard.HomeContent
import com.example.barcode.features.fridge.FridgePage
import com.example.barcode.features.listeCourse.ListeCoursesContent
import com.example.barcode.features.listeCourse.ShoppingListScope
import com.example.barcode.features.listeCourse.ShoppingListViewModel
import com.example.barcode.features.listeCourse.ShoppingListViewModelFactory
import com.example.barcode.features.recipies.RecipesContent
import com.example.barcode.features.settings.SettingsContent
import com.example.barcode.sync.SyncPreferences
import com.example.barcode.sync.SyncScheduler
import com.example.barcode.sync.SyncUiState
import com.example.barcode.widgets.WidgetNavigation
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@SuppressLint("UnrememberedGetBackStackEntry")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainTabsScreen(
    navController: NavHostController,
    authVm: AuthViewModel
) {
    val tabs = listOf("home", "listeCourses", "items", "recipes", "settings")
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { tabs.size }
    )
    val scope = rememberCoroutineScope()

    var itemsReselectToken by rememberSaveable { mutableStateOf(0) }

    var pendingWidgetShoppingScope by rememberSaveable { mutableStateOf<String?>(null) }

    fun goToTab(route: String) {
        val idx = tabs.indexOf(route)
        if (idx >= 0) {
            scope.launch {
                pagerState.animateScrollToPage(
                    page = idx,
                    animationSpec = tween(
                        durationMillis = 550,
                        easing = FastOutSlowInEasing
                    )
                )
            }
        }
    }

    val tabsEntry = remember(navController) {
        navController.getBackStackEntry("tabs")
    }

    val widgetDestination by tabsEntry
        .savedStateHandle
        .getStateFlow<String?>(
            WidgetNavigation.SAVED_STATE_DESTINATION,
            null
        )
        .collectAsState()

    LaunchedEffect(widgetDestination) {
        val destination = widgetDestination ?: return@LaunchedEffect

        val targetRoute = when (destination) {
            WidgetNavigation.DESTINATION_FRIDGE -> "items"

            WidgetNavigation.DESTINATION_SHOPPING,
            WidgetNavigation.DESTINATION_SHOPPING_SHARED,
            WidgetNavigation.DESTINATION_SHOPPING_PERSONAL -> "listeCourses"

            else -> null
        }

        pendingWidgetShoppingScope = when (destination) {
            WidgetNavigation.DESTINATION_SHOPPING_SHARED -> WidgetNavigation.DESTINATION_SHOPPING_SHARED
            WidgetNavigation.DESTINATION_SHOPPING_PERSONAL -> WidgetNavigation.DESTINATION_SHOPPING_PERSONAL

            // fallback si une ancienne version du widget envoie encore "shopping"
            WidgetNavigation.DESTINATION_SHOPPING -> WidgetNavigation.DESTINATION_SHOPPING_SHARED

            else -> pendingWidgetShoppingScope
        }

        val targetIndex = targetRoute?.let { tabs.indexOf(it) } ?: -1

        if (targetIndex >= 0) {
            if (pagerState.currentPage == targetIndex) {
                if (targetRoute == "items") {
                    itemsReselectToken++
                }
            } else {
                pagerState.scrollToPage(targetIndex)
            }
        }

        tabsEntry.savedStateHandle.remove<String>(
            WidgetNavigation.SAVED_STATE_DESTINATION
        )
    }

    val selectedRoute = tabs[pagerState.currentPage]
    val context = LocalContext.current
    val app = context.applicationContext as BarcodeApp

    val itemsVm: ItemsViewModel = viewModel()
    val items by itemsVm.items.collectAsState()

    val prefs = remember { SyncPreferences(context) }
    val lastSuccessAt by prefs.lastSuccessAt.collectAsState(initial = null)
    val authRequired by prefs.authRequired.collectAsState(initial = false)

    val workInfos by WorkManager.getInstance(context)
        .getWorkInfosByTagLiveData(SyncScheduler.SYNC_TAG)
        .observeAsState(emptyList())

    val isSyncing = workInfos.any { it.state == WorkInfo.State.RUNNING }

    val isOnline = isOnline(context)
    val sessionManager = remember { SessionManager(context) }
    val currentUserId by sessionManager.userId.collectAsState(initial = null)
    val currentHomeId by sessionManager.currentHomeId.collectAsState(initial = null)
    val isAuthenticated by produceState(
        initialValue = false,
        key1 = sessionManager
    ) {
        value = sessionManager.isAuthenticated()
    }

    val shoppingListVm: ShoppingListViewModel? =
        if (!currentUserId.isNullOrBlank() && !currentHomeId.isNullOrBlank()) {
            viewModel(
                factory = ShoppingListViewModelFactory(
                    app = app,
                    dao = app.shoppingListDao,
                    currentHomeId = currentHomeId!!,
                    currentUserId = currentUserId!!
                )
            )
        } else {
            null
        }

    val shoppingItems by (shoppingListVm?.items ?: flowOf(emptyList()))
        .collectAsState(initial = emptyList())

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
        onTabReselect = { route ->
            if (route == "items") itemsReselectToken++
        },
        syncState = barSyncState,
        onSyncRetry = { SyncScheduler.enqueueSync(context) }
    ) { innerPadding, _ ->

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
                    items = items,
                    shoppingItems = shoppingItems
                )

                "listeCourses" -> {
                    shoppingListVm?.let { vm ->
                        ListeCoursesContent(
                            innerPadding = PaddingValues(),
                            isActive = isActive,
                            onAddItem = { scope ->
                                navController.navigate("shoppingList/add/${scope.routeValue}") {
                                    launchSingleTop = true
                                }
                            },
                            vm = vm,
                            widgetRequestedScope = pendingWidgetShoppingScope.toShoppingListScopeOrNull(),
                            onWidgetRequestedScopeConsumed = {
                                pendingWidgetShoppingScope = null
                            }
                        )
                    } ?: Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                "items" -> FridgePage(
                    navController = navController,
                    innerPadding = PaddingValues(),
                    authVm = authVm,
                    onAddItem = {
                        navController.navigate("addItem/choose") {
                            launchSingleTop = true
                        }
                    },
                    isActive = isActive,
                    scrollToTopToken = itemsReselectToken
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

private fun String?.toShoppingListScopeOrNull(): ShoppingListScope? {
    return when (this) {
        WidgetNavigation.DESTINATION_SHOPPING_SHARED -> ShoppingListScope.SHARED
        WidgetNavigation.DESTINATION_SHOPPING_PERSONAL -> ShoppingListScope.PERSONAL
        else -> null
    }
}