package com.example.barcode

import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

// MainActivity configure NavController et gère la permission caméra au démarrage
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Demande de la permission CAMERA dès le démarrage
        val requestPerm = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (!granted) finish() // Termine si refus
        }
        requestPerm.launch(Manifest.permission.CAMERA)

        // Configuration de l'UI Compose avec Navigation
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    // Définition des routes
                    NavHost(navController, startDestination = "home") {
                        composable("home") { HomeScreen(navController) }
                        composable("camera") { CameraOcrScreen() }
                    }
                }
            }
        }
    }
}

// Écran d'accueil avec un bouton pour accéder à l'OCR
@Composable
fun HomeScreen(navController: NavHostController) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Button(onClick = { navController.navigate("camera") }) {
            Text(text = "OCR Dates")
        }
    }
}

// Autorise l'accès expérimental à imageProxy.image
@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
fun CameraOcrScreen() {
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
        Regex("""\b(0[1-9]|[12][0-9]|3[01])\s*[-/]\s*(0[1-9]|1[0-2])\s*[-/]\s*(19\d{2}|20\d{2})\b""")
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
                                        val match = dateRegex.find(visionText.text)?.value
                                        if (match != null && match != lastDetectedDate) {
                                            lastDetectedDate = match
                                            detectedDate = match
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

    // UI Compose : flux caméra + overlay date en bas
    Box(modifier = Modifier.fillMaxSize()) {
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
