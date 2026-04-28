package com.example.barcode.widgets

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapShader
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
import com.example.barcode.common.ui.theme.AppRed
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
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.ui.graphics.toArgb



/* CONSTANTEs FRIGO */
private const val WidgetFridgeTextListMaxItems = 6

private const val WidgetFridgeGridColumns = 5
private const val WidgetFridgeGridRows = 1
private const val WidgetFridgeGridMaxItems = WidgetFridgeGridColumns * WidgetFridgeGridRows

private val WidgetFridgeGridTileHeight = 90.dp
private val WidgetFridgeGridImageSize = 86.dp
private val WidgetFridgeGridImageCorner = 16.dp

private val WidgetFridgeTimelineTopSpacing = 1.dp
private val WidgetFridgeTimelineHeight = 34.dp
private val WidgetFridgeTimelineLineHeight = 2.dp
private val WidgetFridgeTimelineDotSize = 8.dp
private val WidgetFridgeTimelineLabelTopSpacing = 3.dp


/* HALO BORDER-BLURRED images item (display grid) */
private const val WidgetProductGlowCanvasPx = 420
private const val WidgetProductGlowBodyScaleRemote = 0.58f
private const val WidgetProductGlowBodyScaleTaxonomy = 0.48f
private const val WidgetProductGlowImageCornerRadiusPx = 26f

private const val WidgetProductGlowUltraFarBlurPx = 84f
private const val WidgetProductGlowFarBlurPx = 58f
private const val WidgetProductGlowOuterBlurPx = 36f
private const val WidgetProductGlowInnerBlurPx = 16f

private const val WidgetProductBitmapMaxPx = 320


/* CONSTANTEs SHOPPING LIST */

private const val WidgetShoppingMaxItems = 14
private const val WidgetShoppingOneColumnMaxItems = 7

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
            items = initialFridgeItems,
            colors = colors
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
                .fillMaxWidth()
                .padding(top = 6.dp)
                .height(WidgetFridgeTimelineLineHeight)
                .background(colors.text.copy(alpha = 0.14f))
                .cornerRadius(99.dp)
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
                    expired = AppRed
                )
            } else {
                WidgetPalette(
                    background = AppWidgetBackgroundLight,
                    surface = AppWidgetSurfaceLight,
                    primary = AppPrimary,
                    text = AppOnSurfaceLight,
                    muted = AppMutedLight,
                    expired = AppRed
                )
            }
        }
    }
}


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
            .height(21.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Horizontal.Start
    ) {
        Text(
            text = name,
            modifier = GlanceModifier.defaultWeight(),
            maxLines = 1,
            style = TextStyle(
                color = colors.text.toColorProvider(),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        )

        Spacer(modifier = GlanceModifier.width(6.dp))

        Text(
            text = expiryLabel,
            maxLines = 1,
            style = TextStyle(
                color = expiryColor.toColorProvider(),
                fontSize = 12.sp,
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

    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Horizontal.Start
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically,
            horizontalAlignment = Alignment.Horizontal.Start
        ) {
            visibleItems.forEach { item ->
                WidgetFridgeImageTile(
                    item = item,
                    bitmap = imageBitmaps[item.id],
                    colors = colors,
                    modifier = GlanceModifier.defaultWeight()
                )
            }

            repeat(WidgetFridgeGridColumns - visibleItems.size) {
                Spacer(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .height(WidgetFridgeGridTileHeight)
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(WidgetFridgeTimelineTopSpacing))

        WidgetFridgeTimeline(
            items = visibleItems,
            colors = colors
        )
    }
}

@Composable
private fun WidgetFridgeTimeline(
    items: List<ItemEntity>,
    colors: WidgetPalette
) {
    val timelineBitmap = remember(items, colors) {
        createWidgetTimelineLineBitmap(
            items = items,
            colors = colors
        )
    }

    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(WidgetFridgeTimelineHeight),
        contentAlignment = Alignment.TopStart
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(WidgetFridgeTimelineDotSize),
            verticalAlignment = Alignment.Vertical.Top,
            horizontalAlignment = Alignment.Horizontal.Start
        ) {
            Image(
                provider = ImageProvider(timelineBitmap),
                contentDescription = null,
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(WidgetFridgeTimelineDotSize),
                contentScale = ContentScale.FillBounds
            )
        }

        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.Top,
            horizontalAlignment = Alignment.Horizontal.Start
        ) {
            items.forEachIndexed { index, item ->
                val showMarker = item.shouldShowTimelineMarker(items, index)

                WidgetFridgeTimelineSlot(
                    item = item,
                    showMarker = showMarker,
                    colors = colors,
                    modifier = GlanceModifier.defaultWeight()
                )
            }

            repeat(WidgetFridgeGridColumns - items.size) {
                Spacer(modifier = GlanceModifier.defaultWeight())
            }
        }
    }
}

@Composable
private fun WidgetFridgeTimelineSlot(
    item: ItemEntity,
    showMarker: Boolean,
    colors: WidgetPalette,
    modifier: GlanceModifier = GlanceModifier
) {
    val markerColor = item.expiryDate.toWidgetExpiryColor(colors)

    Column(
        modifier = modifier,
        verticalAlignment = Alignment.Vertical.Top,
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally
    ) {
        if (showMarker) {
            Spacer(
                modifier = GlanceModifier
                    .size(WidgetFridgeTimelineDotSize)
                    .background(markerColor)
                    .cornerRadius(99.dp)
            )

            Spacer(modifier = GlanceModifier.height(WidgetFridgeTimelineLabelTopSpacing))

            Text(
                text = item.expiryDate.toWidgetTimelineLabel(),
                maxLines = 1,
                style = TextStyle(
                    color = markerColor.toColorProvider(),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        } else {
            Spacer(modifier = GlanceModifier.size(WidgetFridgeTimelineDotSize))
            Spacer(modifier = GlanceModifier.height(WidgetFridgeTimelineLabelTopSpacing))

            Text(
                text = "",
                maxLines = 1,
                style = TextStyle(
                    color = colors.muted.toColorProvider(),
                    fontSize = 9.sp
                )
            )
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
    val alertColor = item.expiryDate.toWidgetExpiryAlertColor(colors)

    val fallbackBorderBackground = alertColor
        ?.copy(alpha = 0.70f)
        ?: colors.background

    val fallbackInnerBackground = alertColor
        ?.copy(alpha = 0.08f)
        ?: colors.background

    Column(
        modifier = modifier
            .height(WidgetFridgeGridTileHeight)
            .padding(horizontal = 1.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally
    ) {
        Box(
            modifier = GlanceModifier.size(WidgetFridgeGridImageSize),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(
                    provider = ImageProvider(bitmap),
                    contentDescription = item.name ?: "Produit",
                    modifier = GlanceModifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(fallbackBorderBackground)
                        .cornerRadius(WidgetFridgeGridImageCorner)
                        .padding(1.dp),
                    contentAlignment = Alignment.Center
                ) {
                    WidgetFridgeImageFallback(
                        item = item,
                        colors = colors,
                        background = fallbackInnerBackground
                    )
                }
            }
        }
    }
}

@Composable
private fun WidgetFridgeImageFallback(
    item: ItemEntity,
    colors: WidgetPalette,
    background: Color = colors.background
) {
    val initial = item.name
        ?.trim()
        ?.firstOrNull()
        ?.uppercaseChar()
        ?.toString()
        ?: "?"

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(background)
            .cornerRadius(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            maxLines = 1,
            style = TextStyle(
                color = colors.primary.toColorProvider(),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}


private fun createWidgetTimelineLineBitmap(
    items: List<ItemEntity>,
    colors: WidgetPalette
): Bitmap {
    val width = 1200
    val height = 48

    val output = Bitmap.createBitmap(
        width,
        height,
        Bitmap.Config.ARGB_8888
    )

    val canvas = Canvas(output)

    val centerY = height / 2f
    val lineHeight = 10f
    val lineRadius = lineHeight / 2f

    val lineRect = RectF(
        0f,
        centerY - lineHeight / 2f,
        width.toFloat(),
        centerY + lineHeight / 2f
    )

    val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colors.text.copy(alpha = 0.14f).toArgb()
    }

    canvas.drawRoundRect(
        lineRect,
        lineRadius,
        lineRadius,
        basePaint
    )

    val visibleItems = items.take(WidgetFridgeGridMaxItems)

    val colorSegments = buildWidgetTimelineColorSegments(
        items = visibleItems,
        colors = colors,
        totalWidth = width.toFloat()
    )

    colorSegments.forEach { segment ->
        drawWidgetTimelineColorSegment(
            canvas = canvas,
            segment = segment,
            centerY = centerY,
            lineRect = lineRect
        )
    }

    val markers = buildWidgetTimelineMarkers(
        items = visibleItems,
        colors = colors,
        totalWidth = width.toFloat()
    )

    markers.forEach { marker ->
        drawWidgetTimelineMarkerHalo(
            canvas = canvas,
            markerX = marker.x,
            centerY = centerY,
            lineRect = lineRect,
            color = marker.color
        )
    }

    return output
}


private fun buildWidgetTimelineColorSegments(
    items: List<ItemEntity>,
    colors: WidgetPalette,
    totalWidth: Float,
    policy: ExpiryPolicy = ExpiryPolicy()
): List<WidgetTimelineColorSegmentRenderData> {
    if (items.isEmpty()) return emptyList()

    val slotWidth = totalWidth / WidgetFridgeGridColumns.toFloat()
    val segments = mutableListOf<WidgetTimelineColorSegmentRenderData>()

    var start = 0

    while (start < items.size) {
        val startLevel = items[start].toWidgetExpiryLevel(policy)
        var end = start

        while (
            end + 1 < items.size &&
            items[end + 1].toWidgetExpiryLevel(policy) == startLevel
        ) {
            end++
        }

        val startX = slotWidth * start + slotWidth * 0.08f
        val markerX = slotWidth * start + slotWidth / 2f
        val endX = (slotWidth * (end + 1) - slotWidth * 0.08f)
            .coerceAtMost(totalWidth)

        segments += WidgetTimelineColorSegmentRenderData(
            startIndex = start,
            endIndex = end,
            startX = startX,
            markerX = markerX,
            endX = endX,
            color = items[start].expiryDate.toWidgetExpiryColor(colors, policy)
        )

        start = end + 1
    }

    return segments
}

private fun drawWidgetTimelineColorSegment(
    canvas: Canvas,
    segment: WidgetTimelineColorSegmentRenderData,
    centerY: Float,
    lineRect: RectF
) {
    if (segment.endX - segment.startX <= 1f) return

    val segmentRect = RectF(
        segment.startX,
        lineRect.top,
        segment.endX,
        lineRect.bottom
    )

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            segment.markerX,
            centerY,
            segment.endX,
            centerY,
            intArrayOf(
                segment.color.copy(alpha = 0.82f).toArgb(),
                segment.color.copy(alpha = 0.64f).toArgb(),
                segment.color.copy(alpha = 0.56f).toArgb(),
                segment.color.copy(alpha = 0.20f).toArgb(),
                Color.Transparent.toArgb()
            ),
            floatArrayOf(
                0f,
                0.10f,
                0.78f,
                0.95f,
                1f
            ),
            Shader.TileMode.CLAMP
        )
    }

    canvas.drawRoundRect(
        segmentRect,
        lineRect.height() / 2f,
        lineRect.height() / 2f,
        paint
    )
}

private fun buildWidgetTimelineMarkers(
    items: List<ItemEntity>,
    colors: WidgetPalette,
    totalWidth: Float
): List<WidgetTimelineMarkerRenderData> {
    if (items.isEmpty()) return emptyList()

    val slotWidth = totalWidth / WidgetFridgeGridColumns.toFloat()

    return items.mapIndexedNotNull { index, item ->
        if (!item.shouldShowTimelineMarker(items, index)) {
            null
        } else {
            WidgetTimelineMarkerRenderData(
                x = slotWidth * index + slotWidth / 2f,
                color = item.expiryDate.toWidgetExpiryColor(colors)
            )
        }
    }
}


private fun drawWidgetTimelineMarkerHalo(
    canvas: Canvas,
    markerX: Float,
    centerY: Float,
    lineRect: RectF,
    color: Color
) {
    val haloRadius = 150f

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(
            markerX,
            centerY,
            haloRadius,
            intArrayOf(
                color.copy(alpha = 0.78f).toArgb(),
                color.copy(alpha = 0.26f).toArgb(),
                color.copy(alpha = 0.00f).toArgb()
            ),
            floatArrayOf(
                0f,
                0.42f,
                1f
            ),
            Shader.TileMode.CLAMP
        )
    }

    canvas.drawRoundRect(
        lineRect,
        lineRect.height() / 2f,
        lineRect.height() / 2f,
        paint
    )
}

private fun ItemEntity.toWidgetExpiryLevel(
    policy: ExpiryPolicy = ExpiryPolicy()
): ExpiryLevel {
    val value = expiryDate?.takeIf { it > 0L }
    return expiryLevel(value, policy)
}

private enum class WidgetImageSourceKind {
    REMOTE,
    LOCAL_TAXONOMY
}

private data class WidgetResolvedImageData(
    val data: Any,
    val sourceKind: WidgetImageSourceKind
)

private data class WidgetGlowSpec(
    val color: Color,
    val ultraFarAlpha: Float,
    val farAlpha: Float,
    val outerAlpha: Float,
    val innerAlpha: Float,
    val blurMultiplier: Float
)

private fun Long?.toWidgetExpiryGlowSpec(
    colors: WidgetPalette,
    policy: ExpiryPolicy = ExpiryPolicy()
): WidgetGlowSpec? {
    val value = this?.takeIf { it > 0L }

    return when (expiryLevel(value, policy)) {
        ExpiryLevel.EXPIRED -> WidgetGlowSpec(
            // Rouge plus lumineux / corail-rosé pour mieux ressortir sur fond sombre
            color = Color(0xFFFF6B7D),
            ultraFarAlpha = 0.16f,
            farAlpha = 0.34f,
            outerAlpha = 0.74f,
            innerAlpha = 1.00f,
            blurMultiplier = 1.18f
        )

        ExpiryLevel.SOON -> WidgetGlowSpec(
            // Jaune doré bien lumineux
            color = Color(0xFFFFD54A),
            ultraFarAlpha = 0.10f,
            farAlpha = 0.24f,
            outerAlpha = 0.56f,
            innerAlpha = 0.96f,
            blurMultiplier = 1.00f
        )

        ExpiryLevel.OK,
        ExpiryLevel.NONE -> null
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
    items: List<ItemEntity>,
    colors: WidgetPalette
): Map<String, Bitmap> {
    val imageLoader = ImageLoader(context)

    return items
        .take(WidgetFridgeGridMaxItems)
        .mapNotNull { item ->
            val resolvedImage = item.resolveWidgetImageData(context)
                ?: return@mapNotNull null

            runCatching {
                val request = ImageRequest.Builder(context)
                    .data(resolvedImage.data)
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

                val expiryGlowSpec = item.expiryDate.toWidgetExpiryGlowSpec(colors)
                val baseGlowSpec = resolvedImage.sourceKind.toWidgetBaseGlowSpec(colors)
                val bodyScale = resolvedImage.sourceKind.toWidgetBodyScale()

                item.id to bitmap.withWidgetGlow(
                    expiryGlowSpec = expiryGlowSpec,
                    baseGlowSpec = baseGlowSpec,
                    bodyScale = bodyScale
                )
            }.onFailure { error ->
                Log.e(
                    "FridgeWidget",
                    "Image load failed for item=${item.id}, name=${item.name}, imageData=${resolvedImage.data}",
                    error
                )
            }.getOrNull()
        }
        .toMap()
}


private fun ItemEntity.resolveWidgetImageData(
    context: Context
): WidgetResolvedImageData? {
    val remoteImageUrl = imageUrl
        ?.trim()
        ?.takeIf { it.isNotBlank() }

    if (remoteImageUrl != null) {
        return WidgetResolvedImageData(
            data = remoteImageUrl,
            sourceKind = WidgetImageSourceKind.REMOTE
        )
    }

    val localDrawableRes = resolveManualWidgetDrawableRes(context)
        ?: return null

    return WidgetResolvedImageData(
        data = localDrawableRes,
        sourceKind = WidgetImageSourceKind.LOCAL_TAXONOMY
    )
}

private fun ItemEntity.resolveManualWidgetDrawableRes(
    context: Context
): Int? {
    if (addMode != "manual") {
        return null
    }

    val candidates = buildList {
        manualSubtype
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let { subtype ->
                add(subtype)
                add(subtype.lowercase(Locale.ROOT))
                add("manual_subtype_${subtype.lowercase(Locale.ROOT)}")
            }

        manualType
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let { type ->
                add(type)
                add(type.lowercase(Locale.ROOT))
                add("manual_type_${type.lowercase(Locale.ROOT)}")
            }
    }
        .map { it.replace("-", "_") }
        .distinct()

    return candidates
        .firstNotNullOfOrNull { drawableName ->
            context.resources.getIdentifier(
                drawableName,
                "drawable",
                context.packageName
            ).takeIf { it != 0 }
        }
}

private fun WidgetImageSourceKind.toWidgetBodyScale(): Float {
    return when (this) {
        WidgetImageSourceKind.REMOTE -> WidgetProductGlowBodyScaleRemote
        WidgetImageSourceKind.LOCAL_TAXONOMY -> WidgetProductGlowBodyScaleTaxonomy
    }
}

private fun WidgetImageSourceKind.toWidgetBaseGlowSpec(
    colors: WidgetPalette
): WidgetGlowSpec? {
    return when (this) {
        WidgetImageSourceKind.REMOTE -> null

        WidgetImageSourceKind.LOCAL_TAXONOMY -> WidgetGlowSpec(
            color = colors.primary,
            ultraFarAlpha = 0.04f,
            farAlpha = 0.09f,
            outerAlpha = 0.18f,
            innerAlpha = 0.30f,
            blurMultiplier = 0.92f
        )
    }
}


private fun Bitmap.withWidgetGlow(
    expiryGlowSpec: WidgetGlowSpec?,
    baseGlowSpec: WidgetGlowSpec?,
    bodyScale: Float
): Bitmap {
    val shouldUseGlowCanvas = expiryGlowSpec != null || baseGlowSpec != null

    if (!shouldUseGlowCanvas) {
        return this.toRoundedBitmap(WidgetProductGlowImageCornerRadiusPx)
    }

    val source = if (config == Bitmap.Config.ARGB_8888) {
        this
    } else {
        copy(Bitmap.Config.ARGB_8888, false)
    }

    val canvasSize = WidgetProductGlowCanvasPx

    val output = Bitmap.createBitmap(
        canvasSize,
        canvasSize,
        Bitmap.Config.ARGB_8888
    )

    val canvas = Canvas(output)

    val effectiveBodyScale = bodyScale.coerceIn(0.20f, 1f)

    val maxBodySize = (canvasSize * effectiveBodyScale)
        .roundToInt()
        .coerceAtLeast(1)

    val sourceMaxSide = maxOf(source.width, source.height).coerceAtLeast(1)

    val bitmapScale = maxBodySize.toFloat() / sourceMaxSide.toFloat()

    val scaledWidth = (source.width * bitmapScale)
        .roundToInt()
        .coerceAtLeast(1)

    val scaledHeight = (source.height * bitmapScale)
        .roundToInt()
        .coerceAtLeast(1)

    val scaledBitmap = Bitmap.createScaledBitmap(
        source,
        scaledWidth,
        scaledHeight,
        true
    )

    val roundedBitmap = scaledBitmap.toRoundedBitmap(
        radiusPx = WidgetProductGlowImageCornerRadiusPx
    )

    val left = (canvasSize - roundedBitmap.width) / 2f
    val top = (canvasSize - roundedBitmap.height) / 2f

    listOfNotNull(baseGlowSpec, expiryGlowSpec).forEach { glowSpec ->
        canvas.drawBitmapGlowLayer(
            source = roundedBitmap,
            glowColor = glowSpec.color,
            left = left,
            top = top,
            blurRadius = WidgetProductGlowUltraFarBlurPx * glowSpec.blurMultiplier,
            alpha = glowSpec.ultraFarAlpha
        )

        canvas.drawBitmapGlowLayer(
            source = roundedBitmap,
            glowColor = glowSpec.color,
            left = left,
            top = top,
            blurRadius = WidgetProductGlowFarBlurPx * glowSpec.blurMultiplier,
            alpha = glowSpec.farAlpha
        )

        canvas.drawBitmapGlowLayer(
            source = roundedBitmap,
            glowColor = glowSpec.color,
            left = left,
            top = top,
            blurRadius = WidgetProductGlowOuterBlurPx * glowSpec.blurMultiplier,
            alpha = glowSpec.outerAlpha
        )

        canvas.drawBitmapGlowLayer(
            source = roundedBitmap,
            glowColor = glowSpec.color,
            left = left,
            top = top,
            blurRadius = WidgetProductGlowInnerBlurPx * glowSpec.blurMultiplier,
            alpha = glowSpec.innerAlpha
        )
    }

    val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    canvas.drawBitmap(
        roundedBitmap,
        left,
        top,
        imagePaint
    )

    if (scaledBitmap !== source) {
        scaledBitmap.recycle()
    }
    if (roundedBitmap !== scaledBitmap) {
        roundedBitmap.recycle()
    }

    return output
}


private fun Bitmap.toRoundedBitmap(
    radiusPx: Float
): Bitmap {
    val output = Bitmap.createBitmap(
        width,
        height,
        Bitmap.Config.ARGB_8888
    )

    val canvas = Canvas(output)

    val shader = BitmapShader(
        this,
        Shader.TileMode.CLAMP,
        Shader.TileMode.CLAMP
    )

    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        this.shader = shader
    }

    val rect = RectF(
        0f,
        0f,
        width.toFloat(),
        height.toFloat()
    )

    canvas.drawRoundRect(
        rect,
        radiusPx,
        radiusPx,
        paint
    )

    return output
}



private fun Canvas.drawBitmapGlowLayer(
    source: Bitmap,
    glowColor: Color,
    left: Float,
    top: Float,
    blurRadius: Float,
    alpha: Float
) {
    val offset = IntArray(2)

    val blurPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        maskFilter = BlurMaskFilter(
            blurRadius,
            BlurMaskFilter.Blur.NORMAL
        )
    }

    val alphaMask = source.extractAlpha(
        blurPaint,
        offset
    )

    val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        colorFilter = PorterDuffColorFilter(
            glowColor.copy(alpha = alpha).toArgb(),
            PorterDuff.Mode.SRC_IN
        )
    }

    drawBitmap(
        alphaMask,
        left + offset[0],
        top + offset[1],
        glowPaint
    )

    alphaMask.recycle()
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

private data class WidgetTimelineColorSegmentRenderData(
    val startIndex: Int,
    val endIndex: Int,
    val startX: Float,
    val markerX: Float,
    val endX: Float,
    val color: Color
)

private data class WidgetTimelineMarkerRenderData(
    val x: Float,
    val color: Color
)

private fun Long?.toWidgetExpiryAlertColor(
    colors: WidgetPalette,
    policy: ExpiryPolicy = ExpiryPolicy()
): Color? {
    val value = this?.takeIf { it > 0L }

    return when (expiryLevel(value, policy)) {
        ExpiryLevel.EXPIRED -> colors.expired
        ExpiryLevel.SOON -> ExpiryWarning
        ExpiryLevel.OK,
        ExpiryLevel.NONE -> null
    }
}

private fun ItemEntity.shouldShowTimelineMarker(
    items: List<ItemEntity>,
    index: Int,
    policy: ExpiryPolicy = ExpiryPolicy()
): Boolean {
    if (index == 0) return true

    val current = this.expiryDate.toWidgetTimelineDayOffset(policy)
    val previous = items[index - 1].expiryDate.toWidgetTimelineDayOffset(policy)

    return current != previous
}


private fun Long?.toWidgetTimelineLabel(
    policy: ExpiryPolicy = ExpiryPolicy()
): String {
    val days = this.toWidgetTimelineDayOffset(policy) ?: return "—"

    return when {
        days <= -2 -> "- ${-days} j"
        days == -1 -> "hier"
        days == 0 -> "auj."
        days == 1 -> "demain"
        days == 2 -> "+ 2 j"
        else -> "+ $days j"
    }
}

private fun Long?.toWidgetTimelineDayOffset(
    policy: ExpiryPolicy = ExpiryPolicy()
): Int? {
    val value = this?.takeIf { it > 0L } ?: return null
    return daysUntilExpiry(value, policy)
}