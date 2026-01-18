package com.example.barcode.core.network

import android.util.Log
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.ResponseBody.Companion.toResponseBody
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

            // ✅ Lire le body de la requête (si présent)
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

            // ✅ Exécuter la requête
            val res = chain.proceed(req)

            // ✅ Lire le body de la réponse (ATTENTION: .string() consomme le flux)
            val responseBody = res.body
            val raw = try {
                responseBody?.string() ?: "<empty>"
            } catch (e: Exception) {
                "<unable to read response body: ${e.message}>"
            }

            Log.e("HTTP", "<= ${res.code} ${req.url}")
            Log.e("HTTP", "<= Content-Type=${res.header("Content-Type")}")
            Log.e("HTTP", "<= Body=$raw")

            // 🔥 IMPORTANT : reconstruire le body pour que Retrofit puisse encore le parser
            val contentType = responseBody?.contentType()
                ?: res.header("Content-Type")?.toMediaTypeOrNull()

            res.newBuilder()
                .body(raw.toResponseBody(contentType))
                .build()
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