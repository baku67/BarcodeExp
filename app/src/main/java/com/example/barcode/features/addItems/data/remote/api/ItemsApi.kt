package com.example.barcode.features.addItems.data.remote.api

import com.example.barcode.features.addItems.data.remote.dto.CreateItemRequestDto
import com.example.barcode.features.addItems.data.remote.dto.ItemDeletedDto
import com.example.barcode.features.addItems.data.remote.dto.ItemDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ItemsApi {

    @GET("api/items")
    suspend fun getItems(
        @Header("Authorization") authorization: String,
        @Query("updatedSince") updatedSince: String? = null
    ): Response<List<ItemDto>>

    @GET("api/items/deleted")
    suspend fun getDeletedItems(
        @Header("Authorization") authorization: String,
        @Query("since") since: String
    ): Response<List<ItemDeletedDto>>

    @POST("api/items")
    suspend fun createItem(
        @Header("Authorization") authorization: String,
        @Body body: CreateItemRequestDto
    ): Response<ItemDto>

    @DELETE("api/items/client/{clientId}")
    suspend fun deleteItemByClientId(
        @Header("Authorization") authorization: String,
        @Path("clientId") clientId: String
    ): Response<Unit>
}