package com.example.barcode.features.addItems

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.*

import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import com.example.barcode.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmStepScreen(
    draft: AddItemDraft,
    onConfirm: (name: String?, brand: String?, expiry: Long?) -> Unit,
    onBack: () -> Unit,
    onCancel: () -> Unit,
    // Nécessaire pour le choix entres les 4 images candidates
    onCycleImage: () -> Unit,
    onNutriScoreChange: (String?) -> Unit
) {
    var name by remember { mutableStateOf(draft.name.orEmpty()) }
    var brand by remember { mutableStateOf(draft.brand.orEmpty()) }
    var expiry by remember { mutableStateOf(draft.expiryDate) }
    var showPicker by remember { mutableStateOf(false) }

    val relative = remember(expiry) { expiry?.let { formatRelativeDaysAnyDistance(it) } ?: "—" }
    val absolute = remember(expiry) { expiry?.let { formatAbsoluteDate(it) } ?: "—" }
    val scrollState = rememberScrollState()

    var showNutriPicker by remember { mutableStateOf(false) }


    AddItemStepScaffold(
        step = 3,
        onBack = onBack,
        onCancel = onCancel
    ) { innerPadding ->

        val footerHeight = 84.dp // zone bouton + gradient
        val footerPadding = 14.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // ✅ Zone scrollable (ne chevauche jamais le bouton)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // --- Garde TOUT ton contenu actuel ici SAUF le Button("Confirmer") ---

                // Image et Bouton pour switcher entres les 4 images candidates
                val canCycleImage = draft.imageCandidates.size > 1

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    val url = draft.imageUrl
                    val painter = rememberAsyncImagePainter(model = url)
                    val isImageLoading = painter.state is AsyncImagePainter.State.Loading

                    if (!url.isNullOrBlank()) {

                        Image(
                            painter = painter,
                            contentDescription = "Image produit",
                            modifier = Modifier
                                .matchParentSize()
                                .clickable(enabled = canCycleImage && !isImageLoading) { onCycleImage() },
                            contentScale = ContentScale.Fit,
                            alignment = Alignment.Center
                        )

                        // ✅ Loader overlay
                        if (isImageLoading) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(Color.Black.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(28.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                                )
                            }
                        }

                        // ✅ Fallback si erreur
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



                        // ✅ LE “BONUS” : gradient AU-DESSUS de l’image
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

                    if (canCycleImage) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.18f)) // plus léger (car gradient aide déjà)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Image ${draft.imageCandidateIndex + 1}/${draft.imageCandidates.size}",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.92f)
                            )

                            Spacer(Modifier.weight(1f))

                            FilledTonalIconButton(
                                onClick = onCycleImage,
                                enabled = !isImageLoading
                            ) {
                                Icon(Icons.Filled.Refresh, contentDescription = "Changer l'image")
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

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

                    // --- Nutri-Score (zone dédiée + clic) ---
                    Box(
                        modifier = Modifier
                            .width(84.dp)          // ✅ plus d'espace
                            .height(56.dp)         // ✅ même "hauteur visuelle" que le champ
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                            .clickable { showNutriPicker = true }, // ✅ ouvre le picker
                        contentAlignment = Alignment.Center
                    ) {
                        val nutriRes = nutriScoreRes(draft.nutriScore)

                        if (nutriRes != null) {
                            Image(
                                painter = painterResource(nutriRes),
                                contentDescription = "Nutri-Score ${draft.nutriScore ?: "neutre"}",
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
                        current = draft.nutriScore,
                        onSelect = {
                            onNutriScoreChange(it)   // ✅ persiste dans le VM
                            showNutriPicker = false
                        },
                        onDismiss = { showNutriPicker = false }
                    )
                }

                Spacer(Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
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
                                            append(relative) // ✅ seulement la partie “aujourd’hui / demain / dans X j”
                                        }
                                        append(" • ")
                                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f))) {
                                            append(absolute)
                                        }
                                    }
                                }
                            )
                        }

                        FilledTonalIconButton(onClick = { showPicker = true }) {
                            Icon(Icons.Filled.EditCalendar, contentDescription = "Modifier la date")
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                if (showPicker) {
                    val initial = expiry ?: System.currentTimeMillis()
                    val state = rememberDatePickerState(initialSelectedDateMillis = initial)
                    DatePickerDialog(
                        onDismissRequest = { showPicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                val selectedUtc = state.selectedDateMillis
                                expiry = selectedUtc?.let { utcMillisToLocalMidnight(it) }
                                showPicker = false
                            }) { Text("OK") }
                        },
                        dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Annuler") } }
                    ) {
                        DatePicker(state = state, showModeToggle = false)
                    }
                }
            }

            // Footer fixe : bouton confirmé
            Spacer(Modifier.height(12.dp))

            // ✅ Bouton en bas (dans le flux, comme ItemsContent)
            Button(
                onClick = { onConfirm(name.ifBlank { null }, brand.ifBlank { null }, expiry) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = (name.isNotBlank() || (draft.name?.isNotBlank() == true))
            ) {
                Text("Confirmer")
            }
            }
        }

    }





@Composable
private fun NutriScorePickerDialog(
    current: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nutri-Score") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val options: List<String?> = listOf("A","B","C","D","E", null)

                options.forEach { opt ->
                    val label = opt ?: "Neutre"
                    val isSelected = (opt?.uppercase() == current?.uppercase()) || (opt == null && current == null)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
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
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fermer") }
        }
    )
}








// ——— Utils ——————————————————————————————————————————————————————————————

@Composable
private fun expiryRelativeColor(expiry: Long?): Color {
    val cs = MaterialTheme.colorScheme
    if (expiry == null) return cs.onSurface.copy(alpha = 0.55f)

    return when {
        expiry < System.currentTimeMillis() -> cs.error                    // expiré
        expiry <= System.currentTimeMillis() + 24 * 60 * 60 * 1000 -> Color(0xFFFFC107) // aujourd'hui/demain -> warning
        else -> cs.primary                                                // plus tard
    }
}


private fun utcMillisToLocalMidnight(utcMillis: Long): Long {
    val localDate = Instant.ofEpochMilli(utcMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    return localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
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