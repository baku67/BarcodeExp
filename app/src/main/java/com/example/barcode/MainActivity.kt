package com.example.barcode

import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

// MainActivity hérite de ComponentActivity pour utiliser Jetpack Compose
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Demande de la permission CAMERA au runtime
        // registerForActivityResult lance un dialogue system pour demander la permission
        val requestPerm = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            // Si l'utilisateur refuse la permission, on ferme l'activité (et l'app)
            if (!granted) finish()
        }
        // Lance la demande de permission caméra
        requestPerm.launch(Manifest.permission.CAMERA)

        // 2. Définition du contenu Compose de l'activité
        setContent {
            // Utilise le thème Material3 par défaut
            MaterialTheme {
                // Surface fournit un fond et gère les thèmes (clair/sombre)
                Surface(modifier = Modifier.fillMaxSize()) {
                    // Affiche notre composable de vue OCR
                    CameraOcrView()
                }
            }
        }
    }
}

// Annotation pour autoriser l'appel expérimental imageProxy.image
@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
fun CameraOcrView() {
    // Contexte Android (nécessaire pour ProcessCameraProvider, Toast, etc.)
    val ctx = LocalContext.current

    // Stocke la Vue native PreviewView (affiche le flux caméra)
    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    // State qui contient le texte détecté par l'OCR, affiché à l'écran
    var ocrText by remember { mutableStateOf("") }
    // Pour éviter de redessiner le même texte plusieurs fois
    var lastDisplayedText by remember { mutableStateOf("") }
    // Pour throttler la fréquence d'analyse (en ms)
    var lastAnalyseTime by remember { mutableStateOf(0L) }
    val minIntervalMs = 500L // intervalle minimal entre deux analyses

    // Si previewView est non-null, on initialise CameraX et ML Kit
    previewView?.let { view ->
        DisposableEffect(view) {
            // Récupère le ProcessCameraProvider asynchrone
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            // Executor sur le thread principal
            val executor = ContextCompat.getMainExecutor(ctx)
            // Initialise le client ML Kit OCR une fois
            val recognizer = TextRecognition.getClient(
                TextRecognizerOptions.DEFAULT_OPTIONS
            )

            // Dès que cameraProviderFuture est prêt, configure la caméra
            val listener = Runnable {
                // Obtient l'instance ProcessCameraProvider
                val cameraProvider = cameraProviderFuture.get()

                // 1) Setup Preview (affiche le flux caméra)
                val preview = Preview.Builder()
                    .build()
                    .also { it.setSurfaceProvider(view.surfaceProvider) }

                // 2) Setup ImageAnalysis pour récupérer les frames
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build().also { imageAnalysis ->
                        // Configure l'analyseur qui traite chaque frame
                        imageAnalysis.setAnalyzer(executor) { imageProxy ->
                            val now = System.currentTimeMillis()
                            // 2a) Throttle : n'analyser que si le temps écoulé > minIntervalMs
                            if (now - lastAnalyseTime < minIntervalMs) {
                                imageProxy.close()
                                return@setAnalyzer
                            }
                            lastAnalyseTime = now

                            // 2b) Récupère l'image brute (ExperimentalGetImage)
                            imageProxy.image?.let { mediaImage ->
                                // Convertit en InputImage pour ML Kit
                                val inputImg = InputImage.fromMediaImage(
                                    mediaImage,
                                    imageProxy.imageInfo.rotationDegrees
                                )
                                // Lance la reconnaissance de texte
                                recognizer.process(inputImg)
                                    .addOnSuccessListener { visionText ->
                                        // Récupère le texte détecté
                                        val text = visionText.text.trim()
                                        // 3) Filtrage : texte suffisement long et différent du précédent
                                        if (text.length >= 3 && text != lastDisplayedText) {
                                            lastDisplayedText = text
                                            ocrText = text
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        // En cas d'erreur, log dans Logcat
                                        Log.e("OCR", "Erreur OCR", e)
                                    }
                                    .addOnCompleteListener {
                                        // Toujours fermer le proxy pour libérer la mémoire
                                        imageProxy.close()
                                    }
                            } ?: imageProxy.close() // Si pas d"image, ferme aussi
                        }
                    }

                // Détache tous les cas d'usage avant de binder
                cameraProvider.unbindAll()
                // Bind les use cases Preview + Analysis dans le cycle de vie
                cameraProvider.bindToLifecycle(
                    ctx as ComponentActivity,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )
            }

            // Ajoute le listener sur cameraProviderFuture
            cameraProviderFuture.addListener(listener, executor)

            onDispose {
                // Libère la caméra si l'effet est disposé
                if (cameraProviderFuture.isDone) {
                    cameraProviderFuture.get().unbindAll()
                }
            }
        }
    }

    // UI Compose finale : flux caméra + overlay du texte OCR
    Box(modifier = Modifier.fillMaxSize()) {
        // Intègre PreviewView (vue native) dans Compose
        AndroidView(
            factory = { context ->
                PreviewView(context).also { previewView = it }
            },
            modifier = Modifier.fillMaxSize()
        )
        // Affiche le texte OCR en surimpression en haut de l'écran
        Text(
            text = ocrText,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.8f),
            maxLines = 3
        )
    }
}
