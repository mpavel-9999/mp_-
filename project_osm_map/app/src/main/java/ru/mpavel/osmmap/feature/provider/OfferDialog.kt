package ru.mpavel.osmmap.feature.provider

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.mpavel.osmmap.AppGraph
import ru.mpavel.osmmap.core.i18n.t
import ru.mpavel.osmmap.core.net.ApiError
import ru.mpavel.osmmap.core.net.AppError
import ru.mpavel.osmmap.core.theme.Mp
import ru.mpavel.osmmap.data.AttrValue
import ru.mpavel.osmmap.data.DealRepository
import ru.mpavel.osmmap.data.FormField
import ru.mpavel.osmmap.data.OfferCreate
import ru.mpavel.osmmap.data.ProviderRepository
import ru.mpavel.osmmap.data.ServiceRepository
import ru.mpavel.osmmap.feature.request.FieldInput
import ru.mpavel.osmmap.feature.request.UNSUPPORTED_TYPES
import ru.mpavel.osmmap.feature.request.invalidFields
import ru.mpavel.osmmap.ui.common.errorMessage

/** Отклик поставщика (п. 3.3-3.5): цена (по умолчанию рекомендованная) и поля отклика attr_scope = offer. */
data class OfferState(
    val loading: Boolean = true,
    val fields: List<FormField> = emptyList(),
    val values: Map<Long, String> = emptyMap(),
    val invalid: Set<Long> = emptySet(),
    val price: String = "",
    val priceInvalid: Boolean = false,
    val sending: Boolean = false,
    val error: AppError? = null,
)

sealed interface OfferEvent {
    data object Sent : OfferEvent
    data class ReviewFirst(val dealId: Long) : OfferEvent
}

/** Тело POST /deals/{id}/offers; null - цена не число. Пустые поля не передаются. */
fun buildOfferCreate(s: OfferState): OfferCreate? {
    val price = s.price.replace(',', '.').trim().toDoubleOrNull() ?: return null
    return OfferCreate(
        price = price,
        attributes = s.fields.mapNotNull { f -> s.values[f.attributeId]?.trim()?.takeIf { it.isNotEmpty() }?.let { AttrValue(f.attributeId, it) } },
    )
}

class OfferViewModel(
    private val services: ServiceRepository,
    private val provider: ProviderRepository,
    private val deals: DealRepository,
    private val dealId: Long,
    serviceId: Long,
    recommended: Double?,
) : ViewModel() {

    private val _state = MutableStateFlow(OfferState(price = recommended?.toInt()?.toString().orEmpty()))
    val state: StateFlow<OfferState> = _state
    private val _events = MutableSharedFlow<OfferEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<OfferEvent> = _events

    init {
        viewModelScope.launch {
            val fields = try {
                services.form(serviceId, "offer").filter { it.type !in UNSUPPORTED_TYPES }
            } catch (e: AppError) {
                _state.update { it.copy(error = e) }
                emptyList()
            }
            _state.update { it.copy(loading = false, fields = fields) }
        }
    }

    fun setPrice(v: String) = _state.update { it.copy(price = v.filter { c -> c.isDigit() || c == '.' || c == ',' }, priceInvalid = false) }
    fun setField(id: Long, v: String) = _state.update { it.copy(values = it.values + (id to v), invalid = it.invalid - id) }

    fun send() {
        val s = _state.value
        val invalid = invalidFields(s.fields, s.values)
        val body = buildOfferCreate(s)
        if (invalid.isNotEmpty() || body == null) {
            _state.update { it.copy(invalid = invalid, priceInvalid = body == null, error = null) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(sending = true, error = null) }
            try {
                provider.offer(dealId, body)
                _state.update { it.copy(sending = false) }
                _events.tryEmit(OfferEvent.Sent)
            } catch (e: AppError) {
                // без баллов за прошлые сделки откликнуться нельзя (п. 5.6) - сразу к оценке
                if (e is ApiError && e.code == "REVIEW_REQUIRED") {
                    runCatching { deals.pendingReviews().firstOrNull() }.getOrNull()?.let { _events.tryEmit(OfferEvent.ReviewFirst(it.dealId)) }
                }
                _state.update { it.copy(sending = false, error = e) }
            }
        }
    }
}

/**
 * Диалог отклика - общий для списка входящих запросов и экрана сделки (кандидат).
 * Условия подбора проверяет БД (триггер на deal_offer): отказ показывается по коду ui.err.*.
 */
@Composable
fun OfferDialog(
    graph: AppGraph,
    dealId: Long,
    serviceId: Long,
    recommended: Double?,
    onSent: () -> Unit,
    onReviewFirst: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val vm: OfferViewModel = viewModel(key = "offer:$dealId") {
        OfferViewModel(graph.services, graph.provider, graph.deals, dealId, serviceId, recommended)
    }
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(vm) {
        vm.events.collect { e ->
            when (e) {
                OfferEvent.Sent -> onSent()
                is OfferEvent.ReviewFirst -> onReviewFirst(e.dealId)
            }
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(t("ui.respond")) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // для такси рекомендованная цена уже посчитана по тарифу: max(порог, цена за км x длина П -> Н), п. 3.3
                recommended?.let {
                    Text("${t("ui.recommended_price")}: ${it.toInt()} ${t("ui.currency")}", color = Mp.colors.textMuted)
                }
                OutlinedTextField(
                    value = state.price,
                    onValueChange = vm::setPrice,
                    label = { Text("${t("ui.price")}, ${t("ui.currency")}") },
                    isError = state.priceInvalid,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                for (f in state.fields) {
                    FieldInput(f, state.values[f.attributeId].orEmpty(), f.attributeId in state.invalid) { vm.setField(f.attributeId, it) }
                }
                if (state.invalid.isNotEmpty()) Text(t("ui.err.required_fields"), color = Mp.colors.danger)
                state.error?.let { Text(errorMessage(it), color = Mp.colors.danger, style = MaterialTheme.typography.bodyMedium) }
            }
        },
        confirmButton = {
            Button(onClick = vm::send, enabled = !state.sending && !state.loading, shape = Mp.shapes.button,
                   modifier = Modifier.heightIn(min = 48.dp)) { Text(t("ui.respond")) }
        },
        dismissButton = { TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text(t("ui.back")) } },
    )
}
