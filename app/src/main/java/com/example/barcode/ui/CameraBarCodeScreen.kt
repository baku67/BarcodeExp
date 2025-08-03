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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.barcode.model.ProductInfo
import com.example.barcode.util.fetchProductInfo
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch

// Composable pour scanner un code-barres et récupérer le nom du produit
@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
fun CameraOcrBarCodeScreen() {
    val ctx = LocalContext.current
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var scannedCode by remember { mutableStateOf("") }
    var lastScanned by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    var productInfo by remember { mutableStateOf(ProductInfo("", "", "", "")) }

    previewView?.let { view ->
        DisposableEffect(view) {
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
                            imageProxy.image?.let { mediaImage ->
                                val inputImg = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                scanner.process(inputImg)
                                    .addOnSuccessListener { barcodes ->
                                        // Prend le premier code non-nul
                                        val first = barcodes.firstOrNull { it.rawValue != null }?.rawValue
                                        if (first != null && first != lastScanned) {
                                            lastScanned = first
                                            scannedCode = first
                                            scope.launch {
                                                productInfo = fetchProductInfo(first)
                                            }

                                        }
                                    }
                                    .addOnFailureListener { e -> Log.e("BARCODE", "Erreur scan", e) }
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

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { c -> PreviewView(c).also { previewView = it } }, modifier = Modifier.fillMaxSize())
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .background(Color.Black)
                .padding(8.dp)
        ) {
            // Affiche l'image du produit si disponible
            if (productInfo.imageUrl.isNotEmpty()) {
                AsyncImage(
                    model = productInfo.imageUrl,
                    contentDescription = "Image du produit",
                    modifier = Modifier
                        .size(100.dp)
                        .padding(bottom = 8.dp)
                )
            }
            Text(text = "Code: $scannedCode", color = Color.White)
            Text(text = "Produit: ${productInfo.name}", color = Color.White)
            Text(text = "Marque: ${productInfo.brand}", color = Color.White)
            // Affiche le Nutri-score
            Text(text = "Nutri-Score: ${productInfo.nutriScore.uppercase()}", color = Color.White)
        }
    }
}