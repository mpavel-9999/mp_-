package ru.mpavel.osmmap.core.time

import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// Время API - ISO 8601 без зоны: 2026-10-24T18:00:00 (ТЗ 4), по Алматы независимо от пояса устройства (Р-4).

/** Пояс времени API. */
val MP_ZONE: ZoneId = ZoneId.of("Asia/Almaty")

/** "Сейчас" по времени API; [clock] - для тестов. */
fun nowMp(clock: Clock = Clock.systemUTC()): LocalDateTime = LocalDateTime.now(clock.withZone(MP_ZONE))

private val SHORT = DateTimeFormatter.ofPattern("dd.MM HH:mm")
private val TIME = DateTimeFormatter.ofPattern("HH:mm")

/** Время из ответа сервера; PostgreSQL может отдать доли секунды или пробел вместо T. */
fun parseServerTime(value: String?): LocalDateTime? {
    val text = value?.trim()?.replace(' ', 'T')?.take(19) ?: return null
    return runCatching { LocalDateTime.parse(text) }.getOrNull()
}

/** Время для запроса к серверу: до минут, без зоны. */
fun toServerTime(time: LocalDateTime): String =
    time.withSecond(0).withNano(0).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

/** "24.10 18:00" */
fun formatShort(time: LocalDateTime?): String = time?.format(SHORT) ?: ""

/** Период "24.10 18:00–19:00"; другой день окончания - полностью. */
fun formatPeriod(start: LocalDateTime?, end: LocalDateTime?): String {
    if (start == null) return ""
    if (end == null) return formatShort(start)
    val tail = if (end.toLocalDate() == start.toLocalDate()) end.format(TIME) else formatShort(end)
    return "${formatShort(start)}–$tail"
}

/** Период запроса по умолчанию: начало через 30 мин, округлённое вверх до 5 мин; длительность 1 ч. */
fun defaultPeriod(now: LocalDateTime): Pair<LocalDateTime, LocalDateTime> {
    val base = now.plusMinutes(30).withSecond(0).withNano(0)
    val extra = (5 - base.minute % 5) % 5
    val start = base.plusMinutes(extra.toLong())
    return start to start.plusHours(1)
}
