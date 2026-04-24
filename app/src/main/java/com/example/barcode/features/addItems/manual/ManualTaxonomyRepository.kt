package com.example.barcode.features.addItems.manual

import android.content.Context
import android.util.Log
import com.example.barcode.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

private const val MANUAL_TAXONOMY_TAG = "ManualTaxonomy"

object ManualTaxonomyRepository {

    private const val TAG = MANUAL_TAXONOMY_TAG

    private val _taxonomyState = MutableStateFlow<ManualTaxonomy?>(null)
    val taxonomyState: StateFlow<ManualTaxonomy?> = _taxonomyState.asStateFlow()

    @Volatile
    private var cached: ManualTaxonomy? = null

    private val mutex = Mutex()


    // ✅ scope interne (évite GlobalScope)
    private val scope = kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO
    )

    /** ✅ Accès immédiat si déjà chargé */
    fun peek(): ManualTaxonomy? = cached

    /**
     * ✅ Précharge en background (idempotent)
     * Utilisable depuis MainActivity (LaunchedEffect ou pas).
     */
    fun preload(context: android.content.Context) {
        if (cached != null) return
        val appCtx = context.applicationContext
        scope.launch {
            runCatching { load(appCtx) }
        }
    }

    suspend fun load(context: Context): ManualTaxonomy = mutex.withLock {
        cached?.let { return it }

        val taxonomy = withContext(Dispatchers.IO) {
            loadFromRaw(context)
        }

        cached = taxonomy
        _taxonomyState.value = taxonomy
        taxonomy
    }

    private fun loadFromRaw(context: Context): ManualTaxonomy {
        return try {
            val raw = context.resources.openRawResource(R.raw.manual_taxonomy)
                .bufferedReader()
                .use { it.readText() }

            val root = JSONObject(raw)

            // ✅ snippets = { "k":"...", ... } (optionnel)
            val snippets = root.optJSONObject("snippets")?.toStringMap().orEmpty()

            val types = root.getJSONArray("types").toTypeMetas()
            val subtypes = root.getJSONArray("subtypes").toSubtypeMetas(snippets)

            ManualTaxonomy(types = types, subtypes = subtypes)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load taxonomy", e)
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

    private fun JSONArray.toSubtypeMetas(snippets: Map<String, String>): List<ManualSubtypeMeta> = buildList {
        for (i in 0 until length()) {
            val o = optJSONObject(i) ?: continue

            val code = o.optString("code").takeIf { it.isNotBlank() } ?: continue
            val parent = o.optString("parent").takeIf { it.isNotBlank() } ?: continue
            val title = o.optString("title").takeIf { it.isNotBlank() } ?: continue

            val seasons = o.optJSONObject("seasons")?.toMonthsByRegion()

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

                    seasons = seasons,

                    gradient = gradient,

                    fridgeAdvise = o.optManualContent("fridgeAdvise", snippets),
                    healthGood = o.optManualContent("health_good", snippets),
                    healthWarning = o.optManualContent("health_warning", snippets),
                    goodToKnow = o.optManualContent("goodToKnow", snippets),
                )
            )
        }
    }
}

private fun JSONObject.optIntOrNull(key: String): Int? {
    if (!has(key) || isNull(key)) return null
    return optInt(key)
}

private fun JSONObject.optManualContent(key: String, snippets: Map<String, String>): ManualContent? {
    if (!has(key) || isNull(key)) return null

    return when (val v = opt(key)) {
        is JSONObject -> v.toManualContent(snippets)
        is String -> v.trim().takeIf { it.isNotBlank() }?.let { ManualContent.Markdown(it) }
        else -> null
    }
}

/**
 * Supporte:
 * - { "type":"bullets", "items":[ "a", "b" ], "refs":["k1","k2"] }
 * - { "type":"markdown", "text":"...", "refs":["k1"] }  (ou "md")
 * - { "type":"text", "text":"..." }
 */
private fun JSONObject.toManualContent(snippets: Map<String, String>): ManualContent? {
    val type = optString("type").trim().lowercase()

    val refs = optJSONArray("refs")
        ?.toStringList()
        .orEmpty()
        .map { it.trim() }
        .filter { it.isNotBlank() }

    fun resolveRefs(): List<String> {
        if (refs.isEmpty()) return emptyList()
        return refs.mapNotNull { key ->
            snippets[key]?.trim()?.takeIf { it.isNotBlank() }.also { resolved ->
                if (resolved == null) Log.w(MANUAL_TAXONOMY_TAG, "Snippet manquant: $key")
            }
        }
    }

    return when (type) {
        "bullets", "bullet", "list" -> {
            val rawItems = optJSONArray("items")
                ?.toStringList()
                .orEmpty()

            val items = expandInlineRefsOrdered(rawItems, snippets)
                .map { it.trim() }
                .filter { it.isNotBlank() }

            val hasInlineRefs = rawItems.any { it.trim().startsWith("@ref:", ignoreCase = true) }
            val resolved = if (hasInlineRefs) emptyList() else resolveRefs()
            val merged = dedupeKeepOrder(resolved + items)

            if (merged.isEmpty()) null else ManualContent.Bullets(merged)
        }

        "markdown", "md", "text", "" -> {
            val base = optString("text").takeIf { it.isNotBlank() }
                ?: optString("md").takeIf { it.isNotBlank() }
                ?: ""

            val resolved = resolveRefs()
            val merged = buildString {
                if (resolved.isNotEmpty()) append(resolved.joinToString("\n\n"))
                if (base.isNotBlank()) {
                    if (isNotEmpty()) append("\n\n")
                    append(base.trim())
                }
            }

            merged.trim().takeIf { it.isNotBlank() }?.let { ManualContent.Markdown(it) }
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

private fun JSONArray.toIntList(): List<Int> = buildList {
    for (i in 0 until length()) {
        val v = when (val raw = opt(i)) {
            is Number -> raw.toInt()
            is String -> raw.trim().toIntOrNull()
            else -> null
        } ?: continue

        if (v in 1..12) add(v)
    }
}

/** { "EU_TEMPERATE":[1,2,3], "EU_SOUTH":[...]} */
private fun JSONObject.toMonthsByRegion(): Map<String, List<Int>> {
    val out = LinkedHashMap<String, List<Int>>()
    val ks = keys()
    while (ks.hasNext()) {
        val k = ks.next()
        val arr = optJSONArray(k) ?: continue
        val months = arr.toIntList().distinct().sorted()
        if (k.isNotBlank() && months.isNotEmpty()) out[k.trim()] = months
    }
    return out
}

private fun JSONObject.optFloatOrNull(key: String): Float? {
    if (!has(key) || isNull(key)) return null
    return optDouble(key).toFloat()
}

private fun JSONObject.toStringMap(): Map<String, String> {
    val out = LinkedHashMap<String, String>()
    val keys = keys()
    while (keys.hasNext()) {
        val k = keys.next()
        val v = optString(k).trim()
        if (k.isNotBlank() && v.isNotBlank()) out[k.trim()] = v
    }
    return out
}

private fun expandInlineRefsOrdered(
    rawItems: List<String>,
    snippets: Map<String, String>
): List<String> {
    if (rawItems.isEmpty()) return emptyList()

    val out = ArrayList<String>(rawItems.size)

    for (raw in rawItems) {
        val t = raw.trim()
        if (t.startsWith("@ref:", ignoreCase = true)) {
            val key = t.removePrefix("@ref:").trim()
            val resolved = snippets[key]?.trim()
            if (!resolved.isNullOrBlank()) {
                out += resolved
            } else {
                Log.w(MANUAL_TAXONOMY_TAG, "Snippet manquant (inline): $key")
            }
        } else {
            if (t.isNotBlank()) out += t
        }
    }

    return out
}

private fun dedupeKeepOrder(list: List<String>): List<String> {
    if (list.isEmpty()) return emptyList()
    val seen = LinkedHashSet<String>(list.size)
    val out = ArrayList<String>(list.size)
    for (s in list) {
        val t = s.trim()
        if (t.isNotBlank() && seen.add(t)) out += t
    }
    return out
}
