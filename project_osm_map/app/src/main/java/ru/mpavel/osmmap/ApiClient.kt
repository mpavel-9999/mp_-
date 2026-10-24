package ru.mpavel.osmmap

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Клиент локального API над схемой mp_service (модуль server/).
 * Методы блокирующие: вызывать только из фонового потока.
 */
class ApiClient(private val baseUrl: String, private val lang: Int = 1) {

    class ApiException(val code: Int, message: String) : Exception(message)

    data class BaseLayer(val code: String, val url: String, val version: Long?, val attribution: String)

    data class Deal(
        val id: Long,
        val serviceName: String,
        val status: String,
        val lat: Double?,
        val lon: Double?,
        val radiusM: Double?,
        val priceUnit: String?,
    )

    data class Offer(
        val id: Long,
        val position: Int,
        val pseudonym: String,
        val rating: Double?,
        val price: Double,
        val lat: Double?,
        val lon: Double?,
        val distanceM: Double?,
        val isExact: Boolean,
        val label: String,
    )

    data class RouteLink(val offerId: Long, val providerName: String, val url: String)

    fun baseLayers(): List<BaseLayer> = array(request("GET", "/api/map/base-layers?lang=$lang")).map {
        BaseLayer(
            code = it.getString("code"),
            url = it.getString("url"),
            version = it.longOrNull("version"),
            attribution = it.getString("attribution"),
        )
    }

    fun deal(dealId: Long): Deal {
        val o = JSONObject(request("GET", "/api/deals/$dealId?lang=$lang"))
        return Deal(
            id = o.getLong("deal_id"),
            serviceName = o.getString("service_name"),
            status = o.getString("status"),
            lat = o.doubleOrNull("geo_lat"),
            lon = o.doubleOrNull("geo_lon"),
            radiusM = o.doubleOrNull("zone_radius_m"),
            priceUnit = o.stringOrNull("price_unit"),
        )
    }

    fun offers(dealId: Long): List<Offer> = parseOffers(request("GET", "/api/deals/$dealId/offers"))

    fun routes(dealId: Long): List<RouteLink> =
        array(request("GET", "/api/deals/$dealId/routes?lang=$lang")).map {
            RouteLink(
                offerId = it.getLong("deal_offer_id"),
                providerName = it.getString("provider_name"),
                url = it.getString("route_url"),
            )
        }

    /** Клиент согласует отклик; сервер возвращает обновленный список откликов. */
    fun agree(dealId: Long, offerId: Long): List<Offer> =
        parseOffers(request("POST", "/api/deals/$dealId/offers/$offerId/agree"))

    private fun parseOffers(body: String): List<Offer> = array(body).map {
        Offer(
            id = it.getLong("deal_offer_id"),
            position = it.getInt("position"),
            pseudonym = it.optString("pseudonym"),
            rating = it.doubleOrNull("rating"),
            price = it.getDouble("offer_price"),
            lat = it.doubleOrNull("show_lat"),
            lon = it.doubleOrNull("show_lon"),
            distanceM = it.doubleOrNull("distance_m"),
            isExact = it.getBoolean("is_exact"),
            label = it.getString("marker_label"),
        )
    }

    private fun request(method: String, path: String): String {
        val conn = URL(baseUrl + path).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = method
            conn.connectTimeout = 5_000
            conn.readTimeout = 10_000
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                val message = runCatching { JSONObject(body).getString("error") }.getOrDefault(body)
                throw ApiException(code, message)
            }
            return body
        } finally {
            conn.disconnect()
        }
    }

    private fun array(body: String): List<JSONObject> {
        val a = JSONArray(body)
        return List(a.length()) { a.getJSONObject(it) }
    }

    private fun JSONObject.doubleOrNull(name: String): Double? = if (isNull(name)) null else getDouble(name)

    private fun JSONObject.longOrNull(name: String): Long? = if (isNull(name)) null else getLong(name)

    private fun JSONObject.stringOrNull(name: String): String? = if (isNull(name)) null else getString(name)
}
