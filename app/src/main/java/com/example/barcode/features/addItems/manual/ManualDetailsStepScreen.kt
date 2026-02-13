package com.example.barcode.features.addItems.manual

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.barcode.common.ui.components.MonthWheelFormat
import com.example.barcode.common.ui.components.WheelDatePickerDialog
import com.example.barcode.features.addItems.AddItemDraft
import com.example.barcode.features.addItems.AddItemStepScaffold
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ManualDetailsStepScreen(
    draft: AddItemDraft,
    onNext: (name: String, brand: String?, expiryMs: Long?) -> Unit,
    onBack: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val taxonomy = remember(context) { ManualTaxonomyRepository.get(context) }

    val typeCode = draft.manualTypeCode
    val subtypeCode = draft.manualSubtypeCode

    val typeMeta = typeCode?.let { taxonomy.typeMeta(it) }
    val subtypeMeta = subtypeCode?.let { taxonomy.subtypeMeta(it) }

    // Header = SubType (fallback = Type si pas de subtype)
    val headerTitle = subtypeMeta?.title ?: typeMeta?.title ?: ""
    val headerImageResId = drawableId(context, subtypeMeta?.image ?: typeMeta?.image)
    val headerPaletteCode = typeCode ?: subtypeMeta?.parentCode ?: ""

    val suggestedName = remember(draft.name, draft.manualTypeCode, draft.manualSubtypeCode) {
        draft.name ?: subtypeMeta?.title ?: typeMeta?.title ?: ""
    }

    var name by remember(draft.name, draft.manualTypeCode, draft.manualSubtypeCode) {
        mutableStateOf(suggestedName)
    }
    var brand by remember(draft.brand) { mutableStateOf(draft.brand.orEmpty()) }

    var expiryMs by remember(draft.expiryDate) { mutableStateOf(draft.expiryDate) }
    var showWheel by remember { mutableStateOf(false) }

    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val expiryLabel = remember(expiryMs) {
        expiryMs?.let { dateFormatter.format(Date(it)) } ?: "Choisir une date"
    }

    AddItemStepScaffold(
        step = 2,
        onBack = onBack,
        onCancel = onCancel
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding) // header collé sous topbar
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

                val storageLine = remember(
                    subtypeMeta?.title,
                    subtypeMeta?.storageDaysMin,
                    subtypeMeta?.storageDaysMax
                ) {
                    val label = subtypeMeta?.title?.lowercase(Locale.getDefault())
                    val min = subtypeMeta?.storageDaysMin
                    val max = subtypeMeta?.storageDaysMax

                    if (label == null || min == null) return@remember null

                    val daysText = when {
                        max != null && max != min -> "$min–$max jours"
                        else -> "$min jours"
                    }

                    "Temps de conservation conseillé pour $label : $daysText"
                }

                if (storageLine != null) {
                    Text(
                        text = storageLine!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // ✅ Date limite via WheelDatePicker
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Date limite (optionnel)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showWheel = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = expiryLabel,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (expiryMs != null) {
                            TextButton(onClick = { expiryMs = null }) {
                                Text("Effacer")
                            }
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                Button(
                    onClick = {
                        val cleanedName = name.trim()
                        if (cleanedName.isNotEmpty()) {
                            onNext(cleanedName, brand.trim().ifBlank { null }, expiryMs)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = name.trim().isNotEmpty()
                ) {
                    Text("Continuer")
                }
            }
        }

        if (showWheel) {
            WheelDatePickerDialog(
                initialMillis = expiryMs,
                onConfirm = { pickedMillis ->
                    expiryMs = pickedMillis
                    showWheel = false
                },
                onDismiss = { showWheel = false },
                title = "Date limite",
                monthFormat = MonthWheelFormat.ShortText,
                showExpiredHint = true
            )
        }
    }
}
