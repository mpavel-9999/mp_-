package ru.mpavel.osmmap.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

// Модели API v0.7 (server/openapi.yaml). Имена полей - camelCase, в JSON - snake_case (ApiJson).
// Деньги, расстояния и рейтинги - Double: PostgreSQL отдаёт numeric с дробной частью (900.00).
// Время - строка ISO 8601 без зоны, как отдаёт сервер.

// ---------------------------------------------------------------- вход и профиль

@Serializable
data class DevAgent(
    val agentId: Long,
    /** null у агентов без псевдонима (например, компании) */
    val pseudonym: String? = null,
    val isTesting: Boolean = false,
    val roles: List<String>? = null,
    val login: String? = null,
)

@Serializable
data class LoginRequest(val login: String, val password: String)

@Serializable
data class DevLoginRequest(val agentId: Long)

@Serializable
data class LoginResponse(val token: String, val agent: Me)

@Serializable
data class Me(
    val agentId: Long,
    val pseudonym: String? = null,
    val name: String? = null,
    val isTesting: Boolean = false,
    val lang: String = "RUS",
    val roles: List<String>? = null,
    val pendingReviews: Int = 0,
    val services: List<MyService> = emptyList(),
)

@Serializable
data class MyService(
    val serviceId: Long,
    val name: String,
    val nodeType: String? = null,
    val priceUnit: String? = null,
    /** point / route / none: без гео публикация не нужна. */
    val geoMode: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    val zoneRadiusM: Double? = null,
    val workTimeFrom: String? = null,
    val workTimeTo: String? = null,
    val priceMin: Double? = null,
    val priceMax: Double? = null,
    val unitPriceMin: Double? = null,
    val capacity: Double? = null,
    val clientRatingMin: Double? = null,
    val isAuto: Boolean = false,
    val rating: Double? = null,
    val newbie: Boolean = false,
)

@Serializable
data class StatusOk(val status: String = "ok")

// ---------------------------------------------------------------- конфигурация интерфейса (GET /config)

@Serializable
data class UiConfig(
    val checksum: String = "",
    val config: UserConfig = UserConfig(),
    val template: Template = Template(),
    val scheme: Scheme = Scheme(),
    val texts: Map<String, String> = emptyMap(),
    val notifyTexts: Map<String, String> = emptyMap(),
    val statuses: Map<String, String> = emptyMap(),
    val cancelReasons: List<CancelReason> = emptyList(),
    val icons: List<IconDef> = emptyList(),
    val cursors: Map<String, String> = emptyMap(),
    val baseLayer: BaseLayer? = null,
)

@Serializable
data class UserConfig(
    val lang: String = "RUS",
    val template: String = "A",
    val scheme: String = "A_LIGHT",
    val map: String? = "OSM",
    val version: Int? = null,
)

/** Шаблон интерфейса (справочник UI_TEMPLATE): шрифт, формы, раскладки экранов, навигация. */
@Serializable
data class Template(
    val code: String = "A",
    val title: String? = null,
    val font: FontSpec = FontSpec(),
    val shape: ShapeSpec = ShapeSpec(),
    val screens: Map<String, String> = emptyMap(),
    val navigation: String = "top_actions",
    val map: MapSpec? = null,
)

@Serializable
data class FontSpec(val family: String? = null, val mono: String? = null)

@Serializable
data class ShapeSpec(val chip: Int = 16, val button: Int = 20, val corner: Int = 16)

@Serializable
data class MapSpec(val position: String? = null, val sheetPeek: Int? = null)

/** Цветовая схема (справочник UI_SCHEME). Ключи палитры - camelCase как в БД: onAccent, textMuted, mapLand. */
@Serializable
data class Scheme(val code: String = "A_LIGHT", val dark: Boolean = false, val colors: Map<String, String> = emptyMap())

@Serializable
data class IconDef(val iconId: Long, val code: String, val svg: String)

@Serializable
data class CancelReason(val id: Long, val code: String, val text: String)

@Serializable
data class BaseLayer(val code: String, val url: String? = null, val version: Double? = null, val attribution: String? = null)

/** PUT /config: отсутствующие поля не меняются. */
@Serializable
data class ConfigUpdate(val template: String? = null, val scheme: String? = null, val lang: String? = null)

@Serializable
data class UiOptions(val options: List<UiOption> = emptyList(), val languages: List<Language> = emptyList())

@Serializable
data class UiOption(val template: String, val scheme: String, val label: String, val isDark: Boolean = false)

@Serializable
data class Language(val code: String, val name: String)

// ---------------------------------------------------------------- услуги

@Serializable
data class ServiceNode(
    val serviceId: Long,
    val nodeType: String,
    val parentServiceId: Long? = null,
    val serviceName: String,
    val servicePath: String = "",
    val iconId: Long? = null,
    val iconCode: String? = null,
    val geoMode: String? = null,
    val priceUnit: String? = null,
)

@Serializable
data class FormField(
    val attributeId: Long,
    val type: String,
    val label: String,
    val iconCode: String? = null,
    val required: Boolean = false,
    val showStage: String? = null,
    val sortOrder: Int? = null,
    val options: List<FieldOption>? = null,
)

@Serializable
data class FieldOption(val attributeId: Long, val label: String)

@Serializable
data class FieldValue(val attributeId: Long, val label: String? = null, val iconCode: String? = null, val value: String? = null)

@Serializable
data class AttrValue(val attributeId: Long, val value: String)

// ---------------------------------------------------------------- сделка

@Serializable
data class Party(val pseudonym: String? = null, val rating: Double? = null, val newbie: Boolean = false, val isTesting: Boolean? = null)

@Serializable
data class Deal(
    val dealId: Long,
    val serviceId: Long,
    val serviceName: String? = null,
    val servicePath: String? = null,
    val status: String,
    val statusName: String? = null,
    val myRole: String,
    val geoMode: String? = null,
    val placeMode: String? = null,
    val priceUnit: String? = null,
    val closeMethod: String? = null,
    val nearRadiusM: String? = null,
    val currency: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    val zoneRadiusM: Double? = null,
    val toLat: Double? = null,
    val toLon: Double? = null,
    val routeLengthM: Double? = null,
    val periodStart: String? = null,
    val periodEnd: String? = null,
    val priceForecastMin: Double? = null,
    val priceForecastMax: Double? = null,
    val clientPrice: Double? = null,
    val providerRatingMin: Double? = null,
    val priceProvider: Double? = null,
    val priceTotal: Double? = null,
    val dateCreation: String? = null,
    val dateAgreed: String? = null,
    val dateClose: String? = null,
    val slotId: Long? = null,
    val client: Party? = null,
    val provider: Party? = null,
    val counterpartFields: List<FieldValue> = emptyList(),
    val requestFields: List<FieldValue> = emptyList(),
    val reviewPending: Boolean = false,
    /** Итог отмены, если нажать сейчас (deal_cancel_mode в БД); null - отменить нельзя. */
    val cancelMode: String? = null,
    /** Допустимые действия - решает сервер: agree, cancel, close, show_code, enter_code, test_scan, ... */
    val actions: List<String> = emptyList(),
)

@Serializable
data class DealListItem(
    val dealId: Long,
    val serviceId: Long,
    val serviceName: String? = null,
    val status: String,
    val statusName: String? = null,
    val myRole: String,
    val periodStart: String? = null,
    val periodEnd: String? = null,
    val priceTotal: Double? = null,
    val clientPrice: Double? = null,
    val priceForecastMin: Double? = null,
    val priceForecastMax: Double? = null,
    val priceUnit: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    val offersActive: Int = 0,
    val counterpart: String? = null,
    val reviewPending: Boolean = false,
)

@Serializable
data class DealCreate(
    val serviceId: Long,
    val slotId: Long? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    val toLat: Double? = null,
    val toLon: Double? = null,
    val zoneRadiusM: Double? = null,
    val periodStart: String,
    val periodEnd: String,
    val providerRatingMin: Double? = null,
    val clientPrice: Double? = null,
    val attributes: List<AttrValue> = emptyList(),
)

@Serializable
data class ForecastRequest(val serviceId: Long, val lat: Double? = null, val lon: Double? = null, val toLat: Double? = null, val toLon: Double? = null)

@Serializable
data class Forecast(
    val priceMin: Double? = null,
    val priceMax: Double? = null,
    val priceMedian: Double? = null,
    val providers: Int? = null,
    val priceUnit: String? = null,
    val routeLengthM: Double? = null,
    val routePolyline: String? = null,
)

/** Отклик (v_deal_offer_list): точка смещена до согласования, расстояние - интервалом. */
@Serializable
data class Offer(
    val dealOfferId: Long,
    val dealId: Long,
    val serviceId: Long? = null,
    val position: Int? = null,
    val providerAgentId: Long,
    val pseudonym: String? = null,
    val rating: Double? = null,
    val offerPrice: Double? = null,
    val offerStatus: String? = null,
    val offerMode: String? = null,
    val spotNumber: String? = null,
    val isExact: Boolean = false,
    val showLat: Double? = null,
    val showLon: Double? = null,
    val distanceM: Double? = null,
    val distanceBand: String? = null,
    val markerLabel: String? = null,
    val isTesting: Boolean = false,
    val newbie: Boolean = false,
)

@Serializable
data class OfferCreate(val price: Double, val lat: Double? = null, val lon: Double? = null, val attributes: List<AttrValue> = emptyList())

@Serializable
data class RouteLink(val dealOfferId: Long? = null, val providerCode: String, val sortOrder: Int? = null, val routeUrl: String, val providerName: String? = null)

@Serializable
data class Position(
    val agentId: Long,
    val role: String,
    val lat: Double,
    val lon: Double,
    val source: String? = null,
    val dateUpdate: String? = null,
    val cursor: String,
)

@Serializable
data class PositionUpdate(val lat: Double, val lon: Double, val source: String = "gps", val accuracyM: Double? = null)

@Serializable
data class CloseCode(val method: String, val code: String, val qrPayload: String, val ttlS: Int, val expiresInS: Int)

@Serializable
data class CloseSubmit(val code: String)

@Serializable
data class CancelRequest(val cancelReasonId: Long? = null, val comment: String? = null)

@Serializable
data class CancelResult(val status: String, val deal: Deal)

@Serializable
data class ReviewRequest(val score: Int, val comment: String? = null)

@Serializable
data class PendingReview(val dealId: Long, val counterpart: String? = null)

// ---------------------------------------------------------------- карта и поставщик

/** Публикация поставщика на карте клиента (v_provider_slot_map): точка смещена. */
@Serializable
data class MapSlot(
    val slotId: Long,
    val agentId: Long,
    val serviceId: Long,
    val serviceName: String? = null,
    val pseudonym: String? = null,
    val rating: Double? = null,
    val isTesting: Boolean = false,
    val priceMin: Double? = null,
    val periodStart: String? = null,
    val periodEnd: String? = null,
    val lat: Double,
    val lon: Double,
    val toLat: Double? = null,
    val toLon: Double? = null,
    val radiusM: Double? = null,
    val radiusToM: Double? = null,
    val routePolyline: String? = null,
    val markerLabel: String? = null,
    val distanceM: Double? = null,
)

/** Своя публикация (строка provider_slot). */
@Serializable
data class Slot(
    val slotId: Long,
    val agentId: Long,
    val serviceId: Long,
    val serviceName: String? = null,
    val geoLat: Double? = null,
    val geoLon: Double? = null,
    val radiusM: Double? = null,
    val geoToLat: Double? = null,
    val geoToLon: Double? = null,
    val radiusToM: Double? = null,
    val routePolyline: String? = null,
    val routeLengthM: Double? = null,
    val periodStart: String? = null,
    val periodEnd: String? = null,
    val priceMin: Double? = null,
    val slotStatus: String? = null,
)

@Serializable
data class SlotCreate(
    val serviceId: Long,
    val lat: Double,
    val lon: Double,
    val radiusM: Double? = null,
    val toLat: Double? = null,
    val toLon: Double? = null,
    val radiusToM: Double? = null,
    val periodStart: String,
    val periodEnd: String,
    val priceMin: Double? = null,
)

/** Входящий запрос для поставщика с рекомендованной ценой. */
@Serializable
data class IncomingRequest(
    val dealId: Long,
    val serviceId: Long,
    val serviceName: String? = null,
    val periodStart: String? = null,
    val periodEnd: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    val zoneRadiusM: Double? = null,
    val toLat: Double? = null,
    val toLon: Double? = null,
    val routeLengthM: Double? = null,
    val clientPrice: Double? = null,
    val priceForecastMin: Double? = null,
    val priceForecastMax: Double? = null,
    val priceUnit: String? = null,
    val geoMode: String? = null,
    val placeMode: String? = null,
    /** Мой последний отклик; null - ещё не откликался (запрос с откликом остаётся в списке, пока сделка открыта). */
    val myOffer: MyOffer? = null,
    val clientRating: Double? = null,
    val clientNewbie: Boolean = false,
    val offersActive: Int = 0,
    val recommendedPrice: Double? = null,
)

/** Свой отклик во входящем запросе: active / selected / declined / withdrawn / expired. */
@Serializable
data class MyOffer(val dealOfferId: Long, val price: Double, val status: String)

/** PUT /me/services/{id}: отсутствующие поля не меняются. */
@Serializable
data class ServiceSettingsUpdate(
    val lat: Double? = null,
    val lon: Double? = null,
    val zoneRadiusM: Double? = null,
    val workTimeFrom: String? = null,
    val workTimeTo: String? = null,
    val priceMin: Double? = null,
    val priceMax: Double? = null,
    val unitPriceMin: Double? = null,
    val clientRatingMin: Double? = null,
    val isAuto: Boolean? = null,
    val capacity: Int? = null,
)

// ---------------------------------------------------------------- уведомления

@Serializable
data class Notification(
    val notificationId: Long,
    val dealId: Long? = null,
    val type: String,
    val text: String? = null,
    val payload: JsonObject? = null,
    val dateCreation: String? = null,
    val dateRead: String? = null,
)
