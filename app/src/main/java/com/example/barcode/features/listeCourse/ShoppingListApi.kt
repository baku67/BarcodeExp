package com.example.barcode.features.listeCourse

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query


interface ShoppingListApi {

    @POST("api/shopping-items")
    suspend fun createOrUpdateItem(
        @Header("Authorization") authorization: String,
        @Body body: ShoppingItemDto
    ): Response<Unit>

    @DELETE("api/shopping-items/{clientId}")
    suspend fun deleteItemByClientId(
        @Header("Authorization") authorization: String,
        @Path("clientId") clientId: String
    ): Response<Unit>

   @GET("api/shopping-items")
    suspend fun getItems(
        @Header("Authorization") authorization: String,
        @Query("updatedSince") updatedSince: String
    ): Response<List<ShoppingItemDto>>

    @GET("api/shopping-items/deleted")
    suspend fun getDeletedItems(
        @Header("Authorization") authorization: String,
        @Query("since") since: String
    ): Response<List<ShoppingItemDeletedDto>>
}