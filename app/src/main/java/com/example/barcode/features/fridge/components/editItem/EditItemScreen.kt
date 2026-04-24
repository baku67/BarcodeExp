package com.example.barcode.features.fridge.components.editItem

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import com.example.barcode.R
import com.example.barcode.common.ui.components.WheelDatePickerDialog
import com.example.barcode.core.UserPreferencesStore
import com.example.barcode.data.local.entities.ItemEntity
import com.example.barcode.domain.models.ThemeMode
import com.example.barcode.features.addItems.manual.MANUAL_TYPES_WITH_SUBTYPE_IMAGE
import com.example.barcode.features.addItems.manual.ManualTaxonomyImageResolver
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.flow.map

data class EditItemResult(
    val name: String?,
    val brand: String?,
    val expiryDate: Long?,
    val imageUrl: String?,
    val nutriScore: String?,
    val imageIngredientsUrl: String?,
    val imageNutritionUrl: String?,
)

private data class PreviewImageInfo(
    val url: String,
    val isAuto: Boolean,
    val label: String? // "Illustration (sous-type)" / "Illustration (type)" / null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditItemScreen(
    itemEntity: ItemEntity,
    onSave: (EditItemResult) -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current

    val isManual = remember(itemEntity.addMode) { itemEntity.addMode == "manual" }

    var name by rememberSaveable { mutableStateOf(itemEntity.name.orEmpty()) }
    var brand by rememberSaveable { mutableStateOf(itemEntity.brand.orEmpty()) }
    var expiry by rememberSaveable { mutableStateOf(itemEntity.expiryDate) }
    var nutriScore by rememberSaveable { mutableStateOf(itemEntity.nutriScore) }

    var imageUrl by rememberSaveable { mutableStateOf(itemEntity.imageUrl.orEmpty()) }
    var ingredientsUrl by rememberSaveable { mutableStateOf(itemEntity.imageIngredientsUrl.orEmpty()) }
    var nutritionUrl by rememberSaveable { mutableStateOf(itemEntity.imageNutritionUrl.orEmpty()) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showNutriPicker by remember { mutableStateOf(false) }
    var showImageDialog by remember { mutableStateOf(false) }

    val relative = remember(expiry) { expiry?.let { formatRelativeDaysAnyDistance(it) } ?: "—" }
    val absolute = remember(expiry) { expiry?.let { formatAbsoluteDate(it) } ?: "—" }
    val scrollState = rememberScrollState()

    // ✅ Preview image :
    // 1) manual + (VEGETABLES/MEAT/FISH/DAIRY) + subtype => image sous-type
    // 2) manual + type (ex: LEFTOVERS) => image type
    // 3) fallback => imageUrl (modifiable)
    val previewInfo = remember(
        itemEntity.addMode,
        itemEntity.manualType,
        itemEntity.manualSubtype,
        imageUrl,
        context.packageName
    ) {
        val fallback = imageUrl.trim()
        if (itemEntity.addMode != "manual") {
            return@remember PreviewImageInfo(url = fallback, isAuto = false, label = null)
        }

        val type = itemEntity.manualType?.trim().orEmpty()
        val subtype = itemEntity.manualSubtype?.trim().orEmpty()
        val pkg = context.packageName

        // 1) Sous-type
        if (type in MANUAL_TYPES_WITH_SUBTYPE_IMAGE && subtype.isNotBlank()) {
            val resId = ManualTaxonomyImageResolver.resolveSubtypeDrawableResId(context, subtype)
            if (resId != 0) {
                return@remember PreviewImageInfo(
                    url = "android.resource://$pkg/$resId",
                    isAuto = true,
                    label = "Illustration (sous-type)"
                )
            }
        }

        // 2) Type (ex: LEFTOVERS)
        if (type.isNotBlank()) {
            val resId = ManualTaxonomyImageResolver.resolveTypeDrawableResId(context, type)
            if (resId != 0) {
                return@remember PreviewImageInfo(
                    url = "android.resource://$pkg/$resId",
                    isAuto = true,
                    label = "Illustration (type)"
                )
            }
        }

        // 3) Fallback
        PreviewImageInfo(url = fallback, isAuto = false, label = null)
    }

    val previewImageUrl = previewInfo.url
    val isAutoIllustration = previewInfo.isAuto
    val previewLabel = previewInfo.label

    val prefsStore = remember(context) { UserPreferencesStore(context) }

    val themeMode by prefsStore.preferences
        .map { it.theme }
        .collectAsState(initial = ThemeMode.SYSTEM)

    val useDarkTheme = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Modifier le produit", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Filled.Close, contentDescription = "Fermer")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            onSave(
                                EditItemResult(
                                    name = name.trim().ifBlank { null },
                                    // ⚠️ Ajout manuel : pas d’édition de marque / Nutri-Score
                                    brand = if (isManual) itemEntity.brand?.trim()?.ifBlank { null }
                                    else brand.trim().ifBlank { null },
                                    expiryDate = expiry,
                                    imageUrl = imageUrl.trim().ifBlank { null },
                                    nutriScore = if (isManual) itemEntity.nutriScore?.trim()?.ifBlank { null }
                                    else nutriScore?.trim()?.ifBlank { null },
                                    imageIngredientsUrl = ingredientsUrl.trim().ifBlank { null },
                                    imageNutritionUrl = nutritionUrl.trim().ifBlank { null },
                                )
                            )
                        },
                        enabled = name.isNotBlank()
                    ) { Text("Enregistrer") }
                }
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // --- IMAGE ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    val painter = rememberAsyncImagePainter(model = previewImageUrl.ifBlank { null })
                    val isImageLoading = painter.state is AsyncImagePainter.State.Loading

                    if (previewImageUrl.isNotBlank()) {

                        // 1) Fond : crop + blur
                        Image(
                            painter = painter,
                            contentDescription = null,
                            modifier = Modifier.matchParentSize().blur(22.dp),
                            contentScale = ContentScale.Crop,
                            alpha = 0.25f
                        )
                        // 2) Premier plan : image entière
                        Image(
                            painter = painter,
                            contentDescription = "Image produit",
                            modifier = Modifier.matchParentSize(),
                            contentScale = ContentScale.Fit,
                            alignment = Alignment.Center
                        )

                        if (isImageLoading) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(Color.Black.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                androidx.compose.material3.CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(28.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                                )
                            }
                        }

                        if (painter.state is AsyncImagePainter.State.Error) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Image, contentDescription = null)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    Brush.verticalGradient(
                                        0f to Color.Transparent,
                                        0.70f to Color.Black.copy(alpha = 0.10f),
                                        1f to Color.Black.copy(alpha = 0.45f)
                                    )
                                )
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Image, contentDescription = null)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.18f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when {
                                previewImageUrl.isBlank() -> "Aucune image"
                                isAutoIllustration -> previewLabel ?: "Illustration"
                                else -> "Image actuelle"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.92f)
                        )

                        Spacer(Modifier.weight(1f))

                        // ✅ Pour l’instant : si l’image est gérée via type/sous-type => pas d’édition
                        FilledTonalIconButton(
                            onClick = { showImageDialog = true },
                            enabled = !isAutoIllustration
                        ) {
                            Icon(Icons.Filled.Image, contentDescription = "Modifier l'image")
                        }
                    }
                }

                if (showImageDialog && !isAutoIllustration) {
                    EditImageUrlDialog(
                        initial = imageUrl,
                        onConfirm = {
                            imageUrl = it
                            showImageDialog = false
                        },
                        onDismiss = { showImageDialog = false }
                    )
                }

                Spacer(Modifier.height(16.dp))

                // --- TEXTE ---
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (!isManual) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = brand,
                            onValueChange = { brand = it },
                            label = { Text("Marque") },
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(Modifier.width(20.dp))

                        // --- NutriScore ---
                        Box(
                            modifier = Modifier
                                .width(84.dp)
                                .height(56.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                                .clickable { showNutriPicker = true },
                            contentAlignment = Alignment.Center
                        ) {
                            val nutriRes = nutriScoreRes(nutriScore)

                            if (nutriRes != null) {
                                Image(
                                    painter = painterResource(nutriRes),
                                    contentDescription = "Nutri-Score $nutriScore",
                                    modifier = Modifier.height(24.dp)
                                )
                            } else {
                                Image(
                                    painter = painterResource(R.drawable.nutri_score_neutre),
                                    contentDescription = "Nutri-Score neutre",
                                    modifier = Modifier.height(24.dp).alpha(0.35f)
                                )
                            }
                        }
                    }

                    if (showNutriPicker) {
                        NutriScorePickerDialog(
                            current = nutriScore,
                            onSelect = {
                                nutriScore = it
                                showNutriPicker = false
                            },
                            onDismiss = { showNutriPicker = false }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // --- EXPIRATION ---
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Expiration",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                            )

                            val relColor = expiryRelativeColor(expiry)

                            Text(
                                text = buildAnnotatedString {
                                    if (expiry == null) {
                                        append("Non définie")
                                    } else {
                                        withStyle(SpanStyle(color = relColor, fontWeight = FontWeight.SemiBold)) {
                                            append(relative)
                                        }
                                        append(" • ")
                                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f))) {
                                            append(absolute)
                                        }
                                    }
                                }
                            )
                        }

                        FilledTonalIconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Filled.EditCalendar, contentDescription = "Modifier la date")
                        }
                    }
                }

                if (showDatePicker) {
                    WheelDatePickerDialog(
                        initialMillis = expiry,
                        onConfirm = { newMillis ->
                            expiry = newMillis
                            showDatePicker = false
                        },
                        onDismiss = { showDatePicker = false },
                        showExpiredHint = true,
                        useDarkTheme = useDarkTheme,
                    )
                }
            }

            Button(
                onClick = {
                    onSave(
                        EditItemResult(
                            name = name.trim().ifBlank { null },
                            // ⚠️ Ajout manuel : pas d’édition de marque / Nutri-Score
                            brand = if (isManual) itemEntity.brand?.trim()?.ifBlank { null }
                            else brand.trim().ifBlank { null },
                            expiryDate = expiry,
                            imageUrl = imageUrl.trim().ifBlank { null },
                            nutriScore = if (isManual) itemEntity.nutriScore?.trim()?.ifBlank { null }
                            else nutriScore?.trim()?.ifBlank { null },
                            imageIngredientsUrl = ingredientsUrl.trim().ifBlank { null },
                            imageNutritionUrl = nutritionUrl.trim().ifBlank { null },
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = name.isNotBlank()
            ) {
                Text("Enregistrer")
            }
        }
    }
}

@Composable
private fun EditImageUrlDialog(
    initial: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var value by rememberSaveable { mutableStateOf(initial) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifier l’URL de l’image") },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text("URL") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value.trim()) }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

@Composable
private fun NutriScorePickerDialog(
    current: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf("A", "B", "C", "D", "E", null)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nutri-Score") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { opt ->
                    val label = opt ?: "Neutre"
                    val isSelected =
                        (opt?.uppercase() == current?.uppercase()) || (opt == null && current == null)

                    val shape = RoundedCornerShape(12.dp)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                                else Color.Transparent
                            )
                            .clickable { onSelect(opt) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val res = nutriScoreRes(opt)

                        if (res != null) {
                            Image(
                                painter = painterResource(res),
                                contentDescription = null,
                                modifier = Modifier.height(20.dp)
                            )
                        } else {
                            Image(
                                painter = painterResource(R.drawable.nutri_score_neutre),
                                contentDescription = null,
                                modifier = Modifier.height(20.dp).alpha(0.35f)
                            )
                        }

                        Spacer(Modifier.width(12.dp))

                        Text(
                            text = label,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fermer") } }
    )
}

// --- Utils (copiés de DetailsStepScreen) ---

@Composable
private fun expiryRelativeColor(expiry: Long?): Color {
    val cs = MaterialTheme.colorScheme
    if (expiry == null) return cs.onSurface.copy(alpha = 0.55f)

    return when {
        expiry < System.currentTimeMillis() -> cs.error
        expiry <= System.currentTimeMillis() + 24 * 60 * 60 * 1000 -> Color(0xFFFFC107)
        else -> cs.primary
    }
}

private fun formatAbsoluteDate(ms: Long): String =
    Instant.ofEpochMilli(ms)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))

private fun formatRelativeDaysAnyDistance(targetMillis: Long): String {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val target = Instant.ofEpochMilli(targetMillis).atZone(zone).toLocalDate()
    val days = ChronoUnit.DAYS.between(today, target).toInt()
    return when {
        days == 0 -> "aujourd'hui"
        days == 1 -> "demain"
        days == -1 -> "hier"
        days > 1 -> "dans $days j"
        else -> "il y a ${-days} j"
    }
}

@DrawableRes
private fun nutriScoreRes(score: String?): Int? = when (score?.trim()?.uppercase()) {
    "A" -> R.drawable.nutri_score_a
    "B" -> R.drawable.nutri_score_b
    "C" -> R.drawable.nutri_score_c
    "D" -> R.drawable.nutri_score_d
    "E" -> R.drawable.nutri_score_e
    else -> null
}
