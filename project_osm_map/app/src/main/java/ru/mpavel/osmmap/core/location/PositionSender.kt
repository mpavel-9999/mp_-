package ru.mpavel.osmmap.core.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ru.mpavel.osmmap.core.net.AppError
import ru.mpavel.osmmap.data.PositionResult
import ru.mpavel.osmmap.data.PositionUpdate
import ru.mpavel.osmmap.data.SessionRepository

/** Точка положения: широта, долгота, точность GPS в метрах. */
data class Fix(val lat: Double, val lon: Double, val accuracyM: Double? = null)

/**
 * Что отправить на сервер (план Э6, принцип 3): агент в режиме тестирования - только точку своего курсора
 * (`manual`), обычный агент - только GPS (`manual` сервер ему отклонит). null - отправлять нечего.
 */
fun positionUpdate(isTesting: Boolean, manual: Fix?, gps: Fix?): PositionUpdate? =
    if (isTesting) {
        manual?.let { PositionUpdate(it.lat, it.lon, source = "manual") }
    } else {
        gps?.let { PositionUpdate(it.lat, it.lon, source = "gps", accuracyM = it.accuracyM) }
    }

/**
 * Положение агента на экране согласованной сделки (ТЗ 5.4): раз в [SEND_MS], пока открыт хотя бы один такой
 * экран ([acquire] / [release]). В режиме тестирования повторяется последняя точка курсора - иначе она устареет
 * через position_ttl_s (120 с) и сближение не засчитается. Сделки, перешедшие в CONTACT, - в [contact].
 */
class PositionSender(private val context: Context, private val session: SessionRepository) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _manual = MutableStateFlow<Fix?>(null)

    /** Точка своего курсора (режим тестирования). */
    val manual: StateFlow<Fix?> = _manual

    private val _gps = MutableStateFlow<Fix?>(null)

    /** Последнее положение с GPS (обычный режим). */
    val gps: StateFlow<Fix?> = _gps

    private val _contact = MutableSharedFlow<List<Long>>(extraBufferCapacity = 8)

    /** Номера сделок, перешедших в CONTACT после отправки положения. */
    val contact: SharedFlow<List<Long>> = _contact

    @Volatile
    private var isTesting = false
    private var holders = 0
    private var job: Job? = null
    private var listener: LocationListener? = null

    /** Есть ли разрешение на геолокацию (нужно только обычному агенту). */
    fun hasPermission(): Boolean =
        context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /** Экран согласованной сделки открыт: начать отправку (первый экран) с источником по режиму агента. */
    @Synchronized
    fun acquire(testing: Boolean) {
        isTesting = testing
        holders++
        if (holders > 1) return
        if (!testing) startGps()
        job = scope.launch {
            while (isActive) {
                sendNow()
                delay(SEND_MS)
            }
        }
    }

    /** Экран закрыт: последний закрытый останавливает отправку и GPS. */
    @Synchronized
    fun release() {
        holders = (holders - 1).coerceAtLeast(0)
        if (holders > 0) return
        job?.cancel()
        job = null
        stopGps()
    }

    /** Разрешение выдано после [acquire] - включить GPS, если экран сделки ещё открыт. */
    @Synchronized
    fun onPermissionGranted() {
        if (holders > 0 && !isTesting) startGps()
    }

    /** Агент отпустил свой курсор (режим тестирования): запомнить и сразу отправить. */
    suspend fun moveManual(fix: Fix): PositionResult? {
        _manual.value = fix
        return sendNow()
    }

    /** Отправить текущее положение; ошибка сети не критична - повторим через [SEND_MS]. */
    suspend fun sendNow(): PositionResult? {
        val update = positionUpdate(isTesting, _manual.value, _gps.value) ?: return null
        return try {
            session.updatePosition(update).also { if (it.contact.isNotEmpty()) _contact.emit(it.contact) }
        } catch (_: AppError) {
            null
        }
    }

    /** Выход агента: свои точки забываются. */
    @Synchronized
    fun reset() {
        _manual.value = null
        _gps.value = null
    }

    @SuppressLint("MissingPermission") // проверено в hasPermission()
    private fun startGps() {
        if (listener != null || !hasPermission()) return
        val lm = context.getSystemService(LocationManager::class.java) ?: return
        val l = LocationListener { loc: Location -> _gps.value = loc.toFix() }
        listener = l
        for (provider in listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)) {
            if (!lm.isProviderEnabled(provider)) continue
            lm.getLastKnownLocation(provider)?.let { if (_gps.value == null) _gps.value = it.toFix() }
            lm.requestLocationUpdates(provider, GPS_MS, 0f, l, Looper.getMainLooper())
        }
    }

    private fun stopGps() {
        val l = listener ?: return
        context.getSystemService(LocationManager::class.java)?.removeUpdates(l)
        listener = null
    }

    private fun Location.toFix() = Fix(latitude, longitude, if (hasAccuracy()) accuracy.toDouble() else null)

    companion object {
        /** Отправка положения на экранах сделки - раз в 10 с (ТЗ 5.4). */
        const val SEND_MS = 10_000L

        /** Как часто просить обновления у GPS. */
        const val GPS_MS = 5_000L
    }
}
