package ru.mpavel.osmmap.feature.provider

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.mpavel.osmmap.AppGraph
import ru.mpavel.osmmap.Nav
import ru.mpavel.osmmap.core.i18n.t

// Заготовка экрана поставщика (план Э5, шаг 4): маршрут уже работает, содержимое - на шаге 9.

/** Настройки поставщика по сервису [serviceId]. */
@Composable
fun ServiceSettingsScreen(graph: AppGraph, nav: Nav, serviceId: Long) = Placeholder("ui.service_settings")

@Composable
private fun Placeholder(titleKey: String) {
    Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        Text(t(titleKey), style = MaterialTheme.typography.titleMedium)
    }
}
