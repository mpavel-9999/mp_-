package ru.mpavel.osmmap.feature.request

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
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
import ru.mpavel.osmmap.data.FormField
import ru.mpavel.osmmap.ui.common.DateTimeField
import ru.mpavel.osmmap.ui.common.ErrorBox
import ru.mpavel.osmmap.ui.common.LoadingBox
import ru.mpavel.osmmap.ui.common.errorMessage
import ru.mpavel.osmmap.ui.map.DefaultCenter
import ru.mpavel.osmmap.ui.map.MapLine
import ru.mpavel.osmmap.ui.map.MapMarker
import ru.mpavel.osmmap.ui.map.MapZone
import ru.mpavel.osmmap.ui.map.MpMap
import ru.mpavel.osmmap.ui.template.a.RequestA
import ru.mpavel.osmmap.ui.template.b.RequestB
import ru.mpavel.osmmap.ui.template.c.RequestC
import kotlin.math.roundToInt

/** Действия экрана запроса - общие для всех раскладок. */
class RequestActions(val vm: RequestViewModel)

/** Шаблон выбирает только раскладку: template.screens.deal_form = sheet | wizard | form_rows. */
@Composable
fun RequestScreen(graph: AppGraph, nav: Nav, serviceId: Long, slotId: Long?) {
    val vm: RequestViewModel = viewModel(key = "request:$serviceId:$slotId") {
        RequestViewModel(graph.services, graph.deals, serviceId, slotId)
    }
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(vm) {
        vm.events.collect { e ->
            when (e) {
                is RequestEvent.Created -> nav.deal(e.dealId, replace = true)
                is RequestEvent.ReviewFirst -> nav.review(e.dealId)
            }
        }
    }
    val error = state.error
    val actions = RequestActions(vm)
    when {
        state.selected == null && error != null -> ErrorBox(error, onRetry = vm::load)
        state.selected == null -> LoadingBox()
        else -> when (Mp.template.screens["deal_form"]) {
            "wizard" -> RequestB(state, actions)
            "form_rows" -> RequestC(state, actions)
            else -> RequestA(state, actions)
        }
    }
}

// ---------------------------------------------------------------- общие части раскладок

/** Карта выбора места: точка услуги или П и Н, зона, маршрут по дорогам; подсказка "нажмите на карту". */
@Composable
fun RequestMap(state: RequestState, actions: RequestActions, modifier: Modifier = Modifier) {
    val icons = LocalIcons.current
    val accent = MaterialTheme.colorScheme.primary.hex()
    val markers = buildList {
        state.point?.takeIf { state.geoMode == "point" }?.let {
            add(MapMarker("point", it, t("ui.point"), svg = icons.cursors["CLIENT"]))
        }
        if (state.geoMode == "route") {
            state.pickup?.let { add(MapMarker("pickup", it, t("ui.pickup"), svg = icons.icons["pin"]?.let { s -> tintSvg(s, accent) })) }
            state.dest?.let { add(MapMarker("dest", it, t("ui.destination"), svg = icons.icons["pin"]?.let { s -> tintSvg(s, Mp.colors.danger.hex()) })) }
        }
    }
    // Центр - один раз (публикация или центр по умолчанию): после нажатия карта не прыгает к новой точке,
    // иначе следующее нажатие (точка назначения) попадает не туда
    val center = remember(state.selected?.serviceId) { state.anchor ?: DefaultCenter }
    Box(modifier) {
        MpMap(
            Modifier.fillMaxSize(),
            center = center,
            zone = state.point?.takeIf { state.showRadius }?.let { MapZone(it, state.radiusM.toDouble()) },
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

/** Такси и доставка: какую точку ставит нажатие - подачу или назначение. */
@Composable
fun RouteTargetToggle(state: RequestState, actions: RequestActions) {
    if (state.geoMode != "route") return
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        RouteTarget.entries.forEachIndexed { i, target ->
            SegmentedButton(
                selected = state.routeTarget == target,
                onClick = { actions.vm.setRouteTarget(target) },
                shape = SegmentedButtonDefaults.itemShape(i, RouteTarget.entries.size),
                label = { Text(if (target == RouteTarget.PICKUP) t("ui.pickup") else t("ui.destination")) },
            )
        }
    }
}

/** Услуга: путь в дереве и выбор варианта (классы такси, виды парковки). */
@Composable
fun ServiceBlock(state: RequestState, actions: RequestActions) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(t("ui.service"), style = MaterialTheme.typography.labelLarge, color = Mp.colors.textMuted)
        Text(state.node?.servicePath ?: state.selected?.serviceName.orEmpty(), style = MaterialTheme.typography.titleMedium)
        if (state.variants.isNotEmpty()) {
            Text(t("ui.variant"), style = MaterialTheme.typography.labelLarge, color = Mp.colors.textMuted)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (v in state.variants) {
                    FilterChip(
                        selected = state.selected?.serviceId == v.serviceId,
                        onClick = { actions.vm.select(v) },
                        label = { Text(v.serviceName) },
                        shape = Mp.shapes.chip,
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                }
            }
        }
    }
}

/** Где и когда: период, радиус зоны, минимальный рейтинг поставщика. */
@Composable
fun WhenBlock(state: RequestState, actions: RequestActions) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(t("ui.where_when"), style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DateTimeField(t("ui.period_from"), state.start, actions.vm::setStart, Modifier.weight(1f))
            DateTimeField(t("ui.period_to"), state.end, actions.vm::setEnd, Modifier.weight(1f))
        }
        if (state.showRadius) {
            Text("${t("ui.radius")}: ${state.radiusM} ${t("ui.unit_m")}", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = state.radiusM.toFloat(),
                onValueChange = { actions.vm.setRadius((it / 50f).roundToInt() * 50) },
                valueRange = 50f..2000f,
            )
        }
        Text(t("ui.provider_rating_min"), style = MaterialTheme.typography.bodyMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (r in listOf<Double?>(null, 3.5, 4.0, 4.5)) {
                FilterChip(
                    selected = state.ratingMin == r,
                    onClick = { actions.vm.setRatingMin(r) },
                    label = { Text(r?.let { "★ $it" } ?: t("ui.any")) },
                    shape = Mp.shapes.chip,
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }
        }
    }
}

/** Поля запроса по attribute_type; новые сервисы добавляются данными - виджет выбирается по типу. */
@Composable
fun FieldsBlock(state: RequestState, actions: RequestActions) {
    val shown = state.fields.filter { it.type !in UNSUPPORTED_TYPES }
    if (shown.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(t("ui.request_fields"), style = MaterialTheme.typography.titleSmall)
        for (f in shown) FieldInput(f, state.values[f.attributeId].orEmpty(), f.attributeId in state.invalid) {
            actions.vm.setField(f.attributeId, it)
        }
    }
}

private val UNSUPPORTED_TYPES = setOf("photo", "card_token", "card_exp")

@Composable
private fun FieldInput(field: FormField, value: String, invalid: Boolean, onChange: (String) -> Unit) {
    val options = field.options.orEmpty()
    if (options.isNotEmpty()) {
        Text(field.label + if (field.required) " *" else "", style = MaterialTheme.typography.bodyMedium,
             color = if (invalid) Mp.colors.danger else Mp.colors.text)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (o in options) {
                FilterChip(selected = value == o.label, onClick = { onChange(o.label) }, label = { Text(o.label) },
                           shape = Mp.shapes.chip, modifier = Modifier.heightIn(min = 48.dp))
            }
        }
        return
    }
    val keyboard = when (field.type) {
        "number", "dim" -> KeyboardOptions(keyboardType = KeyboardType.Decimal)
        "mobile" -> KeyboardOptions(keyboardType = KeyboardType.Phone)
        "car_number" -> KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
        else -> KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
    }
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(field.label + if (field.required) " *" else "") },
        isError = invalid,
        singleLine = field.type != "text",
        keyboardOptions = keyboard,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Прогноз цены и "Моя цена"; для такси - оценка поездки и длина маршрута (п. 1.7-1.9). */
@Composable
fun PriceBlock(state: RequestState, actions: RequestActions) {
    val currency = t("ui.currency")
    val f = state.forecast
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(if (state.priceUnit == "km") t("ui.estimate") else t("ui.forecast"), style = MaterialTheme.typography.titleSmall)
        when {
            state.forecastError != null -> Text(errorMessage(state.forecastError), color = Mp.colors.danger)
            // маршрут без точки назначения - прогноз не считается, подсказать, что сделать
            state.geoMode == "route" && state.dest == null -> Text(t("ui.choose_point"), color = Mp.colors.textMuted)
            f == null -> Text("…", color = Mp.colors.textMuted)
            f.priceMin == null -> Text("—", color = Mp.colors.textMuted)
            else -> Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${f.priceMin.toInt()}–${f.priceMax?.toInt() ?: f.priceMin.toInt()} $currency",
                    fontFamily = Mp.fonts.mono,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                f.routeLengthM?.let {
                    Text("%.1f %s".format(it / 1000, t("ui.unit_km")), fontFamily = Mp.fonts.mono, color = Mp.colors.textMuted)
                }
            }
        }
        OutlinedTextField(
            value = state.myPrice,
            onValueChange = actions.vm::setMyPrice,
            label = { Text("${t("ui.my_price")}, $currency") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** "Найти поставщиков" и ошибка отправки. */
@Composable
fun SubmitBlock(state: RequestState, actions: RequestActions) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        state.submitError?.let { Text(errorMessage(it), color = Mp.colors.danger) }
        if (state.invalid.isNotEmpty()) Text(t("ui.err.required_fields"), color = Mp.colors.danger)
        Button(
            onClick = actions.vm::submit,
            enabled = !state.submitting && !state.loading,
            shape = Mp.shapes.button,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        ) {
            if (state.submitting) {
                CircularProgressIndicator(Modifier.padding(end = 8.dp).size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
            }
            Text(t("ui.find_providers"))
        }
    }
}
