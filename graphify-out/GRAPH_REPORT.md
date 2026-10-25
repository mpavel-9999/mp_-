# Graph Report - github_repo  (2026-10-26)

## Corpus Check
- 38 files · ~87,752 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 1361 nodes · 3392 edges · 123 communities (57 shown, 66 thin omitted)
- Extraction: 94% EXTRACTED · 6% INFERRED · 0% AMBIGUOUS · INFERRED: 212 edges (avg confidence: 0.87)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- Сделка: экран и части раскладок
- Оболочка приложения и этап Э3
- ТЗ v0.6: принципы и антифрод
- Экраны: вход, настройки, профиль
- Этапы Э0–Э2 и договорённости
- Сервер: Db, Jobs и план Э4 (API)
- Модели API приложения
- ТЗ v0.6: разделы
- Раскладки главной
- Карта MpMap
- Сервер: ConfigApi
- Сервер: зависимости и Routing
- Тема MpTheme и шрифты
- Сервер: утилиты и Auth
- HTTP-тест api_smoke.py
- Запрос: поля формы
- Репозитории API
- Тексты глоссария Texts
- ApiServer: маршруты
- Запрос: блоки формы
- Главная: HomeViewModel
- Сервер: сессии Auth
- Баллы и мастер запроса
- Запрос: RequestViewModel
- Настройки интерфейса
- Пиктограммы MpIcon
- Уведомления NotificationCenter
- Конфигурация ConfigRepository
- MpApp и зависимости
- Выбор даты и времени
- Маршрут: декодер polyline
- Этап Э4: итоги и окружение
- Сетевой клиент ApiClient
- Мои сделки
- Навигация и корень App
- Модели поставщика
- Сроки и антифрод
- Таблицы сделки и миграция
- Маршруты навигации
- Подбор поставщиков и такси
- Тесты моделей API
- Р-3 и план Э4
- Состояние приложения AppViewModel
- Каркас навигации AppFrame
- Роли и тестовый режим
- Этапы алгоритма сделки
- CLAUDE.md и следующий этап Э5
- Время периода Times
- Код закрытия CloseCode
- Профиль: ProfileViewModel
- Правила в БД и модель ошибок
- Планы Э1–Э2 и тестовые пользователи
- Отправка запроса
- Закрытие и отмена в алгоритме
- Глоссарий и правило описаний БД
- deal_cancel_mode и решения Э4
- Комплект документации v0.7
- Малая группа 58
- Малая группа 59
- Малая группа 60
- Малая группа 61
- Малая группа 62
- Малая группа 63
- Малая группа 64
- Малая группа 65
- Малая группа 66
- Малая группа 68
- Малая группа 69
- Малая группа 70
- Малая группа 71
- Малая группа 72
- Малая группа 73
- Малая группа 74
- Малая группа 75
- Малая группа 76
- Малая группа 77
- Малая группа 78
- Малая группа 79
- Малая группа 80
- Малая группа 81
- Малая группа 82
- Малая группа 83
- Малая группа 84
- Малая группа 85
- Малая группа 86
- Малая группа 87
- Малая группа 88
- Малая группа 90
- Малая группа 91
- Малая группа 92
- Малая группа 93
- Малая группа 94
- Малая группа 95
- Малая группа 96
- Малая группа 97
- Малая группа 98
- Малая группа 99
- Малая группа 100
- Малая группа 101
- Малая группа 102
- Малая группа 103
- Малая группа 104
- Малая группа 105
- Малая группа 106
- Малая группа 107
- Малая группа 108
- Малая группа 109
- Малая группа 110
- Малая группа 111
- Малая группа 112
- Малая группа 113
- Малая группа 114
- Малая группа 115
- Малая группа 116
- Малая группа 118
- Малая группа 119
- Малая группа 120
- Малая группа 121

## God Nodes (most connected - your core abstractions)
1. `DealApi` - 34 edges
2. `Request` - 31 edges
3. `Response` - 29 edges
4. `Этап Э3: каркас Android-приложения` - 25 edges
5. `Deal` - 25 edges
6. `DealState` - 25 edges
7. `RequestState` - 24 edges
8. `План этапа Э4 — экраны клиента` - 24 edges
9. `RequestViewModel` - 23 edges
10. `AppGraph` - 22 edges

## Surprising Connections (you probably didn't know these)
- `Этап Э3: каркас Android-приложения` --semantically_similar_to--> `Этап Э3: каркас приложения на Compose выполнен (U-1…U-4, JVM-тесты 9/9)`  [INFERRED] [semantically similar]
  project_osm_map/docs/v0.7/plans/E3_app.md → CLAUDE.md
- `Р-3 (Э4): какие сервисы на главной — решено: все сервисы всем агентам` --semantically_similar_to--> `Р-3 решён: на главной все сервисы всем агентам`  [INFERRED] [semantically similar]
  project_osm_map/docs/v0.7/03_implementation_spec.md → CLAUDE.md
- `Функция БД deal_cancel_mode(deal_id, agent_id) → CANCEL_FREE / CLIENT / PROVIDER / NULL` --semantically_similar_to--> `БД: deal_cancel_mode — итог отмены, одно правило для отмены и экрана`  [INFERRED] [semantically similar]
  project_osm_map/docs/v0.7/plans/E4_client.md → CLAUDE.md
- `Тест db/tests/02_cancel_mode.sql (FREE до согласования и в окне, CLIENT/PROVIDER позже, CLOSED — NULL)` --semantically_similar_to--> `Тест db/tests/02_cancel_mode.sql`  [INFERRED] [semantically similar]
  project_osm_map/docs/v0.7/plans/E4_client.md → CLAUDE.md
- `Итог Э1: PostgreSQL 17.6, 30 таблиц, 14 представлений, 35 функций, 17 триггеров` --semantically_similar_to--> `Этап Э1: БД mp_service v0.7 (правила в БД, тесты 29/29)`  [INFERRED] [semantically similar]
  project_osm_map/docs/v0.7/plans/E1_db.md → CLAUDE.md

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Конфигурация сервиса данными (дерево, настройки, поля, глоссарий)** — project_osm_map_docs_tz_marketplace_service_tree, project_osm_map_docs_tz_marketplace_service_setting, project_osm_map_docs_tz_marketplace_service_attribute, project_osm_map_docs_tz_marketplace_glossary, project_osm_map_docs_tz_marketplace_get_setting [EXTRACTED 1.00]
- **Правила сделки, проверяемые БД (переходы, подбор, отзывы, один selected)** — project_osm_map_docs_v0_7_02_db_architecture_trg_deal_status, project_osm_map_docs_v0_7_02_db_architecture_trg_deal_offer_check, project_osm_map_docs_v0_7_02_db_architecture_provider_can_offer, project_osm_map_docs_v0_7_02_db_architecture_trg_deal_review, project_osm_map_docs_v0_7_02_db_architecture_uq_deal_offer_selected, project_osm_map_docs_v0_7_02_db_architecture_dict_deal_transition [EXTRACTED 1.00]
- **Этапы алгоритма сделки: инициация → подбор → отклик → согласование → закрытие** — project_osm_map_docs_v0_7_01_deal_algorithm_stage_initiation, project_osm_map_docs_v0_7_01_deal_algorithm_stage_matching, project_osm_map_docs_v0_7_01_deal_algorithm_stage_offer, project_osm_map_docs_v0_7_01_deal_algorithm_stage_agreement, project_osm_map_docs_v0_7_01_deal_algorithm_stage_closing [EXTRACTED 1.00]
- **Правила сделки в БД: функции, триггеры, переходы, плановое задание** — project_osm_map_docs_v0_7_plans_e1_db_provider_can_offer, project_osm_map_docs_v0_7_plans_e1_db_deal_agree, project_osm_map_docs_v0_7_plans_e1_db_deal_cancel, project_osm_map_docs_v0_7_plans_e1_db_run_deal_jobs, project_osm_map_docs_v0_7_plans_e1_db_trg_deal_status, project_osm_map_docs_v0_7_plans_e1_db_deal_transition [EXTRACTED 1.00]
- **E2 server module split (ApiServer, Db, Http, Auth, ConfigApi, DealApi, ProviderApi, Routing, CloseCode, Jobs)** — project_osm_map_docs_v0_7_plans_e2_api_apiserver, project_osm_map_docs_v0_7_plans_e2_api_db, project_osm_map_docs_v0_7_plans_e2_api_http, project_osm_map_docs_v0_7_plans_e2_api_auth, project_osm_map_docs_v0_7_plans_e2_api_configapi, project_osm_map_docs_v0_7_plans_e2_api_dealapi, project_osm_map_docs_v0_7_plans_e2_api_providerapi, project_osm_map_docs_v0_7_plans_e2_api_routing, project_osm_map_docs_v0_7_plans_e2_api_closecode, project_osm_map_docs_v0_7_plans_e2_api_jobs [EXTRACTED 1.00]
- **Интерфейс из конфигурации: кэш по сумме → тема, тексты, пиктограммы** — project_osm_map_docs_v0_7_plans_e3_app_config_checksum_cache, project_osm_map_docs_v0_7_plans_e3_app_theme_palette, project_osm_map_docs_v0_7_plans_e3_app_texts_glossary, project_osm_map_docs_v0_7_plans_e3_app_svg_icons, project_osm_map_docs_v0_7_plans_e3_app_settings_feature [EXTRACTED 1.00]
- **Вход тестовых пользователей по хешу пароля** — claude_test_users, project_osm_map_docs_v0_7_plans_e1_db_step_12a_test_users, project_osm_map_docs_v0_7_plans_e1_db_agent_login, project_osm_map_docs_v0_7_plans_e1_db_agent_login_check [EXTRACTED 1.00]
- **Меры против обхода платформы до согласования** — project_osm_map_docs_tz_marketplace_approx_offset, project_osm_map_docs_tz_marketplace_pseudonym, project_osm_map_docs_tz_marketplace_show_stage_near, project_osm_map_docs_tz_marketplace_antifraud [INFERRED 0.85]
- **Одна логика — три раскладки A/B/C** — project_osm_map_docs_v0_7_plans_e3_app_principle_one_logic_three_layouts, project_osm_map_app_src_main_java_ru_mpavel_osmmap_ui_template_a_homea_homea, project_osm_map_app_src_main_java_ru_mpavel_osmmap_ui_template_b_homeb_homeb, project_osm_map_app_src_main_java_ru_mpavel_osmmap_ui_template_c_homec_homec [INFERRED 0.85]
- **Taxi on-the-way matching (server-side routing, slots, requests, forecast)** — project_osm_map_docs_v0_7_plans_e2_api_routing, project_osm_map_docs_v0_7_plans_e2_api_graphhopper, project_osm_map_server_openapi_slots, project_osm_map_server_openapi_requests, project_osm_map_server_openapi_forecast [INFERRED 0.85]
- **Режим отмены: одно правило БД → поле API → диалог отмены** — project_osm_map_docs_v0_7_plans_e4_client_deal_cancel_mode, project_osm_map_server_openapi_deal_cancel_mode, project_osm_map_server_openapi_cancel, project_osm_map_app_src_main_java_ru_mpavel_osmmap_feature_deal_dealscreen_canceldialog, project_osm_map_docs_v0_7_plans_e4_client_test_02_cancel_mode [INFERRED 0.85]
- **Одна логика — три раскладки откликов и сделки (A/B/C)** — project_osm_map_docs_v0_7_plans_e4_client_principle_three_layouts, project_osm_map_app_src_main_java_ru_mpavel_osmmap_feature_deal_dealviewmodel_dealviewmodel, project_osm_map_app_src_main_java_ru_mpavel_osmmap_ui_template_a_deallayoutsa_offersa, project_osm_map_app_src_main_java_ru_mpavel_osmmap_ui_template_b_deallayoutsb_offersb, project_osm_map_app_src_main_java_ru_mpavel_osmmap_ui_template_c_deallayoutsc_offersc, project_osm_map_app_src_main_java_ru_mpavel_osmmap_ui_template_a_deallayoutsa_deala, project_osm_map_app_src_main_java_ru_mpavel_osmmap_ui_template_b_deallayoutsb_dealb, project_osm_map_app_src_main_java_ru_mpavel_osmmap_ui_template_c_deallayoutsc_dealc [EXTRACTED 1.00]
- **Время без часового пояса: истечение запросов и вопрос Р-4** — project_osm_map_docs_v0_7_plans_e4_client_timezone, project_osm_map_docs_v0_7_03_implementation_spec_question_r4, claude_emulator_timezone, project_osm_map_readme_timezone, claude_question_time_offset [INFERRED 0.95]

## Communities (123 total, 66 thin omitted)

### Community 0 - "Сделка: экран и части раскладок"
Cohesion: 0.08
Nodes (43): parseServerTime(), CancelDialog(), DealActions, DealActionsRow(), DealHeader(), DealMap(), DealScreen(), FieldList() (+35 more)

### Community 1 - "Оболочка приложения и этап Э3"
Cohesion: 0.05
Nodes (33): Kotlin 2.4.21 вместо встроенного в AGP 9 2.2.10 (buildscript), Этап Э3: каркас приложения на Compose выполнен (U-1…U-4, JVM-тесты 9/9), ApiError, AppError, DecodeError, ErrorBody, HttpResult, NetworkError (+25 more)

### Community 2 - "ТЗ v0.6: принципы и антифрод"
Cohesion: 0.05
Nodes (42): Антифрод и agent_sanction (эскалация санкций), Кейс A/B/C/D (сделка 6, порядок B, D, C), Триггер check_dict_type() (ОВ-22), Основной документ ТЗ в Claude Docs, Сделка deal, Отмена сделки (CANCEL_FREE/CLIENT/PROVIDER, EXPIRED), Отклик deal_offer, Статусы сделки INIT→MATCHING→OFFERS→AGREED→CONTACT→CLOSED (+34 more)

### Community 3 - "Экраны: вход, настройки, профиль"
Cohesion: 0.11
Nodes (4): ProfileContent(), ProfileScreen(), roleKey(), AppGraph

### Community 4 - "Этапы Э0–Э2 и договорённости"
Cohesion: 0.08
Nodes (31): server/ — локальный API на Java (HttpServer + JDBC), Python не в PATH: тесты запускать ~/.local/bin/python3.15.exe, Договорённость: детальный план + проверка логической связанности перед реализацией, Этап Э1: БД mp_service v0.7 (правила в БД, тесты 29/29), Этап Э2: API v0.7 (39 методов, smoke 16/16), Эскизы интерфейса (шаблоны A/B/C) — холст, Таблица agent_login (хеш bcrypt через pgcrypto), agent_login_check(login, password) — crypt() pgcrypto (+23 more)

### Community 5 - "Сервер: Db, Jobs и план Э4 (API)"
Cohesion: 0.17
Nodes (6): Шаг 2: API — поле cancel_mode в DealApi.SQL_DEAL и схеме Deal, Проверка по HTTP: server/tests/api_smoke.py, /deals/{dealId}/cancel (итог решает deal_cancel: CANCEL_FREE / CLIENT / PROVIDER), Поле Deal.cancel_mode (CANCEL_FREE / CLIENT / PROVIDER, null — отменить нельзя), CodeParams, DealApi

### Community 6 - "Модели API приложения"
Cohesion: 0.11
Nodes (26): AttrValue, CancelRequest, CancelResult, CloseSubmit, Deal, DealCreate, DevLoginRequest, FieldOption (+18 more)

### Community 7 - "ТЗ v0.6: разделы"
Cohesion: 0.05
Nodes (36): Антифрод, Жизненный цикл подписки, Изменения относительно исходной модели, Как добавить сервис, Карта в приложении, Кейс A/B/C/D, Модель данных, Монетизация (+28 more)

### Community 8 - "Раскладки главной"
Cohesion: 0.18
Nodes (11): DealsSummary(), HomeActions, HomeScreen(), PendingReviewsBanner(), SearchField(), ServiceRow(), slotMarkers(), TreeHeader() (+3 more)

### Community 9 - "Карта MpMap"
Cohesion: 0.09
Nodes (7): applyBaseLayer(), LatLon, MapLine, MapMarker, MapZone, MpMap(), UrlTileSource

### Community 10 - "Сервер: ConfigApi"
Cohesion: 0.26
Nodes (4): ConfigApi, Request, Response, ProviderApi

### Community 12 - "Тема MpTheme и шрифты"
Cohesion: 0.13
Nodes (9): findActivity(), googleFamily(), Mp, MpColors, MpFonts, MpShapes, MpTheme(), withFamily() (+1 more)

### Community 14 - "HTTP-тест api_smoke.py"
Cohesion: 0.18
Nodes (17): call(), check(), main(), agree(), close(), config(), contact(), create() (+9 more)

### Community 15 - "Запрос: поля формы"
Cohesion: 0.32
Nodes (14): FieldInput(), FieldsBlock(), PriceBlock(), RequestActions, RequestMap(), RequestScreen(), RouteTargetToggle(), ServiceBlock() (+6 more)

### Community 16 - "Репозитории API"
Cohesion: 0.13
Nodes (9): CloseCode, Notification, RouteLink, DealRepository, enc(), NotificationRepository, ServiceRepository, Модели и репозитории data/ (@Serializable по openapi.yaml, тонкие репозитории) (+1 more)

### Community 17 - "Тексты глоссария Texts"
Cohesion: 0.12
Nodes (5): t(), Texts, tintSvg(), parseHexColor(), CoreTest

### Community 18 - "ApiServer: маршруты"
Cohesion: 0.15
Nodes (6): ApiServer, Handler, Route, Projection, Route, Routing

### Community 19 - "Запрос: блоки формы"
Cohesion: 0.13
Nodes (4): ErrorBox(), errorMessage(), LoadingBox(), textOr()

### Community 20 - "Главная: HomeViewModel"
Cohesion: 0.15
Nodes (8): DealListItem, MapSlot, PendingReview, ServiceNode, HomeState, HomeViewModel, Шаг 4: Главная — дерево категория → сервис → вариант, запрос из публикации, плашка баллов, Схема Me (роли, pending_reviews, сервисы)

### Community 21 - "Сервер: сессии Auth"
Cohesion: 0.17
Nodes (5): Auth, Session, Db, Work, Jobs

### Community 22 - "Баллы и мастер запроса"
Cohesion: 0.14
Nodes (3): ReviewScreen(), ReviewState, ReviewViewModel

### Community 23 - "Запрос: RequestViewModel"
Cohesion: 0.14
Nodes (4): RequestViewModel, RouteTarget, DEST, PICKUP

### Community 24 - "Настройки интерфейса"
Cohesion: 0.15
Nodes (11): ConfigUpdate, Language, UiOption, UiOptions, SchemeFilter, ALL, DARK, LIGHT (+3 more)

### Community 25 - "Пиктограммы MpIcon"
Cohesion: 0.13
Nodes (4): hex(), IconSet, MpIcon(), svgBitmap()

### Community 27 - "Конфигурация ConfigRepository"
Cohesion: 0.20
Nodes (6): ConfigRepository, BaseLayer, CancelReason, IconDef, UiConfig, UserConfig

### Community 30 - "Маршрут: декодер polyline"
Cohesion: 0.22
Nodes (4): decodePolyline(), apiErrorText(), errorKey(), ClientFlowTest

### Community 31 - "Этап Э4: итоги и окружение"
Cohesion: 0.17
Nodes (13): Кириллицу через adb shell input text не ввести — вводить латиницу и цифры, Открытый вопрос владельцу: проверка «начало в прошлом» и время со смещением, Этап Э4: экраны клиента выполнен (S-1, S-2, S-8–S-12, U-1, U-2; план E4_client.md), Тест db/tests/02_cancel_mode.sql, Тестовые данные: кейс A/B/C/D (сделка 6 — парковка, сделка 7 — такси), Найдено и исправлено при проверке (карта под шторкой A, перецентровка, обновление главной, «Назад» на баллах), Итог Э4 (2026-10-25): SQL 30/30 и 5/5, api_smoke 16/16, JVM-тесты 15/15, Тест db/tests/02_cancel_mode.sql (FREE до согласования и в окне, CLIENT/PROVIDER позже, CLOSED — NULL) (+5 more)

### Community 32 - "Сетевой клиент ApiClient"
Cohesion: 0.16
Nodes (4): ApiClient, SessionStore, DevAgent, SessionRepository

### Community 33 - "Мои сделки"
Cohesion: 0.17
Nodes (10): DealRow(), DealsScreen(), DealsState, DealsViewModel, Failed, Loaded, Loading, Шаг 8: «Мои сделки» — GET /deals?role=all&status=active (+2 more)

### Community 35 - "Модели поставщика"
Cohesion: 0.16
Nodes (7): IncomingRequest, PositionUpdate, ServiceSettingsUpdate, Slot, SlotCreate, StatusOk, ProviderRepository

### Community 36 - "Сроки и антифрод"
Cohesion: 0.17
Nodes (11): 6) Антифрод: BYPASS_GEO, NEAR_CANCEL, FAKE_REFERRALS, блокировки, Статус EXPIRED, Статус NOT_CLOSED, Плановые задания (раз в минуту): истечение, CLOSING_ASK, NOT_CLOSED, сближение, публикации, Таблица agent_sanction (warning / rating_down / block), Структура модулей приложения ru.mpavel.osmmap (core/*, feature/*, ui/template, ui/map), GraphHopper (маршруты по дорогам OSM), Модуль: Android-приложение (Kotlin, Jetpack Compose) (+3 more)

### Community 37 - "Таблицы сделки и миграция"
Cohesion: 0.17
Nodes (12): Миграция v0.6 → v0.7, Таблица agent_position [new], Таблица deal, Таблица deal_offer (offer_status), Таблица provider_slot [new], Триггер trg_agent_position (manual только при is_testing), Триггер trg_deal_offer_check, Индекс uq_deal_offer_selected (один selected на сделку) (+4 more)

### Community 38 - "Маршруты навигации"
Cohesion: 0.21
Nodes (9): DealRoute, DealsRoute, HomeRoute, ProfileRoute, RequestRoute, ReviewRoute, SettingsRoute, Э3. Каркас Android-приложения (Compose) (+1 more)

### Community 39 - "Подбор поставщиков и такси"
Cohesion: 0.18
Nodes (8): Сценарий: частная парковка (capacity), 2) Подбор поставщиков (MATCHING), agent_rating(agent, role, service, on_date), provider_can_offer(deal_id, agent_id) → (ok, reason), Таблица agent_service (цены, тариф, capacity, назначение), Триггер trg_agent_service_assign (субагент), Представление v_agent_available_service [chg], Схема подбора такси по пути

### Community 42 - "Р-3 и план Э4"
Cohesion: 0.20
Nodes (10): Р-3 решён: на главной все сервисы всем агентам, Правило: план перед каждым этапом (docs/v0.7/plans), Р-3 (Э4): какие сервисы на главной — решено: все сервисы всем агентам, Э4. Экраны клиента, ViewModel + UiState; шаблон выбирает только composable-реализацию, План этапа Э4 — экраны клиента, Цель Э4: путь «услуга → запрос → отклики → согласование → сделка → отмена или баллы» в A/B/C, Шаг 7: Баллы — оценка 1–5 обязательна, «Назад» не уводит (+2 more)

### Community 43 - "Состояние приложения AppViewModel"
Cohesion: 0.20
Nodes (6): AppState, AppViewModel, Failed, LoggedOut, Ready, Starting

### Community 44 - "Каркас навигации AppFrame"
Cohesion: 0.29
Nodes (9): ActionIcon(), AppFrame(), BottomTabs(), Tab, DEALS, HOME, PROFILE, SETTINGS (+1 more)

### Community 45 - "Роли и тестовый режим"
Cohesion: 0.18
Nodes (11): Роли агентов: CLIENT, PROVIDER, SUBAGENT, COMPANY, ADMIN, Режим тестирования администратора, Справочник MAP_CURSOR (капля / капля с шестерёнкой), Таблица agent (is_testing), Триггер trg_agent_testing (is_testing только при ADMIN), Представление v_map_cursor, Закрытие: /close/request, /close/code, POST /close, /close/test-scan, /close/test-provide, PUT /me/position, GET /deals/{id}/positions (+3 more)

### Community 46 - "Этапы алгоритма сделки"
Cohesion: 0.18
Nodes (8): Публикация (окно) поставщика (Н-2), Сценарий: парковка — публикация места, 1) Инициация — клиент создаёт запрос (INIT → MATCHING), agent_pending_reviews(agent_id), price_forecast(service_id, lat, lon, period_start), Таблица deal_review, Триггер trg_deal_review, Клиент: /map/slots, POST /forecast, POST /deals, /deals/{id}/offers, /agree, /routes

### Community 47 - "CLAUDE.md и следующий этап Э5"
Cohesion: 0.20
Nodes (8): JAVA_HOME = JBR Android Studio для сервера и сборки, Каталог планов docs/v0.7/plans, Этап Э5: экраны поставщика — следующий шаг (план E5_provider.md), Договорённости, О проекте, Структура, Текущее состояние и решения, E3_app.md — план этапа Э3

### Community 48 - "Время периода Times"
Cohesion: 0.27
Nodes (4): defaultPeriod(), formatPeriod(), formatShort(), toServerTime()

### Community 50 - "Профиль: ProfileViewModel"
Cohesion: 0.24
Nodes (5): Failed, Loaded, Loading, ProfileState, ProfileViewModel

### Community 51 - "Правила в БД и модель ошибок"
Cohesion: 0.22
Nodes (7): Схема PostgreSQL mp_service (db/mp_service.sql), ApiServer (routes, lang, DB errors to HTTP), DB rule functions (provider_can_offer, deal_agree, deal_cancel, make_auto_offers, price_forecast, run_deal_jobs, agent_login_check), DB views (v_deal_offer_list, v_deal_candidates, v_provider_slot_map, v_ui_options, v_map_cursor), Error model {error, code} with HTTP 400/401/403/404/409/422, Jobs module (run_deal_jobs every minute, taxi route requests), /notifications (since, read)

### Community 52 - "Планы Э1–Э2 и тестовые пользователи"
Cohesion: 0.22
Nodes (9): Тестовые пользователи user_testing_1…5 (агенты 7–11), E2 plan: API v0.7, api_smoke.py HTTP smoke test (16/16), 02_db_architecture.md (sections 6.3, 8), E1_db.md (E1 plan), 03_implementation_spec.md (TZ v0.7, sections 4 and 6), openapi.yaml - mp_service API 0.7.0, /deals/{dealId}/routes (ссылки на маршрут после согласования) (+1 more)

### Community 53 - "Отправка запроса"
Cohesion: 0.28
Nodes (7): Created, invalidFields(), RequestEvent, ReviewFirst, Шаг 5: Запрос — гео point / route / none, поля формы, прогноз через 500 мс, «Моя цена» = медиана, /forecast (price forecast, taxi by route length), Схема Forecast (price_median, route_length_m, route_polyline)

### Community 54 - "Закрытие и отмена в алгоритме"
Cohesion: 0.22
Nodes (7): Сценарий: онлайн-услуга (без CONTACT, ключ KEY), 5) Закрытие сделки, check_dict_type() + 9 триггеров, Справочник DEAL_TRANSITION [new], Таблица deal_status_log (пишет только триггер), Общий справочник dictionary (value_json), Триггер trg_deal_status

### Community 55 - "Глоссарий и правило описаний БД"
Cohesion: 0.25
Nodes (7): Правило БД: COMMENT ON для каждого объекта, поля и функции, 24 термина глоссария ui.* / ui.err.*, Э1. БД v0.7, Шаг 1: БД — deal_cancel_mode и термины ui.* / ui.err.*, Термины ошибок ui.err.* (review_required, no_service_access, required_fields, offer_not_active, busy, routing_unavailable, no_route, rule), Schema Error {error, code}, Коды ошибок API (REQUIRED_FIELDS, NO_SERVICE_ACCESS, BUSY, OFFER_NOT_ACTIVE, REVIEW_REQUIRED, NO_ROUTE, ROUTING_UNAVAILABLE…)

### Community 56 - "deal_cancel_mode и решения Э4"
Cohesion: 0.25
Nodes (5): Функция БД deal_cancel_mode(deal_id, agent_id) → CANCEL_FREE / CLIENT / PROVIDER / NULL, coreLibraryDesugaring desugar_jdk_libs 2.1.5 (java.time при minSdk 24), Вопросы владельцу В-Э4-1…В-Э4-4 и решение 25.10.2026, Схема Deal (status, actions, cancel_mode, counterpart_fields по show_stage), /deals/{dealId} (сделка: статус, действия, открытые поля)

### Community 57 - "Комплект документации v0.7"
Cohesion: 0.46
Nodes (8): Алгоритм формирования сделки v0.7, Журнал решений В-1…В-13, Н-1…Н-12, П-1…П-6, Архитектура БД mp_service v0.7 (29 таблиц), ТЗ на реализацию прототипа v0.7 (для Claude), Общая архитектура продукта v0.7, Комплект документации project_osm_map v0.7, ТЗ v0.6 (docs/TZ_marketplace.md), TZ_project_WIN_SERVICE.txt (исходный алгоритм)

### Community 59 - "Малая группа 59"
Cohesion: 0.33
Nodes (5): Режимы сервиса: geo_mode, place_mode, price_unit, max_parallel_deals, slot_required, close_method, get_setting(service_id, code, rating), Таблица service (дерево категория → сервис → вариант), Таблица service_setting (настройки по дереву и рейтингу), Модель конфигурации «новый сервис — только данными» (пример «Эвакуатор»)

### Community 60 - "Малая группа 60"
Cohesion: 0.29
Nodes (7): Справочник UI_TEMPLATE (A, B, C) и UI_SCHEME (LIGHT/DARK), Формат JSON шаблона интерфейса и палитры, Представление v_ui_options, Нефункциональные требования (безопасность, ПДн, карта, доступность, масштабирование), Шаблон A «Карта в центре» (Manrope), Шаблон B «Карточки» (Golos Text), Шаблон C «Диспетчер» (Onest + JetBrains Mono)

### Community 61 - "Малая группа 61"
Cohesion: 0.29
Nodes (7): API v0.7 (http://127.0.0.1:8080/api), Открытые вопросы реализации Р-1…Р-4, Вопрос Р-4 (Э5): время без пояса — отклонять начало в прошлом и/или передавать +05:00, Э5. Экраны поставщика, Э6. Режим тестирования и закрытие, Границы Э4 (поставщик — Э5; GPS, курсор и закрытие кодом — Э6), Наблюдение: отклик такси «0 м · примерно» — убрать пометку для маршрутных (Э5)

### Community 63 - "Малая группа 63"
Cohesion: 0.33
Nodes (4): 4) Согласование (OFFERS → AGREED → CONTACT), 3) Выбор — поставщик откликается (MATCHING → OFFERS), Статус CONTACT (сближение ≤ near_radius_m), Представление v_deal_offer_list (смещение, distance_band)

### Community 64 - "Малая группа 64"
Cohesion: 0.40
Nodes (5): Эмуляторы: 1 — user_testing_1 (агент 7), 2 — user_testing_2 (агент 8), Э7. Тестирование на двух эмуляторах, Сценарии U-1…U-5 (шаблоны, язык, 304, недоступность сервера, курсоры), Сценарии S-1…S-16 (раздел 7 алгоритма), Шаг 9: проверка на эмуляторе (S-1, S-2, S-8–S-12, U-1, U-2)

### Community 65 - "Малая группа 65"
Cohesion: 0.40
Nodes (3): Проверка на тестовых данных 25.10.2026 (расхождения v0.6), Модуль: PostgreSQL mp_service, Функциональные блоки (каталог, сделки, гео, закрытие, антифрод, конфигурация UI…)

### Community 66 - "Малая группа 66"
Cohesion: 0.50
Nodes (4): Таблица ui_config (config_json, checksum md5), GET/PUT /config, GET /ui/options, Загрузка конфигурации интерфейса по контрольной сумме (304), Взаимодействие: загрузка конфигурации интерфейса

### Community 68 - "Малая группа 68"
Cohesion: 0.67
Nodes (3): Auth module (in-memory bearer tokens 12h, dev login), Bearer security scheme, /session (login, logout)

## Knowledge Gaps
- **150 isolated node(s):** `ALL`, `DARK`, `LIGHT`, `Снимки экранов E3_screens/`, `Что остаётся на Э4–Э6 (feature/request, offers, deal, provider, core/location, QR)` (+145 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 371 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **66 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `План этапа Э4 — экраны клиента` connect `Р-3 и план Э4` to `Сделка: экран и части раскладок`, `Малая группа 64`, `Мои сделки`, `Этапы Э0–Э2 и договорённости`, `Сервер: Db, Jobs и план Э4 (API)`, `Маршруты навигации`, `Главная: HomeViewModel`, `Отправка запроса`, `Глоссарий и правило описаний БД`, `deal_cancel_mode и решения Э4`, `Комплект документации v0.7`, `Уведомления NotificationCenter`, `Малая группа 61`, `Этап Э4: итоги и окружение`?**
  _High betweenness centrality (0.148) - this node is a cross-community bridge._
- **What connects `ALL`, `DARK`, `LIGHT` to the rest of the system?**
  _150 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Сделка: экран и части раскладок` be split into smaller, more focused modules?**
  _Cohesion score 0.07922991484635321 - nodes in this community are weakly interconnected._
- **Why does `Шаг 5: Запрос — гео point / route / none, поля формы, прогноз через 500 мс, «Моя цена» = медиана` connect `Отправка запроса` to `Сделка: экран и части раскладок`, `Мои сделки`, `Р-3 и план Э4`, `Этапы алгоритма сделки`, `Запрос: поля формы`, `Главная: HomeViewModel`, `Планы Э1–Э2 и тестовые пользователи`, `Запрос: RequestViewModel`, `Малая группа 62`?**
  _High betweenness centrality (0.139) - this node is a cross-community bridge._
- **Should `Оболочка приложения и этап Э3` be split into smaller, more focused modules?**
  _Cohesion score 0.05048076923076923 - nodes in this community are weakly interconnected._
- **Why does `Клиент: /map/slots, POST /forecast, POST /deals, /deals/{id}/offers, /agree, /routes` connect `Этапы алгоритма сделки` to `Отправка запроса`?**
  _High betweenness centrality (0.123) - this node is a cross-community bridge._
- **Should `ТЗ v0.6: принципы и антифрод` be split into smaller, more focused modules?**
  _Cohesion score 0.05314009661835749 - nodes in this community are weakly interconnected._