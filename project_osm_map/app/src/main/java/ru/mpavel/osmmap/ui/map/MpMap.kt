package ru.mpavel.osmmap.ui.map

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.tileprovider.modules.OfflineTileProvider
import org.osmdroid.tileprovider.tilesource.FileBasedTileSource
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline
import ru.mpavel.osmmap.R
import ru.mpavel.osmmap.core.icons.svgBitmap
import ru.mpavel.osmmap.core.theme.Mp
import ru.mpavel.osmmap.data.BaseLayer
import java.io.File

/** Точка на карте. */
@Immutable
data class LatLon(val lat: Double, val lon: Double)

/** Маркер: подпись по нажатию, пиктограмма - SVG как есть (курсоры, отклики). */
@Immutable
data class MapMarker(
    val id: String,
    val point: LatLon,
    val title: String,
    val snippet: String? = null,
    val svg: String? = null,
)

/** Зона клиента: точка и радиус в метрах. */
@Immutable
data class MapZone(val center: LatLon, val radiusM: Double)

/** Линия маршрута (такси, доставка) цвета акцента темы. */
@Immutable
data class MapLine(val points: List<LatLon>)

/** Подложка карты из конфигурации (config.base_layer). */
val LocalBaseLayer = staticCompositionLocalOf<BaseLayer?> { null }

/** Алматы - центр по умолчанию, как в тестовых данных. */
val DefaultCenter = LatLon(43.2389, 76.8897)

/** Онлайн-тайлы по шаблону адреса {z}/{x}/{y} из справочника MAP_BASE_LAYER. */
private class UrlTileSource(code: String, private val template: String) :
    OnlineTileSourceBase(code, 0, 19, 256, ".png", arrayOf(template)) {
    override fun getTileURLString(index: Long): String = template
        .replace("{z}", MapTileIndex.getZoom(index).toString())
        .replace("{x}", MapTileIndex.getX(index).toString())
        .replace("{y}", MapTileIndex.getY(index).toString())
}

/**
 * Подложка: офлайн-пакет MBTiles, если он уже есть на устройстве (files/maps/<код>.mbtiles),
 * иначе онлайн-тайлы. tile.openstreetmap.org - только для тестов: массовая загрузка запрещена политикой OSM.
 */
private suspend fun applyBaseLayer(map: MapView, layer: BaseLayer?) {
    val context = map.context
    val offline = layer?.takeIf { it.code.startsWith("OSM_OFFLINE") }?.let { l ->
        withContext(Dispatchers.IO) { File(context.getExternalFilesDir("maps"), "${l.code}.mbtiles").takeIf { it.exists() } }
    }
    if (offline != null) {
        val provider = OfflineTileProvider(SimpleRegisterReceiver(context), arrayOf(offline))
        map.setTileProvider(provider)
        val source = provider.archives.firstOrNull()?.tileSources?.firstOrNull()
        map.setTileSource(source?.let { FileBasedTileSource.getSource(it) } ?: TileSourceFactory.MAPNIK)
        map.setUseDataConnection(false)
        return
    }
    val url = layer?.url
    map.setTileSource(
        if (layer == null || url == null || url.startsWith("https://tile.openstreetmap.org/") || !url.contains("{z}")) {
            TileSourceFactory.MAPNIK
        } else {
            UrlTileSource(layer.code, url)
        },
    )
}

/**
 * Карта osmdroid: зона, маркеры, подпись правообладателя поверх карты (всегда видна, ТЗ 5.4).
 * [center] меняет центр только при смене значения - пользователь может двигать карту.
 */
@Composable
fun MpMap(
    modifier: Modifier = Modifier,
    center: LatLon = DefaultCenter,
    zoom: Double = 15.0,
    zone: MapZone? = null,
    markers: List<MapMarker> = emptyList(),
    lines: List<MapLine> = emptyList(),
    onMarkerClick: (String) -> Unit = {},
    /** Нажатие на свободное место карты (выбор точки запроса); null - карта только для просмотра. */
    onMapTap: ((LatLon) -> Unit)? = null,
    /** Где подпись правообладателя: там, где её не закроют шторка или кнопки раскладки. */
    attributionAlignment: Alignment = Alignment.BottomEnd,
) {
    val context = LocalContext.current
    val layer = LocalBaseLayer.current
    val colors = Mp.colors
    val map = remember {
        MapView(context).apply {
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            isTilesScaledToDpi = true
            controller.setZoom(zoom)
        }
    }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, map) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> map.onResume()
                Lifecycle.Event.ON_PAUSE -> map.onPause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            map.onResume()
        }
        onDispose {
            lifecycle.removeObserver(observer)
            map.onPause()
            map.onDetach()
        }
    }
    LaunchedEffect(layer) { applyBaseLayer(map, layer) }
    LaunchedEffect(center) { map.controller.setCenter(GeoPoint(center.lat, center.lon)) }

    val sizePx = (36 * context.resources.displayMetrics.density).toInt()
    val bitmaps by produceState(emptyMap<String, Bitmap>(), markers) {
        value = markers.mapNotNull { m ->
            val svg = m.svg ?: return@mapNotNull null
            svgBitmap(context, svg, sizePx, svg.hashCode().toString())?.let { m.id to it }
        }.toMap()
    }

    Box(modifier.clipToBounds()) {
        AndroidView(
            factory = { map },
            // MapView рисует тайлы и за своими границами - обрезаем, чтобы карта не просвечивала под соседями
            modifier = Modifier.fillMaxSize().clipToBounds(),
            update = { view ->
                view.overlays.clear()
                if (onMapTap != null) {
                    view.overlays.add(MapEventsOverlay(object : MapEventsReceiver {
                        override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                            onMapTap(LatLon(p.latitude, p.longitude))
                            return true
                        }

                        override fun longPressHelper(p: GeoPoint): Boolean = false
                    }))
                }
                for (line in lines) {
                    view.overlays.add(Polyline(view).apply {
                        setPoints(line.points.map { GeoPoint(it.lat, it.lon) })
                        outlinePaint.color = colors.accent.toArgb()
                        outlinePaint.strokeWidth = 10f
                        setOnClickListener { _, _, _ -> false }
                    })
                }
                if (zone != null) {
                    view.overlays.add(Polygon(view).apply {
                        points = Polygon.pointsAsCircle(GeoPoint(zone.center.lat, zone.center.lon), zone.radiusM)
                        fillPaint.color = colors.zone.copy(alpha = 0.15f).toArgb()
                        outlinePaint.color = colors.zone.toArgb()
                        outlinePaint.strokeWidth = 4f
                        setOnClickListener { _, _, _ -> false }
                    })
                }
                for (m in markers) {
                    view.overlays.add(Marker(view).apply {
                        position = GeoPoint(m.point.lat, m.point.lon)
                        title = m.title
                        snippet = m.snippet
                        bitmaps[m.id]?.let { icon = BitmapDrawable(view.resources, it) }
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        setOnMarkerClickListener { marker, _ ->
                            marker.showInfoWindow()
                            onMarkerClick(m.id)
                            true
                        }
                    })
                }
                view.invalidate()
            },
        )
        Text(
            text = layer?.attribution ?: stringResource(R.string.osm_attribution),
            style = MaterialTheme.typography.labelSmall,
            color = colors.text,
            modifier = Modifier
                .align(attributionAlignment)
                .background(colors.surface.copy(alpha = 0.85f))
                .padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}
