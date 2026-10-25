# Graph Report - github_repo  (2026-10-25)

## Corpus Check
- 31 files · ~74,502 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 1050 nodes · 2490 edges · 56 communities (40 shown, 16 thin omitted)
- Extraction: 91% EXTRACTED · 9% INFERRED · 0% AMBIGUOUS · INFERRED: 228 edges (avg confidence: 0.87)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- Приложение: модели API и тесты
- Сервер: ApiServer и обработчики
- Приложение: сеть и конфигурация
- Раскладки главной A/B/C
- Навигация и экраны приложения
- Приложение: MpApp и зависимости
- Карта MpMap (osmdroid)
- План Э2: модули API
- ТЗ v0.6: разделы
- Сделка: правила и антифрод
- Тема MpTheme и шрифты
- Функции правил сделки в БД
- HTTP-тест api_smoke.py
- Тексты глоссария Texts
- План Э3: шаги и проверка
- Каркас навигации AppFrame
- Пиктограммы MpIcon
- Этапы алгоритма и сценарии
- Закрытие, роли, тестовый режим
- CLAUDE.md: договорённости
- Структура проекта и README
- Архитектура API v0.7
- Маршрутизация Routing
- Переходы статусов и проверки БД
- Отклики и provider_can_offer
- Окружение: Kotlin, OSM, эмуляторы
- Этапы Э3–Э4 и вопросы
- Конфигурация интерфейса в БД
- Главная: HomeViewModel
- Сервер: Http и ошибки
- Э1–Э2: итоги и ресурсы
- Код закрытия CloseCode
- Дерево услуг и настройки
- Профиль: ProfileViewModel
- Комплект документации v0.7
- MainActivity
- ТЗ v0.6: основа
- Справочники и подпись OSM
- Приёмка и тестирование
- Антифрод и сроки
- Маршруты ApiServer
- Малая группа 45
- Малая группа 47
- Малая группа 48
- Малая группа 49
- Малая группа 52
- Малая группа 53

## God Nodes (most connected - your core abstractions)
1. `Request` - 51 edges
2. `Response` - 49 edges
3. `DealApi` - 33 edges
4. `Этап Э3: каркас Android-приложения` - 26 edges
5. `DealRepository` - 24 edges
6. `Routing` - 22 edges
7. `AppGraph` - 22 edges
8. `t()` - 22 edges
9. `MpIcon()` - 21 edges
10. `ApiClient` - 21 edges

## Surprising Connections (you probably didn't know these)
- `Итог Э1: PostgreSQL 17.6, 30 таблиц, 14 представлений, 35 функций, 17 триггеров` --semantically_similar_to--> `Этап Э1: БД mp_service v0.7 (правила в БД, тесты 29/29)`  [INFERRED] [semantically similar]
  project_osm_map/docs/v0.7/plans/E1_db.md → CLAUDE.md
- `Локальный API-сервер server/ (Java, JSON над mp_service)` --semantically_similar_to--> `server/ — локальный API на Java (HttpServer + JDBC)`  [INFERRED] [semantically similar]
  project_osm_map/README.md → CLAUDE.md
- `db/mp_service.sql (схема и тестовые данные)` --semantically_similar_to--> `Схема PostgreSQL mp_service (db/mp_service.sql)`  [INFERRED] [semantically similar]
  project_osm_map/README.md → CLAUDE.md
- `Этап Э3: каркас Android-приложения` --semantically_similar_to--> `Этап Э3: каркас приложения на Compose выполнен (U-1…U-4, JVM-тесты 9/9)`  [INFERRED] [semantically similar]
  project_osm_map/docs/v0.7/plans/E3_app.md → CLAUDE.md
- `Kotlin 2.4.21 вместо встроенного 2.2.10` --semantically_similar_to--> `Kotlin 2.4.21 вместо встроенного в AGP 9 2.2.10 (buildscript)`  [INFERRED] [semantically similar]
  project_osm_map/docs/v0.7/plans/E3_app.md → CLAUDE.md

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Конфигурация сервиса данными (дерево, настройки, поля, глоссарий)** — project_osm_map_docs_tz_marketplace_service_tree, project_osm_map_docs_tz_marketplace_service_setting, project_osm_map_docs_tz_marketplace_service_attribute, project_osm_map_docs_tz_marketplace_glossary, project_osm_map_docs_tz_marketplace_get_setting [EXTRACTED 1.00]
- **Правила сделки, проверяемые БД (переходы, подбор, отзывы, один selected)** — project_osm_map_docs_v0_7_02_db_architecture_trg_deal_status, project_osm_map_docs_v0_7_02_db_architecture_trg_deal_offer_check, project_osm_map_docs_v0_7_02_db_architecture_provider_can_offer, project_osm_map_docs_v0_7_02_db_architecture_trg_deal_review, project_osm_map_docs_v0_7_02_db_architecture_uq_deal_offer_selected, project_osm_map_docs_v0_7_02_db_architecture_dict_deal_transition [EXTRACTED 1.00]
- **Этапы алгоритма сделки: инициация → подбор → отклик → согласование → закрытие** — project_osm_map_docs_v0_7_01_deal_algorithm_stage_initiation, project_osm_map_docs_v0_7_01_deal_algorithm_stage_matching, project_osm_map_docs_v0_7_01_deal_algorithm_stage_offer, project_osm_map_docs_v0_7_01_deal_algorithm_stage_agreement, project_osm_map_docs_v0_7_01_deal_algorithm_stage_closing [EXTRACTED 1.00]
- **Правила сделки в БД: функции, триггеры, переходы, плановое задание** — project_osm_map_docs_v0_7_plans_e1_db_provider_can_offer, project_osm_map_docs_v0_7_plans_e1_db_deal_agree, project_osm_map_docs_v0_7_plans_e1_db_deal_cancel, project_osm_map_docs_v0_7_plans_e1_db_run_deal_jobs, project_osm_map_docs_v0_7_plans_e1_db_trg_deal_status, project_osm_map_docs_v0_7_plans_e1_db_deal_transition [EXTRACTED 1.00]
- **E2 server module split (ApiServer, Db, Http, Auth, ConfigApi, DealApi, ProviderApi, Routing, CloseCode, Jobs)** — project_osm_map_docs_v0_7_plans_e2_api_apiserver, project_osm_map_docs_v0_7_plans_e2_api_db, project_osm_map_docs_v0_7_plans_e2_api_http, project_osm_map_docs_v0_7_plans_e2_api_auth, project_osm_map_docs_v0_7_plans_e2_api_configapi, project_osm_map_docs_v0_7_plans_e2_api_dealapi, project_osm_map_docs_v0_7_plans_e2_api_providerapi, project_osm_map_docs_v0_7_plans_e2_api_routing, project_osm_map_docs_v0_7_plans_e2_api_closecode, project_osm_map_docs_v0_7_plans_e2_api_jobs [EXTRACTED 1.00]
- **Этапы реализации Э0–Э8** — project_osm_map_docs_v0_7_03_implementation_spec_stage_e0, project_osm_map_docs_v0_7_03_implementation_spec_stage_e1, project_osm_map_docs_v0_7_03_implementation_spec_stage_e2, project_osm_map_docs_v0_7_03_implementation_spec_stage_e3, project_osm_map_docs_v0_7_03_implementation_spec_stage_e4, project_osm_map_docs_v0_7_03_implementation_spec_stage_e5, project_osm_map_docs_v0_7_03_implementation_spec_stage_e6, project_osm_map_docs_v0_7_03_implementation_spec_stage_e7, project_osm_map_docs_v0_7_03_implementation_spec_stage_e8 [EXTRACTED 1.00]
- **Вход тестовых пользователей по хешу пароля** — claude_test_users, project_osm_map_docs_v0_7_plans_e1_db_step_12a_test_users, project_osm_map_docs_v0_7_plans_e1_db_agent_login, project_osm_map_docs_v0_7_plans_e1_db_agent_login_check [EXTRACTED 1.00]
- **Меры против обхода платформы до согласования** — project_osm_map_docs_tz_marketplace_approx_offset, project_osm_map_docs_tz_marketplace_pseudonym, project_osm_map_docs_tz_marketplace_show_stage_near, project_osm_map_docs_tz_marketplace_antifraud [INFERRED 0.85]
- **Deal lifecycle via API: request, offers, agree, close, review, cancel** — project_osm_map_server_openapi_deals, project_osm_map_server_openapi_deal_offers, project_osm_map_server_openapi_offer_agree, project_osm_map_server_openapi_close, project_osm_map_server_openapi_review, project_osm_map_server_openapi_cancel [INFERRED 0.85]
- **Taxi on-the-way matching (server-side routing, slots, requests, forecast)** — project_osm_map_docs_v0_7_plans_e2_api_routing, project_osm_map_docs_v0_7_plans_e2_api_graphhopper, project_osm_map_server_openapi_slots, project_osm_map_server_openapi_requests, project_osm_map_server_openapi_forecast [INFERRED 0.85]
- **Интерфейс из конфигурации: кэш по сумме → тема, тексты, пиктограммы** — project_osm_map_docs_v0_7_plans_e3_app_config_checksum_cache, project_osm_map_docs_v0_7_plans_e3_app_theme_palette, project_osm_map_docs_v0_7_plans_e3_app_texts_glossary, project_osm_map_docs_v0_7_plans_e3_app_svg_icons, project_osm_map_docs_v0_7_plans_e3_app_settings_feature [EXTRACTED 1.00]
- **Одна логика — три раскладки A/B/C** — project_osm_map_docs_v0_7_plans_e3_app_principle_one_logic_three_layouts, project_osm_map_app_src_main_java_ru_mpavel_osmmap_ui_template_a_homea_homea, project_osm_map_app_src_main_java_ru_mpavel_osmmap_ui_template_b_homeb_homeb, project_osm_map_app_src_main_java_ru_mpavel_osmmap_ui_template_c_homec_homec [INFERRED 0.85]
- **Этапы прототипа v0.7 Э0–Э4** — claude_stage_e0, claude_stage_e1, claude_stage_e2, claude_stage_e3, claude_stage_e4 [EXTRACTED 1.00]

## Communities (56 total, 16 thin omitted)

### Community 0 - "Приложение: модели API и тесты"
Cohesion: 0.05
Nodes (53): AttrValue, BaseLayer, CancelReason, CancelRequest, CancelResult, CloseCode, CloseSubmit, Deal (+45 more)

### Community 1 - "Сервер: ApiServer и обработчики"
Cohesion: 0.11
Nodes (12): Auth, Session, ConfigApi, Db, Work, CodeParams, DealApi, ApiException (+4 more)

### Community 2 - "Приложение: сеть и конфигурация"
Cohesion: 0.06
Nodes (14): ConfigRepository, ApiClient, decodeBody(), DecodeError, ErrorBody, HttpResult, ConfigUpdate, SchemeFilter (+6 more)

### Community 3 - "Раскладки главной A/B/C"
Cohesion: 0.10
Nodes (13): Mp, DealListItem, ServiceNode, DealsSummary(), HomeActions, HomeScreen(), SearchField(), ServiceRow() (+5 more)

### Community 4 - "Навигация и экраны приложения"
Cohesion: 0.09
Nodes (16): App(), HomeRoute, MainNav(), ProfileRoute, SettingsRoute, ApiError, AppError, NetworkError (+8 more)

### Community 5 - "Приложение: MpApp и зависимости"
Cohesion: 0.07
Nodes (14): AppState, AppViewModel, Failed, LoggedOut, Ready, Starting, SessionStore, AgentList() (+6 more)

### Community 6 - "Карта MpMap (osmdroid)"
Cohesion: 0.08
Nodes (8): svgBitmap(), slotMarkers(), applyBaseLayer(), LatLon, MapMarker, MapZone, MpMap(), UrlTileSource

### Community 7 - "План Э2: модули API"
Cohesion: 0.06
Nodes (32): E2 plan: API v0.7, ApiServer (routes, lang, DB errors to HTTP), Auth module (in-memory bearer tokens 12h, dev login), Db module (connection, session params, transaction), 02_db_architecture.md (sections 6.3, 8), DB rule functions (provider_can_offer, deal_agree, deal_cancel, make_auto_offers, price_forecast, run_deal_jobs, agent_login_check), DB views (v_deal_offer_list, v_deal_candidates, v_provider_slot_map, v_ui_options, v_map_cursor), DealApi module (client) (+24 more)

### Community 8 - "ТЗ v0.6: разделы"
Cohesion: 0.06
Nodes (32): Антифрод, Жизненный цикл подписки, Изменения относительно исходной модели, Как добавить сервис, Карта в приложении, Кейс A/B/C/D, Модель данных, Монетизация (+24 more)

### Community 9 - "Сделка: правила и антифрод"
Cohesion: 0.08
Nodes (30): Антифрод и agent_sanction (эскалация санкций), Кейс A/B/C/D (сделка 6, порядок B, D, C), Сделка deal, Отмена сделки (CANCEL_FREE/CLIENT/PROVIDER, EXPIRED), Отклик deal_offer, Статусы сделки INIT→MATCHING→OFFERS→AGREED→CONTACT→CLOSED, Отчёты по выручке (v_agent_finance_daily, v_company_finance_daily), geo_distance_m() (гаверсинус, ОВ-15/ОВ-19) (+22 more)

### Community 10 - "Тема MpTheme и шрифты"
Cohesion: 0.13
Nodes (8): findActivity(), googleFamily(), MpColors, MpFonts, MpShapes, MpTheme(), withFamily(), Scheme

### Community 11 - "Функции правил сделки в БД"
Cohesion: 0.12
Nodes (22): Эскизы интерфейса (шаблоны A/B/C) — холст, Таблица agent_login (хеш bcrypt через pgcrypto), agent_login_check(login, password) — crypt() pgcrypto, Кейс A/B/C/D: порядок откликов B, D, C; E отсечён правилом В-1, deal_agree(), Справочник DEAL_TRANSITION (18 переходов), Режим загрузки mp.load_mode (is_load_mode, RESET), trg_agent_testing / trg_agent_service_assign / trg_agent_position (+14 more)

### Community 12 - "HTTP-тест api_smoke.py"
Cohesion: 0.18
Nodes (17): call(), check(), main(), agree(), close(), config(), contact(), create() (+9 more)

### Community 13 - "Тексты глоссария Texts"
Cohesion: 0.12
Nodes (4): Texts, tintSvg(), parseHexColor(), CoreTest

### Community 14 - "План Э3: шаги и проверка"
Cohesion: 0.14
Nodes (18): Конфигурация с кэшем по контрольной сумме (GET /config?checksum= → 304/200), Снимки экранов E3_screens/, Проверка на эмуляторе: сценарии U-1…U-4, Вход feature/login/ (GET /dev/agents при MP_DEV_LOGIN=1, иначе POST /session), Сетевой слой core/net/ (ApiClient, ApiError, NetworkError, SessionStore), Что остаётся на Э4–Э6 (feature/request, offers, deal, provider, core/location, QR), Карта ui/map/ MpMap (osmdroid, зона, маркеры, подпись © OSM), Вопросы владельцу В-Э3-1…3 и решение от 25.10.2026 (+10 more)

### Community 15 - "Каркас навигации AppFrame"
Cohesion: 0.14
Nodes (9): t(), ActionIcon(), AppFrame(), BottomTabs(), Tab, HOME, PROFILE, SETTINGS (+1 more)

### Community 16 - "Пиктограммы MpIcon"
Cohesion: 0.13
Nodes (3): hex(), IconSet, MpIcon()

### Community 17 - "Этапы алгоритма и сценарии"
Cohesion: 0.11
Nodes (13): Сценарий: парковка — публикация места, Сценарий: частная парковка (capacity), 1) Инициация — клиент создаёт запрос (INIT → MATCHING), 2) Подбор поставщиков (MATCHING), 3) Выбор — поставщик откликается (MATCHING → OFFERS), agent_pending_reviews(agent_id), price_forecast(service_id, lat, lon, period_start), Таблица agent_service (цены, тариф, capacity, назначение) (+5 more)

### Community 18 - "Закрытие, роли, тестовый режим"
Cohesion: 0.12
Nodes (16): Роли агентов: CLIENT, PROVIDER, SUBAGENT, COMPANY, ADMIN, Сценарий: онлайн-услуга (без CONTACT, ключ KEY), 5) Закрытие сделки, Режим тестирования администратора, Справочник MAP_CURSOR (капля / капля с шестерёнкой), Таблица agent (is_testing), Таблица agent_position [new], Триггер trg_agent_position (manual только при is_testing) (+8 more)

### Community 19 - "CLAUDE.md: договорённости"
Cohesion: 0.18
Nodes (8): Сессия API задаёт mp.agent_id и mp.geo_secret, graphify-out — граф ведения проектов, Схема PostgreSQL mp_service (db/mp_service.sql), Этап Э1: БД mp_service v0.7 (правила в БД, тесты 29/29), Договорённости, О проекте, Структура, Текущее состояние и решения

### Community 21 - "Структура проекта и README"
Cohesion: 0.16
Nodes (14): Структура app/ на Compose: core/, data/, feature/, ui/, Офлайн-карта: только растровые MBTiles (скачивание с CDN — заглушка), project_osm_map — Android-приложение с картой OSM (osmdroid), ConfigApi module, Android-приложение app/ (Compose, Material 3, osmdroid), API, Подложка по справочнику MAP_BASE_LAYER, Офлайн-пакет OSM_OFFLINE_ALMATY.mbtiles на устройстве (+6 more)

### Community 22 - "Архитектура API v0.7"
Cohesion: 0.16
Nodes (14): Плановые задания (раз в минуту): истечение, CLOSING_ASK, NOT_CLOSED, сближение, публикации, Структура модулей приложения ru.mpavel.osmmap (core/*, feature/*, ui/template, ui/map), API v0.7 (http://127.0.0.1:8080/api), GraphHopper (маршруты по дорогам OSM), Библиотеки: Compose, OkHttp, kotlinx.serialization, osmdroid, Coil-svg, ZXing, Code Scanner, GraphHopper, Открытые вопросы Р-1…Р-3, Экраны приложения (вход, главная, запрос, отклики, сделка, код, баллы, отмена, публикация…), Э2. API v0.7 (+6 more)

### Community 23 - "Маршрутизация Routing"
Cohesion: 0.21
Nodes (3): Projection, Route, Routing

### Community 24 - "Переходы статусов и проверки БД"
Cohesion: 0.14
Nodes (11): Проверка на тестовых данных 25.10.2026 (расхождения v0.6), check_dict_type() + 9 триггеров, Справочник DEAL_TRANSITION [new], Миграция v0.6 → v0.7, Таблица deal_status_log (пишет только триггер), Общий справочник dictionary (value_json), Триггер trg_deal_status, Э0. Подготовка (второй AVD, OSM-данные, MP_GEO_SECRET) (+3 more)

### Community 25 - "Отклики и provider_can_offer"
Cohesion: 0.17
Nodes (14): Публикация (окно) поставщика (Н-2), agent_rating(agent, role, service, on_date), provider_can_offer(deal_id, agent_id) → (ok, reason), Таблица deal, Таблица deal_offer (offer_status), Таблица provider_slot [new], Триггер trg_deal_offer_check, Индекс uq_deal_offer_selected (один selected на сделку) (+6 more)

### Community 26 - "Окружение: Kotlin, OSM, эмуляторы"
Cohesion: 0.15
Nodes (11): adb reverse tcp:8080 → http://127.0.0.1:8080, kazakhstan-latest.osm.pbf (Geofabrik) для маршрутов такси, Этап Э0: второй эмулятор и данные OSM, Развёртывание прототипа (локально, 2 эмулятора через adb reverse), Библиотеки и версии (вопрос Р-2): Compose BOM 2026.09.00, OkHttp 5.5.1, Coil 3.6.3, navigation 2.10.2 и др., Проверка по HTTP: server/tests/api_smoke.py, Переменные сервера (MP_DB_URL, MP_DB_PASSWORD, MP_GEO_SECRET, MP_DEV_LOGIN, MP_OSM_FILE...), Маршруты такси — GraphHopper по kazakhstan-latest.osm.pbf (кэш gh-cache, 503 ROUTING_UNAVAILABLE) (+3 more)

### Community 28 - "Этапы Э3–Э4 и вопросы"
Cohesion: 0.17
Nodes (11): Эмуляторы: 1 — user_testing_1 (агент 7), 2 — user_testing_2 (агент 8), adb из Git Bash: MSYS_NO_PATHCONV=1, Каталог планов docs/v0.7/plans, Вопрос Р-3: какие сервисы показывать на главной, Этап Э3: каркас приложения на Compose выполнен (U-1…U-4, JVM-тесты 9/9), Этап Э4: экраны клиента (следующий шаг, план E4_client.md), Тестовые пользователи user_testing_1…5 (агенты 7–11), Перезапуск сервера сбрасывает токены (в памяти) → 401 и возврат на вход (+3 more)

### Community 29 - "Конфигурация интерфейса в БД"
Cohesion: 0.17
Nodes (12): Справочник UI_TEMPLATE (A, B, C) и UI_SCHEME (LIGHT/DARK), Таблица ui_config (config_json, checksum md5), Формат JSON шаблона интерфейса и палитры, Представление v_ui_options, GET/PUT /config, GET /ui/options, GET /dev/agents, POST /session (dev-авторизация, MP_DEV_LOGIN), Загрузка конфигурации интерфейса по контрольной сумме (304), Нефункциональные требования (безопасность, ПДн, карта, доступность, масштабирование) (+4 more)

### Community 33 - "Э1–Э2: итоги и ресурсы"
Cohesion: 0.24
Nodes (5): server/ — локальный API на Java (HttpServer + JDBC), Этап Э2: API v0.7 (39 методов, smoke 16/16), run_deal_jobs() — плановое задание раз в минуту, Этап Э8 — правка документации, Такси: маршрут 6,4 км, цена max(700, 150×6,4) = 960 тг

### Community 35 - "Дерево услуг и настройки"
Cohesion: 0.22
Nodes (8): Режимы сервиса: geo_mode, place_mode, price_unit, max_parallel_deals, slot_required, close_method, get_setting(service_id, code, rating), Таблица glossary_text (тексты на языке агента), Таблица service (дерево категория → сервис → вариант), Таблица service_setting (настройки по дереву и рейтингу), Представление v_service_search, GET /services, /services/{id}/form, /services/{id}/settings, Модель конфигурации «новый сервис — только данными» (пример «Эвакуатор»)

### Community 37 - "Профиль: ProfileViewModel"
Cohesion: 0.32
Nodes (5): Failed, Loaded, Loading, ProfileState, ProfileViewModel

### Community 38 - "Комплект документации v0.7"
Cohesion: 0.54
Nodes (8): Алгоритм формирования сделки v0.7, Журнал решений В-1…В-13, Н-1…Н-12, П-1…П-6, Архитектура БД mp_service v0.7 (29 таблиц), ТЗ на реализацию прототипа v0.7 (для Claude), Общая архитектура продукта v0.7, Комплект документации project_osm_map v0.7, ТЗ v0.6 (docs/TZ_marketplace.md), TZ_project_WIN_SERVICE.txt (исходный алгоритм)

### Community 40 - "ТЗ v0.6: основа"
Cohesion: 0.29
Nodes (6): Основной документ ТЗ в Claude Docs, Схема mp_service (27 таблиц, 10 представлений, 6 функций), Открытые вопросы ОВ-1…ОВ-24, ТЗ: платформа-маркетплейс услуг v0.6, TZ_project_WIN_SERVICE.txt (исходная модель), db/mp_service.sql (схема и тестовые данные)

### Community 41 - "Справочники и подпись OSM"
Cohesion: 0.29
Nodes (6): Триггер check_dict_type() (ОВ-22), Таблица dictionary (общий справочник, dict_type), Справочник MAP_BASE_LAYER / OSM_OFFLINE_ALMATY (ОВ-20), Ссылки на маршрут MAP_ROUTE_LINK (Яндекс, 2ГИС, Google), Подпись «© участники OpenStreetMap», v_deal_offer_route

### Community 42 - "Приёмка и тестирование"
Cohesion: 0.40
Nodes (6): Сценарии приёмки S-1…S-16, Определение готовности, Э6. Режим тестирования и закрытие, Э7. Тестирование на двух эмуляторах, Э8. Документация и фиксация, Сценарии U-1…U-5 (шаблоны, язык, 304, недоступность сервера, курсоры)

### Community 43 - "Антифрод и сроки"
Cohesion: 0.33
Nodes (5): 6) Антифрод: BYPASS_GEO, NEAR_CANCEL, FAKE_REFERRALS, блокировки, Статус EXPIRED, Статус NOT_CLOSED, Таблица agent_sanction (warning / rating_down / block), Схема антифрода (эпизоды → предупреждение → понижение → блокировка)

### Community 44 - "Маршруты ApiServer"
Cohesion: 0.67
Nodes (3): ApiServer, Handler, Route

### Community 45 - "Малая группа 45"
Cohesion: 0.50
Nodes (3): 4) Согласование (OFFERS → AGREED → CONTACT), Статус CONTACT (сближение ≤ near_radius_m), Представление v_deal_offer_list (смещение, distance_band)

### Community 47 - "Малая группа 47"
Cohesion: 0.67
Nodes (3): Справочник NOTIFY_TYPE (NEW_REQUEST, NEW_OFFER, AGREED, NEAR, CLOSE_REQUEST, CLOSE_CODE, …), Таблица notification, GET /notifications?since (опрос 2–5 с), GET /me

### Community 48 - "Малая группа 48"
Cohesion: 1.00
Nodes (3): CloseCode module (HMAC-SHA256, current+previous interval), Close endpoints (close/request, close/code, close, test-scan, test-provide), Schema CloseCode (QR/KEY, mpclose payload, ttl)

## Knowledge Gaps
- **76 isolated node(s):** `agent_rating(agent, role, service, on_date)`, `Индекс uq_deal_offer_selected (один selected на сделку)`, `Представление v_agent_available_service [chg]`, `Bearer security scheme`, `/me/position (gps/manual, manual only testing)` (+71 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 266 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **16 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Этап Э3: каркас Android-приложения` connect `План Э3: шаги и проверка` to `Приложение: модели API и тесты`, `Раскладки главной A/B/C`, `CLAUDE.md: договорённости`, `Структура проекта и README`, `Окружение: Kotlin, OSM, эмуляторы`, `Этапы Э3–Э4 и вопросы`?**
  _High betweenness centrality (0.288) - this node is a cross-community bridge._
- **What connects `agent_rating(agent, role, service, on_date)`, `Индекс uq_deal_offer_selected (один selected на сделку)`, `Представление v_agent_available_service [chg]` to the rest of the system?**
  _76 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Приложение: модели API и тесты` be split into smaller, more focused modules?**
  _Cohesion score 0.05047250859106529 - nodes in this community are weakly interconnected._
- **Why does `Этап Э3: каркас приложения на Compose выполнен (U-1…U-4, JVM-тесты 9/9)` connect `Этапы Э3–Э4 и вопросы` to `Э1–Э2: итоги и ресурсы`, `CLAUDE.md: договорённости`, `Структура проекта и README`, `План Э3: шаги и проверка`?**
  _High betweenness centrality (0.192) - this node is a cross-community bridge._
- **Should `Сервер: ApiServer и обработчики` be split into smaller, more focused modules?**
  _Cohesion score 0.10593505039193729 - nodes in this community are weakly interconnected._
- **Why does `Текущее состояние и решения` connect `CLAUDE.md: договорённости` to `Э1–Э2: итоги и ресурсы`, `Функции правил сделки в БД`, `Структура проекта и README`, `Окружение: Kotlin, OSM, эмуляторы`, `Этапы Э3–Э4 и вопросы`?**
  _High betweenness centrality (0.190) - this node is a cross-community bridge._
- **Should `Приложение: сеть и конфигурация` be split into smaller, more focused modules?**
  _Cohesion score 0.061952861952861954 - nodes in this community are weakly interconnected._