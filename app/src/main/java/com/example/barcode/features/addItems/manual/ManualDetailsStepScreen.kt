package com.example.barcode.features.addItems.manual

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.barcode.features.addItems.AddItemDraft
import com.example.barcode.features.addItems.AddItemStepScaffold

@Composable
fun ManualDetailsStepScreen(
    draft: AddItemDraft,
    onNext: (name: String, brand: String?) -> Unit,
    onBack: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val taxonomy = remember(context) { ManualTaxonomyRepository.get(context) }

    val typeCode = draft.manualTypeCode
    val subtypeCode = draft.manualSubtypeCode

    val typeMeta = typeCode?.let { taxonomy.typeMeta(it) }
    val subtypeMeta = subtypeCode?.let { taxonomy.subtypeMeta(it) }

    // ✅ Header = SubType (fallback = Type si pas de subtype)
    val headerTitle = subtypeMeta?.title ?: typeMeta?.title ?: ""
    val headerImageResId = drawableId(context, subtypeMeta?.image ?: typeMeta?.image)
    val headerPaletteCode = typeCode ?: subtypeMeta?.parentCode ?: ""

    val suggestedName = remember(draft.name, draft.manualTypeCode, draft.manualSubtypeCode) {
        draft.name
            ?: subtypeMeta?.title
            ?: typeMeta?.title
            ?: ""
    }

    var name by remember(draft.name, draft.manualTypeCode, draft.manualSubtypeCode) {
        mutableStateOf(suggestedName)
    }
    var brand by remember(draft.brand) { mutableStateOf(draft.brand.orEmpty()) }

    AddItemStepScaffold(
        step = 2,
        onBack = onBack,
        onCancel = onCancel
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding) // ✅ header collé sous la topbar
        ) {
            if (headerTitle.isNotBlank() && headerPaletteCode.isNotBlank()) {
                ManualSubtypeFullBleedHeader(
                    typeTitle = headerTitle,
                    typeImageResId = headerImageResId,
                    palette = paletteForType(headerPaletteCode)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nom") },
                    placeholder = { Text("ex: Blanc de poulet, Carottes, Omelette...") },
                    singleLine = true
                )

                Spacer(Modifier.weight(1f))

                Button(
                    onClick = {
                        val cleanedName = name.trim()
                        if (cleanedName.isNotEmpty()) {
                            onNext(cleanedName, brand.trim().ifBlank { null })
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = name.trim().isNotEmpty()
                ) {
                    Text("Continuer")
                }
            }
        }
    }
}
