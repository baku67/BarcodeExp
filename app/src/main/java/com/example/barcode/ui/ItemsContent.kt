package com.example.barcode.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.example.barcode.ui.components.ItemsViewModel
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun ItemsContent(
    navController: NavHostController,
    innerPadding: PaddingValues,
    vm: ItemsViewModel = viewModel()
) {
    val list by vm.items.collectAsState(initial = emptyList())
    val primary = MaterialTheme.colorScheme.primary

    // Tri décroissant : les dates les plus lointaines en haut, les plus proches (et expirés) en bas
    val sorted = remember(list) {
        list.sortedWith(
            compareByDescending<com.example.barcode.data.Item> { it.expiryDate ?: Long.MAX_VALUE }
                .thenBy { (it.name ?: "").lowercase() }
        )
    }


    Column(
        modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Frigo", fontSize = 22.sp, color = primary, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(sorted, key = { it.id }) { it ->
                ItemCard(
                    name = it.name ?: "(sans nom)",
                    brand = it.brand,
                    expiry = it.expiryDate,
                    imageUrl = it.imageUrl, // ⚠️ suppose que ton Item a bien imageUrl
                    onDelete = { vm.deleteItem(it.id) }
                )
            }
            item { Spacer(Modifier.height(4.dp)) }
        }

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { navController.navigate("addItem") },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Ajouter un produit")
        }

    }
}

@Composable
private fun ItemCard(
    name: String,
    brand: String?,
    expiry: Long?,
    imageUrl: String?,
    onDelete: () -> Unit
) {
    val surface = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface

    val relativeCompact = remember(expiry) { expiry?.let { formatRelativeDaysCompact(it) } ?: "—" }
    val absolute = remember(expiry) { expiry?.let { formatAbsoluteDate(it) } ?: "—" }

    Card(
        colors = CardDefaults.cardColors(containerColor = surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ⬅️ Image du produit (remplace le badge)
            ProductThumb(imageUrl)

            Spacer(Modifier.width(12.dp))

            // Infos produit
            Column(Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.SemiBold, color = onSurface)
                val brandText = brand?.takeIf { it.isNotBlank() } ?: "—"
                Text(
                    brandText,
                    color = onSurface.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium
                )
                // “dans 3j.” / “aujourd’hui” / “hier” / “il y a 2j.”
                Text(
                    relativeCompact,
                    color = when {
                        expiry == null -> onSurface.copy(alpha = 0.6f)
                        isExpired(expiry) -> MaterialTheme.colorScheme.error
                        isSoon(expiry) -> MaterialTheme.colorScheme.tertiary
                        else -> onSurface.copy(alpha = 0.8f)
                    },
                    style = MaterialTheme.typography.bodySmall
                )
                // (optionnel) la date absolue en plus petit
                if (absolute != "—") {
                    Text(
                        "($absolute)",
                        color = onSurface.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Supprimer")
            }
        }
    }
}

@Composable
private fun ProductThumb(imageUrl: String?) {
    val shape = RoundedCornerShape(12.dp)
    if (!imageUrl.isNullOrBlank()) {
        Image(
            painter = rememberAsyncImagePainter(imageUrl),
            contentDescription = null,
            modifier = Modifier
                .size(56.dp)
                .clip(shape)
        )
    } else {
        // Placeholder simple si pas d'image
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text("🧴", fontSize = 20.sp)
        }
    }
}

/* ——— Utils ——— */

private fun formatAbsoluteDate(ms: Long): String =
    Instant.ofEpochMilli(ms)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))

private fun formatRelativeDaysCompact(targetMillis: Long): String {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val target = Instant.ofEpochMilli(targetMillis).atZone(zone).toLocalDate()
    val days = ChronoUnit.DAYS.between(today, target).toInt()
    return when {
        days == 0 -> "aujourd'hui"
        days == 1 -> "demain"
        days > 1  -> "dans ${days}j."
        days == -1 -> "hier"
        else -> "il y a ${-days}j."
    }
}

private fun isExpired(expiry: Long): Boolean {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val target = Instant.ofEpochMilli(expiry).atZone(zone).toLocalDate()
    return target.isBefore(today)
}

private fun isSoon(expiry: Long): Boolean {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val target = Instant.ofEpochMilli(expiry).atZone(zone).toLocalDate()
    val days = ChronoUnit.DAYS.between(today, target).toInt()
    return days in 0..2
}
