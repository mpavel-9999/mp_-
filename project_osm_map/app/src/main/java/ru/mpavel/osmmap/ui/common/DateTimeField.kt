package ru.mpavel.osmmap.ui.common

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.mpavel.osmmap.core.i18n.t
import ru.mpavel.osmmap.core.theme.Mp
import ru.mpavel.osmmap.core.time.formatShort
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Поле "дата и время": кнопка с текущим значением, по нажатию - выбор даты, затем времени (Material 3).
 * Подписи кнопок диалога - из глоссария; названия месяцев и дней - на языке устройства (так рисует Material).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimeField(label: String, value: LocalDateTime, onChange: (LocalDateTime) -> Unit, modifier: Modifier = Modifier) {
    var step by remember { mutableStateOf(0) } // 0 - закрыто, 1 - дата, 2 - время
    var date by remember { mutableStateOf(value.toLocalDate()) }

    OutlinedButton(onClick = { step = 1 }, shape = Mp.shapes.button, modifier = modifier.heightIn(min = 48.dp)) {
        Text("$label  ${formatShort(value)}", fontFamily = Mp.fonts.mono)
    }

    if (step == 1) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = value.toLocalDate().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { step = 0 },
            confirmButton = {
                TextButton(onClick = {
                    date = state.selectedDateMillis
                        ?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() } ?: value.toLocalDate()
                    step = 2
                }) { Text(t("ui.next")) }
            },
            dismissButton = { TextButton(onClick = { step = 0 }) { Text(t("ui.back")) } },
        ) { DatePicker(state = state) }
    }
    if (step == 2) {
        val state = rememberTimePickerState(initialHour = value.hour, initialMinute = value.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { step = 0 },
            confirmButton = {
                TextButton(onClick = {
                    onChange(LocalDateTime.of(date ?: LocalDate.now(), java.time.LocalTime.of(state.hour, state.minute)))
                    step = 0
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { step = 1 }) { Text(t("ui.back")) } },
            text = { TimePicker(state = state) },
        )
    }
}
