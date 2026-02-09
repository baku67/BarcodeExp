package com.example.barcode.features.fridge

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import com.example.barcode.sync.SyncScheduler
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

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
) {
    val list by vm.items.collectAsState(initial = emptyList())

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
    val prefs = authVm.preferences.collectAsState(initial = UserPreferences()).value

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



    // TODO plus tard : calculer via une vraie source (enum zone, tags, etc.)
    val vegDrawerEmpty = true
    val vegDrawerOpacity = if (vegDrawerEmpty) ghostOpacity else 1f

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

    // ✅ Message d'état centré quand la liste est vide (LIST et FRIDGE)
    val emptyCenterLabel: String? = when {
        sorted.isNotEmpty() -> null
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
    val shelves = remember(sorted, selectedViewMode) {
        val base = sorted.chunked(itemsPerShelf).toMutableList()

        val isFridge = selectedViewMode == ViewMode.Fridge
        if (isFridge) {
            while (base.size < minShelvesCount) {
                base.add(emptyList())
            }
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
        ModalBottomSheet(
            onDismissRequest = { closeSheet() },
            sheetState = sheetState,
            dragHandle = null
        ) {
            ItemDetailsBottomSheet(
                itemEntity = sheetItemEntity!!,
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

                when (selectedViewMode) {

                    // LIST
                    ViewMode.List -> {
                        if (emptyCenterLabel != null) {
                            // ✅ Pas d'étagères en mode LISTE : on centre le message sur l'aire de contenu
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = emptyCenterLabel,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = ghostOpacity) // ✅ même "ghost" que Fridge
                                )
                            }
                        } else {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(sorted, key = { it.id }) { it ->
                                    val isSelected = selectionMode && selectedIds.contains(it.id)

                                    val rowAlpha by animateFloatAsState(
                                        targetValue = if (selectionMode && !isSelected) 0.45f else 1f,
                                        animationSpec = tween(durationMillis = 180),
                                        label = "listMultiSelectAlpha"
                                    )

                                    Box(modifier = Modifier.alpha(rowAlpha)) {
                                        ItemListCard(
                                            name = it.name ?: "(sans nom)",
                                            brand = it.brand,
                                            expiry = it.expiryDate,
                                            imageUrl = it.imageUrl,
                                            selected = isSelected,
                                            selectionMode = selectionMode,
                                            onClick = {
                                                if (selectionMode) toggleSelect(it.id) else sheetItemEntity = it
                                            },
                                            onLongPress = {
                                                if (!selectionMode) enterSelectionWith(it.id) else toggleSelect(it.id)
                                            },
                                            onDelete = { vm.deleteItem(it.id) }
                                        )
                                    }
                                }

                                item { Spacer(Modifier.height(4.dp)) }
                            }
                        }
                    }

                    // FRIDGE DESIGN
                    ViewMode.Fridge -> {
                        // ✅ Même si sorted est vide, shelves contient au moins 5 rangées → affichage “éteint”
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
                                    else -> 6.dp
                                }

                                if (extraTop > 0.dp) {
                                    Spacer(Modifier.height(extraTop))
                                }

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
                                    selectionMode = selectionMode,
                                    selectedIds = selectedIds,
                                    onClickItem = { item ->
                                        if (selectionMode) toggleSelect(item.id) else sheetItemEntity = item
                                    },
                                    onLongPressItem = { item ->
                                        if (!selectionMode) enterSelectionWith(item.id) else toggleSelect(item.id)
                                    },
                                    dimAlpha = dimAlpha,
                                    selectedSheetId = sheetItemEntity?.id,
                                    emptyOpacity = shelfOpacity,
                                    emptyCenterLabel = if (index == 2) emptyCenterLabel else null // message "liste vide" ou "sync" sur etagere 5 par exemple
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

                Spacer(Modifier.height(8.dp))

                if (selectionMode) {
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

                            // 🧠 Chercher recette
                            Button(
                                onClick = {
                                    SnackbarBus.show("Bientôt: recherche de recette avec les ingrédients sélectionnés.")
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null) // TODO: icône "restaurant"
                                Spacer(Modifier.width(8.dp))
                                Text("Recette", fontWeight = FontWeight.SemiBold)
                            }

                            // 🗑 Supprimer
                            OutlinedButton(
                                onClick = {
                                    removeItems(selectedIds)
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
                                !canScroll

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

/* ——— Utils ——— */

fun formatRelativeDaysCompact(targetMillis: Long): String {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val target = Instant.ofEpochMilli(targetMillis).atZone(zone).toLocalDate()
    val days = ChronoUnit.DAYS.between(today, target).toInt()
    return when {
        days == 0 -> "aujourd'hui"
        days == 1 -> "demain"
        days > 1 -> "dans ${days}j"
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
