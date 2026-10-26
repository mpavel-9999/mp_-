package ru.mpavel.osmmap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.mpavel.osmmap.core.net.decodeBody
import ru.mpavel.osmmap.data.AttrValue
import ru.mpavel.osmmap.data.Deal
import ru.mpavel.osmmap.data.FormField
import ru.mpavel.osmmap.data.IncomingRequest
import ru.mpavel.osmmap.data.MyOffer
import ru.mpavel.osmmap.data.MyService
import ru.mpavel.osmmap.data.Offer
import ru.mpavel.osmmap.data.Slot
import ru.mpavel.osmmap.feature.deal.offererNoteKey
import ru.mpavel.osmmap.feature.provider.OfferState
import ru.mpavel.osmmap.feature.provider.SlotState
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import ru.mpavel.osmmap.feature.provider.buildOfferCreate
import ru.mpavel.osmmap.feature.provider.buildServiceUpdate
import ru.mpavel.osmmap.feature.provider.formOf
import ru.mpavel.osmmap.feature.provider.buildSlotCreate
import ru.mpavel.osmmap.feature.provider.visibleRequests
import ru.mpavel.osmmap.ui.map.LatLon
import ru.mpavel.osmmap.ui.template.Tab
import ru.mpavel.osmmap.ui.template.tabsFor
import java.time.LocalDateTime

/** Основа экранов поставщика (Э5): разделы по ролям, модели входящих запросов и публикаций. */
class ProviderFlowTest {

    private fun sample(name: String): String =
        requireNotNull(javaClass.classLoader?.getResource("api/$name")) { "нет образца $name" }.readText()

    @Test
    fun providerTabOnlyForProviderRoles() {
        assertTrue(Tab.PROVIDER in tabsFor(listOf("CLIENT", "PROVIDER", "ADMIN")))
        assertTrue(Tab.PROVIDER in tabsFor(listOf("PROVIDER", "SUBAGENT")))
        assertFalse(Tab.PROVIDER in tabsFor(listOf("CLIENT", "ADMIN")))
        assertFalse(Tab.PROVIDER in tabsFor(listOf("COMPANY")))
        assertFalse(Tab.PROVIDER in tabsFor(null))
        // остальные разделы - всем, порядок как в Tab
        assertEquals(listOf(Tab.HOME, Tab.DEALS, Tab.SETTINGS, Tab.PROFILE), tabsFor(listOf("CLIENT")))
        assertEquals(Tab.entries.toList(), tabsFor(listOf("PROVIDER")))
    }

    @Test
    fun incomingRequestWithMyOffer() {
        // GET /requests агента 2 (Петр_Парковка), снят с работающего сервера: сделка 6 кейса A/B/C/D с его автооткликом
        val reqs = decodeBody<List<IncomingRequest>>("/requests", sample("requests.json"))
        val r = reqs.single { it.dealId == 6L }
        assertEquals("provider", r.placeMode)
        assertEquals("point", r.geoMode)
        val mine = requireNotNull(r.myOffer) { "нет my_offer" }
        assertEquals("active", mine.status)
        assertEquals(1000.0, mine.price, 0.0)
        assertTrue((r.recommendedPrice ?: 0.0) > 0)
        // запрос без отклика: my_offer = null
        val fresh = decodeBody<List<IncomingRequest>>("/requests", """[{"deal_id": 9, "service_id": 1001, "my_offer": null}]""")
        assertNull(fresh.single().myOffer)
    }

    @Test
    fun slotBodyPointAndRoute() {
        val parking = MyService(serviceId = 100, name = "p", geoMode = "point", priceMin = 500.0)
        val start = LocalDateTime.of(2026, 10, 26, 18, 0)
        val point = SlotState(service = parking, pointA = LatLon(43.25, 76.92), start = start, end = start.plusMinutes(30), price = "600")
        val p = requireNotNull(buildSlotCreate(point))
        assertEquals(100L, p.serviceId)
        assertNull("у места радиус не передаётся", p.radiusM)
        assertNull(p.toLat)
        assertEquals("2026-10-26T18:00:00", p.periodStart)
        assertEquals(600.0, p.priceMin!!, 0.0)
        // без точки публиковать нечего
        assertNull(buildSlotCreate(point.copy(pointA = null)))

        val taxi = MyService(serviceId = 1101, name = "t", geoMode = "route", priceUnit = "km")
        val route = SlotState(service = taxi, pointA = LatLon(43.24, 76.90), pointB = LatLon(43.26, 76.95), radiusA = 300, radiusB = 1000,
                              start = start, end = start.plusHours(2), price = "700")
        val r = requireNotNull(buildSlotCreate(route))
        assertEquals(300.0, r.radiusM!!, 0.0)
        assertEquals(43.26, r.toLat!!, 1e-9)
        assertEquals(1000.0, r.radiusToM!!, 0.0)
        // без Б: только rA вокруг А (п. 2.1.1)
        val noB = requireNotNull(buildSlotCreate(route.copy(pointB = null)))
        assertNull(noB.toLat)
        assertNull(noB.radiusToM)
        assertEquals(300.0, noB.radiusM!!, 0.0)
    }

    @Test
    fun skippedHiddenUnlessOffered() {
        val reqs = listOf(
            IncomingRequest(dealId = 1, serviceId = 1001),
            IncomingRequest(dealId = 2, serviceId = 1001),
            IncomingRequest(dealId = 3, serviceId = 1001, myOffer = MyOffer(7, 900.0, "active")),
        )
        assertEquals(listOf(1L, 2L, 3L), visibleRequests(reqs, emptySet()).map { it.dealId })
        // пропущенный скрыт; с моим откликом - остаётся (видно "Отклик отправлен")
        assertEquals(listOf(1L, 3L), visibleRequests(reqs, setOf(2L, 3L)).map { it.dealId })
    }

    @Test
    fun offerBody() {
        val fields = listOf(
            FormField(attributeId = 30, type = "text", label = "a"),
            FormField(attributeId = 31, type = "car_number", label = "b"),
        )
        val body = requireNotNull(buildOfferCreate(OfferState(fields = fields, values = mapOf(30L to " 18:20 ", 31L to ""), price = "1100")))
        assertEquals(1100.0, body.price, 0.0)
        assertEquals(listOf(AttrValue(30, "18:20")), body.attributes)
        assertEquals(950.5, buildOfferCreate(OfferState(price = "950,5"))!!.price, 0.0)
        assertNull("цена не число", buildOfferCreate(OfferState(price = "")))
    }

    @Test
    fun mySlotsFromServer() {
        // GET /slots?mine=1 агента 2: строка provider_slot и название услуги
        val slots = decodeBody<List<Slot>>("/slots?mine=1", sample("slots_mine.json"))
        assertTrue(slots.isNotEmpty())
        val s = slots.first()
        assertEquals(2L, s.agentId)
        assertNotNull(s.serviceName)
        assertNotNull(s.periodStart)
        assertTrue(s.slotStatus in setOf("active", "closed", "cancelled"))
    }

    @Test
    fun offererNote() {
        fun deal(status: String, role: String) = Deal(dealId = 6, serviceId = 1001, status = status, myRole = role)
        val mine = listOf(Offer(dealOfferId = 1, dealId = 6, providerAgentId = 8, offerStatus = "active"))
        // клиенту, кандидату и исполнителю пометки нет
        assertNull(offererNoteKey(deal("OFFERS", "client"), mine))
        assertNull(offererNoteKey(deal("OFFERS", "candidate"), emptyList()))
        assertNull(offererNoteKey(deal("AGREED", "provider"), mine))
        assertEquals("ui.waiting_client", offererNoteKey(deal("OFFERS", "offerer"), mine))
        // отклик снят (withdrawn / expired) - в списке поставщика его нет
        assertEquals("ui.offer_withdrawn", offererNoteKey(deal("OFFERS", "offerer"), emptyList()))
        // согласовали с другим
        assertEquals("ui.offer_declined", offererNoteKey(deal("AGREED", "offerer"), emptyList()))
    }

    @Test
    fun serviceSettingsBody() {
        val s = MyService(serviceId = 1101, name = "e", priceUnit = "km", lat = 43.239, lon = 76.889, zoneRadiusM = 5000.0,
                          workTimeFrom = "08:00:00", priceMin = 700.0, priceMax = 5000.0, unitPriceMin = 150.0, isAuto = false)
        val form = formOf(s)
        assertEquals("08:00", form.timeFrom)
        assertEquals("700", form.priceMin)
        // без изменений - пустое тело
        assertTrue(buildServiceUpdate(s, form).body.isEmpty())
        // только изменённое: цена и автоотклик; очищенное время - null; новая точка - lat и lon
        val body = buildServiceUpdate(s, form.copy(priceMin = "800", isAuto = true, timeFrom = "", point = LatLon(43.25, 76.9))).body
        assertEquals(setOf("price_min", "is_auto", "work_time_from", "lat", "lon"), body.keys)
        assertEquals(JsonPrimitive(800.0), body["price_min"])
        assertEquals(JsonNull, body["work_time_from"])
        // проверки до отправки
        assertEquals(setOf("work_time_to"), buildServiceUpdate(s, form.copy(timeTo = "25:00")).invalid)
        assertEquals(setOf("client_rating_min"), buildServiceUpdate(s, form.copy(ratingMin = "6")).invalid)
        assertEquals("ui.err.price_min_max", buildServiceUpdate(s, form.copy(priceMin = "6000")).errorKey)
    }
}
