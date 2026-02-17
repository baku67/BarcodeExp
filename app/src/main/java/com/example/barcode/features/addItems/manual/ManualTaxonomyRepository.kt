package com.example.barcode.features.addItems.manual

import android.content.Context
import com.example.barcode.R
import org.json.JSONArray
import org.json.JSONObject

object ManualTaxonomyRepository {

    @Volatile private var cached: ManualTaxonomy? = null

    fun get(context: Context): ManualTaxonomy {
        cached?.let { return it }
        return synchronized(this) {
            cached ?: load(context).also { cached = it }
        }
    }

    private fun load(context: Context): ManualTaxonomy {
        return try {
            val json = context.resources
                // ✅ si ton fichier s'appelle manual_taxonomy.json
                .openRawResource(R.raw.manual_taxonomy)
                // ✅ si ton fichier s'appelle manual_taxonomie.json, remplace par :
                // .openRawResource(R.raw.manual_taxonomie)
                .bufferedReader()
                .use { it.readText() }

            val root = JSONObject(json)
            val types = root.getJSONArray("types").toTypeMetas()
            val subtypes = root.getJSONArray("subtypes").toSubtypeMetas()
            return ManualTaxonomy(types = types, subtypes = subtypes)
        } catch (e: Exception) {
            android.util.Log.e("ManualTaxonomy", "Failed to load taxonomy", e)
            ManualTaxonomy(types = emptyList(), subtypes = emptyList())
        }
    }

    private fun JSONArray.toTypeMetas(): List<ManualTypeMeta> = buildList {
        for (i in 0 until length()) {
            val o = optJSONObject(i) ?: continue
            val code = o.optString("code").takeIf { it.isNotBlank() } ?: continue
            val title = o.optString("title").takeIf { it.isNotBlank() } ?: continue

            add(
                ManualTypeMeta(
                    code = code,
                    title = title,
                    image = o.optString("image").takeIf { it.isNotBlank() }
                )
            )
        }
    }

    private fun JSONArray.toSubtypeMetas(): List<ManualSubtypeMeta> = buildList {
        for (i in 0 until length()) {
            val o = optJSONObject(i) ?: continue

            val code = o.optString("code").takeIf { it.isNotBlank() } ?: continue
            val parent = o.optString("parent").takeIf { it.isNotBlank() } ?: continue
            val title = o.optString("title").takeIf { it.isNotBlank() } ?: continue

            val gradient = o.optJSONObject("gradient")?.let { g ->
                val colors = g.optJSONArray("colors")
                    ?.toStringList()
                    .orEmpty()
                    .map { it.trim() }
                    .filter { it.isNotBlank() }

                if (colors.isEmpty()) null
                else ManualGradientMeta(
                    colors = colors,
                    angleDeg = g.optFloatOrNull("angleDeg")
                )
            }

            add(
                ManualSubtypeMeta(
                    code = code,
                    parentCode = parent,
                    title = title,
                    image = o.optString("image").takeIf { it.isNotBlank() },
                    storageDaysMin = o.optIntOrNull("storageDaysMin"),
                    storageDaysMax = o.optIntOrNull("storageDaysMax"),

                    gradient = gradient,

                    // ✅ sections dynamiques (acceptent String OU {type,...})
                    fridgeAdvise = o.optManualContent("fridgeAdvise"),
                    healthGood = o.optManualContent("health_good"),
                    healthWarning = o.optManualContent("health_warning"),
                    goodToKnow = o.optManualContent("goodToKnow"),
                )
            )
        }
    }
}


private fun JSONObject.optIntOrNull(key: String): Int? {
    if (!has(key) || isNull(key)) return null
    return optInt(key)
}

private fun JSONObject.optManualContent(key: String): ManualContent? {
    if (!has(key) || isNull(key)) return null

    return when (val v = opt(key)) {
        is JSONObject -> v.toManualContent()
        is String -> v.trim().takeIf { it.isNotBlank() }?.let { ManualContent.Markdown(it) }
        else -> null
    }
}

/**
 * Supporte:
 * - { "type":"bullets", "items":[ "a", "b" ] }
 * - { "type":"markdown", "text":"..." }  (ou "md")
 * - { "type":"text", "text":"..." }
 */
private fun JSONObject.toManualContent(): ManualContent? {
    val type = optString("type").trim().lowercase()
    return when (type) {
        "bullets", "bullet", "list" -> {
            val items = optJSONArray("items")?.toStringList().orEmpty()
                .map { it.trim() }
                .filter { it.isNotBlank() }
            if (items.isEmpty()) null else ManualContent.Bullets(items)
        }
        "markdown", "md", "text", "" -> {
            val text = optString("text").takeIf { it.isNotBlank() }
                ?: optString("md").takeIf { it.isNotBlank() }
                ?: ""
            text.trim().takeIf { it.isNotBlank() }?.let { ManualContent.Markdown(it) }
        }
        else -> null
    }
}

private fun JSONArray.toStringList(): List<String> = buildList {
    for (i in 0 until length()) {
        val s = optString(i).takeIf { it.isNotBlank() } ?: continue
        add(s)
    }
}

private fun JSONObject.optFloatOrNull(key: String): Float? {
    if (!has(key) || isNull(key)) return null
    return optDouble(key).toFloat()
}
