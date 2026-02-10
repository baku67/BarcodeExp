package com.example.barcode.features.addItems

import android.graphics.Bitmap
import android.util.Log
import android.util.Size
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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.rememberAsyncImagePainter
import com.example.barcode.common.ui.components.HeaderBar
import com.example.barcode.common.ui.components.MonthWheelFormat
import com.example.barcode.common.ui.components.WheelDatePickerBottomSheet
import com.example.barcode.domain.models.AppIcon
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.delay
import java.nio.ByteBuffer
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

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

    // ✅ ROI (UI + OCR) — ici tu assumes vraiment “scan uniquement dans le cadre”
    val roiWidthFraction = 0.94f
    val roiAspect = 2.8f
    val roiCornerRadius = 18.dp
    val roiCenterYFraction = 0.40f // ✅ 0.50 = centre, 0.40 = plus haut

    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var detectedDate by remember { mutableStateOf("") }
    var detectedDateMs by remember { mutableStateOf<Long?>(null) }
    var detectedLocalDate by remember { mutableStateOf<LocalDate?>(null) } // ✅ source interne
    var showManualSheet by remember { mutableStateOf(false) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }

    var lastDetectedDate by remember { mutableStateOf("") }
    var frozen by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var lastAnalyzeStartedAt by remember { mutableStateOf(0L) }
    val analyzingHoldMs = 450L // ✅ ajuste (350–700)
    var lastAnalyseTime by remember { mutableStateOf(0L) }
    var cameraError by remember { mutableStateOf<String?>(null) }

    // ✅ ROI + anti-overlap => tu peux monter en FPS
    val minIntervalMs = 160L

    var boundPreview by remember { mutableStateOf<Preview?>(null) }
    var boundAnalysis by remember { mutableStateOf<ImageAnalysis?>(null) }


    val dateRegex = remember {
        // ** old value:
        // Regex("""\b(0[1-9]|[12][0-9]|3[01])\s*([\-\/\. ])\s*(0[1-9]|1[0-2])\s*\2\s*(202\d|203\d|204\d|2099|2[0-9]|3[0-9]|4[0-9]|99)\b""")
        // ** Matching DD/MM sans année (+ ajout manuel année actuelle après)
        Regex(
            """\b(0[1-9]|[12][0-9]|3[01])\s*([\-\/\. ])\s*(0[1-9]|1[0-2])(?:\s*\2\s*((?:202\d|203\d|204\d|2050|2[0-9]|3[0-9]|4[0-9]|50))|(?!\s*\2\s*(?:202\d|203\d|204\d|2050|2[0-9]|3[0-9]|4[0-9]|50)))\b"""
        )
    }

    val onRetry = {
        frozen = false
        isProcessing = false
        lastAnalyzeStartedAt = 0L
        lastDetectedDate = ""
        detectedDate = ""
        detectedDateMs = null
        lastAnalyseTime = 0L
    }

    val onValidate: () -> Unit = { detectedDateMs?.let { onValidated?.invoke(it) } }

    fun normalizeYear(twoOrFour: String?): Int? {
        // ✅ Si pas d’année => on assume l’année actuelle
        if (twoOrFour.isNullOrBlank()) return LocalDate.now().year

        return when (twoOrFour.length) {
            2 -> (2000 + twoOrFour.toInt()).takeIf { it in 2020..2050 }
            4 -> twoOrFour.toInt().takeIf { it in 2020..2050 }
            else -> null
        }
    }


    fun parseToLocalDate(day: String, month: String, year: Int): LocalDate? = try {
        LocalDate.of(year, month.toInt(), day.toInt())
    } catch (_: Exception) { null }

    fun localDateToEpochMs(ld: LocalDate): Long =
        ld.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    val content: @Composable (Modifier) -> Unit = { mod ->
        ScanDlcContent(
            modifier = mod,
            productName = productName,
            productBrand = productBrand,
            productImageUrl = productImageUrl,
            previewView = previewView,
            onPreviewViewChange = { previewView = it },
            detectedDate = detectedDate,
            detectedDateMs = detectedDateMs,
            onRetry = onRetry,
            onValidate = onValidate,
            cameraError = cameraError,
            roiWidthFraction = roiWidthFraction,
            roiAspect = roiAspect,
            roiCornerRadius = roiCornerRadius,
            isProcessing = isProcessing,
            frozen = frozen,
            analyzeStartedAt = lastAnalyzeStartedAt, // ✅ AJOUT
            analyzingHoldMs = analyzingHoldMs,        // ✅ AJOUT
            onOpenManualEntry = { showManualSheet = true },
            roiCenterYFraction = roiCenterYFraction,
        )
    }

    if (showHeader) {
        Scaffold(
            topBar = { HeaderBar(title = "FrigoZen", "Scan d'une date", AppIcon.Vector(Icons.Filled.AddCircle)) }
        ) { inner -> content(modifier.fillMaxSize().padding(inner)) }
    } else {
        content(modifier.fillMaxSize())
    }

    if (showManualSheet) {
        WheelDatePickerBottomSheet(
            initialDate = detectedLocalDate,
            monthFormat = MonthWheelFormat.TwoDigits,
            onDismiss = {
                showManualSheet = false
                if (detectedDateMs == null) frozen = false
            },
            onConfirm = { ld ->
                detectedLocalDate = ld
                detectedDate = ld.format(dateFormatter)
                detectedDateMs = localDateToEpochMs(ld)
                lastDetectedDate = detectedDate
                frozen = true
                showManualSheet = false
            }
        )
    }




    DisposableEffect(previewView, lifecycleOwner) {
        val view = previewView ?: return@DisposableEffect onDispose { }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

        // ✅ OCR sur thread dédié
        val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()
        val mainExecutor = ContextCompat.getMainExecutor(ctx)

        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        // ✅ Buffer RGBA réutilisable (évite allocations par frame)
        val rgbaBuffer = RgbaFrameBuffer()

        val listener = Runnable {
            try {
                val cameraProvider = cameraProviderFuture.get()
                cameraProvider.unbindAll()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(view.surfaceProvider)
                }

                val analysisBuilder = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setTargetResolution(Size(1280, 720))

                // ✅ Tente RGBA_8888 (CameraX récent) => ROI strict + perf
                analysisBuilder.tryEnableRgba8888Output() // Commenter pour tester fallback FullView

                val analysis = analysisBuilder.build().also { ia ->
                    ia.setAnalyzer(analysisExecutor) { imageProxy ->
                        try {
                            if (frozen) { imageProxy.close(); return@setAnalyzer }
                            if (isProcessing) { imageProxy.close(); return@setAnalyzer }

                            val now = System.currentTimeMillis()
                            if (now - lastAnalyseTime < minIntervalMs) {
                                imageProxy.close()
                                return@setAnalyzer
                            }
                            lastAnalyseTime = now

                            val rotation = imageProxy.imageInfo.rotationDegrees

                            // ✅ ROI strict si RGBA dispo
                            val bmp = rgbaBuffer.toBitmapOrNull(imageProxy)
                            if (bmp != null) {
                                val input = bmp.cropForRoi(
                                    rotationDegrees = rotation,
                                    roiWidthFraction = roiWidthFraction,
                                    roiAspect = roiAspect,
                                    roiCenterYFraction = roiCenterYFraction
                                )

                                val startedAt = System.currentTimeMillis()
                                mainExecutor.execute {
                                    lastAnalyzeStartedAt = startedAt
                                    isProcessing = true
                                }

                                recognizer.process(input)
                                    .addOnSuccessListener(mainExecutor) { visionText ->
                                        dateRegex.find(visionText.text)?.let { m ->
                                            val (d, sep, mo, yRaw) = m.destructured

                                            // ✅ yRaw peut être vide => année actuelle
                                            val y = normalizeYear(yRaw) ?: return@addOnSuccessListener

                                            val ld = parseToLocalDate(d, mo, y) ?: return@addOnSuccessListener
                                            val normalizedUi = ld.format(dateFormatter)

                                            // ✅ dédoublonnage cohérent (peu importe le séparateur détecté)
                                            if (normalizedUi == lastDetectedDate) return@addOnSuccessListener

                                            detectedLocalDate = ld
                                            detectedDate = normalizedUi
                                            detectedDateMs = localDateToEpochMs(ld)
                                            lastDetectedDate = normalizedUi
                                            frozen = true
                                        }
                                    }
                                    .addOnFailureListener(mainExecutor) { e ->
                                        Log.e("OCR", "Erreur", e)
                                    }
                                    .addOnCompleteListener(mainExecutor) {
                                        isProcessing = false
                                        imageProxy.close()
                                    }

                                return@setAnalyzer

                            }

                            // ⚠️ fallback (ROI non strict) si RGBA pas dispo
                            val mediaImage = imageProxy.image ?: run { imageProxy.close(); return@setAnalyzer }
                            val input = InputImage.fromMediaImage(mediaImage, rotation)

                            val startedAt = System.currentTimeMillis()
                            mainExecutor.execute {
                                lastAnalyzeStartedAt = startedAt
                                isProcessing = true
                            }

                            recognizer.process(input)
                                .addOnSuccessListener(mainExecutor) { visionText ->
                                    dateRegex.find(visionText.text)?.let { m ->
                                        val (d, sep, mo, yRaw) = m.destructured

                                        // ✅ yRaw peut être vide => année actuelle
                                        val y = normalizeYear(yRaw) ?: return@addOnSuccessListener

                                        val ld = parseToLocalDate(d, mo, y) ?: return@addOnSuccessListener
                                        val normalizedUi = ld.format(dateFormatter)

                                        // ✅ dédoublonnage cohérent (peu importe le séparateur détecté)
                                        if (normalizedUi == lastDetectedDate) return@addOnSuccessListener

                                        detectedLocalDate = ld
                                        detectedDate = normalizedUi
                                        detectedDateMs = localDateToEpochMs(ld)
                                        lastDetectedDate = normalizedUi
                                        frozen = true
                                    }
                                }
                                .addOnFailureListener(mainExecutor) { e ->
                                    Log.e("OCR", "Erreur", e)
                                }
                                .addOnCompleteListener(mainExecutor) {
                                    isProcessing = false
                                    imageProxy.close()
                                }

                        } catch (_: Throwable) {
                            mainExecutor.execute { isProcessing = false }
                            try { imageProxy.close() } catch (_: Exception) {}
                        }
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

        cameraProviderFuture.addListener(listener, mainExecutor)

        onDispose {
            try {
                if (cameraProviderFuture.isDone) {
                    val provider = cameraProviderFuture.get()
                    val toUnbind = listOfNotNull(boundAnalysis, boundPreview).toTypedArray()
                    if (toUnbind.isNotEmpty()) provider.unbind(*toUnbind)
                    boundAnalysis = null
                    boundPreview = null
                }
            } catch (_: Exception) {}

            try { recognizer.close() } catch (_: Exception) {}
            try { analysisExecutor.shutdown() } catch (_: Exception) {}
        }
    }
}

@Composable
private fun ScanDlcContent(
    modifier: Modifier,
    productName: String?,
    productBrand: String?,
    productImageUrl: String?,
    previewView: PreviewView?,
    onPreviewViewChange: (PreviewView?) -> Unit,
    detectedDate: String,
    detectedDateMs: Long?,
    onRetry: () -> Unit,
    onValidate: () -> Unit,
    cameraError: String?,
    roiWidthFraction: Float,
    roiAspect: Float,
    roiCornerRadius: Dp,
    roiCenterYFraction: Float, // ✅ AJOUT
    isProcessing: Boolean,
    frozen: Boolean,
    analyzeStartedAt: Long,
    analyzingHoldMs: Long,
    onOpenManualEntry: () -> Unit,
)
 {
    Box(modifier = modifier) {

        AndroidView(
            factory = { c -> PreviewView(c).also { onPreviewViewChange(it) } },
            modifier = Modifier.fillMaxSize(),
            onRelease = { onPreviewViewChange(null) }
        )

        // ✅ Overlay “honnête” : on OCR dans cette zone (si RGBA dispo)
        ExpiryRoiOverlay(
            modifier = Modifier.fillMaxSize(),
            roiWidthFraction = roiWidthFraction,
            roiAspect = roiAspect,
            cornerRadius = roiCornerRadius,
            roiCenterYFraction = roiCenterYFraction, // ✅ AJOUT
            isProcessing = isProcessing,
            frozen = frozen,
            hasResult = detectedDateMs != null && detectedDate.isNotBlank(),
            analyzeStartedAt = analyzeStartedAt, // ✅ utilise le param
            analyzingHoldMs = analyzingHoldMs    // ✅
        )

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

        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .wrapContentHeight()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!productImageUrl.isNullOrBlank()) {
                    Image(
                        painter = rememberAsyncImagePainter(productImageUrl),
                        contentDescription = null,
                        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(Modifier.width(12.dp))
                }

                Column(Modifier.weight(1f)) {
                    Text(text = productName ?: "(sans nom)", fontWeight = FontWeight.SemiBold)

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
                        color = if (hasDate) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { onOpenManualEntry() }) {
                    Text(if (detectedDateMs == null) "Saisir manuellement" else "Modifier")
                }
            }

            Divider(thickness = 1.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            Row(modifier = Modifier.height(52.dp).fillMaxWidth()) {
                Button(
                    onClick = onRetry,
                    modifier = Modifier.weight(1f).fillMaxSize(),
                    shape = RoundedCornerShape(bottomStart = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = SolidColor(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
                    )
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Re-try", fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = onValidate,
                    enabled = detectedDateMs != null,
                    modifier = Modifier.weight(1f).fillMaxSize(),
                    shape = RoundedCornerShape(bottomEnd = 16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Valider", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}


@Composable
private fun ExpiryRoiOverlay(
    modifier: Modifier = Modifier,
    roiWidthFraction: Float,
    roiAspect: Float,
    cornerRadius: Dp,
    roiCenterYFraction: Float, // ✅ doit exister
    isProcessing: Boolean,
    frozen: Boolean,
    hasResult: Boolean,
    analyzeStartedAt: Long,
    analyzingHoldMs: Long
) {
    val now = System.currentTimeMillis()
    val showAnalyzing = isProcessing || (!hasResult && (now - analyzeStartedAt) < analyzingHoldMs)

    val onePx = with(LocalDensity.current) { 1f.toDp() }
    val overlayColor = Color.Black.copy(alpha = 0.42f)
    val bracketColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.92f)
    val cornerLen = 30.dp
    val cornerInset = 10.dp
    val scanInset = 14.dp
    val safePad = 16.dp

    val transition = rememberInfiniteTransition(label = "expiryScanLine")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "t"
    )

    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current

        // ✅ ROI en Dp (1 seule source de vérité)
        val roiW = maxWidth * roiWidthFraction
        val roiH = roiW / roiAspect
        val left = (maxWidth - roiW) / 2f

        val centerY = maxHeight * roiCenterYFraction
        val topUnclamped = centerY - (roiH / 2f)
        val top = topUnclamped.coerceIn(
            safePad,
            (maxHeight - safePad - roiH).coerceAtLeast(safePad)
        )

        // ✅ Canvas : on dessine avec EXACTEMENT la même géométrie
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        ) {
            val leftPx = with(density) { left.toPx() }
            val topPx = with(density) { top.toPx() }
            val wPx = with(density) { roiW.toPx() }
            val hPx = with(density) { roiH.toPx() }

            val rect = Rect(leftPx, topPx, leftPx + wPx, topPx + hPx)

            drawRect(overlayColor)

            val r = with(density) { cornerRadius.toPx() }
            drawRoundRect(
                color = Color.Transparent,
                topLeft = rect.topLeft,
                size = rect.size,
                cornerRadius = CornerRadius(r, r),
                blendMode = BlendMode.Clear
            )

            if (!frozen) {
                val y = rect.top + rect.height * t
                val insetPx = with(density) { scanInset.toPx() }
                drawLine(
                    color = bracketColor.copy(alpha = if (showAnalyzing) 0.55f else 0.35f),
                    start = Offset(rect.left + insetPx, y),
                    end = Offset(rect.right - insetPx, y),
                    strokeWidth = with(density) { onePx.toPx() },
                    cap = StrokeCap.Round
                )
            }

            val sw = with(density) { onePx.toPx() }
            val len = with(density) { cornerLen.toPx() }
            val inset = with(density) { cornerInset.toPx() }

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
        }

        // ✅ UI (Analyse/Place la date) centrée DANS le ROI (plus sur tout l'écran)
        Box(
            modifier = Modifier
                .offset(x = left, y = top)
                .size(roiW, roiH)
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.18f))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .widthIn(min = 220.dp)
                ) {
                    Text(
                        text = when {
                            hasResult -> "Date détectée ✅"
                            showAnalyzing -> "Analyse…"
                            else -> "Place la date imprimée dans le cadre"
                        },
                        color = Color.White.copy(alpha = 0.92f),
                        fontStyle = FontStyle.Italic,
                        textAlign = TextAlign.Center,
                        fontSize = 15.sp,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                CircularProgressIndicator(
                    modifier = Modifier
                        .size(20.dp)
                        .alpha(if (showAnalyzing && !hasResult) 1f else 0f),
                    strokeWidth = 2.dp
                )
            }
        }
    }
}



// ---------- ROI helpers (RGBA strict) ----------

class RgbaFrameBuffer {
    private var packed: ByteArray = ByteArray(0)
    private var rowTmp: ByteArray = ByteArray(0)
    private var byteBuffer: ByteBuffer? = null
    private var bitmap: Bitmap? = null

    fun toBitmapOrNull(imageProxy: androidx.camera.core.ImageProxy): Bitmap? {
        if (imageProxy.planes.size != 1) return null

        val plane = imageProxy.planes[0]
        val width = imageProxy.width
        val height = imageProxy.height
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride

        if (pixelStride != 4) return null

        val needed = width * height * 4
        if (packed.size < needed) {
            packed = ByteArray(needed)
            byteBuffer = ByteBuffer.wrap(packed)
        }
        if (rowTmp.size < rowStride) rowTmp = ByteArray(rowStride)

        val buf = plane.buffer
        buf.rewind()

        if (rowStride == width * 4) {
            buf.get(packed, 0, needed)
        } else {
            for (row in 0 until height) {
                val rowStart = row * rowStride
                buf.position(rowStart)
                buf.get(rowTmp, 0, rowStride)
                val outStart = row * width * 4
                System.arraycopy(rowTmp, 0, packed, outStart, width * 4)
            }
        }

        val bmp = bitmap
        val out = if (bmp == null || bmp.width != width || bmp.height != height) {
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap = it }
        } else bmp

        val bb = byteBuffer ?: return null
        bb.rewind()
        out.copyPixelsFromBuffer(bb)

        return out
    }
}


private fun Bitmap.cropForRoi(
    rotationDegrees: Int,
    roiWidthFraction: Float,
    roiAspect: Float,
    roiCenterYFraction: Float
): InputImage {
    val rawW = this.width
    val rawH = this.height

    // Dimensions une fois "upright" (après rotationDegrees)
    val uprightW = if (rotationDegrees == 90 || rotationDegrees == 270) rawH else rawW
    val uprightH = if (rotationDegrees == 90 || rotationDegrees == 270) rawW else rawH

    val roiW = (uprightW * roiWidthFraction).toInt().coerceAtLeast(1)
    val roiH = (roiW / roiAspect).toInt().coerceAtLeast(1)

    val centerXu = uprightW / 2f
    val centerYu = (uprightH * roiCenterYFraction).coerceIn(0f, uprightH.toFloat())

    val leftU = (centerXu - roiW / 2f).toInt().coerceIn(0, (uprightW - roiW).coerceAtLeast(0))
    val topU = (centerYu - roiH / 2f).toInt().coerceIn(0, (uprightH - roiH).coerceAtLeast(0))

    // Map rect "upright" -> rect "raw" (sans faire tourner le bitmap)
    val (leftR, topR, wR, hR) = when (rotationDegrees) {
        0 -> Quad(leftU, topU, roiW, roiH)
        180 -> Quad(rawW - (leftU + roiW), rawH - (topU + roiH), roiW, roiH)
        90 -> {
            // rotation clockwise 90 pour être upright
            // rawX = uprightY, rawY = rawH - (uprightX + width)
            Quad(
                leftR = topU,
                topR = rawH - (leftU + roiW),
                wR = roiH,
                hR = roiW
            )
        }
        270 -> {
            // rotation clockwise 270 pour être upright
            // rawX = rawW - (uprightY + height), rawY = uprightX
            Quad(
                leftR = rawW - (topU + roiH),
                topR = leftU,
                wR = roiH,
                hR = roiW
            )
        }
        else -> Quad(leftU, topU, roiW, roiH)
    }

    val safeLeft = leftR.coerceIn(0, (rawW - 1).coerceAtLeast(0))
    val safeTop = topR.coerceIn(0, (rawH - 1).coerceAtLeast(0))
    val safeW = wR.coerceIn(1, rawW - safeLeft)
    val safeH = hR.coerceIn(1, rawH - safeTop)

    val cropped = Bitmap.createBitmap(this, safeLeft, safeTop, safeW, safeH)
    return InputImage.fromBitmap(cropped, rotationDegrees)
}

private data class Quad(val leftR: Int, val topR: Int, val wR: Int, val hR: Int)


private fun ImageAnalysis.Builder.tryEnableRgba8888Output() {
    try {
        val builderCls = this::class.java
        val imageAnalysisCls = ImageAnalysis::class.java

        val setOutputImageFormat = builderCls.getMethod("setOutputImageFormat", Int::class.javaPrimitiveType)
        val rgbaField = imageAnalysisCls.getField("OUTPUT_IMAGE_FORMAT_RGBA_8888")
        val rgbaValue = rgbaField.getInt(null)

        setOutputImageFormat.invoke(this, rgbaValue)
    } catch (_: Throwable) {
        // CameraX trop ancien => ROI strict sans conversion YUV (coûteuse)
    }
}
