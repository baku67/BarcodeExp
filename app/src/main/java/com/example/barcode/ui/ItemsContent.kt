package com.example.barcode.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.MarqueeSpacing
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
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
import java.time.temporal.ChronoUnit
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.barcode.R
import com.example.barcode.ui.components.SnackbarBus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

enum class ViewMode { List, Fridge }

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
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    // Viewer plein écran (click sur images BottomSheet)
    var viewerUrl by remember { mutableStateOf<String?>(null) }

    // Tri croissant : expirés + plus proches en haut, plus lointaines en bas
    val sorted = remember(list) {
        list.sortedWith(
            compareBy<com.example.barcode.data.Item> { it.expiryDate ?: Long.MAX_VALUE }
                .thenBy { (it.name ?: "").lowercase() }
        )
    }

    // Etageres grid
    val itemsPerShelf = 5
    val shelves = remember(sorted) {
        sorted.chunked(itemsPerShelf)
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

    // BottomSheet au click sur ItemCard
    fun closeSheet() {
        scope.launch {
            sheetState.hide()
            sheetItem = null
        }
    }

    // Couleur du calque pour le composant du fond quand BottomSheet actif
    // if (sheetItem != null) {
    //     Box(
    //         modifier = Modifier
    //             .fillMaxSize()
    //             .background(Color.Black.copy(alpha = 0.35f))
    //             .clickable { closeSheet() } // tap outside => close (optionnel)
    //     )
    // }

    // BottomSheet
    if (sheetItem != null) {
        ModalBottomSheet(
            onDismissRequest = { closeSheet() },
            sheetState = sheetState,
            dragHandle = null
        ) {
            ItemDetailsBottomSheet(
                item = sheetItem!!,
                onClose = { closeSheet() },
                onOpenViewer = { viewerUrl = it }
            )
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

    // Tout ça pour relancer l'anim open BottomSheet
    // ✅ Quand on ouvre (sheetItem != null), on force l’anim show()
    LaunchedEffect(sheetItem) {
        if (sheetItem != null) {
            sheetState.show()
        }
    }
    // ✅ Si l’utilisateur ferme en swipant vers le bas, on “nettoie” sheetItem
    LaunchedEffect(sheetState.currentValue) {
        if (sheetState.currentValue == SheetValue.Hidden) {
            sheetItem = null
        }
    }


    // --- Sélection multiple (IDs = String)
    var selectionMode by rememberSaveable { mutableStateOf(false) }
    var selectedIds by rememberSaveable { mutableStateOf<Set<String>>(emptySet()) }
    fun toggleSelect(id: String) {
        selectedIds = if (selectedIds.contains(id)) selectedIds - id else selectedIds + id
        if (selectedIds.isEmpty()) selectionMode = false
    }
    fun enterSelectionWith(id: String) {
        selectionMode = true
        selectedIds = setOf(id)
    }
    fun exitSelection() {
        selectionMode = false
        selectedIds = emptySet()
    }


    var showHelp by rememberSaveable { mutableStateOf(false) }
    // Modal d'aide onClick sur "?" à coté du titre page
    if (showHelp) {
        Dialog(onDismissRequest = { showHelp = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        "Comment utiliser le Frigo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    HelpRow("👆", "Appuie sur un produit pour voir ses détails")
                    HelpRow("✋", "Appui long pour sélectionner plusieurs produits")
                    HelpRow("🗑", "Supprime plusieurs produits d’un coup")
                    HelpRow("🍳", "Cherche des recettes avec les produits sélectionnés")
                    HelpRow("⬇️", "Tire vers le bas pour synchroniser")

                    Spacer(Modifier.height(8.dp))

                    Button(
                        onClick = { showHelp = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("J’ai compris")
                    }
                }
            }
        }
    }




    Box(Modifier.fillMaxSize()) {

        // Barre de chargement top
        if (initialLoading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .align(Alignment.TopCenter),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
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

                    Spacer(Modifier.width(6.dp))

                    IconButton(
                        onClick = { showHelp = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                .clip(CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "?",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    FridgeDisplayIconToggle(
                        selected = viewMode,
                        onSelect = { viewMode = it }
                    )
                }

                Spacer(Modifier.height(12.dp))

                when (viewMode) {
                    ViewMode.List -> {
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
                                    selected = selectionMode && selectedIds.contains(it.id),
                                    selectionMode = selectionMode,
                                    onClick = {
                                        if (selectionMode) toggleSelect(it.id) else sheetItem = it
                                    },
                                    onLongPress = {
                                        if (!selectionMode) enterSelectionWith(it.id) else toggleSelect(it.id)
                                    },
                                    onDelete = { vm.deleteItem(it.id) }
                                )
                            }
                            item { Spacer(Modifier.height(4.dp)) }
                        }
                    }

                    ViewMode.Fridge -> {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(bottom = 8.dp)
                        ) {
                            itemsIndexed(shelves) { index, shelfItems ->
                                ShelfRow(
                                    index = index,
                                    items = shelfItems,
                                    selectionMode = selectionMode,
                                    selectedIds = selectedIds,
                                    onClickItem = { item ->
                                        if (selectionMode) toggleSelect(item.id)
                                        else sheetItem = item
                                    },
                                    onLongPressItem = { item ->
                                        if (!selectionMode) enterSelectionWith(item.id)
                                        else toggleSelect(item.id)
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                if(selectionMode) {
                    // ✅ mini ligne au-dessus : compteur + Annuler (secondaire)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${selectedIds.size} sélectionné(s)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(Modifier.weight(1f))

                        TextButton(onClick = { exitSelection() }) {
                            Icon(Icons.Filled.Close, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Annuler")
                        }
                    }

                    // ✅ action bar flottante “premium”
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {

                            // 🧠 Chercher recette (primaire, mais non destructif)
                            Button(
                                onClick = {
                                    // TODO plus tard: récupérer ingrédients depuis les items sélectionnés
                                    SnackbarBus.show("Bientôt: recherche de recette avec les ingrédients sélectionnés.")
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null) // TODO: remplace par une icône "restaurant" plus tard
                                Spacer(Modifier.width(8.dp))
                                Text("Recette", fontWeight = FontWeight.SemiBold)
                            }

                            // 🗑 Supprimer (destructif, mais sans wording culpabilisant)
                            OutlinedButton(
                                onClick = {
                                    selectedIds.forEach { id -> vm.deleteItem(id) }
                                    exitSelection()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.8f))
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Retirer", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }


                if(!selectionMode) {
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
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ItemCard(
    name: String,
    brand: String?,
    expiry: Long?,
    imageUrl: String?,
    selected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onDelete: () -> Unit
) {
    val surface = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface
    val relativeCompact = remember(expiry) { expiry?.let { formatRelativeDaysCompact(it) } ?: "—" }

    Card(
        modifier = Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongPress
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (selected)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            else surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = when {
            selected -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            expiry == null -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            expiry != null && isSoon(expiry) -> BorderStroke(1.dp, Color.Yellow)
            expiry != null && isExpired(expiry) -> BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary)
            else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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



@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShelfRow(
    index: Int,
    items: List<com.example.barcode.data.Item>,
    selectionMode: Boolean,
    selectedIds: Set<String>,
    onClickItem: (com.example.barcode.data.Item) -> Unit,
    onLongPressItem: (com.example.barcode.data.Item) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {

        // ---- PRODUITS POSÉS SUR L’ÉTAGÈRE
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            items.forEach { item ->
                Box(
                    modifier = Modifier.combinedClickable(
                        onClick = { onClickItem(item) },
                        onLongClick = { onLongPressItem(item) }
                    )
                ) {
                    ProductThumb(item.imageUrl)
                }
            }

            // placeholders pour garder l’alignement
            repeat(5 - items.size) {
                Spacer(Modifier.size(56.dp))
            }
        }

        // ---- SVG D’ÉTAGÈRE selon index row
        ShelfTrapezoid(
            modifier = Modifier.padding(horizontal = 8.dp),
            height = 10.dp,
            insetTop = 22.dp,
            lipHeight = 2.dp
        )
    }
}

enum class ShelfType {
    TOP1,
    TOP2,
    MIDDLE,
    BOTTOM,
    VEGETABLE
}
fun shelfTypeForIndex(index: Int): ShelfType =
    when {
        index == 0 -> ShelfType.TOP1
        index == 1 -> ShelfType.TOP2
        // TODO BOTTOM (index selon le nombre de MIDDLES inséré)
        index % 6 == 5 -> ShelfType.VEGETABLE
        else -> ShelfType.MIDDLE
    }

@Composable
fun ShelfSvg(
    type: ShelfType,
    modifier: Modifier = Modifier
) {
    val res = when (type) {
        ShelfType.TOP1 -> R.drawable.etagere_1_xml
        ShelfType.TOP2 -> R.drawable.etagere_1_xml
        ShelfType.MIDDLE -> R.drawable.etagere_1_xml
        ShelfType.BOTTOM -> R.drawable.etagere_1_xml
        ShelfType.VEGETABLE -> R.drawable.etagere_1_xml
    }

    Image(
        painter = painterResource(res),
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.FillWidth,
        alignment = Alignment.Center
    )
}


@Composable
fun ShelfTrapezoid(
    modifier: Modifier = Modifier,
    height: Dp = 10.dp,
    insetTop: Dp = 18.dp,     // combien le bord du haut est “rentré”
    lipHeight: Dp = 2.dp      // petite lèvre devant
) {
    val shelfColor = MaterialTheme.colorScheme.surfaceVariant
    val edgeColor = MaterialTheme.colorScheme.primary

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val w = size.width
        val h = size.height

        val inset = insetTop.toPx().coerceAtMost(w / 3f)
        val lip = lipHeight.toPx().coerceAtMost(h)

        // Trapèze : haut plus court, bas plein
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(inset, 0f)          // haut gauche
            lineTo(w - inset, 0f)      // haut droite
            lineTo(w, h - lip)         // bas droite
            lineTo(0f, h - lip)        // bas gauche
            close()
        }

        // Remplissage étagère
        drawPath(path, color = shelfColor)

        // Petite lèvre en bas (donne l'effet 3D)
        drawRect(
            color = edgeColor,
            topLeft = androidx.compose.ui.geometry.Offset(0f, h - lip),
            size = androidx.compose.ui.geometry.Size(w, lip)
        )

        // Ligne fine en haut de l'etagere
/*        drawLine(
            color = edgeColor,
            start = androidx.compose.ui.geometry.Offset(inset, 0f),
            end = androidx.compose.ui.geometry.Offset(w - inset, 0f),
            strokeWidth = 0.5.dp.toPx()
        )*/
    }
}

/* ——— Utils ——— */

private fun formatRelativeDaysCompact(targetMillis: Long): String {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val target = Instant.ofEpochMilli(targetMillis).atZone(zone).toLocalDate()
    val days = ChronoUnit.DAYS.between(today, target).toInt()
    return when {
        days == 0 -> "aujourd'hui"
        days == 1 -> "demain"
        days > 1  -> "dans ${days}j"
        days == -1 -> "hier"
        else -> "il y a ${-days}j (!)"
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


// Template ligne/étape contenu Modal d'aide (click "?"):
@Composable
private fun HelpRow(icon: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(icon, fontSize = 18.sp)
        Spacer(Modifier.width(10.dp))
        Text(text)
    }
}



// BOTTOM SHEET 1/2:
@Composable
private fun ItemDetailsBottomSheet(
    item: com.example.barcode.data.Item,
    onClose: () -> Unit,
    onOpenViewer: (String) -> Unit // ouverture du viewer d'image
) {

    Column(Modifier.fillMaxWidth()) {

        // Border top coloré AU TOP, avant tout
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.primary)
        )

        // Poignée custom en haut
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(44.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f))
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp) // pour laisser respirer la barre top(Box au dessus)
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

            ExtraImageBlockCollapsible(
                title = "Ingrédients",
                url = item.imageIngredientsUrl,
                onOpenViewer = onOpenViewer
            )

            ExtraImageBlockCollapsible(
                title = "Nutrition",
                url = item.imageNutritionUrl,
                onOpenViewer = onOpenViewer
            )

            Spacer(Modifier.height(6.dp))
        }
    }
}
// BOTTOM SHEET 2/2:
@Composable
private fun ExtraImageBlockCollapsible(
    title: String,
    url: String?,
    onOpenViewer: (String) -> Unit
) {
    var expanded by rememberSaveable(title) { mutableStateOf(false) }

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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !url.isNullOrBlank()) { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                if (!url.isNullOrBlank()) {
                    Text(
                        if (expanded) "Masquer" else "Afficher",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            if (url.isNullOrBlank()) {
                Text(
                    "Aucune image disponible.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                return@Column
            }

            // ✅ IMPORTANT: l'Image n'est composée que si expanded == true
            if (expanded) {
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



@Composable
private fun FridgeBackground(
    scrimAlpha: Float = 0.35f
) {
    Box(modifier = Modifier.fillMaxSize()) {

        // ✅ Fond opaque derrière (empêche toute transparence vers l’AppBackground)
        //Box(
        //    modifier = Modifier
        //        .matchParentSize()
        //        .background(MaterialTheme.colorScheme.onPrimary) // ou surfaceVariant
        //)

        Column(modifier = Modifier.matchParentSize()) {
            /* Image(
                painter = painterResource(R.drawable.fridge_background_global),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.FillWidth
            )*/
            Image(
                painter = painterResource(R.drawable.etagere_1),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentScale = ContentScale.Crop
            )
            Image(
                painter = painterResource(R.drawable.etagere2),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentScale = ContentScale.Crop
            )
            Image(
                painter = painterResource(R.drawable.etagere3),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentScale = ContentScale.Crop
            )
            Image(
                painter = painterResource(R.drawable.etagere4),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentScale = ContentScale.Crop
            )
            Image(
                painter = painterResource(R.drawable.etagere5),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentScale = ContentScale.Crop
            )
            Image(
                painter = painterResource(R.drawable.etagere6),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentScale = ContentScale.Crop
            )
        }

        // Voile par-dessus (optionnel)
        Box(
            modifier = Modifier
                .matchParentSize()

        )
    }
}
