package com.example.barcode.common.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp


val AppTypography = Typography(
    // Corp texts Inter
    bodyLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    // Titres Manrope
    titleLarge = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.ExtraBold, // Marche pas
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
)
