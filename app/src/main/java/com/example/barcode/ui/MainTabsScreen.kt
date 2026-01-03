package com.example.barcode.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.example.barcode.auth.AuthViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainTabsScreen(navController: NavHostController, authVm: AuthViewModel) {

    val tabs = listOf("home", "items", "listeCourses", "settings")
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { tabs.size })
    val scope = rememberCoroutineScope()

    val selectedRoute = tabs[pagerState.currentPage]

    AppContentWithBars(
        navController = navController,
        selectedRoute = selectedRoute,
        onTabClick = { route ->
            val idx = tabs.indexOf(route)
            if (idx >= 0) scope.launch {
                pagerState.animateScrollToPage(
                    page = idx,
                    animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing)
                )
            }
        }
    ) { innerPadding, snackbarHostState  ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { page ->

            when (tabs[page]) {
                "home" -> HomeContent(navController, innerPadding = androidx.compose.foundation.layout.PaddingValues())
                "items" -> ItemsContent(navController, innerPadding = androidx.compose.foundation.layout.PaddingValues())
                "listeCourses" -> ListeCoursesContent(navController, innerPadding = androidx.compose.foundation.layout.PaddingValues())
                "settings" -> SettingsContent(
                    navController = navController,
                    innerPadding = androidx.compose.foundation.layout.PaddingValues(),
                    authVm = authVm
                )
            }
        }
    }
}
