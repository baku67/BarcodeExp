package com.example.barcode.addItems

import androidx.compose.runtime.Composable
import com.example.barcode.ui.ScanDlcScreen

@Composable
fun ScanDlcStepScreen(
    productName: String?,
    productBrand: String?,
    productImageUrl: String?,
    onValidated: (expiryMs: Long) -> Unit,
    onCancel: () -> Unit
) {
    ScanDlcScreen(
        productName = productName,
        productBrand = productBrand,
        productImageUrl = productImageUrl,
        onValidated = onValidated,
        onCancel = onCancel
    )
}