package ru.mpavel.osmmap.core.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/** JSON API: поля в snake_case (`deal_id`), лишние поля ответа пропускаются. */
@OptIn(ExperimentalSerializationApi::class)
val ApiJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    coerceInputValues = true
    namingStrategy = JsonNamingStrategy.SnakeCase
}

/** Ошибка, которую можно показать: ответ API с кодом или отсутствие связи. */
sealed class AppError(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Ответ API `{"error": "текст", "code": "КОД"}` (openapi.yaml, модель ошибок). */
class ApiError(val status: Int, val code: String, message: String) : AppError(message)

/** Сервер недоступен или соединение оборвалось. */
class NetworkError(val baseUrl: String, cause: Throwable) : AppError(cause.message ?: "network", cause)

/** Ответ не совпал с моделью (расхождение с openapi.yaml) - показывается как ошибка, а не падение. */
class DecodeError(val path: String, cause: Throwable) : AppError("$path: ${cause.message}", cause)

/** Разбор ответа [path]; несовпадение с моделью -> [DecodeError]. */
inline fun <reified T> decodeBody(path: String, text: String): T = try {
    ApiJson.decodeFromString<T>(text)
} catch (e: IllegalArgumentException) { // SerializationException - наследник IllegalArgumentException
    throw DecodeError(path, e)
}

/** Ответ без разбора тела: код и текст. */
class HttpResult(val status: Int, val body: String)

@Serializable
private class ErrorBody(val error: String = "", val code: String = "")

/**
 * HTTP-клиент API v0.7. Все вызовы - suspend на Dispatchers.IO, из UI-потока сеть не трогается.
 * Добавляет токен сессии и язык ответа; ответ 401 при открытой сессии сообщает в [unauthorized].
 */
class ApiClient(val baseUrl: String, private val session: SessionStore) {

    /** Язык ответа: RUS, ENG, KAZ, DEU (из конфигурации интерфейса). */
    @Volatile
    var lang: String? = null

    private val http = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val _unauthorized = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Сессия истекла или закрыта на сервере - нужно войти заново. */
    val unauthorized: SharedFlow<Unit> = _unauthorized

    /**
     * Запрос к `/api` + [path]. Успех - 2xx, а также 304 при [allowNotModified].
     * Иначе - [ApiError] с кодом из тела ответа; нет связи - [NetworkError].
     */
    suspend fun call(method: String, path: String, body: String? = null, allowNotModified: Boolean = false): HttpResult =
        withContext(Dispatchers.IO) {
            val builder = Request.Builder().url("$baseUrl/api$path")
            session.token?.let { builder.header("Authorization", "Bearer $it") }
            lang?.let { builder.header("Accept-Language", it) }
            val requestBody = when {
                body != null -> body.toRequestBody(JSON_TYPE)
                method == "POST" || method == "PUT" -> "{}".toRequestBody(JSON_TYPE)
                else -> null
            }
            builder.method(method, requestBody)
            val response = try {
                http.newCall(builder.build()).execute()
            } catch (e: IOException) {
                throw NetworkError(baseUrl, e)
            }
            response.use {
                val text = try {
                    it.body.string()
                } catch (e: IOException) {
                    throw NetworkError(baseUrl, e)
                }
                if (it.code == 304 && allowNotModified) {
                    return@withContext HttpResult(304, "")
                }
                if (!it.isSuccessful) {
                    if (it.code == 401 && session.token != null) {
                        _unauthorized.tryEmit(Unit)
                    }
                    throw parseError(it.code, text)
                }
                HttpResult(it.code, text)
            }
        }

    suspend inline fun <reified T> get(path: String): T = decodeBody(path, call("GET", path).body)

    suspend inline fun <reified B, reified T> post(path: String, body: B): T =
        decodeBody(path, call("POST", path, ApiJson.encodeToString(body)).body)

    suspend inline fun <reified B, reified T> put(path: String, body: B): T =
        decodeBody(path, call("PUT", path, ApiJson.encodeToString(body)).body)

    companion object {
        private val JSON_TYPE = "application/json; charset=utf-8".toMediaType()

        /** Тело ошибки API -> [ApiError]; не JSON - код HTTP_<статус>. */
        fun parseError(status: Int, text: String): ApiError {
            val parsed = runCatching { ApiJson.decodeFromString<ErrorBody>(text) }.getOrNull()
            return if (parsed != null && parsed.code.isNotEmpty()) {
                ApiError(status, parsed.code, parsed.error)
            } else {
                ApiError(status, "HTTP_$status", text.take(200))
            }
        }
    }
}
