package ru.mpavel.osmmap.feature.provider

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import ru.mpavel.osmmap.AppGraph
import ru.mpavel.osmmap.Nav
import ru.mpavel.osmmap.core.i18n.t
import ru.mpavel.osmmap.core.net.ApiError
import ru.mpavel.osmmap.core.net.AppError
import ru.mpavel.osmmap.core.theme.Mp
import ru.mpavel.osmmap.data.MyService
import ru.mpavel.osmmap.data.ProviderRepository
import ru.mpavel.osmmap.data.SessionRepository
import ru.mpavel.osmmap.ui.common.ErrorBox
import ru.mpavel.osmmap.ui.common.LoadingBox
import ru.mpavel.osmmap.ui.common.errorMessage
import ru.mpavel.osmmap.ui.map.DefaultCenter
import ru.mpavel.osmmap.ui.map.LatLon
import ru.mpavel.osmmap.ui.map.MapMarker
import ru.mpavel.osmmap.ui.map.MapZone
import ru.mpavel.osmmap.ui.map.MpMap

/** Поля формы настроек сервиса - строками, как их ввёл поставщик (п. 2.1-2.9: по ним БД подбирает запросы). */
data class SettingsForm(
    val point: LatLon? = null,
    val radius: String = "",
    val timeFrom: String = "",
    val timeTo: String = "",
    val priceMin: String = "",
    val priceMax: String = "",
    val unitPrice: String = "",
    val ratingMin: String = "",
    val isAuto: Boolean = false,
    val capacity: String = "",
)

/** Итог проверки формы: тело PUT (только изменённые поля) или ошибочные поля и термин ошибки. */
data class SettingsCheck(val body: JsonObject, val invalid: Set<String> = emptySet(), val errorKey: String? = null)

private val TIME = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")

private fun fmt(d: Double?): String = d?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() }.orEmpty()

private fun parseNum(s: String): Double? = s.replace(',', '.').trim().toDoubleOrNull()

/** Форма из текущих настроек (GET /me); время сервер отдаёт "08:00:00" - показываем "08:00". */
fun formOf(s: MyService) = SettingsForm(
    point = if (s.lat != null && s.lon != null) LatLon(s.lat, s.lon) else null,
    radius = fmt(s.zoneRadiusM),
    timeFrom = s.workTimeFrom?.take(5).orEmpty(),
    timeTo = s.workTimeTo?.take(5).orEmpty(),
    priceMin = fmt(s.priceMin),
    priceMax = fmt(s.priceMax),
    unitPrice = fmt(s.unitPriceMin),
    ratingMin = fmt(s.clientRatingMin),
    isAuto = s.isAuto,
    capacity = fmt(s.capacity),
)

/**
 * Тело PUT /me/services/{id}: только изменённые поля; пустое поле, где было значение, - JsonNull (очистить).
 * Проверки до отправки: числа, время ЧЧ:ММ, рейтинг 0-5, мин. цена не больше максимальной. Ключи - как в API.
 */
fun buildServiceUpdate(s: MyService, f: SettingsForm): SettingsCheck {
    val invalid = mutableSetOf<String>()
    val body = buildJsonObject {
        fun num(key: String, text: String, orig: Double?, range: ClosedFloatingPointRange<Double>? = null) {
            if (text.isBlank()) {
                if (orig != null) put(key, JsonNull)
                return
            }
            val v = parseNum(text)
            if (v == null || v < 0 || (range != null && v !in range)) invalid += key
            else if (v != orig) put(key, JsonPrimitive(v))
        }
        fun time(key: String, text: String, orig: String?) {
            val o = orig?.take(5)
            when {
                text.isBlank() -> if (o != null) put(key, JsonNull)
                !TIME.matches(text.trim()) -> invalid += key
                text.trim() != o -> put(key, JsonPrimitive(text.trim()))
            }
        }
        val p = f.point
        if (p != null && (p.lat != s.lat || p.lon != s.lon)) {
            put("lat", JsonPrimitive(p.lat))
            put("lon", JsonPrimitive(p.lon))
        }
        num("zone_radius_m", f.radius, s.zoneRadiusM)
        time("work_time_from", f.timeFrom, s.workTimeFrom)
        time("work_time_to", f.timeTo, s.workTimeTo)
        num("price_min", f.priceMin, s.priceMin)
        num("price_max", f.priceMax, s.priceMax)
        num("unit_price_min", f.unitPrice, s.unitPriceMin)
        num("client_rating_min", f.ratingMin, s.clientRatingMin, 0.0..5.0)
        num("capacity", f.capacity, s.capacity)
        if (f.isAuto != s.isAuto) put("is_auto", JsonPrimitive(f.isAuto))
    }
    if (invalid.isNotEmpty()) return SettingsCheck(body, invalid, "ui.err.check_fields")
    val min = parseNum(f.priceMin)
    val max = parseNum(f.priceMax)
    if (min != null && max != null && min > max) return SettingsCheck(body, setOf("price_min", "price_max"), "ui.err.price_min_max")
    return SettingsCheck(body)
}

data class SettingsState(
    val loading: Boolean = true,
    val error: AppError? = null,
    val service: MyService? = null,
    val form: SettingsForm = SettingsForm(),
    val invalid: Set<String> = emptySet(),
    val saving: Boolean = false,
    /** Итог последнего действия: термин (ui.saved, ui.no_changes, ui.err.*) или ошибка сервера. */
    val messageKey: String? = null,
    val saveError: AppError? = null,
)

class ServiceSettingsViewModel(
    private val session: SessionRepository,
    private val provider: ProviderRepository,
    private val serviceId: Long,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val service = session.me().services.firstOrNull { it.serviceId == serviceId }
                    ?: throw ApiError(404, "NOT_FOUND", "#$serviceId")
                _state.update { it.copy(loading = false, service = service, form = formOf(service)) }
            } catch (e: AppError) {
                _state.update { it.copy(loading = false, error = e) }
            }
        }
    }

    fun edit(change: SettingsForm.() -> SettingsForm) =
        _state.update { it.copy(form = it.form.change(), invalid = emptySet(), messageKey = null, saveError = null) }

    /** Сохранить изменённое; после - перечитать GET /me (значения, которые записала БД). */
    fun save() {
        val s = _state.value
        val service = s.service ?: return
        val check = buildServiceUpdate(service, s.form)
        if (check.errorKey != null) {
            _state.update { it.copy(invalid = check.invalid, messageKey = check.errorKey) }
            return
        }
        if (check.body.isEmpty()) {
            _state.update { it.copy(messageKey = "ui.no_changes") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(saving = true, messageKey = null, saveError = null) }
            try {
                provider.saveService(serviceId, check.body)
                val fresh = session.me().services.firstOrNull { it.serviceId == serviceId } ?: service
                _state.update { it.copy(saving = false, service = fresh, form = formOf(fresh), messageKey = "ui.saved") }
            } catch (e: AppError) {
                _state.update { it.copy(saving = false, saveError = e) }
            }
        }
    }
}

/**
 * Настройки поставщика по сервису (общий экран для всех шаблонов, ТЗ 5.3): точка обслуживания и зона,
 * рабочее время, цены, тариф, мин. рейтинг клиента, автоотклик (п. 3.1), ёмкость.
 */
@Composable
fun ServiceSettingsScreen(graph: AppGraph, nav: Nav, serviceId: Long) {
    val vm: ServiceSettingsViewModel = viewModel(key = "service_settings:$serviceId") {
        ServiceSettingsViewModel(graph.session, graph.provider, serviceId)
    }
    val state by vm.state.collectAsStateWithLifecycle()
    val service = state.service
    val error = state.error
    when {
        service == null && error != null -> ErrorBox(error, onRetry = vm::load)
        service == null -> LoadingBox()
        else -> SettingsContent(service, state, vm)
    }
}

@Composable
private fun SettingsContent(service: MyService, state: SettingsState, vm: ServiceSettingsViewModel) {
    val f = state.form
    val unit = service.priceUnit
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(service.name, style = MaterialTheme.typography.titleLarge)
        Text(if (service.newbie) t("ui.newbie") else service.rating?.let { "★ %.2f".format(it) }.orEmpty(),
             color = Mp.colors.textMuted, fontFamily = Mp.fonts.mono)
        // класс авто - это сам сервис (Эконом, Комфорт); сменить нельзя, только просмотр
        if (unit == "km") ReadOnlyRow(t("ui.class"), service.name)

        if (service.geoMode != "none") {
            Text(t("ui.service_point"), style = MaterialTheme.typography.titleSmall)
            val center = remember(service.serviceId) { f.point ?: DefaultCenter }
            val radius = parseNum(f.radius)
            MpMap(
                Modifier.fillMaxWidth().height(240.dp).clip(Mp.shapes.card),
                center = center,
                zoom = 14.0,
                zone = f.point?.takeIf { radius != null && radius > 0 }?.let { MapZone(it, radius!!) },
                markers = listOfNotNull(f.point?.let { MapMarker("home", it, t("ui.service_point")) }),
                onMapTap = { p -> vm.edit { copy(point = p) } },
            )
            Text(t("ui.choose_point"), style = MaterialTheme.typography.bodySmall, color = Mp.colors.textMuted)
            NumField("zone_radius_m", "${t("ui.radius")}, ${t("ui.unit_m")}", f.radius, state) { v -> vm.edit { copy(radius = v) } }
        }

        Text(t("ui.work_time"), style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextField("work_time_from", t("ui.period_from"), f.timeFrom, state, Modifier.weight(1f)) { v -> vm.edit { copy(timeFrom = v) } }
            TextField("work_time_to", t("ui.period_to"), f.timeTo, state, Modifier.weight(1f)) { v -> vm.edit { copy(timeTo = v) } }
        }

        val cur = t("ui.currency")
        // для такси мин. цена - порог стоимости поездки (Н-7), к нему - цена за км
        NumField("price_min", "${if (unit == "km") t("ui.trip_min") else t("ui.price_min")}, $cur", f.priceMin, state) { v -> vm.edit { copy(priceMin = v) } }
        NumField("price_max", "${t("ui.price_max")}, $cur", f.priceMax, state) { v -> vm.edit { copy(priceMax = v) } }
        if (unit == "km" || unit == "hour") {
            val label = if (unit == "km") t("ui.price_per_km") else t("ui.price_per_hour")
            NumField("unit_price_min", "$label, $cur", f.unitPrice, state) { v -> vm.edit { copy(unitPrice = v) } }
        }
        NumField("client_rating_min", t("ui.client_rating_min"), f.ratingMin, state) { v -> vm.edit { copy(ratingMin = v) } }
        // ёмкость - только у сервисов, где она задана (частная парковка)
        if (service.capacity != null) {
            NumField("capacity", t("ui.capacity"), f.capacity, state) { v -> vm.edit { copy(capacity = v) } }
        }
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(t("ui.auto_offer"), modifier = Modifier.weight(1f))
            Switch(checked = f.isAuto, onCheckedChange = { v -> vm.edit { copy(isAuto = v) } })
        }

        state.messageKey?.let { key ->
            Text(t(key), color = if (key.startsWith("ui.err.")) Mp.colors.danger else Mp.colors.success)
        }
        state.saveError?.let { Text(errorMessage(it), color = Mp.colors.danger) }
        Button(onClick = vm::save, enabled = !state.saving, shape = Mp.shapes.button,
               modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(t("ui.save")) }
    }
}

@Composable
private fun ReadOnlyRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, color = Mp.colors.textMuted, modifier = Modifier.weight(1f))
        Text(value)
    }
}

@Composable
private fun NumField(key: String, label: String, value: String, state: SettingsState, onChange: (String) -> Unit) =
    TextField(key, label, value, state, Modifier.fillMaxWidth(), KeyboardType.Decimal) { onChange(it.filter { c -> c.isDigit() || c == '.' || c == ',' }) }

@Composable
private fun TextField(
    key: String,
    label: String,
    value: String,
    state: SettingsState,
    modifier: Modifier,
    // время ЧЧ:ММ - нужна клавиатура с двоеточием
    keyboard: KeyboardType = KeyboardType.Ascii,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        isError = key in state.invalid,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        modifier = modifier,
    )
}
