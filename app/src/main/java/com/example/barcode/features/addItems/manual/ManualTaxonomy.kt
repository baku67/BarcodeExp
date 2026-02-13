package com.example.barcode.features.addItems.manual

import android.content.Context
import android.util.Log
import com.example.barcode.R
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

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
    private var subtypeToImageName: Map<String, String>? = null

    private val imageNameToResId = ConcurrentHashMap<String, Int>()

    fun resolveSubtypeDrawableResId(context: Context, subtypeCode: String): Int {
        val imageName = getSubtypeImageName(context, subtypeCode) ?: return 0
        if (imageName.isBlank()) return 0

        return imageNameToResId.getOrPut(imageName) {
            context.resources.getIdentifier(imageName, "drawable", context.packageName)
        }
    }

    private fun getSubtypeImageName(context: Context, subtypeCode: String): String? {
        val key = subtypeCode.trim()
        if (key.isEmpty()) return null

        val cached = subtypeToImageName
        if (cached != null) return cached[key]

        synchronized(this) {
            val again = subtypeToImageName
            if (again != null) return again[key]

            val map = runCatching {
                val json = context.resources
                    .openRawResource(R.raw.manual_taxonomy)
                    .bufferedReader()
                    .use { it.readText() }

                val root = JSONObject(json)
                val arr = root.getJSONArray("subtypes")

                HashMap<String, String>(arr.length()).apply {
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val code = obj.optString("code").trim()
                        val image = obj.optString("image").trim()
                        if (code.isNotEmpty() && image.isNotEmpty()) {
                            put(code, image)
                        }
                    }
                }
            }.getOrElse { e ->
                Log.e(TAG, "Impossible de lire R.raw.manual_taxonomy", e)
                emptyMap()
            }

            subtypeToImageName = map
            return map[key]
        }
    }
}
