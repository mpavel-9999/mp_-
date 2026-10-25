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
import ru.mpavel.osmmap.feature.provider.SlotActions
import ru.mpavel.osmmap.feature.provider.SlotMap
import ru.mpavel.osmmap.feature.provider.SlotPlaceBlock
import ru.mpavel.osmmap.feature.provider.SlotPriceBlock
import ru.mpavel.osmmap.feature.provider.SlotState
import ru.mpavel.osmmap.feature.provider.SlotSubmitBlock
import ru.mpavel.osmmap.feature.provider.SlotWindowBlock

/** Шаблон A: карта сверху, форма публикации - шторкой снизу (как форма запроса). */
@Composable
fun SlotA(state: SlotState, actions: SlotActions) {
    val corner = Mp.template.shape.corner.dp
    BoxWithConstraints(Modifier.fillMaxSize()) {
        // карта - над шторкой (с заходом под скругление): точка и круги не прячутся
        SlotMap(state, actions, Modifier.fillMaxWidth().height(maxHeight * 0.45f + corner))
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = corner, topEnd = corner),
            shadowElevation = 8.dp,
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(maxHeight * 0.55f),
        ) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).imePadding().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                SlotPlaceBlock(state, actions)
                SlotWindowBlock(state, actions)
                SlotPriceBlock(state, actions)
                SlotSubmitBlock(state, actions)
            }
        }
    }
}
