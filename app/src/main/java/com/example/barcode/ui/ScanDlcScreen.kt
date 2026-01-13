package com.example.barcode.ui

import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.rememberAsyncImagePainter
import com.example.barcode.ui.components.HeaderBar
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

// Autorise l'accès expérimental à imageProxy.image
@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
fun ScanDlcScreen(
    productName: String? = null,
    productBrand: String? = null,
    productImageUrl: String? = null,
    onValidated: ((expiryEpochMs: Long) -> Unit)? = null,
    onCancel: (() -> Unit)? = null,
    showHeader: Boolean = true,
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var detectedDate by remember { mutableStateOf("") }
    var detectedDateMs by remember { mutableStateOf<Long?>(null) }
    var lastDetectedDate by remember { mutableStateOf("") }
    var frozen by remember { mutableStateOf(false) }
    var lastAnalyseTime by remember { mutableStateOf(0L) }
    var cameraError by remember { mutableStateOf<String?>(null) }

    val minIntervalMs = 500L

    // 👉 Mémorise les use-cases liés par CET écran
    var boundPreview by remember { mutableStateOf<Preview?>(null) }
    var boundAnalysis by remember { mutableStateOf<ImageAnalysis?>(null) }

    val dateRegex = remember {
        Regex("""\b(0[1-9]|[12][0-9]|3[01])\s*([\-\/\. ])\s*(0[1-9]|1[0-2])\s*\2\s*(202\d|203\d|204\d|2050|2[0-9]|3[0-9]|4[0-9]|50)\b""")
    }

    val onRetry = {
        frozen = false
        lastDetectedDate = ""
        detectedDate = ""
        detectedDateMs = null
        lastAnalyseTime = 0L
    }

    val onValidate: () -> Unit = {
        detectedDateMs?.let { ms -> onValidated?.invoke(ms) }
    }

    fun normalizeYear(twoOrFour: String): Int? = when (twoOrFour.length) {
        2 -> (2000 + twoOrFour.toInt()).takeIf { it in 2020..2050 }   // ✅ range
        4 -> twoOrFour.toInt().takeIf { it in 2020..2050 }            // ✅ range
        else -> null
    }

    fun parseToEpochMs(day: String, month: String, year: Int): Long? = try {
        val ld = java.time.LocalDate.of(year, month.toInt(), day.toInt())
        ld.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    } catch (_: Exception) { null }

    if (showHeader) {
        Scaffold(topBar = { HeaderBar(title = "FrigoZen", "Scan d'une date", Icons.Filled.AddCircle) }) { inner ->
            ScanDlcContent(
                modifier = modifier.fillMaxSize().padding(inner),
                productName = productName,
                productBrand = productBrand,
                productImageUrl = productImageUrl,
                previewView = previewView,
                onPreviewViewChange = { previewView = it },
                detectedDate = detectedDate,
                detectedDateMs = detectedDateMs,
                onRetry = onRetry,
                onValidate = onValidate,
                cameraError = cameraError
            )
        }
    } else {
        ScanDlcContent(
            modifier = modifier.fillMaxSize(),
            productName = productName,
            productBrand = productBrand,
            productImageUrl = productImageUrl,
            previewView = previewView,
            onPreviewViewChange = { previewView = it },
            detectedDate = detectedDate,
            detectedDateMs = detectedDateMs,
            onRetry = onRetry,
            onValidate = onValidate,
            cameraError = cameraError
        )
    }

    DisposableEffect(previewView, lifecycleOwner) {
        val view = previewView ?: return@DisposableEffect onDispose { }
        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
        val executor = ContextCompat.getMainExecutor(ctx)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        val listener = Runnable {
            try {
                val cameraProvider = cameraProviderFuture.get()

                // ✅ évite "already bound" / conflits entre écrans
                cameraProvider.unbindAll()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(view.surfaceProvider)
                }

                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build().also { ia ->
                        ia.setAnalyzer(executor) { imageProxy ->

                            if (frozen) {
                                imageProxy.close()
                                return@setAnalyzer
                            }

                            val now = System.currentTimeMillis()
                            if (now - lastAnalyseTime < minIntervalMs) {
                                imageProxy.close()
                                return@setAnalyzer
                            }
                            lastAnalyseTime = now

                            val mediaImage = imageProxy.image
                            if (mediaImage == null) {
                                imageProxy.close()
                                return@setAnalyzer
                            }

                            val inputImage = InputImage.fromMediaImage(
                                mediaImage,
                                imageProxy.imageInfo.rotationDegrees
                            )

                            recognizer.process(inputImage)
                                .addOnSuccessListener { visionText ->
                                    dateRegex.find(visionText.text)?.let { m ->
                                        val (d, sep, mo, yRaw) = m.destructured
                                        val y = normalizeYear(yRaw) ?: return@addOnSuccessListener

                                        val normalized = "$d$sep$mo$sep$y"
                                        if (normalized == lastDetectedDate) return@addOnSuccessListener

                                        val ms = parseToEpochMs(d, mo, y)
                                        detectedDate = normalized
                                        detectedDateMs = ms

                                        // ✅ ne “freeze” que si la date est réellement exploitable
                                        if (ms != null) {
                                            lastDetectedDate = normalized
                                            frozen = true
                                        }
                                    }
                                }
                                .addOnFailureListener { e -> Log.e("OCR", "Erreur", e) }
                                .addOnCompleteListener { imageProxy.close() } // ✅ close unique ici
                        }
                    }

                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )

                boundPreview = preview
                boundAnalysis = analysis
                cameraError = null
            } catch (t: Throwable) {
                Log.e("ScanDlc", "Camera bind failed", t)
                cameraError = t.message ?: t::class.java.simpleName
            }
        }

        cameraProviderFuture.addListener(listener, executor)

        onDispose {
            try {
                if (cameraProviderFuture.isDone) {
                    val provider = cameraProviderFuture.get()
                    // ✅ Désabonne UNIQUEMENT tes use-cases
                    val toUnbind = listOfNotNull(boundAnalysis, boundPreview).toTypedArray()
                    if (toUnbind.isNotEmpty()) provider.unbind(*toUnbind)
                    boundAnalysis = null
                    boundPreview = null
                } else {
                    // Même s’il n’est pas prêt, planifie un unbind “local”
                    cameraProviderFuture.addListener({
                        try {
                            val provider = cameraProviderFuture.get()
                            val toUnbind = listOfNotNull(boundAnalysis, boundPreview).toTypedArray()
                            if (toUnbind.isNotEmpty()) provider.unbind(*toUnbind)
                            boundAnalysis = null
                            boundPreview = null
                        } catch (_: Exception) {}
                    }, executor)
                }
            } catch (_: Exception) {}
            try { recognizer.close() } catch (_: Exception) {}
        }
    }
}


@Composable
private fun ScanDlcContent(
    modifier: Modifier,
    productName: String?,
    productBrand: String?,
    productImageUrl: String?,
    // ✅ state unique venant du parent
    previewView: PreviewView?,
    onPreviewViewChange: (PreviewView?) -> Unit,
    detectedDate: String,
    detectedDateMs: Long?,
    onRetry: () -> Unit,
    onValidate: () -> Unit,
    cameraError: String?
) {
    Box(modifier = modifier) {

        // ✅ CAMERA toujours visible
        AndroidView(
            factory = { c -> PreviewView(c).also { onPreviewViewChange(it) } },
            modifier = Modifier.fillMaxSize(),
            onRelease = { onPreviewViewChange(null) }
        )

        // ✅ Erreur caméra au-dessus
        cameraError?.let { msg ->
            Card(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text(
                    text = "Caméra indisponible : $msg",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        // ✅ CARD toujours affichée (en bas)
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

            // --- Bloc infos produit + date (date placeholder si pas détectée) ---
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!productImageUrl.isNullOrBlank()) {
                    Image(
                        painter = rememberAsyncImagePainter(productImageUrl),
                        contentDescription = null,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(12.dp))
                }

                Column(Modifier.weight(1f)) {
                    Text(
                        text = productName ?: "(sans nom)",
                        fontWeight = FontWeight.SemiBold
                    )

                    if (!productBrand.isNullOrBlank()) {
                        Text(
                            text = productBrand,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f)
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    val hasDate = detectedDateMs != null && detectedDate.isNotBlank()
                    Text(
                        text = if (hasDate) "Date détectée : $detectedDate" else "Date détectée : en attente…",
                        color = if (hasDate) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                        },
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Divider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
            )

            // --- Boutons (gros blocs) ---
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
                        brush = SolidColor(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
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
                    enabled = detectedDateMs != null, // ✅ important
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(bottomEnd = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
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