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
            val o = optJSONObject(i)
            if (o == null) {
                android.util.Log.e("ManualTaxonomy", "Subtype null at index=$i")
                continue
            }

            val code = o.optString("code").takeIf { it.isNotBlank() } ?: continue
            val parent = o.optString("parent").takeIf { it.isNotBlank() } ?: continue
            val title = o.optString("title").takeIf { it.isNotBlank() } ?: continue

            add(
                ManualSubtypeMeta(
                    code = code,
                    parentCode = parent,
                    title = title,
                    image = o.optString("image").takeIf { it.isNotBlank() },
                    goodToKnow = o.optString("goodToKnow").takeIf { it.isNotBlank() }
                )
            )
        }
    }
}
