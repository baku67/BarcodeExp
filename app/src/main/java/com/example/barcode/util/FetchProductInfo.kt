package com.example.barcode.util

import android.util.Log
import com.example.barcode.model.ProductInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

// Fonction suspend pour récupérer nom et marque du produit
suspend fun fetchProductInfo(code: String): ProductInfo =
    withContext(Dispatchers.IO) {
        return@withContext try {
            val json = URL("https://world.openfoodfacts.org/api/v0/product/$code.json")
                .openConnection().getInputStream().bufferedReader().use { it.readText() }
            val obj = JSONObject(json).getJSONObject("product")
            val name = obj.optString("product_name", "Inconnu")
            val brand = obj.optString("brands", "Inconnue")
            val imgUrl = obj.optString("image_url", "")
            // Récupère le nutri-score, champ nutrition_grade_fr ou nutrition_grades
            val nutri = obj.optString("nutrition_grade_fr", "").ifEmpty {
                // fallback sur liste tags Nutriscore
                obj.optJSONArray("nutrition_grades_tags")?.optString(0)?.substringAfterLast('-') ?: ""
            }
            ProductInfo(name, brand, imgUrl, nutri)
        } catch (e: Exception) {
            Log.e("BARCODE", "Erreur API", e)
            ProductInfo("Erreur", "Erreur", "", "?")
        }
    }