package ru.mpavel.osmmap.core.icons

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import ru.mpavel.osmmap.data.UiConfig

/** Пиктограммы и курсоры из конфигурации: код -> SVG. */
class IconSet(val icons: Map<String, String>, val cursors: Map<String, String>) {
    companion object {
        fun of(config: UiConfig?): IconSet =
            IconSet(config?.icons.orEmpty().associate { it.code to it.svg }, config?.cursors.orEmpty())
    }
}

val LocalIcons = staticCompositionLocalOf { IconSet(emptyMap(), emptyMap()) }

fun Color.hex(): String = "#%06X".format(toArgb() and 0xFFFFFF)

/** Пиктограмма из БД в цвет текста вокруг (или [tint]); неизвестный код - пустое место того же размера. */
@Composable
fun MpIcon(
    code: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    size: Dp = 24.dp,
    contentDescription: String? = null,
) {
    val svg = code?.let { LocalIcons.current.icons[it] }
    if (svg == null) {
        Spacer(modifier.size(size))
        return
    }
    val hex = tint.hex()
    val context = LocalPlatformContext.current
    val request = remember(code, svg, hex) {
        ImageRequest.Builder(context)
            .data(tintSvg(svg, hex).toByteArray())
            .memoryCacheKey("icon:$code:$hex")
            .build()
    }
    AsyncImage(model = request, contentDescription = contentDescription, modifier = modifier.size(size))
}

/** SVG -> Bitmap для маркеров карты osmdroid (курсоры - как есть, без подмены цвета). */
suspend fun svgBitmap(context: Context, svg: String, sizePx: Int, cacheKey: String): Bitmap? {
    val request = ImageRequest.Builder(context)
        .data(svg.toByteArray())
        .size(sizePx)
        .allowHardware(false)
        .memoryCacheKey("bitmap:$cacheKey:$sizePx")
        .build()
    return SingletonImageLoader.get(context).execute(request).image?.toBitmap()
}
