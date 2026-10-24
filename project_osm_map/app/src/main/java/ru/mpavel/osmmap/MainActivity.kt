package ru.mpavel.osmmap

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowInsets
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.modules.OfflineTileProvider
import org.osmdroid.tileprovider.tilesource.FileBasedTileSource
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Экран сделки: точка клиента, зона поиска и отклики поставщиков на карте OSM (раздел ТЗ
 * «Отклики, карта и маршрут»). Данные берутся из локального API над mp_service (модуль server/).
 */
class MainActivity : Activity() {

    private lateinit var map: MapView
    private lateinit var attribution: TextView
    private lateinit var status: TextView
    private lateinit var offerList: LinearLayout
    private lateinit var routeBar: LinearLayout

    private lateinit var api: ApiClient
    private val io: ExecutorService = Executors.newSingleThreadExecutor()
    private var dealId = 0L
    private var baseLayerApplied = false
    private val offerMarkers = mutableMapOf<Long, Marker>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Серверы OSM требуют уникальный User-Agent, иначе тайлы блокируются
        Configuration.getInstance().userAgentValue = packageName

        api = ApiClient(getString(R.string.api_base_url))
        dealId = resources.getInteger(R.integer.demo_deal_id).toLong()

        map = MapView(this).apply {
            setTileSource(TileSourceFactory.MAPNIK) // до ответа API: онлайн-тайлы OSM, только для тестов
            setMultiTouchControls(true)             // масштаб двумя пальцами
            controller.setZoom(17.0)
            controller.setCenter(GeoPoint(43.2389, 76.8897)) // Алматы
        }
        setContentView(buildLayout())
        load()
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }

    override fun onDestroy() {
        io.shutdownNow()
        map.onDetach()
        super.onDestroy()
    }

    private fun buildLayout(): LinearLayout {
        // Подпись «© участники OpenStreetMap» обязательна по лицензии OSM: всегда поверх карты
        attribution = TextView(this).apply {
            text = getString(R.string.osm_attribution)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setTextColor(Color.DKGRAY)
            setBackgroundColor(0xCCFFFFFF.toInt())
            setPadding(dp(6), dp(2), dp(6), dp(2))
        }
        val mapFrame = FrameLayout(this).apply {
            addView(map, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
            addView(attribution, FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, Gravity.BOTTOM or Gravity.START))
        }
        status = TextView(this).apply {
            text = getString(R.string.loading)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTextColor(Color.BLACK)
            setPadding(0, 0, 0, dp(4))
            setOnClickListener { load() } // повтор загрузки, например после ошибки связи
        }
        offerList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        routeBar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(dp(12), dp(8), dp(12), dp(8))
            addView(status)
            addView(ScrollView(this@MainActivity).apply { addView(offerList) },
                LinearLayout.LayoutParams(MATCH_PARENT, dp(170)))
            addView(routeBar)
        }
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(mapFrame, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
            addView(panel, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            // С Android 15 окно рисуется под системными панелями: панель не должна уходить под полосу жестов
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                setOnApplyWindowInsetsListener { _, insets ->
                    val bars = insets.getInsets(WindowInsets.Type.systemBars())
                    panel.setPadding(dp(12), dp(8), dp(12), dp(8) + bars.bottom)
                    insets
                }
            }
        }
    }

    private fun load() {
        status.text = getString(R.string.loading)
        io.execute {
            try {
                val layers = if (baseLayerApplied) null else api.baseLayers()
                val deal = api.deal(dealId)
                val offers = api.offers(dealId)
                val routes = api.routes(dealId)
                runOnUi {
                    layers?.let { applyBaseLayer(it) }
                    showDeal(deal, offers, routes)
                }
            } catch (e: Exception) {
                runOnUi { status.text = getString(R.string.api_error, getString(R.string.api_base_url), e.message) }
            }
        }
    }

    private fun agree(offerId: Long) {
        io.execute {
            try {
                api.agree(dealId, offerId)
                runOnUi { load() }
            } catch (e: Exception) {
                runOnUi { Toast.makeText(this, e.message, Toast.LENGTH_LONG).show() }
            }
        }
    }

    /**
     * Подложка по справочнику MAP_BASE_LAYER: офлайн-пакет OSM, если он уже скачан на устройство
     * (files/maps/<код>.mbtiles), иначе онлайн-тайлы tile.openstreetmap.org (только для тестов).
     */
    private fun applyBaseLayer(layers: List<ApiClient.BaseLayer>) {
        baseLayerApplied = true
        val offline = layers.firstNotNullOfOrNull { layer ->
            if (!layer.code.startsWith("OSM_OFFLINE")) return@firstNotNullOfOrNull null
            File(getExternalFilesDir("maps"), "${layer.code}.mbtiles").takeIf { it.exists() }?.let { layer to it }
        }
        if (offline != null) {
            val provider = OfflineTileProvider(SimpleRegisterReceiver(this), arrayOf(offline.second))
            map.setTileProvider(provider)
            val sourceName = provider.archives.firstOrNull()?.tileSources?.firstOrNull()
            map.setTileSource(sourceName?.let { FileBasedTileSource.getSource(it) } ?: TileSourceFactory.MAPNIK)
            map.setUseDataConnection(false)
            attribution.text = offline.first.attribution
        } else {
            map.setTileSource(TileSourceFactory.MAPNIK)
            attribution.text = layers.firstOrNull { it.code == "OSM" }?.attribution ?: getString(R.string.osm_attribution)
        }
    }

    private fun showDeal(deal: ApiClient.Deal, offers: List<ApiClient.Offer>, routes: List<ApiClient.RouteLink>) {
        map.overlays.clear()
        offerMarkers.clear()

        if (deal.lat != null && deal.lon != null) {
            val center = GeoPoint(deal.lat, deal.lon)
            deal.radiusM?.let { radius ->
                map.overlays.add(Polygon(map).apply {
                    setPoints(Polygon.pointsAsCircle(center, radius))
                    fillPaint.color = 0x220066FF
                    outlinePaint.color = 0xFF0066FF.toInt()
                    outlinePaint.strokeWidth = 3f
                })
            }
            map.overlays.add(Marker(map).apply {
                position = center
                title = getString(R.string.client_point)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                alpha = 0.6f
            })
            map.controller.setCenter(center)
        }

        for (offer in offers) {
            if (offer.lat == null || offer.lon == null) continue
            val marker = Marker(map).apply {
                position = GeoPoint(offer.lat, offer.lon)
                title = offer.label
                snippet = offerDetails(offer)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            map.overlays.add(marker)
            offerMarkers[offer.id] = marker
        }

        status.text = getString(R.string.deal_header, deal.serviceName, statusName(deal.status), offers.size)
        offerList.removeAllViews()
        for (offer in offers) {
            offerList.addView(offerRow(deal, offer))
        }

        routeBar.removeAllViews()
        for (route in routes) {
            routeBar.addView(Button(this).apply {
                text = route.providerName
                isAllCaps = false
                setOnClickListener { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(route.url))) }
            }, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        }
        map.invalidate()
    }

    private fun offerRow(deal: ApiClient.Deal, offer: ApiClient.Offer): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(TextView(this@MainActivity).apply {
                text = getString(R.string.offer_row, offer.position, offer.label, offerDetails(offer))
                setTextColor(Color.BLACK)
                setOnClickListener { focus(offer.id) }
            }, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
            if (deal.status == "OFFERS") {
                addView(Button(this@MainActivity).apply {
                    text = getString(R.string.agree)
                    isAllCaps = false
                    setOnClickListener { agree(offer.id) }
                })
            }
        }

    private fun offerDetails(offer: ApiClient.Offer): String {
        val distance = offer.distanceM?.let { getString(R.string.distance_m, it) }.orEmpty()
        val point = getString(if (offer.isExact) R.string.point_exact else R.string.point_approx)
        return listOf(distance, point).filter { it.isNotEmpty() }.joinToString(" · ")
    }

    private fun focus(offerId: Long) {
        val marker = offerMarkers[offerId] ?: return
        map.controller.animateTo(marker.position)
        marker.showInfoWindow()
    }

    private fun statusName(code: String): String = when (code) {
        "OFFERS" -> getString(R.string.status_offers)
        "AGREED" -> getString(R.string.status_agreed)
        "CONTACT" -> getString(R.string.status_contact)
        "CLOSED" -> getString(R.string.status_closed)
        else -> code
    }

    private fun runOnUi(block: () -> Unit) = runOnUiThread { if (!isDestroyed) block() }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
