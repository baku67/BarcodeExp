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
    }

    private fun JSONArray.toTypeMetas(): List<ManualTypeMeta> =
        List(length()) { i ->
            val o = getJSONObject(i)
            ManualTypeMeta(
                code = o.getString("code"),
                title = o.getString("title"),
                subtitle = o.optString("subtitle").takeIf { it.isNotBlank() },
                description = o.optString("description").takeIf { it.isNotBlank() },
                iconKey = o.optString("icon").takeIf { it.isNotBlank() },
                tips = o.optJSONArray("tips")?.toStringList().orEmpty()
            )
        }

    private fun JSONArray.toSubtypeMetas(): List<ManualSubtypeMeta> =
        List(length()) { i ->
            val o = getJSONObject(i)
            ManualSubtypeMeta(
                code = o.getString("code"),
                parentCode = o.getString("parent"),
                title = o.getString("title"),
                subtitle = o.optString("subtitle").takeIf { it.isNotBlank() }
            )
        }

    private fun JSONArray.toStringList(): List<String> =
        List(length()) { i -> getString(i) }
}
