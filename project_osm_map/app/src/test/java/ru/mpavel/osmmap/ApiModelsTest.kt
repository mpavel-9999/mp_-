package ru.mpavel.osmmap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.mpavel.osmmap.core.net.decodeBody
import ru.mpavel.osmmap.data.Deal
import ru.mpavel.osmmap.data.DealListItem
import ru.mpavel.osmmap.data.DevAgent
import ru.mpavel.osmmap.data.MapSlot
import ru.mpavel.osmmap.data.Me
import ru.mpavel.osmmap.data.Notification
import ru.mpavel.osmmap.data.Offer
import ru.mpavel.osmmap.data.ServiceNode
import ru.mpavel.osmmap.data.UiConfig
import ru.mpavel.osmmap.data.UiOptions

/**
 * Модели совпадают с ответами API v0.7: образцы в src/test/resources/api сняты с работающего сервера
 * (агент 7, user_testing_1) 25.10.2026. Расхождение с сервером даёт DecodeError - тест падает.
 */
class ApiModelsTest {

    private fun sample(name: String): String =
        requireNotNull(javaClass.classLoader?.getResource("api/$name")) { "нет образца $name" }.readText()

    @Test
    fun config() {
        val c = decodeBody<UiConfig>("/config", sample("config.json"))
        assertEquals(32, c.checksum.length)
        assertTrue(c.template.code in setOf("A", "B", "C"))
        assertTrue(c.template.screens.containsKey("home"))
        assertTrue(c.scheme.colors.containsKey("onAccent"))
        assertTrue(c.texts.getValue("ui.retry").isNotBlank())
        assertTrue(c.icons.any { it.code == "parking" && it.svg.contains("currentColor") })
        assertEquals(setOf("CLIENT", "PROVIDER", "CLIENT_TEST", "PROVIDER_TEST"), c.cursors.keys)
        assertNotNull(c.baseLayer?.url)
        assertTrue(c.cancelReasons.isNotEmpty())
    }

    @Test
    fun profileAndLogin() {
        val me = decodeBody<Me>("/me", sample("me.json"))
        assertEquals(7L, me.agentId)
        assertTrue(me.isTesting)
        assertTrue(me.services.isNotEmpty())
        val agents = decodeBody<List<DevAgent>>("/dev/agents", sample("dev_agents.json"))
        assertTrue(agents.any { it.login == "user_testing_1" })
        val options = decodeBody<UiOptions>("/ui/options", sample("ui_options.json"))
        assertEquals(6, options.options.size)
        assertEquals(3, options.options.count { it.isDark })
        assertEquals(4, options.languages.size)
    }

    @Test
    fun servicesAndMap() {
        val services = decodeBody<List<ServiceNode>>("/services", sample("services.json"))
        assertEquals(5, services.count { it.nodeType == "category" })
        val slots = decodeBody<List<MapSlot>>("/map/slots", sample("map_slots.json"))
        assertTrue(slots.isNotEmpty())
    }

    @Test
    fun deals() {
        val list = decodeBody<List<DealListItem>>("/deals", sample("deals.json"))
        assertTrue(list.isNotEmpty())
        val deal = decodeBody<Deal>("/deals/{id}", sample("deal.json"))
        assertEquals(list.first().dealId, deal.dealId)
        assertFalse(deal.status.isBlank())
        val offers = decodeBody<List<Offer>>("/deals/{id}/offers", sample("offers.json"))
        assertTrue(offers.all { it.dealId == deal.dealId })
        val notifications = decodeBody<List<Notification>>("/notifications", sample("notifications.json"))
        assertTrue(notifications.isNotEmpty())
    }
}
