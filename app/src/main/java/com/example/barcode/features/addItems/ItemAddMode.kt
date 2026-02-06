package com.example.barcode.features.addItems

enum class ItemAddMode(val value: String) {
    BARCODE_SCAN("barcode_scan"),
    MANUAL("manual");

    companion object {
        fun from(value: String?): ItemAddMode =
            entries.firstOrNull { it.value == value } ?: BARCODE_SCAN
    }
}