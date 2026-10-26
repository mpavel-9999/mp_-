package ru.mpavel.osmmap

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import ru.mpavel.osmmap.core.i18n.LocalTexts
import ru.mpavel.osmmap.core.i18n.Texts
import ru.mpavel.osmmap.core.i18n.t
import ru.mpavel.osmmap.core.icons.IconSet
import ru.mpavel.osmmap.core.icons.LocalIcons
import ru.mpavel.osmmap.core.net.ApiError
import ru.mpavel.osmmap.core.net.AppError
import ru.mpavel.osmmap.core.theme.MpTheme
import ru.mpavel.osmmap.feature.close.CloseCodeScreen
import ru.mpavel.osmmap.feature.close.CloseConfirmScreen
import ru.mpavel.osmmap.feature.close.autoConfirm
import ru.mpavel.osmmap.feature.close.closeCodeFrom
import ru.mpavel.osmmap.feature.deal.DealScreen
import ru.mpavel.osmmap.feature.deals.DealsScreen
import ru.mpavel.osmmap.feature.home.HomeScreen
import ru.mpavel.osmmap.feature.login.LoginScreen
import ru.mpavel.osmmap.feature.profile.ProfileScreen
import ru.mpavel.osmmap.feature.provider.IncomingScreen
import ru.mpavel.osmmap.feature.provider.ProviderScreen
import ru.mpavel.osmmap.feature.provider.ServiceSettingsScreen
import ru.mpavel.osmmap.feature.provider.SlotScreen
import ru.mpavel.osmmap.feature.request.RequestScreen
import ru.mpavel.osmmap.feature.review.ReviewScreen
import ru.mpavel.osmmap.feature.settings.SettingsScreen
import ru.mpavel.osmmap.ui.common.ErrorBox
import ru.mpavel.osmmap.ui.common.LoadingBox
import ru.mpavel.osmmap.ui.map.LocalBaseLayer
import ru.mpavel.osmmap.ui.template.AppFrame
import ru.mpavel.osmmap.ui.template.Tab
import ru.mpavel.osmmap.ui.template.tabsFor

// Маршруты навигации (типобезопасные, kotlinx.serialization)
@Serializable
object HomeRoute

@Serializable
object DealsRoute

@Serializable
object SettingsRoute

@Serializable
object ProfileRoute

/** Запрос на услугу; slotId >= 0 - запрос из публикации поставщика на карте (п. 1.1.2). */
@Serializable
data class RequestRoute(val serviceId: Long, val slotId: Long = -1)

/** Сделка: до согласования - отклики, после - сама сделка. */
@Serializable
data class DealRoute(val dealId: Long)

/** Обязательные баллы по сделке (п. 5.6). */
@Serializable
data class ReviewRoute(val dealId: Long)

/** Код закрытия у поставщика: QR и цифры, обновляются по TTL (п. 5.1, ТЗ 6). */
@Serializable
data class CloseCodeRoute(val dealId: Long)

/** Подтверждение закрытия клиентом; [code] - код из уведомления CLOSE_CODE (п. 5.1.3, 5.1.4). */
@Serializable
data class CloseConfirmRoute(val dealId: Long, val code: String = "")

/** Раздел поставщика: мои сервисы, публикации, входящие запросы. */
@Serializable
object ProviderRoute

/** Входящие запросы поставщика: подходящие запросы, отклик, "Пропустить" (алгоритм 3.2-3.5). */
@Serializable
object IncomingRoute

/** Публикация окна или маршрута по подключённому сервису (алгоритм 4.1, 4.2). */
@Serializable
data class SlotRoute(val serviceId: Long)

/** Настройки поставщика по сервису: зона, время, цены, тариф, автоотклик. */
@Serializable
data class ServiceSettingsRoute(val serviceId: Long)

/** Состояние приложения: вход -> конфигурация интерфейса -> экраны. */
sealed interface AppState {
    data object Starting : AppState
    data object LoggedOut : AppState
    data object Ready : AppState
    data class Failed(val error: AppError) : AppState
}

class AppViewModel(private val graph: AppGraph) : ViewModel() {

    private val _state = MutableStateFlow<AppState>(AppState.Starting)
    val state: StateFlow<AppState> = _state

    /** Роли агента из GET /me - от них зависят разделы оболочки (раздел "Поставщик"). */
    private val _roles = MutableStateFlow<List<String>>(emptyList())
    val roles: StateFlow<List<String>> = _roles

    init {
        viewModelScope.launch {
            graph.api.unauthorized.collect {
                graph.notifyCenter.stop()
                graph.positions.reset()
                graph.session.forget()
                _state.value = AppState.LoggedOut
            }
        }
        if (graph.session.isLoggedIn) loadConfig() else _state.value = AppState.LoggedOut
    }

    /** GET /config: без сессии конфигурации нет, поэтому порядок - вход, затем конфигурация. */
    fun loadConfig() {
        viewModelScope.launch {
            _state.value = AppState.Starting
            _state.value = try {
                graph.config.load()
                _roles.value = graph.session.me().roles.orEmpty()
                graph.notifyCenter.start(viewModelScope)
                AppState.Ready
            } catch (e: ApiError) {
                if (e.status == 401) AppState.LoggedOut else AppState.Failed(e)
            } catch (e: AppError) {
                AppState.Failed(e)
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            graph.notifyCenter.stop()
            graph.skipped.clear()
            graph.positions.reset()
            runCatching { graph.session.logout() }
            _state.value = AppState.LoggedOut
        }
    }
}

/** Корень интерфейса: тема, тексты и пиктограммы из конфигурации; смена конфигурации перестраивает всё сразу. */
@Composable
fun App(graph: AppGraph) {
    val vm: AppViewModel = viewModel { AppViewModel(graph) }
    val state by vm.state.collectAsStateWithLifecycle()
    val roles by vm.roles.collectAsStateWithLifecycle()
    val config by graph.config.config.collectAsStateWithLifecycle()

    MpTheme(config?.scheme, config?.template) {
        CompositionLocalProvider(
            LocalTexts provides remember(config) { Texts.of(config) },
            LocalIcons provides remember(config) { IconSet.of(config) },
            LocalBaseLayer provides config?.baseLayer,
        ) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                when (val s = state) {
                    AppState.Starting -> LoadingBox()
                    AppState.LoggedOut -> LoginScreen(graph, onLoggedIn = vm::loadConfig)
                    is AppState.Failed -> ErrorBox(s.error, onRetry = vm::loadConfig)
                    AppState.Ready -> MainNav(graph, roles, onLogout = vm::logout)
                }
            }
        }
    }
}

/** Переходы между экранами - одни для всех шаблонов. */
class Nav(private val nav: NavHostController) {
    fun request(serviceId: Long, slotId: Long = -1) = nav.navigate(RequestRoute(serviceId, slotId))

    /** Сделка; [replace] - убрать текущий экран (запрос создан, баллы поставлены). */
    fun deal(dealId: Long, replace: Boolean = false) = nav.navigate(DealRoute(dealId)) {
        if (replace) nav.currentDestination?.id?.let { popUpTo(it) { inclusive = true } }
        launchSingleTop = true
    }

    fun review(dealId: Long) = nav.navigate(ReviewRoute(dealId)) { launchSingleTop = true }
    fun closeCode(dealId: Long) = nav.navigate(CloseCodeRoute(dealId)) { launchSingleTop = true }
    fun closeConfirm(dealId: Long, code: String = "") = nav.navigate(CloseConfirmRoute(dealId, code)) { launchSingleTop = true }
    fun deals() = nav.navigate(DealsRoute) { launchSingleTop = true }
    fun provider() = nav.navigate(ProviderRoute) { launchSingleTop = true }
    fun incoming() = nav.navigate(IncomingRoute) { launchSingleTop = true }
    fun slot(serviceId: Long) = nav.navigate(SlotRoute(serviceId))
    fun serviceSettings(serviceId: Long) = nav.navigate(ServiceSettingsRoute(serviceId))
    fun home() = nav.navigate(HomeRoute) { popUpTo(HomeRoute) { inclusive = true } }
    fun back() = nav.popBackStack()
}

@Composable
private fun MainNav(graph: AppGraph, roles: List<String>, onLogout: () -> Unit) {
    val controller = rememberNavController()
    val nav = remember(controller) { Nav(controller) }
    val entry by controller.currentBackStackEntryAsState()
    val destination = entry?.destination
    val tabs = remember(roles) { tabsFor(roles) }
    val current: Tab? = when {
        destination == null || destination.hasRoute<HomeRoute>() -> Tab.HOME
        destination.hasRoute<DealsRoute>() -> Tab.DEALS
        destination.hasRoute<ProviderRoute>() -> Tab.PROVIDER
        destination.hasRoute<SettingsRoute>() -> Tab.SETTINGS
        destination.hasRoute<ProfileRoute>() -> Tab.PROFILE
        else -> null
    }
    val title = when {
        destination?.hasRoute<RequestRoute>() == true -> t("ui.new_request")
        destination?.hasRoute<DealRoute>() == true -> t("ui.deal")
        destination?.hasRoute<ReviewRoute>() == true -> t("ui.review_title")
        destination?.hasRoute<CloseCodeRoute>() == true || destination?.hasRoute<CloseConfirmRoute>() == true -> t("ui.close_deal")
        destination?.hasRoute<IncomingRoute>() == true -> t("ui.incoming")
        destination?.hasRoute<SlotRoute>() == true -> t("ui.publish")
        destination?.hasRoute<ServiceSettingsRoute>() == true -> t("ui.service_settings")
        else -> null
    }

    // Новые уведомления - снекбаром; нажатие открывает сделку.
    // Закрытие (п. 5.1.1, 5.6): клиент нажал "Закрыть сделку" - у поставщика сам открывается код;
    // сделка закрыта или отменена со штрафом - сразу к обязательным баллам.
    val snackbar = remember { SnackbarHostState() }
    val texts by rememberUpdatedState(LocalTexts.current)
    LaunchedEffect(Unit) {
        graph.notifyCenter.events.collect { n ->
            val dealId = n.dealId
            when {
                dealId == null -> Unit
                n.type == "CLOSE_REQUEST" -> nav.closeCode(dealId)
                n.type == "REVIEW_REQUIRED" -> nav.review(dealId)
                // "Предоставить QR" (п. 5.1.4): тестовый клиент подтверждает код сам, обычный - на экране с подставленным кодом
                n.type == "CLOSE_CODE" -> closeCodeFrom(n)?.let { code ->
                    launch {
                        val deal = runCatching { graph.deals.get(dealId) }.getOrNull()
                        val closed = deal != null && autoConfirm(deal) && runCatching { graph.deals.close(dealId, code) }.isSuccess
                        // закрыто - к баллам ведёт REVIEW_REQUIRED; не вышло - экран подтверждения покажет ошибку
                        if (!closed && deal?.myRole == "client") nav.closeConfirm(dealId, code)
                    }
                }
            }
            launch {
                val result = snackbar.showSnackbar(
                    message = n.text ?: texts.notification(n.type),
                    actionLabel = n.dealId?.let { texts.t("ui.deal") },
                    duration = SnackbarDuration.Short,
                )
                graph.notifyCenter.markRead(n.notificationId)
                if (result == SnackbarResult.ActionPerformed) n.dealId?.let { nav.deal(it) }
            }
        }
    }

    AppFrame(
        current = current,
        title = title,
        snackbar = snackbar,
        tabs = tabs,
        onTab = { tab ->
            val route: Any = when (tab) {
                Tab.HOME -> HomeRoute
                Tab.DEALS -> DealsRoute
                Tab.PROVIDER -> ProviderRoute
                Tab.SETTINGS -> SettingsRoute
                Tab.PROFILE -> ProfileRoute
            }
            controller.navigate(route) {
                popUpTo(HomeRoute) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        },
        // баллы обязательны (п. 5.6): с экрана оценки "Назад" не уводит
        onBack = if (destination?.hasRoute<ReviewRoute>() == true) null else { { nav.back() } },
    ) { modifier ->
        NavHost(controller, startDestination = HomeRoute, modifier = modifier) {
            composable<HomeRoute> { HomeScreen(graph, nav) }
            composable<DealsRoute> { DealsScreen(graph, nav) }
            composable<SettingsRoute> { SettingsScreen(graph) }
            composable<ProfileRoute> { ProfileScreen(graph, onLogout = onLogout) }
            composable<RequestRoute> { e ->
                val r = e.toRoute<RequestRoute>()
                RequestScreen(graph, nav, r.serviceId, r.slotId.takeIf { it >= 0 })
            }
            composable<DealRoute> { e -> DealScreen(graph, nav, e.toRoute<DealRoute>().dealId) }
            composable<ReviewRoute> { e -> ReviewScreen(graph, nav, e.toRoute<ReviewRoute>().dealId) }
            composable<CloseCodeRoute> { e -> CloseCodeScreen(graph, e.toRoute<CloseCodeRoute>().dealId) }
            composable<CloseConfirmRoute> { e ->
                val r = e.toRoute<CloseConfirmRoute>()
                CloseConfirmScreen(graph, nav, r.dealId, r.code)
            }
            composable<ProviderRoute> { ProviderScreen(graph, nav) }
            composable<IncomingRoute> { IncomingScreen(graph, nav) }
            composable<SlotRoute> { e -> SlotScreen(graph, nav, e.toRoute<SlotRoute>().serviceId) }
            composable<ServiceSettingsRoute> { e -> ServiceSettingsScreen(graph, nav, e.toRoute<ServiceSettingsRoute>().serviceId) }
        }
    }
}
