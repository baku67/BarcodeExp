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

// Fonction suspend pour récupérer nom et marque du produit
suspend fun fetchProductInfo(code: String): FetchResult =
    withContext(Dispatchers.IO) {
        val url = URL("https://world.openfoodfacts.org/api/v0/product/$code.json")
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
            val name = obj.optString("product_name", "Inconnu")
            val brand = obj.optString("brands", "Inconnue")
            val imgUrl = obj.optString("image_url", "")
            val nutri = obj.optString("nutrition_grade_fr", "").ifEmpty {
                obj.optJSONArray("nutrition_grades_tags")?.optString(0)?.substringAfterLast('-') ?: ""
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