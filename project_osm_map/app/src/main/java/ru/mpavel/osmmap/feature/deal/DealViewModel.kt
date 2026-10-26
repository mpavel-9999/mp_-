package ru.mpavel.osmmap.feature.deal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ru.mpavel.osmmap.core.geo.distanceM
import ru.mpavel.osmmap.core.location.Fix
import ru.mpavel.osmmap.core.location.PositionSender
import ru.mpavel.osmmap.core.net.ApiError
import ru.mpavel.osmmap.core.net.AppError
import ru.mpavel.osmmap.core.notify.NotificationCenter
import ru.mpavel.osmmap.data.CancelRequest
import ru.mpavel.osmmap.data.Deal
import ru.mpavel.osmmap.data.DealRepository
import ru.mpavel.osmmap.data.Offer
import ru.mpavel.osmmap.data.Position
import ru.mpavel.osmmap.data.ProviderRepository
import ru.mpavel.osmmap.data.RouteLink
import ru.mpavel.osmmap.ui.map.LatLon

/** Статусы до согласования - экран откликов; после - экран сделки (алгоритм, раздел 2). */
val OPEN_STATUSES = setOf("INIT", "MATCHING", "OFFERS")
val AGREED_STATUSES = setOf("AGREED", "CONTACT")

data class DealState(
    val loading: Boolean = true,
    val error: AppError? = null,
    val deal: Deal? = null,
    val offers: List<Offer> = emptyList(),
    val routes: List<RouteLink> = emptyList(),
    val positions: List<Position> = emptyList(),
    val busy: Boolean = false,
    val actionError: AppError? = null,
    val cancelOpen: Boolean = false,
    /** Кандидат: рекомендованная цена из GET /requests (для такси - по тарифу) и открыт ли диалог отклика. */
    val recommended: Double? = null,
    val offerOpen: Boolean = false,
    /** Точка своего курсора (режим тестирования), пока сервер не вернул своё положение. */
    val myCursor: LatLon? = null,
    /** Разрешение на геолокацию (обычный режим): null - ещё не спрашивали, false - отказано. */
    val gpsGranted: Boolean? = null,
) {
    val isOpen: Boolean get() = deal?.status in OPEN_STATUSES

    /** Онлайн-услуга (geo_mode = none) - без карты. */
    val hasMap: Boolean get() = deal?.lat != null && deal.geoMode != "none"
    fun can(action: String): Boolean = deal?.actions?.contains(action) == true

    /** Агент в режиме тестирования (своя сторона сделки): положение - курсором, не GPS (ТЗ 5.4). */
    val iAmTesting: Boolean
        get() = (if (deal?.myRole == "provider") deal.provider?.isTesting else deal?.client?.isTesting) == true

    /** Экран отправляет положение: сторона согласованной гео-сделки (онлайн-услуге сближение не нужно). */
    val tracksPosition: Boolean
        get() = deal != null && deal.status in AGREED_STATUSES && (deal.myRole == "client" || deal.myRole == "provider") &&
            hasMap && deal.placeMode != "online"

    /** Своё положение: в режиме тестирования - точка курсора на экране (сервер догонит), иначе - с сервера (GPS). */
    val myPoint: LatLon?
        get() = (if (iAmTesting) myCursor else null)
            ?: positions.firstOrNull { it.role == deal?.myRole }?.let { LatLon(it.lat, it.lon) }

    /** Расстояние до другой стороны, м; null - чьё-то положение неизвестно. */
    val distanceToOther: Double?
        get() {
            val me = myPoint ?: return null
            val other = positions.firstOrNull { it.role != deal?.myRole } ?: return null
            return distanceM(me.lat, me.lon, other.lat, other.lon)
        }
}

/**
 * Где поставить свой курсор, пока своего положения нет (план Э6, шаг 4): клиенту - точка запроса, поставщику -
 * точка выбранного отклика (после согласования она точная), иначе точка запроса. Сервер о ней не знает,
 * пока агент не передвинет курсор - иначе сближение засчиталось бы само.
 */
fun ownStartPoint(deal: Deal?, offers: List<Offer>): LatLon? {
    deal ?: return null
    val dealPoint = if (deal.lat != null && deal.lon != null) LatLon(deal.lat, deal.lon) else null
    if (deal.myRole != "provider") return dealPoint
    val mine = offers.firstOrNull { it.offerStatus == "selected" } ?: offers.firstOrNull()
    return if (mine?.showLat != null && mine.showLon != null) LatLon(mine.showLat, mine.showLon) else dealPoint
}

/**
 * Пометка для откликнувшегося поставщика (offerer): ждём клиента, отклик снят или клиент выбрал другого.
 * Список откликов поставщику - только свой и только active/selected (v_deal_offer_list), поэтому снятый отклик - пустой список.
 */
fun offererNoteKey(deal: Deal?, offers: List<Offer>): String? = when {
    deal?.myRole != "offerer" -> null
    deal.status !in OPEN_STATUSES -> "ui.offer_declined"
    offers.any { it.offerStatus == "active" } -> "ui.waiting_client"
    else -> "ui.offer_withdrawn"
}

sealed interface DealEvent {
    data class Review(val dealId: Long) : DealEvent
}

/**
 * Сделка глазами любой стороны (роль - Deal.myRole): клиент - отклики и согласование (п. 4);
 * поставщик-кандидат - запрос и отклик (п. 3.3); откликнувшийся - свой отклик и ожидание; исполнитель - сделка.
 * Затем этапы, маршрут, положения сторон, отмена (п. 5.2-5.4).
 * Свежесть - по уведомлениям этой сделки (опрос раз в 2 с, пока экран открыт); положения сторон - раз в 5 с.
 */
class DealViewModel(
    private val deals: DealRepository,
    private val provider: ProviderRepository,
    private val center: NotificationCenter,
    private val sender: PositionSender,
    private val dealId: Long,
) : ViewModel() {

    /** Экран держит отправку положения (PositionSender.acquire) - только сторона согласованной гео-сделки. */
    private var tracking = false

    private val _state = MutableStateFlow(DealState())
    val state: StateFlow<DealState> = _state
    private val _events = MutableSharedFlow<DealEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<DealEvent> = _events

    init {
        center.fast(true)
        load()
        // откликнувшемуся - по любому уведомлению: отклик снимается, когда его выбрали в другой сделке (уведомление - о ней)
        viewModelScope.launch {
            center.events.filter { it.dealId == dealId || _state.value.deal?.myRole == "offerer" }.collect { load(quiet = true) }
        }
        // сближение засчитано сразу после отправки положения (PUT /me/position -> contact)
        viewModelScope.launch {
            sender.contact.filter { dealId in it }.collect { load(quiet = true) }
        }
        viewModelScope.launch {
            while (isActive) {
                delay(POSITIONS_MS)
                val d = _state.value.deal
                if (d?.status in AGREED_STATUSES && (d?.myRole == "client" || d?.myRole == "provider")) refreshPositions()
            }
        }
    }

    override fun onCleared() {
        center.fast(false)
        track(false)
    }

    /** Включить или выключить отправку положения по статусу сделки (ТЗ 5.4: раз в 10 с на экране сделки). */
    private fun track(on: Boolean) {
        if (on == tracking) return
        tracking = on
        if (on) sender.acquire(_state.value.iAmTesting) else sender.release()
        if (on && !_state.value.iAmTesting) _state.update { it.copy(gpsGranted = if (sender.hasPermission()) true else it.gpsGranted) }
    }

    /** Свой курсор отпущен или поставлен долгим нажатием (режим тестирования): положение - на сервер сразу. */
    fun moveMyCursor(point: LatLon) {
        if (!_state.value.iAmTesting) return
        _state.update { it.copy(myCursor = point) }
        viewModelScope.launch {
            val result = sender.moveManual(Fix(point.lat, point.lon))
            if (result == null || dealId !in result.contact) refreshPositions()
        }
    }

    /** Ответ на запрос разрешения геолокации. */
    fun gpsPermission(granted: Boolean) {
        _state.update { it.copy(gpsGranted = granted) }
        if (granted) sender.onPermissionGranted()
    }

    fun hasGpsPermission(): Boolean = sender.hasPermission()

    /** Сделка и отклики; после согласования - ссылки на маршрут и положения сторон. */
    fun load(quiet: Boolean = false) {
        viewModelScope.launch {
            if (!quiet) _state.update { it.copy(loading = true, error = null) }
            try {
                val deal = deals.get(dealId)
                val offers = deals.offers(dealId)
                val agreed = deal.status in AGREED_STATUSES
                // маршрут и положения - только сторонам сделки (откликнувшийся, которого не выбрали, их не получает)
                val party = deal.myRole == "client" || deal.myRole == "provider"
                val routes = if (agreed && party) runCatching { deals.routes(dealId) }.getOrDefault(emptyList()) else emptyList()
                val recommended = if ("offer" in deal.actions) {
                    runCatching { provider.requests().firstOrNull { it.dealId == dealId }?.recommendedPrice }.getOrNull()
                } else null
                _state.update {
                    it.copy(loading = false, deal = deal, offers = offers.sortedBy { o -> o.position ?: Int.MAX_VALUE }, routes = routes,
                            recommended = recommended ?: it.recommended)
                }
                if (agreed && party) refreshPositions()
                _state.update { it.copy(myCursor = it.myCursor ?: if (it.iAmTesting) ownStartPoint(deal, it.offers) else null) }
                track(_state.value.tracksPosition)
            } catch (e: AppError) {
                _state.update { it.copy(loading = false, error = if (it.deal == null) e else null, actionError = if (it.deal != null) e else null) }
            }
        }
    }

    private suspend fun refreshPositions() {
        try {
            val positions = deals.positions(dealId)
            _state.update { it.copy(positions = positions) }
            // после перезапуска приложения точка курсора есть только на сервере - продолжаем повторять её
            val mine = positions.firstOrNull { it.role == _state.value.deal?.myRole && it.source == "manual" }
            if (mine != null && sender.manual.value == null && _state.value.iAmTesting) {
                _state.update { it.copy(myCursor = LatLon(mine.lat, mine.lon)) }
                sender.moveManual(Fix(mine.lat, mine.lon))
            }
        } catch (_: AppError) {
            // положения не обязательны для экрана
        }
    }

    /** "Согласовать" (п. 4.4); отклик мог уже уйти - сообщение и свежий список. */
    fun agree(offerId: Long) = act {
        deals.agree(dealId, offerId)
        load(quiet = true)
    }

    fun openCancel(open: Boolean) = _state.update { it.copy(cancelOpen = open, actionError = null) }

    /** Отклик кандидата - общий диалог OfferDialog; после отправки роль становится offerer. */
    fun openOffer(open: Boolean) = _state.update { it.copy(offerOpen = open, actionError = null) }

    fun offerSent() {
        openOffer(false)
        load(quiet = true)
    }

    /** Отмена: итог решает БД (deal_cancel); пострадавшей стороне - баллы (п. 5.6). */
    fun cancel(reasonId: Long?, comment: String?) = act {
        val result = deals.cancel(dealId, CancelRequest(reasonId, comment?.takeIf { it.isNotBlank() }))
        _state.update { it.copy(deal = result.deal, cancelOpen = false) }
        if (result.deal.reviewPending) _events.tryEmit(DealEvent.Review(dealId))
        load(quiet = true)
    }

    private fun act(block: suspend () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, actionError = null) }
            try {
                block()
            } catch (e: ApiError) {
                _state.update { it.copy(actionError = e) }
                load(quiet = true)
            } catch (e: AppError) {
                _state.update { it.copy(actionError = e) }
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }

    private companion object {
        const val POSITIONS_MS = 5_000L
    }
}
