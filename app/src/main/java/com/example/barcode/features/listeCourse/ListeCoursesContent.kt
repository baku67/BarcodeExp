package com.example.barcode.features.listeCourse

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Comment
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LocalGroceryStore
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.ShoppingCartCheckout
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.barcode.common.bus.AppSnackbarEvent
import com.example.barcode.common.bus.SnackbarBus
import com.example.barcode.common.ui.components.LocalAppTopBarState
import com.example.barcode.core.AppMode
import com.example.barcode.core.SessionManager
import com.example.barcode.domain.models.AppIcon
import com.example.barcode.features.fridge.components.shared.SegIcon
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.UUID

private enum class CoursesTab(val label: String) {
    SHARED("Partagée"),
    PERSONAL("Personnelle"),
}

private enum class CourseFilter(val label: String) {
    ALL("Tout"),
    FRUITS_LEGUMES("Fruits & légumes"),
    FRAIS("Frais"),
    EPICERIE("Épicerie"),
    VIANDE("Viande"),
    POISSON("Poisson"),
    MAISON("Maison"),
}

private data class CourseItemUi(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val qty: String,
    val category: CourseFilter,
    val tab: CoursesTab,
    val note: String? = null,
    val isFavorite: Boolean = false,
    val isChecked: Boolean = false,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ListeCoursesContent(
    innerPadding: PaddingValues,
    isActive: Boolean,
) {
    val appContext = androidx.compose.ui.platform.LocalContext.current.applicationContext
    val session = remember { SessionManager(appContext) }
    val scope = rememberCoroutineScope()

    val mode = session.appMode.collectAsState(initial = AppMode.AUTH).value
    val token = session.token.collectAsState(initial = null).value

    var refreshing by rememberSaveable { mutableStateOf(false) }
    var initialLoading by rememberSaveable { mutableStateOf(false) }
    var loadedForToken by rememberSaveable { mutableStateOf<String?>(null) }

    val items = remember {
        mutableStateListOf(
            CourseItemUi(
                name = "Carottes fanes extra fraîches",
                qty = "1 kg",
                category = CourseFilter.FRUITS_LEGUMES,
                tab = CoursesTab.SHARED,
                note = "Prendre bio si possible"
            ),
            CourseItemUi(
                name = "Avocats",
                qty = "x2",
                category = CourseFilter.FRUITS_LEGUMES,
                tab = CoursesTab.SHARED
            ),
            CourseItemUi(
                name = "Tomates cerises",
                qty = "1 barquette",
                category = CourseFilter.FRUITS_LEGUMES,
                tab = CoursesTab.PERSONAL
            ),
            CourseItemUi(
                name = "Citron jaune non traité",
                qty = "x3",
                category = CourseFilter.FRUITS_LEGUMES,
                tab = CoursesTab.PERSONAL
            ),
            CourseItemUi(
                name = "Saumon",
                qty = "2 filets",
                category = CourseFilter.FRAIS,
                tab = CoursesTab.SHARED,
                note = "Pour bowl du soir",
                isFavorite = true
            ),
            CourseItemUi(
                name = "Yaourt grec",
                qty = "1 pot",
                category = CourseFilter.FRAIS,
                tab = CoursesTab.PERSONAL
            ),
            CourseItemUi(
                name = "Fromage râpé",
                qty = "200 g",
                category = CourseFilter.FRAIS,
                tab = CoursesTab.SHARED,
                isChecked = true
            ),
            CourseItemUi(
                name = "Riz basmati",
                qty = "1 kg",
                category = CourseFilter.EPICERIE,
                tab = CoursesTab.SHARED
            ),
            CourseItemUi(
                name = "Sauce soja",
                qty = "1 bouteille",
                category = CourseFilter.EPICERIE,
                tab = CoursesTab.PERSONAL,
                isChecked = true
            ),
            CourseItemUi(
                name = "Liquide vaisselle",
                qty = "1",
                category = CourseFilter.MAISON,
                tab = CoursesTab.PERSONAL
            ),
            CourseItemUi(
                name = "Sacs poubelle",
                qty = "30 L",
                category = CourseFilter.MAISON,
                tab = CoursesTab.SHARED,
                note = "Lien coulissant"
            ),
        )
    }

    var filter by rememberSaveable { mutableStateOf(CourseFilter.ALL) }
    var tab by rememberSaveable { mutableStateOf(CoursesTab.SHARED) }

    val topBarState = LocalAppTopBarState.current
    val owner = "shopping_list"
    var showHelp by rememberSaveable { mutableStateOf(false) }

    if (showHelp) {
        Dialog(onDismissRequest = { showHelp = false }) {
            ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Comment utiliser la liste de courses",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    ShoppingHelpRow("👆", "Appui court sur un article pour le basculer entre À acheter et Terminé")
                    ShoppingHelpRow("✋", "Appui long sur un article pour ouvrir le menu d’options")
                    ShoppingHelpRow("⭐", "Ajoute ou retire un produit des favoris depuis le menu")
                    ShoppingHelpRow("🗑", "Supprime un article depuis l’appui long ou vide les articles cochés")
                    ShoppingHelpRow("👥", "Utilise le switch en haut pour passer entre la liste partagée et personnelle")
                    ShoppingHelpRow("⬇️", "Tire vers le bas pour rafraîchir la liste")

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

    DisposableEffect(topBarState, isActive, tab) {
        if (isActive) {
            topBarState.subtitle = tab.label
            topBarState.clearTitleTrailing("items")
            topBarState.setTitleTrailing(owner) {
                IconButton(
                    onClick = { showHelp = true },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.HelpOutline,
                        contentDescription = "Aide",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }
            }
            topBarState.setActions(owner) {
                CoursesScopeIconToggle(
                    selected = tab,
                    onSelect = { tab = it }
                )
            }
        } else {
            topBarState.clearActions(owner)
            topBarState.clearTitleTrailing(owner)
        }

        onDispose {
            topBarState.clearActions(owner)
            topBarState.clearTitleTrailing(owner)
        }
    }

    val availableFilters by remember(tab) {
        derivedStateOf {
            val present = items
                .asSequence()
                .filter { it.tab == tab }
                .map { it.category }
                .toSet()

            buildList {
                add(CourseFilter.ALL)
                addAll(CourseFilter.values().filter { it != CourseFilter.ALL && it in present })
            }
        }
    }

    LaunchedEffect(availableFilters) {
        if (filter != CourseFilter.ALL && filter !in availableFilters) {
            filter = CourseFilter.ALL
        }
    }

    val listState = rememberLazyListState()
    var stickyFiltersVisible by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(listState) {
        var previousIndex = 0
        var previousOffset = 0

        snapshotFlow {
            listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        }.collect { (index, offset) ->
            val isAtTop = index == 0 && offset <= 8
            val scrollingDown = index > previousIndex || (index == previousIndex && offset > previousOffset + 6)
            val scrollingUp = index < previousIndex || (index == previousIndex && offset < previousOffset - 6)

            stickyFiltersVisible = when {
                isAtTop -> true
                scrollingDown -> false
                scrollingUp -> true
                else -> stickyFiltersVisible
            }

            previousIndex = index
            previousOffset = offset
        }
    }

    val filtered by remember(items, filter, tab) {
        derivedStateOf {
            items
                .asSequence()
                .filter { it.tab == tab }
                .filter { filter == CourseFilter.ALL || it.category == filter }
                .toList()
        }
    }

    val (toBuy, inCart) = remember(filtered) { filtered.partition { !it.isChecked } }

    suspend fun refreshListeCourses() {
        if (mode == AppMode.AUTH && !token.isNullOrBlank()) delay(650) else delay(450)

        val pool = items.filter { it.tab == tab }
        if (pool.isNotEmpty()) {
            val pick = pool.random()
            val idx = items.indexOfFirst { it.id == pick.id }
            if (idx >= 0) items[idx] = items[idx].copy(isFavorite = !items[idx].isFavorite)
        }
    }

    fun addFakeItem() {
        val itemCategory = if (filter == CourseFilter.ALL) CourseFilter.EPICERIE else filter
        items.add(
            0,
            CourseItemUi(
                name = "Nouveau produit",
                qty = "x1",
                category = itemCategory,
                tab = tab,
            )
        )
        SnackbarBus.show("Produit ajouté")
    }

    fun toggleChecked(itemId: String) {
        val idx = items.indexOfFirst { it.id == itemId }
        if (idx < 0) return

        val updated = items[idx].copy(isChecked = !items[idx].isChecked)
        items[idx] = updated
    }

    fun toggleFavoriteWithFeedback(itemId: String) {
        val idx = items.indexOfFirst { it.id == itemId }
        if (idx < 0) return

        val updated = items[idx].copy(isFavorite = !items[idx].isFavorite)
        items[idx] = updated

        SnackbarBus.show(
            if (updated.isFavorite) "Ajouté aux favoris" else "Retiré des favoris"
        )
    }

    fun removeItemWithUndo(itemId: String) {
        val idx = items.indexOfFirst { it.id == itemId }
        if (idx < 0) return

        val removed = items[idx]
        items.removeAt(idx)

        SnackbarBus.show(
            AppSnackbarEvent(
                message = "Produit supprimé",
                actionLabel = "Annuler",
                duration = SnackbarDuration.Long,
                onAction = {
                    val safeIndex = idx.coerceIn(0, items.size)
                    items.add(safeIndex, removed)
                }
            )
        )
    }

    fun clearCheckedItemsWithUndo() {
        val removed = items.mapIndexedNotNull { index, item ->
            if (item.tab == tab && item.isChecked) index to item else null
        }

        if (removed.isEmpty()) return

        removed
            .asReversed()
            .forEach { (index, _) -> items.removeAt(index) }

        SnackbarBus.show(
            AppSnackbarEvent(
                message = "Liste vidée",
                actionLabel = "Annuler",
                duration = SnackbarDuration.Long,
                onAction = {
                    removed.forEach { (index, item) ->
                        val safeIndex = index.coerceIn(0, items.size)
                        items.add(safeIndex, item)
                    }
                }
            )
        )
    }

    LaunchedEffect(isActive, mode, token) {
        val canLoad = isActive && mode == AppMode.AUTH && !token.isNullOrBlank()
        if (!canLoad) return@LaunchedEffect
        if (loadedForToken == token) return@LaunchedEffect

        initialLoading = true
        try {
            refreshListeCourses()
        } finally {
            initialLoading = false
            loadedForToken = token
        }
    }

    val bottomInset = innerPadding.calculateBottomPadding()

    Box(Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = {
                scope.launch {
                    refreshing = true
                    refreshListeCourses()
                    refreshing = false
                }
            },
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 96.dp + bottomInset)
            ) {
                stickyHeader {
                    AnimatedVisibility(
                        visible = stickyFiltersVisible,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        StickyListeCoursesHeader(
                            filter = filter,
                            availableFilters = availableFilters,
                            onFilterChange = { filter = it },
                        )
                    }
                }

                item {
                    SectionTitle(
                        icon = Icons.Rounded.LocalGroceryStore,
                        title = "À acheter",
                        count = toBuy.size
                    )
                }

                if (toBuy.isEmpty()) {
                    item { EmptyHint(text = "Rien à acheter — tu es à jour 👌") }
                } else {
                    items(toBuy, key = { it.id }) { item ->
                        CourseRow(
                            item = item,
                            onToggleChecked = { toggleChecked(item.id) },
                            onToggleFav = { toggleFavoriteWithFeedback(item.id) },
                            onRemove = { removeItemWithUndo(item.id) }
                        )
                    }
                }

                item {
                    Spacer(Modifier.height(2.dp))
                    SectionTitle(
                        icon = Icons.Rounded.ShoppingCartCheckout,
                        title = "Terminé",
                        count = inCart.size,
                        trailing = {
                            if (inCart.isNotEmpty()) {
                                IconButton(onClick = { clearCheckedItemsWithUndo() }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Delete,
                                        contentDescription = "Vider les éléments terminés",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    )
                }

                if (inCart.isEmpty()) {
                    item { EmptyHint(text = "Aucun item validé pour l’instant.") }
                } else {
                    items(inCart, key = { it.id }) { item ->
                        CourseRow(
                            item = item,
                            onToggleChecked = { toggleChecked(item.id) },
                            onToggleFav = { toggleFavoriteWithFeedback(item.id) },
                            onRemove = { removeItemWithUndo(item.id) }
                        )
                    }
                }

                item {
                    Spacer(Modifier.height(6.dp))
                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    Text(
                        text = "Appui simple = valider. Appui long = options.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                    )
                }
            }
        }

        BottomAddBar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp)
                .padding(bottom = 12.dp + bottomInset),
            onClick = { addFakeItem() }
        )
    }
}

@Composable
private fun StickyListeCoursesHeader(
    filter: CourseFilter,
    availableFilters: List<CourseFilter>,
    onFilterChange: (CourseFilter) -> Unit,
) {
    val cs = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cs.surface.copy(alpha = 0.98f))
            .border(
                width = 1.dp,
                color = cs.outlineVariant.copy(alpha = 0.45f),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        ListeCoursesHeader(
            filter = filter,
            availableFilters = availableFilters,
            onFilterChange = onFilterChange,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ListeCoursesHeader(
    filter: CourseFilter,
    availableFilters: List<CourseFilter>,
    onFilterChange: (CourseFilter) -> Unit,
) {
    val cs = MaterialTheme.colorScheme

    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(cs.primary.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Tune,
                contentDescription = "Filtres",
                tint = cs.primary,
                modifier = Modifier.size(13.dp)
            )
        }

        availableFilters.forEach { currentFilter ->
            FilterChipCompact(
                selected = filter == currentFilter,
                label = currentFilter.label,
                onClick = { onFilterChange(currentFilter) }
            )
        }
    }
}

@Composable
private fun CoursesScopeIconToggle(
    selected: CoursesTab,
    onSelect: (CoursesTab) -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    val borderColor = MaterialTheme.colorScheme.outlineVariant

    Row(
        modifier = Modifier
            .clip(shape)
            .border(1.dp, borderColor, shape)
            .padding(2.dp)
    ) {
        SegIcon(
            active = selected == CoursesTab.SHARED,
            icon = AppIcon.Vector(Icons.Rounded.People),
            onClick = { onSelect(CoursesTab.SHARED) },
            shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
        )

        SegIcon(
            active = selected == CoursesTab.PERSONAL,
            icon = AppIcon.Vector(Icons.Rounded.Person),
            onClick = { onSelect(CoursesTab.PERSONAL) },
            shape = RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp)
        )
    }
}

@Composable
private fun FilterChipCompact(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val bg by animateColorAsState(
        targetValue = if (selected) cs.primary.copy(alpha = 0.14f) else cs.onSurface.copy(alpha = 0.04f),
        label = "chipBg"
    )
    val fg by animateColorAsState(
        targetValue = if (selected) cs.primary else cs.onSurfaceVariant,
        label = "chipFg"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = fg,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
            ),
            maxLines = 1
        )
    }
}

@Composable
private fun BottomAddBar(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme

    Box(modifier = modifier.fillMaxWidth()) {
        Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = cs.primary,
                contentColor = cs.onPrimary
            ),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null)
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Ajouter",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
        }
    }
}

@Composable
private fun SectionTitle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    count: Int,
    trailing: @Composable (() -> Unit)? = null,
) {
    val cs = MaterialTheme.colorScheme

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = cs.onSurfaceVariant.copy(alpha = 0.8f)
        )

        Spacer(Modifier.width(8.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = cs.onSurface
        )

        Spacer(Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(cs.onSurface.copy(alpha = 0.07f))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = cs.onSurfaceVariant
            )
        }

        Spacer(Modifier.weight(1f))

        trailing?.invoke()
    }
}

@Composable
private fun EmptyHint(text: String) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cs.onSurface.copy(alpha = 0.04f))
            .padding(14.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = cs.onSurfaceVariant.copy(alpha = 0.85f)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CourseRow(
    item: CourseItemUi,
    onToggleChecked: () -> Unit,
    onToggleFav: () -> Unit,
    onRemove: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current

    var menuExpanded by rememberSaveable(item.id) { mutableStateOf(false) }

    val alpha by animateFloatAsState(
        targetValue = if (item.isChecked) 0.60f else 1f,
        label = "rowAlpha"
    )
    val scale by animateFloatAsState(
        targetValue = if (item.isChecked) 0.992f else 1f,
        label = "rowScale"
    )

    val warmBg by animateColorAsState(
        targetValue = if (item.isChecked) {
            cs.tertiary.copy(alpha = 0.07f)
        } else {
            cs.tertiary.copy(alpha = 0.10f)
        },
        label = "rowBg"
    )

    val emoji = when (item.category) {
        CourseFilter.FRUITS_LEGUMES -> "🥕"
        CourseFilter.FRAIS -> "🧀"
        CourseFilter.EPICERIE -> "🍚"
        CourseFilter.VIANDE -> "🥩"
        CourseFilter.POISSON -> "🐟"
        CourseFilter.MAISON -> "🧼"
        CourseFilter.ALL -> "🛒"
    }

    val titleDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None

    Box {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(alpha)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .combinedClickable(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggleChecked()
                    },
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuExpanded = true
                    }
                ),
            colors = CardDefaults.elevatedCardColors(
                containerColor = cs.surface.copy(alpha = 0.92f)
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(warmBg)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(cs.onSurface.copy(alpha = 0.06f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = emoji,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = item.name,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                textDecoration = titleDecoration
                            ),
                            color = cs.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (item.isFavorite) {
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Rounded.Favorite,
                                contentDescription = "Favori",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .size(16.dp)
                            )
                        }
                    }

                    Text(
                        text = item.qty,
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant
                    )

                    if (!item.note.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Comment,
                                contentDescription = null,
                                tint = cs.onSurfaceVariant.copy(alpha = 0.75f),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = item.note,
                                style = MaterialTheme.typography.bodySmall,
                                color = cs.onSurfaceVariant.copy(alpha = 0.88f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false }
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        if (item.isFavorite) "Retirer des favoris" else "Ajouter aux favoris"
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = if (item.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.Star,
                        contentDescription = null
                    )
                },
                onClick = {
                    menuExpanded = false
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onToggleFav()
                }
            )

            DropdownMenuItem(
                text = { Text("Supprimer") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = null,
                        tint = cs.error
                    )
                },
                onClick = {
                    menuExpanded = false
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onRemove()
                }
            )
        }
    }
}

@Composable
private fun ShoppingHelpRow(icon: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = icon)
        Spacer(Modifier.width(10.dp))
        Text(text = text)
    }
}
