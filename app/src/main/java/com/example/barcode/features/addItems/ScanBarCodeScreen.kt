package com.example.barcode.features.addItems

import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.barcode.R
import com.example.barcode.core.OpenFoodFactsStore
import com.example.barcode.data.remote.fetchProductInfo
import com.example.barcode.domain.models.ProductInfo
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.util.Size

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
fun ScanBarCodeScreen(
    onValidated: ((product: ProductInfo, barcode: String) -> Unit)? = null
) {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    // ✅ ROI commun (overlay + tentative scan ROI)
    val roiWidthFraction = 0.94f
    val roiAspect = 1.9f
    val roiCornerRadius = 22.dp

    // Camera
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var boundPreview by remember { mutableStateOf<Preview?>(null) }
    var boundAnalysis by remember { mutableStateOf<ImageAnalysis?>(null) }
    var boundCamera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }

    // Scan states
    var scannedCode by remember { mutableStateOf("") }
    var lastScanned by remember { mutableStateOf("") }
    var lastApiCallAt by remember { mutableStateOf(0L) }
    var scanLocked by remember { mutableStateOf(false) }
    var isFetching by remember { mutableStateOf(false) }
    var productInfo by remember { mutableStateOf<ProductInfo?>(null) }

    // UI states
    var torchOn by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }
    var rateLimitMsg by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    // (debug) compteur de requêtes
    val fetchCount by remember(ctx) { OpenFoodFactsStore.countFlow(ctx) }.collectAsState(initial = 0)

    // Bind camera once previewView is ready
    previewView?.let { view ->
        DisposableEffect(view, lifecycleOwner) {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            val mainExecutor = ContextCompat.getMainExecutor(ctx)
            val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()

            val scanner = BarcodeScanning.getClient()

            // ✅ Buffer RGBA réutilisable (ROI strict quand dispo)
            val rgbaBuffer = RgbaFrameBuffer()


            val listener = Runnable {
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also { it.setSurfaceProvider(view.surfaceProvider) }

                val analysisBuilder = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setTargetResolution(Size(1280, 720))

                // ✅ Tente RGBA_8888 -> ROI strict possible
                analysisBuilder.tryEnableRgba8888Output() // Commenter pour tester fallback FullView

                val analysis = analysisBuilder
                    .build()
                    .also { ia ->
                        ia.setAnalyzer(analysisExecutor) { imageProxy ->

                            if (scanLocked || isFetching) {
                                imageProxy.close()
                                return@setAnalyzer
                            }

                            val rotation = imageProxy.imageInfo.rotationDegrees

                            // ✅ Branche ROI strict si RGBA dispo
                            val bmp: Bitmap? = rgbaBuffer.toBitmapOrNull(imageProxy)
                            if (bmp != null) {
                                val inputImg = bmp.cropCenterForRoi(
                                    rotationDegrees = rotation,
                                    roiWidthFraction = roiWidthFraction,
                                    roiAspect = roiAspect
                                )

                                scanner.process(inputImg)
                                    .addOnSuccessListener(mainExecutor) { barcodes ->
                                        if (scanLocked || isFetching) return@addOnSuccessListener

                                        val first = barcodes.firstOrNull { it.rawValue != null }?.rawValue
                                        if (first != null && first != lastScanned) {
                                            val now = System.currentTimeMillis()
                                            if (now - lastApiCallAt < 1000) return@addOnSuccessListener
                                            lastApiCallAt = now

                                            lastScanned = first
                                            scannedCode = first

                                            scanLocked = true
                                            isFetching = true

                                            scope.launch {
                                                OpenFoodFactsStore.increment(ctx)
                                                val res = fetchProductInfo(first)
                                                rateLimitMsg = if (res.rateLimited) (res.message ?: "Rate limit atteint") else null

                                                if (res.product != null) {
                                                    productInfo = res.product
                                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                } else {
                                                    delay(650)
                                                    lastScanned = ""
                                                    scannedCode = ""
                                                    scanLocked = false
                                                }
                                                isFetching = false
                                            }
                                        }
                                    }
                                    .addOnFailureListener(mainExecutor) { e ->
                                        Log.e("BARCODE", "Erreur scan", e)
                                    }
                                    .addOnCompleteListener(mainExecutor) {
                                        imageProxy.close()
                                    }

                                return@setAnalyzer
                            }

                            // ⚠️ Fallback full viewport (ROI non strict) si RGBA indispo
                            val mediaImage = imageProxy.image ?: run {
                                imageProxy.close()
                                return@setAnalyzer
                            }

                            val inputImg = InputImage.fromMediaImage(mediaImage, rotation)

                            scanner.process(inputImg)
                                .addOnSuccessListener(mainExecutor) { barcodes ->
                                    if (scanLocked || isFetching) return@addOnSuccessListener

                                    val first = barcodes.firstOrNull { it.rawValue != null }?.rawValue
                                    if (first != null && first != lastScanned) {
                                        val now = System.currentTimeMillis()
                                        if (now - lastApiCallAt < 1000) return@addOnSuccessListener
                                        lastApiCallAt = now

                                        lastScanned = first
                                        scannedCode = first

                                        scanLocked = true
                                        isFetching = true

                                        scope.launch {
                                            OpenFoodFactsStore.increment(ctx)
                                            val res = fetchProductInfo(first)
                                            rateLimitMsg = if (res.rateLimited) (res.message ?: "Rate limit atteint") else null

                                            if (res.product != null) {
                                                productInfo = res.product
                                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            } else {
                                                delay(650)
                                                lastScanned = ""
                                                scannedCode = ""
                                                scanLocked = false
                                            }
                                            isFetching = false
                                        }
                                    }
                                }
                                .addOnFailureListener(mainExecutor) { e ->
                                    Log.e("BARCODE", "Erreur scan", e)
                                }
                                .addOnCompleteListener(mainExecutor) {
                                    imageProxy.close()
                                }
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

                camera.cameraInfo.torchState.observe(lifecycleOwner) { state ->
                    torchOn = (state == androidx.camera.core.TorchState.ON)
                }
            }

            cameraProviderFuture.addListener(listener, mainExecutor)

            onDispose {
                if (cameraProviderFuture.isDone) {
                    val provider = cameraProviderFuture.get()
                    val toUnbind = listOfNotNull(boundAnalysis, boundPreview).toTypedArray()
                    if (toUnbind.isNotEmpty()) provider.unbind(*toUnbind)
                    boundAnalysis = null
                    boundPreview = null
                    boundCamera = null
                }
                try { analysisExecutor.shutdown() } catch (_: Exception) {}
            }
        }
    }

    // Snackbar rate limit
    LaunchedEffect(rateLimitMsg) {
        val msg = rateLimitMsg ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        rateLimitMsg = null
    }

    // Flash feedback quand produit trouvé
    var captureFlash by remember { mutableStateOf(false) }
    val flashAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (captureFlash) 0.16f else 0f,
        animationSpec = tween(durationMillis = 90, easing = LinearEasing),
        label = "flashAlpha"
    )
    LaunchedEffect(productInfo) {
        if (productInfo != null) {
            captureFlash = true
            delay(90)
            captureFlash = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // ✅ Camera preview
        AndroidView(
            factory = { c -> PreviewView(c).also { previewView = it } },
            modifier = Modifier.fillMaxSize(),
            onRelease = { previewView = null }
        )

        // ✅ Overlay flash court (feedback “capture”)
        if (flashAlpha > 0.001f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = flashAlpha))
            )
        }

        // ✅ UI scan (mask + cutout + coins 1px + scan-line + hint)
        if (productInfo == null) {
            CameraScanOverlay(
                modifier = Modifier.fillMaxSize(),
                isFetching = isFetching,
                fetchCount = fetchCount,
                showDebugCount = false,
                roiWidthFraction = roiWidthFraction,
                roiAspect = roiAspect,
                roiCornerRadius = roiCornerRadius
            )
        }

        // ✅ Top controls (flash + help)
        CameraTopControls(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
            torchOn = torchOn,
            onToggleTorch = {
                boundCamera?.cameraControl?.enableTorch(!torchOn)
                torchOn = !torchOn
            },
            onHelp = { showHelp = true }
        )

        // ✅ Résultat en bas
        if (productInfo != null) {
            ResultCard(
                modifier = Modifier.align(Alignment.BottomCenter),
                productInfo = productInfo!!,
                onRetry = {
                    lastScanned = ""
                    scannedCode = ""
                    productInfo = null
                    scanLocked = false
                    isFetching = false
                },
                onValidate = { onValidated?.invoke(productInfo!!, scannedCode) }
            )
        }

        // ✅ Snackbars
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 8.dp)
        ) {
            SnackbarHost(hostState = snackbarHostState)
        }
    }

    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            confirmButton = {
                Button(onClick = { showHelp = false }) { Text("OK") }
            },
            title = { Text("Aide scan") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("• Aligne le code-barres dans le cadre.")
                    Text("• Éloigne ou rapproche légèrement si ça ne détecte pas.")
                    Text("• Active le flash si la lumière est faible.")
                }
            }
        )
    }
}

@Composable
private fun CameraTopControls(
    modifier: Modifier = Modifier,
    torchOn: Boolean,
    onToggleTorch: () -> Unit,
    onHelp: () -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.35f)) {
            IconButton(onClick = onHelp) {
                Icon(imageVector = Icons.Default.Info, contentDescription = "Aide", tint = Color.White)
            }
        }

        Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.35f)) {
            IconButton(onClick = onToggleTorch) {
                Icon(
                    imageVector = if (torchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = "Flash",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun CameraScanOverlay(
    modifier: Modifier = Modifier,
    isFetching: Boolean,
    fetchCount: Int,
    showDebugCount: Boolean,
    roiWidthFraction: Float,
    roiAspect: Float,
    roiCornerRadius: androidx.compose.ui.unit.Dp
) {
    val windowWidthFraction = roiWidthFraction
    val windowAspect = roiAspect
    val cornerRadius = roiCornerRadius

    val bracketLen = 34.dp            // ✅ coins plus visibles
    val bracketInset = 10.dp
    val scanLineInset = 14.dp

    val overlayColor = Color.Black.copy(alpha = 0.42f) // ✅ dimming moins agressif => moins "fake"

    val bracketColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.92f)

    val onePx = with(LocalDensity.current) { 1f.toDp() } // ✅ 1px exact
    val bracketStroke = onePx

    val transition = rememberInfiniteTransition(label = "scanLine")
    val scanT by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1450, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanT"
    )

    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val windowW = maxWidth * windowWidthFraction
        val windowH = windowW / windowAspect
        val windowShape = RoundedCornerShape(cornerRadius)

        // ✅ Mask + cutout + coins + scanline (1 seul Canvas = perf)
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        ) {
            val w = size.width * windowWidthFraction
            val h = w / windowAspect
            val left = (size.width - w) / 2f
            val top = (size.height - h) / 2f
            val rect = Rect(left, top, left + w, top + h)

            // overlay sombre
            drawRect(color = overlayColor)

            // cutout (fenêtre transparente)
            val r = with(density) { cornerRadius.toPx() }
            drawRoundRect(
                color = Color.Transparent,
                topLeft = rect.topLeft,
                size = rect.size,
                cornerRadius = CornerRadius(r, r),
                blendMode = BlendMode.Clear
            )

            // scan line
            val scanY = rect.top + rect.height * scanT
            val scanInsetPx = with(density) { scanLineInset.toPx() }
            drawLine(
                color = bracketColor.copy(alpha = 0.55f),
                start = Offset(rect.left + scanInsetPx, scanY),
                end = Offset(rect.right - scanInsetPx, scanY),
                strokeWidth = with(density) { onePx.toPx() },
                cap = StrokeCap.Round
            )

            // corner brackets (fenêtre)
            val sw = with(density) { bracketStroke.toPx() }
            val len = with(density) { bracketLen.toPx() }
            val inset = with(density) { bracketInset.toPx() }

            // top-left
            drawLine(bracketColor, Offset(rect.left + inset, rect.top + inset), Offset(rect.left + inset + len, rect.top + inset), sw, cap = StrokeCap.Round)
            drawLine(bracketColor, Offset(rect.left + inset, rect.top + inset), Offset(rect.left + inset, rect.top + inset + len), sw, cap = StrokeCap.Round)
            // top-right
            drawLine(bracketColor, Offset(rect.right - inset - len, rect.top + inset), Offset(rect.right - inset, rect.top + inset), sw, cap = StrokeCap.Round)
            drawLine(bracketColor, Offset(rect.right - inset, rect.top + inset), Offset(rect.right - inset, rect.top + inset + len), sw, cap = StrokeCap.Round)
            // bottom-left
            drawLine(bracketColor, Offset(rect.left + inset, rect.bottom - inset), Offset(rect.left + inset + len, rect.bottom - inset), sw, cap = StrokeCap.Round)
            drawLine(bracketColor, Offset(rect.left + inset, rect.bottom - inset - len), Offset(rect.left + inset, rect.bottom - inset), sw, cap = StrokeCap.Round)
            // bottom-right
            drawLine(bracketColor, Offset(rect.right - inset - len, rect.bottom - inset), Offset(rect.right - inset, rect.bottom - inset), sw, cap = StrokeCap.Round)
            drawLine(bracketColor, Offset(rect.right - inset, rect.bottom - inset - len), Offset(rect.right - inset, rect.bottom - inset), sw, cap = StrokeCap.Round)

            // (bonus) coins “viewport” discrets : effet caméra
            val vColor = Color.White.copy(alpha = 0.16f)
            val vInset = with(density) { 18.dp.toPx() }
            val vLen = with(density) { 34.dp.toPx() }
            val vSw = with(density) { onePx.toPx() }

            // top-left
            drawLine(vColor, Offset(vInset, vInset), Offset(vInset + vLen, vInset), vSw, cap = StrokeCap.Round)
            drawLine(vColor, Offset(vInset, vInset), Offset(vInset, vInset + vLen), vSw, cap = StrokeCap.Round)
            // top-right
            drawLine(vColor, Offset(size.width - vInset - vLen, vInset), Offset(size.width - vInset, vInset), vSw, cap = StrokeCap.Round)
            drawLine(vColor, Offset(size.width - vInset, vInset), Offset(size.width - vInset, vInset + vLen), vSw, cap = StrokeCap.Round)
            // bottom-left
            drawLine(vColor, Offset(vInset, size.height - vInset), Offset(vInset + vLen, size.height - vInset), vSw, cap = StrokeCap.Round)
            drawLine(vColor, Offset(vInset, size.height - vInset - vLen), Offset(vInset, size.height - vInset), vSw, cap = StrokeCap.Round)
            // bottom-right
            drawLine(vColor, Offset(size.width - vInset - vLen, size.height - vInset), Offset(size.width - vInset, size.height - vInset), vSw, cap = StrokeCap.Round)
            drawLine(vColor, Offset(size.width - vInset, size.height - vInset - vLen), Offset(size.width - vInset, size.height - vInset), vSw, cap = StrokeCap.Round)
        }

        // ✅ Lottie centrée dans la fenêtre (discrète)
        val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.barcode_scanner))
        val progress by animateLottieCompositionAsState(
            composition = composition,
            iterations = LottieConstants.IterateForever
        )

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .width(windowW)
                .height(windowH)
                .clip(windowShape)
        ) {
            LottieAnimation(
                composition = composition,
                progress = progress,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(200.dp)
                    .alpha(if (isFetching) 0.10f else 0.18f)
            )
        }

        // ✅ Hint + état fetching
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = windowH / 2 + 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = if (isFetching) "Recherche du produit…" else "Cherche un code-barres",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 15.sp,
                fontStyle = FontStyle.Italic,
                color = Color.White.copy(alpha = 0.92f),
                modifier = Modifier.padding(horizontal = 22.dp)
            )

            if (isFetching) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Scan en cours",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            if (showDebugCount) {
                Text(
                    text = "API: $fetchCount",
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun ResultCard(
    modifier: Modifier = Modifier,
    productInfo: ProductInfo,
    onRetry: () -> Unit,
    onValidate: () -> Unit
) {
    Card(
        modifier = modifier
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
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val imgUrl = productInfo.imageUrl

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
                    text = productInfo.name,
                    fontWeight = FontWeight.SemiBold
                )
                if (productInfo.brand.isNotEmpty()) {
                    Text(
                        text = productInfo.brand,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f)
                    )
                }
            }
        }

        Divider(thickness = 1.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

        Row(
            modifier = Modifier
                .height(52.dp)
                .fillMaxWidth()
        ) {
            Button(
                onClick = onRetry,
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
                onClick = onValidate,
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



// ---------- ROI helpers (RGBA strict + fallback full viewport) ----------

private fun Bitmap.cropCenterForRoi(
    rotationDegrees: Int,
    roiWidthFraction: Float,
    roiAspect: Float
): InputImage {
    val uprightW = if (rotationDegrees == 90 || rotationDegrees == 270) this.height else this.width
    val roiW = (uprightW * roiWidthFraction).toInt().coerceAtLeast(1)
    val roiH = (roiW / roiAspect).toInt().coerceAtLeast(1)

    val cropW = if (rotationDegrees == 90 || rotationDegrees == 270) roiH else roiW
    val cropH = if (rotationDegrees == 90 || rotationDegrees == 270) roiW else roiH

    val left = ((this.width - cropW) / 2).coerceAtLeast(0)
    val top = ((this.height - cropH) / 2).coerceAtLeast(0)
    val safeW = cropW.coerceAtMost(this.width - left)
    val safeH = cropH.coerceAtMost(this.height - top)

    val cropped = Bitmap.createBitmap(this, left, top, safeW, safeH)
    return InputImage.fromBitmap(cropped, rotationDegrees)
}

private fun ImageAnalysis.Builder.tryEnableRgba8888Output() {
    try {
        val builderCls = this::class.java
        val imageAnalysisCls = ImageAnalysis::class.java

        val setOutputImageFormat = builderCls.getMethod("setOutputImageFormat", Int::class.javaPrimitiveType)
        val rgbaField = imageAnalysisCls.getField("OUTPUT_IMAGE_FORMAT_RGBA_8888")
        val rgbaValue = rgbaField.getInt(null)

        setOutputImageFormat.invoke(this, rgbaValue)
    } catch (_: Throwable) {
        // CameraX trop ancien / device incompatible -> fallback full viewport
    }
}
