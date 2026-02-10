package com.example.barcode.common.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Date picker "3 roues" (iOS-like) réutilisable.
 * - Pas de gradient overlay (ça salit le fond)
 * - Opacité du texte modulée selon la distance au centre
 * - Snap au centre
 */

enum class MonthWheelFormat {
    ShortText,   // Jan, Fév, ...
    TwoDigits    // 01, 02, ... 12
}

@Composable
fun WheelDatePickerDialog(
    initialMillis: Long?,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit,
    title: String = "Choisir une date",
    itemHeight: Dp = 34.dp,
    visibleCount: Int = 5,
    monthFormat: MonthWheelFormat = MonthWheelFormat.ShortText,
    showExpiredHint: Boolean = false,
    expiredHintText: String = "Déjà expiré",
) {
    val zone = ZoneId.systemDefault()

    val initialDate = remember(initialMillis) {
        initialMillis?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
    } ?: LocalDate.now(zone)

    var year by rememberSaveable { mutableStateOf(initialDate.year) }
    var month by rememberSaveable { mutableStateOf(initialDate.monthValue) }
    var day by rememberSaveable { mutableStateOf(initialDate.dayOfMonth) }

    val nowYear = LocalDate.now(zone).year
    val years = remember(nowYear) { (nowYear - 10..nowYear + 15).toList() }
    val monthOptions = remember(monthFormat) {
        when (monthFormat) {
            MonthWheelFormat.TwoDigits -> (1..12).map { it.toString().padStart(2, '0') }
            MonthWheelFormat.ShortText -> listOf("Jan", "Fév", "Mar", "Avr", "Mai", "Juin", "Juil", "Août", "Sep", "Oct", "Nov", "Déc")
        }
    }


    val maxDay = remember(year, month) { YearMonth.of(year, month).lengthOfMonth() }
    if (day > maxDay) day = maxDay

    val selected = remember(year, month, day) { LocalDate.of(year, month, day) }
    val expiredHint = remember(selected, showExpiredHint, expiredHintText) {
        if (!showExpiredHint) null
        else selected.takeIf { it.isBefore(LocalDate.now(zone)) }?.let { expiredHintText }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WheelPicker(
                        options = (1..maxDay).map { it.toString().padStart(2, '0') },
                        selectedIndex = (day - 1).coerceIn(0, maxDay - 1),
                        onIndexChanged = { day = it + 1 },
                        modifier = Modifier.weight(1f),
                        itemHeight = itemHeight,
                        visibleCount = visibleCount,
                    )

                    WheelPicker(
                        options = monthOptions,
                        selectedIndex = (month - 1).coerceIn(0, 11),
                        onIndexChanged = { month = it + 1 },
                        modifier = Modifier.weight(1f),
                        itemHeight = itemHeight,
                        visibleCount = visibleCount,
                    )

                    WheelPicker(
                        options = years.map { it.toString() },
                        selectedIndex = years.indexOf(year).let { if (it >= 0) it else 0 },
                        onIndexChanged = { year = years[it] },
                        modifier = Modifier.weight(1f),
                        itemHeight = itemHeight,
                        visibleCount = visibleCount,
                    )
                }

                Text(
                    text = LocalDate.of(year, month, day)
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (expiredHint != null) {
                        Text(
                            text = expiredHint,
                            color = MaterialTheme.colorScheme.tertiary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                TextButton(onClick = onDismiss) { Text("Annuler") }

                TextButton(
                    onClick = { onConfirm(selected.atStartOfDay(zone).toInstant().toEpochMilli()) }
                ) { Text("OK") }
            }
        },
        dismissButton = {}
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WheelDatePickerBottomSheet(
    initialDate: LocalDate?,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
    title: String = "Saisie manuelle",
    itemHeight: Dp = 34.dp,
    visibleCount: Int = 5,
    showExpiredHint: Boolean = true,
    monthFormat: MonthWheelFormat = MonthWheelFormat.ShortText,
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
    )

    val zone = remember { ZoneId.systemDefault() }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }

    val initial = remember(initialDate) { initialDate ?: LocalDate.now(zone) }

    var year by rememberSaveable { mutableStateOf(initial.year) }
    var month by rememberSaveable { mutableStateOf(initial.monthValue) }
    var day by rememberSaveable { mutableStateOf(initial.dayOfMonth) }

    val nowYear = LocalDate.now(zone).year
    val years = remember(nowYear) { (nowYear - 10..nowYear + 15).toList() }
    val monthOptions = remember(monthFormat) {
        when (monthFormat) {
            MonthWheelFormat.TwoDigits -> (1..12).map { it.toString().padStart(2, '0') }
            MonthWheelFormat.ShortText -> listOf("Jan", "Fév", "Mar", "Avr", "Mai", "Juin", "Juil", "Août", "Sep", "Oct", "Nov", "Déc")
        }
    }

    val maxDay = remember(year, month) { YearMonth.of(year, month).lengthOfMonth() }
    if (day > maxDay) day = maxDay

    val selected = remember(year, month, day) { LocalDate.of(year, month, day) }
    val expiredHint = remember(selected) {
        if (!showExpiredHint) null
        else selected.takeIf { it.isBefore(LocalDate.now(zone)) }?.let { "Déjà expiré" }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "Date sélectionnée : ${selected.format(dateFormatter)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                WheelPicker(
                    options = (1..maxDay).map { it.toString().padStart(2, '0') },
                    selectedIndex = (day - 1).coerceIn(0, maxDay - 1),
                    onIndexChanged = { day = it + 1 },
                    modifier = Modifier.weight(1f),
                    itemHeight = itemHeight,
                    visibleCount = visibleCount,
                )

                WheelPicker(
                    options = monthOptions,
                    selectedIndex = (month - 1).coerceIn(0, 11),
                    onIndexChanged = { month = it + 1 },
                    modifier = Modifier.weight(1f),
                    itemHeight = itemHeight,
                    visibleCount = visibleCount,
                )

                WheelPicker(
                    options = years.map { it.toString() },
                    selectedIndex = years.indexOf(year).let { if (it >= 0) it else 0 },
                    onIndexChanged = { year = years[it] },
                    modifier = Modifier.weight(1f),
                    itemHeight = itemHeight,
                    visibleCount = visibleCount,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp)
                ) {
                    if (expiredHint != null) {
                        Text(
                            text = expiredHint,
                            color = MaterialTheme.colorScheme.tertiary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Annuler") }
                    Button(onClick = { onConfirm(selected) }) { Text("Valider") }
                }
            }
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WheelPicker(
    options: List<String>,
    selectedIndex: Int,
    onIndexChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    visibleCount: Int = 5,
    itemHeight: Dp = 34.dp,
) {
    val blockParentSheetDrag = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // On bouffe le scroll restant pour empêcher le BottomSheet de bouger
                return available
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                // Pareil pour le fling
                return available
            }
        }
    }

    if (options.isEmpty()) return

    val safeSelected = selectedIndex.coerceIn(0, options.lastIndex)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = safeSelected)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    val verticalPadding = itemHeight * ((visibleCount - 1) / 2f)

    LaunchedEffect(safeSelected, options.size) {
        if (!listState.isScrollInProgress) {
            listState.scrollToItem(safeSelected)
        }
    }

    val currentIndex by remember {
        derivedStateOf {
            val layout = listState.layoutInfo
            val center = (layout.viewportStartOffset + layout.viewportEndOffset) / 2
            layout.visibleItemsInfo.minByOrNull { info ->
                abs((info.offset + info.size / 2) - center)
            }?.index ?: safeSelected
        }
    }

    val normalizedCenterDistanceByIndex by remember {
        derivedStateOf {
            val layout = listState.layoutInfo
            val centerY = (layout.viewportStartOffset + layout.viewportEndOffset) / 2
            layout.visibleItemsInfo.associate { info ->
                val itemCenterY = info.offset + info.size / 2
                val normalized = (abs(itemCenterY - centerY) / info.size.toFloat())
                info.index to normalized
            }
        }
    }

    LaunchedEffect(listState, options.size) {
        snapshotFlow { currentIndex }
            .distinctUntilChanged()
            .collect { idx ->
                if (idx in options.indices) onIndexChanged(idx)
            }
    }

    Box(
        modifier = modifier
            .height(itemHeight * visibleCount)
            .padding(horizontal = 6.dp)
            .nestedScroll(blockParentSheetDrag)
    ) {
        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            contentPadding = PaddingValues(vertical = verticalPadding),
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(options.size) { idx ->
                val selected = idx == currentIndex
                val norm = (normalizedCenterDistanceByIndex[idx] ?: 2.5f).coerceIn(0f, 2.5f)

                val alpha = (1f - norm * 0.35f).coerceIn(0.12f, 1f)
                val scale = (1f - norm * 0.06f).coerceIn(0.90f, 1f)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight),
                    contentAlignment = Alignment.Center
                ) {
                    val baseColor = if (selected) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant

                    Text(
                        text = options[idx],
                        style = if (selected) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                        color = baseColor.copy(alpha = if (selected) 1f else alpha),
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.graphicsLayer(
                            scaleX = if (selected) 1f else scale,
                            scaleY = if (selected) 1f else scale
                        )
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(itemHeight)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
        )
    }
}
