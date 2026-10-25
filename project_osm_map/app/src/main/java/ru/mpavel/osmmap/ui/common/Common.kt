package ru.mpavel.osmmap.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.mpavel.osmmap.R
import ru.mpavel.osmmap.core.i18n.LocalTexts
import ru.mpavel.osmmap.core.i18n.Texts
import ru.mpavel.osmmap.core.net.ApiError
import ru.mpavel.osmmap.core.net.NetworkError
import ru.mpavel.osmmap.core.theme.Mp

/** Текст глоссария, а до загрузки конфигурации - строка ресурсов. */
@Composable
fun textOr(key: String, fallbackRes: Int): String {
    val text = LocalTexts.current.t(key)
    return if (text == key) stringResource(fallbackRes) else text
}

/** Ключ глоссария для кода ошибки API: REVIEW_REQUIRED -> ui.err.review_required. */
fun errorKey(code: String): String = "ui.err." + code.lowercase()

/** Текст ошибки API на языке интерфейса: термин ui.err.<код>, если он есть, иначе текст сервера (только по-русски). */
fun apiErrorText(texts: Texts, error: ApiError): String {
    val key = errorKey(error.code)
    val text = texts.t(key)
    return if (text != key) text else error.message?.takeIf { it.isNotBlank() } ?: error.code
}

/** Понятный текст ошибки: нет связи - своим сообщением, ответ API - по коду или текстом сервера. */
@Composable
fun errorMessage(error: Throwable): String = when (error) {
    is NetworkError -> {
        val text = LocalTexts.current.t("ui.error_network")
        if (text == "ui.error_network") stringResource(R.string.error_network, error.baseUrl) else "$text (${error.baseUrl})"
    }
    is ApiError -> apiErrorText(LocalTexts.current, error)
    else -> stringResource(R.string.error_failed, error.message ?: error.javaClass.simpleName)
}

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

/** Ошибка и "Повторить" - без падения приложения (сценарий U-4). */
@Composable
fun ErrorBox(error: Throwable, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(errorMessage(error), color = Mp.colors.danger, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
        Button(onClick = onRetry, shape = Mp.shapes.button, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(textOr("ui.retry", R.string.retry))
        }
    }
}
