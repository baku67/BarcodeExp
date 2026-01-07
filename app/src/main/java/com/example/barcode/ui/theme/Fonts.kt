package com.example.barcode.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.barcode.R

val Inter = FontFamily(
    Font(R.font.inter_variable, FontWeight.Normal),   // 400
    Font(R.font.inter_variable, FontWeight.Medium),   // 500
    Font(R.font.inter_variable, FontWeight.SemiBold), // 600
    Font(R.font.inter_variable, FontWeight.Bold),     // 700 (optionnel)
)

val Manrope = FontFamily(
    Font(R.font.manrope_variable, FontWeight.Medium),   // 500 (optionnel)
    Font(R.font.manrope_variable, FontWeight.SemiBold), // 600
    Font(R.font.manrope_variable, FontWeight.Bold),     // 700
    Font(R.font.manrope_variable, FontWeight.ExtraBold),
)