package ru.mpavel.osmmap.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import ru.mpavel.osmmap.core.config.ConfigRepository
import ru.mpavel.osmmap.core.i18n.t
import ru.mpavel.osmmap.core.icons.MpIcon
import ru.mpavel.osmmap.core.net.AppError
import ru.mpavel.osmmap.core.theme.Mp
import ru.mpavel.osmmap.data.ConfigUpdate
import ru.mpavel.osmmap.data.UiOption
import ru.mpavel.osmmap.data.UiOptions
import ru.mpavel.osmmap.ui.common.ErrorBox
import ru.mpavel.osmmap.ui.common.LoadingBox
import ru.mpavel.osmmap.ui.common.errorMessage

/** Фильтр списка "Шаблон / Схема". */
enum class SchemeFilter(val textKey: String) { ALL("ui.scheme_all"), LIGHT("ui.scheme_light"), DARK("ui.scheme_dark") }

data class SettingsState(
    val options: UiOptions? = null,
    val filter: SchemeFilter = SchemeFilter.ALL,
    val busy: Boolean = false,
    val error: AppError? = null,
) {
    val shown: List<UiOption>
        get() = options?.options.orEmpty().filter {
            when (filter) {
                SchemeFilter.ALL -> true
                SchemeFilter.LIGHT -> !it.isDark
                SchemeFilter.DARK -> it.isDark
            }
        }
}

class SettingsViewModel(private val config: ConfigRepository) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state

    init {
        loadOptions()
    }

    /** Подписи вариантов приходят на языке интерфейса - после смены языка список запрашивается заново. */
    fun loadOptions() {
        viewModelScope.launch {
            _state.update { it.copy(error = null) }
            try {
                val options = config.options()
                _state.update { it.copy(options = options) }
            } catch (e: AppError) {
                _state.update { it.copy(error = e) }
            }
        }
    }

    fun setFilter(filter: SchemeFilter) = _state.update { it.copy(filter = filter) }

    fun choose(option: UiOption) = apply(ConfigUpdate(template = option.template, scheme = option.scheme), reloadOptions = false)

    fun chooseLang(code: String) = apply(ConfigUpdate(lang = code), reloadOptions = true)

    /** PUT /config: новая конфигурация сразу уходит в тему, тексты и раскладку - без перезапуска. */
    private fun apply(update: ConfigUpdate, reloadOptions: Boolean) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null) }
            try {
                config.update(update)
                if (reloadOptions) {
                    _state.update { it.copy(options = config.options()) }
                }
            } catch (e: AppError) {
                _state.update { it.copy(error = e) }
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }
}

@Composable
fun SettingsScreen(graph: AppGraph) {
    val vm: SettingsViewModel = viewModel { SettingsViewModel(graph.config) }
    val state by vm.state.collectAsStateWithLifecycle()
    val current by graph.config.config.collectAsStateWithLifecycle()
    val options = state.options
    val error = state.error
    when {
        options == null && error != null -> ErrorBox(error, onRetry = vm::loadOptions)
        options == null -> LoadingBox()
        else -> Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            error?.let { Text(errorMessage(it), color = Mp.colors.danger) }

            Text(t("ui.interface"), style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (f in SchemeFilter.entries) {
                    FilterChip(
                        selected = state.filter == f,
                        onClick = { vm.setFilter(f) },
                        label = { Text(t(f.textKey)) },
                        shape = Mp.shapes.chip,
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                }
            }
            val cfg = current?.config
            val selected = options.options.firstOrNull { it.template == cfg?.template && it.scheme == cfg.scheme }
            Dropdown(
                icon = "layout",
                label = selected?.label ?: cfg?.let { "${it.template} / ${it.scheme}" }.orEmpty(),
                items = state.shown.map { it.label to { vm.choose(it) } },
                enabled = !state.busy,
            )

            Text(t("ui.language"), style = MaterialTheme.typography.titleMedium)
            val lang = options.languages.firstOrNull { it.code == cfg?.lang }
            Dropdown(
                icon = "note",
                label = lang?.name ?: cfg?.lang.orEmpty(),
                items = options.languages.map { it.name to { vm.chooseLang(it.code) } },
                enabled = !state.busy,
            )
        }
    }
}

/** Выпадающий список на стабильных компонентах Material 3 (кнопка + DropdownMenu). */
@Composable
private fun Dropdown(icon: String, label: String, items: List<Pair<String, () -> Unit>>, enabled: Boolean) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { open = true },
            enabled = enabled,
            shape = Mp.shapes.button,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
            MpIcon(icon, size = 20.dp)
            Text(label, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            for ((text, action) in items) {
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = {
                        open = false
                        action()
                    },
                )
            }
        }
    }
}
