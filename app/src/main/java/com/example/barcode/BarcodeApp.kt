package com.example.barcode

import android.app.Application
import com.example.barcode.core.AuthStore
import com.example.barcode.core.network.ApiClient
import com.example.barcode.data.local.AppDb
import com.example.barcode.data.local.dao.ShoppingListDao
import com.example.barcode.features.auth.AuthApi
import com.example.barcode.features.auth.AuthRepository

class BarcodeApp : Application() {

    lateinit var authStore: AuthStore
        private set

    lateinit var apiClient: ApiClient
        private set

    lateinit var authApi: AuthApi
        private set

    lateinit var authRepository: AuthRepository
        private set

    lateinit var appDb: AppDb
        private set

    val shoppingListDao: ShoppingListDao
        get() = appDb.shoppingListDao()

    override fun onCreate() {
        super.onCreate()

        authStore = AuthStore(applicationContext)

        apiClient = ApiClient(
            baseUrl = BASE_URL,
            authStore = authStore,
            enableHttpLogs = true
        )

        authApi = apiClient.createApi(AuthApi::class.java)
        authRepository = AuthRepository(authApi)
        appDb = AppDb.get(applicationContext)
    }

    companion object {
        private const val BASE_URL = "http://127.0.0.1:8080/"
    }
}
