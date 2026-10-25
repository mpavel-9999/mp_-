package ru.mpavel.osmmap.feature.provider

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
import ru.mpavel.osmmap.core.time.nowMp
import ru.mpavel.osmmap.core.time.toServerTime
import ru.mpavel.osmmap.data.DealRepository
import ru.mpavel.osmmap.data.ForecastRequest
import ru.mpavel.osmmap.data.MyService
import ru.mpavel.osmmap.data.ProviderRepository
import ru.mpavel.osmmap.data.ServiceRepository
import ru.mpavel.osmmap.data.SessionRepository
import ru.mpavel.osmmap.data.SlotCreate
import ru.mpavel.osmmap.ui.map.LatLon
import java.time.LocalDateTime

/** Какую точку маршрута ставит нажатие на карту: А (откуда) или Б (куда). */
enum class SlotTarget { A, B }

/**
 * Публикация поставщика (алгоритм 4.1, 4.2): место или маршрут А -> Б с радиусами rA и rB, окно, мин. цена.
 * Время - по Алматы (Р-4): окно по умолчанию через 30 мин на 1 ч.
 */
data class SlotState(
    val loading: Boolean = true,
    val error: AppError? = null,
    val service: MyService? = null,
    val settings: Map<String, String?> = emptyMap(),
    val pointA: LatLon? = null,
    val pointB: LatLon? = null,
    val target: SlotTarget = SlotTarget.A,
    val radiusA: Int = DEFAULT_RADIUS_A_M,
    val radiusB: Int = DEFAULT_RADIUS_B_M,
    val start: LocalDateTime = defaultPeriod(nowMp()).first,
    val end: LocalDateTime = defaultPeriod(nowMp()).second,
    val price: String = "",
    /** Линия маршрута А -> Б по дорогам до публикации (для такси - из POST /forecast). */
    val routeLine: List<LatLon> = emptyList(),
    val routeLengthM: Double? = null,
    val geoMissing: Boolean = false,
    val submitting: Boolean = false,
    val submitError: AppError? = null,
) {
    val geoMode: String get() = settings["geo_mode"] ?: service?.geoMode ?: "point"
    val priceUnit: String? get() = settings["price_unit"] ?: service?.priceUnit
    val isRoute: Boolean get() = geoMode == "route"

    companion object {
        const val DEFAULT_RADIUS_A_M = 300
        const val DEFAULT_RADIUS_B_M = 1000
    }
}

/** Тело POST /slots; null - не выбрана точка (или А маршрута). Для места радиус не нужен, для маршрута - rA и, если есть Б, rB. */
fun buildSlotCreate(s: SlotState): SlotCreate? {
    val serviceId = s.service?.serviceId ?: return null
    val a = s.pointA ?: return null
    val b = s.pointB?.takeIf { s.isRoute }
    return SlotCreate(
        serviceId = serviceId,
        lat = a.lat,
        lon = a.lon,
        radiusM = if (s.isRoute) s.radiusA.toDouble() else null,
        toLat = b?.lat,
        toLon = b?.lon,
        radiusToM = b?.let { s.radiusB.toDouble() },
        periodStart = toServerTime(s.start),
        periodEnd = toServerTime(s.end),
        priceMin = s.price.replace(',', '.').trim().toDoubleOrNull(),
    )
}

class SlotViewModel(
    private val session: SessionRepository,
    private val services: ServiceRepository,
    private val deals: DealRepository,
    private val provider: ProviderRepository,
    private val serviceId: Long,
) : ViewModel() {

    private val _state = MutableStateFlow(SlotState())
    val state: StateFlow<SlotState> = _state
    private val _published = MutableSharedFlow<Long>(extraBufferCapacity = 1)
    /** Номер созданной публикации - экран возвращается в раздел поставщика. */
    val published: SharedFlow<Long> = _published
    private var routeJob: Job? = null

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val service = session.me().services.firstOrNull { it.serviceId == serviceId }
                    ?: throw ApiError(404, "NOT_FOUND", "#$serviceId")
                val settings = services.settings(serviceId)
                // точка по умолчанию - точка обслуживания из настроек сервиса поставщика (если задана)
                val home = if (service.lat != null && service.lon != null) LatLon(service.lat, service.lon) else null
                _state.update {
                    it.copy(
                        loading = false,
                        service = service,
                        settings = settings,
                        pointA = it.pointA ?: home,
                        price = it.price.ifEmpty { service.priceMin?.toInt()?.toString().orEmpty() },
                    )
                }
            } catch (e: AppError) {
                _state.update { it.copy(loading = false, error = e) }
            }
        }
    }

    /** Нажатие на карту: место; для маршрута - А или Б по переключателю, после А переключается на Б. */
    fun onMapTap(p: LatLon) {
        _state.update { s ->
            when {
                !s.isRoute -> s.copy(pointA = p, geoMissing = false)
                s.target == SlotTarget.A -> s.copy(pointA = p, target = SlotTarget.B, geoMissing = false)
                else -> s.copy(pointB = p)
            }
        }
        if (_state.value.isRoute) refreshRoute()
    }

    fun setTarget(t: SlotTarget) = _state.update { it.copy(target = t) }
    fun clearB() {
        _state.update { it.copy(pointB = null, routeLine = emptyList(), routeLengthM = null, target = SlotTarget.B) }
    }
    fun setRadiusA(m: Int) = _state.update { it.copy(radiusA = m) }
    fun setRadiusB(m: Int) = _state.update { it.copy(radiusB = m) }
    fun setStart(t: LocalDateTime) = _state.update { s ->
        // окно не короче 5 мин: конец сдвигается вместе с началом
        s.copy(start = t, end = if (s.end.isAfter(t)) s.end else t.plusMinutes(s.durationMin()))
    }
    fun setEnd(t: LocalDateTime) = _state.update { it.copy(end = t) }
    fun setPrice(v: String) = _state.update { it.copy(price = v.filter { c -> c.isDigit() || c == '.' || c == ',' }) }

    private fun SlotState.durationMin(): Long =
        java.time.Duration.between(start, end).toMinutes().coerceAtLeast(5)

    /** Линия А -> Б по дорогам до публикации: сервер строит её в POST /forecast для услуг с ценой за км. */
    private fun refreshRoute() {
        routeJob?.cancel()
        val s = _state.value
        val a = s.pointA
        val b = s.pointB
        if (a == null || b == null || s.priceUnit != "km") {
            _state.update { it.copy(routeLine = emptyList(), routeLengthM = null) }
            return
        }
        routeJob = viewModelScope.launch {
            delay(400)
            try {
                val f = deals.forecast(ForecastRequest(serviceId, a.lat, a.lon, b.lat, b.lon))
                _state.update {
                    it.copy(routeLine = decodePolyline(f.routePolyline).map { (lat, lon) -> LatLon(lat, lon) }, routeLengthM = f.routeLengthM)
                }
            } catch (_: AppError) {
                // линия - только подсказка; публикация построит маршрут сама и вернёт понятную ошибку
                _state.update { it.copy(routeLine = emptyList(), routeLengthM = null) }
            }
        }
    }

    fun submit() {
        val body = buildSlotCreate(_state.value)
        if (body == null) {
            _state.update { it.copy(geoMissing = true) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(submitting = true, submitError = null) }
            try {
                val slot = provider.publish(body)
                _state.update { it.copy(submitting = false) }
                _published.tryEmit(slot.slotId)
            } catch (e: AppError) {
                _state.update { it.copy(submitting = false, submitError = e) }
            }
        }
    }
}
