package ru.mpavel.osmmap

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.StrictMode
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.svg.SvgDecoder
import org.osmdroid.config.Configuration
import ru.mpavel.osmmap.core.config.ConfigRepository
import ru.mpavel.osmmap.core.location.PositionSender
import ru.mpavel.osmmap.core.net.ApiClient
import ru.mpavel.osmmap.core.notify.NotificationCenter
import ru.mpavel.osmmap.core.net.SessionStore
import ru.mpavel.osmmap.data.DealRepository
import ru.mpavel.osmmap.data.NotificationRepository
import ru.mpavel.osmmap.data.ProviderRepository
import ru.mpavel.osmmap.data.ServiceRepository
import ru.mpavel.osmmap.data.SessionRepository
import ru.mpavel.osmmap.feature.provider.SkippedRequests
import java.io.File

/** Зависимости приложения: один клиент API и репозитории на весь процесс. */
class AppGraph(context: Context) {
    private val store = SessionStore(context)
    val api = ApiClient(context.getString(R.string.api_base_url), store)
    val config = ConfigRepository(context, api)
    val session = SessionRepository(api, store)
    val services = ServiceRepository(api)
    val deals = DealRepository(api)
    val provider = ProviderRepository(api)
    val notifications = NotificationRepository(api)
    val notifyCenter = NotificationCenter(notifications)
    val skipped = SkippedRequests(context)
    val positions = PositionSender(context, session)
}

class MpApp : Application(), SingletonImageLoader.Factory {

    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            // Сеть в UI-потоке роняет отладочную сборку сразу (план Э3, принцип 3); остальное - в лог
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().penaltyDeathOnNetwork().build(),
            )
        }
        // Серверы OSM требуют уникальный User-Agent; кэш тайлов - во внутренней памяти, без разрешений
        Configuration.getInstance().apply {
            userAgentValue = "$packageName/${packageManager.getPackageInfo(packageName, 0).versionName}"
            osmdroidBasePath = File(filesDir, "osmdroid")
            osmdroidTileCache = File(cacheDir, "osmdroid-tiles")
        }
        graph = AppGraph(this)
    }

    /** Coil с декодером SVG: пиктограммы и курсоры из БД. */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context).components { add(SvgDecoder.Factory()) }.build()
}
