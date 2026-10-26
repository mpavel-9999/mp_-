package ru.mpavel.osmmap.ui.template.c

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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

/** Шаблон C, отклики: карта сверху, ниже таблица откликов (цифры моноширинным). */
@Composable
fun OffersC(state: DealState, actions: DealActions) {
    DenseLayout(state, actions) {
        OffersList(state, actions, dense = true)
        DealActionsRow(state, actions)
    }
}

/** Шаблон C, сделка: карта сверху, плотные строки этапов, сторон и полей. */
@Composable
fun DealC(state: DealState, actions: DealActions) {
    val deal = state.deal ?: return
    DenseLayout(state, actions) {
        StatusTimeline(deal, horizontal = true)
        HorizontalDivider(color = Mp.colors.border)
        PartyBlock(deal)
        RouteLinks(state)
        DealActionsRow(state, actions)
    }
}

@Composable
private fun DenseLayout(state: DealState, actions: DealActions, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        if (state.hasMap) DealMap(state, Modifier.fillMaxWidth().height(240.dp), onMoveMe = actions.vm::moveMyCursor)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            state.deal?.let { DealHeader(it) }
            HorizontalDivider(color = Mp.colors.border)
            content()
        }
    }
}
