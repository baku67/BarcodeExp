package com.example.barcode.data.mappers

import com.example.barcode.data.local.entities.ItemEntity
import com.example.barcode.features.addItems.data.remote.dto.ItemDto
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private fun String?.toEpochMillisFromIsoDate(): Long? =
    this?.let { LocalDate.parse(it).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() }

private fun String.toEpochMillisFromAtom(): Long =
    Instant.parse(this).toEpochMilli()

fun ItemDto.toEntity(): ItemEntity {
    val scanResolved = scan
    val barcodeResolved = scanResolved?.barcode ?: barcode

    val addModeResolved = addMode ?: "barcode_scan"

    return ItemEntity(
        id = clientId, // 🔥 IMPORTANT : ton PK local doit rester le clientId (sinon sync galère)

        photoId = photoId,

        name = name,
        expiryDate = expiryDate.toEpochMillisFromIsoDate(),
        addMode = addModeResolved,

        // scan (depuis scan.* si présent, sinon fallback flat)
        barcode = barcodeResolved,
        brand = scanResolved?.brand ?: brand,
        imageUrl = scanResolved?.imageUrl ?: imageUrl,
        imageIngredientsUrl = scanResolved?.imageIngredientsUrl ?: imageIngredientsUrl,
        imageNutritionUrl = scanResolved?.imageNutritionUrl ?: imageNutritionUrl,
        nutriScore = scanResolved?.nutriScore ?: nutriScore,

        // manual
        manualType = manual?.type,
        manualSubtype = manual?.subtype,
        manualMetaJson = manual?.meta?.let { JSONObject(it).toString() },

        // serveur updatedAt pour delta sync / merge
        serverUpdatedAt = updatedAt.toEpochMillisFromAtom(),

        // tombstone
        deletedAt = deletedAt?.let { Instant.parse(it).toEpochMilli() }
    )
}
