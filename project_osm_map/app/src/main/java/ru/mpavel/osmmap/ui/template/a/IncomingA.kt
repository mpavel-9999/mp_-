package ru.mpavel.osmmap.ui.template.a

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.mpavel.osmmap.core.theme.Mp
import ru.mpavel.osmmap.feature.provider.IncomingActions
import ru.mpavel.osmmap.feature.provider.IncomingMap
import ru.mpavel.osmmap.feature.provider.IncomingState
import ru.mpavel.osmmap.feature.provider.NoRequests
import ru.mpavel.osmmap.feature.provider.RequestCard

/** Шаблон A: карта запросов сверху, список - шторкой; нажатие на точку прокручивает шторку к запросу. */
@Composable
fun IncomingA(state: IncomingState, actions: IncomingActions) {
    val corner = Mp.template.shape.corner.dp
    val list = rememberLazyListState()
    LaunchedEffect(state.selected) {
        val i = state.requests.indexOfFirst { it.dealId == state.selected }
        if (i >= 0) list.animateScrollToItem(i)
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        IncomingMap(state, actions, Modifier.fillMaxWidth().height(maxHeight * 0.4f + corner))
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = corner, topEnd = corner),
            shadowElevation = 8.dp,
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(maxHeight * 0.6f),
        ) {
            if (state.requests.isEmpty()) {
                NoRequests()
            } else {
                LazyColumn(state = list, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.requests, key = { it.dealId }) { r -> RequestCard(r, actions, selected = r.dealId == state.selected) }
                }
            }
        }
    }
}
