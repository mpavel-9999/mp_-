package ru.mpavel.osmmap.core.icons

/** SVG пиктограммы в цвет темы: stroke/fill="currentColor" -> "#RRGGBB". Курсоры не перекрашиваются. */
fun tintSvg(svg: String, hex: String): String = svg.replace("currentColor", hex)
