package com.example.barcode.util

import android.util.Log
import com.example.barcode.model.ProductInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.zip.GZIPInputStream

data class FetchResult(
    val product: ProductInfo?,     // null si erreur
    val rateLimited: Boolean,      // true si HTTP 429
    val message: String? = null    // message optionnel pour l'UI
)


private fun firstNonBlank(obj: JSONObject, keys: List<String>): String? =
    keys.firstNotNullOfOrNull { k ->
        obj.optString(k).takeIf { it.isNotBlank() }
    }

private fun pickProductName(obj: JSONObject, lang: String): String {
    val candidates = buildList {
        add("product_name")                           // déjà localisé si lc=...
        add("product_name_${lang}")                  // langue du device
        addAll(listOf("product_name_fr","product_name_en","product_name_es"))
        add("generic_name_${lang}")
        add("generic_name")
    }

    return firstNonBlank(obj, candidates)
        ?: obj.optString("brands").takeIf { it.isNotBlank() }
        ?: "Produit sans nom"
}


// Fonction suspend pour récupérer nom et marque du produit
suspend fun fetchProductInfo(code: String): FetchResult =
    withContext(Dispatchers.IO) {

        val lang = Locale.getDefault().language.lowercase(Locale.ROOT)

        // Champs strictement nécessaires (→ + rapide)
        val fields = listOf(
            "product_name",
            "product_name_$lang",
            "product_name_fr","product_name_en","product_name_es",
            "generic_name","generic_name_$lang",
            "brands","image_url","languages_tags","lang",
            "nutrition_grade_fr","nutrition_grades_tags"
        ).joinToString(",")

        val url = URL("https://world.openfoodfacts.org/api/v2/product/$code.json?lc=fr&fields=product_name,product_name_fr,product_name_en,product_name_es,generic_name,generic_name_fr,brands,image_url,languages_tags,lang")
        var conn: HttpURLConnection? = null
        try {
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("User-Agent", "FrigoZen/0.1 (Android; contact: basile08@hotmail.fr)")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Accept-Language", Locale.getDefault().toLanguageTag())
                setRequestProperty("Accept-Encoding", "gzip")
            }

            val codeHttp = conn.responseCode
            val baseStream = if (codeHttp in 200..299) conn.inputStream else conn.errorStream
            val stream = if ((conn.contentEncoding ?: "").equals("gzip", ignoreCase = true))
                GZIPInputStream(baseStream) else baseStream

            val json = stream.bufferedReader().use { it.readText() }

            if (codeHttp == 429) {
                Log.w("BARCODE", "Rate limit OFF (429): $json")
                return@withContext FetchResult(
                    product = null,
                    rateLimited = true,
                    message = "Trop de requêtes, réessayez dans un instant."
                )
            }

            if (codeHttp !in 200..299) {
                Log.e("BARCODE", "OFF HTTP $codeHttp: $json")
                return@withContext FetchResult(
                    product = null,
                    rateLimited = false,
                    message = "Erreur serveur ($codeHttp)"
                )
            }

            val obj = JSONObject(json).getJSONObject("product")

            // ✅ Fallbacks robustes
            val name = pickProductName(obj, lang)
            val brand = obj.optString("brands", "Inconnue")
            val imgUrl = obj.optString("image_url", "")
            val nutri = obj.optString("nutrition_grade_fr", "").ifEmpty {
                obj.optJSONArray("nutrition_grades_tags")?.optString(0)?.substringAfterLast('-')
                    ?: ""
            }

            FetchResult(
                product = ProductInfo(name, brand, imgUrl, nutri),
                rateLimited = false,
                message = null
            )
        } catch (e: Exception) {
            Log.e("BARCODE", "Erreur API", e)
            FetchResult(
                product = null,
                rateLimited = false,
                message = "Erreur réseau"
            )
        } finally {
            conn?.disconnect()
        }
    }