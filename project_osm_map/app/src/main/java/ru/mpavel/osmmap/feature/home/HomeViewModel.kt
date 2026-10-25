package ru.mpavel.osmmap.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.mpavel.osmmap.core.net.AppError
import ru.mpavel.osmmap.data.DealListItem
import ru.mpavel.osmmap.data.DealRepository
import ru.mpavel.osmmap.data.MapSlot
import ru.mpavel.osmmap.data.Notification
import ru.mpavel.osmmap.data.PendingReview
import ru.mpavel.osmmap.data.ServiceNode
import ru.mpavel.osmmap.data.ServiceRepository
import ru.mpavel.osmmap.ui.map.DefaultCenter

/** Главная: дерево услуг и поиск, свободные места на карте, мои сделки, напоминание о баллах (п. 1.1, 5.6). */
data class HomeState(
    val query: String = "",
    /** Всё дерево услуг (для перехода категория -> сервис -> вариант). */
    val tree: List<ServiceNode> = emptyList(),
    /** Результат поиска по запросу. */
    val found: List<ServiceNode> = emptyList(),
    /** Открытый узел дерева; null - категории верхнего уровня. */
    val parentId: Long? = null,
    val slots: List<MapSlot> = emptyList(),
    val deals: List<DealListItem> = emptyList(),
    val pending: List<PendingReview> = emptyList(),
    val loading: Boolean = true,
    val error: AppError? = null,
) {
    val parent: ServiceNode? get() = parentId?.let { id -> tree.firstOrNull { it.serviceId == id } }

    /** Без запроса - дети открытого узла (или категории), с запросом - найденные услуги. */
    val shown: List<ServiceNode>
        get() = if (query.isBlank()) {
            tree.filter { it.parentServiceId == parentId && it.nodeType != "system" }
        } else {
            found.filter { it.nodeType != "system" }
        }

    fun hasChildren(node: ServiceNode): Boolean = tree.any { it.parentServiceId == node.serviceId }
}

class HomeViewModel(
    private val services: ServiceRepository,
    private val deals: DealRepository,
    lang: Flow<String?>,
    notifications: Flow<Notification>,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state
    private var searchJob: Job? = null

    init {
        // Названия услуг и статусов приходят на языке интерфейса: смена языка - загрузить заново
        viewModelScope.launch { lang.distinctUntilChanged().collect { load() } }
        // Новый отклик, отмена, баллы - обновить сделки и напоминание о баллах
        viewModelScope.launch { notifications.collect { refreshDeals() } }
    }

    /** Мои сделки и баллы - без перезагрузки дерева услуг. */
    fun refreshDeals() {
        viewModelScope.launch {
            try {
                coroutineScope {
                    val active = async { deals.list() }
                    val pending = async { deals.pendingReviews() }
                    _state.update { it.copy(deals = active.await(), pending = pending.await()) }
                }
            } catch (_: AppError) {
                // устаревшие данные лучше пустого экрана; ошибка покажется при следующей полной загрузке
            }
        }
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                // coroutineScope: ошибка одного запроса приходит сюда в catch, а не роняет viewModelScope
                coroutineScope {
                    val tree = async { services.search("") }
                    val slots = async { deals.mapSlots(DefaultCenter.lat, DefaultCenter.lon, SLOTS_RADIUS_M) }
                    val active = async { deals.list() }
                    val pending = async { deals.pendingReviews() }
                    _state.update {
                        it.copy(tree = tree.await(), slots = slots.await(), deals = active.await(),
                                pending = pending.await(), loading = false)
                    }
                }
                if (_state.value.query.isNotBlank()) onQuery(_state.value.query)
            } catch (e: AppError) {
                _state.update { it.copy(loading = false, error = e) }
            }
        }
    }

    /** Поиск с задержкой 300 мс, чтобы не дёргать API на каждую букву. */
    fun onQuery(query: String) {
        _state.update { it.copy(query = query) }
        searchJob?.cancel()
        if (query.isBlank()) return
        searchJob = viewModelScope.launch {
            delay(300)
            try {
                val found = services.search(query)
                _state.update { it.copy(found = found, error = null) }
            } catch (e: AppError) {
                _state.update { it.copy(error = e) }
            }
        }
    }

    /** Узел с детьми открывается; лист (обычно вариант) - возвращается для запроса (п. 1.1). */
    fun onNode(node: ServiceNode): Long? {
        if (_state.value.hasChildren(node)) {
            _state.update { it.copy(parentId = node.serviceId, query = "") }
            return null
        }
        return node.serviceId
    }

    /** На уровень вверх по дереву. */
    fun onUp() = _state.update { s -> s.copy(parentId = s.parent?.parentServiceId) }

    fun slot(markerId: String): MapSlot? =
        markerId.removePrefix("slot:").toLongOrNull()?.let { id -> _state.value.slots.firstOrNull { it.slotId == id } }

    private companion object {
        const val SLOTS_RADIUS_M = 5000
    }
}
