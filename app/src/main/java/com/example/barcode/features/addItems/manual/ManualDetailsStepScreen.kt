package com.example.barcode.features.addItems.manual

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.barcode.common.ui.components.MonthWheelFormat
import com.example.barcode.common.ui.components.WheelDatePickerDialog
import com.example.barcode.core.UserPreferencesStore
import com.example.barcode.domain.models.ThemeMode
import com.example.barcode.features.addItems.AddItemDraft
import com.example.barcode.features.addItems.AddItemStepScaffold
import kotlinx.coroutines.flow.map
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

    val taxonomy = rememberManualTaxonomy()

    val prefsStore = remember(context) { UserPreferencesStore(context) }

    val themeMode by prefsStore.preferences
        .map { it.theme }
        .collectAsState(initial = ThemeMode.SYSTEM)

    val useDarkTheme = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val typeCode = draft.manualTypeCode
    val subtypeCode = draft.manualSubtypeCode

    val typeMeta = typeCode?.let { taxonomy?.typeMeta(it) }
    val subtypeMeta = subtypeCode?.let { taxonomy?.subtypeMeta(it) }

    // Header = SubType (fallback = Type si pas de subtype)
    val headerTitle = subtypeMeta?.title ?: typeMeta?.title ?: ""
    val headerImageResId = drawableId(context, subtypeMeta?.image ?: typeMeta?.image)

    // Palette fallback (si pas de gradient sur le subtype)
    val headerPaletteCode = typeCode ?: subtypeMeta?.parentCode ?: ""

    // ✅ Couleurs item (SUBTYPE) pour gradient background + gradient titre (comme GoodToKnowScreen)
    // (si absent => le header continue à utiliser le paletteForType() comme avant)
    val headerGradientColors: List<Color>? = remember(subtypeMeta) {
        val hexes = subtypeMeta?.gradient?.colors?.take(3) ?: return@remember null
        val parsed = hexes.mapNotNull { hex ->
            runCatching { Color(AndroidColor.parseColor(hex)) }.getOrNull()
        }
        parsed.takeIf { it.size >= 3 }
    }

    var name by rememberSaveable(draft.name, typeCode, subtypeCode) {
        mutableStateOf(draft.name.orEmpty())
    }

    val autoName = (subtypeMeta?.title ?: typeMeta?.title).orEmpty()

    LaunchedEffect(taxonomy, typeCode, subtypeCode) {
        // ✅ ne remplit que si l'user n’a rien tapé et que draft.name est vide
        if (draft.name.isNullOrBlank() && name.isBlank() && autoName.isNotBlank()) {
            name = autoName
        }
    }

    var brand by remember(draft.brand) { mutableStateOf(draft.brand.orEmpty()) }

    var expiryMs by remember(draft.expiryDate) { mutableStateOf(draft.expiryDate) }
    var showWheel by remember { mutableStateOf(false) }

    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val expiryLabel = remember(expiryMs) {
        expiryMs?.let { dateFormatter.format(Date(it)) } ?: "Choisir une date"
    }

    AddItemStepScaffold(
        step = 3,
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
                    palette = paletteForType(headerPaletteCode),
                    // ✅ override pour reprendre le tri-color du SUBTYPE
                    gradientColors = headerGradientColors,
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val scrollState = rememberScrollState()
                val showTopScrim by remember { derivedStateOf { scrollState.value > 0 } }

                Box(modifier = Modifier.weight(1f)) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState),
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

                        Spacer(Modifier.height(12.dp))
                    }

                    if (showTopScrim) {
                        TopEdgeFadeScrim(
                            modifier = Modifier.align(Alignment.TopCenter),
                            height = 18.dp
                        )
                    }
                }

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
                    Text("Ajouter")
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
                showExpiredHint = true,
                useDarkTheme = useDarkTheme,
            )
        }
    }
}
