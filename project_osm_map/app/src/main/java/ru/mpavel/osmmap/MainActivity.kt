package ru.mpavel.osmmap

import android.app.Activity
import android.os.Bundle
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

class MainActivity : Activity() {

    private lateinit var map: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Серверы OSM требуют уникальный User-Agent, иначе тайлы блокируются
        Configuration.getInstance().userAgentValue = packageName

        map = MapView(this).apply {
            setTileSource(TileSourceFactory.MAPNIK) // стандартная карта OSM, ключ не нужен
            setMultiTouchControls(true)             // масштаб двумя пальцами
            controller.setZoom(12.0)
            controller.setCenter(GeoPoint(55.7558, 37.6173)) // Москва
        }
        setContentView(map)
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }
}
