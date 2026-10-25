package ru.mpavel.osmmap.feature.provider

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.mpavel.osmmap.AppGraph
import ru.mpavel.osmmap.Nav
import ru.mpavel.osmmap.core.i18n.t
import ru.mpavel.osmmap.core.net.AppError
import ru.mpavel.osmmap.core.notify.NotificationCenter
import ru.mpavel.osmmap.core.theme.Mp
import ru.mpavel.osmmap.core.time.formatPeriod
import ru.mpavel.osmmap.core.time.parseServerTime
import ru.mpavel.osmmap.data.MyService
import ru.mpavel.osmmap.data.ProviderRepository
import ru.mpavel.osmmap.data.SessionRepository
import ru.mpavel.osmmap.data.Slot
import ru.mpavel.osmmap.ui.common.ErrorBox
import ru.mpavel.osmmap.ui.common.LoadingBox
import ru.mpavel.osmmap.ui.common.errorMessage

/** Раздел поставщика: подключённые сервисы (GET /me), входящие запросы (GET /requests), мои публикации (GET /slots?mine=1). */
data class ProviderState(
    val loading: Boolean = true,
    val error: AppError? = null,
    val services: List<MyService> = emptyList(),
    val incoming: Int = 0,
    val slots: List<Slot> = emptyList(),
    /** Ошибка действия (снятие публикации) - строкой над списком, данные остаются на экране. */
    val actionError: AppError? = null,
    val removing: Long? = null,
)

class ProviderViewModel(
    private val session: SessionRepository,
    private val provider: ProviderRepository,
    center: NotificationCenter,
    lang: Flow<String?>,
) : ViewModel() {

    private val _state = MutableStateFlow(ProviderState())
    val state: StateFlow<ProviderState> = _state

    init {
        // названия сервисов - на языке интерфейса
        viewModelScope.launch { lang.distinctUntilChanged().collect { load() } }
        // новый запрос, согласование, отмена - обновить счётчики и публикации
        viewModelScope.launch { center.events.collect { load(quiet = true) } }
    }

    fun load(quiet: Boolean = false) {
        viewModelScope.launch {
            if (!quiet) _state.update { it.copy(loading = true, error = null) }
            try {
                val me = session.me()
                val incoming = provider.requests()
                val slots = provider.mySlots()
                _state.update {
                    it.copy(loading = false, error = null, services = me.services, incoming = incoming.size, slots = slots)
                }
            } catch (e: AppError) {
                // тихое обновление не затирает уже показанные данные
                _state.update { if (quiet && !it.loading) it else it.copy(loading = false, error = e) }
            }
        }
    }

    /** DELETE /slots/{id}: снятая публикация пропадает с карты клиентов. */
    fun remove(slotId: Long) {
        viewModelScope.launch {
            _state.update { it.copy(removing = slotId, actionError = null) }
            try {
                provider.removeSlot(slotId)
                _state.update { it.copy(removing = null) }
                load(quiet = true)
            } catch (e: AppError) {
                _state.update { it.copy(removing = null, actionError = e) }
                load(quiet = true)
            }
        }
    }
}

/** Общий для всех шаблонов экран раздела; входящие запросы в раскладке шаблона - шаг 7. */
@Composable
fun ProviderScreen(graph: AppGraph, nav: Nav) {
    val vm: ProviderViewModel = viewModel {
        ProviderViewModel(graph.session, graph.provider, graph.notifyCenter, graph.config.lang)
    }
    val state by vm.state.collectAsStateWithLifecycle()
    // возврат с публикации или настроек - свежие данные
    LaunchedEffect(Unit) { vm.load(quiet = true) }
    when {
        state.loading && state.services.isEmpty() -> LoadingBox()
        state.error != null && state.services.isEmpty() -> ErrorBox(state.error!!, onRetry = { vm.load() })
        else -> ProviderContent(state, nav, onRemove = vm::remove)
    }
}

@Composable
private fun ProviderContent(state: ProviderState, nav: Nav, onRemove: (Long) -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        state.actionError?.let { e ->
            item { Text(errorMessage(e), color = MaterialTheme.colorScheme.error) }
        }
        item { SectionTitle(t("ui.my_services")) }
        items(state.services, key = { "s${it.serviceId}" }) { s ->
            ServiceCard(s, onPublish = { nav.slot(s.serviceId) }, onSettings = { nav.serviceSettings(s.serviceId) })
        }
        item {
            SectionTitle(t("ui.incoming"))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (state.incoming == 0) t("ui.no_requests") else "${t("ui.incoming")}: ${state.incoming}",
                    color = if (state.incoming == 0) Mp.colors.textMuted else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                // список - в раскладке шаблона (provider_request); открывается и при пустом списке - ждать новых
                Button(onClick = nav::incoming, shape = Mp.shapes.button, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(t("ui.incoming"))
                }
            }
        }
        item { SectionTitle(t("ui.my_slots")) }
        if (state.slots.isEmpty()) {
            item { Text(t("ui.no_slots"), color = Mp.colors.textMuted) }
        } else {
            items(state.slots, key = { "p${it.slotId}" }) { slot ->
                SlotRow(slot, removing = state.removing == slot.slotId, onRemove = { onRemove(slot.slotId) })
                HorizontalDivider(color = Mp.colors.border)
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
}

/** Сервис поставщика: рейтинг (или "новичок"), автоотклик, "Опубликовать" (кроме услуг без гео) и настройки. */
@Composable
private fun ServiceCard(s: MyService, onPublish: () -> Unit, onSettings: () -> Unit) {
    Surface(shape = Mp.shapes.card, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(s.name, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(
                    if (s.newbie) t("ui.newbie") else s.rating?.let { "★ %.2f".format(it) },
                    if (s.isAuto) t("ui.auto_offer") else null,
                    s.priceMin?.let { "${t("ui.price_min")}: ${it.toInt()} ${t("ui.currency")}" },
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = Mp.colors.textMuted,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (s.geoMode != "none") {
                    Button(onClick = onPublish, shape = Mp.shapes.button, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(t("ui.publish"))
                    }
                }
                OutlinedButton(onClick = onSettings, shape = Mp.shapes.button, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(t("ui.service_settings"), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

/** Публикация: услуга, окно, маршрут и его длина, мин. цена; активную можно снять. */
@Composable
private fun SlotRow(slot: Slot, removing: Boolean, onRemove: () -> Unit) {
    val active = slot.slotStatus == "active"
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                slot.serviceName.orEmpty(),
                style = MaterialTheme.typography.bodyLarge,
                color = if (active) MaterialTheme.colorScheme.onSurface else Mp.colors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                listOfNotNull(
                    formatPeriod(parseServerTime(slot.periodStart), parseServerTime(slot.periodEnd)),
                    slot.routeLengthM?.let { "${t("ui.route")} %.1f ${t("ui.unit_km")}".format(it / 1000) },
                    slot.priceMin?.let { "${t("ui.price_min")}: ${it.toInt()} ${t("ui.currency")}" },
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = Mp.colors.textMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (active) {
            TextButton(onClick = onRemove, enabled = !removing, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(t("ui.remove_slot"))
            }
        } else {
            // closed - окно прошло, cancelled - снята поставщиком
            Text(
                if (slot.slotStatus == "cancelled") t("ui.slot_cancelled") else t("ui.slot_closed"),
                style = MaterialTheme.typography.labelMedium,
                color = Mp.colors.textMuted,
            )
        }
    }
}
