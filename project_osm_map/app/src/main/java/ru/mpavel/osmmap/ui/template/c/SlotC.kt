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
import ru.mpavel.osmmap.feature.provider.SlotActions
import ru.mpavel.osmmap.feature.provider.SlotMap
import ru.mpavel.osmmap.feature.provider.SlotPlaceBlock
import ru.mpavel.osmmap.feature.provider.SlotPriceBlock
import ru.mpavel.osmmap.feature.provider.SlotState
import ru.mpavel.osmmap.feature.provider.SlotSubmitBlock
import ru.mpavel.osmmap.feature.provider.SlotWindowBlock

/** Шаблон C "Диспетчер": карта сверху, ниже плотные строки формы публикации. */
@Composable
fun SlotC(state: SlotState, actions: SlotActions) {
    Column(Modifier.fillMaxSize().imePadding()) {
        SlotMap(state, actions, Modifier.fillMaxWidth().height(240.dp))
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SlotPlaceBlock(state, actions)
            HorizontalDivider(color = Mp.colors.border)
            SlotWindowBlock(state, actions)
            HorizontalDivider(color = Mp.colors.border)
            SlotPriceBlock(state, actions)
            SlotSubmitBlock(state, actions)
        }
    }
}
