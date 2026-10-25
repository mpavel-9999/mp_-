# Graph Report - github_repo  (2026-10-26)

## Corpus Check
- 3 files · ~0 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 1521 nodes · 3758 edges · 116 communities (66 shown, 50 thin omitted)
- Extraction: 94% EXTRACTED · 6% INFERRED · 0% AMBIGUOUS · INFERRED: 221 edges (avg confidence: 0.87)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- Сделка: экран и части раскладок
- ТЗ v0.6: принципы и антифрод
- Раскладки главной (2)
- Модели API приложения
- Сервер: Db, Jobs и план Э4 (API)
- Карта MpMap
- ТЗ v0.6: разделы
- Оболочка приложения и этап Э3
- Запрос: поля формы
- CLAUDE.md и следующий этап Э5 (2)
- Схема БД mp_service (SQL)
- Сервер: ConfigApi
- Этапы Э0–Э2 и договорённости
- Сервер: утилиты и Auth
- Сервер: зависимости и Routing
- Тема MpTheme и шрифты
- HTTP-тест api_smoke.py
- Запрос: RequestViewModel
- Уведомления NotificationCenter
- Главная: HomeViewModel
- Оболочка приложения и этап Э3 (2)
- Эмуляторы и HTTP-проверки
- Репозитории API
- ApiServer: маршруты
- Навигация и корень App
- Сервер: сессии Auth
- Маршрут: декодер polyline
- Настройки интерфейса
- Схема БД mp_service (SQL) (2)
- Экраны: вход, настройки, профиль
- Пиктограммы MpIcon
- CLAUDE.md и следующий этап Э5
- Профиль: ProfileViewModel
- Уведомления NotificationCenter (2)
- Сделка: экран и части раскладок (2)
- Конфигурация ConfigRepository
- Бэкап и продолжение сессии
- CLAUDE.md и следующий этап Э5 (2) (2)
- История этапов
- Тексты глоссария Texts
- Модели поставщика
- Мои сделки
- Оболочка приложения и этап Э3 (2) (2)
- Схема БД mp_service (SQL) (3)
- Схема БД mp_service (SQL) (4)
- Баллы и мастер запроса
- Сроки и антифрод
- Таблицы сделки и миграция
- Подбор поставщиков и такси
- Тесты моделей API
- CLAUDE.md и следующий этап Э5 (3)
- Сохранность работы и запас токенов
- Время периода Times
- Навигация и корень App (2)
- Навигация и корень App (3)
- Навигация и корень App (4)
- Роли и тестовый режим
- Этапы алгоритма сделки
- Код закрытия CloseCode
- Тексты глоссария Texts (2)
- Сетевой клиент ApiClient
- Схема БД mp_service (SQL) (5)
- Баллы и мастер запроса (2)
- Закрытие и отмена в алгоритме
- Комплект документации v0.7
- Схема БД mp_service (SQL) (6)
- Малая группа 59
- Малая группа 60
- Малая группа 63
- Сделка: экран и части раскладок (2) (2)
- SQL-тесты правил сделки
- Малая группа 65
- SQL-тесты правил сделки (2)
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
4. `DealState` - 25 edges
5. `Deal` - 25 edges
6. `Этап Э3: каркас Android-приложения` - 25 edges
7. `RequestState` - 24 edges
8. `План этапа Э4 — экраны клиента` - 24 edges
9. `RequestViewModel` - 23 edges
10. `agent` - 23 edges

## Surprising Connections (you probably didn't know these)
- `Этап Э3: каркас Android-приложения` --semantically_similar_to--> `Этап Э3: каркас приложения на Compose — вход, главная A/B/C, настройки, профиль (U-1…U-4)`  [INFERRED] [semantically similar]
  project_osm_map/docs/v0.7/plans/E3_app.md → CLAUDE.md
- `Термины ошибок ui.err.* (review_required, no_service_access, required_fields, offer_not_active, busy, routing_unavailable, no_route, rule)` --semantically_similar_to--> `24 термина глоссария ui.* / ui.err.*`  [INFERRED] [semantically similar]
  project_osm_map/docs/v0.7/plans/E4_client.md → CLAUDE.md
- `Функция БД deal_cancel_mode(deal_id, agent_id) → CANCEL_FREE / CLIENT / PROVIDER / NULL` --semantically_similar_to--> `БД: deal_cancel_mode — итог отмены`  [INFERRED] [semantically similar]
  project_osm_map/docs/v0.7/plans/E4_client.md → CLAUDE.md
- `Р-3 (Э4): какие сервисы на главной — решено: все сервисы всем агентам` --semantically_similar_to--> `Р-3 решён: на главной все сервисы всем агентам`  [INFERRED] [semantically similar]
  project_osm_map/docs/v0.7/03_implementation_spec.md → CLAUDE.md
- `Итог Э1: PostgreSQL 17.6, 30 таблиц, 14 представлений, 35 функций, 17 триггеров` --semantically_similar_to--> `Этап Э1: БД mp_service v0.7 — 30 таблиц, правила сделки в БД (план E1_db.md)`  [INFERRED] [semantically similar]
  project_osm_map/docs/v0.7/plans/E1_db.md → CLAUDE.md

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

## Communities (116 total, 50 thin omitted)

### Community 0 - "Сделка: экран и части раскладок"
Cohesion: 0.09
Nodes (35): parseServerTime(), Offer, CancelDialog(), DealActions, DealActionsRow(), DealHeader(), DealMap(), DealScreen() (+27 more)

### Community 1 - "ТЗ v0.6: принципы и антифрод"
Cohesion: 0.05
Nodes (42): Антифрод и agent_sanction (эскалация санкций), Кейс A/B/C/D (сделка 6, порядок B, D, C), Триггер check_dict_type() (ОВ-22), Основной документ ТЗ в Claude Docs, Сделка deal, Отмена сделки (CANCEL_FREE/CLIENT/PROVIDER, EXPIRED), Отклик deal_offer, Статусы сделки INIT→MATCHING→OFFERS→AGREED→CONTACT→CLOSED (+34 more)

### Community 3 - "Раскладки главной (2)"
Cohesion: 0.15
Nodes (15): DealListItem, MapSlot, PendingReview, DealsSummary(), HomeActions, HomeScreen(), PendingReviewsBanner(), SearchField() (+7 more)

### Community 4 - "Модели API приложения"
Cohesion: 0.10
Nodes (26): AttrValue, CancelRequest, CancelResult, CloseSubmit, Deal, DealCreate, DevLoginRequest, FieldOption (+18 more)

### Community 5 - "Сервер: Db, Jobs и план Э4 (API)"
Cohesion: 0.17
Nodes (6): Шаг 2: API — поле cancel_mode в DealApi.SQL_DEAL и схеме Deal, Проверка по HTTP: server/tests/api_smoke.py, /deals/{dealId}/cancel (итог решает deal_cancel: CANCEL_FREE / CLIENT / PROVIDER), Поле Deal.cancel_mode (CANCEL_FREE / CLIENT / PROVIDER, null — отменить нельзя), CodeParams, DealApi

### Community 6 - "Карта MpMap"
Cohesion: 0.08
Nodes (8): applyBaseLayer(), LatLon, MapLine, MapMarker, MapZone, MpMap(), MapEventsReceiver, UrlTileSource

### Community 7 - "ТЗ v0.6: разделы"
Cohesion: 0.05
Nodes (36): Антифрод, Жизненный цикл подписки, Изменения относительно исходной модели, Как добавить сервис, Карта в приложении, Кейс A/B/C/D, Модель данных, Монетизация (+28 more)

### Community 8 - "Оболочка приложения и этап Э3"
Cohesion: 0.10
Nodes (22): MainActivity, ViewModel + UiState; шаблон выбирает только composable-реализацию, Конфигурация с кэшем по контрольной сумме (GET /config?checksum= → 304/200), Модели и репозитории data/ (@Serializable по openapi.yaml, тонкие репозитории), Снимки экранов E3_screens/, Проверка на эмуляторе: сценарии U-1…U-4, Вход feature/login/ (GET /dev/agents при MP_DEV_LOGIN=1, иначе POST /session), Сетевой слой core/net/ (ApiClient, ApiError, NetworkError, SessionStore) (+14 more)

### Community 9 - "Запрос: поля формы"
Cohesion: 0.24
Nodes (14): FieldInput(), FieldsBlock(), PriceBlock(), RequestActions, RequestMap(), RequestScreen(), RouteTargetToggle(), ServiceBlock() (+6 more)

### Community 10 - "CLAUDE.md и следующий этап Э5 (2)"
Cohesion: 0.08
Nodes (32): Кириллицу через adb shell input text не ввести — вводить латиницу и цифры, adb reverse tcp:8080 tcp:8080, HTTP-тест server/tests/api_smoke.py (MP_TEST_PASSWORD, сервер запущен), Текущее состояние: Э4 завершён, Э5 (экраны поставщика) не начат, план Э5 не написан, Правило БД: COMMENT ON для каждого объекта, поля и функции (проверка по pg_attribute), БД: deal_cancel_mode — итог отмены, Эмулятор в поясе Алматы: adb shell cmd alarm set-timezone Asia/Almaty, adb из Git Bash: MSYS_NO_PATHCONV=1 (+24 more)

### Community 11 - "Схема БД mp_service (SQL)"
Cohesion: 0.07
Nodes (8): agent_has_rating(), agent_rating(), agent_service, g_src, provider_limit(), rating_history, trg_agent_service_assign, trg_rating_history_dict

### Community 12 - "Сервер: ConfigApi"
Cohesion: 0.26
Nodes (4): ConfigApi, Request, Response, ProviderApi

### Community 13 - "Этапы Э0–Э2 и договорённости"
Cohesion: 0.10
Nodes (24): Эскизы интерфейса (шаблоны A/B/C) — холст, Таблица agent_login (хеш bcrypt через pgcrypto), agent_login_check(login, password) — crypt() pgcrypto, Кейс A/B/C/D: порядок откликов B, D, C; E отсечён правилом В-1, deal_agree(), Справочник DEAL_TRANSITION (18 переходов), Режим загрузки mp.load_mode (is_load_mode, RESET), trg_agent_testing / trg_agent_service_assign / trg_agent_position (+16 more)

### Community 14 - "Сервер: утилиты и Auth"
Cohesion: 0.10
Nodes (4): ApiServer, Handler, Route, Http

### Community 16 - "Тема MpTheme и шрифты"
Cohesion: 0.13
Nodes (9): findActivity(), googleFamily(), Mp, MpColors, MpFonts, MpShapes, MpTheme(), withFamily() (+1 more)

### Community 17 - "HTTP-тест api_smoke.py"
Cohesion: 0.18
Nodes (17): call(), check(), main(), agree(), close(), config(), contact(), create() (+9 more)

### Community 18 - "Запрос: RequestViewModel"
Cohesion: 0.11
Nodes (7): Created, RequestEvent, RequestViewModel, ReviewFirst, RouteTarget, DEST, PICKUP

### Community 19 - "Уведомления NotificationCenter"
Cohesion: 0.11
Nodes (7): NotificationCenter, GET /notifications?since (опрос 2–5 с), GET /me, ApiServer (routes, lang, DB errors to HTTP), DB rule functions (provider_can_offer, deal_agree, deal_cancel, make_auto_offers, price_forecast, run_deal_jobs, agent_login_check), Error model {error, code} with HTTP 400/401/403/404/409/422, Jobs module (run_deal_jobs every minute, taxi route requests), /notifications (since, read)

### Community 21 - "Оболочка приложения и этап Э3 (2)"
Cohesion: 0.12
Nodes (8): ApiClient, ApiError, AppError, DecodeError, ErrorBody, HttpResult, NetworkError, Найдено и исправлено при проверке (pseudonym null, clipToBounds, coroutineScope, DecodeError)

### Community 22 - "Эмуляторы и HTTP-проверки"
Cohesion: 0.10
Nodes (21): Эмуляторы: 5554 — user_testing_1 (агент 7, клиент), 5556 — user_testing_2 (агент 8, поставщик), Тестовые пользователи user_testing_1…5 (агенты 7–11; в репозитории только хеш пароля), API v0.7 (http://127.0.0.1:8080/api), Открытые вопросы реализации Р-1…Р-4, Р-3 (Э4): какие сервисы на главной — решено: все сервисы всем агентам, Вопрос Р-4 (Э5): время без пояса — отклонять начало в прошлом и/или передавать +05:00, Э4. Экраны клиента, Э5. Экраны поставщика (+13 more)

### Community 23 - "Репозитории API"
Cohesion: 0.16
Nodes (6): CloseCode, RouteLink, DealRepository, enc(), NotificationRepository, ServiceRepository

### Community 24 - "ApiServer: маршруты"
Cohesion: 0.14
Nodes (3): Projection, Route, Routing

### Community 25 - "Навигация и корень App"
Cohesion: 0.13
Nodes (7): DealRoute, DealsRoute, HomeRoute, ProfileRoute, RequestRoute, ReviewRoute, SettingsRoute

### Community 26 - "Сервер: сессии Auth"
Cohesion: 0.17
Nodes (5): Auth, Session, Db, Work, Jobs

### Community 27 - "Маршрут: декодер polyline"
Cohesion: 0.17
Nodes (6): decodePolyline(), invalidFields(), apiErrorText(), errorKey(), ClientFlowTest, Термины ошибок ui.err.* (review_required, no_service_access, required_fields, offer_not_active, busy, routing_unavailable, no_route, rule)

### Community 28 - "Настройки интерфейса"
Cohesion: 0.15
Nodes (11): ConfigUpdate, Language, UiOption, UiOptions, SchemeFilter, ALL, DARK, LIGHT (+3 more)

### Community 29 - "Схема БД mp_service (SQL) (2)"
Cohesion: 0.14
Nodes (20): agent_pending_reviews(), agent_sanction, check_dict_type(), deal, deal_attribute, deal_offer, deal_review, deal_status_log (+12 more)

### Community 31 - "Пиктограммы MpIcon"
Cohesion: 0.13
Nodes (4): hex(), IconSet, MpIcon(), svgBitmap()

### Community 32 - "CLAUDE.md и следующий этап Э5"
Cohesion: 0.13
Nodes (17): Структура app/ на Compose: core/, data/, feature/, ui/ (шаблоны a|b|c), Kotlin 2.4.21 вместо встроенного в AGP 9 2.2.10 (buildscript), server/ — локальный API на Java (HttpServer + JDBC + GraphHopper), 39 методов, openapi.yaml, Прототип маркетплейса услуг v0.7: Android-приложение, локальный API, PostgreSQL, Схема PostgreSQL mp_service (db/mp_service.sql, пересоздаёт схему целиком), PostgreSQL 17.6 на localhost:5432, база postgres, схема mp_service, project_osm_map — проект (Android с картой osmdroid, сервер, БД, документация), Этап Э2: API v0.7 (39 методов, openapi.yaml, api_smoke.py 16/16) (+9 more)

### Community 33 - "Профиль: ProfileViewModel"
Cohesion: 0.18
Nodes (9): Failed, Loaded, Loading, ProfileContent(), ProfileScreen(), ProfileState, ProfileViewModel, roleKey() (+1 more)

### Community 35 - "Сделка: экран и части раскладок (2)"
Cohesion: 0.13
Nodes (15): DealApi module (client), Шаг 4: Главная — дерево категория → сервис → вариант, запрос из публикации, плашка баллов, Шаг 5: Запрос — гео point / route / none, поля формы, прогноз через 500 мс, «Моя цена» = медиана, openapi.yaml - mp_service API 0.7.0, Схема Deal (status, actions, cancel_mode, counterpart_fields по show_stage), /deals/{dealId} (сделка: статус, действия, открытые поля), /deals (create request INIT->MATCHING, list), /forecast (price forecast, taxi by route length) (+7 more)

### Community 36 - "Конфигурация ConfigRepository"
Cohesion: 0.21
Nodes (6): ConfigRepository, BaseLayer, CancelReason, IconDef, UiConfig, UserConfig

### Community 37 - "Бэкап и продолжение сессии"
Cohesion: 0.15
Nodes (13): mp_service.sql — дамп схемы БД (при заданном MP_DB_PASSWORD / PGPASSWORD / pgpass.conf), Срезы в C:\MP\Backups\github_repo\<ГГГГММДД-ЧЧмм>\, журнал backup.log, repo.bundle — ветки и срез рабочей копии (ссылка refs/backup/<время>), в GitHub ничего не уходит, Хранение срезов: 48 ч, первый срез дня — 14 дней; без изменений срез не создаётся; при C: < 20 ГБ пропуск, tools/backup/hourly_backup.ps1 — скрипт ежечасного среза, Переменные MP_DB_PASSWORD, MP_GEO_SECRET, MP_DEV_LOGIN, MP_TEST_PASSWORD — пароли только в переменных, Ежечасный срез репозитория и БД, Перед долгой или рискованной операцией (перезагрузка БД, массовая правка, импорт) — коммит или ручной срез hourly_backup.ps1 (+5 more)

### Community 38 - "CLAUDE.md и следующий этап Э5 (2) (2)"
Cohesion: 0.17
Nodes (14): Android SDK: emulator.exe, adb.exe, Документация docs/v0.7: 01 алгоритм сделки, 02 БД, 03 ТЗ (этапы Э0–Э8), 04 архитектура, Репозиторий github.com/mpavel-9999/mp_- (владелец — Павел), JAVA_HOME = JBR Android Studio для Gradle и сервера (java нет в PATH), Python не в PATH: ~/.local/bin/python3.15.exe; для pyyaml/PIL — anaconda3, Вопрос: отправить ветку в GitHub (копия вне компьютера) — по просьбе владельца, Порядок чтения после обрыва: Сейчас → Открытые вопросы → план этапа → Окружение и запуск, Договорённость: в GitHub не отправлять без просьбы владельца (+6 more)

### Community 39 - "История этапов"
Cohesion: 0.13
Nodes (15): Сессия API задаёт mp.agent_id и mp.geo_secret, 2026-10-26: ежечасный бэкап, правила продолжения после обрыва, CLAUDE.md переструктурирован, 2026-10-19: создан CLAUDE.md, Офлайн-карта: только растровые MBTiles (скачивание с CDN — заглушка), kazakhstan-latest.osm.pbf (214 МБ) и кэш gh-cache для маршрутов (в .gitignore), Р-3 решён: на главной все сервисы всем агентам, Решены Р-1…Р-3, В-Э3-*, В-Э4-* (OSM, библиотеки, сервисы на главной), Правила сделки — в БД; приложение показывает действия из Deal.actions (+7 more)

### Community 40 - "Тексты глоссария Texts"
Cohesion: 0.21
Nodes (3): tintSvg(), parseHexColor(), CoreTest

### Community 41 - "Модели поставщика"
Cohesion: 0.16
Nodes (7): IncomingRequest, PositionUpdate, ServiceSettingsUpdate, Slot, SlotCreate, StatusOk, ProviderRepository

### Community 42 - "Мои сделки"
Cohesion: 0.20
Nodes (8): DealRow(), DealsScreen(), DealsState, DealsViewModel, Failed, Loaded, Loading, Шаг 8: «Мои сделки» — GET /deals?role=all&status=active

### Community 43 - "Оболочка приложения и этап Э3 (2) (2)"
Cohesion: 0.29
Nodes (5): AgentList(), LoginScreen(), LoginState, LoginViewModel, PasswordForm()

### Community 44 - "Схема БД mp_service (SQL) (3)"
Cohesion: 0.20
Nodes (14): agent_role, agent_subscription, package_service, referral_reward, service_package, subscription_payment, trg_agent_role_dict, trg_referral_reward_dict (+6 more)

### Community 45 - "Схема БД mp_service (SQL) (4)"
Cohesion: 0.25
Nodes (14): attribute, get_setting(), glossary, glossary_text, icon, service, service_attribute, service_base_id() (+6 more)

### Community 46 - "Баллы и мастер запроса"
Cohesion: 0.18
Nodes (5): ReviewScreen(), ReviewState, ReviewViewModel, Шаг 7: Баллы — оценка 1–5 обязательна, «Назад» не уводит, /deals/{dealId}/review and /me/pending-reviews

### Community 47 - "Сроки и антифрод"
Cohesion: 0.17
Nodes (11): 6) Антифрод: BYPASS_GEO, NEAR_CANCEL, FAKE_REFERRALS, блокировки, Статус EXPIRED, Статус NOT_CLOSED, Плановые задания (раз в минуту): истечение, CLOSING_ASK, NOT_CLOSED, сближение, публикации, Таблица agent_sanction (warning / rating_down / block), Структура модулей приложения ru.mpavel.osmmap (core/*, feature/*, ui/template, ui/map), GraphHopper (маршруты по дорогам OSM), Модуль: Android-приложение (Kotlin, Jetpack Compose) (+3 more)

### Community 48 - "Таблицы сделки и миграция"
Cohesion: 0.17
Nodes (12): Миграция v0.6 → v0.7, Таблица agent_position [new], Таблица deal, Таблица deal_offer (offer_status), Таблица provider_slot [new], Триггер trg_agent_position (manual только при is_testing), Триггер trg_deal_offer_check, Индекс uq_deal_offer_selected (один selected на сделку) (+4 more)

### Community 49 - "Подбор поставщиков и такси"
Cohesion: 0.18
Nodes (8): Сценарий: частная парковка (capacity), 2) Подбор поставщиков (MATCHING), agent_rating(agent, role, service, on_date), provider_can_offer(deal_id, agent_id) → (ok, reason), Таблица agent_service (цены, тариф, capacity, назначение), Триггер trg_agent_service_assign (субагент), Представление v_agent_available_service [chg], Схема подбора такси по пути

### Community 51 - "CLAUDE.md и следующий этап Э5 (3)"
Cohesion: 0.24
Nodes (11): info.txt — ветка, HEAD, незакоммиченные изменения, Каталог планов docs/v0.7/plans (итоги и снимки экранов E<n>_screens/), Правило: ход выполнения в плане (раздел «Ход выполнения»: шаг, дата, результат проверки) + обновление «Сейчас»; итог — раздел «Итог», Договорённость: следить за ресурсами системы (ОЗУ ≥ 6 ГБ, C: ≥ 20 ГБ, ЦП < 80 %), останавливать свои процессы, без параллельных тяжёлых задач, Договорённость: сообщать, когда нужен ответ владельца, Договорённость: детальный план + проверка логической связанности + согласование перед реализацией, Договорённость: общение и коммиты на русском, Начало сессии после обрыва (4 шага): «Сейчас» и вопросы + git/backup.log → «Ход выполнения» плана → graphify query → остановить процессы и проверить ресурсы (+3 more)

### Community 52 - "Сохранность работы и запас токенов"
Cohesion: 0.20
Nodes (11): Локальный коммит после каждого шага плана с пройденной проверкой («Э<n> шаг <k>: …»), Пороги контекста: ≥70 % — коммит после каждого шага; ≥85 % — не начинать крупных шагов, предложить новую сессию; 97 % — автосжатие, Инструмент get_usage (контекст сессии и лимиты тарифа): в начале этапа, перед долгой операцией, ~каждые 10 шагов, Правило: обновлять граф graphify-out в конце этапа и коммитить вместе с CLAUDE.md, graphify-out — граф знаний проекта (исключения — .graphifyignore), Лимиты тарифа: 5-часовой ≥80 % или недельный ≥90 % — коммит, «Сейчас», предупредить владельца, Вопрос: установить graphifyy[sql] для разбора SQL в графе — ждёт решения владельца, Договорённость: в конце дня с доработками — версия графа и синхронизация с Git (+3 more)

### Community 53 - "Время периода Times"
Cohesion: 0.25
Nodes (6): defaultPeriod(), formatPeriod(), formatShort(), toServerTime(), Э3. Каркас Android-приложения (Compose), Шаг 3: основа приложения — core/time, Polyline, onMapTap, NotificationCenter, маршруты навигации

### Community 54 - "Навигация и корень App (2)"
Cohesion: 0.24
Nodes (3): App(), MainNav(), Nav

### Community 55 - "Навигация и корень App (3)"
Cohesion: 0.20
Nodes (6): AppState, AppViewModel, Failed, LoggedOut, Ready, Starting

### Community 56 - "Навигация и корень App (4)"
Cohesion: 0.29
Nodes (9): ActionIcon(), AppFrame(), BottomTabs(), Tab, DEALS, HOME, PROFILE, SETTINGS (+1 more)

### Community 57 - "Роли и тестовый режим"
Cohesion: 0.18
Nodes (11): Роли агентов: CLIENT, PROVIDER, SUBAGENT, COMPANY, ADMIN, Режим тестирования администратора, Справочник MAP_CURSOR (капля / капля с шестерёнкой), Таблица agent (is_testing), Триггер trg_agent_testing (is_testing только при ADMIN), Представление v_map_cursor, Закрытие: /close/request, /close/code, POST /close, /close/test-scan, /close/test-provide, PUT /me/position, GET /deals/{id}/positions (+3 more)

### Community 58 - "Этапы алгоритма сделки"
Cohesion: 0.18
Nodes (8): Публикация (окно) поставщика (Н-2), Сценарий: парковка — публикация места, 1) Инициация — клиент создаёт запрос (INIT → MATCHING), agent_pending_reviews(agent_id), price_forecast(service_id, lat, lon, period_start), Таблица deal_review, Триггер trg_deal_review, Клиент: /map/slots, POST /forecast, POST /deals, /deals/{id}/offers, /agree, /routes

### Community 61 - "Сетевой клиент ApiClient"
Cohesion: 0.20
Nodes (3): SessionStore, DevAgent, SessionRepository

### Community 62 - "Схема БД mp_service (SQL) (5)"
Cohesion: 0.22
Nodes (10): agent, agent_attribute, agent_login, agent_login_check(), agent_pseudonym(), interface_lang, ix_provider_slot_search, provider_slot (+2 more)

### Community 63 - "Баллы и мастер запроса (2)"
Cohesion: 0.44
Nodes (4): ErrorBox(), errorMessage(), LoadingBox(), textOr()

### Community 64 - "Закрытие и отмена в алгоритме"
Cohesion: 0.22
Nodes (7): Сценарий: онлайн-услуга (без CONTACT, ключ KEY), 5) Закрытие сделки, check_dict_type() + 9 триггеров, Справочник DEAL_TRANSITION [new], Таблица deal_status_log (пишет только триггер), Общий справочник dictionary (value_json), Триггер trg_deal_status

### Community 65 - "Комплект документации v0.7"
Cohesion: 0.46
Nodes (8): Алгоритм формирования сделки v0.7, Журнал решений В-1…В-13, Н-1…Н-12, П-1…П-6, Архитектура БД mp_service v0.7 (29 таблиц), ТЗ на реализацию прототипа v0.7 (для Claude), Общая архитектура продукта v0.7, Комплект документации project_osm_map v0.7, ТЗ v0.6 (docs/TZ_marketplace.md), TZ_project_WIN_SERVICE.txt (исходный алгоритм)

### Community 66 - "Схема БД mp_service (SQL) (6)"
Cohesion: 0.29
Nodes (7): agent_position, ix_agent_position_update, ix_notification_agent, notification, run_deal_jobs(), trg_agent_position, trg_notification_dict

### Community 67 - "Малая группа 59"
Cohesion: 0.33
Nodes (5): Режимы сервиса: geo_mode, place_mode, price_unit, max_parallel_deals, slot_required, close_method, get_setting(service_id, code, rating), Таблица service (дерево категория → сервис → вариант), Таблица service_setting (настройки по дереву и рейтингу), Модель конфигурации «новый сервис — только данными» (пример «Эвакуатор»)

### Community 68 - "Малая группа 60"
Cohesion: 0.29
Nodes (7): Справочник UI_TEMPLATE (A, B, C) и UI_SCHEME (LIGHT/DARK), Формат JSON шаблона интерфейса и палитры, Представление v_ui_options, Нефункциональные требования (безопасность, ПДн, карта, доступность, масштабирование), Шаблон A «Карта в центре» (Manrope), Шаблон B «Карточки» (Golos Text), Шаблон C «Диспетчер» (Onest + JetBrains Mono)

### Community 69 - "Малая группа 63"
Cohesion: 0.33
Nodes (4): 4) Согласование (OFFERS → AGREED → CONTACT), 3) Выбор — поставщик откликается (MATCHING → OFFERS), Статус CONTACT (сближение ≤ near_radius_m), Представление v_deal_offer_list (смещение, distance_band)

### Community 70 - "Сделка: экран и части раскладок (2) (2)"
Cohesion: 0.33
Nodes (6): GraphHopper + kazakhstan-latest.osm.pbf (gh-cache), ProviderApi module, Routing module (GraphHopper 10.2, car profile, on-the-way check), /deals/{dealId}/offers (list, manual offer), /requests (v_deal_candidates + recommended price), /slots (publish place or route)

### Community 72 - "Малая группа 65"
Cohesion: 0.40
Nodes (3): Проверка на тестовых данных 25.10.2026 (расхождения v0.6), Модуль: PostgreSQL mp_service, Функциональные блоки (каталог, сделки, гео, закрытие, антифрод, конфигурация UI…)

### Community 74 - "Малая группа 66"
Cohesion: 0.50
Nodes (4): Таблица ui_config (config_json, checksum md5), GET/PUT /config, GET /ui/options, Загрузка конфигурации интерфейса по контрольной сумме (304), Взаимодействие: загрузка конфигурации интерфейса

### Community 76 - "Малая группа 68"
Cohesion: 0.67
Nodes (3): Auth module (in-memory bearer tokens 12h, dev login), Bearer security scheme, /session (login, logout)

## Knowledge Gaps
- **144 isolated node(s):** `LoggedOut`, `Ready`, `Starting`, `Loading`, `DEALS` (+139 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 397 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **50 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `План этапа Э4 — экраны клиента` connect `Эмуляторы и HTTP-проверки` to `Сделка: экран и части раскладок`, `Комплект документации v0.7`, `Сделка: экран и части раскладок (2)`, `Сервер: Db, Jobs и план Э4 (API)`, `Оболочка приложения и этап Э3`, `CLAUDE.md и следующий этап Э5 (2)`, `Мои сделки`, `Баллы и мастер запроса`, `CLAUDE.md и следующий этап Э5 (3)`, `Уведомления NotificationCenter`, `Время периода Times`, `Маршрут: декодер polyline`?**
  _High betweenness centrality (0.116) - this node is a cross-community bridge._
- **What connects `LoggedOut`, `Ready`, `Starting` to the rest of the system?**
  _144 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Сделка: экран и части раскладок` be split into smaller, more focused modules?**
  _Cohesion score 0.09134808853118712 - nodes in this community are weakly interconnected._
- **Why does `Шаг 5: Запрос — гео point / route / none, поля формы, прогноз через 500 мс, «Моя цена» = медиана` connect `Сделка: экран и части раскладок (2)` to `Сделка: экран и части раскладок`, `Запрос: поля формы`, `Запрос: RequestViewModel`, `Эмуляторы и HTTP-проверки`, `Этапы алгоритма сделки`, `Маршрут: декодер polyline`?**
  _High betweenness centrality (0.089) - this node is a cross-community bridge._
- **Should `ТЗ v0.6: принципы и антифрод` be split into smaller, more focused modules?**
  _Cohesion score 0.05314009661835749 - nodes in this community are weakly interconnected._
- **Why does `OSM Карта — экран сделки маркетплейса услуг` connect `ТЗ v0.6: разделы` to `CLAUDE.md и следующий этап Э5 (2)`?**
  _High betweenness centrality (0.075) - this node is a cross-community bridge._
- **Should `Раскладки главной` be split into smaller, more focused modules?**
  _Cohesion score 0.07897793263646923 - nodes in this community are weakly interconnected._