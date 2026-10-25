package ru.mpavel.osmmap.ui.template.c

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.mpavel.osmmap.core.theme.Mp
import ru.mpavel.osmmap.feature.request.FieldsBlock
import ru.mpavel.osmmap.feature.request.PriceBlock
import ru.mpavel.osmmap.feature.request.RequestActions
import ru.mpavel.osmmap.feature.request.RequestMap
import ru.mpavel.osmmap.feature.request.RequestState
import ru.mpavel.osmmap.feature.request.RouteTargetToggle
import ru.mpavel.osmmap.feature.request.ServiceBlock
import ru.mpavel.osmmap.feature.request.SubmitBlock
import ru.mpavel.osmmap.feature.request.WhenBlock

/** Шаблон C "Диспетчер": карта сверху, ниже плотные строки формы. */
@Composable
fun RequestC(state: RequestState, actions: RequestActions) {
    Column(Modifier.fillMaxSize().imePadding()) {
        if (state.geoMode != "none") RequestMap(state, actions, Modifier.fillMaxWidth().height(240.dp))
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ServiceBlock(state, actions)
            RouteTargetToggle(state, actions)
            HorizontalDivider(color = Mp.colors.border)
            WhenBlock(state, actions)
            HorizontalDivider(color = Mp.colors.border)
            FieldsBlock(state, actions)
            PriceBlock(state, actions)
            SubmitBlock(state, actions)
        }
    }
}
