package ru.mpavel.osmmap.feature.close

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.mpavel.osmmap.AppGraph
import ru.mpavel.osmmap.Nav
import ru.mpavel.osmmap.core.i18n.t
import ru.mpavel.osmmap.core.net.AppError
import ru.mpavel.osmmap.core.notify.NotificationCenter
import ru.mpavel.osmmap.core.theme.Mp
import ru.mpavel.osmmap.data.Deal
import ru.mpavel.osmmap.data.DealRepository
import ru.mpavel.osmmap.ui.common.ErrorBox
import ru.mpavel.osmmap.ui.common.LoadingBox
import ru.mpavel.osmmap.ui.common.errorMessage

data class CloseConfirmState(
    val loading: Boolean = true,
    val error: AppError? = null,
    val deal: Deal? = null,
    val code: String = "",
    val busy: Boolean = false,
    val actionError: AppError? = null,
    /** Сканер Google недоступен (нет сервисов Google Play или модуль не скачан) - подсказка про ввод цифр. */
    val scannerFailed: Boolean = false,
    /** Сделка закрыта - экран ведёт к баллам. */
    val closed: Boolean = false,
) {
    fun can(action: String): Boolean = deal?.actions?.contains(action) == true

    /** Агент-клиент в режиме тестирования: вместо камеры - "Сканировать QR" (п. 5.1.4). */
    val iAmTesting: Boolean get() = deal?.client?.isTesting == true
}

/**
 * Подтверждение закрытия клиентом (п. 5.1.3): цифры или строка QR уходят на сервер как есть (POST /close),
 * проверяет код только сервер. [initialCode] - код из уведомления CLOSE_CODE ("Предоставить QR").
 */
class CloseConfirmViewModel(
    private val deals: DealRepository,
    private val center: NotificationCenter,
    private val dealId: Long,
    initialCode: String,
) : ViewModel() {

    private val _state = MutableStateFlow(CloseConfirmState(code = initialCode))
    val state: StateFlow<CloseConfirmState> = _state

    init {
        center.fast(true)
        load()
        // сближение (NEAR) открывает ввод кода - перечитать сделку; "Предоставить QR" (CLOSE_CODE) - подставить код,
        // даже если экран уже открыт (переход на тот же экран его не пересоздаёт)
        viewModelScope.launch {
            center.events.filter { it.dealId == dealId }.collect { n ->
                closeCodeFrom(n)?.let(::setCode)
                load()
            }
        }
    }

    override fun onCleared() {
        center.fast(false)
    }

    fun load() {
        viewModelScope.launch {
            try {
                val deal = deals.get(dealId)
                _state.update { it.copy(loading = false, error = null, deal = deal, closed = deal.status == "CLOSED") }
            } catch (e: AppError) {
                _state.update { it.copy(loading = false, error = if (it.deal == null) e else null) }
            }
        }
    }

    fun setCode(text: String) = _state.update { it.copy(code = text.filter(Char::isDigit).take(MAX_DIGITS), actionError = null) }

    /** Цифры с экрана поставщика или строка QR "mpclose:<сделка>:<код>" из сканера. */
    fun submit(raw: String = _state.value.code) = act { deals.close(dealId, raw.trim()) }

    /** "Сканировать QR" в режиме тестирования: действующий код подставляет сервер. */
    fun testScan() = act { deals.testScan(dealId) }

    fun scannerFailed() = _state.update { it.copy(scannerFailed = true) }

    private fun act(block: suspend () -> Deal) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, actionError = null) }
            try {
                val deal = block()
                _state.update { it.copy(deal = deal, closed = deal.status == "CLOSED") }
            } catch (e: AppError) {
                _state.update { it.copy(actionError = e) }
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }

    private companion object {
        /** Длина кода - настройка сервиса (close_code_len, 6); запас на другие значения. */
        const val MAX_DIGITS = 10
    }
}

/** Экран "Подтверждение" - общий для всех шаблонов (ТЗ 5.3): ввод ключа, сканер, "Сканировать QR" (тест). */
@Composable
fun CloseConfirmScreen(graph: AppGraph, nav: Nav, dealId: Long, initialCode: String) {
    val vm: CloseConfirmViewModel = viewModel(key = "close-confirm:$dealId") {
        CloseConfirmViewModel(graph.deals, graph.notifyCenter, dealId, initialCode)
    }
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.closed) { if (state.closed) nav.review(dealId) }
    val context = LocalContext.current
    val deal = state.deal
    val error = state.error
    when {
        deal == null && error != null -> ErrorBox(error, onRetry = vm::load)
        deal == null -> LoadingBox()
        else -> Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(deal.serviceName.orEmpty(), style = MaterialTheme.typography.titleMedium, color = Mp.colors.textMuted)
            Text(t("ui.waiting_code"), style = MaterialTheme.typography.titleLarge)
            if (state.can(CloseButton.ENTER_CODE.action)) {
                Text(t("ui.code_hint"), color = Mp.colors.textMuted)
                OutlinedTextField(
                    value = state.code,
                    onValueChange = vm::setCode,
                    singleLine = true,
                    textStyle = TextStyle(fontFamily = Mp.fonts.mono, fontSize = 28.sp, letterSpacing = 4.sp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = { vm.submit() },
                    enabled = !state.busy && state.code.isNotEmpty(),
                    shape = Mp.shapes.button,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) { Text(t("ui.confirm_code")) }
                // камера - в обычном режиме; тестовый агент закрывает без камеры (алгоритм, раздел 5)
                if (!state.iAmTesting) {
                    OutlinedButton(
                        onClick = {
                            GmsBarcodeScanning.getClient(context).startScan()
                                .addOnSuccessListener { barcode -> barcode.rawValue?.let { vm.submit(it) } }
                                // "Назад" в сканере на эмуляторе приходит как INTERNAL (13), а не CANCELLED (найдено в Э6 шаг 8):
                                // подсказка - только когда сканера действительно нет (модуль или сервисы Google Play)
                                .addOnFailureListener { e -> if (scannerUnavailable(e)) vm.scannerFailed() }
                        },
                        enabled = !state.busy,
                        shape = Mp.shapes.button,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) { Text(t("ui.scan_camera")) }
                }
                if (state.scannerFailed) Text(t("ui.err.scanner_unavailable"), color = Mp.colors.warning)
            } else {
                // гео-услуга до сближения: сервер не даёт ввести код (CONTACT_REQUIRED)
                Text(t("ui.err.contact_required"), color = Mp.colors.warning)
            }
            if (state.can(CloseButton.TEST_SCAN.action)) {
                OutlinedButton(
                    onClick = vm::testScan,
                    enabled = !state.busy,
                    shape = Mp.shapes.button,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) { Text(t(CloseButton.TEST_SCAN.textKey)) }
            }
            state.actionError?.let { Text(errorMessage(it), color = Mp.colors.danger) }
        }
    }
}
