package ru.mpavel.osmmap.ui.template.a

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
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

/** Шаблон A, отклики: карта на весь экран (точки откликов смещены), список - шторкой снизу. */
@Composable
fun OffersA(state: DealState, actions: DealActions) {
    MapWithSheet(state, actions, sheetShare = 0.55f) {
        state.deal?.let { DealHeader(it) }
        OffersList(state, actions)
        DealActionsRow(state, actions)
    }
}

/** Шаблон A, сделка: карта с точным местом и курсорами, шторка с этапами, стороной и маршрутом. */
@Composable
fun DealA(state: DealState, actions: DealActions) {
    MapWithSheet(state, actions, sheetShare = 0.6f) {
        val deal = state.deal ?: return@MapWithSheet
        DealHeader(deal)
        StatusTimeline(deal, horizontal = true)
        PartyBlock(deal)
        RouteLinks(state)
        DealActionsRow(state, actions)
    }
}

@Composable
private fun MapWithSheet(state: DealState, actions: DealActions, sheetShare: Float, content: @Composable () -> Unit) {
    val corner: Dp = Mp.template.shape.corner.dp
    // без гео (онлайн-услуга) - только шторка на весь экран
    val share = if (state.hasMap) sheetShare else 1f
    BoxWithConstraints(Modifier.fillMaxSize()) {
        // Карта - над шторкой (с заходом под скругление): центр сделки, зона и отклики остаются видны
        if (state.hasMap) {
            DealMap(state, Modifier.fillMaxWidth().height(maxHeight * (1 - sheetShare) + corner), attributionAlignment = Alignment.TopEnd, onMoveMe = actions.vm::moveMyCursor)
        }
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = corner, topEnd = corner),
            shadowElevation = 8.dp,
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(maxHeight * share),
        ) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) { content() }
        }
    }
}
