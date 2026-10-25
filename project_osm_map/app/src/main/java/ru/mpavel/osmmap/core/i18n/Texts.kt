package ru.mpavel.osmmap.core.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import ru.mpavel.osmmap.data.UiConfig

/**
 * Тексты интерфейса из глоссария (config.texts, ключи ui.*) на языке конфигурации.
 * Нет ключа - показывается сам ключ: сразу видно, какого термина не хватает в БД.
 * Подстановки - по имени: "Откликов: {n}" + ("n" to 3).
 */
@Immutable
class Texts(
    private val ui: Map<String, String>,
    private val statuses: Map<String, String> = emptyMap(),
    private val notify: Map<String, String> = emptyMap(),
) {
    fun t(key: String, vararg args: Pair<String, Any?>): String {
        var text = ui[key] ?: key
        for ((name, value) in args) {
            text = text.replace("{$name}", value?.toString() ?: "")
        }
        return text
    }

    /** Название статуса сделки (DEAL_STATUS). */
    fun status(code: String): String = statuses[code] ?: code

    /** Текст уведомления (NOTIFY_TYPE). */
    fun notification(code: String): String = notify[code] ?: code

    companion object {
        fun of(config: UiConfig?): Texts =
            if (config == null) Texts(emptyMap()) else Texts(config.texts, config.statuses, config.notifyTexts)
    }
}

val LocalTexts = staticCompositionLocalOf { Texts(emptyMap()) }

/** Текст по ключу глоссария: t("ui.find_providers"). */
@Composable
fun t(key: String, vararg args: Pair<String, Any?>): String = LocalTexts.current.t(key, *args)
