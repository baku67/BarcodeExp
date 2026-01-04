package com.example.barcode.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.MarqueeSpacing
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.barcode.items.ItemsViewModel
import com.example.barcode.ui.components.IconToggle
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

enum class ViewMode { List, Grid }


@Composable
fun ItemsContent(
    innerPadding: PaddingValues,
    onAddItem: () -> Unit,
    vm: ItemsViewModel = viewModel()
) {
    val list by vm.items.collectAsState(initial = emptyList())
    var viewMode by rememberSaveable { mutableStateOf(ViewMode.List) }

    // Tri croissant : expirés + plus proches en haut, plus lointaines en bas
    val sorted = remember(list) {
        list.sortedWith(
            compareBy<com.example.barcode.data.Item> { it.expiryDate ?: Long.MAX_VALUE }
                .thenBy { (it.name ?: "").lowercase() }
        )
    }

    Column(
        modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Frigo",
                fontSize = 22.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.weight(1f))

            IconToggle(
                selected = viewMode,
                onSelect = { viewMode = it }
            )
        }


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
            onClick = onAddItem,
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

@OptIn(ExperimentalFoundationApi::class)
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

    Card(
        colors = CardDefaults.cardColors(containerColor = surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = when {
            expiry == null -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            expiry != null && isSoon(expiry) -> BorderStroke(1.dp, Color.Yellow)
            expiry != null && isExpired(expiry) -> BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary)
            // Si encore bien frais: gris
            else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            // Ou alors primary:
            // else -> BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        }
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // TODO: removeBG natif
            ProductThumb(imageUrl)

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {

                Text(
                    text = name,
                    fontWeight = FontWeight.SemiBold,
                    color = onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Clip, // important: sinon l’ellipsis masque l’intérêt du marquee
                    modifier = Modifier
                        .fillMaxWidth() // important: il faut une contrainte de largeur
                        .basicMarquee(
                            animationMode = MarqueeAnimationMode.Immediately,
                            iterations = Int.MAX_VALUE,
                            initialDelayMillis = 1200,   // pause avant le 1er défilement
                            repeatDelayMillis = 2000,    // pause entre chaque boucle (ton “interval régulier”)
                            velocity = 28.dp,            // vitesse (dp/sec environ selon version)
                            spacing = MarqueeSpacing(24.dp) // espace avant de “re-boucler”
                        )
                )

                val brandText = brand?.takeIf { it.isNotBlank() } ?: "—"
                Text(
                    brandText,
                    color = onSurface.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false
                )

                // “dans 3j.” / “aujourd’hui” / “hier” / “il y a 2j.”
                Text(
                    relativeCompact,
                    color = when {
                        expiry == null -> onSurface.copy(alpha = 0.6f)
                        isSoon(expiry) -> Color.Yellow
                        isExpired(expiry) -> MaterialTheme.colorScheme.tertiary
                        else -> onSurface.copy(alpha = 0.8f)
                    },
                    style = MaterialTheme.typography.bodySmall
                )

                // DATE absolue en plus petit
                //if (absolute != "—") {
                //    Text(
                //        "($absolute)",
                //        color = onSurface.copy(alpha = 0.5f),
                //        style = MaterialTheme.typography.bodySmall
                //    )
                //}
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

// Laisser l'utilisateur modifier la valeur de "isSoon" dans Settings
private fun isSoon(expiry: Long): Boolean {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val target = Instant.ofEpochMilli(expiry).atZone(zone).toLocalDate()
    val days = ChronoUnit.DAYS.between(today, target).toInt()
    return days in 0..2
}
