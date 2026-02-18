package com.example.barcode.features.addItems.manual

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext

@Composable
fun rememberManualTaxonomy(): ManualTaxonomy? {
    val appCtx = LocalContext.current.applicationContext

    val taxonomy by produceState<ManualTaxonomy?>(
        initialValue = ManualTaxonomyRepository.peek(),
        key1 = appCtx
    ) {
        if (value == null) {
            value = ManualTaxonomyRepository.get(appCtx)
        }
    }

    return taxonomy
}
