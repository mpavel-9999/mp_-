package ru.mpavel.osmmap.core.geo

/**
 * Декодер encoded polyline (точность 1e-5) - формат линии маршрута сервера (Routing.encode),
 * тот же, что у Google и OSRM. Возвращает пары (широта, долгота).
 */
fun decodePolyline(encoded: String?): List<Pair<Double, Double>> {
    if (encoded.isNullOrEmpty()) return emptyList()
    val points = ArrayList<Pair<Double, Double>>()
    var index = 0
    var lat = 0L
    var lon = 0L
    fun next(): Long {
        var result = 0L
        var shift = 0
        var b: Int
        do {
            b = encoded[index++].code - 63
            result = result or ((b and 0x1f).toLong() shl shift)
            shift += 5
        } while (b >= 0x20 && index < encoded.length)
        return if (result and 1L != 0L) (result shr 1).inv() else result shr 1
    }
    while (index < encoded.length) {
        lat += next()
        if (index >= encoded.length) break
        lon += next()
        points.add(lat / 1e5 to lon / 1e5)
    }
    return points
}
