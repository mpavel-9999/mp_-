package ru.mpavel.osmmap.core.config

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import ru.mpavel.osmmap.core.net.ApiClient
import ru.mpavel.osmmap.core.net.ApiJson
import ru.mpavel.osmmap.core.net.decodeBody
import ru.mpavel.osmmap.data.ConfigUpdate
import ru.mpavel.osmmap.data.UiConfig
import ru.mpavel.osmmap.data.UiOptions
import java.io.File

/**
 * Конфигурация интерфейса (архитектура 6.5): шаблон, палитра, тексты, пиктограммы, курсоры, подложка.
 * Кэш - файл ui_config.json с контрольной суммой. При запуске GET /config?checksum=<кэш>:
 * 304 - берётся кэш, 200 - новая конфигурация сохраняется. PUT /config применяется сразу.
 */
class ConfigRepository(context: Context, private val api: ApiClient) {

    private val file = File(context.filesDir, "ui_config.json")
    private val _config = MutableStateFlow<UiConfig?>(null)

    /** Текущая конфигурация; null - ещё не загружена. */
    val config: StateFlow<UiConfig?> = _config

    /** Язык интерфейса: экраны с данными сервера перезагружаются при его смене. */
    val lang: Flow<String?> = _config.map { it?.config?.lang }

    suspend fun load(): UiConfig {
        val cached = _config.value ?: readCache()
        val query = cached?.let { "?checksum=${it.checksum}" } ?: ""
        val result = api.call("GET", "/config$query", allowNotModified = true)
        if (result.status == 304 && cached != null) {
            Log.i(TAG, "config: 304 cache ${cached.checksum}")
            apply(cached)
            return cached
        }
        return save(result.body).also { Log.i(TAG, "config: 200 saved ${it.checksum}") }
    }

    /** Шаблон, схема или язык: ответ - новая конфигурация. */
    suspend fun update(update: ConfigUpdate): UiConfig =
        save(api.call("PUT", "/config", ApiJson.encodeToString(update)).body)

    suspend fun options(): UiOptions = api.get("/ui/options")

    private suspend fun readCache(): UiConfig? = withContext(Dispatchers.IO) {
        if (!file.exists()) {
            return@withContext null
        }
        runCatching { ApiJson.decodeFromString<UiConfig>(file.readText()) }.getOrNull()
    }

    private suspend fun save(text: String): UiConfig {
        val config = decodeBody<UiConfig>("/config", text)
        withContext(Dispatchers.IO) { file.writeText(text) }
        apply(config)
        return config
    }

    private fun apply(config: UiConfig) {
        api.lang = config.config.lang
        _config.value = config
    }

    private companion object {
        const val TAG = "MpConfig"
    }
}
