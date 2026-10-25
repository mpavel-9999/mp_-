package ru.mpavel.osmmap.feature.provider

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.mpavel.osmmap.AppGraph
import ru.mpavel.osmmap.Nav
import ru.mpavel.osmmap.core.i18n.t
import ru.mpavel.osmmap.core.icons.LocalIcons
import ru.mpavel.osmmap.core.icons.hex
import ru.mpavel.osmmap.core.icons.tintSvg
import ru.mpavel.osmmap.core.net.AppError
import ru.mpavel.osmmap.core.notify.NotificationCenter
import ru.mpavel.osmmap.core.theme.Mp
import ru.mpavel.osmmap.core.time.formatPeriod
import ru.mpavel.osmmap.core.time.parseServerTime
import ru.mpavel.osmmap.data.IncomingRequest
import ru.mpavel.osmmap.data.ProviderRepository
import ru.mpavel.osmmap.ui.common.ErrorBox
import ru.mpavel.osmmap.ui.common.LoadingBox
import ru.mpavel.osmmap.ui.map.DefaultCenter
import ru.mpavel.osmmap.ui.map.LatLon
import ru.mpavel.osmmap.ui.map.MapMarker
import ru.mpavel.osmmap.ui.map.MapZone
import ru.mpavel.osmmap.ui.map.MpMap
import ru.mpavel.osmmap.ui.template.a.IncomingA
import ru.mpavel.osmmap.ui.template.b.IncomingB
import ru.mpavel.osmmap.ui.template.c.IncomingC

/** Входящие запросы (GET /requests): подходящие по provider_can_offer, такси - по пути; с откликом - пока сделка открыта. */
data class IncomingState(
    val loading: Boolean = true,
    val error: AppError? = null,
    val requests: List<IncomingRequest> = emptyList(),
    /** Выбранный на карте или в списке запрос (шаблон A - подсветка). */
    val selected: Long? = null,
    /** Запрос, на который открыт диалог отклика. */
    val offerFor: IncomingRequest? = null,
)

/** Показать только не пропущенные; запрос с моим откликом не скрывается, даже если его пропускали раньше. */
fun visibleRequests(all: List<IncomingRequest>, skipped: Set<Long>): List<IncomingRequest> =
    all.filter { it.dealId !in skipped || it.myOffer != null }

class IncomingViewModel(
    private val provider: ProviderRepository,
    private val skipped: SkippedRequests,
    center: NotificationCenter,
    lang: Flow<String?>,
) : ViewModel() {

    private val all = MutableStateFlow<List<IncomingRequest>>(emptyList())
    private val _state = MutableStateFlow(IncomingState())
    val state: StateFlow<IncomingState> = _state

    init {
        viewModelScope.launch { lang.distinctUntilChanged().collect { load() } }
        // новый запрос (NEW_REQUEST), согласование другим, отмена клиентом - перечитать список
        viewModelScope.launch { center.events.collect { load(quiet = true) } }
        viewModelScope.launch {
            combine(all, skipped.ids) { list, ids -> visibleRequests(list, ids) }.collect { v ->
                _state.update { it.copy(requests = v) }
            }
        }
    }

    fun load(quiet: Boolean = false) {
        viewModelScope.launch {
            if (!quiet) _state.update { it.copy(loading = true, error = null) }
            try {
                all.value = provider.requests()
                _state.update { it.copy(loading = false, error = null) }
            } catch (e: AppError) {
                _state.update { if (quiet && !it.loading) it else it.copy(loading = false, error = e) }
            }
        }
    }

    fun select(dealId: Long?) = _state.update { it.copy(selected = dealId) }
    fun skip(dealId: Long) = skipped.skip(dealId)
    fun openOffer(r: IncomingRequest) = _state.update { it.copy(offerFor = r, selected = r.dealId) }
    fun closeOffer(sent: Boolean) {
        _state.update { it.copy(offerFor = null) }
        if (sent) load(quiet = true)
    }
}

/** Действия экрана входящих - общие для раскладок. */
class IncomingActions(val vm: IncomingViewModel, val nav: Nav)

/** Шаблон выбирает только раскладку: template.screens.provider_request = sheet | cards | dense. */
@Composable
fun IncomingScreen(graph: AppGraph, nav: Nav) {
    val vm: IncomingViewModel = viewModel {
        IncomingViewModel(graph.provider, graph.skipped, graph.notifyCenter, graph.config.lang)
    }
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.load(quiet = true) }
    val actions = IncomingActions(vm, nav)
    when {
        state.loading && state.requests.isEmpty() -> LoadingBox()
        state.error != null && state.requests.isEmpty() -> ErrorBox(state.error!!, onRetry = { vm.load() })
        else -> when (Mp.template.screens["provider_request"]) {
            "cards" -> IncomingB(state, actions)
            "dense" -> IncomingC(state, actions)
            else -> IncomingA(state, actions)
        }
    }
    state.offerFor?.let { r ->
        OfferDialog(
            graph, r.dealId, r.serviceId, r.recommendedPrice,
            onSent = { vm.closeOffer(sent = true) },
            onReviewFirst = { id -> vm.closeOffer(sent = false); nav.review(id) },
            onDismiss = { vm.closeOffer(sent = false) },
        )
    }
}

// ---------------------------------------------------------------- общие части раскладок

/** Пусто: ждём новые запросы (список обновится по уведомлению NEW_REQUEST). */
@Composable
fun NoRequests() {
    Text(t("ui.no_requests"), color = Mp.colors.textMuted, modifier = Modifier.padding(16.dp))
}

/** Место запроса: зона клиента или П -> Н и длина поездки; для такси - пометка "по пути". */
@Composable
fun requestPlace(r: IncomingRequest): String = when {
    r.geoMode == "route" -> listOfNotNull(
        "${t("ui.pickup")} → ${t("ui.destination")}",
        r.routeLengthM?.let { "%.1f %s".format(it / 1000, t("ui.unit_km")) },
        t("ui.on_the_way"),
    ).joinToString(" · ")
    r.geoMode == "none" -> ""
    r.zoneRadiusM != null && r.placeMode == "provider" -> "${t("ui.radius")}: ${r.zoneRadiusM.toInt()} ${t("ui.unit_m")}"
    else -> t("ui.point")
}

/** Цены: цена клиента, прогноз, рекомендованная (п. 3.1, 3.3). */
@Composable
fun requestPrices(r: IncomingRequest): String {
    val cur = t("ui.currency")
    return listOfNotNull(
        r.clientPrice?.let { "${t("ui.price")}: ${it.toInt()} $cur" },
        r.priceForecastMin?.let { min -> "${t("ui.forecast")}: ${min.toInt()}–${(r.priceForecastMax ?: min).toInt()}" },
    ).joinToString(" · ")
}

/** Клиент: рейтинг по сервису или "новичок" (В-4), число откликов. */
@Composable
fun requestClient(r: IncomingRequest): String = listOfNotNull(
    t("ui.role_client") + " " + if (r.clientNewbie) t("ui.newbie") else r.clientRating?.let { "★ %.2f".format(it) }.orEmpty(),
    r.offersActive.takeIf { it > 0 }?.let { t("ui.offers_count", "n" to it) },
).joinToString(" · ")

/** Кнопки: "Откликнуться" / "Пропустить"; после отклика - "Отклик отправлен" и цена. */
@Composable
fun RequestActionsRow(r: IncomingRequest, actions: IncomingActions, dense: Boolean = false) {
    val mine = r.myOffer
    if (mine != null) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${t("ui.offer_sent")}: ${mine.price.toInt()} ${t("ui.currency")}", color = Mp.colors.success,
                 style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = { actions.nav.deal(r.dealId) }, modifier = Modifier.heightIn(min = 48.dp)) { Text(t("ui.deal")) }
        }
        return
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Button(onClick = { actions.vm.openOffer(r) }, shape = Mp.shapes.button, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(t("ui.respond") + (r.recommendedPrice?.let { " · ${it.toInt()} ${t("ui.currency")}" } ?: ""))
        }
        if (dense) {
            TextButton(onClick = { actions.vm.skip(r.dealId) }, modifier = Modifier.heightIn(min = 48.dp)) { Text(t("ui.skip")) }
        } else {
            OutlinedButton(onClick = { actions.vm.skip(r.dealId) }, shape = Mp.shapes.button, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(t("ui.skip"))
            }
        }
    }
}

/** Карточка запроса (A - в шторке, B - карточки). */
@Composable
fun RequestCard(r: IncomingRequest, actions: IncomingActions, selected: Boolean = false) {
    Surface(
        shape = Mp.shapes.card,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
        onClick = { actions.vm.select(r.dealId) },
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(r.serviceName.orEmpty(), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold,
                     maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text(formatPeriod(parseServerTime(r.periodStart), parseServerTime(r.periodEnd)), fontFamily = Mp.fonts.mono,
                     style = MaterialTheme.typography.bodyMedium)
            }
            for (line in listOf(requestPlace(r), requestPrices(r), requestClient(r)).filter { it.isNotEmpty() }) {
                Text(line, style = MaterialTheme.typography.bodySmall, color = Mp.colors.textMuted)
            }
            RequestActionsRow(r, actions)
        }
    }
}

/** Плотная строка (шаблон C, как эскиз C4): моноширинные цифры, кнопки текстом. */
@Composable
fun RequestDenseRow(r: IncomingRequest, actions: IncomingActions) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("#${r.dealId}", fontFamily = Mp.fonts.mono, color = Mp.colors.textMuted)
            Text(r.serviceName.orEmpty(), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                 modifier = Modifier.weight(1f))
            Text(formatPeriod(parseServerTime(r.periodStart), parseServerTime(r.periodEnd)), fontFamily = Mp.fonts.mono,
                 style = MaterialTheme.typography.bodySmall)
        }
        Text(listOf(requestPlace(r), requestPrices(r), requestClient(r)).filter { it.isNotEmpty() }.joinToString(" · "),
             fontFamily = Mp.fonts.mono, style = MaterialTheme.typography.bodySmall, color = Mp.colors.textMuted)
        RequestActionsRow(r, actions, dense = true)
    }
}

/** Карта входящих (шаблон A): точки запросов, зоны клиентов, назначения такси; нажатие выбирает запрос. */
@Composable
fun IncomingMap(state: IncomingState, actions: IncomingActions, modifier: Modifier = Modifier) {
    val icons = LocalIcons.current
    val danger = Mp.colors.danger.hex()
    val markers = buildList {
        for (r in state.requests) {
            if (r.lat == null || r.lon == null) continue
            add(MapMarker("req:${r.dealId}", LatLon(r.lat, r.lon), r.serviceName.orEmpty(), snippet = requestPrices(r),
                          svg = icons.cursors["CLIENT"]))
            if (r.toLat != null && r.toLon != null) {
                add(MapMarker("dst:${r.dealId}", LatLon(r.toLat, r.toLon), t("ui.destination"),
                              svg = icons.icons["pin"]?.let { tintSvg(it, danger) }))
            }
        }
    }
    val zones = state.requests.mapNotNull { r ->
        if (r.lat == null || r.lon == null || r.zoneRadiusM == null || r.placeMode != "provider") null
        else MapZone(LatLon(r.lat, r.lon), r.zoneRadiusM)
    }
    // центр - выбранный запрос (нажатие на карточку), иначе первый видимый; MpMap сдвигает карту только при смене центра
    val focus = state.requests.firstOrNull { it.dealId == state.selected && it.lat != null } ?: state.requests.firstOrNull { it.lat != null }
    val center = focus?.let { LatLon(it.lat!!, it.lon!!) } ?: DefaultCenter
    MpMap(
        modifier,
        center = center,
        zoom = 14.0,
        zones = zones,
        markers = markers,
        onMarkerClick = { id -> id.substringAfter(':').toLongOrNull()?.let(actions.vm::select) },
    )
}
