package com.example.barcode.features.bootstrap

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.barcode.BarcodeApp
import com.example.barcode.R
import com.example.barcode.core.SessionManager

@Composable
fun GlobalLoaderScreen(nav: NavHostController) {
    val appContext = LocalContext.current.applicationContext

    // ✅ Version "recommandée" : on récupère les singletons init dans Application
    val app = appContext as BarcodeApp
    val repo = remember { app.authRepository }

    val session = remember(appContext) { SessionManager(appContext) }
    val timelineIntroStore = remember(appContext) { IntroStore(appContext) }
    val timelineRepo = remember { TimelineRepository() }

    val vm: BootstrapViewModel = viewModel(
        factory = BootstrapViewModelFactory(repo, session, timelineIntroStore, timelineRepo)
    )

    val state by vm.state.collectAsState()

    LaunchedEffect(Unit) { vm.bootstrap() }

    // ✅ Gradient basé sur le thème (plus cohérent que bleu/vert hardcodés)
    val cs = MaterialTheme.colorScheme
    val gradient = remember {
        Brush.horizontalGradient(
            listOf(
                Color(0xFF2196F3), // bleu
                Color(0xFF4CAF50)  // vert
            )
        )
    }

    // Navigation (évite les doubles triggers)
    LaunchedEffect(state) {
        when (val s = state) {
            is BootState.Go -> {
                nav.navigate(s.route) {
                    popUpTo("splash") { inclusive = true }
                    launchSingleTop = true
                }
            }

            is BootState.ShowTimeline -> {
                // ⚠️ On garde "splash" dans la stack (inclusive=false) pour que TimelineIntroScreen
                // puisse lire via nav.previousBackStackEntry?.savedStateHandle
                nav.getBackStackEntry("splash").savedStateHandle.apply {
                    set("timeline_target", s.targetRoute)
                    set("timeline_expired", s.expired)
                    set("timeline_soon", s.soon)
                }

                nav.navigate("introTimeline") {
                    popUpTo("splash") { inclusive = false }
                    launchSingleTop = true
                }
            }

            else -> Unit
        }
    }

    // UI (layout sans offsets “hacky”)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Image(
                painter = painterResource(id = R.drawable.frigozen_title),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .heightIn(max = 240.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            brush = gradient,
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.SemiBold
                        )
                    ) {
                        append("Piece of food, Peace of mind")
                    }
                },
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 18.sp),
                modifier = Modifier.padding(bottom = 42.dp)
            )

            CircularProgressIndicator(
                color = cs.primary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(32.dp)
            )

            // ✅ micro-UX: petit hint discret, sans spam visuel
            Text(
                text = "Initialisation…",
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurface.copy(alpha = 0.55f),
                modifier = Modifier.padding(top = 14.dp)
            )
        }
    }
}