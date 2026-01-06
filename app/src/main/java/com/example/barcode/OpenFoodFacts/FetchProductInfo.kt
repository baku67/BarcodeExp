package com.example.barcode.OpenFoodFacts

import android.util.Log
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
    val message: String? = null,    // message optionnel pour l'UI
    val nutri: String? = null,
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
            "brands",
            "image_url",          // fallback
            "images",             // ✅ le map qui nous intéresse
            "nutrition_grade_fr","nutrition_grades_tags"
        ).joinToString(",")

        val url = URL(
            "https://world.openfoodfacts.org/api/v2/product/$code.json" +
                    "?lc=$lang&fields=$fields"
        )

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
            val images = obj.optJSONObject("images")
            val langs = listOf(lang, "fr", "en").distinct()
            val frontUrl = images?.let { selectedTypeUrl(code, it, "front", langs) }

            val name = pickProductName(obj, lang)
            val brand = obj.optString("brands", "Inconnue")
            val nutri = obj.optString("nutrition_grade_fr", "").ifEmpty {
                obj.optJSONArray("nutrition_grades_tags")?.optString(0)?.substringAfterLast('-')
                    ?: ""
            }
            // image principale par défaut (thumbnail)
            val imgUrl = frontUrl ?: obj.optString("image_url", "")
            // // Images candidates aux choix parmis 4 images uploadés par des randoms (un peu claqué et peut correspondre aux images ingredients et nutrition...)
            val candidates = if (images != null) {
                buildFrontCandidates(code, images, langs, max = 4)
            } else {
                listOfNotNull(frontUrl).distinct()
            }
            val ingredientsUrl = images?.let { selectedTypeUrl(code, it, "ingredients", langs) }
            val nutritionUrl = images?.let { selectedTypeUrl(code, it, "nutrition", langs) }


            FetchResult(
                product = ProductInfo(
                    name = name,
                    brand = brand,
                    imageUrl = imgUrl,
                    nutriScore = nutri,
                    imageCandidates = candidates, // Images candidates aux choix parmis 4 images uploadés par des randoms (un peu claqué et peut correspondre aux images ingredients et nutrition...)
                    imageIngredientsUrl = ingredientsUrl,
                    imageNutritionUrl = nutritionUrl
                ),
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


// -----------------------------------   Helpers

private fun selectedTypeUrl(
    code: String,
    images: JSONObject,
    type: String,
    langs: List<String>
): String? {
    val key = pickSelectedKey(images, type, langs) ?: return null
    val rev = getRev(images, key) ?: return null
    return selectedImageUrl(code, key, rev, size = "400")
}

private fun rawImageUrl(code: String, imgId: String, size: String = "400"): String {
    val folder = productImagesFolder(code)
    // OFF: raw image = /<id>.jpg, resized = /<id>.400.jpg :contentReference[oaicite:2]{index=2}
    return "https://images.openfoodfacts.org/images/products/$folder/$imgId.$size.jpg"
}

// Images candidates aux choix parmis 4 images uploadés par des randoms (un peu claqué et peut correspondre aux images ingredients et nutrition...)
private fun buildFrontCandidates(
    code: String,
    images: JSONObject,
    langs: List<String>,
    max: Int = 4
): List<String> {
    val out = LinkedHashSet<String>()

    // 1) Front "sélectionnées" par langue (souvent 1 seule, mais on tente plusieurs langues)
    val preferredFrontKeys = buildList {
        langs.forEach { add("front_$it") }   // device lang puis fr/en
        add("front")                        // rare
    }.distinct()

    for (key in preferredFrontKeys) {
        val rev = getRev(images, key) ?: continue
        out.add(selectedImageUrl(code, key, rev, size = "400"))
        if (out.size >= max) return out.toList()
    }

    // 2) Complément avec images brutes numérotées (souvent d'autres faces/angles)
    val rawIds = mutableListOf<String>()
    val it = images.keys()
    while (it.hasNext()) {
        val k = it.next()
        if (k.all(Char::isDigit)) rawIds.add(k)
    }

    // Les plus récentes d'abord
    rawIds.sortByDescending { k -> images.optJSONObject(k)?.optLong("uploaded_t") ?: 0L }

    for (id in rawIds) {
        out.add(rawImageUrl(code, id, size = "400"))
        if (out.size >= max) break
    }

    return out.toList()
}

private fun padBarcode13(code: String): String =
    code.filter(Char::isDigit).padStart(13, '0')

private fun productImagesFolder(code: String): String {
    val c = padBarcode13(code)
    // 13 digits => 3+3+3+rest
    return "${c.substring(0,3)}/${c.substring(3,6)}/${c.substring(6,9)}/${c.substring(9)}"
}

private fun selectedImageUrl(code: String, imageKey: String, rev: String, size: String = "400"): String {
    val folder = productImagesFolder(code)
    return "https://images.openfoodfacts.org/images/products/$folder/$imageKey.$rev.$size.jpg"
}

private fun pickSelectedKey(images: JSONObject, type: String, langs: List<String>): String? {
    // On essaye type_lang (front_fr), puis fallback fr/en
    for (l in langs) {
        val k = "${type}_$l"
        if (images.has(k)) return k
    }
    // Certains produits peuvent avoir un key sans langue (rare)
    return type.takeIf { images.has(it) }
}

private fun getRev(images: JSONObject, key: String): String? =
    images.optJSONObject(key)?.opt("rev")?.toString()?.takeIf { it.isNotBlank() }
