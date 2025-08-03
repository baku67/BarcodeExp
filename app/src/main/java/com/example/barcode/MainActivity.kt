package com.example.barcode

import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import androidx.camera.core.Preview
import androidx.camera.core.ExperimentalGetImage
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Demande runtime de la permission caméra
        val requestPerm = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (!granted) finish()
        }
        requestPerm.launch(Manifest.permission.CAMERA)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CameraOcrView()
                }
            }
        }
    }
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
fun CameraOcrView() {
    val ctx = LocalContext.current
    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    // 1) State pour stocker le texte OCR
    var ocrText by remember { mutableStateOf("") }

    // 2) On lance CameraX + ML Kit dès que previewView != null
    previewView?.let { view ->
        DisposableEffect(view) {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            val executor = ContextCompat.getMainExecutor(ctx)
            val recognizer = TextRecognition.getClient(
                TextRecognizerOptions.DEFAULT_OPTIONS
            )

            val listener = Runnable {
                val cameraProvider = cameraProviderFuture.get()

                // Preview
                val preview = Preview.Builder()
                    .build()
                    .also { it.setSurfaceProvider(view.surfaceProvider) }

                // Analyse d’image + OCR
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { ia ->
                        ia.setAnalyzer(executor) { imageProxy ->
                            imageProxy.image?.let { mediaImage ->
                                val inputImg = InputImage.fromMediaImage(
                                    mediaImage,
                                    imageProxy.imageInfo.rotationDegrees
                                )
                                recognizer.process(inputImg)
                                    .addOnSuccessListener { visionText ->
                                        // 3) Met à jour le state avec le texte lu
                                        ocrText = visionText.text
                                    }
                                    .addOnFailureListener { it.printStackTrace() }
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

            onDispose {
                if (cameraProviderFuture.isDone) {
                    cameraProviderFuture.get().unbindAll()
                }
            }
        }
    }

    // 4) L’UI : Preview + superposition du texte OCR
    Box(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { c ->
                PreviewView(c).also { previewView = it }
            },
            modifier = Modifier.fillMaxSize()
        )
        // Texte en overlay, semi-transparent, en haut de l'écran
        Text(
            text = ocrText,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.8f),
            maxLines = 5
        )
    }
}