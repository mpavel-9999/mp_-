package ru.mpavel.osmmap

import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.mpavel.osmmap.core.location.Fix
import ru.mpavel.osmmap.core.location.positionUpdate
import ru.mpavel.osmmap.core.net.decodeBody
import ru.mpavel.osmmap.data.CloseCode
import ru.mpavel.osmmap.data.Deal
import ru.mpavel.osmmap.data.Notification
import ru.mpavel.osmmap.data.Party
import ru.mpavel.osmmap.data.PositionResult
import ru.mpavel.osmmap.feature.close.CloseButton
import ru.mpavel.osmmap.feature.close.autoConfirm
import ru.mpavel.osmmap.feature.close.closeCodeFrom
import ru.mpavel.osmmap.feature.close.closeButtons
import ru.mpavel.osmmap.feature.close.codeSecondsLeft
import ru.mpavel.osmmap.feature.close.qrDealId
import ru.mpavel.osmmap.feature.close.qrMatrix

/** Основа закрытия и положения (план Э6, шаги 3, 5): ответы сервера, источник положения, кнопки, таймер, QR. */
class CloseFlowTest {

    @Test
    fun closeCodeFromServer() {
        // ответ GET /deals/{id}/close/code, снят с сервера 26.10.2026 (api_smoke.py)
        val c = decodeBody<CloseCode>("/close/code",
            """{"method":"QR","code":"048213","qr_payload":"mpclose:8:048213","ttl_s":10,"expires_in_s":7}""")
        assertEquals("QR", c.method)
        assertEquals("048213", c.code)
        assertEquals(10, c.ttlS)
        assertEquals(7, c.expiresInS)
        assertEquals(8L, qrDealId(c.qrPayload))
    }

    @Test
    fun positionResult() {
        assertEquals(listOf(8L), decodeBody<PositionResult>("/me/position", """{"status":"ok","contact":[8]}""").contact)
        assertEquals(emptyList<Long>(), decodeBody<PositionResult>("/me/position", """{"status":"ok","contact":[]}""").contact)
    }

    @Test
    fun sourceByTestingMode() {
        val cursor = Fix(43.238, 76.889)
        val gps = Fix(43.240, 76.890, accuracyM = 12.0)
        // тестовый агент - только курсор, даже если есть GPS
        val manual = positionUpdate(isTesting = true, manual = cursor, gps = gps)!!
        assertEquals("manual", manual.source)
        assertEquals(43.238, manual.lat, 1e-9)
        assertNull(positionUpdate(isTesting = true, manual = null, gps = gps))
        // обычный агент - только GPS с точностью
        val fromGps = positionUpdate(isTesting = false, manual = cursor, gps = gps)!!
        assertEquals("gps", fromGps.source)
        assertEquals(12.0, fromGps.accuracyM!!, 1e-9)
        assertNull(positionUpdate(isTesting = false, manual = cursor, gps = null))
    }

    @Test
    fun buttonsOnlyFromActions() {
        assertEquals(emptyList<CloseButton>(), closeButtons(listOf("cancel", "route")))
        assertEquals(listOf(CloseButton.CLOSE, CloseButton.SHOW_CODE, CloseButton.TEST_PROVIDE),
                     closeButtons(listOf("test_provide", "cancel", "show_code", "close")))
        assertEquals(listOf(CloseButton.CLOSE, CloseButton.ENTER_CODE, CloseButton.TEST_SCAN),
                     closeButtons(listOf("close", "enter_code", "test_scan", "review")))
    }

    @Test
    fun codeTimer() {
        assertEquals(7, codeSecondsLeft(7, receivedAtMs = 1_000, nowMs = 1_500))
        assertEquals(4, codeSecondsLeft(7, receivedAtMs = 1_000, nowMs = 4_000))
        assertEquals(0, codeSecondsLeft(7, receivedAtMs = 1_000, nowMs = 60_000))
    }

    @Test
    fun qrPayload() {
        assertEquals(12L, qrDealId(" mpclose:12:000123 "))
        assertNull(qrDealId("123456"))
        assertNull(qrDealId("mpclose:x:123456"))
        assertNull(qrDealId("https://example.com/mpclose:1:2"))
        assertNull(qrDealId("mpclose:12:"))
    }

    @Test
    fun qrImageReadsBack() {
        // то, что рисует экран кода, читается сканером: строка возвращается без изменений
        val m = qrMatrix("mpclose:8:048213", 256)
        val pixels = IntArray(m.width * m.height) { i -> if (m.get(i % m.width, i / m.width)) 0xFF000000.toInt() else -1 }
        val bitmap = BinaryBitmap(HybridBinarizer(RGBLuminanceSource(m.width, m.height, pixels)))
        assertEquals("mpclose:8:048213", QRCodeReader().decode(bitmap).text)
    }

    @Test
    fun closeCodeNotification() {
        // уведомление CLOSE_CODE ("Предоставить QR"): payload - ответ close/code
        val n = decodeBody<Notification>("/notifications",
            """{"notification_id":41,"deal_id":8,"type":"CLOSE_CODE","text":"Код закрытия получен",
               "payload":{"method":"QR","code":"048213","qr_payload":"mpclose:8:048213","ttl_s":10,"expires_in_s":6}}""")
        assertEquals("048213", closeCodeFrom(n))
        assertNull(closeCodeFrom(n.copy(type = "CLOSE_REQUEST")))
        assertNull(closeCodeFrom(n.copy(payload = null)))
    }

    @Test
    fun autoConfirmOnlyTestingClient() {
        fun deal(role: String, testing: Boolean, actions: List<String>) =
            Deal(dealId = 8, serviceId = 1001, status = "CONTACT", myRole = role, client = Party(isTesting = testing), actions = actions)
        assertTrue(autoConfirm(deal("client", testing = true, actions = listOf("close", "enter_code", "test_scan"))))
        // обычный клиент - экран подтверждения с подставленным кодом
        assertFalse(autoConfirm(deal("client", testing = false, actions = listOf("close", "enter_code"))))
        // ввод кода сервер не разрешает (гео-услуга до сближения)
        assertFalse(autoConfirm(deal("client", testing = true, actions = listOf("close"))))
        assertFalse(autoConfirm(deal("provider", testing = true, actions = listOf("close", "show_code"))))
    }
}
