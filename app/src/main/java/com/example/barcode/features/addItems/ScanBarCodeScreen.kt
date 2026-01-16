package com.example.barcode.features.addItems

import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
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
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.barcode.R
import com.example.barcode.OpenFoodFacts.ProductInfo
import com.example.barcode.OpenFoodFacts.fetchProductInfo
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch
import com.example.barcode.OpenFoodFacts.OpenFoodFactsStore
import androidx.compose.material3.Icon
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter

// Composable pour scanner un code-barres et récupérer le nom du produit
@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
fun ScanBarCodeScreen(
    onValidated: ((product: ProductInfo, barcode: String) -> Unit)? = null
) {
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
                                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    scanLocked = true // 🔒 toujours verrouiller après succès
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

    // Un seul grand Box "relatif" pour pouvoir positionner en absolu
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column(Modifier.fillMaxSize()) {

            /* ---------- TODO Compteur de requetes (compteur stored) à retirer ---------- */
            Text("API: $fetchCount")

            /* ---------- Erreur "rate limit" ---------- */
            if (rateLimitMsg != null) {
                Text(
                    text = rateLimitMsg!!,
                    color = Color(0xFFD32F2F),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {

                /* ---------- Camera ---------- */
                AndroidView(
                    factory = { c -> PreviewView(c).also {
                        previewView = it
                    } },
                    modifier = Modifier.fillMaxSize(),
                    onRelease = { previewView = null }
                )

                // Btn toggle Flash overlay
/*                FloatingActionButton(
                    onClick = {
                        boundCamera?.cameraControl?.enableTorch(!torchOn)
                        torchOn = !torchOn
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = if (torchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = if (torchOn) "Flash ON" else "Flash OFF"
                    )
                }*/


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
                            modifier = Modifier.size(200.dp).alpha(0.55f)
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
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)
                        ),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        // --- Bloc haut : image + infos (mêmes spacing que ScanDlc) ---
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val imgUrl = productInfo!!.imageUrl

                            if (imgUrl.isNotEmpty()) {
                                val shape = RoundedCornerShape(12.dp)
                                val painter = rememberAsyncImagePainter(imgUrl)
                                val state = painter.state

                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(shape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painter,
                                        contentDescription = null,
                                        modifier = Modifier.matchParentSize(),
                                        contentScale = ContentScale.Fit
                                    )

                                    if (state is AsyncImagePainter.State.Loading) {
                                        CircularProgressIndicator(
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(18.dp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                                        )
                                    }

                                    if (state is AsyncImagePainter.State.Error) {
                                        Text("🧴", fontSize = 20.sp)
                                    }
                                }

                                Spacer(Modifier.width(12.dp))
                            }

                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = productInfo!!.name,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (productInfo!!.brand.isNotEmpty()) {
                                    Text(
                                        text = productInfo!!.brand,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f)
                                    )
                                }

                                // nutriscore (Si on le met, adapter avec nutrisocre dans BottomSheet ItemsContent)
/*                                if (productInfo!!.nutriScore.isNotEmpty()) {
                                    Text(
                                        text = "Nutri-Score : ${productInfo!!.nutriScore.uppercase()}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f)
                                    )
                                }*/
                            }
                        }

                        // Petite séparation légère (plus soft que ton Divider noir)
                        Divider(thickness = 1.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                        // --- Bloc boutons : on garde tes actions, mais coins alignés sur 16dp ---
                        Row(
                            modifier = Modifier
                                .height(52.dp)
                                .fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    lastScanned = ""
                                    scannedCode = ""
                                    productInfo = null
                                    scanLocked = false
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                shape = RoundedCornerShape(bottomStart = 16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
                                    )
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Re-try", fontWeight = FontWeight.SemiBold)
                            }


                            Button(
                                onClick = { productInfo?.let { p -> onValidated?.invoke(p, scannedCode) } },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(bottomEnd = 16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Valider", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                }
            }
        }
    }

}