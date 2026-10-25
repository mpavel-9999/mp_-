package ru.mpavel.osmmap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.mpavel.osmmap.core.net.decodeBody
import ru.mpavel.osmmap.data.IncomingRequest
import ru.mpavel.osmmap.data.Slot
import ru.mpavel.osmmap.ui.template.Tab
import ru.mpavel.osmmap.ui.template.tabsFor

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
