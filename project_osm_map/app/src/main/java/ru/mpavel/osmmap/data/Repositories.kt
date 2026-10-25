package ru.mpavel.osmmap.data

import ru.mpavel.osmmap.core.net.ApiClient
import ru.mpavel.osmmap.core.net.decodeBody
import ru.mpavel.osmmap.core.net.SessionStore
import java.net.URLEncoder

// Репозитории - тонкие обёртки над методами API v0.7 (server/openapi.yaml), без правил процесса:
// что можно делать со сделкой, решает сервер (поле Deal.actions).

private fun enc(value: String) = URLEncoder.encode(value, "UTF-8")

/** Вход, выход, профиль, положение. */
class SessionRepository(private val api: ApiClient, private val store: SessionStore) {

    val isLoggedIn: Boolean get() = store.token != null

    /** Список агентов для входа без пароля; сервер без MP_DEV_LOGIN=1 отвечает 404. */
    suspend fun devAgents(): List<DevAgent> = api.get("/dev/agents")

    suspend fun login(login: String, password: String): Me =
        api.post<LoginRequest, LoginResponse>("/session", LoginRequest(login, password)).also { store.save(it.token) }.agent

    suspend fun loginDev(agentId: Long): Me =
        api.post<DevLoginRequest, LoginResponse>("/session", DevLoginRequest(agentId)).also { store.save(it.token) }.agent

    /** Выход: токен забывается и при недоступном сервере. */
    suspend fun logout() {
        try {
            api.call("DELETE", "/session")
        } finally {
            store.clear()
        }
    }

    /** Сессия истекла (401): только забыть токен. */
    fun forget() = store.clear()

    suspend fun me(): Me = api.get("/me")

    suspend fun updatePosition(update: PositionUpdate): StatusOk = api.put("/me/position", update)
}

/** Поиск услуг, формы и настройки узлов. */
class ServiceRepository(private val api: ApiClient) {

    suspend fun search(query: String = ""): List<ServiceNode> = api.get("/services?q=${enc(query)}")

    suspend fun form(serviceId: Long, scope: String = "request"): List<FormField> =
        api.get("/services/$serviceId/form?scope=${enc(scope)}")

    suspend fun settings(serviceId: Long): Map<String, String?> = api.get("/services/$serviceId/settings")
}

/** Сделка глазами клиента и обеих сторон: карта, прогноз, запрос, отклики, закрытие, отмена, баллы. */
class DealRepository(private val api: ApiClient) {

    suspend fun mapSlots(lat: Double, lon: Double, radiusM: Int, serviceId: Long? = null): List<MapSlot> =
        api.get("/map/slots?lat=$lat&lon=$lon&radius=$radiusM" + (serviceId?.let { "&service_id=$it" } ?: ""))

    suspend fun forecast(request: ForecastRequest): Forecast = api.post("/forecast", request)

    suspend fun create(request: DealCreate): Deal = api.post("/deals", request)

    suspend fun list(role: String = "all", status: String = "active"): List<DealListItem> =
        api.get("/deals?role=${enc(role)}&status=${enc(status)}")

    suspend fun get(dealId: Long): Deal = api.get("/deals/$dealId")

    suspend fun offers(dealId: Long): List<Offer> = api.get("/deals/$dealId/offers")

    suspend fun agree(dealId: Long, offerId: Long): Deal = api.post("/deals/$dealId/offers/$offerId/agree", Unit)

    suspend fun routes(dealId: Long): List<RouteLink> = api.get("/deals/$dealId/routes")

    suspend fun positions(dealId: Long): List<Position> = api.get("/deals/$dealId/positions")

    suspend fun closeRequest(dealId: Long): StatusOk = api.post("/deals/$dealId/close/request", Unit)

    suspend fun closeCode(dealId: Long): CloseCode = api.get("/deals/$dealId/close/code")

    suspend fun close(dealId: Long, code: String): Deal = api.post("/deals/$dealId/close", CloseSubmit(code))

    suspend fun testScan(dealId: Long): Deal = api.post("/deals/$dealId/close/test-scan", Unit)

    suspend fun testProvide(dealId: Long): CloseCode = api.post("/deals/$dealId/close/test-provide", Unit)

    suspend fun cancel(dealId: Long, request: CancelRequest): CancelResult = api.post("/deals/$dealId/cancel", request)

    suspend fun review(dealId: Long, request: ReviewRequest): Deal = api.post("/deals/$dealId/review", request)

    suspend fun pendingReviews(): List<PendingReview> = api.get("/me/pending-reviews")
}

/** Поставщик: публикации, входящие запросы, отклик, настройки сервиса. */
class ProviderRepository(private val api: ApiClient) {

    suspend fun publish(request: SlotCreate): Slot = api.post("/slots", request)

    suspend fun mySlots(): List<Slot> = api.get("/slots?mine=1")

    suspend fun removeSlot(slotId: Long): StatusOk = decodeBody("/slots/$slotId", api.call("DELETE", "/slots/$slotId").body)

    suspend fun requests(): List<IncomingRequest> = api.get("/requests")

    suspend fun offer(dealId: Long, request: OfferCreate): List<Offer> = api.post("/deals/$dealId/offers", request)

    suspend fun saveService(serviceId: Long, update: ServiceSettingsUpdate): StatusOk = api.put("/me/services/$serviceId", update)
}

/** Уведомления in_app (опрос раз в 2-5 с - на экранах, Э4). */
class NotificationRepository(private val api: ApiClient) {

    suspend fun since(lastId: Long): List<Notification> = api.get("/notifications?since=$lastId")

    suspend fun read(notificationId: Long): StatusOk = api.post("/notifications/$notificationId/read", Unit)
}
