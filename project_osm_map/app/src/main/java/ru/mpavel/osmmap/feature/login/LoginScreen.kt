package ru.mpavel.osmmap.feature.login

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
import ru.mpavel.osmmap.R
import ru.mpavel.osmmap.core.net.ApiError
import ru.mpavel.osmmap.core.net.AppError
import ru.mpavel.osmmap.core.theme.Mp
import ru.mpavel.osmmap.data.DevAgent
import ru.mpavel.osmmap.data.SessionRepository
import ru.mpavel.osmmap.ui.common.LoadingBox
import ru.mpavel.osmmap.ui.common.errorMessage

/**
 * Вход. Конфигурации интерфейса до входа нет, поэтому подписи - из ресурсов (values/, values-en/).
 * Сервер с MP_DEV_LOGIN=1 отдаёт список агентов (вход по agent_id); иначе /dev/agents = 404 - форма логина.
 */
data class LoginState(
    val loading: Boolean = true,
    val agents: List<DevAgent>? = null,
    val form: Boolean = false,
    val busy: Boolean = false,
    val error: AppError? = null,
)

class LoginViewModel(private val session: SessionRepository, private val onLoggedIn: () -> Unit) : ViewModel() {

    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state

    init {
        loadAgents()
    }

    fun loadAgents() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val agents = session.devAgents()
                _state.update { it.copy(loading = false, agents = agents, form = false) }
            } catch (e: ApiError) {
                // 404 - вход без пароля выключен на сервере
                _state.update { it.copy(loading = false, form = true, error = if (e.status == 404) null else e) }
            } catch (e: AppError) {
                _state.update { it.copy(loading = false, error = e) }
            }
        }
    }

    fun showForm(show: Boolean) = _state.update { it.copy(form = show, error = null) }

    fun loginDev(agentId: Long) = run { session.loginDev(agentId) }

    fun login(login: String, password: String) = run { session.login(login.trim(), password) }

    private fun run(block: suspend () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null) }
            try {
                block()
                onLoggedIn()
            } catch (e: AppError) {
                _state.update { it.copy(error = e) }
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }
}

@Composable
fun LoginScreen(graph: AppGraph, onLoggedIn: () -> Unit) {
    val vm: LoginViewModel = viewModel { LoginViewModel(graph.session, onLoggedIn) }
    val state by vm.state.collectAsStateWithLifecycle()
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().imePadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Text(stringResource(R.string.login_title), style = MaterialTheme.typography.titleMedium, color = Mp.colors.textMuted)
        state.error?.let {
            // Текст сервера - только по-русски; до входа язык интерфейса берётся из ресурсов
            val text = if (it is ApiError && it.code == "LOGIN_FAILED") stringResource(R.string.login_failed) else errorMessage(it)
            Text(text, color = Mp.colors.danger)
        }
        when {
            state.loading -> LoadingBox(Modifier.weight(1f))
            state.form -> PasswordForm(state, vm, Modifier.weight(1f))
            else -> AgentList(state, vm, Modifier.weight(1f))
        }
    }
}

@Composable
private fun AgentList(state: LoginState, vm: LoginViewModel, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.login_dev_hint), style = MaterialTheme.typography.bodySmall, color = Mp.colors.warning)
        val agents = state.agents
        if (agents == null) {
            Button(onClick = vm::loadAgents, shape = Mp.shapes.button, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.retry))
            }
        } else {
            LazyColumn(Modifier.weight(1f)) {
                items(agents, key = { it.agentId }) { a ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 56.dp)
                            .clickable(enabled = !state.busy) { vm.loginDev(a.agentId) }
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("#${a.agentId}", fontFamily = Mp.fonts.mono, color = Mp.colors.textMuted)
                        Column(Modifier.weight(1f)) {
                            Text(a.pseudonym ?: a.login ?: "#${a.agentId}", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                listOfNotNull(a.login, a.roles?.joinToString(", ")).joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = Mp.colors.textMuted,
                            )
                        }
                        if (a.isTesting) {
                            Text(stringResource(R.string.login_testing), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    HorizontalDivider(color = Mp.colors.border)
                }
            }
        }
        TextButton(onClick = { vm.showForm(true) }, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(R.string.login_by_password))
        }
    }
}

@Composable
private fun PasswordForm(state: LoginState, vm: LoginViewModel, modifier: Modifier) {
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = login, onValueChange = { login = it }, singleLine = true,
            label = { Text(stringResource(R.string.login_login)) }, modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = password, onValueChange = { password = it }, singleLine = true,
            label = { Text(stringResource(R.string.login_password)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = { vm.login(login, password) },
            enabled = !state.busy && login.isNotBlank() && password.isNotEmpty(),
            shape = Mp.shapes.button,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) { Text(stringResource(R.string.login_submit)) }
        if (state.agents != null) {
            TextButton(onClick = { vm.showForm(false) }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.login_by_agent))
            }
        }
    }
}
