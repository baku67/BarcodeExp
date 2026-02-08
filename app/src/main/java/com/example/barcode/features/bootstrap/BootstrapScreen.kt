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
import com.example.barcode.R
import com.example.barcode.features.auth.AuthRepository
import com.example.barcode.core.session.SessionManager
import androidx.compose.runtime.getValue



@Composable
fun GlobalLoaderScreen(nav: NavHostController) {

    val appContext = LocalContext.current.applicationContext
    val timelineIntroStore = remember { IntroStore(appContext) }

    val session = remember { SessionManager(appContext) }
    val repo = remember { AuthRepository() }

    val timelineRepo = remember { TimelineRepository() }

    val vm: BootstrapViewModel = viewModel(
        factory = BootstrapViewModelFactory(repo, session, timelineIntroStore, timelineRepo)
    )

    val state by vm.state.collectAsState()

    LaunchedEffect(Unit) { vm.bootstrap() }


    val gradient = remember {
        Brush.horizontalGradient(
            listOf(
                Color(0xFF2196F3), // bleu
                Color(0xFF4CAF50)  // vert
            )
        )
    }

    // ⏳ Simule le chargement sans bloquer l’UI

    LaunchedEffect(state) {
        when (val s = state) {
            is BootState.Go -> {
                nav.navigate(s.route) {
                    popUpTo("splash") { inclusive = true }
                    launchSingleTop = true
                }
            }
            is BootState.ShowTimeline -> {
                nav.currentBackStackEntry?.savedStateHandle?.set("timeline_target", s.targetRoute)
                nav.currentBackStackEntry?.savedStateHandle?.set("timeline_expired", s.expired)
                nav.currentBackStackEntry?.savedStateHandle?.set("timeline_soon", s.soon)

                nav.navigate("introTimeline") {
                    popUpTo("splash") { inclusive = false }
                    launchSingleTop = true
                }
            }
            else -> Unit
        }
    }

    // UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 🖼 Grande image (remplace par ton visuel)
            Image(
                painter = painterResource(id = R.drawable.frigozen_title),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .aspectRatio(1f)
                    .padding(bottom = 0.dp),
                contentScale = ContentScale.Fit
            )

            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(brush = gradient)) { // dégradé uniquement ici
                        append("Piece of food, Peace of mind")
                    }
                },
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.SemiBold, // ou FontWeight.Bold
                    fontSize = 18.sp                  // ajuste (ex: 20.sp)
                ),
                modifier = Modifier.padding(bottom = 70.dp).absoluteOffset(x = 0.dp, y = -20.dp)
            )


            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 4.dp
            )

            //Text(
            //    text = "Chargement…",
            //    style = MaterialTheme.typography.bodySmall,
            //    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            //    modifier = Modifier.padding(top = 12.dp)
            //)


        }
    }
}