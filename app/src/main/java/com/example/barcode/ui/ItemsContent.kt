package com.example.barcode.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.MarqueeSpacing
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.barcode.addItems.ItemsViewModel
import com.example.barcode.auth.AppMode
import com.example.barcode.auth.SessionManager
import com.example.barcode.ui.components.FridgeDisplayIconToggle
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.barcode.ui.components.SnackbarBus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

enum class ViewMode { List, Grid }

// TODO: bouton explicite de rafraichissement ou alors padding en haut de liste (mais caché) qui permet de ne pas activer le pull-to-refresh sans faire expres (BAD UX°
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemsContent(
    innerPadding: PaddingValues,
    onAddItem: () -> Unit,
    vm: ItemsViewModel = viewModel(),
    isActive: Boolean,
) {
    val list by vm.items.collectAsState(initial = emptyList())
    var viewMode by rememberSaveable { mutableStateOf(ViewMode.List) }

    // --- Session (comme RecipesContent)
    val appContext = LocalContext.current.applicationContext
    val session = remember { SessionManager(appContext) }
    val scope = rememberCoroutineScope()

    val mode = session.appMode.collectAsState(initial = AppMode.AUTH).value
    val token = session.token.collectAsState(initial = null).value

    // --- Pull-to-refresh + initial load
    var refreshing by rememberSaveable { mutableStateOf(false) }
    var initialLoading by rememberSaveable { mutableStateOf(false) }
    var loadedForToken by rememberSaveable { mutableStateOf<String?>(null) }

    // bottom sheet au clic sur ItemCard
    var sheetItem by remember { mutableStateOf<com.example.barcode.data.Item?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // Viewer plein écran (click sur images BottomSheet)
    var viewerUrl by remember { mutableStateOf<String?>(null) }

    // Tri croissant : expirés + plus proches en haut, plus lointaines en bas
    val sorted = remember(list) {
        list.sortedWith(
            compareBy<com.example.barcode.data.Item> { it.expiryDate ?: Long.MAX_VALUE }
                .thenBy { (it.name ?: "").lowercase() }
        )
    }

    // BottomSheet au click sur ItemCard
    if (sheetItem != null) {
        ModalBottomSheet(
            onDismissRequest = { sheetItem = null },
            sheetState = sheetState
        ) {
            ItemExtraBottomSheet(
                item = sheetItem!!,
                onClose = { sheetItem = null },
                onOpenViewer = { viewerUrl = it }
            )
        }
    }

    // Viewer d'Image plein écran (click sur images BottomSheet)
    if (viewerUrl != null) {
        ImageViewerDialog(
            url = viewerUrl!!,
            onDismiss = { viewerUrl = null }
        )
    }

    // TODO: remplacer le delay par vrai refresh VM/API
    suspend fun refreshItems() {
        if (mode == AppMode.AUTH && !token.isNullOrBlank()) {
            delay(3_000)
            // ex: vm.refreshItems(token!!)
        } else {
            // ex: vm.reloadLocal()
        }
    }

    // --- Auto-load 1 seule fois quand l’onglet est réellement actif
    LaunchedEffect(isActive, mode, token) {
        val canLoad = isActive && mode == AppMode.AUTH && !token.isNullOrBlank()
        if (!canLoad) return@LaunchedEffect

        if (loadedForToken == token) return@LaunchedEffect

        initialLoading = true
        try {
            refreshItems()
        } finally {
            initialLoading = false
            loadedForToken = token // même si échec => évite spam navigation (refresh manuel pour retenter)
        }
    }


    Box(Modifier.fillMaxSize()) {

        // Barre de chargement top (jolie + non intrusive)
        if (initialLoading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            )
        }

        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = {
                scope.launch {
                    if (mode != AppMode.AUTH || token.isNullOrBlank()) {
                        SnackbarBus.show("Connecte-toi pour synchroniser.")
                        return@launch
                    }

                    refreshing = true
                    try {
                        refreshItems()
                    } finally {
                        refreshing = false
                    }
                }
            },
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // ✅ On garde ton layout : header + list scroll + bouton sticky en bas
            Column(
                modifier = Modifier
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

                    FridgeDisplayIconToggle(
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
                            imageUrl = it.imageUrl,
                            onLongPress = { sheetItem = it },
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
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ItemCard(
    name: String,
    brand: String?,
    expiry: Long?,
    imageUrl: String?,
    onLongPress: () -> Unit,
    onDelete: () -> Unit
) {
    val surface = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface
    val relativeCompact = remember(expiry) { expiry?.let { formatRelativeDaysCompact(it) } ?: "—" }

    Card(
        modifier = Modifier.combinedClickable(
            onClick = { /* rien pour l'instant */ },
            onLongClick = onLongPress
        ),
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



// BOTTOM SHEET 1/2:
@Composable
private fun ItemExtraBottomSheet(
    item: com.example.barcode.data.Item,
    onClose: () -> Unit,
    onOpenViewer: (String) -> Unit // ouverture du viewer d'image
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Images (infos produit)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onClose) { Text("Fermer") }
        }

        ExtraImageBlock(
            title = "Ingrédients",
            url = item.imageIngredientsUrl,
            onOpenViewer = onOpenViewer
        )

        ExtraImageBlock(
            title = "Nutrition",
            url = item.imageNutritionUrl,
            onOpenViewer = onOpenViewer
        )

        Spacer(Modifier.height(6.dp))
    }
}
// BOTTOM SHEET 2/2:
@Composable
private fun ExtraImageBlock(
    title: String,
    url: String?,
    onOpenViewer: (String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, fontWeight = FontWeight.SemiBold)

            if (url.isNullOrBlank()) {
                Text(
                    "Aucune image disponible.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                return@Column
            }

            Image(
                painter = rememberAsyncImagePainter(url),
                contentDescription = title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .combinedClickable(
                        onClick = { onOpenViewer(url) },
                        onLongClick = null
                    ),
                contentScale = ContentScale.Fit
            )
        }
    }
}

// Viewer d'image du BottomSheet:
@Composable
private fun ImageViewerDialog(
    url: String,
    onDismiss: () -> Unit
) {
    // zoom/pan/rotation
    var scale by remember(url) { mutableStateOf(1f) }
    var rotation by remember(url) { mutableStateOf(0f) }
    var offset by remember(url) { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

    val state = rememberTransformableState { zoomChange, panChange, rotationChange ->
        scale = (scale * zoomChange).coerceIn(1f, 8f)
        rotation += rotationChange
        offset += panChange

        // si on revient proche de 1x, on “recentre” (évite de perdre l’image)
        if (abs(scale - 1f) < 0.03f) {
            scale = 1f
            rotation = 0f
            offset = androidx.compose.ui.geometry.Offset.Zero
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // scrim cliquable pour fermer (optionnel, mais UX top)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.92f)
                    .clickable { onDismiss() }
            )

            // image interactive
            Image(
                painter = rememberAsyncImagePainter(url),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
                    .transformable(state = state, lockRotationOnZoomPan = false) // ✅ pan/zoom/rotate :contentReference[oaicite:6]{index=6}
                    .graphicsLayer {
                        translationX = offset.x
                        translationY = offset.y
                        scaleX = scale
                        scaleY = scale
                        rotationZ = rotation
                    }
            )

            // bouton fermer
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Fermer", tint = Color.White)
            }
        }
    }
}
