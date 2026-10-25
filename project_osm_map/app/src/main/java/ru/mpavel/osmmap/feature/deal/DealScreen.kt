package ru.mpavel.osmmap.feature.deal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.mpavel.osmmap.AppGraph
import ru.mpavel.osmmap.Nav
import ru.mpavel.osmmap.core.i18n.LocalTexts
import ru.mpavel.osmmap.core.i18n.t
import ru.mpavel.osmmap.core.icons.LocalIcons
import ru.mpavel.osmmap.core.icons.MpIcon
import ru.mpavel.osmmap.core.icons.hex
import ru.mpavel.osmmap.core.icons.tintSvg
import ru.mpavel.osmmap.core.theme.Mp
import ru.mpavel.osmmap.core.time.formatPeriod
import ru.mpavel.osmmap.core.time.parseServerTime
import ru.mpavel.osmmap.data.CancelReason
import ru.mpavel.osmmap.data.Deal
import ru.mpavel.osmmap.data.FieldValue
import ru.mpavel.osmmap.data.Offer
import ru.mpavel.osmmap.ui.common.ErrorBox
import ru.mpavel.osmmap.ui.common.LoadingBox
import ru.mpavel.osmmap.ui.common.errorMessage
import ru.mpavel.osmmap.ui.map.DefaultCenter
import ru.mpavel.osmmap.ui.map.LatLon
import ru.mpavel.osmmap.ui.map.MapMarker
import ru.mpavel.osmmap.ui.map.MapZone
import ru.mpavel.osmmap.ui.map.MpMap
import ru.mpavel.osmmap.ui.template.a.DealA
import ru.mpavel.osmmap.ui.template.a.OffersA
import ru.mpavel.osmmap.ui.template.b.DealB
import ru.mpavel.osmmap.ui.template.b.OffersB
import ru.mpavel.osmmap.ui.template.c.DealC
import ru.mpavel.osmmap.ui.template.c.OffersC

/** Действия экрана сделки - общие для всех раскладок. Кнопки - только из Deal.actions (решает сервер). */
class DealActions(val vm: DealViewModel, val onReview: () -> Unit)

/**
 * Сделка: до согласования - отклики (template.screens.offers = map_sheet | cards_toggle | map_table),
 * после - сама сделка (template.screens.deal = sheet_timeline | cards_timeline | dense_rows).
 * Отмена - общий диалог для всех шаблонов.
 */
@Composable
fun DealScreen(graph: AppGraph, nav: Nav, dealId: Long) {
    val vm: DealViewModel = viewModel(key = "deal:$dealId") { DealViewModel(graph.deals, graph.notifyCenter, dealId) }
    val state by vm.state.collectAsStateWithLifecycle()
    val config by graph.config.config.collectAsStateWithLifecycle()
    LaunchedEffect(vm) { vm.events.collect { if (it is DealEvent.Review) nav.review(it.dealId) } }
    val actions = DealActions(vm, onReview = { nav.review(dealId) })
    val error = state.error
    val deal = state.deal
    val screens = Mp.template.screens
    when {
        deal == null && error != null -> ErrorBox(error, onRetry = { vm.load() })
        deal == null -> LoadingBox()
        state.isOpen -> when (screens["offers"]) {
            "cards_toggle" -> OffersB(state, actions)
            "map_table" -> OffersC(state, actions)
            else -> OffersA(state, actions)
        }
        else -> when (screens["deal"]) {
            "cards_timeline" -> DealB(state, actions)
            "dense_rows" -> DealC(state, actions)
            else -> DealA(state, actions)
        }
    }
    if (state.cancelOpen && deal != null) {
        CancelDialog(deal, config?.cancelReasons.orEmpty(), state, actions)
    }
}

// ---------------------------------------------------------------- общие части раскладок

/** Подпись отклика "псевдоним · ★ 4.4 · 1000 тг"; валюта - из глоссария (в marker_label БД она по-русски). */
@Composable
fun offerLabel(o: Offer): String {
    val currency = t("ui.currency")
    return listOfNotNull(
        o.pseudonym,
        if (o.newbie) t("ui.newbie") else o.rating?.let { "★ %.2f".format(it) },
        o.offerPrice?.let { "${it.toInt()} $currency" },
    ).joinToString(" · ")
}

/** Расстояние: точное после согласования, до него - интервал от смещённой точки (П-4). */
@Composable
fun offerDistance(o: Offer): String {
    val m = t("ui.unit_m")
    return when {
        o.isExact && o.distanceM != null -> "${o.distanceM.toInt()} $m"
        o.distanceBand != null -> "${o.distanceBand.replace("-", "–")} $m"
        o.distanceM != null -> "${o.distanceM.toInt()} $m"
        else -> ""
    }
}

/** Карта сделки: точка и зона клиента, отклики (смещённые) или точное место, П -> Н, курсоры сторон. */
@Composable
fun DealMap(state: DealState, modifier: Modifier = Modifier, attributionAlignment: Alignment = Alignment.BottomEnd) {
    val deal = state.deal ?: return
    val icons = LocalIcons.current
    val center = if (deal.lat != null && deal.lon != null) LatLon(deal.lat, deal.lon) else null
    val markers = buildList {
        if (center != null) {
            add(MapMarker("client", center, t("ui.point"),
                          svg = icons.cursors[if (deal.client?.isTesting == true) "CLIENT_TEST" else "CLIENT"]))
        }
        if (deal.toLat != null && deal.toLon != null) {
            add(MapMarker("dest", LatLon(deal.toLat, deal.toLon), t("ui.destination"),
                          svg = icons.icons["pin"]?.let { tintSvg(it, Mp.colors.danger.hex()) }))
        }
        for (o in state.offers) {
            if (o.showLat == null || o.showLon == null) continue
            add(MapMarker("offer:${o.dealOfferId}", LatLon(o.showLat, o.showLon), offerLabel(o),
                          snippet = if (o.isExact) t("ui.exact_point") else t("ui.approx_point"),
                          svg = icons.cursors[if (o.isTesting) "PROVIDER_TEST" else "PROVIDER"]))
        }
        for (p in state.positions) {
            add(MapMarker("pos:${p.agentId}", LatLon(p.lat, p.lon), p.role, svg = icons.cursors[p.cursor]))
        }
    }
    // масштаб по зоне: парковка 100 м - крупно, такси и доставка - шире
    val zoom = when {
        deal.toLat != null -> 14.0
        (deal.zoneRadiusM ?: 0.0) <= 150.0 -> 17.0
        (deal.zoneRadiusM ?: 0.0) <= 500.0 -> 16.0
        else -> 15.0
    }
    MpMap(
        modifier,
        center = center ?: DefaultCenter,
        zoom = zoom,
        zone = center?.takeIf { deal.zoneRadiusM != null && deal.placeMode == "provider" }?.let { MapZone(it, deal.zoneRadiusM!!) },
        markers = markers,
        attributionAlignment = attributionAlignment,
    )
}

/** Заголовок: услуга, статус, период, цена. */
@Composable
fun DealHeader(deal: Deal) {
    val texts = LocalTexts.current
    val currency = t("ui.currency")
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(deal.serviceName.orEmpty(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusChip(deal.status, deal.statusName ?: texts.status(deal.status))
            Text(formatPeriod(parseServerTime(deal.periodStart), parseServerTime(deal.periodEnd)),
                 fontFamily = Mp.fonts.mono, color = Mp.colors.textMuted, style = MaterialTheme.typography.bodyMedium)
        }
        val price = deal.priceTotal ?: deal.clientPrice
        val forecast = if (deal.priceForecastMin != null) "${deal.priceForecastMin.toInt()}–${deal.priceForecastMax?.toInt()}" else null
        Text(
            listOfNotNull(price?.let { "${t("ui.price")}: ${it.toInt()} $currency" }, forecast?.let { "${t("ui.forecast")}: $it $currency" })
                .joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = Mp.fonts.mono,
        )
    }
}

@Composable
fun StatusChip(code: String, text: String) {
    val color = when (code) {
        "CLOSED" -> Mp.colors.success
        "CANCEL_CLIENT", "CANCEL_PROVIDER", "NOT_CLOSED", "EXPIRED" -> Mp.colors.danger
        "CANCEL_FREE" -> Mp.colors.textMuted
        else -> MaterialTheme.colorScheme.primary
    }
    Surface(color = color.copy(alpha = 0.15f), shape = Mp.shapes.chip) {
        Text(text, color = color, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
    }
}

/** Этапы сделки (алгоритм, раздел 2): пройденные, текущий, впереди; финальный статус - отдельной строкой. */
@Composable
fun StatusTimeline(deal: Deal, horizontal: Boolean = false) {
    val texts = LocalTexts.current
    val stages = listOf("MATCHING", "OFFERS", "AGREED", "CONTACT", "CLOSED")
    val current = stages.indexOf(deal.status).let { if (it < 0) stages.indexOf(lastReached(deal)) else it }
    val items: @Composable (Int, String) -> Unit = { i, code ->
        val done = i <= current
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Surface(
                color = if (done) MaterialTheme.colorScheme.primary else Mp.colors.border,
                shape = Mp.shapes.chip,
                modifier = Modifier.size(12.dp),
            ) {}
            Text(texts.status(code), style = MaterialTheme.typography.bodyMedium,
                 color = if (done) Mp.colors.text else Mp.colors.textMuted,
                 fontWeight = if (i == current) FontWeight.SemiBold else FontWeight.Normal)
        }
    }
    if (horizontal) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) { stages.forEachIndexed { i, c -> items(i, c) } }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { stages.forEachIndexed { i, c -> items(i, c) } }
    }
    if (deal.status !in stages) StatusChip(deal.status, deal.statusName ?: texts.status(deal.status))
}

/** Для отменённой или истёкшей сделки - до какого этапа дошли. */
private fun lastReached(deal: Deal): String = when {
    deal.dateAgreed != null -> "AGREED"
    else -> "MATCHING"
}

/** Вторая сторона: псевдоним и рейтинг; открытые поля по show_stage; поля запроса. */
@Composable
fun PartyBlock(deal: Deal) {
    val other = if (deal.myRole == "client") deal.provider else deal.client
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (other != null) {
            Text(t("ui.counterpart"), style = MaterialTheme.typography.labelLarge, color = Mp.colors.textMuted)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MpIcon("user", tint = MaterialTheme.colorScheme.primary)
                Text(other.pseudonym.orEmpty(), style = MaterialTheme.typography.titleMedium)
                Text(if (other.newbie) t("ui.newbie") else other.rating?.let { "★ %.2f".format(it) }.orEmpty(),
                     fontFamily = Mp.fonts.mono, color = Mp.colors.textMuted)
            }
            FieldList(deal.counterpartFields)
            if (deal.status == "AGREED") {
                Text(t("ui.contacts_near"), style = MaterialTheme.typography.bodySmall, color = Mp.colors.textMuted)
            }
        }
        if (deal.requestFields.isNotEmpty()) {
            Text(t("ui.request_fields"), style = MaterialTheme.typography.labelLarge, color = Mp.colors.textMuted)
            FieldList(deal.requestFields)
        }
    }
}

@Composable
fun FieldList(fields: List<FieldValue>) {
    for (f in fields) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MpIcon(f.iconCode, size = 18.dp, tint = Mp.colors.textMuted)
            Text(f.label.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = Mp.colors.textMuted, modifier = Modifier.weight(1f))
            Text(f.value.orEmpty(), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** Ссылки на маршрут (после согласования): открываются во внешнем приложении карт. */
@Composable
fun RouteLinks(state: DealState) {
    if (!state.can("route") || state.routes.isEmpty()) return
    val uri = LocalUriHandler.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(t("ui.open_route"), style = MaterialTheme.typography.labelLarge, color = Mp.colors.textMuted)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (r in state.routes) {
                OutlinedButton(onClick = { uri.openUri(r.routeUrl) }, shape = Mp.shapes.button, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(r.providerName ?: r.providerCode)
                }
            }
        }
    }
}

/** Действия сделки из Deal.actions: отмена и баллы; закрытие кодом - этап Э6. */
@Composable
fun DealActionsRow(state: DealState, actions: DealActions) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        state.actionError?.let { Text(errorMessage(it), color = Mp.colors.danger) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state.can("review")) {
                Button(onClick = actions.onReview, shape = Mp.shapes.button, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                    Text(t("ui.review_title"), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (state.can("cancel")) {
                OutlinedButton(
                    onClick = { actions.vm.openCancel(true) },
                    enabled = !state.busy,
                    shape = Mp.shapes.button,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                ) { Text(t("ui.cancel_deal"), color = Mp.colors.danger) }
            }
        }
    }
}

/** Карточка отклика: место в очереди, поставщик, расстояние, "Согласовать" (если сервер разрешает). */
@Composable
fun OfferCard(o: Offer, state: DealState, actions: DealActions, dense: Boolean = false) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = if (dense) 4.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("${o.position ?: "–"}", fontFamily = Mp.fonts.mono, color = Mp.colors.textMuted)
        Column(Modifier.weight(1f)) {
            Text(offerLabel(o), style = if (dense) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                 maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(offerDistance(o).takeIf { it.isNotEmpty() }, if (o.isExact) t("ui.exact_point") else t("ui.approx_point"),
                              o.spotNumber?.let { "${t("ui.spot_number")}: $it" }, o.offerMode?.takeIf { it == "auto" }?.let { t("ui.auto_offer") })
                    .joinToString(" · "),
                style = MaterialTheme.typography.bodySmall, color = Mp.colors.textMuted, fontFamily = if (dense) Mp.fonts.mono else null,
            )
        }
        if (state.can("agree") && o.offerStatus == "active") {
            Button(onClick = { actions.vm.agree(o.dealOfferId) }, enabled = !state.busy, shape = Mp.shapes.button,
                   modifier = Modifier.heightIn(min = 48.dp)) { Text(t("ui.agree")) }
        }
    }
}

/** Список откликов или ожидание первого (MATCHING). */
@Composable
fun OffersList(state: DealState, actions: DealActions, dense: Boolean = false) {
    Column {
        state.actionError?.let { Text(errorMessage(it), color = Mp.colors.danger) }
        if (state.offers.isEmpty()) {
            Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                Text(t("ui.waiting_offers"), color = Mp.colors.textMuted)
            }
        } else {
            Text(t("ui.offers_count", "n" to state.offers.size), style = MaterialTheme.typography.titleSmall)
            for (o in state.offers) {
                OfferCard(o, state, actions, dense)
                HorizontalDivider(color = Mp.colors.border)
            }
        }
    }
}

/**
 * Отмена (п. 5.2-5.4): итог заранее из cancel_mode (та же функция БД, что и при отмене);
 * причина обязательна, если отмена со штрафом.
 */
@Composable
private fun CancelDialog(deal: Deal, reasons: List<CancelReason>, state: DealState, actions: DealActions) {
    var reason by remember { mutableStateOf<Long?>(null) }
    var comment by remember { mutableStateOf("") }
    val free = deal.cancelMode == "CANCEL_FREE"
    AlertDialog(
        onDismissRequest = { actions.vm.openCancel(false) },
        title = { Text(t("ui.cancel_deal")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (free) t("ui.cancel_free") else t("ui.cancel_penalty"),
                     color = if (free) Mp.colors.success else Mp.colors.warning, style = MaterialTheme.typography.bodyMedium)
                Text(t("ui.cancel_reason") + if (free) "" else " *", style = MaterialTheme.typography.labelLarge)
                for (r in reasons) {
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 44.dp).selectable(selected = reason == r.id, onClick = { reason = r.id }),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = reason == r.id, onClick = { reason = r.id })
                        Text(r.text)
                    }
                }
                OutlinedTextField(value = comment, onValueChange = { comment = it }, label = { Text(t("ui.review_comment")) },
                                  modifier = Modifier.fillMaxWidth())
                state.actionError?.let { Text(errorMessage(it), color = Mp.colors.danger) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { actions.vm.cancel(reason, comment) },
                enabled = !state.busy && (free || reason != null),
            ) { Text(t("ui.cancel_deal"), color = Mp.colors.danger) }
        },
        dismissButton = { TextButton(onClick = { actions.vm.openCancel(false) }) { Text(t("ui.back")) } },
    )
}
