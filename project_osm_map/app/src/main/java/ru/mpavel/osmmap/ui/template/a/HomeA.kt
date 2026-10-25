package ru.mpavel.osmmap.ui.template.a

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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

/** Шаблон A "Карта в центре": карта на весь экран, шторка снизу (template.map.sheet_peek). */
@Composable
fun HomeA(state: HomeState, actions: HomeActions) {
    val peek = (Mp.template.map?.sheetPeek ?: 320).dp
    val corner = Mp.template.shape.corner.dp
    Box(Modifier.fillMaxSize()) {
        // Низ карты закрыт шторкой - подпись OSM сверху; нажатие на публикацию - запрос из неё
        MpMap(
            Modifier.fillMaxSize(),
            markers = slotMarkers(state.slots),
            onMarkerClick = actions.onMarker,
            attributionAlignment = Alignment.TopEnd,
        )
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = corner, topEnd = corner),
            shadowElevation = 8.dp,
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().heightIn(max = peek + 80.dp),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SearchField(state.query, actions.onQuery)
                PendingReviewsBanner(state.pending, actions.onReview)
                TreeHeader(state, actions)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.shown, key = { it.serviceId }) { s ->
                        AssistChip(
                            onClick = { actions.onNode(s) },
                            label = { Text(s.serviceName) },
                            leadingIcon = { MpIcon(s.iconCode, tint = MaterialTheme.colorScheme.primary, size = 18.dp) },
                            shape = Mp.shapes.chip,
                            modifier = Modifier.heightIn(min = 48.dp),
                        )
                    }
                }
                DealsSummary(state.deals, actions.onDeals)
            }
        }
    }
}
