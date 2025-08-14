package com.example.barcode.add

import androidx.compose.runtime.Composable
import com.example.barcode.ui.CameraDateOcrScreen

@Composable
fun DateStepScreen(
    productName: String?,
    productBrand: String?,
    productImageUrl: String?,
    onValidated: (expiryMs: Long) -> Unit,
    onCancel: () -> Unit
) {
    CameraDateOcrScreen(
        productName = productName,
        productBrand = productBrand,
        productImageUrl = productImageUrl,
        onValidated = onValidated,
        onCancel = onCancel
    )
}