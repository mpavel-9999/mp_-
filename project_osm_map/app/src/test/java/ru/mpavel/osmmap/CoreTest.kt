package ru.mpavel.osmmap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ru.mpavel.osmmap.core.i18n.Texts
import ru.mpavel.osmmap.core.icons.tintSvg
import ru.mpavel.osmmap.core.net.ApiClient
import ru.mpavel.osmmap.core.theme.parseHexColor

class CoreTest {

    @Test
    fun apiErrorFromBody() {
        val e = ApiClient.parseError(409, """{"error":"Отклик невозможен: PRICE_OUT_OF_RANGE","code":"PRICE_OUT_OF_RANGE"}""")
        assertEquals(409, e.status)
        assertEquals("PRICE_OUT_OF_RANGE", e.code)
        assertEquals("Отклик невозможен: PRICE_OUT_OF_RANGE", e.message)
    }

    @Test
    fun apiErrorNotJson() {
        val e = ApiClient.parseError(502, "<html>Bad gateway</html>")
        assertEquals("HTTP_502", e.code)
    }

    @Test
    fun hexColors() {
        assertEquals(0xFF0F766EL, parseHexColor("#0F766E"))
        assertEquals(0x800F766EL, parseHexColor("#800F766E"))
        assertNull(parseHexColor("teal"))
        assertNull(parseHexColor(null))
    }

    @Test
    fun textsWithArgsAndMissingKey() {
        val texts = Texts(mapOf("ui.offers_count" to "Откликов: {n}"), statuses = mapOf("OFFERS" to "Есть отклики"))
        assertEquals("Откликов: 3", texts.t("ui.offers_count", "n" to 3))
        assertEquals("ui.unknown", texts.t("ui.unknown"))
        assertEquals("Есть отклики", texts.status("OFFERS"))
        assertEquals("CLOSED", texts.status("CLOSED"))
    }

    @Test
    fun svgTint() {
        val svg = """<svg stroke="currentColor"><path fill="currentColor"/></svg>"""
        assertEquals("""<svg stroke="#0F766E"><path fill="#0F766E"/></svg>""", tintSvg(svg, "#0F766E"))
    }
}
