package ru.mpavel.osmmap.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ru.mpavel.osmmap.AppGraph
import ru.mpavel.osmmap.core.i18n.LocalTexts
import ru.mpavel.osmmap.core.i18n.t
import ru.mpavel.osmmap.core.icons.MpIcon
import ru.mpavel.osmmap.core.net.AppError
import ru.mpavel.osmmap.core.theme.Mp
import ru.mpavel.osmmap.data.Me
import ru.mpavel.osmmap.data.SessionRepository
import ru.mpavel.osmmap.ui.common.ErrorBox
import ru.mpavel.osmmap.ui.common.LoadingBox

/** Профиль - только просмотр: псевдоним, роли, рейтинг по сервисам, режим тестирования. */
sealed interface ProfileState {
    data object Loading : ProfileState
    data class Loaded(val me: Me) : ProfileState
    data class Failed(val error: AppError) : ProfileState
}

class ProfileViewModel(private val session: SessionRepository, lang: Flow<String?>) : ViewModel() {

    private val _state = MutableStateFlow<ProfileState>(ProfileState.Loading)
    val state: StateFlow<ProfileState> = _state

    init {
        // Названия сервисов - на языке интерфейса
        viewModelScope.launch { lang.distinctUntilChanged().collect { load() } }
    }

    fun load() {
        viewModelScope.launch {
            _state.value = ProfileState.Loading
            _state.value = try {
                ProfileState.Loaded(session.me())
            } catch (e: AppError) {
                ProfileState.Failed(e)
            }
        }
    }
}

/** Роль агента -> ключ глоссария; неизвестная роль - её код. */
private fun roleKey(role: String): String? = when (role) {
    "CLIENT" -> "ui.role_client"
    "PROVIDER" -> "ui.role_provider"
    "ADMIN" -> "ui.role_admin"
    "COMPANY" -> "ui.role_company"
    "SUBAGENT" -> "ui.role_subagent"
    else -> null
}

@Composable
fun ProfileScreen(graph: AppGraph, onLogout: () -> Unit) {
    val vm: ProfileViewModel = viewModel { ProfileViewModel(graph.session, graph.config.lang) }
    val state by vm.state.collectAsStateWithLifecycle()
    when (val s = state) {
        ProfileState.Loading -> LoadingBox()
        is ProfileState.Failed -> ErrorBox(s.error, onRetry = vm::load)
        is ProfileState.Loaded -> ProfileContent(s.me, onLogout)
    }
}

@Composable
private fun ProfileContent(me: Me, onLogout: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MpIcon("user", tint = MaterialTheme.colorScheme.primary, size = 40.dp)
                    Column(Modifier.weight(1f)) {
                        Text(me.pseudonym ?: me.name ?: "#${me.agentId}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        me.name?.let { Text(it, color = Mp.colors.textMuted) }
                    }
                }
                val roles = me.roles.orEmpty()
                if (roles.isNotEmpty()) {
                    val texts = LocalTexts.current
                    Text(roles.joinToString(" · ") { r -> roleKey(r)?.let { texts.t(it) } ?: r }, style = MaterialTheme.typography.bodyMedium)
                }
                if (me.isTesting) {
                    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = Mp.shapes.chip) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            MpIcon("gear", size = 18.dp, tint = MaterialTheme.colorScheme.primary)
                            Text(t("ui.testing_mode"), modifier = Modifier.padding(start = 8.dp), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
                Text(t("ui.rating"), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
            }
        }
        items(me.services, key = { it.serviceId }) { s ->
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(s.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                if (s.newbie) {
                    Text(t("ui.newbie"), color = Mp.colors.textMuted, style = MaterialTheme.typography.labelMedium)
                } else {
                    Text("★ ${s.rating?.let { "%.1f".format(it) } ?: "-"}", fontFamily = Mp.fonts.mono)
                }
            }
            HorizontalDivider(color = Mp.colors.border)
        }
        item {
            OutlinedButton(
                onClick = onLogout,
                shape = Mp.shapes.button,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp).heightIn(min = 48.dp),
            ) { Text(t("ui.logout")) }
        }
    }
}
