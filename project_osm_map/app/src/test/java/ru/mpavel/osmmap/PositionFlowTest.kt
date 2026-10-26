package ru.mpavel.osmmap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.mpavel.osmmap.core.geo.distanceM
import ru.mpavel.osmmap.data.Deal
import ru.mpavel.osmmap.data.Offer
import ru.mpavel.osmmap.data.Party
import ru.mpavel.osmmap.data.Position
import ru.mpavel.osmmap.feature.deal.DealState
import ru.mpavel.osmmap.feature.deal.ownStartPoint
import ru.mpavel.osmmap.ui.map.LatLon

/** Положение на экране сделки (план Э6, шаг 4): кто отправляет, где свой курсор, расстояние до другой стороны. */
class PositionFlowTest {

    private fun deal(status: String = "AGREED", role: String = "client", testing: Boolean = true, placeMode: String = "provider") =
        Deal(dealId = 8, serviceId = 1001, status = status, myRole = role, geoMode = "point", placeMode = placeMode,
             lat = 43.238, lon = 76.889, nearRadiusM = "300",
             client = Party(isTesting = testing), provider = Party(isTesting = !testing))

    private fun pos(role: String, lat: Double) =
        Position(agentId = if (role == "client") 7 else 8, role = role, lat = lat, lon = 76.889, source = "manual",
                 cursor = role.uppercase() + "_TEST")

    @Test
    fun distanceLikeDatabase() {
        // 0,001 градуса широты ≈ 111,2 м (geo_distance_m в БД - та же сфера)
        assertEquals(111.2, distanceM(43.238, 76.889, 43.239, 76.889), 0.1)
        assertEquals(0.0, distanceM(43.238, 76.889, 43.238, 76.889), 1e-9)
    }

    @Test
    fun whoTracksPosition() {
        assertTrue(DealState(deal = deal()).tracksPosition)
        assertTrue(DealState(deal = deal(status = "CONTACT", role = "provider")).tracksPosition)
        assertFalse(DealState(deal = deal(status = "OFFERS")).tracksPosition)
        assertFalse(DealState(deal = deal(status = "CLOSED")).tracksPosition)
        assertFalse(DealState(deal = deal(role = "offerer")).tracksPosition)
        assertFalse(DealState(deal = deal(placeMode = "online")).tracksPosition)
    }

    @Test
    fun testingModeOfMySide() {
        assertTrue(DealState(deal = deal(role = "client", testing = true)).iAmTesting)
        // поставщик - вторая сторона: у него признак provider.isTesting
        assertFalse(DealState(deal = deal(role = "provider", testing = true)).iAmTesting)
        assertTrue(DealState(deal = deal(role = "provider", testing = false)).iAmTesting)
    }

    @Test
    fun startPointOfOwnCursor() {
        val offer = Offer(dealOfferId = 3, dealId = 8, providerAgentId = 8, offerStatus = "selected", showLat = 43.2385, showLon = 76.8895)
        assertEquals(LatLon(43.238, 76.889), ownStartPoint(deal(role = "client"), listOf(offer)))
        assertEquals(LatLon(43.2385, 76.8895), ownStartPoint(deal(role = "provider"), listOf(offer)))
        assertEquals(LatLon(43.238, 76.889), ownStartPoint(deal(role = "provider"), emptyList()))
        assertNull(ownStartPoint(null, emptyList()))
    }

    @Test
    fun distanceToOtherSide() {
        val withBoth = DealState(deal = deal(), positions = listOf(pos("client", 43.238), pos("provider", 43.239)))
        assertEquals(111.2, withBoth.distanceToOther!!, 0.1)
        // курсор на экране важнее устаревшего положения на сервере (режим тестирования)
        val moved = withBoth.copy(myCursor = LatLon(43.2385, 76.889))
        assertEquals(55.6, moved.distanceToOther!!, 0.1)
        assertNull(DealState(deal = deal(), positions = listOf(pos("client", 43.238))).distanceToOther)
    }
}
