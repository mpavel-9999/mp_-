package ru.mpavel.osmmap.feature.close

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ru.mpavel.osmmap.AppGraph
import ru.mpavel.osmmap.core.i18n.t
import ru.mpavel.osmmap.core.net.ApiError
import ru.mpavel.osmmap.core.net.AppError
import ru.mpavel.osmmap.core.notify.NotificationCenter
import ru.mpavel.osmmap.core.theme.Mp
import ru.mpavel.osmmap.data.CloseCode
import ru.mpavel.osmmap.data.Deal
import ru.mpavel.osmmap.data.DealRepository
import ru.mpavel.osmmap.ui.common.LoadingBox
import ru.mpavel.osmmap.ui.common.errorMessage

data class CloseCodeState(
    val deal: Deal? = null,
    val code: CloseCode? = null,
    val secondsLeft: Int = 0,
    val error: AppError? = null,
    val busy: Boolean = false,
    val actionError: AppError? = null,
    /** "Предоставить QR": код ушёл клиенту уведомлением CLOSE_CODE. */
    val codeSent: Boolean = false,
)

/**
 * Код закрытия у поставщика (п. 5.1, ТЗ 6): код берётся у сервера (GET /close/code) и обновляется,
 * когда кончается его интервал (expires_in_s); секунды до смены - раз в секунду.
 */
class CloseCodeViewModel(
    private val deals: DealRepository,
    private val center: NotificationCenter,
    private val dealId: Long,
) : ViewModel() {

    private val _state = MutableStateFlow(CloseCodeState())
    val state: StateFlow<CloseCodeState> = _state

    init {
        // частый опрос уведомлений: закрытие клиентом (REVIEW_REQUIRED) ведёт к баллам сразу
        center.fast(true)
        viewModelScope.launch { refreshDeal() }
        viewModelScope.launch { center.events.filter { it.dealId == dealId }.collect { refreshDeal() } }
        viewModelScope.launch {
            while (isActive) {
                val code = try {
                    deals.closeCode(dealId)
                } catch (e: ApiError) {
                    // сделка уже закрыта или отменена - код больше не нужен
                    _state.update { it.copy(error = e, code = null) }
                    break
                } catch (e: AppError) {
                    _state.update { it.copy(error = if (it.code == null) e else null, actionError = e) }
                    delay(RETRY_MS)
                    continue
                }
                val received = System.currentTimeMillis()
                _state.update { it.copy(code = code, error = null, actionError = null) }
                do {
                    val left = codeSecondsLeft(code.expiresInS, received, System.currentTimeMillis())
                    _state.update { it.copy(secondsLeft = left) }
                    delay(TICK_MS)
                } while (left > 0)
            }
        }
    }

    override fun onCleared() {
        center.fast(false)
    }

    private suspend fun refreshDeal() {
        try {
            val deal = deals.get(dealId)
            _state.update { it.copy(deal = deal) }
        } catch (_: AppError) {
            // заголовок и кнопки не обязательны: код показывается и без них
        }
    }

    /** "Предоставить QR" (режим тестирования): клиент получит код уведомлением и подтвердит его сам. */
    fun provide() {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, actionError = null) }
            try {
                deals.testProvide(dealId)
                _state.update { it.copy(codeSent = true) }
            } catch (e: AppError) {
                _state.update { it.copy(actionError = e) }
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }

    private companion object {
        const val TICK_MS = 1_000L
        const val RETRY_MS = 3_000L
    }
}

/** Экран "Код закрытия" - общий для всех шаблонов (ТЗ 5.3): QR (обновляется по TTL) и цифры, таймер. */
@Composable
fun CloseCodeScreen(graph: AppGraph, dealId: Long) {
    val vm: CloseCodeViewModel = viewModel(key = "close-code:$dealId") { CloseCodeViewModel(graph.deals, graph.notifyCenter, dealId) }
    val state by vm.state.collectAsStateWithLifecycle()
    val code = state.code
    val error = state.error
    when {
        // сделка уже закрыта или отменена: повторять нечего (после закрытия приложение само ведёт к баллам)
        code == null && error != null -> Text(errorMessage(error), color = Mp.colors.danger, modifier = Modifier.padding(16.dp))
        code == null -> LoadingBox()
        else -> Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            state.deal?.let { Text(it.serviceName.orEmpty(), style = MaterialTheme.typography.titleMedium, color = Mp.colors.textMuted) }
            Text(t("ui.show_code"), style = MaterialTheme.typography.titleLarge)
            if (code.method == "QR") QrImage(code.qrPayload)
            // цифры - всегда: клиент может ввести их, если сканер недоступен (KEY - только цифры)
            Text(code.code, fontFamily = Mp.fonts.mono, fontWeight = FontWeight.SemiBold, fontSize = 40.sp, letterSpacing = 6.sp)
            Text(t("ui.code_expires", "s" to state.secondsLeft), color = Mp.colors.textMuted, fontFamily = Mp.fonts.mono)
            state.actionError?.let { Text(errorMessage(it), color = Mp.colors.danger) }
            if (state.codeSent) Text(t("ui.code_sent"), color = Mp.colors.success)
            if (state.deal?.actions?.contains(CloseButton.TEST_PROVIDE.action) == true && !state.codeSent) {
                OutlinedButton(
                    onClick = vm::provide,
                    enabled = !state.busy,
                    shape = Mp.shapes.button,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) { Text(t(CloseButton.TEST_PROVIDE.textKey)) }
            }
        }
    }
}

/** QR на белом поле (тёмные модули на светлом читаются сканером в любой схеме, в том числе тёмной). */
@Composable
private fun QrImage(payload: String) {
    val bitmap = remember(payload) {
        val m = qrMatrix(payload, QR_PX)
        val pixels = IntArray(m.width * m.height) { i -> if (m.get(i % m.width, i / m.width)) BLACK else WHITE }
        Bitmap.createBitmap(pixels, m.width, m.height, Bitmap.Config.ARGB_8888).asImageBitmap()
    }
    Image(
        bitmap, contentDescription = payload, filterQuality = FilterQuality.None,
        modifier = Modifier.size(260.dp).background(Color.White).padding(8.dp),
    )
}

private const val QR_PX = 512
private const val BLACK = 0xFF000000.toInt()
private const val WHITE = 0xFFFFFFFF.toInt()
