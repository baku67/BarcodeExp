package com.example.barcode.widgets

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.text.format.DateUtils
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.datastore.preferences.core.Preferences
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.barcode.MainActivity
import com.example.barcode.R
import com.example.barcode.common.expiry.ExpiryLevel
import com.example.barcode.common.expiry.ExpiryPolicy
import com.example.barcode.common.expiry.daysUntilExpiry
import com.example.barcode.common.expiry.expiryLevel
import com.example.barcode.common.ui.expiry.ExpiryWarning
import com.example.barcode.common.ui.theme.AppMutedDark
import com.example.barcode.common.ui.theme.AppMutedLight
import com.example.barcode.common.ui.theme.AppOnSurfaceDark
import com.example.barcode.common.ui.theme.AppOnSurfaceLight
import com.example.barcode.common.ui.theme.AppPrimary
import com.example.barcode.common.ui.theme.AppWidgetBackgroundDark
import com.example.barcode.common.ui.theme.AppWidgetBackgroundLight
import com.example.barcode.common.ui.theme.AppWidgetSurfaceDark
import com.example.barcode.common.ui.theme.AppWidgetSurfaceLight
import com.example.barcode.core.SessionManager
import com.example.barcode.data.local.AppDb
import com.example.barcode.data.local.entities.ItemEntity
import com.example.barcode.data.local.entities.ShoppingListItemEntity
import com.example.barcode.sync.SyncPreferences
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min
import kotlin.math.roundToInt

private const val WidgetFridgeTextListMaxItems = 5

private const val WidgetFridgeGridColumns = 6
private const val WidgetFridgeGridRows = 2
private const val WidgetFridgeGridMaxItems = WidgetFridgeGridColumns * WidgetFridgeGridRows

private const val WidgetShoppingMaxItems = 14
private const val WidgetShoppingOneColumnMaxItems = 7

private const val WidgetProductBitmapMaxPx = 180

class FridgeWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Single

    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId
    ) {
        val appContext = context.applicationContext
        val colors = WidgetPalette.fromContext(appContext)

        val syncPrefs = SyncPreferences(appContext)
        val db = AppDb.get(appContext)
        val itemDao = db.itemDao()
        val shoppingListDao = db.shoppingListDao()
        val sessionManager = SessionManager(appContext)

        val initialFridgeItems = itemDao
            .observeFirstExpiringForWidget(limit = WidgetFridgeGridMaxItems)
            .first()

        val fridgeImageBitmaps = loadWidgetProductBitmaps(
            context = appContext,
            items = initialFridgeItems
        )

        provideContent {
            val widgetState = currentState<Preferences>()

            val displayMode = WidgetDisplayMode.fromStoredValue(
                widgetState[WidgetDisplayModeKey]
            )

            val fridgeContentMode = WidgetFridgeContentMode.fromStoredValue(
                widgetState[WidgetFridgeContentModeKey]
            )

            val shoppingScope = WidgetShoppingScope.fromStoredValue(
                widgetState[WidgetShoppingScopeKey]
            )

            val lastSyncFinishedAt by syncPrefs
                .lastSyncFinishedAt
                .collectAsState(initial = 0L)

            val isWidgetForceSyncRunning by syncPrefs
                .isWidgetForceSyncRunning
                .collectAsState(initial = false)

            val fridgeItems by itemDao
                .observeFirstExpiringForWidget(limit = WidgetFridgeGridMaxItems)
                .collectAsState(initial = emptyList())

            val currentUserId by sessionManager
                .userId
                .collectAsState(initial = null)

            val currentHomeId by sessionManager
                .currentHomeId
                .collectAsState(initial = null)

            val effectiveUserId = currentUserId
                ?.takeIf { it.isNotBlank() }
                ?: ShoppingListItemEntity.LOCAL_USER_ID

            val effectiveHomeId = currentHomeId
                ?.takeIf { it.isNotBlank() }
                ?: ShoppingListItemEntity.LOCAL_HOME_ID

            val sharedShoppingItems by remember(effectiveHomeId, effectiveUserId) {
                shoppingListDao.observeFirstUncheckedForWidget(
                    homeId = effectiveHomeId,
                    userId = effectiveUserId,
                    scope = WidgetShoppingScope.SHARED.daoValue,
                    limit = WidgetShoppingMaxItems
                )
            }.collectAsState(initial = emptyList())

            val personalShoppingItems by remember(effectiveHomeId, effectiveUserId) {
                shoppingListDao.observeFirstUncheckedForWidget(
                    homeId = effectiveHomeId,
                    userId = effectiveUserId,
                    scope = WidgetShoppingScope.PERSONAL.daoValue,
                    limit = WidgetShoppingMaxItems
                )
            }.collectAsState(initial = emptyList())

            val activeShoppingItems = when (shoppingScope) {
                WidgetShoppingScope.SHARED -> sharedShoppingItems
                WidgetShoppingScope.PERSONAL -> personalShoppingItems
            }

            val lastSyncText = lastSyncFinishedAt.toLastSyncTimeLabel()

            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(color = colors.background)
                    .padding(3.dp),
                verticalAlignment = Alignment.Vertical.Top,
                horizontalAlignment = Alignment.Horizontal.Start
            ) {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(color = colors.surface)
                        .cornerRadius(22.dp)
                        .padding(12.dp),
                    verticalAlignment = Alignment.Vertical.Top,
                    horizontalAlignment = Alignment.Horizontal.Start
                ) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Vertical.CenterVertically,
                        horizontalAlignment = Alignment.Horizontal.Start
                    ) {
                        WidgetModeChip(
                            displayMode = displayMode,
                            colors = colors
                        )

                        when (displayMode) {
                            WidgetDisplayMode.FRIDGE -> {
                                Spacer(modifier = GlanceModifier.width(4.dp))

                                WidgetFridgeContentModeChip(
                                    fridgeContentMode = fridgeContentMode,
                                    colors = colors
                                )
                            }

                            WidgetDisplayMode.SHOPPING -> {
                                Spacer(modifier = GlanceModifier.width(4.dp))

                                WidgetShoppingScopeChip(
                                    shoppingScope = shoppingScope,
                                    colors = colors
                                )
                            }
                        }

                        Spacer(modifier = GlanceModifier.defaultWeight())

                        if (isWidgetForceSyncRunning) {
                            Text(
                                text = "Sync en cours",
                                maxLines = 1,
                                style = TextStyle(
                                    color = colors.primary.toColorProvider(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.Vertical.CenterVertically,
                                horizontalAlignment = Alignment.Horizontal.End
                            ) {
                                Text(
                                    text = lastSyncText,
                                    maxLines = 1,
                                    style = TextStyle(
                                        color = colors.muted.toColorProvider(),
                                        fontSize = 11.sp
                                    )
                                )

                                Spacer(modifier = GlanceModifier.width(5.dp))

                                Image(
                                    provider = ImageProvider(R.drawable.ic_widget_refresh),
                                    contentDescription = "Forcer la synchronisation",
                                    modifier = GlanceModifier
                                        .size(22.dp)
                                        .clickable(
                                            actionRunCallback<ForceWidgetSyncActionCallback>()
                                        )
                                )
                            }
                        }
                    }

                    Spacer(modifier = GlanceModifier.height(8.dp))

                    when (displayMode) {
                        WidgetDisplayMode.FRIDGE -> {
                            Column(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .defaultWeight()
                                    .clickable(
                                        actionStartActivity(
                                            openAppIntent(
                                                context = appContext,
                                                destination = WidgetNavigation.DESTINATION_FRIDGE
                                            )
                                        )
                                    ),
                                verticalAlignment = Alignment.Vertical.Top,
                                horizontalAlignment = Alignment.Horizontal.Start
                            ) {
                                if (fridgeItems.isEmpty()) {
                                    WidgetEmptyState(colors = colors)
                                } else {
                                    when (fridgeContentMode) {
                                        WidgetFridgeContentMode.LIST -> {
                                            fridgeItems
                                                .take(WidgetFridgeTextListMaxItems)
                                                .forEach { item ->
                                                    WidgetFridgeCompactRow(
                                                        item = item,
                                                        colors = colors
                                                    )
                                                }
                                        }

                                        WidgetFridgeContentMode.GRID -> {
                                            WidgetFridgeImageGrid(
                                                items = fridgeItems.take(WidgetFridgeGridMaxItems),
                                                imageBitmaps = fridgeImageBitmaps,
                                                colors = colors
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        WidgetDisplayMode.SHOPPING -> {
                            Column(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .defaultWeight()
                                    .clickable(
                                        actionStartActivity(
                                            openAppIntent(
                                                context = appContext,
                                                destination = shoppingScope.toWidgetDestination()
                                            )
                                        )
                                    ),
                                verticalAlignment = Alignment.Vertical.Top,
                                horizontalAlignment = Alignment.Horizontal.Start
                            ) {
                                WidgetShoppingListContent(
                                    items = activeShoppingItems,
                                    shoppingScope = shoppingScope,
                                    colors = colors
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetModeChip(
    displayMode: WidgetDisplayMode,
    colors: WidgetPalette
) {
    Row(
        modifier = GlanceModifier
            .background(colors.background)
            .cornerRadius(14.dp)
            .padding(horizontal = 9.dp, vertical = 5.dp)
            .clickable(
                actionRunCallback<ToggleWidgetDisplayModeActionCallback>()
            ),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Horizontal.Start
    ) {
        Text(
            text = displayMode.label,
            maxLines = 1,
            style = TextStyle(
                color = colors.primary.toColorProvider(),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        )

        Spacer(modifier = GlanceModifier.width(4.dp))

        Image(
            provider = ImageProvider(R.drawable.ic_widget_expand_all),
            contentDescription = "Changer de vue",
            modifier = GlanceModifier.size(16.dp),
            colorFilter = ColorFilter.tint(colors.primary.toColorProvider())
        )
    }
}

@Composable
private fun WidgetFridgeContentModeChip(
    fridgeContentMode: WidgetFridgeContentMode,
    colors: WidgetPalette
) {
    Row(
        modifier = GlanceModifier
            .background(colors.background)
            .cornerRadius(14.dp)
            .padding(horizontal = 9.dp, vertical = 5.dp)
            .clickable(
                actionRunCallback<ToggleWidgetFridgeContentModeActionCallback>()
            ),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally
    ) {
        Image(
            provider = ImageProvider(fridgeContentMode.iconRes()),
            contentDescription = when (fridgeContentMode) {
                WidgetFridgeContentMode.LIST -> "Affichage liste"
                WidgetFridgeContentMode.GRID -> "Affichage grille"
            },
            modifier = GlanceModifier.size(21.dp),
            colorFilter = ColorFilter.tint(colors.primary.toColorProvider())
        )

        Spacer(modifier = GlanceModifier.width(2.dp))

        Image(
            provider = ImageProvider(R.drawable.ic_widget_expand_all),
            contentDescription = "Changer l’affichage du frigo",
            modifier = GlanceModifier.size(16.dp),
            colorFilter = ColorFilter.tint(colors.primary.toColorProvider())
        )
    }
}

private fun WidgetFridgeContentMode.iconRes(): Int {
    return when (this) {
        WidgetFridgeContentMode.LIST -> R.drawable.ic_widget_display_list
        WidgetFridgeContentMode.GRID -> R.drawable.ic_widget_display_grid
    }
}

private fun WidgetShoppingScope.iconRes(): Int {
    return when (this) {
        WidgetShoppingScope.SHARED -> R.drawable.ic_widget_scope_shared
        WidgetShoppingScope.PERSONAL -> R.drawable.ic_widget_scope_personal
    }
}

@Composable
private fun WidgetShoppingScopeChip(
    shoppingScope: WidgetShoppingScope,
    colors: WidgetPalette
) {
    Row(
        modifier = GlanceModifier
            .background(colors.background)
            .cornerRadius(14.dp)
            .padding(horizontal = 9.dp, vertical = 5.dp)
            .clickable(
                actionRunCallback<ToggleWidgetShoppingScopeActionCallback>()
            ),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally
    ) {
        Image(
            provider = ImageProvider(shoppingScope.iconRes()),
            contentDescription = when (shoppingScope) {
                WidgetShoppingScope.SHARED -> "Liste partagée"
                WidgetShoppingScope.PERSONAL -> "Liste personnelle"
            },
            modifier = GlanceModifier.size(22.dp),
            colorFilter = ColorFilter.tint(colors.primary.toColorProvider())
        )

        Spacer(modifier = GlanceModifier.width(2.dp))

        Image(
            provider = ImageProvider(R.drawable.ic_widget_expand_all),
            contentDescription = "Changer de liste de courses",
            modifier = GlanceModifier.size(16.dp),
            colorFilter = ColorFilter.tint(colors.primary.toColorProvider())
        )
    }
}

private enum class WidgetShoppingRowLayout {
    SINGLE_COLUMN,
    TWO_COLUMNS
}

@Composable
private fun WidgetShoppingListContent(
    items: List<ShoppingListItemEntity>,
    shoppingScope: WidgetShoppingScope,
    colors: WidgetPalette
) {
    if (items.isEmpty()) {
        WidgetShoppingEmptyState(
            shoppingScope = shoppingScope,
            colors = colors
        )
        return
    }

    val visibleItems = items.take(WidgetShoppingMaxItems)

    if (visibleItems.size <= WidgetShoppingOneColumnMaxItems) {
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(top = 2.dp),
            verticalAlignment = Alignment.Vertical.Top,
            horizontalAlignment = Alignment.Horizontal.Start
        ) {
            visibleItems.forEach { item ->
                WidgetShoppingCompactRow(
                    item = item,
                    colors = colors,
                    layout = WidgetShoppingRowLayout.SINGLE_COLUMN
                )
            }
        }

        return
    }

    val leftItems = visibleItems.take(WidgetShoppingOneColumnMaxItems)
    val rightItems = visibleItems.drop(WidgetShoppingOneColumnMaxItems)

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(top = 2.dp),
        verticalAlignment = Alignment.Vertical.Top,
        horizontalAlignment = Alignment.Horizontal.Start
    ) {
        Column(
            modifier = GlanceModifier.defaultWeight(),
            verticalAlignment = Alignment.Vertical.Top,
            horizontalAlignment = Alignment.Horizontal.Start
        ) {
            leftItems.forEach { item ->
                WidgetShoppingCompactRow(
                    item = item,
                    colors = colors,
                    layout = WidgetShoppingRowLayout.TWO_COLUMNS
                )
            }
        }

        Spacer(modifier = GlanceModifier.width(14.dp))

        Spacer(
            modifier = GlanceModifier
                .width(1.dp)
                .fillMaxHeight()
                .background(colors.text.copy(alpha = 0.14f))
        )

        Spacer(modifier = GlanceModifier.width(14.dp))

        Column(
            modifier = GlanceModifier.defaultWeight(),
            verticalAlignment = Alignment.Vertical.Top,
            horizontalAlignment = Alignment.Horizontal.Start
        ) {
            rightItems.forEach { item ->
                WidgetShoppingCompactRow(
                    item = item,
                    colors = colors,
                    layout = WidgetShoppingRowLayout.TWO_COLUMNS
                )
            }
        }
    }
}

@Composable
private fun WidgetShoppingCompactRow(
    item: ShoppingListItemEntity,
    colors: WidgetPalette,
    layout: WidgetShoppingRowLayout
) {
    val name = item.name
        .trim()
        .takeIf { it.isNotBlank() }
        ?: "Article"

    val quantity = item.quantity
        ?.trim()
        ?.takeIf { it.isNotBlank() }

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(18.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Horizontal.Start
    ) {
        if (item.isImportant) {
            Image(
                provider = ImageProvider(R.drawable.ic_widget_important),
                contentDescription = "Important",
                modifier = GlanceModifier.size(11.dp),
                colorFilter = ColorFilter.tint(ExpiryWarning.toColorProvider())
            )

            Spacer(modifier = GlanceModifier.width(3.dp))
        }

        Text(
            text = name,
            modifier = when (layout) {
                WidgetShoppingRowLayout.SINGLE_COLUMN -> GlanceModifier
                WidgetShoppingRowLayout.TWO_COLUMNS -> GlanceModifier.defaultWeight()
            },
            maxLines = 1,
            style = TextStyle(
                color = colors.text.toColorProvider(),
                fontSize = 12.sp,
                fontWeight = if (item.isImportant) {
                    FontWeight.Bold
                } else {
                    FontWeight.Medium
                }
            )
        )

        if (quantity != null) {
            Spacer(modifier = GlanceModifier.width(6.dp))

            Text(
                text = quantity,
                maxLines = 1,
                style = TextStyle(
                    color = colors.muted.toColorProvider(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

@Composable
private fun WidgetShoppingEmptyState(
    shoppingScope: WidgetShoppingScope,
    colors: WidgetPalette
) {
    val title = when (shoppingScope) {
        WidgetShoppingScope.SHARED -> "Liste partagée vide"
        WidgetShoppingScope.PERSONAL -> "Liste perso vide"
    }

    Text(
        text = title,
        style = TextStyle(
            color = colors.text.toColorProvider(),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    )

    Spacer(modifier = GlanceModifier.height(4.dp))

    Text(
        text = "Ajoute des articles dans l’app",
        style = TextStyle(
            color = colors.muted.toColorProvider(),
            fontSize = 12.sp
        )
    )
}

private data class WidgetPalette(
    val background: Color,
    val surface: Color,
    val primary: Color,
    val text: Color,
    val muted: Color,
    val expired: Color
) {
    companion object {
        fun fromContext(context: Context): WidgetPalette {
            val isDark = context.resources.configuration.uiMode
                .and(Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

            return if (isDark) {
                WidgetPalette(
                    background = AppWidgetBackgroundDark,
                    surface = AppWidgetSurfaceDark,
                    primary = AppPrimary,
                    text = AppOnSurfaceDark,
                    muted = AppMutedDark,
                    expired = WidgetExpiredDark
                )
            } else {
                WidgetPalette(
                    background = AppWidgetBackgroundLight,
                    surface = AppWidgetSurfaceLight,
                    primary = AppPrimary,
                    text = AppOnSurfaceLight,
                    muted = AppMutedLight,
                    expired = WidgetExpiredLight
                )
            }
        }
    }
}

private val WidgetExpiredLight = Color(0xFF7D5260)
private val WidgetExpiredDark = Color(0xFFEFB8C8)

@Composable
private fun WidgetFridgeCompactRow(
    item: ItemEntity,
    colors: WidgetPalette
) {
    val name = item.name
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?: "Sans nom"

    val expiryLabel = item.expiryDate.toWidgetExpiryDelayLabel()
    val expiryColor = item.expiryDate.toWidgetExpiryColor(colors)

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(18.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Horizontal.Start
    ) {
        Text(
            text = name,
            modifier = GlanceModifier.defaultWeight(),
            maxLines = 1,
            style = TextStyle(
                color = colors.text.toColorProvider(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        )

        Text(
            text = expiryLabel,
            maxLines = 1,
            style = TextStyle(
                color = expiryColor.toColorProvider(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
private fun WidgetEmptyState(
    colors: WidgetPalette
) {
    Text(
        text = "Aucun produit à surveiller",
        style = TextStyle(
            color = colors.text.toColorProvider(),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    )

    Spacer(modifier = GlanceModifier.height(4.dp))

    Text(
        text = "Ajoute des produits dans ton frigo",
        style = TextStyle(
            color = colors.muted.toColorProvider(),
            fontSize = 12.sp
        )
    )
}

@Composable
private fun WidgetFridgeImageGrid(
    items: List<ItemEntity>,
    imageBitmaps: Map<String, Bitmap>,
    colors: WidgetPalette
) {
    val visibleItems = items.take(WidgetFridgeGridMaxItems)
    val rows = visibleItems
        .chunked(WidgetFridgeGridColumns)
        .take(WidgetFridgeGridRows)

    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalAlignment = Alignment.Vertical.Top,
        horizontalAlignment = Alignment.Horizontal.Start
    ) {
        rows.forEachIndexed { rowIndex, rowItems ->
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Vertical.CenterVertically,
                horizontalAlignment = Alignment.Horizontal.Start
            ) {
                rowItems.forEach { item ->
                    WidgetFridgeImageTile(
                        item = item,
                        bitmap = imageBitmaps[item.id],
                        colors = colors,
                        modifier = GlanceModifier.defaultWeight()
                    )
                }

                repeat(WidgetFridgeGridColumns - rowItems.size) {
                    Spacer(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .height(54.dp)
                    )
                }
            }

            if (rowIndex < rows.lastIndex) {
                Spacer(modifier = GlanceModifier.height(7.dp))
            }
        }
    }
}

@Composable
private fun WidgetFridgeImageTile(
    item: ItemEntity,
    bitmap: Bitmap?,
    colors: WidgetPalette,
    modifier: GlanceModifier = GlanceModifier
) {
    Column(
        modifier = modifier
            .height(54.dp)
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally
    ) {
        if (bitmap != null) {
            Image(
                provider = ImageProvider(bitmap),
                contentDescription = item.name ?: "Produit",
                modifier = GlanceModifier
                    .size(50.dp)
                    .background(colors.background)
                    .cornerRadius(13.dp)
                    .padding(3.dp),
                contentScale = ContentScale.Fit
            )
        } else {
            WidgetFridgeImageFallback(
                item = item,
                colors = colors
            )
        }
    }
}

@Composable
private fun WidgetFridgeImageFallback(
    item: ItemEntity,
    colors: WidgetPalette
) {
    val initial = item.name
        ?.trim()
        ?.firstOrNull()
        ?.uppercaseChar()
        ?.toString()
        ?: "?"

    Box(
        modifier = GlanceModifier
            .size(50.dp)
            .background(colors.background)
            .cornerRadius(13.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            maxLines = 1,
            style = TextStyle(
                color = colors.primary.toColorProvider(),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

private fun Color.toColorProvider(): ColorProvider {
    return ColorProvider(this)
}

private fun Long.toLastSyncTimeLabel(): String {
    if (this <= 0L) {
        return "Dernière sync : jamais"
    }

    val time = SimpleDateFormat("HH:mm", Locale.getDefault())
        .format(Date(this))

    return if (DateUtils.isToday(this)) {
        "Dernière sync : $time"
    } else {
        val date = SimpleDateFormat("dd/MM", Locale.getDefault())
            .format(Date(this))

        "Dernière sync : $date à $time"
    }
}

private fun Long?.toWidgetExpiryDelayLabel(
    policy: ExpiryPolicy = ExpiryPolicy()
): String {
    val value = this?.takeIf { it > 0L } ?: return "—"

    val days = daysUntilExpiry(value, policy)

    return when {
        days < 0 -> "- ${-days} j"
        days > 0 -> "+ $days j"
        else -> "0 j"
    }
}

private fun Long?.toWidgetExpiryColor(
    colors: WidgetPalette,
    policy: ExpiryPolicy = ExpiryPolicy()
): Color {
    val value = this?.takeIf { it > 0L }

    return when (expiryLevel(value, policy)) {
        ExpiryLevel.NONE -> colors.muted
        ExpiryLevel.EXPIRED -> colors.expired
        ExpiryLevel.SOON -> ExpiryWarning
        ExpiryLevel.OK -> colors.primary
    }
}

private suspend fun loadWidgetProductBitmaps(
    context: Context,
    items: List<ItemEntity>
): Map<String, Bitmap> {
    val imageLoader = ImageLoader(context)

    return items
        .take(WidgetFridgeGridMaxItems)
        .mapNotNull { item ->
            val imageUrl = item.imageUrl
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: return@mapNotNull null

            runCatching {
                val request = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .size(WidgetProductBitmapMaxPx, WidgetProductBitmapMaxPx)
                    .allowHardware(false)
                    .build()

                val result = imageLoader.execute(request) as? SuccessResult
                    ?: return@runCatching null

                val drawable = result.drawable

                val sourceWidth = drawable.intrinsicWidth
                    .takeIf { it > 0 }
                    ?: WidgetProductBitmapMaxPx

                val sourceHeight = drawable.intrinsicHeight
                    .takeIf { it > 0 }
                    ?: WidgetProductBitmapMaxPx

                val scale = min(
                    1f,
                    min(
                        WidgetProductBitmapMaxPx.toFloat() / sourceWidth.toFloat(),
                        WidgetProductBitmapMaxPx.toFloat() / sourceHeight.toFloat()
                    )
                )

                val targetWidth = (sourceWidth * scale)
                    .roundToInt()
                    .coerceAtLeast(1)

                val targetHeight = (sourceHeight * scale)
                    .roundToInt()
                    .coerceAtLeast(1)

                val bitmap = drawable.toBitmap(
                    width = targetWidth,
                    height = targetHeight,
                    config = Bitmap.Config.ARGB_8888
                )

                item.id to bitmap
            }.getOrNull()
        }
        .toMap()
}

private fun openAppIntent(
    context: Context,
    destination: String
): Intent {
    return Intent(context, MainActivity::class.java)
        .putExtra(WidgetNavigation.EXTRA_DESTINATION, destination)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
}

private fun WidgetShoppingScope.toWidgetDestination(): String {
    return when (this) {
        WidgetShoppingScope.SHARED -> WidgetNavigation.DESTINATION_SHOPPING_SHARED
        WidgetShoppingScope.PERSONAL -> WidgetNavigation.DESTINATION_SHOPPING_PERSONAL
    }
}

suspend fun updateFridgeWidgets(context: Context) {
    val appContext = context.applicationContext
    val manager = GlanceAppWidgetManager(appContext)
    val glanceIds = manager.getGlanceIds(FridgeWidget::class.java)

    Log.d("FridgeWidget", "updateFridgeWidgets count=${glanceIds.size}")

    glanceIds.forEach { glanceId ->
        FridgeWidget().update(appContext, glanceId)
    }
}