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
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import coil.compose.AsyncImage
import java.net.URL

// Data class pour les infos produits
data class ProductInfo(
    val name: String,
    val brand: String,
    val imageUrl: String,
    val nutriScore: String
)

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
                        composable("dateOCR") { CameraDateOcrScreen() }
                        composable("barCodeOCR") { CameraOcrBarCodeScreen() }
                    }
                }
            }
        }
    }
}

// Écran d'accueil avec un bouton pour accéder à l'OCR
@Composable
fun HomeScreen(navController: NavHostController) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Button(onClick = { navController.navigate("barCodeOCR") }) {
                Text(text = "Scanner code-barres")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { navController.navigate("dateOCR") }) {
                Text(text = "OCR Dates")
            }
        }
    }
}

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

// Fonction suspend pour récupérer nom et marque du produit
suspend fun fetchProductInfo(code: String): ProductInfo =
    withContext(Dispatchers.IO) {
        return@withContext try {
            val json = URL("https://world.openfoodfacts.org/api/v0/product/$code.json")
                .openConnection().getInputStream().bufferedReader().use { it.readText() }
            val obj = JSONObject(json).getJSONObject("product")
            val name = obj.optString("product_name", "Inconnu")
            val brand = obj.optString("brands", "Inconnue")
            val imgUrl = obj.optString("image_url", "")
            // Récupère le nutri-score, champ nutrition_grade_fr ou nutrition_grades
            val nutri = obj.optString("nutrition_grade_fr", "").ifEmpty {
                // fallback sur liste tags Nutriscore
                obj.optJSONArray("nutrition_grades_tags")?.optString(0)?.substringAfterLast('-') ?: ""
            }
            ProductInfo(name, brand, imgUrl, nutri)
        } catch (e: Exception) {
            Log.e("BARCODE", "Erreur API", e)
            ProductInfo("Erreur", "Erreur", "", "?")
        }
    }