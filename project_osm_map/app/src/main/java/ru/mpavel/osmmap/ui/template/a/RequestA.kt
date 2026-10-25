package ru.mpavel.osmmap.ui.template.a

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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

/** Шаблон A: карта на весь экран, форма - шторкой снизу; без гео (онлайн-услуги) - только шторка. */
@Composable
fun RequestA(state: RequestState, actions: RequestActions) {
    val corner = Mp.template.shape.corner.dp
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val hasMap = state.geoMode != "none"
        // Карта - над шторкой (с заходом под скругление): выбранная точка и зона не прячутся
        if (hasMap) RequestMap(state, actions, Modifier.fillMaxWidth().height(maxHeight * 0.4f + corner))
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = corner, topEnd = corner),
            shadowElevation = 8.dp,
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .height(if (hasMap) maxHeight * 0.6f else maxHeight),
        ) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).imePadding().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                ServiceBlock(state, actions)
                RouteTargetToggle(state, actions)
                WhenBlock(state, actions)
                FieldsBlock(state, actions)
                PriceBlock(state, actions)
                SubmitBlock(state, actions)
            }
        }
    }
}
