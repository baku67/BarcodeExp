package com.example.barcode.features.fridge

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.barcode.common.bus.SnackbarBus
import com.example.barcode.common.ui.components.LocalAppTopBarState
import com.example.barcode.core.AppMode
import com.example.barcode.core.SessionManager
import com.example.barcode.data.local.entities.ItemEntity
import com.example.barcode.domain.models.FrigoLayout
import com.example.barcode.domain.models.UserPreferences
import com.example.barcode.features.addItems.ItemsViewModel
import com.example.barcode.features.addItems.manual.MANUAL_TYPES_DRAWER
import com.example.barcode.features.auth.AuthViewModel
import com.example.barcode.features.fridge.components.bottomSheetDetails.ImageViewerDialog
import com.example.barcode.features.fridge.components.bottomSheetDetails.ItemDetailsBottomSheet
import com.example.barcode.features.fridge.components.bottomSheetDetails.ViewerImage
import com.example.barcode.features.fridge.components.editItem.EditItemResult
import com.example.barcode.features.fridge.components.editItem.EditItemScreen
import com.example.barcode.features.fridge.components.fridgeDisplay.ShelfRow
import com.example.barcode.features.fridge.components.fridgeDisplay.VegetableDrawerCube3D
import com.example.barcode.features.fridge.components.listDisplay.ItemListCard
import com.example.barcode.features.fridge.components.shared.FridgeDisplayIconToggle
import com.example.barcode.features.fridge.components.shared.FridgeItemThumbnail
import com.example.barcode.sync.SyncScheduler
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class ViewMode { List, Fridge }

// TODO: bouton explicite de rafraichissement ou alors padding en haut de liste (mais caché) qui permet de ne pas activer le pull-to-refresh sans faire expres (BAD UX°)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FridgePage(
    innerPadding: PaddingValues,
    authVm: AuthViewModel,
    onAddItem: () -> Unit,
    vm: ItemsViewModel = viewModel(),
    isActive: Boolean,
    scrollToTopToken: Int = 0,
) {
    val list by vm.items.collectAsState(initial = emptyList())

    val notesCounts by vm.notesCountByItemId.collectAsState(initial = emptyMap())

    // --- Session (comme RecipesContent)
    val appContext = LocalContext.current.applicationContext
    val session = remember { SessionManager(appContext) }
    val scope = rememberCoroutineScope()

    // Etats refresh ou initial loading du sync
    val workManager = remember(appContext) { WorkManager.getInstance(appContext) }
    val syncInfos by workManager
        .getWorkInfosByTagLiveData(SyncScheduler.SYNC_TAG)
        .observeAsState(emptyList())

    val isSyncing = syncInfos.any { info ->
        when (info.state) {
            WorkInfo.State.RUNNING,
            WorkInfo.State.ENQUEUED,
            WorkInfo.State.BLOCKED -> true
            else -> false
        }
    }

    var showVegDrawerAll by remember { mutableStateOf(false) }
    val vegDrawerAllSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // ✅ Pull-to-refresh state (pour avoir l'icône qui descend pendant le geste)
    val pullState = rememberPullToRefreshState()

    // ✅ "On a déclenché un refresh" (masque l'icône dès que l'utilisateur relâche)
    var pullRefreshRequested by remember { mutableStateOf(false) }

    // ✅ L’état de refresh réellement utilisé par le pull-to-refresh UI
    val isRefreshing = pullRefreshRequested || isSyncing

    // ✅ Barre fine : anti-flicker (n'apparaît que si ça dure un peu)
    var showTopProgress by remember { mutableStateOf(false) }
    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            delay(250) // ✅ délai UX
            showTopProgress = true
        } else {
            showTopProgress = false
        }
    }

    // ✅ Quand la sync se termine, on reset le flag pull
    LaunchedEffect(isSyncing) {
        if (!isSyncing) pullRefreshRequested = false
    }

    val mode = session.appMode.collectAsState(initial = AppMode.AUTH).value
    val token = session.token.collectAsState(initial = null).value

    // Préférences User (displayFridge)
    val prefs = authVm.preferences.collectAsState(
        initial = UserPreferences(frigoLayout = FrigoLayout.DESIGN)
    ).value

    val selectedViewMode = when (prefs.frigoLayout) {
        FrigoLayout.LIST -> ViewMode.List
        FrigoLayout.DESIGN -> ViewMode.Fridge
    }

    // --- Pull-to-refresh + initial load
    var loadedForToken by rememberSaveable { mutableStateOf<String?>(null) }



    // ✅ Bac à legumes
    val vegDrawerHeight = 88.dp
    val ghostOpacity = 0.34f

    // ✅ Trapezoids des étagères vides : très discrets
    val emptyShelfOpacity = 0.18f

    // ✅ Rangée qui affiche "Aucun produit" / "Synchronisation…" : un poil plus visible
    val emptyShelfOpacityWithLabel = 0.28f

    // ✅ Étape intermédiaire : étagère juste après la dernière occupée
    val nextAfterLastOccupiedOpacity = 0.45f

    // Ecran d'édition Item:
    var editItemEntity by remember { mutableStateOf<ItemEntity?>(null) }

    // bottom sheet au clic sur ItemCard
    var sheetItemEntity by remember { mutableStateOf<ItemEntity?>(null) }
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    // Viewer plein écran (click sur images BottomSheet)
    var viewerImages by remember { mutableStateOf<List<ViewerImage>?>(null) }
    var viewerStartIndex by remember { mutableStateOf(0) }

    // ✅ Tri :
    // 1) date d'expiration (au JOUR près)
    // 2) date d'ajout (les derniers ajoutés en dernier)
    val zoneId = remember { ZoneId.systemDefault() }

    val sorted = remember(list, zoneId) {
        list.sortedWith(
            compareBy<ItemEntity> {
                it.expiryDate
                    ?.let { ms -> Instant.ofEpochMilli(ms).atZone(zoneId).toLocalDate() } // ✅ ignore l'heure
                    ?: LocalDate.MAX
            }
                .thenBy { it.addedAt ?: Long.MAX_VALUE } // ⚠️ remplace createdAt par TON champ "date d'ajout"
                .thenBy { (it.name ?: "").lowercase() }  // (optionnel) pour un ordre stable
        )
    }


    // ✅ Bac à légumes : on place ici les ajouts manuels dont le type a une image de sous-type
    // (pour l'instant : on n'affiche que ce qui rentre dans la zone visuelle)
    val vegDrawerItems = remember(sorted) {
        sorted.filter { item ->
            // ⚠️ adapte les noms de champs si besoin selon ton ItemEntity
            item.addMode == "manual" &&
                    item.manualType != null &&
                    MANUAL_TYPES_DRAWER.contains(item.manualType)
        }
    }
    val vegDrawerIds = remember(vegDrawerItems) { vegDrawerItems.map { it.id }.toSet() }

    // ✅ En mode Fridge : on retire ces items des étagères pour éviter le doublon
    val shelfSourceItems = remember(sorted, selectedViewMode, vegDrawerIds) {
        if (selectedViewMode == ViewMode.Fridge) {
            sorted.filterNot { it.id in vegDrawerIds }
        } else {
            sorted
        }
    }

    val vegDrawerEmpty = vegDrawerItems.isEmpty()

    // ✅ Message d'état centré quand la liste est vide (LIST et FRIDGE)
    // En mode Fridge : on considère aussi le bac à légumes.
    val hasAnyItemForCurrentView = when (selectedViewMode) {
        ViewMode.List -> sorted.isNotEmpty()
        ViewMode.Fridge -> shelfSourceItems.isNotEmpty() || vegDrawerItems.isNotEmpty()
    }
    val emptyCenterLabel: String? = when {
        hasAnyItemForCurrentView -> null
        isSyncing -> "Synchronisation…"
        else -> "Aucun produit"
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

    // ✅ Retirer des items (même logique partout : multi-select + menu bottom sheet)
    fun removeItems(ids: Iterable<String>) {
        ids.forEach { id -> vm.deleteItem(id) }
    }

    // ✅ Etageres grid: TOUJOURS au moins 5 rangées en mode Fridge
    val itemsPerShelf = 5
    val minShelvesCount = 5
    val shelves = remember(shelfSourceItems, selectedViewMode) {
        val base = shelfSourceItems.chunked(itemsPerShelf).toMutableList()
        val isFridge = selectedViewMode == ViewMode.Fridge
        if (isFridge) {
            while (base.size < minShelvesCount) base.add(emptyList())
        }
        base
    }

    val listState = rememberLazyListState()

    // ✅ Re-clique sur l’onglet actif => scroll-to-top (animation visible)
    LaunchedEffect(scrollToTopToken, selectedViewMode, emptyCenterLabel) {
            if (scrollToTopToken == 0) return@LaunchedEffect

            val canScrollToTop = when (selectedViewMode) {
                    ViewMode.Fridge -> true // shelves >= 5 donc item 0 existe toujours
                    ViewMode.List -> emptyCenterLabel == null // LazyColumn présent uniquement si non vide
                }

            if (canScrollToTop) {
                    runCatching { listState.animateScrollToItem(0) }
                }
        }

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
    if (!viewerImages.isNullOrEmpty()) {
        ImageViewerDialog(
            images = viewerImages!!,
            startIndex = viewerStartIndex,
            onDismiss = {
                viewerImages = null
                viewerStartIndex = 0
            }
        )
    }


    // TODO: remplacer le delay par vrai refresh VM/API
    suspend fun refreshItems() {
        if (mode == AppMode.AUTH && !token.isNullOrBlank()) {
            if (isSyncing) {
                // SnackbarBus.show("Synchronisation déjà en cours…") // relou
                return
            }
            SyncScheduler.enqueueSync(appContext)
            //SnackbarBus.show("Synchronisation lancée…")
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

    // Ecran Edition Item
    if (editItemEntity != null) {
        Dialog(
            onDismissRequest = { editItemEntity = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surface
            ) {
                EditItemScreen(
                    itemEntity = editItemEntity!!,
                    onCancel = { editItemEntity = null },
                    onSave = { result: EditItemResult ->
                        vm.updateItem(
                            id = editItemEntity!!.id,
                            name = result.name,
                            brand = result.brand,
                            expiry = result.expiryDate,
                            imageUrl = result.imageUrl,
                            imageIngredientsUrl = result.imageIngredientsUrl,
                            imageNutritionUrl = result.imageNutritionUrl,
                            nutriScore = result.nutriScore
                        )

                        editItemEntity = null
                        SnackbarBus.show("Produit modifié ✅")
                    }
                )
            }
        }
    }

    // BottomSheet
    if (sheetItemEntity != null) {
        val itemId = sheetItemEntity!!.id
        val notes by vm.observeNotes(itemId).collectAsState(initial = emptyList())

        ModalBottomSheet(
            onDismissRequest = { closeSheet() },
            sheetState = sheetState,
            dragHandle = null
        ) {
            ItemDetailsBottomSheet(
                itemEntity = sheetItemEntity!!,
                notes = notes,
                onAddNote = { text, pinned -> vm.addNote(itemId, text, pinned) },
                onDeleteNote = { noteId -> vm.deleteNote(noteId) },

                onClose = { closeSheet() },
                onOpenViewer = { images, startIndex ->
                    viewerImages = images
                    viewerStartIndex = startIndex
                },
                onEdit = { item ->
                    scope.launch {
                        sheetState.hide()
                        sheetItemEntity = null
                        editItemEntity = item
                    }
                },
                onRemove = { item ->
                    removeItems(listOf(item.id))
                    closeSheet()
                },
                onAddToFavorites = { item ->
                    SnackbarBus.show("Ajouté aux favoris : \"${item.name ?: "(sans nom)"}\" (à venir)")
                },
                onAddToShoppingList = { item ->
                    SnackbarBus.show("Ajouté à la liste de courses : \"${item.name ?: "(sans nom)"}\" (à venir)")
                }
            )
        }
    }


    if (showVegDrawerAll) {
        ModalBottomSheet(
            onDismissRequest = { showVegDrawerAll = false },
            sheetState = vegDrawerAllSheetState,
        ) {
            Text(
                text = "Bac à légumes (${vegDrawerItems.size})",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 72.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(vegDrawerItems, key = { it.id }) { item ->
                    FridgeItemThumbnail(
                        item = item,
                        size = 72.dp,
                        compact = true,
                        selectionMode = selectionMode,
                        selected = item.id in selectedIds,
                        dimAlpha = dimAlpha,
                        onClick = {
                            if (selectionMode) {
                                // ✅ multi-select : on toggle et on garde le modal ouvert
                                toggleSelect(item.id)
                            } else {
                                // ✅ mode normal : on ferme et on ouvre les détails
                                showVegDrawerAll = false
                                sheetItemEntity = item
                            }
                        },
                        onLongPress = {
                            if (!selectionMode) {
                                // ✅ on entre en multi-select ET on garde le modal ouvert
                                enterSelectionWith(item.id)
                            } else {
                                toggleSelect(item.id)
                            }
                        }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }



    // --- Auto-load 1 seule fois quand l’onglet est réellement actif
    LaunchedEffect(isActive, mode, token) {
        val canLoad = isActive && mode == AppMode.AUTH && !token.isNullOrBlank()
        if (!canLoad) return@LaunchedEffect
        if (loadedForToken == token) return@LaunchedEffect

        try {
            refreshItems()
        } finally {
            loadedForToken = token
        }
    }

    // Tout ça pour relancer l'anim open BottomSheet
    LaunchedEffect(sheetItemEntity) {
        if (sheetItemEntity != null) {
            sheetState.show()
        }
    }
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
        if (showTopProgress) {
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
            state = pullState,
            isRefreshing = isRefreshing,

            // ✅ Icône pendant le geste uniquement (pas pendant le refresh)
            indicator = {
                if (!isRefreshing) {
                    PullToRefreshDefaults.Indicator(
                        state = pullState,
                        isRefreshing = false, // ✅ force "mode drag" (pas de spinner)
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }
            },

            onRefresh = {
                if (isSyncing) return@PullToRefreshBox

                // ✅ dès que l'utilisateur relâche, on masque l'icône (et on passera à la barre)
                pullRefreshRequested = true

                scope.launch {
                    if (mode != AppMode.AUTH || token.isNullOrBlank()) {
                        SnackbarBus.show("Connecte-toi pour synchroniser.")
                        pullRefreshRequested = false // ✅ reset si pas possible
                        return@launch
                    }
                    refreshItems()
                }
            },
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {


                // ✅ VRAI “scrollable ou non”
                val canScroll by remember {
                    derivedStateOf { listState.canScrollForward || listState.canScrollBackward }
                }

                // ✅ Bac à légumes “fixe” uniquement si liste courte + mode Fridge + pas en multi-select
                val showPinnedVegDrawer =
                    selectedViewMode == ViewMode.Fridge && !selectionMode && !canScroll

                // ✅ Hauteur réservée en bas (évite que la LazyColumn change de hauteur d’un coup)
                val dockHeightTarget = when {
                    selectionMode -> 132.dp
                    showPinnedVegDrawer -> vegDrawerHeight + 10.dp + 48.dp + 10.dp
                    else -> 48.dp + 10.dp
                }

                val dockHeight by animateDpAsState(
                    targetValue = dockHeightTarget,
                    animationSpec = tween(durationMillis = 180),
                    label = "dockHeight"
                )

                Box(Modifier.fillMaxSize()) {

                    // =========================
                    // CONTENT (LISTE / FRIDGE)
                    // =========================
                    when (selectedViewMode) {

                        ViewMode.List -> {
                            if (emptyCenterLabel != null) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = emptyCenterLabel,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = ghostOpacity)
                                    )
                                }
                            } else {
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    contentPadding = PaddingValues(bottom = dockHeight + 12.dp)
                                ) {
                                    items(sorted, key = { it.id }) { it ->
                                        val isSelected = selectionMode && selectedIds.contains(it.id)
                                        val noteCount = notesCounts[it.id] ?: 0

                                        val rowAlpha by animateFloatAsState(
                                            targetValue = if (selectionMode && !isSelected) 0.45f else 1f,
                                            animationSpec = tween(durationMillis = 180),
                                            label = "listMultiSelectAlpha"
                                        )

                                        Box(modifier = Modifier.alpha(rowAlpha)) {
                                            val pkg = appContext.packageName

                                            ItemListCard(
                                                item = it,
                                                notesCount = noteCount,
                                                selected = isSelected,
                                                selectionMode = selectionMode,
                                                onClick = { if (selectionMode) toggleSelect(it.id) else sheetItemEntity = it },
                                                onLongPress = { if (!selectionMode) enterSelectionWith(it.id) else toggleSelect(it.id) },
                                                onDelete = { vm.deleteItem(it.id) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        ViewMode.Fridge -> {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                contentPadding = PaddingValues(
                                    bottom = (if (!canScroll) 8.dp else 0.dp) + dockHeight + 12.dp
                                )
                            ) {
                                itemsIndexed(shelves) { index, shelfItems ->

                                    val extraTop = when (index) {
                                        1 -> 5.dp
                                        2 -> 10.dp
                                        3 -> 10.dp
                                        4 -> 10.dp
                                        else -> 6.dp
                                    }
                                    if (extraTop > 0.dp) Spacer(Modifier.height(extraTop))

                                    val lastOccupiedIndex = shelves.indexOfLast { it.isNotEmpty() }
                                    val nextAfterLastOccupiedIndex =
                                        (lastOccupiedIndex + 1).takeIf { lastOccupiedIndex >= 0 && it < shelves.size }

                                    val shelfOpacity = when {
                                        shelfItems.isNotEmpty() -> 1f
                                        index == 2 && emptyCenterLabel != null -> emptyShelfOpacityWithLabel
                                        nextAfterLastOccupiedIndex != null && index == nextAfterLastOccupiedIndex -> nextAfterLastOccupiedOpacity
                                        else -> emptyShelfOpacity
                                    }

                                    ShelfRow(
                                        index = index,
                                        itemEntities = shelfItems,
                                        notesCountByItemId = notesCounts,
                                        selectionMode = selectionMode,
                                        selectedIds = selectedIds,
                                        onClickItem = { item -> if (selectionMode) toggleSelect(item.id) else sheetItemEntity = item },
                                        onLongPressItem = { item -> if (!selectionMode) enterSelectionWith(item.id) else toggleSelect(item.id) },
                                        dimAlpha = dimAlpha,
                                        selectedSheetId = sheetItemEntity?.id,
                                        emptyOpacity = shelfOpacity,
                                        emptyCenterLabel = if (index == 2) emptyCenterLabel else null
                                    )
                                }

                                // ✅ IMPORTANT : ne retire pas l’item du LazyColumn quand on active le multi-select
                                // ✅ IMPORTANT : ne retire pas l’item du LazyColumn quand on active le multi-select
                                item(key = "vegDrawer") {
                                    // ✅ Si on le “pin” dans le dock (liste courte + pas selection), on ne le duplique pas dans la liste
                                    if (!showPinnedVegDrawer) {
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
                                            } else {
                                                VegDrawerPreviewRow(
                                                    items = vegDrawerItems,
                                                    selectionMode = selectionMode,
                                                    selectedIds = selectedIds,
                                                    dimAlpha = dimAlpha,
                                                    onClickItem = { item ->
                                                        if (selectionMode) toggleSelect(item.id) else sheetItemEntity = item
                                                    },
                                                    onLongPressItem = { item ->
                                                        if (!selectionMode) enterSelectionWith(item.id) else toggleSelect(item.id)
                                                    },
                                                    onOpenAll = { showVegDrawerAll = true }
                                                )
                                            }
                                        }
                                    } else {
                                        // ✅ garde le key stable sans réserver de place (sinon doublon visuel)
                                        Spacer(Modifier.height(0.dp))
                                    }
                                }
                            }
                        }
                    }

                    // =========================
                    // DOCK (BOTTOM OVERLAY)
                    // =========================
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                    ) {

                        if (selectionMode) {
                            // ✅ Annuler bien lisible (et on enlève le compteur qui pollue + se voit mal)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 10.dp),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { exitSelection() },
                                    modifier = Modifier.height(40.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.Filled.Close, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Annuler", fontWeight = FontWeight.SemiBold)
                                }
                            }


                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                                    Button(
                                        onClick = {
                                            SnackbarBus.show("Bientôt: recherche de recette avec les ingrédients sélectionnés.")
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Icon(Icons.Filled.Add, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Recette", fontWeight = FontWeight.SemiBold)
                                    }

                                    Button(
                                        onClick = {
                                            removeItems(selectedIds)
                                            exitSelection()
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.errorContainer,
                                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    ) {
                                        Icon(Icons.Filled.Delete, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Retirer (${selectedIds.size})", fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        } else {
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
                                    } else {
                                        VegDrawerManualItemsGrid(
                                            items = vegDrawerItems,
                                            selectionMode = selectionMode,
                                            selectedIds = selectedIds,
                                            dimAlpha = dimAlpha,
                                            onClickItem = { item -> sheetItemEntity = item },
                                            onLongPressItem = { item -> enterSelectionWith(item.id) }
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
    }
}



@Composable
private fun VegDrawerManualItemsGrid(
    items: List<ItemEntity>,
    selectionMode: Boolean,
    selectedIds: Set<String>,
    dimAlpha: Float,
    onClickItem: (ItemEntity) -> Unit,
    onLongPressItem: (ItemEntity) -> Unit,
    thumbSize: Dp = 28.dp,
    gap: Dp = 6.dp,
) {
    if (items.isEmpty()) return

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val cols = ((maxWidth.value + gap.value) / (thumbSize.value + gap.value))
            .toInt()
            .coerceAtLeast(1)
        val rows = ((maxHeight.value + gap.value) / (thumbSize.value + gap.value))
            .toInt()
            .coerceAtLeast(1)

        val capacity = (cols * rows).coerceAtLeast(1)
        val visible = remember(items, capacity) { items.take(capacity) }
        val visibleRows = remember(visible, cols, rows) { visible.chunked(cols).take(rows) }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(gap, Alignment.Bottom)
        ) {
            visibleRows.forEach { rowItems ->
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    rowItems.forEach { item ->
                        FridgeItemThumbnail(
                            item = item,
                            size = 50.dp,
                            selected = item.id in selectedIds,
                            selectionMode = selectionMode,
                            dimAlpha = dimAlpha,
                            onClick = { onClickItem(item) },
                            onLongPress = { onLongPressItem(item) },
                            compact = true
                        )

                    }
                }
            }
        }
    }
}



@Composable
private fun VegDrawerPreviewRow(
    items: List<ItemEntity>,
    selectionMode: Boolean,
    selectedIds: Set<String>,
    dimAlpha: Float,
    onClickItem: (ItemEntity) -> Unit,
    onLongPressItem: (ItemEntity) -> Unit,
    onOpenAll: () -> Unit,
    gap: Dp = 6.dp,
) {
    if (items.isEmpty()) return

    BoxWithConstraints(Modifier.fillMaxWidth()) {

        // ✅ bouton "voir tout" uniquement si on dépasse 5
        val showMore = items.size > 5

        // ✅ 5 items, OU 4 items si on affiche le bouton
        val visibleItemCount = when {
            showMore -> 4
            else -> minOf(5, items.size)
        }

        // ✅ slots réels pour calculer la taille (items + bouton éventuel)
        val slotsForSizing = (visibleItemCount + if (showMore) 1 else 0).coerceAtLeast(1)

        // ✅ ton calcul, mais basé sur les slots réels
        val size = ((maxWidth - gap * (slotsForSizing - 1)) / slotsForSizing)
            .coerceIn(30.dp, 48.dp)

        val visible = remember(items, visibleItemCount) { items.take(visibleItemCount) }
        val remaining = (items.size - visible.size).coerceAtLeast(0)

        // ✅ centrage du groupe
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(gap),
                verticalAlignment = Alignment.Bottom
            ) {
                visible.forEach { item ->
                    FridgeItemThumbnail(
                        item = item,
                        size = size,
                        compact = true,
                        selectionMode = selectionMode,
                        selected = item.id in selectedIds,
                        dimAlpha = dimAlpha,
                        onClick = { onClickItem(item) },
                        onLongPress = { onLongPressItem(item) }
                    )
                }

                if (showMore && remaining > 0) {
                    VegDrawerMoreTile(
                        size = size,
                        remaining = remaining,
                        dimAlpha = dimAlpha,
                        onClick = onOpenAll
                    )
                }
            }
        }
    }
}


@Composable
private fun VegDrawerMoreTile(
    size: Dp,
    remaining: Int,
    dimAlpha: Float,
    onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val contentAlpha = (1f - (dimAlpha * 0.9f)).coerceIn(0.35f, 1f)

    Surface(
        modifier = Modifier
            .size(size)
            .alpha(contentAlpha)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = cs.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, cs.outlineVariant.copy(alpha = 0.55f)),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "+$remaining",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = cs.onSurfaceVariant.copy(alpha = 0.9f)
            )
        }
    }
}




/* ——— Utils ——— */

// Template ligne/étape contenu Modal d'aide (click "?"):
@Composable
private fun HelpRow(icon: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(icon, fontSize = 18.sp)
        Spacer(Modifier.width(10.dp))
        Text(text)
    }
}
