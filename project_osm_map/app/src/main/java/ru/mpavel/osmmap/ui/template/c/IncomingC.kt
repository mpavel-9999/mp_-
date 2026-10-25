package ru.mpavel.osmmap.ui.template.c

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.mpavel.osmmap.core.theme.Mp
import ru.mpavel.osmmap.feature.provider.IncomingActions
import ru.mpavel.osmmap.feature.provider.IncomingState
import ru.mpavel.osmmap.feature.provider.NoRequests
import ru.mpavel.osmmap.feature.provider.RequestDenseRow

/** Шаблон C "Диспетчер" (эскиз C4): плотные строки с моноширинными цифрами и разделителями. */
@Composable
fun IncomingC(state: IncomingState, actions: IncomingActions) {
    if (state.requests.isEmpty()) {
        NoRequests()
        return
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
        items(state.requests, key = { it.dealId }) { r ->
            RequestDenseRow(r, actions)
            HorizontalDivider(color = Mp.colors.border)
        }
    }
}
