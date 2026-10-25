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
import ru.mpavel.osmmap.core.net.ApiError
import ru.mpavel.osmmap.core.net.AppError
import ru.mpavel.osmmap.core.notify.NotificationCenter
import ru.mpavel.osmmap.data.CancelRequest
import ru.mpavel.osmmap.data.Deal
import ru.mpavel.osmmap.data.DealRepository
import ru.mpavel.osmmap.data.Offer
import ru.mpavel.osmmap.data.Position
import ru.mpavel.osmmap.data.RouteLink

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
) {
    val isOpen: Boolean get() = deal?.status in OPEN_STATUSES

    /** Онлайн-услуга (geo_mode = none) - без карты. */
    val hasMap: Boolean get() = deal?.lat != null && deal.geoMode != "none"
    fun can(action: String): Boolean = deal?.actions?.contains(action) == true
}

sealed interface DealEvent {
    data class Review(val dealId: Long) : DealEvent
}

/**
 * Сделка клиента: отклики и согласование (п. 4), затем этапы, маршрут, положения сторон, отмена (п. 5.2-5.4).
 * Свежесть - по уведомлениям этой сделки (опрос раз в 2 с, пока экран открыт); положения сторон - раз в 5 с.
 */
class DealViewModel(
    private val deals: DealRepository,
    private val center: NotificationCenter,
    private val dealId: Long,
) : ViewModel() {

    private val _state = MutableStateFlow(DealState())
    val state: StateFlow<DealState> = _state
    private val _events = MutableSharedFlow<DealEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<DealEvent> = _events

    init {
        center.fast(true)
        load()
        viewModelScope.launch { center.events.filter { it.dealId == dealId }.collect { load(quiet = true) } }
        viewModelScope.launch {
            while (isActive) {
                delay(POSITIONS_MS)
                if (_state.value.deal?.status in AGREED_STATUSES) refreshPositions()
            }
        }
    }

    override fun onCleared() {
        center.fast(false)
    }

    /** Сделка и отклики; после согласования - ссылки на маршрут и положения сторон. */
    fun load(quiet: Boolean = false) {
        viewModelScope.launch {
            if (!quiet) _state.update { it.copy(loading = true, error = null) }
            try {
                val deal = deals.get(dealId)
                val offers = deals.offers(dealId)
                val agreed = deal.status in AGREED_STATUSES
                val routes = if (agreed) runCatching { deals.routes(dealId) }.getOrDefault(emptyList()) else emptyList()
                _state.update { it.copy(loading = false, deal = deal, offers = offers.sortedBy { o -> o.position ?: Int.MAX_VALUE }, routes = routes) }
                if (agreed) refreshPositions()
            } catch (e: AppError) {
                _state.update { it.copy(loading = false, error = if (it.deal == null) e else null, actionError = if (it.deal != null) e else null) }
            }
        }
    }

    private suspend fun refreshPositions() {
        try {
            val positions = deals.positions(dealId)
            _state.update { it.copy(positions = positions) }
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
