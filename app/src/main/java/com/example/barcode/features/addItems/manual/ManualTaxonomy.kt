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

data class ManualSubtypeMeta(
    val code: String,
    val parentCode: String,
    val title: String,
    val image: String? = null,
    val storageDaysMin: Int? = null,
    val storageDaysMax: Int? = null,
    val goodToKnow: String? = null,

    // ✅ NEW: venant de subtypes.gradient.colors[] (+ angleDeg optionnel)
    val gradient: ManualGradientMeta? = null,
)

data class ManualTaxonomy(
    val types: List<ManualTypeMeta>,
    val subtypes: List<ManualSubtypeMeta>
) {
    private val typesByCode = types.associateBy { it.code }
    private val subtypesByCode = subtypes.associateBy { it.code }
    private val subtypesByParent = subtypes.groupBy { it.parentCode }

    // ✅ version “String codes” (celle que tu utilises déjà)
    fun typeMeta(typeCode: String): ManualTypeMeta? = typesByCode[typeCode]
    fun subtypeMeta(subtypeCode: String): ManualSubtypeMeta? = subtypesByCode[subtypeCode]
    fun subtypesOf(typeCode: String): List<ManualSubtypeMeta> = subtypesByParent[typeCode].orEmpty()
}

object ManualTaxonomyImageResolver {

    private const val TAG = "ManualTaxonomyImage"

    @Volatile
    private var typeToImageName: Map<String, String>? = null

    @Volatile
    private var subtypeToImageName: Map<String, String>? = null

    private val imageNameToResId = ConcurrentHashMap<String, Int>()

    fun resolveSubtypeDrawableResId(context: Context, subtypeCode: String): Int {
        val imageName = getSubtypeImageName(context, subtypeCode) ?: return 0
        return resolveImageNameToResId(context, imageName)
    }

    fun resolveTypeDrawableResId(context: Context, typeCode: String): Int {
        val imageName = getTypeImageName(context, typeCode) ?: return 0
        return resolveImageNameToResId(context, imageName)
    }

    private fun resolveImageNameToResId(context: Context, imageName: String): Int {
        val key = imageName.trim()
        if (key.isBlank()) return 0

        return imageNameToResId.getOrPut(key) {
            context.resources.getIdentifier(key, "drawable", context.packageName)
        }
    }

    private fun getSubtypeImageName(context: Context, subtypeCode: String): String? {
        val key = subtypeCode.trim()
        if (key.isEmpty()) return null
        ensureLoaded(context)
        return subtypeToImageName.orEmpty()[key]
    }

    private fun getTypeImageName(context: Context, typeCode: String): String? {
        val key = typeCode.trim()
        if (key.isEmpty()) return null
        ensureLoaded(context)
        return typeToImageName.orEmpty()[key]
    }

    private fun ensureLoaded(context: Context) {
        if (typeToImageName != null && subtypeToImageName != null) return

        synchronized(this) {
            if (typeToImageName != null && subtypeToImageName != null) return

            val pair: Pair<Map<String, String>, Map<String, String>> =
                runCatching {
                    val json = context.resources
                        .openRawResource(R.raw.manual_taxonomy)
                        .bufferedReader()
                        .use { it.readText() }

                    val root = JSONObject(json)
                    val typesArr = root.optJSONArray("types")
                    val subtypesArr = root.optJSONArray("subtypes")

                    val tm = HashMap<String, String>(typesArr?.length() ?: 0)
                    if (typesArr != null) {
                        for (i in 0 until typesArr.length()) {
                            val obj = typesArr.getJSONObject(i)
                            val code = obj.optString("code").trim()
                            val image = obj.optString("image").trim()
                            if (code.isNotEmpty() && image.isNotEmpty()) tm[code] = image
                        }
                    }

                    val sm = HashMap<String, String>(subtypesArr?.length() ?: 0)
                    if (subtypesArr != null) {
                        for (i in 0 until subtypesArr.length()) {
                            val obj = subtypesArr.getJSONObject(i)
                            val code = obj.optString("code").trim()
                            val image = obj.optString("image").trim()
                            if (code.isNotEmpty() && image.isNotEmpty()) sm[code] = image
                        }
                    }

                    tm as Map<String, String> to (sm as Map<String, String>)
                }.getOrElse { e ->
                    Log.e(TAG, "Impossible de lire R.raw.manual_taxonomy", e)
                    emptyMap<String, String>() to emptyMap<String, String>()
                }

            val (typesMap, subtypesMap) = pair

            typeToImageName = typesMap
            subtypeToImageName = subtypesMap
        }
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


internal object ManualTaxonomyUiSpec {
    // IMPORTANT: c’est ça qui faisait basculer ManualType en 2 colonnes (padding trop grand)
    val screenHPad: Dp = 10.dp

    val gridGap: Dp = 4.dp
    val gridBottomPad: Dp = 2.dp

    const val tileAspect: Float = 1.05f

    val minWidthFor3Cols: Dp = 340.dp

    fun colsFor(maxWidth: Dp): Int = if (maxWidth >= minWidthFor3Cols) 3 else 2
}