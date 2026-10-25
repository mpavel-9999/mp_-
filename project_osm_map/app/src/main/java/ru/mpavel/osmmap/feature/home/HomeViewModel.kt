package ru.mpavel.osmmap.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.mpavel.osmmap.core.net.AppError
import ru.mpavel.osmmap.data.DealListItem
import ru.mpavel.osmmap.data.DealRepository
import ru.mpavel.osmmap.data.MapSlot
import ru.mpavel.osmmap.data.ServiceNode
import ru.mpavel.osmmap.data.ServiceRepository
import ru.mpavel.osmmap.ui.map.DefaultCenter

/** Главная (каркас Э3): поиск услуг, категории, активные сделки, свободные места на карте. Содержание экранов - Э4. */
data class HomeState(
    val query: String = "",
    val services: List<ServiceNode> = emptyList(),
    val slots: List<MapSlot> = emptyList(),
    val deals: List<DealListItem> = emptyList(),
    val loading: Boolean = true,
    val error: AppError? = null,
) {
    /** Без запроса - категории верхнего уровня, с запросом - найденные услуги. */
    val shown: List<ServiceNode>
        get() = if (query.isBlank()) services.filter { it.nodeType == "category" } else services.filter { it.nodeType != "system" }
}

class HomeViewModel(
    private val services: ServiceRepository,
    private val deals: DealRepository,
    lang: Flow<String?>,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state
    private var searchJob: Job? = null

    init {
        // Названия услуг и статусов приходят на языке интерфейса: смена языка - загрузить заново
        viewModelScope.launch { lang.distinctUntilChanged().collect { load() } }
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                // coroutineScope: ошибка одного запроса приходит сюда в catch, а не роняет viewModelScope
                coroutineScope {
                    val found = async { services.search(_state.value.query) }
                    val slots = async { deals.mapSlots(DefaultCenter.lat, DefaultCenter.lon, SLOTS_RADIUS_M) }
                    val active = async { deals.list() }
                    _state.update { it.copy(services = found.await(), slots = slots.await(), deals = active.await(), loading = false) }
                }
            } catch (e: AppError) {
                _state.update { it.copy(loading = false, error = e) }
            }
        }
    }

    /** Поиск с задержкой 300 мс, чтобы не дёргать API на каждую букву. */
    fun onQuery(query: String) {
        _state.update { it.copy(query = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            try {
                val found = services.search(query)
                _state.update { it.copy(services = found, error = null) }
            } catch (e: AppError) {
                _state.update { it.copy(error = e) }
            }
        }
    }

    private companion object {
        const val SLOTS_RADIUS_M = 5000
    }
}
