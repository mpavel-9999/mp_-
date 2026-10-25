package ru.mpavel.osmmap.ui.template.c

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.mpavel.osmmap.core.i18n.t
import ru.mpavel.osmmap.core.theme.Mp
import ru.mpavel.osmmap.feature.home.DealsSummary
import ru.mpavel.osmmap.feature.home.HomeActions
import ru.mpavel.osmmap.feature.home.HomeState
import ru.mpavel.osmmap.feature.home.SearchField
import ru.mpavel.osmmap.feature.home.ServiceRow
import ru.mpavel.osmmap.feature.home.slotMarkers
import ru.mpavel.osmmap.ui.map.MpMap

/** Шаблон C "Диспетчер": карта сверху, плотный список, цифры моноширинным шрифтом. */
@Composable
fun HomeC(state: HomeState, actions: HomeActions) {
    Column(Modifier.fillMaxSize()) {
        MpMap(Modifier.fillMaxWidth().height(220.dp), markers = slotMarkers(state.slots))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(t("ui.free_places"), style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            Text(state.slots.size.toString(), fontFamily = Mp.fonts.mono, style = MaterialTheme.typography.labelLarge)
        }
        LazyColumn(Modifier.fillMaxSize()) {
            item { SearchField(state.query, actions.onQuery, Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) }
            item { DealsSummary(state.deals, Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) }
            items(state.shown, key = { it.serviceId }) { s ->
                ServiceRow(s, dense = true)
                HorizontalDivider(color = Mp.colors.border)
            }
        }
    }
}
