package com.sonari.speak2easy.data.remote

import com.sonari.speak2easy.data.auth.TokenStore
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Interceptor
import okhttp3.Response
import retrofit2.HttpException
import java.io.IOException

/**
 * Shared JSON config. SnakeCase naming mirrors the iOS `convertFromSnakeCase`
 * decoder; nulls are omitted on encode (so optional request fields are absent,
 * like iOS) and coerced/ignored on decode for resilience to backend drift.
 */
@OptIn(ExperimentalSerializationApi::class)
val SonariJson: Json = Json {
    namingStrategy = JsonNamingStrategy.SnakeCase
    ignoreUnknownKeys = true
    explicitNulls = false
    coerceInputValues = true
    isLenient = true
}

/** Backend returned a non-2xx status. [message] is the decoded `message`/`error` field. */
class ApiException(val statusCode: Int, override val message: String) : Exception(message)

/** Transport failure (no/failed connection). */
class NetworkException(cause: Throwable) : Exception(cause.message, cause)

fun parseHttpException(json: Json, e: HttpException): ApiException {
    val body = try {
        e.response()?.errorBody()?.string()
    } catch (_: Exception) {
        null
    }
    val message = body?.let { raw ->
        try {
            val obj = json.parseToJsonElement(raw).jsonObject
            (obj["message"] ?: obj["error"])?.jsonPrimitive?.content
        } catch (_: Exception) {
            null
        }
    } ?: "Request failed (status ${e.code()})"
    return ApiException(e.code(), message)
}

/** Runs a Retrofit suspend call, translating transport failures into typed exceptions. */
suspend fun <T> apiCall(json: Json, block: suspend () -> T): T = try {
    block()
} catch (e: HttpException) {
    throw parseHttpException(json, e)
} catch (e: IOException) {
    throw NetworkException(e)
}

/** Adds the bearer token (when present) to every outgoing request. */
class AuthInterceptor(private val tokenStore: TokenStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val token = tokenStore.getToken()
        return if (token != null && request.header("Authorization") == null) {
            chain.proceed(request.newBuilder().header("Authorization", "Bearer $token").build())
        } else {
            chain.proceed(request)
        }
    }
}
