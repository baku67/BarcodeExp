package com.example.barcode.widgets

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.text.format.DateUtils
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.ColorFilter
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
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
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FridgeWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Single

    override val stateDefinition = androidx.glance.state.PreferencesGlanceStateDefinition

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

        provideContent {
            val lastSyncFinishedAt by syncPrefs
                .lastSyncFinishedAt
                .collectAsState(initial = 0L)

            val isWidgetForceSyncRunning by syncPrefs
                .isWidgetForceSyncRunning
                .collectAsState(initial = false)

            val fridgeItems by itemDao
                .observeFirstExpiringForWidget(limit = 10)
                .collectAsState(initial = emptyList())

            val lastSyncText = lastSyncFinishedAt.toLastSyncTimeLabel()

            val widgetState = currentState<Preferences>()

            val displayMode = WidgetDisplayMode.fromStoredValue(
                widgetState[WidgetDisplayModeKey]
            )

            val shoppingScope = WidgetShoppingScope.fromStoredValue(
                widgetState[WidgetShoppingScopeKey]
            )

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
                    limit = 10
                )
            }.collectAsState(initial = emptyList())

            val personalShoppingItems by remember(effectiveHomeId, effectiveUserId) {
                shoppingListDao.observeFirstUncheckedForWidget(
                    homeId = effectiveHomeId,
                    userId = effectiveUserId,
                    scope = WidgetShoppingScope.PERSONAL.daoValue,
                    limit = 10
                )
            }.collectAsState(initial = emptyList())

            val activeShoppingItems = when (shoppingScope) {
                WidgetShoppingScope.SHARED -> sharedShoppingItems
                WidgetShoppingScope.PERSONAL -> personalShoppingItems
            }

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

                        if (displayMode == WidgetDisplayMode.SHOPPING) {
                            Spacer(modifier = GlanceModifier.width(4.dp))

                            WidgetShoppingScopeChip(
                                shoppingScope = shoppingScope,
                                colors = colors
                            )
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
                                    fridgeItems.take(5).forEach { item ->
                                        WidgetFridgeCompactRow(
                                            item = item,
                                            colors = colors
                                        )
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
                                                destination = WidgetNavigation.DESTINATION_SHOPPING
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

        Spacer(modifier = GlanceModifier.width(0.dp))

        Image(
            provider = ImageProvider(R.drawable.ic_widget_expand_all),
            contentDescription = "Changer de liste de courses",
            modifier = GlanceModifier.size(16.dp),
            colorFilter = ColorFilter.tint(colors.primary.toColorProvider())
        )
    }
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

    val visibleItems = items.take(6)
    val splitIndex = (visibleItems.size + 1) / 2
    val leftItems = visibleItems.take(splitIndex)
    val rightItems = visibleItems.drop(splitIndex)

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
                    colors = colors
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
                    colors = colors
                )
            }
        }
    }
}

@Composable
private fun WidgetShoppingCompactRow(
    item: ShoppingListItemEntity,
    colors: WidgetPalette
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
            modifier = GlanceModifier.defaultWeight(),
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
            Spacer(modifier = GlanceModifier.width(4.dp))

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


suspend fun updateFridgeWidgets(context: Context) {
    val appContext = context.applicationContext
    val manager = GlanceAppWidgetManager(appContext)
    val glanceIds = manager.getGlanceIds(FridgeWidget::class.java)

    Log.d("FridgeWidget", "updateFridgeWidgets count=${glanceIds.size}")

    glanceIds.forEach { glanceId ->
        FridgeWidget().update(appContext, glanceId)
    }
}