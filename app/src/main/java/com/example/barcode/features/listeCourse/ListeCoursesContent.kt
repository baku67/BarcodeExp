package com.example.barcode.features.listeCourse

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.LocalGroceryStore
import androidx.compose.material.icons.rounded.ShoppingCartCheckout
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.barcode.core.AppMode
import com.example.barcode.core.SessionManager
import kotlinx.coroutines.delay
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListeCoursesContent(innerPadding: PaddingValues, isActive: Boolean) {

    val appContext = androidx.compose.ui.platform.LocalContext.current.applicationContext
    val session = remember { SessionManager(appContext) }
    val scope = rememberCoroutineScope()

    val mode = session.appMode.collectAsState(initial = AppMode.AUTH).value
    val token = session.token.collectAsState(initial = null).value

    var refreshing by rememberSaveable { mutableStateOf(false) }

    // loader initial dédié au premier chargement
    var initialLoading by rememberSaveable { mutableStateOf(false) }
    var loadedForToken by rememberSaveable { mutableStateOf<String?>(null) }

    // --- Fake data (stateful) ---
    val items = remember {
        mutableStateListOf(
            CourseItemUi(name = "Carottes", qty = "1 kg", category = CourseFilter.FRUITS_LEGUMES, tab = CoursesTab.SHARED, note = "Prendre bio si possible"),
            CourseItemUi(name = "Avocats", qty = "x2", category = CourseFilter.FRUITS_LEGUMES, tab = CoursesTab.SHARED),
            CourseItemUi(name = "Tomates cerises", qty = "1 barquette", category = CourseFilter.FRUITS_LEGUMES, tab = CoursesTab.PERSONAL),
            CourseItemUi(name = "Citron", qty = "x3", category = CourseFilter.FRUITS_LEGUMES, tab = CoursesTab.PERSONAL),

            CourseItemUi(name = "Saumon", qty = "2 filets", category = CourseFilter.FRAIS, tab = CoursesTab.SHARED, note = "Pour bowl du soir", isFavorite = true),
            CourseItemUi(name = "Yaourt grec", qty = "1 pot", category = CourseFilter.FRAIS, tab = CoursesTab.PERSONAL),
            CourseItemUi(name = "Fromage râpé", qty = "200 g", category = CourseFilter.FRAIS, tab = CoursesTab.SHARED, isChecked = true),

            CourseItemUi(name = "Riz basmati", qty = "1 kg", category = CourseFilter.EPICERIE, tab = CoursesTab.SHARED),
            CourseItemUi(name = "Sauce soja", qty = "1 bouteille", category = CourseFilter.EPICERIE, tab = CoursesTab.PERSONAL, isChecked = true),

            CourseItemUi(name = "Liquide vaisselle", qty = "1", category = CourseFilter.MAISON, tab = CoursesTab.PERSONAL),
            CourseItemUi(name = "Sacs poubelle", qty = "30 L", category = CourseFilter.MAISON, tab = CoursesTab.SHARED, note = "Lien coulissant"),
        )
    }

    var filter by rememberSaveable { mutableStateOf(CourseFilter.ALL) }
    var tab by rememberSaveable { mutableStateOf(CoursesTab.SHARED) }

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
        // TODO: remplacer par vrai refresh VM/API
        if (mode == AppMode.AUTH && !token.isNullOrBlank()) delay(650) else delay(450)

        // petit “fake refresh” visuel sur la tab courante
        val pool = items.filter { it.tab == tab }
        if (pool.isNotEmpty()) {
            val pick = pool.random()
            val idx = items.indexOfFirst { it.id == pick.id }
            if (idx >= 0) items[idx] = items[idx].copy(isFavorite = !items[idx].isFavorite)
        }
    }

    fun addFakeItem() {
        items.add(
            0,
            CourseItemUi(
                name = "Nouveau produit",
                qty = "x1",
                category = CourseFilter.ALL,
                tab = tab,
                note = null
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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 96.dp + bottomInset)
            ) {

                item {
                    ListeCoursesHeader(
                        tab = tab,
                        onTabChange = { tab = it },
                        filter = filter,
                        onFilterChange = { filter = it },
                    )
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
                    items(toBuy, key = { it.id }) { it ->
                        CourseRow(
                            item = it,
                            onToggleChecked = {
                                val idx = items.indexOfFirst { x -> x.id == it.id }
                                if (idx >= 0) items[idx] = items[idx].copy(isChecked = !items[idx].isChecked)
                            },
                            onToggleFav = {
                                val idx = items.indexOfFirst { x -> x.id == it.id }
                                if (idx >= 0) items[idx] = items[idx].copy(isFavorite = !items[idx].isFavorite)
                            },
                            onRemove = { items.removeAll { x -> x.id == it.id } }
                        )
                    }
                }

                item {
                    Spacer(Modifier.height(2.dp))
                    SectionTitle(
                        icon = Icons.Rounded.ShoppingCartCheckout,
                        title = "Dans le panier",
                        count = inCart.size
                    )
                }

                if (inCart.isEmpty()) {
                    item { EmptyHint(text = "Aucun item validé pour l’instant.") }
                } else {
                    items(inCart, key = { it.id }) { it ->
                        CourseRow(
                            item = it,
                            onToggleChecked = {
                                val idx = items.indexOfFirst { x -> x.id == it.id }
                                if (idx >= 0) items[idx] = items[idx].copy(isChecked = !items[idx].isChecked)
                            },
                            onToggleFav = {
                                val idx = items.indexOfFirst { x -> x.id == it.id }
                                if (idx >= 0) items[idx] = items[idx].copy(isFavorite = !items[idx].isFavorite)
                            },
                            onRemove = { items.removeAll { x -> x.id == it.id } }
                        )
                    }
                }

                item {
                    Spacer(Modifier.height(6.dp))
                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Astuce: swipe = action (plus tard)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                        )
                        TextButton(onClick = { items.removeAll { it.tab == tab && it.isChecked } }) {
                            Text("Vider le panier")
                        }
                    }
                }
            }
        }

        // ✅ Bouton fixed en bas : "+ Ajouter" (style type FridgePage)
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
private fun ListeCoursesHeader(
    tab: CoursesTab,
    onTabChange: (CoursesTab) -> Unit,
    filter: CourseFilter,
    onFilterChange: (CourseFilter) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

        // ✅ Tabs beaucoup plus “en avant” : fond primary pour l’onglet sélectionné
        CoursesTabsPill(
            selected = tab,
            onSelect = onTabChange,
            modifier = Modifier.fillMaxWidth()
        )

        // ✅ Barre de filtres plus fine
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Tune,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                modifier = Modifier.size(16.dp)
            )

            FilterChipCompact(
                selected = filter == CourseFilter.ALL,
                label = CourseFilter.ALL.label,
                onClick = { onFilterChange(CourseFilter.ALL) }
            )
            FilterChipCompact(
                selected = filter == CourseFilter.FRUITS_LEGUMES,
                label = CourseFilter.FRUITS_LEGUMES.label,
                onClick = { onFilterChange(CourseFilter.FRUITS_LEGUMES) }
            )
            FilterChipCompact(
                selected = filter == CourseFilter.FRAIS,
                label = CourseFilter.FRAIS.label,
                onClick = { onFilterChange(CourseFilter.FRAIS) }
            )
        }
    }
}

@Composable
private fun CoursesTabsPill(
    selected: CoursesTab,
    onSelect: (CoursesTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme

    Row(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(cs.onSurface.copy(alpha = 0.06f))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CoursesTabPillItem(
            label = CoursesTab.SHARED.label,
            selected = selected == CoursesTab.SHARED,
            onClick = { onSelect(CoursesTab.SHARED) },
            modifier = Modifier.weight(1f)
        )
        CoursesTabPillItem(
            label = CoursesTab.PERSONAL.label,
            selected = selected == CoursesTab.PERSONAL,
            onClick = { onSelect(CoursesTab.PERSONAL) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CoursesTabPillItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme

    val bg by animateColorAsState(
        targetValue = if (selected) cs.primary else Color.Transparent,
        label = "tabBg"
    )
    val fg by animateColorAsState(
        targetValue = if (selected) cs.onPrimary else cs.onSurfaceVariant,
        label = "tabFg"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = fg,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1
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
        targetValue = if (selected) cs.primary.copy(alpha = 0.14f) else cs.onSurface.copy(alpha = 0.045f),
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
            .padding(horizontal = 9.dp, vertical = 3.dp),
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
                text = "+ Ajouter",
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
) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = cs.onSurfaceVariant.copy(alpha = 0.8f))
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

@Composable
private fun CourseRow(
    item: CourseItemUi,
    onToggleChecked: () -> Unit,
    onToggleFav: () -> Unit,
    onRemove: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme

    val alpha by animateFloatAsState(
        targetValue = if (item.isChecked) 0.55f else 1f,
        label = "rowAlpha"
    )

    // petit “jaune pâle” discret pour rappeler liste de courses
    val warmBg = cs.tertiary.copy(alpha = 0.10f)

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha),
        colors = CardDefaults.elevatedCardColors(
            containerColor = cs.surface.copy(alpha = 0.92f)
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
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
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(cs.onSurface.copy(alpha = 0.06f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (item.category) {
                        CourseFilter.FRUITS_LEGUMES -> "🥕"
                        CourseFilter.FRAIS -> "🧀"
                        CourseFilter.EPICERIE -> "🍚"
                        CourseFilter.MAISON -> "🧼"
                        CourseFilter.ALL -> "🛒"
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                val textDeco = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None

                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = textDeco
                    ),
                    color = cs.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.qty,
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant
                    )
                    if (!item.note.isNullOrBlank()) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "• ${item.note}",
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurfaceVariant.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(Modifier.width(10.dp))

            FilledTonalIconButton(onClick = onToggleChecked) {
                Icon(
                    imageVector = if (item.isChecked) Icons.Rounded.Check else Icons.Rounded.LocalGroceryStore,
                    contentDescription = "Cocher"
                )
            }

            Spacer(Modifier.width(6.dp))

            // Favori (rose rougeâtre)
            val favTint = Color(0xFFE91E63)
            FilledTonalIconButton(onClick = onToggleFav) {
                Icon(
                    imageVector = if (item.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = "Favori",
                    tint = if (item.isFavorite) favTint else cs.onSurfaceVariant
                )
            }

            Spacer(Modifier.width(6.dp))

            // Retirer (danger)
            FilledTonalIconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Retirer",
                    tint = cs.error
                )
            }
        }
    }
}