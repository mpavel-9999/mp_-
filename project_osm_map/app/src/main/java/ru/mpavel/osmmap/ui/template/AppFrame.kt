package ru.mpavel.osmmap.ui.template

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.mpavel.osmmap.core.i18n.t
import ru.mpavel.osmmap.core.icons.MpIcon
import ru.mpavel.osmmap.core.theme.Mp

/** Разделы оболочки: пиктограмма из БД и ключ глоссария. */
enum class Tab(val icon: String, val textKey: String) {
    HOME("home", "ui.home"),
    DEALS("note", "ui.active_deals"),
    SETTINGS("gear", "ui.settings"),
    PROFILE("user", "ui.profile"),
}

/**
 * Каркас по template.navigation: top_actions (A) - кнопки сверху, bottom_tabs (B) - нижнее меню,
 * fab (C) - кнопка действия с меню разделов. Содержимое экранов одно и то же.
 * [current] = null - экран вне разделов (запрос, сделка, баллы): заголовок [title] и "Назад".
 */
@Composable
fun AppFrame(
    current: Tab?,
    title: String?,
    snackbar: SnackbarHostState,
    onTab: (Tab) -> Unit,
    onBack: (() -> Unit)?,
    content: @Composable (Modifier) -> Unit,
) {
    val snackHost: @Composable () -> Unit = { SnackbarHost(snackbar) }
    when (Mp.template.navigation) {
        "bottom_tabs" -> Scaffold(
            topBar = { if (current == null) TitleBar(current, title, onBack, actions = {}) },
            bottomBar = { if (current != null) BottomTabs(current, onTab) },
            snackbarHost = snackHost,
        ) { padding -> content(Modifier.padding(padding)) }

        "fab" -> Scaffold(
            topBar = { TitleBar(current, title, onBack, actions = {}) },
            floatingActionButton = { if (current != null) SectionsFab(onTab) },
            snackbarHost = snackHost,
        ) { padding -> content(Modifier.padding(padding)) }

        else -> Scaffold(
            topBar = {
                TitleBar(current, title, onBack) {
                    if (current != null) {
                        ActionIcon(Tab.DEALS, current, onTab)
                        ActionIcon(Tab.SETTINGS, current, onTab)
                        ActionIcon(Tab.PROFILE, current, onTab)
                    }
                }
            },
            snackbarHost = snackHost,
        ) { padding -> content(Modifier.padding(padding)) }
    }
}

@Composable
private fun TitleBar(current: Tab?, title: String?, onBack: (() -> Unit)?, actions: @Composable () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().heightIn(min = 56.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // экран без "Назад" (баллы, п. 5.6) передаёт title без onBack - см. MainNav
            if (current != Tab.HOME && onBack != null) {
                TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) { Text(t("ui.back")) }
            }
            Text(
                when {
                    current == Tab.HOME -> t("ui.app_name")
                    current != null -> t(current.textKey)
                    else -> title.orEmpty()
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            )
            actions()
        }
    }
}

@Composable
private fun ActionIcon(tab: Tab, current: Tab?, onTab: (Tab) -> Unit) {
    IconButton(onClick = { onTab(tab) }, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) {
        MpIcon(
            tab.icon,
            tint = if (tab == current) MaterialTheme.colorScheme.primary else Mp.colors.textMuted,
            contentDescription = t(tab.textKey),
        )
    }
}

@Composable
private fun BottomTabs(current: Tab, onTab: (Tab) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        for (tab in Tab.entries) {
            val selected = tab == current
            NavigationBarItem(
                selected = selected,
                onClick = { onTab(tab) },
                icon = {
                    MpIcon(tab.icon, tint = if (selected) MaterialTheme.colorScheme.primary else Mp.colors.textMuted)
                },
                label = { Text(t(tab.textKey), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primaryContainer),
            )
        }
    }
}

@Composable
private fun SectionsFab(onTab: (Tab) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        FloatingActionButton(
            onClick = { open = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = Mp.shapes.button,
        ) {
            MpIcon("layout", tint = MaterialTheme.colorScheme.onPrimary)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            for (tab in Tab.entries) {
                DropdownMenuItem(
                    text = { Text(t(tab.textKey)) },
                    leadingIcon = { MpIcon(tab.icon) },
                    onClick = {
                        open = false
                        onTab(tab)
                    },
                )
            }
        }
    }
}
