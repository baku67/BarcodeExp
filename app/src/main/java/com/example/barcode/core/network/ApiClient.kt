package com.example.barcode.core.network

import android.util.Log
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    // TODO private const val BASE_URL = "https://ton-domaine.tld/" // doit finir par /
    private const val BASE_URL = "http://127.0.0.1:8080/" // doit finir par /

    private val okHttp = OkHttpClient.Builder()
        // Interceptor pour debug chaque envoie de requete (TODO remove en prod)
/*        .addInterceptor(Interceptor { chain ->
            val req = chain.request()
            Log.e("HTTP", "=> ${req.method} ${req.url}")
            Log.e("HTTP", "=> Authorization=${req.header("Authorization")?.take(30)}...")
            val res = chain.proceed(req)
            Log.e("HTTP", "<= ${res.code}")
            res
        })*/
        .build()

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun <T> createApi(service: Class<T>): T =
        retrofit.create(service)
}