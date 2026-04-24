package com.example.barcode.features.addItems.manual

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.barcode.features.addItems.AddItemDraft
import com.example.barcode.features.addItems.AddItemStepScaffold
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun ManualLeftoversDetailsStepScreen(
    draft: AddItemDraft,
    onMetaChange: (String) -> Unit,
    onConfirm: (dishName: String, expiryMs: Long?, metaJson: String) -> Unit,
    onBack: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current

    val taxonomy = rememberManualTaxonomy()

    val typeCode = draft.manualTypeCode
    val subtypeCode = draft.manualSubtypeCode

    val typeMeta = typeCode?.let { taxonomy?.typeMeta(it) }
    val subtypeMeta = subtypeCode?.let { taxonomy?.subtypeMeta(it) }

    // Header = SubType (fallback = Type). Si rien, on force une valeur "Restes".
    val headerTitle = subtypeMeta?.title ?: typeMeta?.title ?: "Restes / Tupperware"
    val headerImageResId = drawableId(context, subtypeMeta?.image ?: typeMeta?.image)
    val headerPaletteCode = typeCode ?: subtypeMeta?.parentCode ?: "LEFTOVERS"

    var dishName by rememberSaveable { mutableStateOf(draft.name ?: "Restes") }

    // --- Toggles
    var containsMeat by rememberSaveable { mutableStateOf(false) }
    var containsCream by rememberSaveable { mutableStateOf(false) }
    var containsRice by rememberSaveable { mutableStateOf(false) }
    var containsFish by rememberSaveable { mutableStateOf(false) }
    var containsEggs by rememberSaveable { mutableStateOf(false) }

    // --- Infos "plat de restes"
    var portionsText by rememberSaveable { mutableStateOf("1") }
    var notes by rememberSaveable { mutableStateOf("") }

    // --- Date de cuisson (simple & robuste)
    var cookedOffsetDays by rememberSaveable { mutableIntStateOf(0) } // 0=today, 1=yesterday, 2=2days...
    val cookedAtMs = remember(cookedOffsetDays) { startOfDayMs(offsetDaysAgo = cookedOffsetDays) }

    // --- DLC conseillée
    val suggestedDays = remember(containsMeat, containsCream, containsRice) {
        when {
            containsMeat -> 2
            containsRice -> 2
            containsCream -> 2
            else -> 3
        }
    }
    var storageDays by rememberSaveable { mutableIntStateOf(suggestedDays) }

    LaunchedEffect(suggestedDays) {
        storageDays = suggestedDays
    }

    val expiryMs = remember(cookedAtMs, storageDays) {
        cookedAtMs + storageDays.toLong() * 24L * 60L * 60L * 1000L
    }

    val metaJson = remember(
        cookedAtMs,
        storageDays,
        containsMeat,
        containsCream,
        containsRice,
        containsFish,
        containsEggs,
        portionsText,
        notes
    ) {
        buildLeftoversMetaJson(
            cookedAtMs = cookedAtMs,
            storageDays = storageDays,
            containsMeat = containsMeat,
            containsCream = containsCream,
            containsRice = containsRice,
            containsFish = containsFish,
            containsEggs = containsEggs,
            portions = portionsText.toIntOrNull(),
            notes = notes.ifBlank { null }
        )
    }

    LaunchedEffect(metaJson) {
        onMetaChange(metaJson)
    }

    AddItemStepScaffold(
        step = 2,
        totalSteps = 2,
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
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                val scrollState = rememberScrollState()
                val showTopScrim by remember { derivedStateOf { scrollState.value > 0 } }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = dishName,
                                onValueChange = { dishName = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Nom du plat") },
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = portionsText,
                                onValueChange = { portionsText = it.filter { ch -> ch.isDigit() }.take(2) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Portions restantes") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = notes,
                                onValueChange = { notes = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Notes") },
                                minLines = 2
                            )
                        }

                        HorizontalDivider()

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Date de cuisson / préparation",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ChipLike(
                                    label = "Aujourd’hui",
                                    selected = cookedOffsetDays == 0,
                                    onClick = { cookedOffsetDays = 0 }
                                )
                                ChipLike(
                                    label = "Hier",
                                    selected = cookedOffsetDays == 1,
                                    onClick = { cookedOffsetDays = 1 }
                                )
                                ChipLike(
                                    label = "Avant-hier",
                                    selected = cookedOffsetDays == 2,
                                    onClick = { cookedOffsetDays = 2 }
                                )
                            }

                            Text(
                                text = "Cuisson : ${formatDate(cookedAtMs)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "DLC estimée",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { storageDays = (storageDays - 1).coerceAtLeast(1) }) {
                                    Text("–", style = MaterialTheme.typography.titleLarge)
                                }
                                Text(
                                    text = "J + $storageDays  →  ${formatDate(expiryMs)}",
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                IconButton(onClick = { storageDays = (storageDays + 1).coerceAtMost(10) }) {
                                    Text("+", style = MaterialTheme.typography.titleLarge)
                                }
                            }

                            Text(
                                text = "Astuce: viande/riz/crème = plutôt J+2.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        HorizontalDivider()

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Composition",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )

                            ToggleRow("Contient de la viande", containsMeat) { containsMeat = it }
                            ToggleRow("Contient de la crème", containsCream) { containsCream = it }
                            ToggleRow("Contient du riz", containsRice) { containsRice = it }
                            ToggleRow("Contient du poisson", containsFish) { containsFish = it }
                            ToggleRow("Contient des œufs", containsEggs) { containsEggs = it }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    if (showTopScrim) {
                        TopEdgeFadeScrim(
                            modifier = Modifier.align(Alignment.TopCenter),
                            height = 18.dp
                        )
                    }
                }

                Button(
                    onClick = { onConfirm(dishName.ifBlank { "Restes" }, expiryMs, metaJson) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Ajouter")
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ChipLike(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    TextButton(onClick = onClick) {
        Text(
            text = if (selected) "• $label" else label,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun buildLeftoversMetaJson(
    cookedAtMs: Long,
    storageDays: Int,
    containsMeat: Boolean,
    containsCream: Boolean,
    containsRice: Boolean,
    containsFish: Boolean,
    containsEggs: Boolean,
    portions: Int?,
    notes: String?
): String {
    val o = JSONObject()
    o.put("kind", "leftovers")
    o.put("cookedAt", cookedAtMs)
    o.put("storageDays", storageDays)

    o.put("containsMeat", containsMeat)
    o.put("containsCream", containsCream)
    o.put("containsRice", containsRice)
    o.put("containsFish", containsFish)
    o.put("containsEggs", containsEggs)

    if (portions != null) o.put("portions", portions)
    if (notes != null) o.put("notes", notes)

    return o.toString()
}

private fun startOfDayMs(offsetDaysAgo: Int): Long {
    val zone = ZoneId.systemDefault()
    val d = LocalDate.now(zone).minusDays(offsetDaysAgo.toLong())
    return d.atStartOfDay(zone).toInstant().toEpochMilli()
}

private fun formatDate(ms: Long): String {
    val zone = ZoneId.systemDefault()
    val d = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()
    return "%02d/%02d/%04d".format(d.dayOfMonth, d.monthValue, d.year)
}
