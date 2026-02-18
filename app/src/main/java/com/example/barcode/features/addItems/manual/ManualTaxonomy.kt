package com.example.barcode.features.addItems.manual

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.barcode.R
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow

val MANUAL_TYPES_WITH_SUBTYPE_IMAGE = setOf("VEGETABLES", "FRUITS", "MEAT", "FISH", "DAIRY")
val MANUAL_TYPES_DRAWER = setOf("VEGETABLES", "FRUITS")

data class ManualTypeMeta(
    val code: String,
    val title: String,
    val image: String? = null,
)

data class ManualGradientMeta(
    val colors: List<String>,
    val angleDeg: Float? = null,
)

sealed interface ManualContent {
    data class Markdown(val text: String) : ManualContent
    data class Bullets(val items: List<String>) : ManualContent
}

data class ManualSubtypeMeta(
    val code: String,
    val parentCode: String,
    val title: String,
    val image: String? = null,
    val storageDaysMin: Int? = null,
    val storageDaysMax: Int? = null,

    // ✅ mois de saison (1-12) par région (ex: EU_TEMPERATE)
    val seasons: Map<String, List<Int>>? = null,

    // ✅ tri-color pour le titre/placeholder
    val gradient: ManualGradientMeta? = null,

    // ✅ sections dynamiques
    val fridgeAdvise: ManualContent? = null,
    val healthGood: ManualContent? = null,
    val healthWarning: ManualContent? = null,
    val goodToKnow: ManualContent? = null,
)

data class ManualTaxonomy(
    val types: List<ManualTypeMeta>,
    val subtypes: List<ManualSubtypeMeta>
) {
    private val typesByCode = types.associateBy { it.code }
    private val subtypesByCode = subtypes.associateBy { it.code }
    private val subtypesByParent = subtypes.groupBy { it.parentCode }

    fun typeMeta(typeCode: String): ManualTypeMeta? = typesByCode[typeCode]
    fun subtypeMeta(subtypeCode: String): ManualSubtypeMeta? = subtypesByCode[subtypeCode]
    fun subtypesOf(typeCode: String): List<ManualSubtypeMeta> = subtypesByParent[typeCode].orEmpty()
}

object ManualTaxonomyImageResolver {

    private val byName = ConcurrentHashMap<String, Int>()
    private val typeByCode = ConcurrentHashMap<String, Int>()
    private val subtypeByCode = ConcurrentHashMap<String, Int>()

    private fun idOf(context: Context, drawableName: String?): Int {
        val name = drawableName?.trim().orEmpty()
        if (name.isBlank()) return 0

        byName[name]?.let { return it }

        val id = context.resources.getIdentifier(name, "drawable", context.packageName)
        if (id != 0) byName[name] = id // ✅ on ne cache pas 0
        return id
    }

    fun resolveTypeDrawableResId(context: Context, typeCode: String): Int {
        typeByCode[typeCode]?.let { return it }

        val tax = ManualTaxonomyRepository.peek()
        val fromJson = tax?.typeMeta(typeCode)?.image
        val fallback = "manual_type_${typeCode.lowercase()}"

        val id = idOf(context, fromJson).takeIf { it != 0 } ?: idOf(context, fallback)

        if (id != 0) typeByCode[typeCode] = id // ✅ pas de cache si 0
        return id
    }

    fun resolveSubtypeDrawableResId(context: Context, subtypeCode: String): Int {
        subtypeByCode[subtypeCode]?.let { return it }

        val tax = ManualTaxonomyRepository.peek()

        // ✅ 1) JSON → source de vérité
        val fromJson = tax?.subtypeMeta(subtypeCode)?.image
        var id = idOf(context, fromJson)

        // ✅ 2) fallback conventions (au cas où)
        if (id == 0) {
            val safe = subtypeCode.lowercase()
            val directName = if (safe.startsWith("manual_subtype_")) safe else "manual_subtype_$safe"
            id = idOf(context, directName)

            // (garde le petit fallback FRUIT_ → fruits_ si tu as du legacy)
            if (id == 0 && safe.startsWith("fruit_")) {
                id = idOf(context, "manual_subtype_fruits_${safe.removePrefix("fruit_")}")
            }
        }

        if (id != 0) subtypeByCode[subtypeCode] = id // ✅ pas de cache si 0
        return id
    }
}






@Composable
internal fun ManualTaxonomyTileCard(
    modifier: Modifier = Modifier,
    title: String,
    palette: TypePalette,
    @DrawableRes imageResId: Int,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    val surface = MaterialTheme.colorScheme.surface

    // léger “wash” du gradient pour rester cohérent quel que soit le thème
    val bg0 = lerp(surface, palette.bg0, if (selected) 0.22f else 0.14f)
    val bg1 = lerp(surface, palette.bg1, if (selected) 0.22f else 0.14f)

    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
    } else {
        palette.accent.copy(alpha = 0.30f)
    }

    val shape = RoundedCornerShape(22.dp)
    val gradient: Brush = Brush.linearGradient(listOf(bg0, bg1))
    val imageSize = 60.dp

    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = shape,
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 2.dp,
        border = BorderStroke(0.75.dp, borderColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 3.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    if (imageResId != 0) {
                        Image(
                            painter = painterResource(imageResId),
                            contentDescription = null,
                            modifier = Modifier.size(imageSize),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                Text(
                    text = title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 6.dp, end = 6.dp, bottom = 0.dp),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-6).dp, y = 6.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}


object ManualTaxonomyUiSpec {
    // IMPORTANT: c’est ça qui faisait basculer ManualType en 2 colonnes (padding trop grand)
    val screenHPad: Dp = 10.dp

    val gridGap: Dp = 4.dp
    val gridBottomPad: Dp = 2.dp

    const val tileAspect: Float = 1.05f

    val minWidthFor3Cols: Dp = 340.dp

    fun colsFor(maxWidth: Dp): Int = if (maxWidth >= minWidthFor3Cols) 3 else 2
}