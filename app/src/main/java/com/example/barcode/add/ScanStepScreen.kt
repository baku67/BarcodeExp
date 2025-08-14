package com.example.barcode.add

import androidx.compose.runtime.Composable
import com.example.barcode.model.ProductInfo
import com.example.barcode.ui.CameraOcrBarCodeScreen

@Composable
fun ScanStepScreen(
    onValidated: (product: ProductInfo, barcode: String) -> Unit,
    onCancel: () -> Unit
) {
    // Réutilise l’écran caméra existant via un callback
    CameraOcrBarCodeScreen(onValidated = onValidated)
}