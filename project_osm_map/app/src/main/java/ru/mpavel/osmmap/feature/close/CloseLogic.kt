package ru.mpavel.osmmap.feature.close

// Закрытие сделки кодом (план Э6, ТЗ 6): что показать - решает сервер (Deal.actions), код считает только сервер.

/** Кнопка закрытия: действие сервера и термин глоссария; порядок перечисления - порядок вывода. */
enum class CloseButton(val action: String, val textKey: String) {
    CLOSE("close", "ui.close_deal"),
    SHOW_CODE("show_code", "ui.open_code"),
    ENTER_CODE("enter_code", "ui.enter_key"),
    TEST_SCAN("test_scan", "ui.scan_qr"),
    TEST_PROVIDE("test_provide", "ui.provide_qr"),
}

/** Кнопки закрытия, которые разрешил сервер. */
fun closeButtons(actions: List<String>): List<CloseButton> = CloseButton.entries.filter { it.action in actions }

/** Сколько секунд осталось жить коду: expires_in_s из ответа минус время с его получения, не меньше 0. */
fun codeSecondsLeft(expiresInS: Int, receivedAtMs: Long, nowMs: Long): Int =
    (expiresInS - ((nowMs - receivedAtMs) / 1000).toInt()).coerceAtLeast(0)

/** Номер сделки из строки QR "mpclose:<deal_id>:<code>"; null - строка не от кода закрытия. */
fun qrDealId(payload: String): Long? {
    val parts = payload.trim().split(':')
    if (parts.size != 3 || parts[0] != "mpclose" || parts[2].isEmpty() || !parts[2].all(Char::isDigit)) return null
    return parts[1].toLongOrNull()
}
