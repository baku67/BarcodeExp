package com.example.barcode.features.addItems.manual

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

@Composable
fun rememberManualTaxonomy(): ManualTaxonomy? {
    val context = LocalContext.current.applicationContext

    var taxonomy by remember { mutableStateOf(ManualTaxonomyRepository.peek()) }

    LaunchedEffect(context) {
        // Si déjà préchargé → load() rend quasi instantanément
        if (taxonomy == null) taxonomy = ManualTaxonomyRepository.load(context)
    }

    return taxonomy
}
