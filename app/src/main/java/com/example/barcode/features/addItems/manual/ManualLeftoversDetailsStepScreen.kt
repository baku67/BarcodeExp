package com.example.barcode.features.addItems.manual

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.barcode.features.addItems.AddItemDraft
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
    var dishName by rememberSaveable { mutableStateOf(draft.name ?: "Restes") }

    // --- Toggles "fake" demandés + quelques extras utiles
    var containsMeat by rememberSaveable { mutableStateOf(false) }
    var containsCream by rememberSaveable { mutableStateOf(false) }
    var containsRice by rememberSaveable { mutableStateOf(false) }

    var containsFish by rememberSaveable { mutableStateOf(false) }
    var containsEggs by rememberSaveable { mutableStateOf(false) }
    var spicy by rememberSaveable { mutableStateOf(false) }
    var toReheat by rememberSaveable { mutableStateOf(true) }
    var canFreeze by rememberSaveable { mutableStateOf(false) }

    // --- Infos "plat de restes"
    var portionsText by rememberSaveable { mutableStateOf("1") }
    var containerText by rememberSaveable { mutableStateOf("Tupperware") }
    var notes by rememberSaveable { mutableStateOf("") }

    // --- Date de cuisson (simple & robuste)
    var cookedOffsetDays by rememberSaveable { mutableIntStateOf(0) } // 0=today, 1=yesterday, 2=2days...
    val cookedAtMs = remember(cookedOffsetDays) { startOfDayMs(offsetDaysAgo = cookedOffsetDays) }

    // --- DLC conseillée (opinion: c’est mieux que demander une date précise à l’utilisateur pour des restes)
    val suggestedDays = remember(containsMeat, containsCream, containsRice) {
        // plus "risqué" => plus court
        when {
            containsMeat -> 2
            containsRice -> 2
            containsCream -> 2
            else -> 3
        }
    }
    var storageDays by rememberSaveable { mutableIntStateOf(suggestedDays) }

    LaunchedEffect(suggestedDays) {
        // on recale automatiquement si l’utilisateur n’a pas encore bricolé
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
        spicy,
        toReheat,
        canFreeze,
        portionsText,
        containerText,
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
            spicy = spicy,
            toReheat = toReheat,
            canFreeze = canFreeze,
            portions = portionsText.toIntOrNull(),
            container = containerText.ifBlank { null },
            notes = notes.ifBlank { null }
        )
    }

    LaunchedEffect(metaJson) {
        onMetaChange(metaJson)
    }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) { Text("Retour") }
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = onCancel) { Text("Annuler") }
        }

        Text(
            text = "Restes / Tupperware",
            style = MaterialTheme.typography.headlineSmall
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                    value = containerText,
                    onValueChange = { containerText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Contenant") },
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
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Date de cuisson", style = MaterialTheme.typography.titleMedium)

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

                Divider()

                Text("DLC estimée", style = MaterialTheme.typography.titleMedium)

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
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Composition (toggles)", style = MaterialTheme.typography.titleMedium)

                ToggleRow("Contient de la viande", containsMeat) { containsMeat = it }
                ToggleRow("Contient de la crème", containsCream) { containsCream = it }
                ToggleRow("Contient du riz", containsRice) { containsRice = it }

                Divider()

                ToggleRow("Contient du poisson", containsFish) { containsFish = it }
                ToggleRow("Contient des œufs", containsEggs) { containsEggs = it }
                ToggleRow("Épicé", spicy) { spicy = it }

                Divider()

                ToggleRow("À réchauffer", toReheat) { toReheat = it }
                ToggleRow("Peut être congelé", canFreeze) { canFreeze = it }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Button(
            onClick = { onConfirm(dishName.ifBlank { "Restes" }, expiryMs, metaJson) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Ajouter au frigo")
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

/**
 * Mini “chip” sans dépendre d’un composant custom : simple, lisible, suffisant.
 * Si tu as déjà des Chips dans ton design system, remplace.
 */
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
    spicy: Boolean,
    toReheat: Boolean,
    canFreeze: Boolean,
    portions: Int?,
    container: String?,
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

    o.put("spicy", spicy)
    o.put("toReheat", toReheat)
    o.put("canFreeze", canFreeze)

    if (portions != null) o.put("portions", portions)
    if (container != null) o.put("container", container)
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
