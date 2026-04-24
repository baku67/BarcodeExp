package com.example.barcode.core.network

import android.util.Log
import com.example.barcode.core.AuthStore
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.Route
import okio.Buffer
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.URLEncoder
import java.nio.charset.Charset
import kotlin.text.Charsets.UTF_8
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class ApiClient(
    baseUrl: String,
    private val authStore: AuthStore,
    enableHttpLogs: Boolean = false
) {
    private val baseUrlNormalized = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
    private val gson = Gson()

    // client "nu" utilisé uniquement pour refresh (évite boucle infinie)
    // TODO prendre celui d'en haut sans le "DebugHttpInterceptor"
    /*private val refreshClient: OkHttpClient = OkHttpClient.Builder().build()*/
    private val refreshClient: OkHttpClient =
        OkHttpClient.Builder()
            .apply { if (enableHttpLogs) addInterceptor(DebugHttpInterceptor()) }
            .build()

    private val okHttp: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(AuthorizationInterceptor(authStore))
        .authenticator(
            TokenAuthenticator(
                baseUrl = baseUrlNormalized,
                gson = gson,
                authStore = authStore,
                refreshClient = refreshClient
            )
        )
        .apply {
            if (enableHttpLogs) addInterceptor(DebugHttpInterceptor())
        }
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(baseUrlNormalized)
        .client(okHttp)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    fun <T> createApi(service: Class<T>): T = retrofit.create(service)
}

private class AuthorizationInterceptor(
    private val authStore: AuthStore
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request()
        val path = req.url.encodedPath

        if (isPublicAuthPath(path)) return chain.proceed(req)
        if (!req.header("Authorization").isNullOrBlank()) return chain.proceed(req)

        val token = runBlocking { authStore.token.first() }
        if (token.isNullOrBlank()) return chain.proceed(req)

        return chain.proceed(
            req.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        )
    }
}

private class TokenAuthenticator(
    private val baseUrl: String,
    private val gson: Gson,
    private val authStore: AuthStore,
    private val refreshClient: OkHttpClient
) : Authenticator {

    private val lock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        val req = response.request
        val path = req.url.encodedPath

        // anti-boucle
        if (isPublicAuthPath(path)) return null
        if (responseCount(response) >= 2) return null

        val currentToken = runBlocking { authStore.token.first() }.orEmpty()
        if (currentToken.isBlank()) return null

        val reqAuth = req.header("Authorization").orEmpty()

        // si un autre thread a déjà refresh -> rejoue avec le nouveau token
        if (reqAuth.isNotBlank() && reqAuth != "Bearer $currentToken") {
            return req.newBuilder().header("Authorization", "Bearer $currentToken").build()
        }

        synchronized(lock) {
            val tokenAfterLock = runBlocking { authStore.token.first() }.orEmpty()
            if (tokenAfterLock.isNotBlank() && reqAuth.isNotBlank() && reqAuth != "Bearer $tokenAfterLock") {
                return req.newBuilder().header("Authorization", "Bearer $tokenAfterLock").build()
            }

            val refreshToken = runBlocking { authStore.refreshToken.first() }
            if (refreshToken.isNullOrBlank()) {
                runBlocking { authStore.clearTokensOnly() }
                return null
            }

            val payload = performRefresh(refreshToken) ?: return null

            // persiste tokens (rotation possible)
            runBlocking {
                authStore.saveToken(payload.token)
                payload.refreshToken?.takeIf { it.isNotBlank() }?.let { authStore.saveRefreshToken(it) }
            }

            return req.newBuilder()
                .header("Authorization", "Bearer ${payload.token}")
                .build()
        }
    }

    private fun performRefresh(refreshToken: String): RefreshPayload? {
        val url = "${baseUrl}auth/token/refresh"

        val body = "refresh_token=" + URLEncoder.encode(refreshToken, UTF_8.name())
        val reqBody = body.toRequestBody("application/x-www-form-urlencoded".toMediaTypeOrNull())

        val req = Request.Builder().url(url).post(reqBody).build()

        val res = try { refreshClient.newCall(req).execute() } catch (_: Exception) { null } ?: return null

        if (!res.isSuccessful) {
            runBlocking { authStore.clearTokensOnly() }
            return null
        }

        val raw = res.body?.string().orEmpty()
        return try { gson.fromJson(raw, RefreshPayload::class.java) } catch (_: Exception) { null }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) { count++; prior = prior.priorResponse }
        return count
    }
}

private data class RefreshPayload(
    val token: String,
    @SerializedName("refresh_token") val refreshToken: String? = null
)

private fun isPublicAuthPath(path: String): Boolean =
    path.startsWith("/auth/login_check") ||
            path.startsWith("/auth/token/refresh") ||
            path.startsWith("/auth/register") ||
            path.startsWith("/auth/verify/email")

private class DebugHttpInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request()

        Log.e("HTTP", "=> ${req.method} ${req.url}")
        Log.e("HTTP", "=> Authorization=${req.header("Authorization")?.take(30)}...")

        val bodyStr = req.body?.let { body ->
            try {
                val buffer = Buffer()
                body.writeTo(buffer)
                val charset = body.contentType()?.charset(Charset.forName("UTF-8")) ?: UTF_8
                buffer.readString(charset)
            } catch (e: Exception) {
                "<unable to read body: ${e.message}>"
            }
        } ?: "<no body>"

        Log.e("HTTP", "=> Body=$bodyStr")

        val res = chain.proceed(req)

        val responseBody = res.body
        val raw = try { responseBody?.string() ?: "<empty>" } catch (e: Exception) { "<unable to read response body: ${e.message}>" }

        Log.e("HTTP", "<= ${res.code} ${req.url}")
        Log.e("HTTP", "<= Content-Type=${res.header("Content-Type")}")
        Log.e("HTTP", "<= Body=$raw")

        val contentType = responseBody?.contentType() ?: res.header("Content-Type")?.toMediaTypeOrNull()

        return res.newBuilder()
            .body(raw.toResponseBody(contentType))
            .build()
    }
}