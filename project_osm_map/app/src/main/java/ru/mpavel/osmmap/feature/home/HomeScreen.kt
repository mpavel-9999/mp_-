package ru.mpavel.osmmap.feature.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
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
import ru.mpavel.osmmap.core.theme.Mp
import ru.mpavel.osmmap.data.DealListItem
import ru.mpavel.osmmap.data.MapSlot
import ru.mpavel.osmmap.data.PendingReview
import ru.mpavel.osmmap.data.ServiceNode
import ru.mpavel.osmmap.ui.common.ErrorBox
import ru.mpavel.osmmap.ui.common.LoadingBox
import ru.mpavel.osmmap.ui.map.LatLon
import ru.mpavel.osmmap.ui.map.MapMarker
import ru.mpavel.osmmap.ui.template.a.HomeA
import ru.mpavel.osmmap.ui.template.b.HomeB
import ru.mpavel.osmmap.ui.template.c.HomeC

/** Действия главной - общие для всех раскладок. */
class HomeActions(
    val onQuery: (String) -> Unit,
    val onRetry: () -> Unit,
    val onNode: (ServiceNode) -> Unit,
    val onUp: () -> Unit,
    val onMarker: (String) -> Unit,
    val onDeals: () -> Unit,
    val onReview: (Long) -> Unit,
)

/** Шаблон выбирает только раскладку: template.screens.home = map_sheet | tiles | dense_list. */
@Composable
fun HomeScreen(graph: AppGraph, nav: Nav) {
    val vm: HomeViewModel = viewModel { HomeViewModel(graph.services, graph.deals, graph.config.lang, graph.notifyCenter.events) }
    val state by vm.state.collectAsStateWithLifecycle()
    // возврат на главную (после запроса, сделки, баллов) - свежие сделки и напоминание о баллах
    LaunchedEffect(Unit) { vm.refreshDeals() }
    val actions = HomeActions(
        onQuery = vm::onQuery,
        onRetry = vm::load,
        onNode = { node -> vm.onNode(node)?.let { nav.request(it) } },
        onUp = vm::onUp,
        onMarker = { id -> vm.slot(id)?.let { nav.request(it.serviceId, it.slotId) } },
        onDeals = nav::deals,
        onReview = nav::review,
    )
    BackHandler(enabled = state.parentId != null && state.query.isBlank(), onBack = vm::onUp)
    val error = state.error
    when {
        state.loading && state.tree.isEmpty() -> LoadingBox()
        error != null && state.tree.isEmpty() -> ErrorBox(error, onRetry = vm::load)
        else -> when (Mp.template.screens["home"]) {
            "tiles" -> HomeB(state, actions)
            "dense_list" -> HomeC(state, actions)
            else -> HomeA(state, actions)
        }
    }
}

// ---------------------------------------------------------------- общие части раскладок

@Composable
fun SearchField(query: String, onQuery: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = query,
        onValueChange = onQuery,
        placeholder = { Text(t("ui.search_hint")) },
        leadingIcon = { MpIcon("pin", tint = Mp.colors.textMuted) },
        singleLine = true,
        shape = Mp.shapes.button,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        modifier = modifier.fillMaxWidth(),
    )
}

/** Заголовок списка: "Категории" или открытый узел с кнопкой "Назад" по дереву. */
@Composable
fun TreeHeader(state: HomeState, actions: HomeActions, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        val parent = state.parent
        Text(
            when {
                state.query.isNotBlank() -> t("ui.popular")
                parent != null -> parent.serviceName
                else -> t("ui.categories")
            },
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (parent != null && state.query.isBlank()) {
            TextButton(onClick = actions.onUp, modifier = Modifier.heightIn(min = 48.dp)) { Text(t("ui.back")) }
        }
    }
}

/** Маркеры публикаций: курсор поставщика (с шестерёнкой у тестовых агентов), подпись по нажатию. */
@Composable
fun slotMarkers(slots: List<MapSlot>): List<MapMarker> {
    val cursors = LocalIcons.current.cursors
    val currency = t("ui.currency")
    return slots.map { s ->
        MapMarker(
            id = "slot:${s.slotId}",
            point = LatLon(s.lat, s.lon),
            title = listOfNotNull(s.pseudonym, s.rating?.let { "★ %.1f".format(it) }).joinToString(" · "),
            snippet = listOfNotNull(s.serviceName, s.priceMin?.let { "${it.toInt()} $currency" }).joinToString(" · "),
            svg = cursors[if (s.isTesting) "PROVIDER_TEST" else "PROVIDER"],
        )
    }
}

/** Строка услуги: пиктограмма, название, путь в дереве; нажатие - открыть узел или запрос. */
@Composable
fun ServiceRow(service: ServiceNode, onClick: () -> Unit, modifier: Modifier = Modifier, dense: Boolean = false) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = if (dense) 4.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MpIcon(service.iconCode, tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f)) {
            Text(service.serviceName, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (service.servicePath.isNotBlank() && service.servicePath != service.serviceName) {
                Text(service.servicePath, style = MaterialTheme.typography.bodySmall, color = Mp.colors.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** Активные сделки: число и последняя; пусто - "Активных сделок нет". Нажатие - "Мои сделки". */
@Composable
fun DealsSummary(deals: List<DealListItem>, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val texts = LocalTexts.current
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = Mp.shapes.card,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(t("ui.active_deals"), style = MaterialTheme.typography.titleSmall)
            val first = deals.firstOrNull()
            if (first == null) {
                Text(t("ui.no_deals"), style = MaterialTheme.typography.bodyMedium, color = Mp.colors.textMuted)
            } else {
                Text(
                    "${first.serviceName.orEmpty()} · ${texts.status(first.status)} · ${t("ui.offers_count", "n" to first.offersActive)}",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (deals.size > 1) {
                    Text("+${deals.size - 1}", style = MaterialTheme.typography.bodySmall, color = Mp.colors.textMuted)
                }
            }
        }
    }
}

/** Напоминание о баллах (п. 5.6): без них нельзя создать запрос. */
@Composable
fun PendingReviewsBanner(pending: List<PendingReview>, onReview: (Long) -> Unit, modifier: Modifier = Modifier) {
    val first = pending.firstOrNull() ?: return
    Surface(
        onClick = { onReview(first.dealId) },
        color = Mp.colors.warning.copy(alpha = 0.15f),
        shape = Mp.shapes.card,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MpIcon("star", tint = Mp.colors.warning)
            Column(Modifier.weight(1f)) {
                Text(t("ui.pending_reviews"), style = MaterialTheme.typography.titleSmall)
                Text(
                    pending.joinToString(" · ") { "#${it.dealId} ${it.counterpart.orEmpty()}" },
                    style = MaterialTheme.typography.bodySmall,
                    color = Mp.colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
