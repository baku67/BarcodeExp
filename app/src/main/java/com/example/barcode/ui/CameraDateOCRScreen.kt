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
fun CameraDateOcrScreen() {
    val ctx = LocalContext.current
    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    // State pour la date détectée et éviter doublons
    var detectedDate by remember { mutableStateOf("") }
    var lastDetectedDate by remember { mutableStateOf("") }
    // Throttle temporel
    var lastAnalyseTime by remember { mutableStateOf(0L) }
    val minIntervalMs = 500L
    // Regex pour dd/MM/yyyy ou dd-MM-yyyy
    val dateRegex = remember {
        // Regex("""\b(0[1-9]|[12][0-9]|3[01])\s*[-\/\.]\s*(0[1-9]|1[0-2])\s*[-\/\.]\s*(19\d{2}|20\d{2})\b""") // sans espaces comme séparateur
        Regex("""\b(0[1-9]|[12][0-9]|3[01])\s*([\-\/\. ])\s*(0[1-9]|1[0-2])\s*\2\s*(202\d|203\d|204\d|2050|2[0-9]|3[0-9]|4[0-9]|50)\b""") // avec espaces comme séparateur et années en AA ou AAAA (2020-2050)
    }

    fun normalizeYear(twoOrFour: String): Int? {
        return when (twoOrFour.length) {
            2 -> {
                val yy = twoOrFour.toInt()
                val year = 2000 + yy       // 20 -> 2020 ... 50 -> 2050
                if (year in 2020..2050) year else null
            }
            4 -> {
                val year = twoOrFour.toInt()
                if (year in 2020..2050) year else null
            }
            else -> null
        }
    }

    // Liaison de CameraX + ML Kit dès que PreviewView est prêt
    previewView?.let { view ->
        DisposableEffect(view) {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            val executor = ContextCompat.getMainExecutor(ctx)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            val listener = Runnable {
                val cameraProvider = cameraProviderFuture.get()
                // Setup Preview
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(view.surfaceProvider)
                }
                // Setup Analysis
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build().also { ia ->
                        ia.setAnalyzer(executor) { imageProxy ->
                            val now = System.currentTimeMillis()
                            if (now - lastAnalyseTime < minIntervalMs) {
                                imageProxy.close()
                                return@setAnalyzer
                            }
                            lastAnalyseTime = now

                            imageProxy.image?.let { mediaImage ->
                                val inputImage = InputImage.fromMediaImage(
                                    mediaImage,
                                    imageProxy.imageInfo.rotationDegrees
                                )
                                recognizer.process(inputImage)
                                    .addOnSuccessListener { visionText ->
                                        // Extraction des dates
                                        val m = dateRegex.find(visionText.text)
                                        if (m != null && m.groupValues.size >= 5) {
                                            val (d, sep, mo, yRaw) = m.destructured
                                            normalizeYear(yRaw)?.let { y ->
                                                val normalized = "$d$sep$mo$sep$y"
                                                if (normalized != lastDetectedDate) {
                                                    lastDetectedDate = normalized
                                                    detectedDate = normalized
                                                }
                                            }
                                        }
                                    }
                                    .addOnFailureListener { e -> Log.e("OCR", "Erreur", e) }
                                    .addOnCompleteListener { imageProxy.close() }
                            } ?: imageProxy.close()
                        }
                    }

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    ctx as ComponentActivity,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )
            }

            cameraProviderFuture.addListener(listener, executor)
            onDispose { if (cameraProviderFuture.isDone) cameraProviderFuture.get().unbindAll() }
        }
    }

    Scaffold(
        topBar = { HeaderBar(title = "FrigoZen", "Scan d'une date", Icons.Filled.AddCircle) }
    ) { innerPadding ->

        // UI Compose : flux caméra + overlay date en bas
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            AndroidView(
                factory = { c -> PreviewView(c).also { previewView = it } },
                modifier = Modifier.fillMaxSize()
            )
            Text(
                text = detectedDate,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    // ajoute un padding équivalent à la hauteur de la nav. bar
                    .navigationBarsPadding()
                    // puis votre padding interne
                    .background(Color.Black)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}