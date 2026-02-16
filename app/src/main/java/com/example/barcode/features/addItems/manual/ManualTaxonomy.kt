package com.example.barcode.features.addItems.manual

import android.content.Context
import android.util.Log
import com.example.barcode.R
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

val MANUAL_TYPES_WITH_SUBTYPE_IMAGE = setOf("VEGETABLES", "FRUITS", "MEAT", "FISH", "DAIRY")

data class ManualTypeMeta(
    val code: String,
    val title: String,
    val image: String? = null,
)

data class ManualSubtypeMeta(
    val code: String,
    val parentCode: String,
    val title: String,
    val image: String? = null,
    val storageDaysMin: Int? = null,
    val storageDaysMax: Int? = null,
    val goodToKnow: String? = null,
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
