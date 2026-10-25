package ru.mpavel.osmmap.ui.template.b

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.mpavel.osmmap.feature.provider.IncomingActions
import ru.mpavel.osmmap.feature.provider.IncomingState
import ru.mpavel.osmmap.feature.provider.NoRequests
import ru.mpavel.osmmap.feature.provider.RequestCard

/** Шаблон B "Карточки": запросы карточками во всю ширину, без карты. */
@Composable
fun IncomingB(state: IncomingState, actions: IncomingActions) {
    if (state.requests.isEmpty()) {
        NoRequests()
        return
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(state.requests, key = { it.dealId }) { r -> RequestCard(r, actions) }
    }
}
