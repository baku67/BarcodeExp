package com.example.barcode.core.network

import android.util.Log
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okio.Buffer
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.nio.charset.Charset

object ApiClient {
    // TODO private const val BASE_URL = "https://ton-domaine.tld/" // doit finir par /
    private const val BASE_URL = "http://127.0.0.1:8080/" // doit finir par /

    private val okHttp = OkHttpClient.Builder()
        // Interceptor pour debug chaque envoie de requete (TODO remove en prod !!!!)
        .addInterceptor(Interceptor { chain ->
            val req = chain.request()

            Log.e("HTTP", "=> ${req.method} ${req.url}")
            Log.e("HTTP", "=> Authorization=${req.header("Authorization")?.take(30)}...")

            // ✅ Lire le body (si présent)
            val bodyStr = req.body?.let { body ->
                try {
                    val buffer = Buffer()
                    body.writeTo(buffer)

                    val charset = body.contentType()?.charset(Charset.forName("UTF-8")) ?: Charsets.UTF_8
                    buffer.readString(charset)
                } catch (e: Exception) {
                    "<unable to read body: ${e.message}>"
                }
            } ?: "<no body>"

            Log.e("HTTP", "=> Body=$bodyStr")

            val res = chain.proceed(req)
            Log.e("HTTP", "<= ${res.code}")
            res
        })
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