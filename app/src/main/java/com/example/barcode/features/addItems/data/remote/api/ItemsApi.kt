package com.example.barcode.features.addItems.data.remote.api

import com.example.barcode.features.addItems.data.remote.dto.ItemCreateDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface ItemsApi {

    @POST("api/items")
    suspend fun createItem(
        @Header("Authorization") authorization: String,
        @Body body: ItemCreateDto
    ): Response<Unit>
}
