package com.example.barcode.features.fridge

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.TimerOff
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
import com.example.barcode.features.addItems.ItemsViewModel
import com.example.barcode.core.session.AppMode
import com.example.barcode.core.session.SessionManager
import java.time.*
import java.time.temporal.ChronoUnit
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import coil.compose.AsyncImagePainter
import com.example.barcode.R
import com.example.barcode.data.local.entities.ItemEntity
import com.example.barcode.features.auth.AuthViewModel
import com.example.barcode.common.ui.components.LocalAppTopBarState
import com.example.barcode.common.bus.SnackbarBus
import com.example.barcode.domain.models.FrigoLayout
import com.example.barcode.domain.models.UserPreferences
import com.example.barcode.sync.SyncScheduler
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import com.example.barcode.features.fridge.components.ItemCard
import com.example.barcode.features.fridge.components.ProductThumb
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope


enum class ViewMode { List, Fridge }

// TODO: bouton explicite de rafraichissement ou alors padding en haut de liste (mais caché) qui permet de ne pas activer le pull-to-refresh sans faire expres (BAD UX°
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemsContent(
    innerPadding: PaddingValues,
    authVm: AuthViewModel,
    onAddItem: () -> Unit,
    vm: ItemsViewModel = viewModel(),
    isActive: Boolean,
) {
    val list by vm.items.collectAsState(initial = emptyList())

    // --- Session (comme RecipesContent)
    val appContext = LocalContext.current.applicationContext
    val session = remember { SessionManager(appContext) }
    val scope = rememberCoroutineScope()

    val mode = session.appMode.collectAsState(initial = AppMode.AUTH).value
    val token = session.token.collectAsState(initial = null).value

    // Préférences User (displayFridge)
    val prefs = authVm.preferences.collectAsState(initial = UserPreferences()).value

    val selectedViewMode = when (prefs.frigoLayout) {
        FrigoLayout.LIST -> ViewMode.List
        FrigoLayout.DESIGN -> ViewMode.Fridge
    }

    // --- Pull-to-refresh + initial load
    var refreshing by rememberSaveable { mutableStateOf(false) }
    var initialLoading by rememberSaveable { mutableStateOf(false) }
    var loadedForToken by rememberSaveable { mutableStateOf<String?>(null) }

    // Bac à legumes
    val vegDrawerHeight = 88.dp
    val ghostOpacity = 0.34f

    // TODO plus tard : calculer via une vraie source (enum zone, tags, etc.)
    val vegDrawerEmpty = true
    val vegDrawerOpacity = if (vegDrawerEmpty) ghostOpacity else 1f

    // bottom sheet au clic sur ItemCard
    var sheetItemEntity by remember { mutableStateOf<ItemEntity?>(null) }
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    // Viewer plein écran (click sur images BottomSheet)
    var viewerUrl by remember { mutableStateOf<String?>(null) }

    // Tri croissant : expirés + plus proches en haut, plus lointaines en bas
    val sorted = remember(list) {
        list.sortedWith(
            compareBy<ItemEntity> { it.expiryDate ?: Long.MAX_VALUE }
                .thenBy { (it.name ?: "").lowercase() }
        )
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

    // Etageres grid
    val itemsPerShelf = 5
    val shelves = remember(sorted, selectedViewMode, selectionMode) {
        val base = sorted.chunked(itemsPerShelf).toMutableList()

        val isFridge = selectedViewMode == ViewMode.Fridge

        // Cas 0 item : on affiche quand même 1 étagère vide (en mode Fridge)
        if (isFridge && base.isEmpty()) {
            base.add(emptyList())
            return@remember base
        }

        // ✅ Ajoute TOUJOURS UNE étagère vide "inoccupée" si on a au moins 1 étagère occupée
        val shouldAddOneEmptyShelf =
            isFridge &&
                    !selectionMode &&
                    base.isNotEmpty()

        if (shouldAddOneEmptyShelf) {
            base.add(emptyList())
        }

        base
    }

    val listState = rememberLazyListState()

    // --- Fridge "turn on" effect (dimming uniquement sur les rangées)
    var fridgeOn by remember { mutableStateOf(false) }
    val dimAlpha by animateFloatAsState(
        targetValue = if (fridgeOn) 0f else 0.55f, // 0.45f soft -> 0.65f fort
        animationSpec = tween(durationMillis = 240), // durée de l'anim d'allumage du frigo
        label = "fridgeDimAlpha"
    )
    LaunchedEffect(isActive, selectedViewMode) {
        val shouldPlay = isActive && selectedViewMode == ViewMode.Fridge
        if (!shouldPlay) {
            fridgeOn = false
            return@LaunchedEffect
        }

        fridgeOn = false
        delay(90) // delai avant "allumage"
        fridgeOn = true
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
            // ✅ lance un sync push+pull en background
            SyncScheduler.enqueueSync(appContext)
            SnackbarBus.show("Synchronisation lancée…")
        } else {
            SnackbarBus.show("Mode local : rien à synchroniser.")
        }
    }

    // BottomSheet au click sur ItemCard
    fun closeSheet() {
        scope.launch {
            sheetState.hide()
            sheetItemEntity = null
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
    if (sheetItemEntity != null) {
        ModalBottomSheet(
            onDismissRequest = { closeSheet() },
            sheetState = sheetState,
            dragHandle = null
        ) {
            ItemDetailsBottomSheet(
                itemEntity = sheetItemEntity!!,
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
    LaunchedEffect(sheetItemEntity) {
        if (sheetItemEntity != null) {
            sheetState.show()
        }
    }
    // ✅ Si l’utilisateur ferme en swipant vers le bas, on “nettoie” sheetItem
    LaunchedEffect(sheetState.currentValue) {
        if (sheetState.currentValue == SheetValue.Hidden) {
            sheetItemEntity = null
        }
    }


    val topBarState = LocalAppTopBarState.current


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


    val owner = "items"

    LaunchedEffect(isActive, selectedViewMode) {
        if (isActive) {
            topBarState.setTitleTrailing(owner) {
                IconButton(
                    onClick = { showHelp = true },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.HelpOutline,
                        contentDescription = "Help",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }
            }

            topBarState.setActions(owner) {
                FridgeDisplayIconToggle(
                    selected = selectedViewMode,
                    onSelect = { authVm.onFridgeDisplaySelected(it) }
                )
            }
        } else {
            // ✅ important : page inactive => elle ne possède plus le header
            topBarState.clearActions(owner)
            topBarState.clearTitleTrailing(owner)
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

                // Pour calculer pinnage ou non du bac a legume
                val canScroll by remember {
                    derivedStateOf {
                        val layout = listState.layoutInfo
                        if (layout.visibleItemsInfo.isEmpty()) return@derivedStateOf false

                        val lastVisible = layout.visibleItemsInfo.last()
                        val viewportEnd = layout.viewportEndOffset

                        // Si le dernier item dépasse le viewport → scroll possible
                        lastVisible.offset + lastVisible.size > viewportEnd
                    }
                }


                if (sorted.isEmpty()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp)
                    ) {
                        item {
                            Box(
                                modifier = Modifier.fillParentMaxHeight(),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                Text("Aucun produit. Tire vers le bas pour synchroniser.")
                            }
                        }
                    }
                } else {
                    when (selectedViewMode) {
                        ViewMode.List -> {
                            LazyColumn(
                                state = listState,
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
                                            if (selectionMode) toggleSelect(it.id) else sheetItemEntity =
                                                it
                                        },
                                        onLongPress = {
                                            if (!selectionMode) enterSelectionWith(it.id) else toggleSelect(
                                                it.id
                                            )
                                        },
                                        onDelete = { vm.deleteItem(it.id) }
                                    )
                                }
                                item { Spacer(Modifier.height(4.dp)) }
                            }
                        }

                        ViewMode.Fridge -> {

                            LazyColumn(
                                state = listState,
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                contentPadding = PaddingValues(
                                    bottom = if (!canScroll) 8.dp else 0.dp
                                )
                            ) {
                                itemsIndexed(shelves) { index, shelfItems ->

                                    // ✅ espace supplémentaire AVANT certaines rangées
                                    val extraTop = when (index) {
                                        1 -> 5.dp
                                        2 -> 10.dp
                                        3 -> 10.dp
                                        4 -> 10.dp
                                        else -> 6.dp // léger espacement constant pour les MID supplémentaires
                                    }
                                    if (extraTop > 0.dp) {
                                        Spacer(Modifier.height(extraTop))
                                    }

                                    ShelfRow(
                                        index = index,
                                        itemEntities = shelfItems,
                                        selectionMode = selectionMode,
                                        selectedIds = selectedIds,
                                        onClickItem = { item ->
                                            if (selectionMode) toggleSelect(item.id) else sheetItemEntity =
                                                item
                                        },
                                        onLongPressItem = { item ->
                                            if (!selectionMode) enterSelectionWith(item.id) else toggleSelect(
                                                item.id
                                            )
                                        },
                                        dimAlpha = dimAlpha, // pour anim allumage frigo
                                        selectedSheetId = sheetItemEntity?.id,
                                        emptyOpacity = if (shelfItems.isEmpty()) ghostOpacity else 1f
                                    )
                                }

                                // ✅ Bac DANS le scroll si la liste est longue
                                if (canScroll && !selectionMode) {
                                    item {
                                        VegetableDrawerCube3D(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 6.dp),
                                            height = vegDrawerHeight,
                                            depth = 16.dp,
                                            dimAlpha = dimAlpha,
                                            isGhost = vegDrawerEmpty
                                        ) {
                                            if (vegDrawerEmpty) {
                                                Text(
                                                    text = "Bac à légumes vide",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                                                )
                                            }
                                        }

                                    }
                                }
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
                                    contentColor = MaterialTheme.colorScheme.tertiary
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f))
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Retirer", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }


                if (!selectionMode) {

                    val showPinnedVegDrawer =
                        selectedViewMode == ViewMode.Fridge &&
                                !selectionMode &&
                                !canScroll   // ✅ LE POINT CLÉ

                    // ✅ Bac à légumes FIXE uniquement en DESIGN
                    if (showPinnedVegDrawer) {
                        VegetableDrawerCube3D(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp),
                            height = vegDrawerHeight,
                            depth = 16.dp,
                            dimAlpha = dimAlpha,
                            isGhost = vegDrawerEmpty
                        ) {
                            if (vegDrawerEmpty) {
                                Text(
                                    text = "Bac à légumes vide",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                    }

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


// ---- ÉTAGÈRE selon index row (5 styles fixes)
enum class ShelfView { TOP, BOTTOM }

enum class ShelfPreset { TOP1, TOP2, MID, BOTTOM1, BOTTOM2 }

data class ShelfSpec(
    val height: Dp,
    val insetTop: Dp,
    val lipHeight: Dp,
    val view: ShelfView,
    val lipAlpha: Float = 1f
)

fun shelfSpec(preset: ShelfPreset): ShelfSpec = when (preset) {

    ShelfPreset.TOP1 -> ShelfSpec(
        height = 16.dp,
        insetTop = 16.dp,
        lipHeight = 1.dp,
        view = ShelfView.TOP,
        lipAlpha = 0.90f
    )

    ShelfPreset.TOP2 -> ShelfSpec(
        height = 11.dp,
        insetTop = 16.dp,
        lipHeight = 1.dp,
        view = ShelfView.TOP,
        lipAlpha = 0.90f
    )

    ShelfPreset.MID -> ShelfSpec(
        height = 2.dp,
        insetTop = 26.dp,
        lipHeight = 1.dp,
        view = ShelfView.TOP,
        lipAlpha = 0.90f
    )


    ShelfPreset.BOTTOM1 -> ShelfSpec(
        height = 10.dp,
        insetTop = 16.dp,
        lipHeight = 1.dp,
        view = ShelfView.BOTTOM,
        lipAlpha = 0.90f
    )

    ShelfPreset.BOTTOM2 -> ShelfSpec(
        height = 16.dp,
        insetTop = 16.dp,
        lipHeight = 1.dp,
        view = ShelfView.BOTTOM,
        lipAlpha = 0.90f
    )
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShelfRow(
    index: Int,
    itemEntities: List<ItemEntity>,
    selectionMode: Boolean,
    selectedIds: Set<String>,
    onClickItem: (ItemEntity) -> Unit,
    onLongPressItem: (ItemEntity) -> Unit,
    dimAlpha: Float = 0f, // pour anim allumage frigo
    selectedSheetId: String? = null,
    emptyOpacity: Float = 1f
) {
    val preset = when (index) {
        0 -> ShelfPreset.TOP1
        1 -> ShelfPreset.TOP2
        2 -> ShelfPreset.MID
        3 -> ShelfPreset.BOTTOM1
        4 -> ShelfPreset.BOTTOM2
        else -> ShelfPreset.MID
    }
    val spec = shelfSpec(preset)

    val productDrop = when (preset) {
        ShelfPreset.TOP1 -> 9.dp
        ShelfPreset.TOP2 -> 5.dp
        ShelfPreset.MID -> 1.dp
        ShelfPreset.BOTTOM1 -> 5.dp
        ShelfPreset.BOTTOM2 -> 9.dp
    }

    // Hauteur de la rangée = produits + étagère (le drop ne change pas la hauteur)
    val productSize = 56.dp
    val rowHeight = productSize + spec.height

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(rowHeight)
    ) {
        val productsOnTop = (spec.view == ShelfView.BOTTOM)

        // --- Produits
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = 12.dp)
                .offset(y = productDrop)
                .zIndex(if (productsOnTop) 1f else 0f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            itemEntities.forEachIndexed { itemIndex, item ->

                val isSheetSelected = selectedSheetId != null && item.id == selectedSheetId // pour mettre l'item en surbrillance pendant le BottomSheetDetails

                val hasSheetSelection = selectedSheetId != null
                val dimOthers = hasSheetSelection && !isSheetSelected

                val isSelected = selectionMode && selectedIds.contains(item.id)

                val glowColor = when {
                    item.expiryDate == null -> null
                    isExpired(item.expiryDate) -> MaterialTheme.colorScheme.tertiary
                    isSoon(item.expiryDate) -> Color(0xFFF9A825)
                    else -> null
                }

                val selectionBorderColor = when {
                    item.expiryDate != null && isExpired(item.expiryDate) -> MaterialTheme.colorScheme.tertiary
                    item.expiryDate != null && isSoon(item.expiryDate) -> Color(0xFFFFC107) // jaune
                    else -> Color(0xFF2ECC71) // vert (ou utilise cs.tertiary si tu préfères un vert "theme")
                }.copy(alpha = 0.95f)

                var imageLoaded by remember(item.id) { mutableStateOf(false) }

                val mt = MaterialTheme.colorScheme

                Box(
                    modifier = Modifier
                        .size(productSize),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    val cornerIcon = when {
                        item.expiryDate == null -> null
                        isExpired(item.expiryDate) -> Icons.Filled.WarningAmber
                        isSoon(item.expiryDate) -> Icons.Outlined.TimerOff
                        else -> null
                    }

                    val shouldGiggle =
                        !selectionMode && // évite que ça bouge pendant la sélection et anim que quand image chargée
                                imageLoaded &&
                                item.expiryDate != null &&
                                (isExpired(item.expiryDate) || isSoon(item.expiryDate))

                    val selectedScale by animateFloatAsState(
                        targetValue = if (isSheetSelected) 1.07f else 1f,
                        animationSpec = tween(durationMillis = 260),
                        label = "selectedScale"
                    )

                    ProductThumb(
                        imageUrl = item.imageUrl,
                        alignBottom = true,
                        cornerIcon = cornerIcon,
                        cornerIconTint = glowColor,
                        onImageLoaded = { imageLoaded = it },
                        dimAlpha = when {
                            isSheetSelected -> 0f           // le sélectionné reste full bright
                            dimOthers -> 0.5f              // assombrit les autres (AJUSTE ICI)
                            else -> dimAlpha                // sinon: anim globale d’allumage frigo
                        },
                        showImageBorder = isSheetSelected, // ✅ NEW
                        imageBorderColor = selectionBorderColor,
                        imageBorderWidth = 2.dp, // ✅ NEW
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(if (dimOthers) 0.92f else 1f)
                            .zIndex(if (isSheetSelected) 2f else 0f)
                            .graphicsLayer {
                                scaleX = selectedScale
                                scaleY = selectedScale
                            }
                            .giggleEvery(
                                enabled = shouldGiggle && !isSheetSelected, // évite 2 anims en même temps
                                intervalMs = 4_200L,
                                initialDelayMs = 500L + itemIndex * 90L
                            )
                            .combinedClickable(
                                onClick = { onClickItem(item) },
                                onLongClick = { onLongPressItem(item) }
                            )
                            .border(
                                width = if (isSelected) 2.dp else 0.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .background(
                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                    )
                }
            }

            repeat(5 - itemEntities.size) { Spacer(Modifier.size(productSize)) }
        }

        // --- Étagère
        ShelfTrapezoid(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .zIndex(if (productsOnTop) 0f else 1f)
                .alpha(emptyOpacity),
            height = spec.height,
            insetTop = spec.insetTop,
            lipHeight = spec.lipHeight,
            view = spec.view,
            lipAlpha = spec.lipAlpha,
            dimAlpha = dimAlpha // ✅ NEW
        )


    }

}




        @Composable
fun ShelfTrapezoid(
            modifier: Modifier = Modifier,
            height: Dp = 10.dp,
            insetTop: Dp = 18.dp,
            lipHeight: Dp = 2.dp,
            view: ShelfView = ShelfView.TOP,
            lipAlpha: Float = 1f,
            sideStrokeAlpha: Float = 0.28f,   // ✅ alpha des côtés
            sideStrokeWidth: Dp = 1.dp,        // ✅ épaisseur des côtés
            dimAlpha: Float = 0f
) {
    val cs = MaterialTheme.colorScheme

    val dimFactor = (dimAlpha / 0.55f).coerceIn(0f, 1f) // 0..1

    val baseShelf = androidx.compose.ui.graphics.lerp(cs.primary, cs.surface, 0.76f)
    val baseEdge = cs.primary
    // ✅ on assombrit uniquement les pixels dessinés (pas de rectangle overlay)
    val shelfColor = androidx.compose.ui.graphics.lerp(baseShelf, Color.Black, dimFactor * 0.65f)
    val edgeColor = androidx.compose.ui.graphics.lerp(baseEdge, Color.Black, dimFactor * 0.55f)


    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val w = size.width
        val h = size.height

        val inset = insetTop.toPx().coerceAtMost(w / 3f)
        val lip = lipHeight.toPx().coerceAtMost(h)
        val sw = sideStrokeWidth.toPx()

        // ✅ Coordonnées des 4 coins du trapèze (hors lèvre)
        val topLeft: Offset
        val topRight: Offset
        val bottomLeft: Offset
        val bottomRight: Offset

        if (view == ShelfView.BOTTOM) {
            // BOTTOM : haut court / bas long
            topLeft = Offset(inset, 0f)
            topRight = Offset(w - inset, 0f)
            bottomLeft = Offset(0f, h - lip)
            bottomRight = Offset(w, h - lip)
        } else {
            // TOP : haut long / bas court
            topLeft = Offset(0f, 0f)
            topRight = Offset(w, 0f)
            bottomLeft = Offset(inset, h - lip)
            bottomRight = Offset(w - inset, h - lip)
        }

        val path = Path().apply {
            moveTo(topLeft.x, topLeft.y)
            lineTo(topRight.x, topRight.y)
            lineTo(bottomRight.x, bottomRight.y)
            lineTo(bottomLeft.x, bottomLeft.y)
            close()
        }

        // Remplissage
        drawPath(path, color = shelfColor)

        // ✅ Bords latéraux (gauche + droite)
        val sideColor = edgeColor.copy(alpha = sideStrokeAlpha)
        drawLine(
            color = sideColor,
            start = topLeft,
            end = bottomLeft,
            strokeWidth = sw
        )
        drawLine(
            color = sideColor,
            start = topRight,
            end = bottomRight,
            strokeWidth = sw
        )

        // Lèvre : haut en TOP, bas en BOTTOM
        val lipY = if (view == ShelfView.BOTTOM) (h - lip) else 0f
        drawRect(
            color = edgeColor.copy(alpha = lipAlpha),
            topLeft = Offset(0f, lipY),
            size = Size(w, lip)
        )
    }
}








/* ——— Utils ——— */

fun formatRelativeDaysCompact(targetMillis: Long): String {
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

fun isExpired(expiry: Long): Boolean {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val target = Instant.ofEpochMilli(expiry).atZone(zone).toLocalDate()
    return target.isBefore(today)
}

// Laisser l'utilisateur modifier la valeur de "isSoon" dans Settings
fun isSoon(expiry: Long): Boolean {
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
    itemEntity: ItemEntity,
    onClose: () -> Unit,
    onOpenViewer: (String) -> Unit
) {
    val strokeColor by animateColorAsState(
        targetValue = expiryStrokeColor(itemEntity.expiryDate),
        label = "sheetStrokeColor"
    )

    Column(Modifier.fillMaxWidth()) {

        CornerRadiusEtPoignee(
            radius = 28.dp,
            strokeWidth = 2.dp,
            strokeColor = strokeColor, // couleur calculée en fonction exp Item
            handleHeight = 4.dp
        )

        Spacer(Modifier.height(5.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ItemDetailsHeader(
                itemEntity = itemEntity,
                onClose = onClose,
                onOpenViewer = onOpenViewer
            )

            DetailsOpenImageButtons(
                ingredientsUrl = itemEntity.imageIngredientsUrl,
                nutritionUrl = itemEntity.imageNutritionUrl,
                onOpenViewer = onOpenViewer
            )

        }
    }
}

@Composable
private fun DetailsOpenImageButtons(
    ingredientsUrl: String?,
    nutritionUrl: String?,
    onOpenViewer: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        DetailsTabButton(
            text = "Ingrédients",
            icon = Icons.Outlined.Science,
            selected = false,
            enabled = !ingredientsUrl.isNullOrBlank(),
            onClick = { ingredientsUrl?.let(onOpenViewer) },
            modifier = Modifier.weight(1f)
        )

        DetailsTabButton(
            text = "Nutrition",
            icon = Icons.Outlined.FactCheck,
            selected = false,
            enabled = !nutritionUrl.isNullOrBlank(),
            onClick = { nutritionUrl?.let(onOpenViewer) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ItemDetailsHeader(
    itemEntity: ItemEntity,
    onClose: () -> Unit,
    onOpenViewer: (String) -> Unit
) {
    val name = itemEntity.name?.takeIf { it.isNotBlank() } ?: "(sans nom)"
    val brand = itemEntity.brand?.takeIf { it.isNotBlank() } ?: "—"
    val nutriScore = itemEntity.nutriScore?.takeIf { it.isNotBlank() } ?: "—"
    val daysText = itemEntity.expiryDate?.let { formatRelativeDaysCompact(it) } ?: "—"

    val chip = expiryChipStyle(itemEntity.expiryDate)

    Box(Modifier.fillMaxWidth()) {

        // ✅ Contenu header normal
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Image
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable(enabled = !itemEntity.imageUrl.isNullOrBlank()) {
                        onOpenViewer(itemEntity.imageUrl!!)
                    }
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (!itemEntity.imageUrl.isNullOrBlank()) {
                    Image(
                        painter = rememberAsyncImagePainter(itemEntity.imageUrl),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text("🧺", fontSize = 22.sp)
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = name.replaceFirstChar { it.titlecase() }, // Majuscule
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = brand,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val nutriRes = nutriScoreRes(itemEntity.nutriScore)

                    if (nutriRes != null) {
                        Image(
                            painter = painterResource(nutriRes),
                            contentDescription = "Nutri-Score ${itemEntity.nutriScore}",
                            modifier = Modifier.height(22.dp)
                        )
                    } else {
                        Image(
                            painter = painterResource(R.drawable.nutri_score_neutre),
                            contentDescription = "Nutri-Score indisponible",
                            modifier = Modifier
                                .height(22.dp)
                                .alpha(0.35f)
                        )
                    }

                    Spacer(Modifier.weight(1f))

                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = { Text(daysText, fontWeight = FontWeight.SemiBold) },
                        colors = AssistChipDefaults.assistChipColors(
                            disabledContainerColor = chip.container,
                            disabledLabelColor = chip.label
                        ),
                        border = BorderStroke(1.dp, chip.border)
                    )
                }
            }
        }
    }
}

// Couleur du border top BottomSheet
@Composable
private fun expiryStrokeColor(expiry: Long?): Color {
    val base = MaterialTheme.colorScheme.primary

    if (expiry == null) return base.copy(alpha = 0.35f)

    return when {
        isExpired(expiry) -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.65f) // expiré
        isSoon(expiry) -> Color(0xFFFFC107).copy(alpha = 0.45f) // bientôt (jaune)
        else -> base.copy(alpha = 0.55f) // ok
    }
}


@Immutable
private data class ExpiryChipStyle(
    val container: Color,
    val label: Color,
    val border: Color
)

@Composable
private fun expiryChipStyle(expiry: Long?): ExpiryChipStyle {
    val cs = MaterialTheme.colorScheme

    if (expiry == null) {
        return ExpiryChipStyle(
            container = cs.surfaceVariant.copy(alpha = 0.55f),
            label = cs.onSurface.copy(alpha = 0.55f),
            border = cs.outlineVariant.copy(alpha = 0.55f)
        )
    }

    return when {
        isExpired(expiry) -> ExpiryChipStyle(
            container = cs.tertiary.copy(alpha = 0.12f),
            label = cs.tertiary.copy(alpha = 0.95f),
            border = cs.tertiary.copy(alpha = 0.35f)
        )

        isSoon(expiry) -> ExpiryChipStyle(
            container = Color(0xFFFFC107).copy(alpha = 0.16f),  // amber
            label = Color(0xFFFFC107).copy(alpha = 0.95f),
            border = Color(0xFFFFC107).copy(alpha = 0.40f)
        )

        else -> ExpiryChipStyle(
            container = cs.primary.copy(alpha = 0.10f),
            label = cs.primary.copy(alpha = 0.95f),
            border = cs.primary.copy(alpha = 0.30f)
        )
    }
}


@Composable
private fun CornerRadiusEtPoignee(
    modifier: Modifier = Modifier,
    radius: Dp = 28.dp,
    strokeWidth: Dp = 2.dp,
    strokeColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
    handleWidth: Dp = 44.dp,
    handleHeight: Dp = 4.dp
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(radius) // ✅ la zone de rayon sert à la fois au trait ET à la poignée
    ) {
        // ✅ Trait arrondi
        TopRoundedStroke(
            modifier = Modifier.matchParentSize(),
            strokeWidth = strokeWidth,
            radius = radius,
            color = strokeColor
        )

        // ✅ Poignée DANS la zone (pas en dessous)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp) // ajuste visuellement
                .width(handleWidth)
                .height(handleHeight)
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f))
        )
    }
}


@Composable
private fun TopRoundedStroke(
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 2.dp,
    radius: Dp = 28.dp,
    color: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
    edgeFadePct: Float = 0.05f // 5%
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(radius)
    ) {
        val w = size.width
        val sw = strokeWidth.toPx()
        val r = radius.toPx().coerceAtMost(w / 2f)
        val y = sw / 2f

        val leftRect = androidx.compose.ui.geometry.Rect(0f, y, 2 * r, 2 * r + y)
        val rightRect = androidx.compose.ui.geometry.Rect(w - 2 * r, y, w, 2 * r + y)

        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(0f, r + y)
            arcTo(leftRect, 180f, 90f, false)
            lineTo(w - r, y)
            arcTo(rightRect, 270f, 90f, false)
        }

        val fade = edgeFadePct.coerceIn(0f, 0.49f)

        // ✅ Brush: transparent -> color -> transparent
        val brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
            colorStops = arrayOf(
                0f to color.copy(alpha = 0f),
                fade to color,
                (1f - fade) to color,
                1f to color.copy(alpha = 0f)
            ),
            startX = 0f,
            endX = w
        )

        drawPath(
            path = path,
            brush = brush,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = sw,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round
            )
        )
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




@Composable
private fun DetailsTabButton(
    text: String,
    icon: ImageVector? = null,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = when {
        !enabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        else -> MaterialTheme.colorScheme.surface
    }

    val border = when {
        !enabled -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
        selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.80f)
    }

    val content = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f)
        selected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f)
    }

    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = content,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
            }

            Text(text, fontWeight = FontWeight.SemiBold, color = content)
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
            val painter = rememberAsyncImagePainter(url)
            val pState = painter.state

            // scrim cliquable pour fermer (optionnel, mais UX top)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.92f)
                    .clickable(enabled = pState !is AsyncImagePainter.State.Loading) { onDismiss() }
            )

            // image interactive (affichée seulement si pas en erreur)
            if (pState !is AsyncImagePainter.State.Error) {
                Image(
                    painter = painter,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp)
                        .transformable(state = state, lockRotationOnZoomPan = false)
                        .graphicsLayer {
                            translationX = offset.x
                            translationY = offset.y
                            scaleX = scale
                            scaleY = scale
                            rotationZ = rotation
                        }
                )
            }

            // ✅ Loader overlay (gris/blanc, discret)
            if (pState is AsyncImagePainter.State.Loading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(36.dp),
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }

            // ✅ Fallback (si erreur)
            if (pState is AsyncImagePainter.State.Error) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Image,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.65f),
                        modifier = Modifier.size(64.dp)
                    )
                }
            }

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
fun VegetableDrawerCube3D(
    modifier: Modifier = Modifier,
    height: Dp = 92.dp,
    depth: Dp = 16.dp,                 // profondeur du "toit"
    corner: Dp = 14.dp,                // arrondi bas uniquement (face avant)
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
    dimAlpha: Float = 0f, // ✅ NEW : dim de l’allumage frigo
    isGhost: Boolean = false,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val cs = MaterialTheme.colorScheme

    val ghostFactor = if (isGhost) 0.55f else 0f

    val dimFactor = (dimAlpha / 0.55f).coerceIn(0f, 1f) // 0..1 (même échelle que les étagères)

    val baseFace = androidx.compose.ui.graphics.lerp(cs.primary, cs.surface, 0.76f)
    val baseStroke = cs.primary.copy(alpha = 0.75f)

    // ✅ 1) GHOST : on rapproche de la surface (matière moins présente), sans transparence
    val ghostFace = androidx.compose.ui.graphics.lerp(baseFace, cs.surface, ghostFactor)
    val ghostStroke = androidx.compose.ui.graphics.lerp(baseStroke, cs.surface, ghostFactor * 0.65f)

    // ✅ 2) DIM : frigo éteint -> on assombrit (comme les étagères)
    val faceColor = androidx.compose.ui.graphics.lerp(ghostFace, Color.Black, dimFactor * 0.65f)
    val stroke = androidx.compose.ui.graphics.lerp(ghostStroke, Color.Black, dimFactor * 0.55f)

    // dessus plus clair que la face avant
    val front = faceColor.copy(alpha = 0.3f)
    val top = faceColor.copy(alpha = 0.15f)

    Box(modifier = modifier.height(height)) {
        Canvas(Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height

            val d = minOf(depth.toPx(), h * 0.35f).coerceAtLeast(8f)

            // ✅ épaisseurs séparées
            val frontSw = 1.4.dp.toPx()   // epaisseur rectangle face front
            val topSw = 0.3.dp.toPx()     // épaisseur trait stroke trapeze profondeur

            // --- Face avant : commence sous le toit
            val frontTop = d
            val frontRectTopLeft = Offset(0f, frontTop)
            val frontRectSize = Size(w, h - frontTop)

            // --- Arrondi UNIQUEMENT en bas
            val rWanted = corner.toPx()
            val rb = rWanted
                .coerceAtMost(frontRectSize.height / 2f)
                .coerceAtMost(frontRectSize.width / 2f)

            // --- Toit (parallélogramme)
            val a = Offset(0f, frontTop)     // avant-gauche
            val b = Offset(w, frontTop)      // avant-droit
            val c = Offset(w - d, 0f)        // arrière-droit
            val dPt = Offset(d, 0f)          // arrière-gauche

            val topPath = Path().apply {
                moveTo(a.x, a.y)
                lineTo(b.x, b.y)
                lineTo(c.x, c.y)
                lineTo(dPt.x, dPt.y)
                close()
            }

            // --- Path face avant : haut carré, bas arrondi
            val x0 = frontRectTopLeft.x
            val y0 = frontRectTopLeft.y
            val x1 = x0 + frontRectSize.width
            val y1 = y0 + frontRectSize.height

            val frontPath = Path().apply {
                moveTo(x0, y0)        // haut carré
                lineTo(x1, y0)

                lineTo(x1, y1 - rb)   // descente droite

                arcTo(
                    rect = androidx.compose.ui.geometry.Rect(
                        left = x1 - 2 * rb,
                        top = y1 - 2 * rb,
                        right = x1,
                        bottom = y1
                    ),
                    startAngleDegrees = 0f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )

                lineTo(x0 + rb, y1)

                arcTo(
                    rect = androidx.compose.ui.geometry.Rect(
                        left = x0,
                        top = y1 - 2 * rb,
                        right = x0 + 2 * rb,
                        bottom = y1
                    ),
                    startAngleDegrees = 90f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )

                lineTo(x0, y0)
                close()
            }

            // ====== FILL ======
            drawPath(topPath, color = top)
            if (front.alpha > 0f) drawPath(frontPath, color = front)

            // ====== STROKES ======

            // ✅ contour toit (plus fin)
            drawPath(
                path = topPath,
                color = stroke,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = topSw)
            )

            // ✅ arêtes du toit (plus fines)
            // drawLine(stroke, start = dPt, end = c, strokeWidth = topSw) // arrière top
            drawLine(stroke, start = dPt, end = a, strokeWidth = topSw) // diagonale gauche
            drawLine(stroke, start = c, end = b, strokeWidth = topSw)   // diagonale droite

            // ✅ contour face avant (plus épais)
            drawPath(
                path = frontPath,
                color = stroke,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = frontSw)
            )

            // petite ligne de relief sous le toit (optionnel)
            drawLine(
                color = stroke.copy(alpha = 0.35f),
                start = Offset(10f, frontTop + 1f),
                end = Offset(w - 10f, frontTop + 1f),
                strokeWidth = 1f
            )
        }

        // Contenu au-dessus (on évite le toit)
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(top = depth)
                .padding(contentPadding)
        ) {
            content()
        }
    }
}



@Composable
private fun Modifier.giggleEvery(
    enabled: Boolean,
    intervalMs: Long = 5_000L,
    initialDelayMs: Long = 320L, // micro délai pour laisser l’onglet “se poser”
): Modifier {
    if (!enabled) return this

    val rotation = remember { Animatable(0f) }
    val tx = remember { Animatable(0f) }
    val ty = remember { Animatable(0f) }
    val scale = remember { Animatable(1f) }

    suspend fun burst() = coroutineScope {
        val d = 110 // ✅ plus long (au lieu de 70)

        // ✅ rotation plus intense
        launch {
            rotation.animateTo(3.4f, tween(d))
            rotation.animateTo(-3.0f, tween(d))
            rotation.animateTo(2.1f, tween(d))
            rotation.animateTo(-1.6f, tween(d))
            rotation.animateTo(0f, tween(d + 40))
        }

        // ✅ micro shake un peu plus visible (toujours discret)
        launch {
            tx.animateTo(2.4f, tween(d))
            tx.animateTo(-2.0f, tween(d))
            tx.animateTo(1.2f, tween(d))
            tx.animateTo(0f, tween(d + 20))
        }

        launch {
            ty.animateTo(-1.6f, tween(d))
            ty.animateTo(1.1f, tween(d))
            ty.animateTo(-0.7f, tween(d))
            ty.animateTo(0f, tween(d + 20))
        }

        // ✅ scale plus intense + plus long, en même temps
        launch {
            scale.animateTo(1.055f, tween(d + 10))
            scale.animateTo(1f, tween(d + 110))
        }
    }

    LaunchedEffect(enabled) {
        if (!enabled) return@LaunchedEffect

        // ✅ burst au spawn (après micro délai)
        delay(initialDelayMs)
        burst()

        // ✅ ensuite cadence fixe
        while (true) {
            delay(intervalMs)
            burst()
        }
    }

    return this.graphicsLayer {
        rotationZ = rotation.value
        translationX = tx.value
        translationY = ty.value
        scaleX = scale.value
        scaleY = scale.value
    }
}



