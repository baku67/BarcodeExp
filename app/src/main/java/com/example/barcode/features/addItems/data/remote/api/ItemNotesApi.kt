package com.example.barcode.features.addItems.data.remote.api

// move to
// package com.example.barcode.features.fridge.data.remote.api

import com.example.barcode.features.addItems.data.remote.dto.ItemNoteCreateDto
import com.example.barcode.features.addItems.data.remote.dto.ItemNoteDto
import com.example.barcode.features.addItems.data.remote.dto.NotesSyncResponseDto
import retrofit2.Response
import retrofit2.http.*

interface ItemNotesApi {

    @GET("/api/sync/notes")
    suspend fun getNotesDelta(
        @Header("Authorization") authorization: String,
        @Query("since") since: String
    ): Response<NotesSyncResponseDto>

    @POST("/api/items/{itemClientId}/notes")
    suspend fun createNote(
        @Header("Authorization") authorization: String,
        @Path("itemClientId") itemClientId: String,
        @Body body: ItemNoteCreateDto
    ): Response<ItemNoteDto>

    @DELETE("/api/notes/{clientId}")
    suspend fun deleteNote(
        @Header("Authorization") authorization: String,
        @Path("clientId") clientId: String
    ): Response<ItemNoteDto>
}
