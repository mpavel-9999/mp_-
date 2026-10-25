package ru.mpavel.osmmap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.mpavel.osmmap.core.geo.decodePolyline
import ru.mpavel.osmmap.core.i18n.Texts
import ru.mpavel.osmmap.core.net.ApiError
import ru.mpavel.osmmap.core.net.decodeBody
import ru.mpavel.osmmap.core.time.defaultPeriod
import ru.mpavel.osmmap.core.time.formatPeriod
import ru.mpavel.osmmap.core.time.nowMp
import ru.mpavel.osmmap.core.time.parseServerTime
import ru.mpavel.osmmap.core.time.toServerTime
import ru.mpavel.osmmap.data.Forecast
import ru.mpavel.osmmap.data.FormField
import ru.mpavel.osmmap.feature.request.RequestState
import ru.mpavel.osmmap.feature.request.buildDealCreate
import ru.mpavel.osmmap.feature.request.invalidFields
import ru.mpavel.osmmap.ui.common.apiErrorText
import ru.mpavel.osmmap.ui.common.errorKey
import ru.mpavel.osmmap.ui.map.LatLon
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.TimeZone
import kotlin.math.abs

/** Логика экранов клиента (Э4) без Android: маршрут, время периода, ошибки по коду, тело запроса. */
class ClientFlowTest {

    private fun sample(name: String): String =
        requireNotNull(javaClass.classLoader?.getResource("api/$name")) { "нет образца $name" }.readText()

    @Test
    fun polylineReferenceExample() {
        // Пример из описания формата Google: (38.5, -120.2), (40.7, -120.95), (43.252, -126.453)
        val pts = decodePolyline("_p~iF~ps|U_ulLnnqC_mqNvxq`@")
        assertEquals(3, pts.size)
        assertTrue(abs(pts[0].first - 38.5) < 1e-6 && abs(pts[0].second + 120.2) < 1e-6)
        assertTrue(abs(pts[2].first - 43.252) < 1e-6 && abs(pts[2].second + 126.453) < 1e-6)
        assertTrue(decodePolyline(null).isEmpty())
    }

    @Test
    fun polylineFromServerForecast() {
        // Ответ POST /forecast такси Эконом (1101), снят с работающего сервера: линия по дорогам Алматы
        val f = decodeBody<Forecast>("/forecast", sample("forecast_taxi.json"))
        val pts = decodePolyline(f.routePolyline)
        assertTrue("точек ${pts.size}", pts.size > 5)
        assertTrue(pts.all { it.first in 43.1..43.4 && it.second in 76.7..77.1 })
        assertTrue((f.routeLengthM ?: 0.0) > 3000)
        assertEquals("km", f.priceUnit)
    }

    @Test
    fun periodDefaultsAndIso() {
        val (start, end) = defaultPeriod(LocalDateTime.of(2026, 10, 25, 13, 41, 17))
        assertEquals(LocalDateTime.of(2026, 10, 25, 14, 15), start)
        assertEquals(start.plusHours(1), end)
        assertEquals(LocalDateTime.of(2026, 10, 25, 14, 15), defaultPeriod(LocalDateTime.of(2026, 10, 25, 13, 45)).first)
        assertEquals("2026-10-25T14:15:00", toServerTime(start))
        assertEquals(LocalDateTime.of(2026, 10, 24, 18, 0), parseServerTime("2026-10-24T18:00:00.123456"))
        assertEquals(LocalDateTime.of(2026, 10, 24, 18, 0), parseServerTime("2026-10-24 18:00:00"))
        assertNull(parseServerTime("вчера"))
        assertEquals("24.10 18:00–19:30", formatPeriod(LocalDateTime.of(2026, 10, 24, 18, 0), LocalDateTime.of(2026, 10, 24, 19, 30)))
    }

    @Test
    fun nowIsAlmatyWhateverDeviceZone() {
        // Р-4: устройство в UTC (как эмулятор по умолчанию) - "сейчас" для API всё равно по Алматы (UTC+5)
        val saved = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
            val clock = Clock.fixed(Instant.parse("2026-10-25T20:00:00Z"), ZoneOffset.UTC)
            assertEquals(LocalDateTime.of(2026, 10, 26, 1, 0), nowMp(clock))
            assertEquals(LocalDateTime.of(2026, 10, 26, 1, 30), defaultPeriod(nowMp(clock)).first)
            val drift = Duration.between(LocalDateTime.now(ZoneId.of("Asia/Almaty")), nowMp()).abs()
            assertTrue("расхождение $drift", drift.seconds < 5)
        } finally {
            TimeZone.setDefault(saved)
        }
    }

    @Test
    fun errorTextByCode() {
        assertEquals("ui.err.review_required", errorKey("REVIEW_REQUIRED"))
        val texts = Texts(mapOf("ui.err.review_required" to "Сначала оцените прошлые сделки"))
        assertEquals("Сначала оцените прошлые сделки",
                     apiErrorText(texts, ApiError(409, "REVIEW_REQUIRED", "Сначала поставьте баллы по прошлым сделкам")))
        // термина нет - текст сервера
        assertEquals("Поставщик уже занят на это время", apiErrorText(texts, ApiError(409, "BUSY", "Поставщик уже занят на это время")))
    }

    @Test
    fun requiredAndNumberFields() {
        val fields = listOf(
            FormField(22, "number", "Количество пассажиров", required = true),
            FormField(17, "text", "Описание задачи", required = true),
            FormField(19, "number", "Количество грузчиков", required = false),
        )
        assertEquals(setOf(22L, 17L), invalidFields(fields, emptyMap()))
        assertEquals(setOf(22L), invalidFields(fields, mapOf(22L to "два", 17L to "Розетка")))
        assertEquals(emptySet<Long>(), invalidFields(fields, mapOf(22L to "2,5", 17L to "Розетка", 19L to "")))
    }

    @Test
    fun dealCreateForRouteAndOnline() {
        val start = LocalDateTime.of(2026, 10, 25, 18, 0)
        val route = RequestState(
            settings = mapOf("geo_mode" to "route", "place_mode" to "client", "price_unit" to "km"),
            pickup = LatLon(43.24, 76.89), dest = LatLon(43.256, 76.945),
            start = start, end = start.plusMinutes(30), myPrice = "900",
            fields = listOf(FormField(22, "number", "Пассажиры", required = true)), values = mapOf(22L to " 2,0 "),
        )
        val body = buildDealCreate(route, 1101)
        assertEquals(43.24, body.lat!!, 1e-9)
        assertEquals(43.256, body.toLat!!, 1e-9)
        assertEquals("2026-10-25T18:00:00", body.periodStart)
        assertEquals(900.0, body.clientPrice!!, 1e-9)
        assertEquals("2.0", body.attributes.single().value)

        val online = RequestState(settings = mapOf("geo_mode" to "none", "place_mode" to "online"), point = LatLon(1.0, 2.0), start = start, end = start.plusHours(1))
        val b2 = buildDealCreate(online, 1801)
        assertNull(b2.lat)
        assertNull(b2.zoneRadiusM)
        assertNull(b2.clientPrice)
    }
}
