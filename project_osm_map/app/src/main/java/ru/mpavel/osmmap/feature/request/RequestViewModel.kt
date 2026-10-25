package ru.mpavel.osmmap.feature.request

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.mpavel.osmmap.core.geo.decodePolyline
import ru.mpavel.osmmap.core.net.ApiError
import ru.mpavel.osmmap.core.net.AppError
import ru.mpavel.osmmap.core.time.defaultPeriod
import ru.mpavel.osmmap.core.time.parseServerTime
import ru.mpavel.osmmap.core.time.toServerTime
import ru.mpavel.osmmap.data.AttrValue
import ru.mpavel.osmmap.data.DealCreate
import ru.mpavel.osmmap.data.DealRepository
import ru.mpavel.osmmap.data.Forecast
import ru.mpavel.osmmap.data.ForecastRequest
import ru.mpavel.osmmap.data.FormField
import ru.mpavel.osmmap.data.ServiceNode
import ru.mpavel.osmmap.data.ServiceRepository
import ru.mpavel.osmmap.ui.map.DefaultCenter
import ru.mpavel.osmmap.ui.map.LatLon
import java.time.LocalDateTime

/** Какую точку маршрута ставит нажатие на карту: подача П или назначение Н. */
enum class RouteTarget { PICKUP, DEST }

/** Запрос клиента (алгоритм п. 1.1-1.10). Поля гео зависят от настроек услуги: geo_mode, place_mode, price_unit. */
data class RequestState(
    val loading: Boolean = true,
    val error: AppError? = null,
    /** Узел, с которого начали (сервис или вариант). */
    val node: ServiceNode? = null,
    /** Варианты узла (классы такси, виды парковки); пусто - сделка на самом узле. */
    val variants: List<ServiceNode> = emptyList(),
    /** Узел сделки (п. 1.1: сделка создаётся на варианте). */
    val selected: ServiceNode? = null,
    val settings: Map<String, String?> = emptyMap(),
    val slotId: Long? = null,
    val point: LatLon? = null,
    val pickup: LatLon? = null,
    val dest: LatLon? = null,
    val routeTarget: RouteTarget = RouteTarget.PICKUP,
    val start: LocalDateTime = defaultPeriod(LocalDateTime.now()).first,
    val end: LocalDateTime = defaultPeriod(LocalDateTime.now()).second,
    val radiusM: Int = DEFAULT_RADIUS_M,
    val ratingMin: Double? = null,
    val fields: List<FormField> = emptyList(),
    val values: Map<Long, String> = emptyMap(),
    /** Поля, которые нужно исправить (обязательные пустые, число не числом). */
    val invalid: Set<Long> = emptySet(),
    val forecast: Forecast? = null,
    val forecastError: AppError? = null,
    val routeLine: List<LatLon> = emptyList(),
    val myPrice: String = "",
    val myPriceTouched: Boolean = false,
    val geoMissing: Boolean = false,
    val submitting: Boolean = false,
    val submitError: AppError? = null,
) {
    val geoMode: String get() = settings["geo_mode"] ?: selected?.geoMode ?: "point"
    val placeMode: String? get() = settings["place_mode"]
    val priceUnit: String? get() = settings["price_unit"] ?: selected?.priceUnit

    /** Радиус зоны имеет смысл, когда поставщик должен быть в зоне клиента (place_mode = provider, п. 2.1). */
    val showRadius: Boolean get() = geoMode == "point" && placeMode == "provider"

    /** Точка, вокруг которой карта и зона: точка услуги или подача. */
    val anchor: LatLon? get() = if (geoMode == "route") pickup else point

    companion object {
        const val DEFAULT_RADIUS_M = 100
    }
}

sealed interface RequestEvent {
    data class Created(val dealId: Long) : RequestEvent
    data class ReviewFirst(val dealId: Long) : RequestEvent
}

class RequestViewModel(
    private val services: ServiceRepository,
    private val deals: DealRepository,
    private val serviceId: Long,
    private val slotId: Long?,
) : ViewModel() {

    private val _state = MutableStateFlow(RequestState(slotId = slotId))
    val state: StateFlow<RequestState> = _state
    private val _events = MutableSharedFlow<RequestEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<RequestEvent> = _events
    private var forecastJob: Job? = null

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val tree = services.search("")
                val node = tree.firstOrNull { it.serviceId == serviceId } ?: throw ApiError(404, "NOT_FOUND", "#$serviceId")
                val variants = tree.filter { it.parentServiceId == node.serviceId }
                // Запрос из публикации (п. 1.1.2): точка и окно поставщика
                val slot = slotId?.let { id ->
                    deals.mapSlots(DefaultCenter.lat, DefaultCenter.lon, SLOT_SEARCH_RADIUS_M).firstOrNull { it.slotId == id }
                }
                _state.update { s ->
                    var next = s.copy(node = node, variants = variants)
                    if (slot != null) {
                        val start = parseServerTime(slot.periodStart)
                        val end = parseServerTime(slot.periodEnd)
                        next = next.copy(point = LatLon(slot.lat, slot.lon), pickup = LatLon(slot.lat, slot.lon))
                        if (start != null && end != null && end.isAfter(LocalDateTime.now())) {
                            val from = maxOf(start, defaultPeriod(LocalDateTime.now()).first)
                            next = next.copy(start = from, end = minOf(end, from.plusHours(1)).takeIf { it.isAfter(from) } ?: from.plusHours(1))
                        }
                    }
                    next
                }
                select(variants.firstOrNull() ?: node)
            } catch (e: AppError) {
                _state.update { it.copy(loading = false, error = e) }
            }
        }
    }

    /** Выбор варианта: настройки и поля запроса - свои у каждого узла (наследуются по дереву). */
    fun select(node: ServiceNode) {
        viewModelScope.launch {
            _state.update { it.copy(selected = node, loading = true, error = null) }
            try {
                val settings = services.settings(node.serviceId)
                val fields = services.form(node.serviceId, "request")
                _state.update { s ->
                    val geo = settings["geo_mode"] ?: node.geoMode ?: "point"
                    s.copy(
                        settings = settings,
                        fields = fields,
                        values = s.values.filterKeys { id -> fields.any { it.attributeId == id } },
                        invalid = emptySet(),
                        point = if (geo == "point") s.point ?: DefaultCenter else s.point,
                        pickup = if (geo == "route") s.pickup ?: DefaultCenter else s.pickup,
                        loading = false,
                    )
                }
                requestForecast()
            } catch (e: AppError) {
                _state.update { it.copy(loading = false, error = e) }
            }
        }
    }

    /** Нажатие на карту: точка услуги или очередная точка маршрута (П, затем Н). */
    fun onMapTap(p: LatLon) {
        _state.update { s ->
            when (s.geoMode) {
                "route" -> if (s.routeTarget == RouteTarget.PICKUP) {
                    s.copy(pickup = p, routeTarget = RouteTarget.DEST, geoMissing = false)
                } else {
                    s.copy(dest = p, geoMissing = false)
                }
                "point" -> s.copy(point = p, geoMissing = false)
                else -> s
            }
        }
        requestForecast()
    }

    fun setRouteTarget(target: RouteTarget) = _state.update { it.copy(routeTarget = target) }

    fun setStart(t: LocalDateTime) = _state.update { s ->
        s.copy(start = t, end = if (s.end.isAfter(t)) s.end else t.plusHours(1))
    }

    fun setEnd(t: LocalDateTime) = _state.update { s -> s.copy(end = if (t.isAfter(s.start)) t else s.start.plusMinutes(15)) }

    fun setRadius(m: Int) = _state.update { it.copy(radiusM = m) }

    fun setRatingMin(r: Double?) = _state.update { it.copy(ratingMin = r) }

    fun setField(id: Long, value: String) = _state.update { it.copy(values = it.values + (id to value), invalid = it.invalid - id) }

    fun setMyPrice(value: String) = _state.update { it.copy(myPrice = value.filter { c -> c.isDigit() }, myPriceTouched = true) }

    /** Прогноз цены (п. 1.7, 1.9) с задержкой 500 мс; "Моя цена" = медиана, пока клиент её не менял (Н-1). */
    private fun requestForecast() {
        forecastJob?.cancel()
        val s = _state.value
        val node = s.selected ?: return
        val from = s.anchor
        if (s.geoMode == "route" && (from == null || s.dest == null)) return
        forecastJob = viewModelScope.launch {
            delay(500)
            try {
                val f = deals.forecast(ForecastRequest(node.serviceId, from?.lat, from?.lon, s.dest?.lat, s.dest?.lon))
                _state.update { cur ->
                    cur.copy(
                        forecast = f,
                        forecastError = null,
                        routeLine = decodePolyline(f.routePolyline).map { LatLon(it.first, it.second) },
                        myPrice = if (cur.myPriceTouched) cur.myPrice else f.priceMedian?.let { "%.0f".format(it) }.orEmpty(),
                    )
                }
            } catch (e: AppError) {
                _state.update { it.copy(forecast = null, forecastError = e, routeLine = emptyList()) }
            }
        }
    }

    /** "Найти поставщиков" (п. 1.10): проверка заполнения, затем POST /deals; правила проверяет сервер. */
    fun submit() {
        val s = _state.value
        val node = s.selected ?: return
        val invalid = invalidFields(s.fields, s.values)
        val geoMissing = when (s.geoMode) {
            "route" -> s.pickup == null || s.dest == null
            "point" -> s.point == null
            else -> false
        }
        _state.update { it.copy(invalid = invalid, geoMissing = geoMissing, submitError = null) }
        if (invalid.isNotEmpty() || geoMissing) return
        viewModelScope.launch {
            _state.update { it.copy(submitting = true) }
            try {
                val deal = deals.create(buildDealCreate(s, node.serviceId))
                _events.tryEmit(RequestEvent.Created(deal.dealId))
            } catch (e: ApiError) {
                if (e.code == "REVIEW_REQUIRED") {
                    runCatching { deals.pendingReviews().firstOrNull() }.getOrNull()?.let {
                        _events.tryEmit(RequestEvent.ReviewFirst(it.dealId))
                    }
                }
                _state.update { it.copy(submitError = e) }
            } catch (e: AppError) {
                _state.update { it.copy(submitError = e) }
            } finally {
                _state.update { it.copy(submitting = false) }
            }
        }
    }

    private companion object {
        const val SLOT_SEARCH_RADIUS_M = 30000
    }
}

/** Поля, которые не дают отправить запрос: обязательное пустое или число не числом. */
fun invalidFields(fields: List<FormField>, values: Map<Long, String>): Set<Long> = fields.filter { f ->
    val v = values[f.attributeId]?.trim().orEmpty()
    (f.required && v.isEmpty()) || (f.type == "number" && v.isNotEmpty() && v.replace(',', '.').toDoubleOrNull() == null)
}.map { it.attributeId }.toSet()

/** Тело POST /deals из состояния экрана; поля с пустым значением не отправляются. */
fun buildDealCreate(s: RequestState, serviceId: Long): DealCreate {
    val from = s.anchor
    return DealCreate(
        serviceId = serviceId,
        slotId = s.slotId,
        lat = if (s.geoMode == "none") null else from?.lat,
        lon = if (s.geoMode == "none") null else from?.lon,
        toLat = if (s.geoMode == "route") s.dest?.lat else null,
        toLon = if (s.geoMode == "route") s.dest?.lon else null,
        zoneRadiusM = if (s.geoMode == "none") null else s.radiusM.toDouble(),
        periodStart = toServerTime(s.start),
        periodEnd = toServerTime(s.end),
        providerRatingMin = s.ratingMin,
        clientPrice = s.myPrice.toDoubleOrNull(),
        attributes = s.values.filterValues { it.isNotBlank() }.map { (id, v) ->
            val field = s.fields.firstOrNull { it.attributeId == id }
            AttrValue(id, if (field?.type == "number") v.trim().replace(',', '.') else v.trim())
        },
    )
}
