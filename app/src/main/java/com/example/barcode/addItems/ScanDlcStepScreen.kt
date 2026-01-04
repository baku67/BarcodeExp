package com.example.barcode.addItems

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.barcode.ui.ScanDlcScreen

@Composable
fun ScanDlcStepScreen(
    productName: String?,
    productBrand: String?,
    productImageUrl: String?,
    onValidated: (expiryMs: Long) -> Unit,
    onBack: () -> Unit,
    onCancel: () -> Unit
) {
    AddItemStepScaffold(
        step = 2,
        onBack = onBack,
        onCancel = onCancel
    ) { innerPadding ->
        ScanDlcScreen(
            productName = productName,
            productBrand = productBrand,
            productImageUrl = productImageUrl,
            onValidated = onValidated,
            showHeader = false, // on enlève le Header interne car page d'étape
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )
    }
}
