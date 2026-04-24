package com.example.barcode.features.listeCourse

import android.content.Context
import android.util.Log
import com.example.barcode.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray

object ShoppingCatalogRepository {

    private const val TAG = "ShoppingCatalogRepository"

    @Volatile
    private var cached: List<ShoppingCatalogItem>? = null

    private val mutex = Mutex()

    fun peek(): List<ShoppingCatalogItem> = cached.orEmpty()

    suspend fun load(context: Context): List<ShoppingCatalogItem> = mutex.withLock {
        cached?.let { return it }

        val loaded = withContext(Dispatchers.IO) {
            loadFromRaw(context.applicationContext)
        }

        cached = loaded
        loaded
    }

    private fun loadFromRaw(context: Context): List<ShoppingCatalogItem> {
        return try {
            val raw = context.resources
                .openRawResource(R.raw.shopping_list_actual)
                .bufferedReader()
                .use { it.readText() }

            JSONArray(raw).toCatalogItems()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load shopping_list.json", e)
            emptyList()
        }
    }
}

private fun JSONArray.toCatalogItems(): List<ShoppingCatalogItem> = buildList {
    for (i in 0 until length()) {
        val o = optJSONObject(i) ?: continue

        val id = o.optString("id").trim()
        val label = o.optString("label").trim()
        val categoryId = o.optString("categoryId").trim()

        if (id.isBlank() || label.isBlank() || categoryId.isBlank()) continue

        add(
            ShoppingCatalogItem(
                id = id,
                label = label,
                image = o.optString("image").trim().ifBlank { null },
                categoryId = categoryId,
                searchText = o.optString("searchText").trim().ifBlank { label },
            )
        )
    }
}