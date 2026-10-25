package ru.mpavel.osmmap.ui.template.b

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import ru.mpavel.osmmap.core.i18n.t
import ru.mpavel.osmmap.core.theme.Mp
import ru.mpavel.osmmap.feature.provider.SlotActions
import ru.mpavel.osmmap.feature.provider.SlotMap
import ru.mpavel.osmmap.feature.provider.SlotPlaceBlock
import ru.mpavel.osmmap.feature.provider.SlotPriceBlock
import ru.mpavel.osmmap.feature.provider.SlotState
import ru.mpavel.osmmap.feature.provider.SlotSubmitBlock
import ru.mpavel.osmmap.feature.provider.SlotWindowBlock

/** Шаблон B "мастер": 1 место (или маршрут) -> 2 окно -> 3 цена и публикация. */
@Composable
fun SlotB(state: SlotState, actions: SlotActions) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    val last = 2
    Column(Modifier.fillMaxSize().imePadding()) {
        LinearProgressIndicator(progress = { (step + 1) / (last + 1f) }, modifier = Modifier.fillMaxWidth())
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("${step + 1} / ${last + 1}", style = MaterialTheme.typography.labelLarge, color = Mp.colors.textMuted)
            when (step) {
                0 -> {
                    SlotPlaceBlock(state, actions)
                    SlotMap(state, actions, Modifier.fillMaxWidth().height(320.dp).clip(Mp.shapes.card))
                }
                1 -> SlotWindowBlock(state, actions)
                else -> {
                    SlotPriceBlock(state, actions)
                    SlotSubmitBlock(state, actions)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (step > 0) {
                OutlinedButton(onClick = { step-- }, shape = Mp.shapes.button, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                    Text(t("ui.back"))
                }
            }
            if (step < last) {
                Button(onClick = { step++ }, shape = Mp.shapes.button, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                    Text(t("ui.next"))
                }
            }
        }
    }
}
