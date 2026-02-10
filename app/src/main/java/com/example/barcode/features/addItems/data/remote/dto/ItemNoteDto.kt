package com.example.barcode.features.addItems.data.remote.dto

// Move to
// package com.example.barcode.features.fridge.data.remote.dto

data class ItemNoteDto(
    val clientId: String,
    val itemClientId: String,
    val body: String,
    val createdAt: String?,
    val updatedAt: String,
    val deletedAt: String?,
    val authorId: Int?
)

data class ItemNoteCreateDto(
    val clientId: String,
    val body: String
)

data class NotesSyncResponseDto(
    val since: String,
    val serverTime: String,
    val notes: List<ItemNoteDto>
)
