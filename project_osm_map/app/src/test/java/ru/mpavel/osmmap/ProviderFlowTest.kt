package ru.mpavel.osmmap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.mpavel.osmmap.core.net.decodeBody
import ru.mpavel.osmmap.data.IncomingRequest
import ru.mpavel.osmmap.data.MyService
import ru.mpavel.osmmap.data.Slot
import ru.mpavel.osmmap.feature.provider.SlotState
import ru.mpavel.osmmap.feature.provider.buildSlotCreate
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
}
