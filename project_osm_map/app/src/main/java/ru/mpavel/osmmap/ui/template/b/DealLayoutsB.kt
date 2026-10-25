package ru.mpavel.osmmap.ui.template.b

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import ru.mpavel.osmmap.core.i18n.t
import ru.mpavel.osmmap.core.theme.Mp
import ru.mpavel.osmmap.feature.deal.DealActions
import ru.mpavel.osmmap.feature.deal.DealActionsRow
import ru.mpavel.osmmap.feature.deal.DealHeader
import ru.mpavel.osmmap.feature.deal.DealMap
import ru.mpavel.osmmap.feature.deal.DealState
import ru.mpavel.osmmap.feature.deal.OffersList
import ru.mpavel.osmmap.feature.deal.PartyBlock
import ru.mpavel.osmmap.feature.deal.RouteLinks
import ru.mpavel.osmmap.feature.deal.StatusTimeline

/** Шаблон B, отклики: переключатель "Список / Карта", карточки. */
@Composable
fun OffersB(state: DealState, actions: DealActions) {
    var showMap by rememberSaveable { mutableStateOf(false) }
    val map = showMap && state.hasMap
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        state.deal?.let { DealHeader(it) }
        if (state.hasMap) SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            SegmentedButton(selected = !map, onClick = { showMap = false }, shape = SegmentedButtonDefaults.itemShape(0, 2),
                            label = { Text(t("ui.list")) })
            SegmentedButton(selected = map, onClick = { showMap = true }, shape = SegmentedButtonDefaults.itemShape(1, 2),
                            label = { Text(t("ui.map")) })
        }
        if (map) {
            DealMap(state, Modifier.fillMaxWidth().weight(1f).clip(Mp.shapes.card))
        } else {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                BCard { OffersList(state, actions) }
            }
        }
        DealActionsRow(state, actions)
    }
}

/** Шаблон B, сделка: карточки и таймлайн этапов, карта - карточкой. */
@Composable
fun DealB(state: DealState, actions: DealActions) {
    val deal = state.deal ?: return
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BCard { DealHeader(deal) }
        BCard {
            Text(t("ui.status"), style = MaterialTheme.typography.titleSmall)
            StatusTimeline(deal)
        }
        BCard { PartyBlock(deal) }
        if (state.hasMap) DealMap(state, Modifier.fillMaxWidth().height(220.dp).clip(Mp.shapes.card))
        RouteLinks(state)
        DealActionsRow(state, actions)
    }
}

@Composable
private fun BCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = Mp.shapes.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}
