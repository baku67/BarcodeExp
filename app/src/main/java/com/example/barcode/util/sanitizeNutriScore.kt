package com.example.barcode.util

import java.util.Locale

enum class NutriScore { A, B, C, D, E }

fun parseNutriScore(raw: String?): NutriScore? {
    val v = raw?.trim()?.uppercase(Locale.ROOT) ?: return null
    return runCatching { NutriScore.valueOf(v) }.getOrNull()
}

fun sanitizeNutriScore(raw: String?): String? {
    val v = raw?.trim()?.uppercase(Locale.ROOT) ?: return null
    return if (v in setOf("A","B","C","D","E")) v else null
}