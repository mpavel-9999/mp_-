package ru.mpavel.osmmap.feature.deals

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import ru.mpavel.osmmap.AppGraph
import ru.mpavel.osmmap.Nav
import ru.mpavel.osmmap.core.i18n.LocalTexts
import ru.mpavel.osmmap.core.i18n.t
import ru.mpavel.osmmap.core.net.AppError
import ru.mpavel.osmmap.core.notify.NotificationCenter
import ru.mpavel.osmmap.core.theme.Mp
import ru.mpavel.osmmap.core.time.formatPeriod
import ru.mpavel.osmmap.core.time.parseServerTime
import ru.mpavel.osmmap.data.DealListItem
import ru.mpavel.osmmap.data.DealRepository
import ru.mpavel.osmmap.feature.close.CloseButton
import ru.mpavel.osmmap.feature.close.closeButtons
import ru.mpavel.osmmap.feature.deal.AGREED_STATUSES
import ru.mpavel.osmmap.feature.deal.StatusChip
import ru.mpavel.osmmap.ui.common.ErrorBox
import ru.mpavel.osmmap.ui.common.LoadingBox
import ru.mpavel.osmmap.ui.common.errorMessage

/** Мои сделки: активные и ждущие баллов (GET /deals?role=all&status=active). */
sealed interface DealsState {
    data object Loading : DealsState

    /**
     * [testActions] - действия закрытия в режиме тестирования по согласованным сделкам (test_scan / test_provide):
     * агент выбирает, какую сделку закрыть, прямо из списка (п. 5.1.4). [sent] - кому код уже предоставлен.
     */
    data class Loaded(
        val deals: List<DealListItem>,
        val testActions: Map<Long, List<CloseButton>> = emptyMap(),
        val busy: Boolean = false,
        val sent: Set<Long> = emptySet(),
        val actionError: AppError? = null,
    ) : DealsState

    data class Failed(val error: AppError) : DealsState
}

class DealsViewModel(private val deals: DealRepository, center: NotificationCenter, lang: Flow<String?>) : ViewModel() {

    private val _state = MutableStateFlow<DealsState>(DealsState.Loading)
    val state: StateFlow<DealsState> = _state
    private val _review = MutableSharedFlow<Long>(extraBufferCapacity = 1)

    /** Сделка закрыта "Сканировать QR" - к баллам. */
    val review: SharedFlow<Long> = _review

    init {
        viewModelScope.launch { lang.distinctUntilChanged().collect { load() } }
        // новый отклик или смена статуса - обновить список
        viewModelScope.launch { center.events.collect { load(quiet = true) } }
    }

    fun load(quiet: Boolean = false) {
        viewModelScope.launch {
            if (!quiet) _state.value = DealsState.Loading
            _state.value = try {
                val list = deals.list("all", "active")
                val old = _state.value as? DealsState.Loaded
                DealsState.Loaded(list, testActionsOf(list), sent = old?.sent.orEmpty())
            } catch (e: AppError) {
                if (quiet && _state.value is DealsState.Loaded) _state.value else DealsState.Failed(e)
            }
        }
    }

    /** Действия есть только в карточке сделки: читаем её для согласованных (их у агента единицы). */
    private suspend fun testActionsOf(list: List<DealListItem>): Map<Long, List<CloseButton>> =
        list.filter { it.status in AGREED_STATUSES }.mapNotNull { d ->
            val buttons = runCatching { deals.get(d.dealId).actions }.getOrDefault(emptyList())
                .let(::closeButtons).filter { it == CloseButton.TEST_SCAN || it == CloseButton.TEST_PROVIDE }
            if (buttons.isEmpty()) null else d.dealId to buttons
        }.toMap()

    /** "Сканировать QR" (клиент) или "Предоставить QR" (поставщик) по выбранной сделке. */
    fun testClose(dealId: Long, button: CloseButton) {
        val loaded = _state.value as? DealsState.Loaded ?: return
        viewModelScope.launch {
            _state.value = loaded.copy(busy = true, actionError = null)
            try {
                if (button == CloseButton.TEST_SCAN) {
                    deals.testScan(dealId)
                    _review.tryEmit(dealId)
                } else {
                    deals.testProvide(dealId)
                    _state.value = loaded.copy(sent = loaded.sent + dealId)
                }
                load(quiet = true)
            } catch (e: AppError) {
                _state.value = loaded.copy(actionError = e)
            }
        }
    }
}

@Composable
fun DealsScreen(graph: AppGraph, nav: Nav) {
    val vm: DealsViewModel = viewModel { DealsViewModel(graph.deals, graph.notifyCenter, graph.config.lang) }
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.load(quiet = true) }
    LaunchedEffect(vm) { vm.review.collect { nav.review(it) } }
    when (val s = state) {
        DealsState.Loading -> LoadingBox()
        is DealsState.Failed -> ErrorBox(s.error, onRetry = { vm.load() })
        is DealsState.Loaded -> if (s.deals.isEmpty()) {
            Text(t("ui.no_deals"), color = Mp.colors.textMuted, modifier = Modifier.padding(16.dp))
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 8.dp)) {
                s.actionError?.let { e ->
                    item { Text(errorMessage(e), color = Mp.colors.danger, modifier = Modifier.padding(horizontal = 16.dp)) }
                }
                items(s.deals, key = { it.dealId }) { d ->
                    DealRow(d, onClick = { if (d.reviewPending) nav.review(d.dealId) else nav.deal(d.dealId) })
                    s.testActions[d.dealId]?.let { buttons ->
                        Row(Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            for (b in buttons) {
                                if (b == CloseButton.TEST_PROVIDE && d.dealId in s.sent) {
                                    Text(t("ui.code_sent"), color = Mp.colors.success, style = MaterialTheme.typography.bodySmall)
                                } else {
                                    OutlinedButton(onClick = { vm.testClose(d.dealId, b) }, enabled = !s.busy, shape = Mp.shapes.button,
                                                   modifier = Modifier.heightIn(min = 48.dp)) { Text(t(b.textKey)) }
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = Mp.colors.border)
                }
            }
        }
    }
}

@Composable
private fun DealRow(d: DealListItem, onClick: () -> Unit) {
    val texts = LocalTexts.current
    val currency = t("ui.currency")
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("#${d.dealId}", fontFamily = Mp.fonts.mono, color = Mp.colors.textMuted)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(d.serviceName.orEmpty(), style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(
                    if (d.myRole == "client") t("ui.role_client") else t("ui.role_provider"),
                    formatPeriod(parseServerTime(d.periodStart), parseServerTime(d.periodEnd)),
                    d.counterpart,
                    (d.priceTotal ?: d.clientPrice)?.let { "${it.toInt()} $currency" },
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = Mp.colors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            StatusChip(d.status, d.statusName ?: texts.status(d.status))
            if (d.reviewPending) Text(t("ui.review_title"), style = MaterialTheme.typography.labelSmall, color = Mp.colors.warning)
            else if (d.offersActive > 0) Text(t("ui.offers_count", "n" to d.offersActive), style = MaterialTheme.typography.labelSmall)
        }
    }
}
