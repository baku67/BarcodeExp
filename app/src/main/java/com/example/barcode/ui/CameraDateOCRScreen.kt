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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.barcode.ui.components.HeaderBar
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

// Autorise l'accès expérimental à imageProxy.image
@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
fun CameraDateOcrScreen(
    onValidated: ((expiryEpochMs: Long) -> Unit)? = null,
    onCancel: (() -> Unit)? = null
) {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    var detectedDate by remember { mutableStateOf("") }
    var lastDetectedDate by remember { mutableStateOf("") }
    var lastAnalyseTime by remember { mutableStateOf(0L) }
    val minIntervalMs = 500L
    var detectedDateMs by remember { mutableStateOf<Long?>(null) }

    // 👉 Mémorise les use-cases liés par CET écran
    var boundPreview by remember { mutableStateOf<Preview?>(null) }
    var boundAnalysis by remember { mutableStateOf<ImageAnalysis?>(null) }

    val dateRegex = remember {
        Regex("""\b(0[1-9]|[12][0-9]|3[01])\s*([\-\/\. ])\s*(0[1-9]|1[0-2])\s*\2\s*(202\d|203\d|204\d|2050|2[0-9]|3[0-9]|4[0-9]|50)\b""")
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

    Scaffold(
        topBar = { HeaderBar(title = "FrigoZen", "Scan d'une date", Icons.Filled.AddCircle) }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            AndroidView(
                factory = { c -> PreviewView(c).also { previewView = it } },
                modifier = Modifier.fillMaxSize(),
                onRelease = { previewView = null } // important
            )

            Text(
                text = detectedDate,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .background(Color.Black)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }

    DisposableEffect(previewView, lifecycleOwner) {
        val view = previewView ?: return@DisposableEffect onDispose { }
        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
        val executor = ContextCompat.getMainExecutor(ctx)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        val listener = Runnable {
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(view.surfaceProvider)
            }

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build().also { ia ->
                    ia.setAnalyzer(executor) { imageProxy ->
                        val now = System.currentTimeMillis()
                        if (now - lastAnalyseTime < minIntervalMs) { imageProxy.close(); return@setAnalyzer }
                        lastAnalyseTime = now

                        imageProxy.image?.let { mediaImage ->
                            val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                            recognizer.process(inputImage)
                                .addOnSuccessListener { visionText ->
                                    dateRegex.find(visionText.text)?.let { m ->
                                        if (m.groupValues.size >= 5) {
                                            val (d, sep, mo, yRaw) = m.destructured
                                            normalizeYear(yRaw)?.let { y ->
                                                val normalized = "$d$sep$mo$sep$y"
                                                if (normalized != lastDetectedDate) {
                                                    lastDetectedDate = normalized
                                                    detectedDate = normalized
                                                    detectedDateMs = parseToEpochMs(d, mo, y)
                                                }
                                            }
                                        }
                                    }
                                }
                                .addOnFailureListener { e -> Log.e("OCR", "Erreur", e) }
                                .addOnCompleteListener { imageProxy.close() }
                        } ?: imageProxy.close()
                    }
                }

            cameraProvider.bindToLifecycle(
                lifecycleOwner, // ✅ pas l’Activity
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                analysis
            )

            boundPreview = preview
            boundAnalysis = analysis
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