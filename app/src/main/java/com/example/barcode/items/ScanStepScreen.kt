package com.example.barcode.items

import androidx.compose.runtime.Composable
import com.example.barcode.OpenFoodFacts.ProductInfo
import com.example.barcode.ui.CameraOcrBarCodeScreen

@Composable
fun ScanStepScreen(
    onValidated: (product: ProductInfo, barcode: String) -> Unit,
    onCancel: () -> Unit
) {
    // Réutilise l’écran caméra existant via un callback
    CameraOcrBarCodeScreen(onValidated = onValidated)
}