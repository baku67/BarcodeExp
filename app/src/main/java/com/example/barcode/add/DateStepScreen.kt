package com.example.barcode.add

import androidx.compose.runtime.Composable
import com.example.barcode.ui.CameraDateOcrScreen

@Composable
fun DateStepScreen(
    onValidated: (expiryMs: Long) -> Unit,
    onCancel: () -> Unit
) {
    CameraDateOcrScreen(
        onValidated = onValidated,
        onCancel = onCancel
    )
}