package com.example.barcode.features.addItems.data.remote.api

import com.example.barcode.features.addItems.data.remote.dto.ItemCreateDto
import com.example.barcode.features.addItems.data.remote.dto.ItemDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface ItemsApi {

    @GET("api/items")
    suspend fun getItems(
        @Header("Authorization") authorization: String
    ): Response<List<ItemDto>>

    @POST("api/items")
    suspend fun createItem(
        @Header("Authorization") authorization: String,
        @Body body: ItemCreateDto
    ): Response<Unit>

    // delete par clientId (UUID)
    @DELETE("api/items/client/{clientId}")
    suspend fun deleteItemByClientId(
        @Header("Authorization") authorization: String,
        @Path("clientId") clientId: String
    ): Response<Unit>
}
