# Graph Report - github_repo  (2026-10-26)

## Corpus Check
- 2 files · ~0 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 1414 nodes · 3528 edges · 108 communities (60 shown, 48 thin omitted)
- Extraction: 94% EXTRACTED · 6% INFERRED · 0% AMBIGUOUS · INFERRED: 221 edges (avg confidence: 0.87)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- Сделка: экран и части раскладок
- Навигация и корень App
- Оболочка приложения и этап Э3
- Оболочка приложения и этап Э3 (2)
- Сервер: ConfigApi
- Тема MpTheme и шрифты
- Баллы и мастер запроса
- Маршрут: декодер polyline
- Раскладки главной (2)
- Запрос: RequestViewModel
- Запрос: поля формы
- Репозитории API
- Пиктограммы MpIcon
- Настройки интерфейса
- Сервер: утилиты и Auth
- Главная: HomeViewModel
- Модели API приложения
- Сервер: сессии Auth
- ApiServer: маршруты
- Мои сделки
- Сетевой клиент ApiClient
- Конфигурация ConfigRepository
- Сервер: утилиты и Auth (2)
- Профиль: ProfileViewModel
- Модели поставщика
- Уведомления NotificationCenter
- Тесты моделей API
- Тексты глоссария Texts
- Код закрытия CloseCode
- Тексты глоссария Texts (2)
- Карта MpMap
- Сервер: Db, Jobs и план Э4 (API)
- Сервер: зависимости и Routing
- HTTP-тест api_smoke.py
- Запрос: блоки формы
- Время периода Times
- Р-3 и план Э4
- Этап Э4: итоги и окружение
- CLAUDE.md и следующий этап Э5
- ТЗ v0.6: принципы и антифрод
- CLAUDE.md и следующий этап Э5 (2)
- Бэкап и продолжение сессии
- CLAUDE.md и следующий этап Э5 (3)
- Этап Э4: итоги и окружение (2)
- ТЗ v0.6: разделы
- Малая группа 115
- Малая группа 116
- Малая группа 118
- Малая группа 119
- Малая группа 120
- Малая группа 121
- Этапы Э0–Э2 и договорённости
- Сделка: экран и части раскладок (2)
- Эмуляторы и HTTP-проверки
- Сроки и антифрод
- Таблицы сделки и миграция
- История этапов
- Подбор поставщиков и такси
- Сохранность работы и запас токенов
- Часовой пояс и вопрос Р-4
- Роли и тестовый режим
- Этапы алгоритма сделки
- Этапы Э0–Э2 и договорённости (2)
- Закрытие и отмена в алгоритме
- Комплект документации v0.7
- Малая группа 59
- Малая группа 60
- Планы Э1–Э2 и тестовые пользователи
- Малая группа 63
- Малая группа 65
- Малая группа 66
- Малая группа 68
- Малая группа 69
- Малая группа 71
- Малая группа 72
- Малая группа 73
- Малая группа 74
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

## God Nodes (most connected - your core abstractions)
1. `DealApi` - 34 edges
2. `Request` - 31 edges
3. `Response` - 29 edges
4. `DealState` - 25 edges
5. `Deal` - 25 edges
6. `Этап Э3: каркас Android-приложения` - 25 edges
7. `RequestState` - 24 edges
8. `План этапа Э4 — экраны клиента` - 24 edges
9. `RequestViewModel` - 23 edges
10. `AppGraph` - 22 edges

## Surprising Connections (you probably didn't know these)
- `Функция БД deal_cancel_mode(deal_id, agent_id) → CANCEL_FREE / CLIENT / PROVIDER / NULL` --semantically_similar_to--> `БД: deal_cancel_mode — итог отмены`  [INFERRED] [semantically similar]
  project_osm_map/docs/v0.7/plans/E4_client.md → CLAUDE.md
- `Этап Э3: каркас Android-приложения` --semantically_similar_to--> `Этап Э3: каркас приложения на Compose — вход, главная A/B/C, настройки, профиль (U-1…U-4)`  [INFERRED] [semantically similar]
  project_osm_map/docs/v0.7/plans/E3_app.md → CLAUDE.md
- `Термины ошибок ui.err.* (review_required, no_service_access, required_fields, offer_not_active, busy, routing_unavailable, no_route, rule)` --semantically_similar_to--> `24 термина глоссария ui.* / ui.err.*`  [INFERRED] [semantically similar]
  project_osm_map/docs/v0.7/plans/E4_client.md → CLAUDE.md
- `Р-3 (Э4): какие сервисы на главной — решено: все сервисы всем агентам` --semantically_similar_to--> `Р-3 решён: на главной все сервисы всем агентам`  [INFERRED] [semantically similar]
  project_osm_map/docs/v0.7/03_implementation_spec.md → CLAUDE.md
- `Kotlin 2.4.21 вместо встроенного 2.2.10` --semantically_similar_to--> `Kotlin 2.4.21 вместо встроенного в AGP 9 2.2.10 (buildscript)`  [INFERRED] [semantically similar]
  project_osm_map/docs/v0.7/plans/E3_app.md → CLAUDE.md

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Состав ежечасного среза** — claude_hourly_backup, claude_backup_repo_bundle, claude_backup_db_dump, claude_backup_info_txt [EXTRACTED 1.00]
- **Сохранность работы: коммиты, срезы, запас токенов** — claude_work_safety_rules, claude_commit_after_green_step, claude_manual_backup_before_risky, claude_token_budget_rule, claude_get_usage [EXTRACTED 1.00]
- **Конфигурация сервиса данными (дерево, настройки, поля, глоссарий)** — project_osm_map_docs_tz_marketplace_service_tree, project_osm_map_docs_tz_marketplace_service_setting, project_osm_map_docs_tz_marketplace_service_attribute, project_osm_map_docs_tz_marketplace_glossary, project_osm_map_docs_tz_marketplace_get_setting [EXTRACTED 1.00]
- **Правила сделки, проверяемые БД (переходы, подбор, отзывы, один selected)** — project_osm_map_docs_v0_7_02_db_architecture_trg_deal_status, project_osm_map_docs_v0_7_02_db_architecture_trg_deal_offer_check, project_osm_map_docs_v0_7_02_db_architecture_provider_can_offer, project_osm_map_docs_v0_7_02_db_architecture_trg_deal_review, project_osm_map_docs_v0_7_02_db_architecture_uq_deal_offer_selected, project_osm_map_docs_v0_7_02_db_architecture_dict_deal_transition [EXTRACTED 1.00]
- **Этапы алгоритма сделки: инициация → подбор → отклик → согласование → закрытие** — project_osm_map_docs_v0_7_01_deal_algorithm_stage_initiation, project_osm_map_docs_v0_7_01_deal_algorithm_stage_matching, project_osm_map_docs_v0_7_01_deal_algorithm_stage_offer, project_osm_map_docs_v0_7_01_deal_algorithm_stage_agreement, project_osm_map_docs_v0_7_01_deal_algorithm_stage_closing [EXTRACTED 1.00]
- **Правила сделки в БД: функции, триггеры, переходы, плановое задание** — project_osm_map_docs_v0_7_plans_e1_db_provider_can_offer, project_osm_map_docs_v0_7_plans_e1_db_deal_agree, project_osm_map_docs_v0_7_plans_e1_db_deal_cancel, project_osm_map_docs_v0_7_plans_e1_db_run_deal_jobs, project_osm_map_docs_v0_7_plans_e1_db_trg_deal_status, project_osm_map_docs_v0_7_plans_e1_db_deal_transition [EXTRACTED 1.00]
- **E2 server module split (ApiServer, Db, Http, Auth, ConfigApi, DealApi, ProviderApi, Routing, CloseCode, Jobs)** — project_osm_map_docs_v0_7_plans_e2_api_apiserver, project_osm_map_docs_v0_7_plans_e2_api_db, project_osm_map_docs_v0_7_plans_e2_api_http, project_osm_map_docs_v0_7_plans_e2_api_auth, project_osm_map_docs_v0_7_plans_e2_api_configapi, project_osm_map_docs_v0_7_plans_e2_api_dealapi, project_osm_map_docs_v0_7_plans_e2_api_providerapi, project_osm_map_docs_v0_7_plans_e2_api_routing, project_osm_map_docs_v0_7_plans_e2_api_closecode, project_osm_map_docs_v0_7_plans_e2_api_jobs [EXTRACTED 1.00]
- **Интерфейс из конфигурации: кэш по сумме → тема, тексты, пиктограммы** — project_osm_map_docs_v0_7_plans_e3_app_config_checksum_cache, project_osm_map_docs_v0_7_plans_e3_app_theme_palette, project_osm_map_docs_v0_7_plans_e3_app_texts_glossary, project_osm_map_docs_v0_7_plans_e3_app_svg_icons, project_osm_map_docs_v0_7_plans_e3_app_settings_feature [EXTRACTED 1.00]
- **Одна логика — три раскладки откликов и сделки (A/B/C)** — project_osm_map_docs_v0_7_plans_e4_client_principle_three_layouts, project_osm_map_app_src_main_java_ru_mpavel_osmmap_feature_deal_dealviewmodel_dealviewmodel, project_osm_map_app_src_main_java_ru_mpavel_osmmap_ui_template_a_deallayoutsa_offersa, project_osm_map_app_src_main_java_ru_mpavel_osmmap_ui_template_b_deallayoutsb_offersb, project_osm_map_app_src_main_java_ru_mpavel_osmmap_ui_template_c_deallayoutsc_offersc, project_osm_map_app_src_main_java_ru_mpavel_osmmap_ui_template_a_deallayoutsa_deala, project_osm_map_app_src_main_java_ru_mpavel_osmmap_ui_template_b_deallayoutsb_dealb, project_osm_map_app_src_main_java_ru_mpavel_osmmap_ui_template_c_deallayoutsc_dealc [EXTRACTED 1.00]
- **Вход тестовых пользователей по хешу пароля** — claude_test_users, project_osm_map_docs_v0_7_plans_e1_db_step_12a_test_users, project_osm_map_docs_v0_7_plans_e1_db_agent_login, project_osm_map_docs_v0_7_plans_e1_db_agent_login_check [EXTRACTED 1.00]
- **Меры против обхода платформы до согласования** — project_osm_map_docs_tz_marketplace_approx_offset, project_osm_map_docs_tz_marketplace_pseudonym, project_osm_map_docs_tz_marketplace_show_stage_near, project_osm_map_docs_tz_marketplace_antifraud [INFERRED 0.85]
- **Режим отмены: одно правило БД → поле API → диалог отмены** — project_osm_map_docs_v0_7_plans_e4_client_deal_cancel_mode, project_osm_map_server_openapi_deal_cancel_mode, project_osm_map_server_openapi_cancel, project_osm_map_app_src_main_java_ru_mpavel_osmmap_feature_deal_dealscreen_canceldialog, project_osm_map_docs_v0_7_plans_e4_client_test_02_cancel_mode [INFERRED 0.85]
- **Продолжение работы после обрыва сессии** — claude_session_resume_procedure, claude_сейчас, claude_progress_tracking_rule, claude_hourly_backup, claude_commit_after_green_step [INFERRED 0.85]
- **Одна логика — три раскладки A/B/C** — project_osm_map_docs_v0_7_plans_e3_app_principle_one_logic_three_layouts, project_osm_map_app_src_main_java_ru_mpavel_osmmap_ui_template_a_homea_homea, project_osm_map_app_src_main_java_ru_mpavel_osmmap_ui_template_b_homeb_homeb, project_osm_map_app_src_main_java_ru_mpavel_osmmap_ui_template_c_homec_homec [INFERRED 0.85]
- **Taxi on-the-way matching (server-side routing, slots, requests, forecast)** — project_osm_map_docs_v0_7_plans_e2_api_routing, project_osm_map_docs_v0_7_plans_e2_api_graphhopper, project_osm_map_server_openapi_slots, project_osm_map_server_openapi_requests, project_osm_map_server_openapi_forecast [INFERRED 0.85]
- **Время без часового пояса: истечение запросов и вопрос Р-4** — project_osm_map_docs_v0_7_plans_e4_client_timezone, project_osm_map_docs_v0_7_03_implementation_spec_question_r4, claude_emulator_timezone, project_osm_map_readme_timezone, claude_question_time_offset [INFERRED 0.95]

## Communities (108 total, 48 thin omitted)

### Community 0 - "Сделка: экран и части раскладок"
Cohesion: 0.10
Nodes (34): Offer, DealActions, DealEvent, DealState, DealViewModel, Review, CancelDialog(), DealActionsRow() (+26 more)

### Community 1 - "Навигация и корень App"
Cohesion: 0.06
Nodes (25): AppState, AppViewModel, DealRoute, DealsRoute, Failed, HomeRoute, LoggedOut, Nav (+17 more)

### Community 11 - "Оболочка приложения и этап Э3"
Cohesion: 0.10
Nodes (22): MainActivity, Конфигурация с кэшем по контрольной сумме (GET /config?checksum= → 304/200), Модели и репозитории data/ (@Serializable по openapi.yaml, тонкие репозитории), Вход feature/login/ (GET /dev/agents при MP_DEV_LOGIN=1, иначе POST /session), Сетевой слой core/net/ (ApiClient, ApiError, NetworkError, SessionStore), Карта ui/map/ MpMap (osmdroid, зона, маркеры, подпись © OSM), Профиль feature/profile/ (GET /me, только просмотр), Настройки feature/settings/ (шаблон/схема/язык → PUT /config) (+14 more)

### Community 12 - "Оболочка приложения и этап Э3 (2)"
Cohesion: 0.10
Nodes (12): ApiError, AppError, DecodeError, ErrorBody, HttpResult, NetworkError, LoginState, LoginViewModel (+4 more)

### Community 13 - "Сервер: ConfigApi"
Cohesion: 0.30
Nodes (3): ConfigApi, Request, Response

### Community 14 - "Тема MpTheme и шрифты"
Cohesion: 0.13
Nodes (9): Mp, MpColors, MpFonts, MpShapes, Scheme, findActivity(), googleFamily(), MpTheme() (+1 more)

### Community 15 - "Баллы и мастер запроса"
Cohesion: 0.14
Nodes (10): ReviewState, ReviewViewModel, AppGraph, ReviewScreen(), ErrorBox(), errorMessage(), LoadingBox(), textOr() (+2 more)

### Community 18 - "Маршрут: декодер polyline"
Cohesion: 0.14
Nodes (8): FormField, ClientFlowTest, decodePolyline(), FieldInput(), invalidFields(), apiErrorText(), errorKey(), Термины ошибок ui.err.* (review_required, no_service_access, required_fields, offer_not_active, busy, routing_unavailable, no_route, rule)

### Community 19 - "Раскладки главной (2)"
Cohesion: 0.27
Nodes (16): DealListItem, MapSlot, PendingReview, ServiceNode, HomeActions, HomeState, DealsSummary(), HomeScreen() (+8 more)

### Community 20 - "Запрос: RequestViewModel"
Cohesion: 0.12
Nodes (8): Created, RequestEvent, RequestViewModel, ReviewFirst, RouteTarget, DEST, PICKUP, Шаг 5: Запрос — гео point / route / none, поля формы, прогноз через 500 мс, «Моя цена» = медиана

### Community 21 - "Запрос: поля формы"
Cohesion: 0.39
Nodes (13): RequestActions, RequestState, FieldsBlock(), PriceBlock(), RequestMap(), RequestScreen(), RouteTargetToggle(), ServiceBlock() (+5 more)

### Community 23 - "Репозитории API"
Cohesion: 0.17
Nodes (5): CloseCode, DealRepository, NotificationRepository, ServiceRepository, enc()

### Community 24 - "Пиктограммы MpIcon"
Cohesion: 0.13
Nodes (4): IconSet, hex(), MpIcon(), svgBitmap()

### Community 25 - "Настройки интерфейса"
Cohesion: 0.15
Nodes (11): ConfigUpdate, Language, UiOption, UiOptions, SchemeFilter, SettingsState, SettingsViewModel, SettingsScreen() (+3 more)

### Community 27 - "Сервер: утилиты и Auth"
Cohesion: 0.19
Nodes (4): ApiServer, Handler, Route, Http

### Community 3 - "Модели API приложения"
Cohesion: 0.10
Nodes (28): AttrValue, BaseLayer, CancelRequest, CancelResult, CloseSubmit, Deal, DealCreate, DevLoginRequest (+20 more)

### Community 31 - "Сервер: сессии Auth"
Cohesion: 0.24
Nodes (4): Db, Work, Jobs, ProviderApi

### Community 32 - "ApiServer: маршруты"
Cohesion: 0.20
Nodes (3): Projection, Route, Routing

### Community 33 - "Мои сделки"
Cohesion: 0.22
Nodes (7): DealsState, DealsViewModel, Failed, Loaded, Loading, DealsScreen(), Шаг 8: «Мои сделки» — GET /deals?role=all&status=active

### Community 34 - "Сетевой клиент ApiClient"
Cohesion: 0.17
Nodes (3): ApiClient, SessionStore, SessionRepository

### Community 35 - "Конфигурация ConfigRepository"
Cohesion: 0.25
Nodes (4): ConfigRepository, CancelReason, UiConfig, UserConfig

### Community 37 - "Профиль: ProfileViewModel"
Cohesion: 0.25
Nodes (8): Failed, Loaded, Loading, ProfileState, ProfileViewModel, ProfileContent(), ProfileScreen(), roleKey()

### Community 38 - "Модели поставщика"
Cohesion: 0.16
Nodes (7): IncomingRequest, PositionUpdate, ServiceSettingsUpdate, Slot, SlotCreate, StatusOk, ProviderRepository

### Community 4 - "Уведомления NotificationCenter"
Cohesion: 0.06
Nodes (8): NotificationCenter, MpApp, ApiServer (routes, lang, DB errors to HTTP), Jobs module (run_deal_jobs every minute, taxi route requests), DB rule functions (provider_can_offer, deal_agree, deal_cancel, make_auto_offers, price_forecast, run_deal_jobs, agent_login_check), Error model {error, code} with HTTP 400/401/403/404/409/422, GET /notifications?since (опрос 2–5 с), GET /me, /notifications (since, read)

### Community 40 - "Тесты моделей API"
Cohesion: 0.31
Nodes (3): DevAgent, ApiModelsTest, decodeBody()

### Community 47 - "Тексты глоссария Texts"
Cohesion: 0.27
Nodes (3): CoreTest, tintSvg(), parseHexColor()

### Community 6 - "Карта MpMap"
Cohesion: 0.08
Nodes (8): LatLon, MapLine, MapMarker, MapZone, MapEventsReceiver, UrlTileSource, applyBaseLayer(), MpMap()

### Community 7 - "Сервер: Db, Jobs и план Э4 (API)"
Cohesion: 0.15
Nodes (8): CodeParams, DealApi, Поле Deal.cancel_mode (CANCEL_FREE / CLIENT / PROVIDER, null — отменить нельзя), Шаг 2: API — поле cancel_mode в DealApi.SQL_DEAL и схеме Deal, Проверка по HTTP: server/tests/api_smoke.py, /deals/{dealId}/cancel (итог решает deal_cancel: CANCEL_FREE / CLIENT / PROVIDER), Схема Deal (status, actions, cancel_mode, counterpart_fields по show_stage), /deals/{dealId} (сделка: статус, действия, открытые поля)

### Community 17 - "HTTP-тест api_smoke.py"
Cohesion: 0.18
Nodes (17): call(), check(), main(), agree(), close(), config(), contact(), create() (+9 more)

### Community 45 - "Время периода Times"
Cohesion: 0.26
Nodes (6): defaultPeriod(), formatPeriod(), formatShort(), parseServerTime(), toServerTime(), DealRow()

### Community 46 - "Р-3 и план Э4"
Cohesion: 0.23
Nodes (10): Р-3 (Э4): какие сервисы на главной — решено: все сервисы всем агентам, coreLibraryDesugaring desugar_jdk_libs 2.1.5 (java.time при minSdk 24), Цель Э4: путь «услуга → запрос → отклики → согласование → сделка → отмена или баллы» в A/B/C, Схема Me (роли, pending_reviews, сервисы), Э3. Каркас Android-приложения (Compose), Э4. Экраны клиента, План этапа Э4 — экраны клиента, Вопросы владельцу В-Э4-1…В-Э4-4 и решение 25.10.2026 (+2 more)

### Community 51 - "Этап Э4: итоги и окружение"
Cohesion: 0.22
Nodes (9): Правило БД: COMMENT ON для каждого объекта, поля и функции (проверка по pg_attribute), SQL-тесты db/tests/01_rules.sql и 02_cancel_mode.sql — на свежезагруженной БД (кейс A/B/C/D), Тест db/tests/02_cancel_mode.sql, Тестовые данные: кейс A/B/C/D (сделка 6 — парковка, сделка 7 — такси), Функция БД deal_cancel_mode(deal_id, agent_id) → CANCEL_FREE / CLIENT / PROVIDER / NULL, Э1. БД v0.7, Шаг 1: БД — deal_cancel_mode и термины ui.* / ui.err.*, Тест db/tests/02_cancel_mode.sql (FREE до согласования и в окне, CLIENT/PROVIDER позже, CLOSED — NULL) (+1 more)

### Community 22 - "CLAUDE.md и следующий этап Э5"
Cohesion: 0.12
Nodes (18): О проекте, Структура, Текущее состояние и решения, HTTP-тест server/tests/api_smoke.py (MP_TEST_PASSWORD, сервер запущен), Структура app/ на Compose: core/, data/, feature/, ui/ (шаблоны a|b|c), Репозиторий github.com/mpavel-9999/mp_- (владелец — Павел), Kotlin 2.4.21 вместо встроенного в AGP 9 2.2.10 (buildscript), server/ — локальный API на Java (HttpServer + JDBC + GraphHopper), 39 методов, openapi.yaml (+10 more)

### Community 2 - "ТЗ v0.6: принципы и антифрод"
Cohesion: 0.05
Nodes (42): Условия подбора поставщика, Антифрод и agent_sanction (эскалация санкций), Кейс A/B/C/D (сделка 6, порядок B, D, C), Триггер check_dict_type() (ОВ-22), Сделка deal, Отмена сделки (CANCEL_FREE/CLIENT/PROVIDER, EXPIRED), Отклик deal_offer, Статусы сделки INIT→MATCHING→OFFERS→AGREED→CONTACT→CLOSED (+34 more)

### Community 26 - "CLAUDE.md и следующий этап Э5 (2)"
Cohesion: 0.15
Nodes (18): Android SDK: emulator.exe, adb.exe, Текущее состояние: Э4 завершён, Э5 (экраны поставщика) не начат, план Э5 не написан, БД: deal_cancel_mode — итог отмены, JAVA_HOME = JBR Android Studio для Gradle и сервера (java нет в PATH), Следующее действие: ответ по Р-4 → план E5_provider.md → согласование, Python не в PATH: ~/.local/bin/python3.15.exe; для pyyaml/PIL — anaconda3, Вопрос: отправить ветку в GitHub (копия вне компьютера) — по просьбе владельца, Р-4 (открыт): время в API без часового пояса — отклонять начало в прошлом и/или передавать смещение +05:00 (+10 more)

### Community 30 - "Бэкап и продолжение сессии"
Cohesion: 0.15
Nodes (13): mp_service.sql — дамп схемы БД (при заданном MP_DB_PASSWORD / PGPASSWORD / pgpass.conf), Срезы в C:\MP\Backups\github_repo\<ГГГГММДД-ЧЧмм>\, журнал backup.log, repo.bundle — ветки и срез рабочей копии (ссылка refs/backup/<время>), в GitHub ничего не уходит, Хранение срезов: 48 ч, первый срез дня — 14 дней; без изменений срез не создаётся; при C: < 20 ГБ пропуск, tools/backup/hourly_backup.ps1 — скрипт ежечасного среза, Переменные MP_DB_PASSWORD, MP_GEO_SECRET, MP_DEV_LOGIN, MP_TEST_PASSWORD — пароли только в переменных, Ежечасный срез репозитория и БД, Перед долгой или рискованной операцией (перезагрузка БД, массовая правка, импорт) — коммит или ручной срез hourly_backup.ps1 (+5 more)

### Community 49 - "CLAUDE.md и следующий этап Э5 (3)"
Cohesion: 0.24
Nodes (11): Договорённости, info.txt — ветка, HEAD, незакоммиченные изменения, Каталог планов docs/v0.7/plans (итоги и снимки экранов E<n>_screens/), Правило: ход выполнения в плане (раздел «Ход выполнения»: шаг, дата, результат проверки) + обновление «Сейчас»; итог — раздел «Итог», Договорённость: следить за ресурсами системы (ОЗУ ≥ 6 ГБ, C: ≥ 20 ГБ, ЦП < 80 %), останавливать свои процессы, без параллельных тяжёлых задач, Договорённость: сообщать, когда нужен ответ владельца, Договорённость: детальный план + проверка логической связанности + согласование перед реализацией, Договорённость: общение и коммиты на русском (+3 more)

### Community 64 - "Этап Э4: итоги и окружение (2)"
Cohesion: 0.33
Nodes (5): API, Ошибки {"error","code"}; нарушение правил сделки — 409 с кодом причины, Коды ошибок API (REQUIRED_FIELDS, NO_SERVICE_ACCESS, BUSY, OFFER_NOT_ACTIVE, REVIEW_REQUIRED, NO_ROUTE, ROUTING_UNAVAILABLE…), Приложение под API v0.7: Э3 каркас, Э4 экраны клиента, Э5 поставщик, Э6 закрытие, Schema Error {error, code}

### Community 9 - "ТЗ v0.6: разделы"
Cohesion: 0.05
Nodes (36): Антифрод, Жизненный цикл подписки, Изменения относительно исходной модели, Как добавить сервис, Карта в приложении, Кейс A/B/C/D, Модель данных, Монетизация (+28 more)

### Community 16 - "Этапы Э0–Э2 и договорённости"
Cohesion: 0.12
Nodes (19): Таблица agent_login (хеш bcrypt через pgcrypto), agent_login_check(login, password) — crypt() pgcrypto, deal_agree(), trg_agent_testing / trg_agent_service_assign / trg_agent_position, Шаг 3: функции правил сделки, run_deal_jobs() — плановое задание раз в минуту, trg_deal_status / trg_deal_status_after, Шаг 4: триггеры (+11 more)

### Community 39 - "Сделка: экран и части раскладок (2)"
Cohesion: 0.14
Nodes (13): DealApi module (client), ProviderApi module, Routing module (GraphHopper 10.2, car profile, on-the-way check), GraphHopper + kazakhstan-latest.osm.pbf (gh-cache), Схема DealListItem (статус, роль, откликов, контрагент, review_pending), Схема Forecast (price_median, route_length_m, route_polyline), /deals/{dealId}/offers (list, manual offer), /deals (create request INIT->MATCHING, list) (+5 more)

### Community 41 - "Эмуляторы и HTTP-проверки"
Cohesion: 0.15
Nodes (13): api_smoke.py HTTP smoke test (16/16), Кириллицу через adb shell input text не ввести — вводить латиницу и цифры, adb reverse tcp:8080 tcp:8080, Эмуляторы: 5554 — user_testing_1 (агент 7, клиент), 5556 — user_testing_2 (агент 8, поставщик), adb из Git Bash: MSYS_NO_PATHCONV=1, Команды запуска: БД с нуля, сервер (gradlew :server:run, /api/health), эмуляторы, установка приложения, тесты, Тестовые пользователи user_testing_1…5 (агенты 7–11; в репозитории только хеш пароля), Перезапуск сервера сбрасывает токены (в памяти) → 401 и возврат на вход (+5 more)

### Community 42 - "Сроки и антифрод"
Cohesion: 0.17
Nodes (11): 6) Антифрод: BYPASS_GEO, NEAR_CANCEL, FAKE_REFERRALS, блокировки, Статус EXPIRED, Статус NOT_CLOSED, Плановые задания (раз в минуту): истечение, CLOSING_ASK, NOT_CLOSED, сближение, публикации, Таблица agent_sanction (warning / rating_down / block), Модуль: Android-приложение (Kotlin, Jetpack Compose), Модуль: API-сервер (Java, HttpServer + JDBC), Схема антифрода (эпизоды → предупреждение → понижение → блокировка) (+3 more)

### Community 43 - "Таблицы сделки и миграция"
Cohesion: 0.17
Nodes (12): Триггер trg_agent_position (manual только при is_testing), Триггер trg_deal_offer_check, Индекс uq_deal_offer_selected (один selected на сделку), Представление v_deal_candidates, Представление v_provider_slot_map, Миграция v0.6 → v0.7, Таблица agent_position [new], Таблица deal (+4 more)

### Community 44 - "История этапов"
Cohesion: 0.18
Nodes (12): 2026-10-26: ежечасный бэкап, правила продолжения после обрыва, CLAUDE.md переструктурирован, 2026-10-19: создан CLAUDE.md, Офлайн-карта: только растровые MBTiles (скачивание с CDN — заглушка), kazakhstan-latest.osm.pbf (214 МБ) и кэш gh-cache для маршрутов (в .gitignore), Р-3 решён: на главной все сервисы всем агентам, Решены Р-1…Р-3, В-Э3-*, В-Э4-* (OSM, библиотеки, сервисы на главной), Функции схемы закреплены SET search_path = mp_service, Этап Э0: второй эмулятор Medium_Phone_B и данные OSM для такси (+4 more)

### Community 48 - "Подбор поставщиков и такси"
Cohesion: 0.18
Nodes (8): agent_rating(agent, role, service, on_date), provider_can_offer(deal_id, agent_id) → (ok, reason), Триггер trg_agent_service_assign (субагент), Представление v_agent_available_service [chg], Сценарий: частная парковка (capacity), 2) Подбор поставщиков (MATCHING), Таблица agent_service (цены, тариф, capacity, назначение), Схема подбора такси по пути

### Community 50 - "Сохранность работы и запас токенов"
Cohesion: 0.20
Nodes (11): Локальный коммит после каждого шага плана с пройденной проверкой («Э<n> шаг <k>: …»), Пороги контекста: ≥70 % — коммит после каждого шага; ≥85 % — не начинать крупных шагов, предложить новую сессию; 97 % — автосжатие, Инструмент get_usage (контекст сессии и лимиты тарифа): в начале этапа, перед долгой операцией, ~каждые 10 шагов, Правило: обновлять граф graphify-out в конце этапа и коммитить вместе с CLAUDE.md, graphify-out — граф знаний проекта (исключения — .graphifyignore), Лимиты тарифа: 5-часовой ≥80 % или недельный ≥90 % — коммит, «Сейчас», предупредить владельца, Вопрос: установить graphifyy[sql] для разбора SQL в графе — ждёт решения владельца, Договорённость: в конце дня с доработками — версия графа и синхронизация с Git (+3 more)

### Community 52 - "Часовой пояс и вопрос Р-4"
Cohesion: 0.20
Nodes (11): Эмулятор в поясе Алматы: adb shell cmd alarm set-timezone Asia/Almaty, Вопрос Р-4 (Э5): время без пояса — отклонять начало в прошлом и/или передавать +05:00, Границы Э4 (поставщик — Э5; GPS, курсор и закрытие кодом — Э6), Наблюдение: отклик такси «0 м · примерно» — убрать пометку для маршрутных (Э5), Наблюдение: часовой пояс — эмулятор в UTC, запросы сразу истекают; предложение +05:00, API v0.7 (http://127.0.0.1:8080/api), Открытые вопросы реализации Р-1…Р-4, Э5. Экраны поставщика (+3 more)

### Community 53 - "Роли и тестовый режим"
Cohesion: 0.18
Nodes (11): Триггер trg_agent_testing (is_testing только при ADMIN), Представление v_map_cursor, Роли агентов: CLIENT, PROVIDER, SUBAGENT, COMPANY, ADMIN, Режим тестирования администратора, Справочник MAP_CURSOR (капля / капля с шестерёнкой), Таблица agent (is_testing), Взаимодействие: сближение и закрытие, Взаимодействие: закрытие без камеры в режиме тестирования (+3 more)

### Community 54 - "Этапы алгоритма сделки"
Cohesion: 0.18
Nodes (8): agent_pending_reviews(agent_id), price_forecast(service_id, lat, lon, period_start), Триггер trg_deal_review, Публикация (окно) поставщика (Н-2), Сценарий: парковка — публикация места, 1) Инициация — клиент создаёт запрос (INIT → MATCHING), Таблица deal_review, Клиент: /map/slots, POST /forecast, POST /deals, /deals/{id}/offers, /agree, /routes

### Community 55 - "Этапы Э0–Э2 и договорённости (2)"
Cohesion: 0.24
Nodes (8): provider_can_offer(), db/tests/01_rules.sql — SQL-тесты (29/29), Сессия API задаёт mp.agent_id и mp.geo_secret, Правила сделки — в БД; приложение показывает действия из Deal.actions, Этап Э1: БД mp_service v0.7 — 30 таблиц, правила сделки в БД (план E1_db.md), Кейс A/B/C/D: порядок откликов B, D, C; E отсечён правилом В-1, Итог Э1: PostgreSQL 17.6, 30 таблиц, 14 представлений, 35 функций, 17 триггеров, Шаг 12: тестовые сделки 1–7 (парковка A/B/C/D, такси)

### Community 58 - "Закрытие и отмена в алгоритме"
Cohesion: 0.22
Nodes (7): check_dict_type() + 9 триггеров, Триггер trg_deal_status, Сценарий: онлайн-услуга (без CONTACT, ключ KEY), 5) Закрытие сделки, Справочник DEAL_TRANSITION [new], Таблица deal_status_log (пишет только триггер), Общий справочник dictionary (value_json)

### Community 59 - "Комплект документации v0.7"
Cohesion: 0.46
Nodes (8): Журнал решений В-1…В-13, Н-1…Н-12, П-1…П-6, Алгоритм формирования сделки v0.7, Архитектура БД mp_service v0.7 (29 таблиц), ТЗ на реализацию прототипа v0.7 (для Claude), Общая архитектура продукта v0.7, Комплект документации project_osm_map v0.7, ТЗ v0.6 (docs/TZ_marketplace.md), TZ_project_WIN_SERVICE.txt (исходный алгоритм)

### Community 60 - "Малая группа 59"
Cohesion: 0.33
Nodes (5): get_setting(service_id, code, rating), Режимы сервиса: geo_mode, place_mode, price_unit, max_parallel_deals, slot_required, close_method, Таблица service (дерево категория → сервис → вариант), Таблица service_setting (настройки по дереву и рейтингу), Модель конфигурации «новый сервис — только данными» (пример «Эвакуатор»)

### Community 61 - "Малая группа 60"
Cohesion: 0.29
Nodes (7): Представление v_ui_options, Справочник UI_TEMPLATE (A, B, C) и UI_SCHEME (LIGHT/DARK), Формат JSON шаблона интерфейса и палитры, Нефункциональные требования (безопасность, ПДн, карта, доступность, масштабирование), Шаблон A «Карта в центре» (Manrope), Шаблон B «Карточки» (Golos Text), Шаблон C «Диспетчер» (Onest + JetBrains Mono)

### Community 62 - "Планы Э1–Э2 и тестовые пользователи"
Cohesion: 0.29
Nodes (7): E2 plan: API v0.7, 02_db_architecture.md (sections 6.3, 8), E1_db.md (E1 plan), 03_implementation_spec.md (TZ v0.7, sections 4 and 6), openapi.yaml - mp_service API 0.7.0, /deals/{dealId}/routes (ссылки на маршрут после согласования), /services/{serviceId}/form и /settings (поля и настройки узла)

### Community 63 - "Малая группа 63"
Cohesion: 0.33
Nodes (4): Представление v_deal_offer_list (смещение, distance_band), 4) Согласование (OFFERS → AGREED → CONTACT), 3) Выбор — поставщик откликается (MATCHING → OFFERS), Статус CONTACT (сближение ≤ near_radius_m)

### Community 65 - "Малая группа 65"
Cohesion: 0.40
Nodes (3): Проверка на тестовых данных 25.10.2026 (расхождения v0.6), Модуль: PostgreSQL mp_service, Функциональные блоки (каталог, сделки, гео, закрытие, антифрод, конфигурация UI…)

### Community 66 - "Малая группа 66"
Cohesion: 0.50
Nodes (4): Таблица ui_config (config_json, checksum md5), Взаимодействие: загрузка конфигурации интерфейса, GET/PUT /config, GET /ui/options, Загрузка конфигурации интерфейса по контрольной сумме (304)

### Community 68 - "Малая группа 68"
Cohesion: 0.67
Nodes (3): Auth module (in-memory bearer tokens 12h, dev login), Bearer security scheme, /session (login, logout)

## Knowledge Gaps
- **144 isolated node(s):** `LoggedOut`, `Ready`, `Starting`, `Loading`, `DEALS` (+139 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 365 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **48 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `План этапа Э4 — экраны клиента` connect `Р-3 и план Э4` to `Сделка: экран и части раскладок`, `Мои сделки`, `Уведомления NotificationCenter`, `Сервер: Db, Jobs и план Э4 (API)`, `Эмуляторы и HTTP-проверки`, `Оболочка приложения и этап Э3`, `Баллы и мастер запроса`, `CLAUDE.md и следующий этап Э5 (3)`, `Маршрут: декодер polyline`, `Этап Э4: итоги и окружение`, `Часовой пояс и вопрос Р-4`, `Запрос: RequestViewModel`, `CLAUDE.md и следующий этап Э5 (2)`, `Комплект документации v0.7`?**
  _High betweenness centrality (0.130) - this node is a cross-community bridge._
- **What connects `LoggedOut`, `Ready`, `Starting` to the rest of the system?**
  _144 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Сделка: экран и части раскладок` be split into smaller, more focused modules?**
  _Cohesion score 0.10357304387155133 - nodes in this community are weakly interconnected._
- **Why does `Шаг 5: Запрос — гео point / route / none, поля формы, прогноз через 500 мс, «Моя цена» = медиана` connect `Запрос: RequestViewModel` to `Сделка: экран и части раскладок`, `Сделка: экран и части раскладок (2)`, `Р-3 и план Э4`, `Маршрут: декодер polyline`, `Запрос: поля формы`, `Этапы алгоритма сделки`, `Планы Э1–Э2 и тестовые пользователи`?**
  _High betweenness centrality (0.102) - this node is a cross-community bridge._
- **Should `Навигация и корень App` be split into smaller, more focused modules?**
  _Cohesion score 0.0593990216631726 - nodes in this community are weakly interconnected._
- **Why does `OSM Карта — экран сделки маркетплейса услуг` connect `ТЗ v0.6: разделы` to `Этап Э4: итоги и окружение (2)`?**
  _High betweenness centrality (0.092) - this node is a cross-community bridge._
- **Should `Оболочка приложения и этап Э3` be split into smaller, more focused modules?**
  _Cohesion score 0.09915966386554621 - nodes in this community are weakly interconnected._