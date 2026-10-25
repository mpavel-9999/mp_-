package ru.mpavel.osmmap.feature.review

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.mpavel.osmmap.AppGraph
import ru.mpavel.osmmap.Nav
import ru.mpavel.osmmap.core.i18n.t
import ru.mpavel.osmmap.core.net.AppError
import ru.mpavel.osmmap.core.theme.Mp
import ru.mpavel.osmmap.data.Deal
import ru.mpavel.osmmap.data.DealRepository
import ru.mpavel.osmmap.data.ReviewRequest
import ru.mpavel.osmmap.ui.common.ErrorBox
import ru.mpavel.osmmap.ui.common.LoadingBox
import ru.mpavel.osmmap.ui.common.errorMessage

/** Баллы второй стороне (п. 5.6): обязательны после закрытия и после отмены другой стороной. */
data class ReviewState(
    val loading: Boolean = true,
    val error: AppError? = null,
    val deal: Deal? = null,
    val score: Int? = null,
    val comment: String = "",
    val sending: Boolean = false,
    val sendError: AppError? = null,
    val done: Boolean = false,
)

class ReviewViewModel(private val deals: DealRepository, private val dealId: Long) : ViewModel() {

    private val _state = MutableStateFlow(ReviewState())
    val state: StateFlow<ReviewState> = _state

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val deal = deals.get(dealId)
                // баллы уже поставлены - экран не нужен
                _state.update { it.copy(loading = false, deal = deal, done = !deal.reviewPending) }
            } catch (e: AppError) {
                _state.update { it.copy(loading = false, error = e) }
            }
        }
    }

    fun setScore(score: Int) = _state.update { it.copy(score = score) }
    fun setComment(text: String) = _state.update { it.copy(comment = text) }

    fun send() {
        val score = _state.value.score ?: return
        viewModelScope.launch {
            _state.update { it.copy(sending = true, sendError = null) }
            try {
                deals.review(dealId, ReviewRequest(score, _state.value.comment.takeIf { it.isNotBlank() }))
                _state.update { it.copy(done = true) }
            } catch (e: AppError) {
                _state.update { it.copy(sendError = e) }
            } finally {
                _state.update { it.copy(sending = false) }
            }
        }
    }
}

/** Общий для всех шаблонов экран; "Назад" не уводит, пока оценка не поставлена (ТЗ 5.3). */
@Composable
fun ReviewScreen(graph: AppGraph, nav: Nav, dealId: Long) {
    val vm: ReviewViewModel = viewModel(key = "review:$dealId") { ReviewViewModel(graph.deals, dealId) }
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.done) { if (state.done) nav.home() }
    BackHandler(enabled = !state.done) { }
    val error = state.error
    val deal = state.deal
    when {
        deal == null && error != null -> ErrorBox(error, onRetry = vm::load)
        deal == null -> LoadingBox()
        else -> Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            val other = if (deal.myRole == "client") deal.provider else deal.client
            Text(t("ui.review_title"), style = MaterialTheme.typography.titleLarge)
            Text("${deal.serviceName.orEmpty()} · ${other?.pseudonym.orEmpty()}", color = Mp.colors.textMuted)
            Text(t("ui.score"), style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (s in 1..5) {
                    val selected = state.score == s
                    val modifier = Modifier.sizeIn(minWidth = 52.dp, minHeight = 52.dp)
                    if (selected) {
                        Button(onClick = { vm.setScore(s) }, shape = Mp.shapes.button, modifier = modifier) { Text("★ $s") }
                    } else {
                        OutlinedButton(onClick = { vm.setScore(s) }, shape = Mp.shapes.button, modifier = modifier) { Text("$s") }
                    }
                }
            }
            OutlinedTextField(
                value = state.comment,
                onValueChange = vm::setComment,
                label = { Text(t("ui.review_comment")) },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
            state.sendError?.let { Text(errorMessage(it), color = Mp.colors.danger) }
            FilledTonalButton(
                onClick = vm::send,
                enabled = state.score != null && !state.sending,
                shape = Mp.shapes.button,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) { Text(t("ui.send")) }
        }
    }
}
