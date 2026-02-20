package com.example.barcode

import android.app.Application
import com.example.barcode.core.AuthStore
import com.example.barcode.core.network.ApiClient
import com.example.barcode.features.auth.AuthApi
import com.example.barcode.features.auth.AuthRepository
import com.google.android.datatransport.BuildConfig

class BarcodeApp : Application() {

    lateinit var authStore: AuthStore
        private set

    lateinit var apiClient: ApiClient
        private set

    lateinit var authApi: AuthApi
        private set

    lateinit var authRepository: AuthRepository
        private set

    override fun onCreate() {
        super.onCreate()

        authStore = AuthStore(applicationContext)

        apiClient = ApiClient(
            baseUrl = BASE_URL,
            authStore = authStore,
            enableHttpLogs = true // TODO PROD TEMPORAIRE (ApiClient:DebugHttpInterceptor pour voir les logs HTTP)
        )

        authApi = apiClient.createApi(AuthApi::class.java)
        authRepository = AuthRepository(authApi)
    }

    companion object {
        // ✅ mets ici la bonne URL (cf. section 0)
        // TODO private const val BASE_URL = "https://domaine.tld/" // doit finir par /
        private const val BASE_URL = "http://127.0.0.1:8080/"
    }
}