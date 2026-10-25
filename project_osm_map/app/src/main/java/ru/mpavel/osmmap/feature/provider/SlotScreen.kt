package ru.mpavel.osmmap.feature.provider

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.mpavel.osmmap.AppGraph
import ru.mpavel.osmmap.Nav
import ru.mpavel.osmmap.core.i18n.t
import ru.mpavel.osmmap.core.icons.LocalIcons
import ru.mpavel.osmmap.core.icons.hex
import ru.mpavel.osmmap.core.icons.tintSvg
import ru.mpavel.osmmap.core.theme.Mp
import ru.mpavel.osmmap.ui.common.DateTimeField
import ru.mpavel.osmmap.ui.common.ErrorBox
import ru.mpavel.osmmap.ui.common.LoadingBox
import ru.mpavel.osmmap.ui.common.errorMessage
import ru.mpavel.osmmap.ui.map.DefaultCenter
import ru.mpavel.osmmap.ui.map.MapLine
import ru.mpavel.osmmap.ui.map.MapMarker
import ru.mpavel.osmmap.ui.map.MapZone
import ru.mpavel.osmmap.ui.map.MpMap
import ru.mpavel.osmmap.ui.template.a.SlotA
import ru.mpavel.osmmap.ui.template.b.SlotB
import ru.mpavel.osmmap.ui.template.c.SlotC
import kotlin.math.roundToInt

/** Действия экрана публикации - общие для всех раскладок. */
class SlotActions(val vm: SlotViewModel)

/** Шаблон выбирает только раскладку: template.screens.slot_form = sheet | wizard | form_rows. */
@Composable
fun SlotScreen(graph: AppGraph, nav: Nav, serviceId: Long) {
    val vm: SlotViewModel = viewModel(key = "slot:$serviceId") {
        SlotViewModel(graph.session, graph.services, graph.deals, graph.provider, serviceId)
    }
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(vm) { vm.published.collect { nav.back() } }
    val error = state.error
    val actions = SlotActions(vm)
    when {
        state.service == null && error != null -> ErrorBox(error, onRetry = vm::load)
        state.service == null -> LoadingBox()
        else -> when (Mp.template.screens["slot_form"]) {
            "wizard" -> SlotB(state, actions)
            "form_rows" -> SlotC(state, actions)
            else -> SlotA(state, actions)
        }
    }
}

// ---------------------------------------------------------------- общие части раскладок

/** Карта публикации: место или А и Б, круги rA и rB, линия маршрута; подсказка "нажмите на карту". */
@Composable
fun SlotMap(state: SlotState, actions: SlotActions, modifier: Modifier = Modifier) {
    val icons = LocalIcons.current
    val markers = buildList {
        state.pointA?.let {
            add(MapMarker("a", it, if (state.isRoute) t("ui.pickup") else t("ui.slot_point"), svg = icons.cursors["PROVIDER"]))
        }
        state.pointB?.takeIf { state.isRoute }?.let {
            add(MapMarker("b", it, t("ui.destination"), svg = icons.icons["pin"]?.let { s -> tintSvg(s, Mp.colors.danger.hex()) }))
        }
    }
    val zones = if (!state.isRoute) emptyList() else listOfNotNull(
        state.pointA?.let { MapZone(it, state.radiusA.toDouble()) },
        state.pointB?.let { MapZone(it, state.radiusB.toDouble()) },
    )
    // центр - один раз: точка обслуживания или центр по умолчанию (иначе нажатие на Б попадёт не туда)
    val center = remember(state.service?.serviceId) { state.pointA ?: DefaultCenter }
    Box(modifier) {
        MpMap(
            Modifier.fillMaxSize(),
            center = center,
            zones = zones,
            markers = markers,
            lines = if (state.routeLine.isNotEmpty()) listOf(MapLine(state.routeLine)) else emptyList(),
            onMapTap = actions.vm::onMapTap,
        )
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            shape = Mp.shapes.chip,
            modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
        ) {
            Text(
                t("ui.choose_point"),
                style = MaterialTheme.typography.labelMedium,
                color = if (state.geoMissing) Mp.colors.danger else Mp.colors.text,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
    }
}

/** Услуга и место: для маршрута - переключатель А / Б, радиусы rA и rB, "без Б" (любой запрос в rA вокруг А, п. 2.1.1). */
@Composable
fun SlotPlaceBlock(state: SlotState, actions: SlotActions) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(t("ui.service"), style = MaterialTheme.typography.labelLarge, color = Mp.colors.textMuted)
        Text(state.service?.name.orEmpty(), style = MaterialTheme.typography.titleMedium)
        if (!state.isRoute) return@Column
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            SlotTarget.entries.forEachIndexed { i, target ->
                SegmentedButton(
                    selected = state.target == target,
                    onClick = { actions.vm.setTarget(target) },
                    shape = SegmentedButtonDefaults.itemShape(i, SlotTarget.entries.size),
                    label = { Text(if (target == SlotTarget.A) t("ui.pickup") else t("ui.destination")) },
                )
            }
        }
        RadiusSlider("${t("ui.radius_a")}: ${state.radiusA} ${t("ui.unit_m")}", state.radiusA, 100f..1000f, actions.vm::setRadiusA)
        if (state.pointB != null) {
            RadiusSlider("${t("ui.radius_b")}: ${state.radiusB} ${t("ui.unit_m")}", state.radiusB, 200f..3000f, actions.vm::setRadiusB)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    state.routeLengthM?.let { "${t("ui.route")}: %.1f ${t("ui.unit_km")}".format(it / 1000) } ?: t("ui.route_preview"),
                    style = MaterialTheme.typography.bodySmall,
                    color = Mp.colors.textMuted,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = actions.vm::clearB, modifier = Modifier.heightIn(min = 48.dp)) {
                    // без Б - подходит любой запрос с подачей в rA вокруг А (п. 2.1.1)
                    Text("✕ ${t("ui.destination")}")
                }
            }
        }
    }
}

@Composable
private fun RadiusSlider(label: String, value: Int, range: ClosedFloatingPointRange<Float>, onChange: (Int) -> Unit) {
    Text(label, style = MaterialTheme.typography.bodyMedium)
    Slider(value = value.toFloat(), onValueChange = { onChange((it / 50f).roundToInt() * 50) }, valueRange = range)
}

/** Окно доступности: с - по, время по Алматы (Р-4). */
@Composable
fun SlotWindowBlock(state: SlotState, actions: SlotActions) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(t("ui.slot_window"), style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DateTimeField(t("ui.period_from"), state.start, actions.vm::setStart, Modifier.weight(1f))
            DateTimeField(t("ui.period_to"), state.end, actions.vm::setEnd, Modifier.weight(1f))
        }
        Text(t("ui.time_almaty"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Минимальная цена; для такси - порог стоимости поездки (п. 3.3). */
@Composable
fun SlotPriceBlock(state: SlotState, actions: SlotActions) {
    val label = if (state.priceUnit == "km") t("ui.min_fare") else t("ui.price_min")
    OutlinedTextField(
        value = state.price,
        onValueChange = actions.vm::setPrice,
        label = { Text("$label, ${t("ui.currency")}") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}

/** "Опубликовать" и ошибка отправки (окно в прошлом, нет подписки, маршрут не построен). */
@Composable
fun SlotSubmitBlock(state: SlotState, actions: SlotActions) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        state.submitError?.let { Text(errorMessage(it), color = Mp.colors.danger) }
        if (state.geoMissing) Text(t("ui.choose_point"), color = Mp.colors.danger)
        Button(
            onClick = actions.vm::submit,
            enabled = !state.submitting,
            shape = Mp.shapes.button,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        ) {
            if (state.submitting) {
                CircularProgressIndicator(Modifier.padding(end = 8.dp).size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
            }
            Text(t("ui.publish"))
        }
    }
}
