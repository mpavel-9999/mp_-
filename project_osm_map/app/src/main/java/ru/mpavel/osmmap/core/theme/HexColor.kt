package ru.mpavel.osmmap.core.theme

/** Цвет палитры "#RRGGBB" или "#AARRGGBB" -> ARGB; иначе null. */
fun parseHexColor(value: String?): Long? {
    val hex = value?.trim()?.removePrefix("#") ?: return null
    return when (hex.length) {
        6 -> hex.toLongOrNull(16)?.or(0xFF000000L)
        8 -> hex.toLongOrNull(16)
        else -> null
    }
}
