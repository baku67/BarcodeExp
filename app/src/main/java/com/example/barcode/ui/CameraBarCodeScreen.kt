package com.example.barcode.ui

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.barcode.R
import com.example.barcode.model.ProductInfo
import com.example.barcode.ui.components.HeaderBar
import com.example.barcode.util.fetchProductInfo
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch
import com.example.barcode.stores.OpenFoodFactsStore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.ui.zIndex
import com.example.barcode.stores.AppSettingsStore
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner

// Composable pour scanner un code-barres et récupérer le nom du produit
@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
fun CameraOcrBarCodeScreen( onValidated: ((product: ProductInfo, barcode: String) -> Unit)? = null ) {
    val ctx = LocalContext.current
    val haptics = LocalHapticFeedback.current // pour vibartions
    val primary = MaterialTheme.colorScheme.primary
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var scannedCode by remember { mutableStateOf("") }
    var lastScanned by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val fetchCount by remember(ctx) { OpenFoodFactsStore.countFlow(ctx) }.collectAsState(initial = 0)
    var rateLimitMsg by remember { mutableStateOf<String?>(null) }
    var lastApiCallAt by remember { mutableStateOf(0L) } // Debouncing
    var productInfo by remember { mutableStateOf<ProductInfo?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current
    var boundPreview by remember { mutableStateOf<Preview?>(null) }
    var boundAnalysis by remember { mutableStateOf<ImageAnalysis?>(null) }

    // Flash
    var boundCamera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }
    var torchOn by remember { mutableStateOf(false) }

    val autoLockEnabled by remember(ctx) { AppSettingsStore.autoLockEnabledFlow(ctx) }
        .collectAsState(initial = true) // bouton pour toggle le verrou auto
    var scanLocked by remember { mutableStateOf(false) } // Verrou pour bloquer detection lorsqu'un produit a été trouvé

    previewView?.let { view ->
        DisposableEffect(view, lifecycleOwner) {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            val executor = ContextCompat.getMainExecutor(ctx)
            val scanner = BarcodeScanning.getClient()

            val listener = Runnable {
                val cameraProvider = cameraProviderFuture.get()
                // Preview
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(view.surfaceProvider) }
                // Analyse
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build().also { ia ->
                        ia.setAnalyzer(executor) { imageProxy ->

                            if (scanLocked) { imageProxy.close(); return@setAnalyzer } // Si verrou actif (produit trouvé) return

                            imageProxy.image?.let { mediaImage ->
                                val inputImg = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                scanner.process(inputImg)
                                    .addOnSuccessListener { barcodes ->

                                        if (scanLocked) return@addOnSuccessListener    // ⬅️ ignore résultat si verrou activé entre-temps

                                        // Prend le premier code non-nul
                                        val first = barcodes.firstOrNull { it.rawValue != null }?.rawValue
                                        if (first != null && first != lastScanned) {

                                            lastScanned = first
                                            scannedCode = first

                                            val now = System.currentTimeMillis()
                                            if (now - lastApiCallAt < 1000) return@addOnSuccessListener  // bloque si < 1s
                                            lastApiCallAt = now

                                            scope.launch {
                                                OpenFoodFactsStore.counterIncrement(ctx)
                                                val res = fetchProductInfo(first)
                                                rateLimitMsg = if (res.rateLimited) (res.message ?: "Rate limit atteint") else null
                                                productInfo = res.product

                                                if (res.product != null) {
                                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress) // petite vibration
                                                    if (autoLockEnabled) scanLocked = true // verrouille après succès si autoLockEnabled (bouton)
                                                }
                                            }

                                        }
                                    }
                                    .addOnFailureListener { e -> Log.e("BARCODE", "Erreur scan", e) }
                                    .addOnCompleteListener { imageProxy.close() }
                            } ?: imageProxy.close()
                        }
                    }
                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )
                boundCamera = camera
                boundPreview = preview
                boundAnalysis = analysis

                // (optionnel) si tu veux refléter l’état système du flash :
                val torchLiveData = camera.cameraInfo.torchState
                torchLiveData.observe(lifecycleOwner) { state ->
                    torchOn = (state == androidx.camera.core.TorchState.ON)
                }
            }
            cameraProviderFuture.addListener(listener, executor)
            onDispose {
                if (cameraProviderFuture.isDone) {
                    val provider = cameraProviderFuture.get()
                    val toUnbind = listOfNotNull(boundAnalysis, boundPreview).toTypedArray()
                    if (toUnbind.isNotEmpty()) provider.unbind(*toUnbind)   // ✅ unbind local
                    boundAnalysis = null
                    boundPreview = null
                }
            }
        }
    }

    Scaffold(
        topBar = { HeaderBar(title = "FrigoZen", "Scan du produit", Icons.Filled.AddCircle) }
    ) { innerPadding ->

        // Un seul grand Box "relatif" pour pouvoir positionner en absolu
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding) // sous la HeaderBar
        ) {
            Column(Modifier.fillMaxSize()) {

                // FAB flash
                FloatingActionButton(
                    onClick = {
                        boundCamera?.cameraControl?.enableTorch(!torchOn)
                        torchOn = !torchOn   // synchronisation locale immédiate
                    },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = if (torchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = if (torchOn) "Flash ON" else "Flash OFF"
                    )
                }

                /* ---------- Compteur de requetes (compteur stored) ---------- */
                Text("API: $fetchCount")

                /* ---------- Erreur "rate limit" ---------- */
                if (rateLimitMsg != null) {
                    Text(
                        text = rateLimitMsg!!,
                        color = Color(0xFFD32F2F), // rouge discret
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                    )
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    /* ---------- Camera ---------- */
                    AndroidView(
                        factory = { c -> PreviewView(c).also {
                            // it.preferredImplementationMode = PreviewView.ImplementationMode.COMPATIBLE
                            previewView = it
                        } },
                        modifier = Modifier.fillMaxSize(),
                        onRelease = { previewView = null }
                    )


                    /* ---------- État « pas de données » (todo: composant dédié, props icon ou Lottie) ---------- */
                    if (productInfo == null) {
                        /* Charge la composition Lottie une seule fois */
                        val composition by rememberLottieComposition(
                            LottieCompositionSpec.RawRes(R.raw.barcode_scanner)   // ton fichier .json dans res/raw
                        )
                        /* Anime-la en boucle infinie */
                        val progress by animateLottieCompositionAsState(
                            composition = composition,
                            iterations = LottieConstants.IterateForever
                        )


                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .background(
                                    Color.White.copy(alpha = .4f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .navigationBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            // CircularProgressIndicator(color = Color.White) // Spinner
                            LottieAnimation(
                                composition = composition,
                                progress = progress,
                                modifier = Modifier.size(200.dp).alpha(0.55f)  // ajuste la taille
                            )

                            Text(
                                text = "Survolez un code-barre pour ajouter un produit",
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyMedium,
                                fontSize = 16.sp,
                                fontStyle = FontStyle.Italic,
                                color = Color.Black,
                                modifier = Modifier.fillMaxWidth().alpha(0.6f)
                            )
                        }
                    }


                    /* ---------- Carte de résultat (todo: composant dédié générique mais props de config d'affichage) ---------- */
                    else {
                        Card(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .navigationBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(24.dp),
                            elevation = CardDefaults.cardElevation(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE0E0E0)) // fond gris clair
                        ) {

                            /* --- Bloc haut : image + infos --- */
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                // Image produit
                                if (productInfo!!.imageUrl.isNotEmpty()) {
                                    AsyncImage(
                                        model = productInfo!!.imageUrl,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(110.dp)
                                            .clip(RoundedCornerShape(16.dp))      // mêmes coins que la carte
                                    )
                                }

                                Spacer(Modifier.width(16.dp))

                                /* Texte (colonne) */
                                Column(
                                    modifier = Modifier.weight(1f)               // prend toute la place dispo
                                ) {
                                    Text(
                                        text = productInfo!!.name,
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = productInfo!!.brand,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = scannedCode,
                                        style = MaterialTheme.typography.bodySmall
                                    )

                                    /* Badge Nutri-Score (optionnel) */
                                    val nutriColor = when (productInfo!!.nutriScore.uppercase()) {
                                        "A" -> Color(0xFF2E7D32)
                                        "B" -> Color(0xFF7CB342)
                                        "C" -> Color(0xFFFDD835)
                                        "D" -> Color(0xFFF4511E)
                                        "E" -> Color(0xFFE53935)
                                        else -> Color.Gray
                                    }
                                    if (productInfo!!.nutriScore.isNotEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .padding(start = 8.dp)
                                                .background(nutriColor, RoundedCornerShape(6.dp))
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = productInfo!!.nutriScore.uppercase(),
                                                style = MaterialTheme.typography.labelLarge.copy(
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = Color.White
                                            )
                                        }
                                    }
                                }


                            }

                            Divider(thickness = 1.dp, color = Color.Black.copy(alpha = .15f))

                            /* --- Bloc 2 boutons --- */
                            Row(
                                modifier = Modifier
                                    .height(56.dp)
                                    .fillMaxWidth()
                            ) {
                                /* Bouton Re-try */
                                Button(
                                    onClick = {
                                        lastScanned = ""
                                        scannedCode = ""
                                        productInfo = null
                                        scanLocked = false   // on rouvre la détection
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(
                                            0xFFD32F2F
                                        )
                                    ),
                                    shape = RoundedCornerShape(bottomStart = 24.dp)   // coin bas-gauche arrondi
                                ) {
                                    Text("Re-try")
                                }

                                /* Bouton Valider */
                                Button(
                                    onClick = { productInfo?.let { p -> onValidated?.invoke(p, scannedCode) } },
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = primary
                                    ),
                                    shape = RoundedCornerShape(bottomEnd = 24.dp)      // coin bas-droit arrondi
                                ) {
                                    Text("Valider")
                                }
                            }
                        }
                    }
                }
            }

            /* ---------- Bouton auto-lock (état stored) -----------*/
            FloatingActionButton(
                onClick = {
                    // Toggle persistant + si on désactive, on lève le verrou courant
                    scope.launch {
                        val newEnabled = !autoLockEnabled
                        AppSettingsStore.setAutoLockEnabled(ctx, newEnabled)
                        // 🔒 Si on active, on verrouille tout de suite (évite "une dernière détection")
                        scanLocked = newEnabled
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)       // coin haut-droit DU BOX
                    .padding(top = 8.dp, end = 12.dp) // sous la HeaderBar, collé à droite
                    .zIndex(1f),                   // au-dessus des autres overlays si besoin
                containerColor = primary,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = if (autoLockEnabled) Icons.Filled.Lock else Icons.Filled.LockOpen,
                    contentDescription = if (autoLockEnabled) "Auto-lock activé" else "Auto-lock désactivé"
                )
            }
        }
    }
}