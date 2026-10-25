package ru.mpavel.osmmap.ui.template.b

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.mpavel.osmmap.core.i18n.t
import ru.mpavel.osmmap.core.icons.MpIcon
import ru.mpavel.osmmap.core.theme.Mp
import ru.mpavel.osmmap.feature.home.DealsSummary
import ru.mpavel.osmmap.feature.home.HomeActions
import ru.mpavel.osmmap.feature.home.HomeState
import ru.mpavel.osmmap.feature.home.PendingReviewsBanner
import ru.mpavel.osmmap.feature.home.SearchField
import ru.mpavel.osmmap.feature.home.TreeHeader
import ru.mpavel.osmmap.feature.home.slotMarkers
import ru.mpavel.osmmap.ui.map.MpMap

/** Шаблон B "Карточки": поиск, плитки дерева услуг, активные сделки, карта мест карточкой. */
@Composable
fun HomeB(state: HomeState, actions: HomeActions) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) { SearchField(state.query, actions.onQuery) }
        if (state.pending.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) { PendingReviewsBanner(state.pending, actions.onReview) }
        }
        item(span = { GridItemSpan(maxLineSpan) }) { DealsSummary(state.deals, actions.onDeals) }
        item(span = { GridItemSpan(maxLineSpan) }) { TreeHeader(state, actions) }
        items(state.shown, key = { it.serviceId }) { s ->
            Card(
                onClick = { actions.onNode(s) },
                shape = Mp.shapes.card,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.aspectRatio(1f),
            ) {
                Column(
                    Modifier.fillMaxSize().padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    MpIcon(s.iconCode, tint = MaterialTheme.colorScheme.primary, size = 32.dp)
                    Text(
                        s.serviceName,
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(t("ui.free_places"), style = MaterialTheme.typography.titleMedium)
                MpMap(
                    Modifier.fillMaxWidth().height(200.dp).clip(Mp.shapes.card),
                    markers = slotMarkers(state.slots),
                    onMarkerClick = actions.onMarker,
                )
            }
        }
    }
}
