# Graph Report - github_repo  (2026-10-26)

## Corpus Check
- 41 files · ~105,140 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 1543 nodes · 4381 edges · 101 communities (66 shown, 35 thin omitted)
- Extraction: 96% EXTRACTED · 4% INFERRED · 0% AMBIGUOUS · INFERRED: 159 edges (avg confidence: 0.87)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- Сервер API: HTTP и транзакции
- Входящие запросы поставщика
- Экран сделки A/B/C
- Экран запроса клиента
- Авторизация сервера
- Модели данных API
- Главная и карта
- Карта osmdroid
- План Э1 — БД и правила
- Репозитории и граф зависимостей
- Схема БД и рейтинг
- Публикация: раскладки
- HTTP-тест api_smoke
- Тема и палитра
- Репозиторий сделок
- План Э3 — каркас приложения
- Настройки сервиса поставщика
- Маршрутизация GraphHopper
- Иконки SVG
- Навигация App.kt
- Настройки приложения
- Центр уведомлений
- Профиль
- План Э4 — экраны клиента
- Публикация: модель
- ТЗ маркетплейса v0.6
- Главная: модель
- ТЗ на реализацию v0.7
- Таблицы сделки
- JVM-тесты ядра
- Время по Алматы и таблицы
- Экран баллов
- README и OpenAPI
- Сетевой клиент и ошибки
- Конфигурация интерфейса
- Тесты поставщика
- Р-4, итог Э5 и следующий этап
- Настройки: модель
- DealsScreen()
- LoginViewModel
- Смещение точки до согласования approx_of
- CLAUDE.md
- Архитектура БД mp_service v0.7
- ProviderViewModel
- agent_role
- Таблица dictionary (общий справочник, di
- provider_can_offer(deal_id, agent_id) — 
- ApiClient
- OfferViewModel
- Tab
- service
- AppViewModel
- Nav
- Шаг 3: основа приложения — core/time, Po
- Texts
- MyService
- agent
- glossary
- Дерево услуг service: категория → сервис
- Сделка deal
- decodeBody()
- MainActivity.kt
- LatLon
- SkippedRequests
- notification
- Подбор поставщиков п. 2.1–2.10 (гео по p
- 03_period.sql
- MainNav()
- 01_rules.sql
- Монетизация
- Отклики, карта и маршрут
- Отмена: CANCEL_FREE / CANCEL_CLIENT / CA
- ApiServer (routes, lang, DB errors to HT
- MapEventsReceiver
- deal_offer
- 02_cancel_mode.sql
- Процесс сделки
- Комплект документации project_osm_map v0
- Db module (connection, session params, t
- Рабочий контур (балансировщик, API ×N, р
- Развёртывание прототипа (локально, 2 эму
- Модель конфигурации «новый сервис — толь
- Нефункциональные требования (безопасност
- Дорожная карта: прототип v0.7 → следующи
- Взаимодействие: загрузка конфигурации ин
- Взаимодействие: сближение и закрытие
- Шаблон A «Карта в центре» (Manrope)
- Шаблон B «Карточки» (Golos Text)
- Шаблон C «Диспетчер» (Onest + JetBrains 
- Auth module (in-memory bearer tokens 12h
- ConfigApi module
- DB views (v_deal_offer_list, v_deal_cand
- Http module (body, responses, errors)

## God Nodes (most connected - your core abstractions)
1. `AppGraph` - 38 edges
2. `DealApi` - 34 edges
3. `Nav` - 33 edges
4. `LatLon` - 31 edges
5. `DealRepository` - 30 edges
6. `MpMap()` - 28 edges
7. `Deal` - 26 edges
8. `DealState` - 25 edges
9. `Этап Э3: каркас Android-приложения` - 24 edges
10. `RequestState` - 24 edges

## Surprising Connections (you probably didn't know these)
- `Сначала план, потом выполнение (план этапа в docs/v0.7/plans)` --rationale_for--> `План этапа Э5 — экраны поставщика`  [INFERRED]
  CLAUDE.md → project_osm_map/docs/v0.7/plans/E5_provider.md
- `Окружение и запуск (JAVA_HOME, PostgreSQL 17.6, эмуляторы 5554/5556, MP_DB_PASSWORD)` --semantically_similar_to--> `Запуск: mp_service.sql, :server:run, adb reverse, installDebug, тесты`  [INFERRED] [semantically similar]
  CLAUDE.md → project_osm_map/README.md
- `Тема MpTheme из палитры → ColorScheme Material 3 + MpColors` --implements--> `MpColors`  [INFERRED]
  project_osm_map/docs/v0.7/plans/E3_app.md → project_osm_map/app/src/main/java/ru/mpavel/osmmap/core/theme/MpTheme.kt
- `Сетевой слой core/net/ (ApiClient, ApiError, NetworkError, SessionStore)` --implements--> `ApiClient`  [INFERRED]
  project_osm_map/docs/v0.7/plans/E3_app.md → project_osm_map/app/src/main/java/ru/mpavel/osmmap/core/net/ApiClient.kt
- `Сетевой слой core/net/ (ApiClient, ApiError, NetworkError, SessionStore)` --implements--> `ApiError`  [INFERRED]
  project_osm_map/docs/v0.7/plans/E3_app.md → project_osm_map/app/src/main/java/ru/mpavel/osmmap/core/net/ApiClient.kt

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Конфигурация сервиса данными (дерево, настройки, поля, глоссарий)** — project_osm_map_docs_tz_marketplace_service_tree, project_osm_map_docs_tz_marketplace_service_setting, project_osm_map_docs_tz_marketplace_service_attribute, project_osm_map_docs_tz_marketplace_glossary, project_osm_map_docs_tz_marketplace_get_setting [EXTRACTED 1.00]
- **Правила сделки в БД: функции, триггеры, переходы, плановое задание** — project_osm_map_docs_v0_7_plans_e1_db_provider_can_offer, project_osm_map_docs_v0_7_plans_e1_db_deal_agree, project_osm_map_docs_v0_7_plans_e1_db_deal_cancel, project_osm_map_docs_v0_7_plans_e1_db_run_deal_jobs, project_osm_map_docs_v0_7_plans_e1_db_trg_deal_status, project_osm_map_docs_v0_7_plans_e1_db_deal_transition [EXTRACTED 1.00]
- **E2 server module split (ApiServer, Db, Http, Auth, ConfigApi, DealApi, ProviderApi, Routing, CloseCode, Jobs)** — project_osm_map_docs_v0_7_plans_e2_api_apiserver, project_osm_map_docs_v0_7_plans_e2_api_db, project_osm_map_docs_v0_7_plans_e2_api_http, project_osm_map_docs_v0_7_plans_e2_api_auth, project_osm_map_docs_v0_7_plans_e2_api_configapi, project_osm_map_docs_v0_7_plans_e2_api_dealapi, project_osm_map_docs_v0_7_plans_e2_api_providerapi, project_osm_map_docs_v0_7_plans_e2_api_routing, project_osm_map_docs_v0_7_plans_e2_api_closecode, project_osm_map_docs_v0_7_plans_e2_api_jobs [EXTRACTED 1.00]
- **Интерфейс из конфигурации: кэш по сумме → тема, тексты, пиктограммы** — project_osm_map_docs_v0_7_plans_e3_app_config_checksum_cache, project_osm_map_docs_v0_7_plans_e3_app_theme_palette, project_osm_map_docs_v0_7_plans_e3_app_texts_glossary, project_osm_map_docs_v0_7_plans_e3_app_svg_icons, project_osm_map_docs_v0_7_plans_e3_app_settings_feature [EXTRACTED 1.00]
- **Одна логика — три раскладки откликов и сделки (A/B/C)** — project_osm_map_docs_v0_7_plans_e4_client_principle_three_layouts, project_osm_map_app_src_main_java_ru_mpavel_osmmap_feature_deal_dealviewmodel_dealviewmodel, project_osm_map_app_src_main_java_ru_mpavel_osmmap_ui_template_a_deallayoutsa_offersa, project_osm_map_app_src_main_java_ru_mpavel_osmmap_ui_template_b_deallayoutsb_offersb, project_osm_map_app_src_main_java_ru_mpavel_osmmap_ui_template_c_deallayoutsc_offersc, project_osm_map_app_src_main_java_ru_mpavel_osmmap_ui_template_a_deallayoutsa_deala, project_osm_map_app_src_main_java_ru_mpavel_osmmap_ui_template_b_deallayoutsb_dealb, project_osm_map_app_src_main_java_ru_mpavel_osmmap_ui_template_c_deallayoutsc_dealc [EXTRACTED 1.00]
- **Вход тестовых пользователей по хешу пароля** — project_osm_map_docs_v0_7_plans_e1_db_step_12a_test_users, project_osm_map_docs_v0_7_plans_e1_db_agent_login, project_osm_map_docs_v0_7_plans_e1_db_agent_login_check [EXTRACTED 1.00]
- **Меры против обхода платформы до согласования** — project_osm_map_docs_tz_marketplace_approx_offset, project_osm_map_docs_tz_marketplace_pseudonym, project_osm_map_docs_tz_marketplace_show_stage_near, project_osm_map_docs_tz_marketplace_antifraud [INFERRED 0.85]
- **Режим отмены: одно правило БД → поле API → диалог отмены** — project_osm_map_docs_v0_7_plans_e4_client_deal_cancel_mode, project_osm_map_app_src_main_java_ru_mpavel_osmmap_feature_deal_dealscreen_canceldialog, project_osm_map_docs_v0_7_plans_e4_client_test_02_cancel_mode [INFERRED 0.85]
- **Одна логика — три раскладки A/B/C** — project_osm_map_docs_v0_7_plans_e3_app_principle_one_logic_three_layouts, project_osm_map_app_src_main_java_ru_mpavel_osmmap_ui_template_a_homea_homea, project_osm_map_app_src_main_java_ru_mpavel_osmmap_ui_template_b_homeb_homeb, project_osm_map_app_src_main_java_ru_mpavel_osmmap_ui_template_c_homec_homec [INFERRED 0.85]
- **Taxi on-the-way matching (server-side routing, slots, requests, forecast)** — project_osm_map_docs_v0_7_plans_e2_api_routing, project_osm_map_docs_v0_7_plans_e2_api_graphhopper, project_osm_map_server_openapi_slots, project_osm_map_server_openapi_requests [INFERRED 0.85]
- **Время без часового пояса: истечение запросов и вопрос Р-4** — project_osm_map_docs_v0_7_plans_e4_client_timezone [INFERRED 0.95]
- **Р-4: время по Алматы — сервер, БД, приложение** — project_osm_map_docs_v0_7_03_implementation_spec_r_4, project_osm_map_docs_v0_7_plans_e5_provider_trg_period_check, project_osm_map_docs_v0_7_plans_e5_provider_now_mp, project_osm_map_server_openapi_health [EXTRACTED 1.00]
- **Экраны поставщика Э5: раздел → публикация → входящие → сделка → настройки** — project_osm_map_docs_v0_7_plans_e5_provider_provider_screen, project_osm_map_docs_v0_7_plans_e5_provider_slot_screens, project_osm_map_docs_v0_7_plans_e5_provider_incoming, project_osm_map_docs_v0_7_plans_e5_provider_deal_roles, project_osm_map_docs_v0_7_plans_e5_provider_service_settings [EXTRACTED 1.00]
- **Текущее состояние проекта: Э5 завершён, следующий Э6** — claude_current_status, project_osm_map_docs_v0_7_plans_e5_provider_result, project_osm_map_docs_v0_7_plans_e5_provider_progress, project_osm_map_docs_v0_7_03_implementation_spec_stage_e6 [EXTRACTED 1.00]

## Communities (101 total, 35 thin omitted)

### Community 0 - "Сервер API: HTTP и транзакции"
Cohesion: 0.06
Nodes (11): Шаг 2: API — поле cancel_mode в DealApi.SQL_DEAL и схеме Deal, ApiServer, Handler, Route, ConfigApi, Db, Work, CodeParams (+3 more)

### Community 1 - "Входящие запросы поставщика"
Cohesion: 0.07
Nodes (26): defaultPeriod(), formatPeriod(), formatShort(), nowMp(), parseServerTime(), toServerTime(), IncomingRequest, IncomingActions (+18 more)

### Community 2 - "Экран сделки A/B/C"
Cohesion: 0.08
Nodes (32): Offer, CancelDialog(), DealActions, DealActionsRow(), DealHeader(), DealMap(), DealScreen(), FieldList() (+24 more)

### Community 3 - "Экран запроса клиента"
Cohesion: 0.10
Nodes (24): FormField, FieldInput(), FieldsBlock(), PriceBlock(), RequestActions, RequestMap(), RequestScreen(), RouteTargetToggle() (+16 more)

### Community 4 - "Авторизация сервера"
Cohesion: 0.06
Nodes (7): Auth, Session, CloseCode, ApiException, Http, Request, Response

### Community 5 - "Модели данных API"
Cohesion: 0.10
Nodes (34): BaseLayer, CancelReason, CancelRequest, CancelResult, CloseSubmit, DealCreate, DealListItem, DevAgent (+26 more)

### Community 6 - "Главная и карта"
Cohesion: 0.16
Nodes (16): PendingReview, ServiceNode, DealsSummary(), HomeActions, HomeScreen(), PendingReviewsBanner(), SearchField(), ServiceRow() (+8 more)

### Community 8 - "Карта osmdroid"
Cohesion: 0.07
Nodes (3): decodePolyline(), applyBaseLayer(), UrlTileSource

### Community 9 - "План Э1 — БД и правила"
Cohesion: 0.10
Nodes (24): Таблица agent_login (хеш bcrypt через pgcrypto), agent_login_check(login, password) — crypt() pgcrypto, Кейс A/B/C/D: порядок откликов B, D, C; E отсечён правилом В-1, deal_agree(), Справочник DEAL_TRANSITION (18 переходов), Режим загрузки mp.load_mode (is_load_mode, RESET), trg_agent_testing / trg_agent_service_assign / trg_agent_position, План этапа Э1 — БД v0.7 (+16 more)

### Community 10 - "Репозитории и граф зависимостей"
Cohesion: 0.12
Nodes (8): StatusOk, NotificationRepository, ProviderRepository, ServiceRepository, SessionRepository, AppGraph, MpApp, Модели и репозитории data/ (@Serializable по openapi.yaml, тонкие репозитории)

### Community 11 - "Схема БД и рейтинг"
Cohesion: 0.07
Nodes (5): agent_has_rating(), agent_rating(), g_src, rating_history, trg_rating_history_dict

### Community 12 - "Публикация: раскладки"
Cohesion: 0.29
Nodes (12): RadiusSlider(), SlotActions, SlotMap(), SlotPlaceBlock(), SlotPriceBlock(), SlotScreen(), SlotSubmitBlock(), SlotWindowBlock() (+4 more)

### Community 13 - "HTTP-тест api_smoke"
Cohesion: 0.18
Nodes (19): call(), check(), main(), agree(), close(), config(), contact(), create() (+11 more)

### Community 14 - "Тема и палитра"
Cohesion: 0.13
Nodes (8): findActivity(), googleFamily(), Mp, MpColors, MpFonts, MpShapes, MpTheme(), withFamily()

### Community 16 - "Репозиторий сделок"
Cohesion: 0.12
Nodes (5): CloseCode, Deal, ReviewRequest, DealRepository, enc()

### Community 17 - "План Э3 — каркас приложения"
Cohesion: 0.14
Nodes (18): E3_app.md — план этапа Э3, Конфигурация с кэшем по контрольной сумме (GET /config?checksum= → 304/200), Снимки экранов E3_screens/, Проверка на эмуляторе: сценарии U-1…U-4, Библиотеки и версии (вопрос Р-2): Compose BOM 2026.09.00, OkHttp 5.5.1, Coil 3.6.3, navigation 2.10.2 и др., Вход feature/login/ (GET /dev/agents при MP_DEV_LOGIN=1, иначе POST /session), Сетевой слой core/net/ (ApiClient, ApiError, NetworkError, SessionStore), Что остаётся на Э4–Э6 (feature/request, offers, deal, provider, core/location, QR) (+10 more)

### Community 18 - "Настройки сервиса поставщика"
Cohesion: 0.19
Nodes (13): buildServiceUpdate(), fmt(), formOf(), NumField(), parseNum(), ReadOnlyRow(), ServiceSettingsScreen(), ServiceSettingsViewModel (+5 more)

### Community 19 - "Маршрутизация GraphHopper"
Cohesion: 0.12
Nodes (3): Projection, Route, Routing

### Community 20 - "Иконки SVG"
Cohesion: 0.12
Nodes (4): hex(), IconSet, MpIcon(), svgBitmap()

### Community 21 - "Навигация App.kt"
Cohesion: 0.13
Nodes (5): HomeRoute, IncomingRoute, ProfileRoute, ProviderRoute, SettingsRoute

### Community 22 - "Настройки приложения"
Cohesion: 0.22
Nodes (7): SettingsScreen(), apiErrorText(), ErrorBox(), errorKey(), errorMessage(), LoadingBox(), textOr()

### Community 24 - "Профиль"
Cohesion: 0.19
Nodes (8): Failed, Loaded, Loading, ProfileContent(), ProfileScreen(), ProfileState, ProfileViewModel, roleKey()

### Community 25 - "План Э4 — экраны клиента"
Cohesion: 0.14
Nodes (14): План этапа Э4 — экраны клиента, Тестовые данные: кейс A/B/C/D (сделка 6 — парковка, сделка 7 — такси), Проверка связности плана Э4 (порядок, алгоритм, шаблоны, данные, доступ, безопасность), Функция БД deal_cancel_mode(deal_id, agent_id) → CANCEL_FREE / CLIENT / PROVIDER / NULL, coreLibraryDesugaring desugar_jdk_libs 2.1.5 (java.time при minSdk 24), Найдено и исправлено при проверке (карта под шторкой A, перецентровка, обновление главной, «Назад» на баллах), Цель Э4: путь «услуга → запрос → отклики → согласование → сделка → отмена или баллы» в A/B/C, Вопросы владельцу В-Э4-1…В-Э4-4 и решение 25.10.2026 (+6 more)

### Community 26 - "Публикация: модель"
Cohesion: 0.15
Nodes (4): SlotTarget, A, B, SlotViewModel

### Community 27 - "ТЗ маркетплейса v0.6"
Cohesion: 0.11
Nodes (18): Антифрод, Изменения относительно исходной модели, Как добавить сервис, Модель данных, Настройки сервисов, Основные понятия, Открытые вопросы, Пиктограммы (+10 more)

### Community 29 - "ТЗ на реализацию v0.7"
Cohesion: 0.13
Nodes (17): История этапов Э0–Э5, Алгоритм формирования сделки v0.7 (источник истины), Сценарии приёмки S-1…S-16, Антифрод: блокировки и санкции, Статусы сделки и переходы INIT→MATCHING→OFFERS→AGREED→CONTACT→CLOSED, Журнал решений В-1…В-13, Н-1…Н-12, П-1…П-6, trg_deal_status — проверка переходов и deal_status_log, ТЗ на реализацию прототипа v0.7 (+9 more)

### Community 30 - "Таблицы сделки"
Cohesion: 0.16
Nodes (17): agent_pending_reviews(), agent_sanction, check_dict_type(), deal, deal_review, deal_status_log, dictionary, ix_deal_status_period (+9 more)

### Community 31 - "JVM-тесты ядра"
Cohesion: 0.19
Nodes (4): tintSvg(), ApiError, parseHexColor(), CoreTest

### Community 32 - "Время по Алматы и таблицы"
Cohesion: 0.13
Nodes (13): Публикация (окно) поставщика, Таблица deal (сделка / запрос клиента), Таблица deal_offer (отклики, offer_status), Таблица provider_slot (публикации поставщика), Плановые задания раз в минуту (EXPIRED, CLOSING_ASK, NOT_CLOSED, CONTACT, публикации), Модуль: Android-приложение (Kotlin, Jetpack Compose), Модуль: API-сервер (Java, HttpServer + JDBC), Модуль: PostgreSQL mp_service (+5 more)

### Community 33 - "Экран баллов"
Cohesion: 0.22
Nodes (4): ReviewScreen(), ReviewState, ReviewViewModel, Шаг 7: Баллы — оценка 1–5 обязательна, «Назад» не уводит

### Community 34 - "README и OpenAPI"
Cohesion: 0.14
Nodes (13): Прототип маркетплейса услуг v0.7 (Android + API + PostgreSQL), E2 plan: API v0.7, api_smoke.py HTTP smoke test (16/16), 02_db_architecture.md (sections 6.3, 8), E1_db.md (E1 plan), 03_implementation_spec.md (TZ v0.7, sections 4 and 6), API, Карта OSM: офлайн-пакет MBTiles (растровый), онлайн-тайлы только для тестов (+5 more)

### Community 35 - "Сетевой клиент и ошибки"
Cohesion: 0.16
Nodes (5): AppError, DecodeError, ErrorBody, NetworkError, Найдено и исправлено при проверке (pseudonym null, clipToBounds, coroutineScope, DecodeError)

### Community 37 - "Тесты поставщика"
Cohesion: 0.20
Nodes (5): AttrValue, OfferCreate, buildOfferCreate(), OfferState, buildSlotCreate()

### Community 38 - "Р-4, итог Э5 и следующий этап"
Cohesion: 0.20
Nodes (13): Сейчас: Э5 завершён 2026-10-26 (шаги 0–11), следующее — план Э6 в новой сессии, Открытые вопросы (Р-4 реализован, SQL в графе, ветка в GitHub), Закрытие сделки кодом QR / KEY (HMAC по интервалу), Режим тестирования администратора (курсор, «Сканировать / Предоставить QR»), Э6 — режим тестирования и закрытие (следующий этап, сценарии S-4…S-8), Взаимодействие: закрытие без камеры в режиме тестирования, CloseCode module (HMAC-SHA256, current+previous interval), Границы Э4 (поставщик — Э5; GPS, курсор и закрытие кодом — Э6) (+5 more)

### Community 39 - "Настройки: модель"
Cohesion: 0.20
Nodes (7): ConfigUpdate, SchemeFilter, ALL, DARK, LIGHT, SettingsState, SettingsViewModel

### Community 40 - "DealsScreen()"
Cohesion: 0.20
Nodes (8): DealRow(), DealsScreen(), DealsState, DealsViewModel, Failed, Loaded, Loading, Шаг 8: «Мои сделки» — GET /deals?role=all&status=active

### Community 41 - "LoginViewModel"
Cohesion: 0.29
Nodes (5): AgentList(), LoginScreen(), LoginState, LoginViewModel, PasswordForm()

### Community 42 - "Смещение точки до согласования approx_of"
Cohesion: 0.19
Nodes (12): Кейс A/B/C/D (сделка 6, порядок B, D, C), Отмена сделки (CANCEL_FREE/CLIENT/PROVIDER, EXPIRED), Статусы сделки INIT→MATCHING→OFFERS→AGREED→CONTACT→CLOSED, geo_distance_m() (гаверсинус, ОВ-15/ОВ-19), get_setting(), Порядок откликов offer_sort_1..3 / offer_sort_key(), Прогноз цены (forecast_window_min, forecast_grid_m, ОВ-10), Рейтинг total_rating = 0,5×system + 0,5×review (ОВ-11) (+4 more)

### Community 43 - "CLAUDE.md"
Cohesion: 0.15
Nodes (7): Окружение и запуск (JAVA_HOME, PostgreSQL 17.6, эмуляторы 5554/5556, MP_DB_PASSWORD), Ежечасный бэкап hourly_backup.ps1 и продолжение после обрыва, Договорённости, О проекте, Структура, Текущее состояние и решения, Запуск: mp_service.sql, :server:run, adb reverse, installDebug, тесты

### Community 44 - "Архитектура БД mp_service v0.7"
Cohesion: 0.15
Nodes (12): Архитектура БД mp_service v0.7, Глоссарий glossary / glossary_text (тексты ui.* на RUS, ENG), Настройки service_setting (наследование по дереву, get_setting), UI_TEMPLATE: ключи screens (deal_form, provider_request, slot_form) для A/B/C, v_deal_offer_list — смещённые точки (md5 с секретом), интервал расстояния, Экраны прототипа и раскладки A/B/C (раздел 5.3), Функция БД gloss(glossary_id, lang) — текст с запасным русским, Раздел «Поставщик»: ProviderScreen, мои сервисы и публикации (Tab.PROVIDER по ролям) (+4 more)

### Community 45 - "ProviderViewModel"
Cohesion: 0.24
Nodes (7): ProviderContent(), ProviderScreen(), ProviderState, ProviderViewModel, SectionTitle(), ServiceCard(), SlotRow()

### Community 46 - "agent_role"
Cohesion: 0.19
Nodes (13): agent_role, agent_service, agent_subscription, package_service, provider_limit(), service_package, trg_agent_role_dict, trg_agent_service_assign (+5 more)

### Community 47 - "Таблица dictionary (общий справочник, di"
Cohesion: 0.15
Nodes (11): Триггер check_dict_type() (ОВ-22), Основной документ ТЗ в Claude Docs, Таблица dictionary (общий справочник, dict_type), Справочник MAP_BASE_LAYER / OSM_OFFLINE_ALMATY (ОВ-20), Ссылки на маршрут MAP_ROUTE_LINK (Яндекс, 2ГИС, Google), Схема mp_service (27 таблиц, 10 представлений, 6 функций), Открытые вопросы ОВ-1…ОВ-24, Подпись «© участники OpenStreetMap» (+3 more)

### Community 48 - "provider_can_offer(deal_id, agent_id) — "
Cohesion: 0.19
Nodes (12): provider_can_offer(deal_id, agent_id) — все условия подбора, v_deal_candidates — источник входящих запросов и автооткликов, Схема подбора такси по пути, Взаимодействие: публикация, запрос, отклик, согласование, DealApi module (client), GraphHopper + kazakhstan-latest.osm.pbf (gh-cache), ProviderApi module, Routing module (GraphHopper 10.2, car profile, on-the-way check) (+4 more)

### Community 49 - "ApiClient"
Cohesion: 0.21
Nodes (3): ApiClient, HttpResult, SessionStore

### Community 50 - "OfferViewModel"
Cohesion: 0.20
Nodes (5): OfferDialog(), OfferEvent, OfferViewModel, ReviewFirst, Sent

### Community 51 - "Tab"
Cohesion: 0.26
Nodes (10): ActionIcon(), AppFrame(), BottomTabs(), Tab, DEALS, HOME, PROFILE, PROVIDER (+2 more)

### Community 52 - "service"
Cohesion: 0.23
Nodes (12): get_setting(), ix_provider_slot_search, provider_slot, service, service_attribute, service_base_id(), service_covers(), service_setting (+4 more)

### Community 53 - "AppViewModel"
Cohesion: 0.20
Nodes (6): AppState, AppViewModel, Failed, LoggedOut, Ready, Starting

### Community 54 - "Nav"
Cohesion: 0.20
Nodes (3): Nav, ServiceSettingsRoute, SlotRoute

### Community 55 - "Шаг 3: основа приложения — core/time, Po"
Cohesion: 0.20
Nodes (6): DealRoute, DealsRoute, RequestRoute, ReviewRoute, offererNoteKey(), Шаг 3: основа приложения — core/time, Polyline, onMapTap, NotificationCenter, маршруты навигации

### Community 58 - "agent"
Cohesion: 0.31
Nodes (9): agent, agent_login, agent_login_check(), referral_reward, subscription_payment, trg_agent_testing, trg_referral_reward_dict, ui_config (+1 more)

### Community 59 - "glossary"
Cohesion: 0.31
Nodes (9): agent_attribute, agent_pseudonym(), attribute, gloss(), glossary, glossary_text, icon, interface_lang (+1 more)

### Community 60 - "Дерево услуг service: категория → сервис"
Cohesion: 0.22
Nodes (9): Антифрод и agent_sanction (эскалация санкций), glossary / glossary_text (термины и переводы), Пиктограммы icon (SVG currentColor), service_attribute (поля profile/request/offer, show_stage), Дерево услуг service: категория → сервис → вариант, show_stage = near (открытие гос. номера/телефона при сближении), ТОП-10 сервисов (парковка, такси, курьер … репетитор), ui_config (конфигурация интерфейса, контрольная сумма) (+1 more)

### Community 61 - "Сделка deal"
Cohesion: 0.25
Nodes (9): Сделка deal, Отклик deal_offer, Отчёты по выручке (v_agent_finance_daily, v_company_finance_daily), Монетизация: абонплата за пакеты, Оплата сделки DIRECT / PLATFORM (ОВ-8), Реферальное вознаграждение 10/5/2% (pending→accrued→paid_out), Пакеты service_package / package_service, Жизненный цикл подписки (trial, paid, failed, refund) (+1 more)

### Community 64 - "LatLon"
Cohesion: 0.43
Nodes (3): LatLon, MapLine, MapZone

### Community 66 - "notification"
Cohesion: 0.29
Nodes (7): agent_position, ix_agent_position_update, ix_notification_agent, notification, run_deal_jobs(), trg_agent_position, trg_notification_dict

### Community 67 - "Подбор поставщиков п. 2.1–2.10 (гео по p"
Cohesion: 0.33
Nodes (7): Автоотклик (п. 3.1, is_auto), Подбор поставщиков п. 2.1–2.10 (гео по place_mode, время, цена, рейтинг, доступ, лимит), Такси по пути: маршрут А→Б, коридор ±rA, радиус rB, направление, Таблица agent_service (настройки сервиса поставщика), Что считает сервер: маршруты GraphHopper, код закрытия, плановые задания, Настройки сервиса: ServiceSettingsScreen, только изменённые поля, очистка null, PUT /me/services/{serviceId} — настройки поставщика

### Community 69 - "MainNav()"
Cohesion: 0.50
Nodes (3): App(), MainNav(), tabsFor()

### Community 71 - "Монетизация"
Cohesion: 0.40
Nodes (5): Жизненный цикл подписки, Монетизация, Отчёты по выручке, Пакеты, Реферальное вознаграждение

### Community 72 - "Отклики, карта и маршрут"
Cohesion: 0.40
Nodes (5): Карта в приложении, Кейс A/B/C/D, Отклики, карта и маршрут, Правила, Ссылки на маршрут

### Community 73 - "Отмена: CANCEL_FREE / CANCEL_CLIENT / CA"
Cohesion: 0.40
Nodes (5): Согласование п. 4.4: selected / declined / withdrawn, Отмена: CANCEL_FREE / CANCEL_CLIENT / CANCEL_PROVIDER, Обязательные баллы (п. 5.6, В-11), Уведомление OFFER_DECLINED невыбранным поставщикам, Сделка глазами поставщика: роли candidate / offerer / provider

### Community 74 - "ApiServer (routes, lang, DB errors to HT"
Cohesion: 0.40
Nodes (4): ApiServer (routes, lang, DB errors to HTTP), DB rule functions (provider_can_offer, deal_agree, deal_cancel, make_auto_offers, price_forecast, run_deal_jobs, agent_login_check), Error model {error, code} with HTTP 400/401/403/404/409/422, Jobs module (run_deal_jobs every minute, taxi route requests)

### Community 76 - "deal_offer"
Cohesion: 0.50
Nodes (4): deal_attribute, deal_offer, ix_deal_offer_provider, uq_deal_offer_selected

### Community 78 - "Процесс сделки"
Cohesion: 0.50
Nodes (4): Отмена, Процесс сделки, Цена, Этапы

### Community 79 - "Комплект документации project_osm_map v0"
Cohesion: 0.50
Nodes (4): Общая архитектура продукта v0.7, Комплект документации project_osm_map v0.7, ТЗ v0.6 (docs/TZ_marketplace.md), TZ_project_WIN_SERVICE.txt (исходный алгоритм)

## Knowledge Gaps
- **90 isolated node(s):** `Loading`, `ALL`, `DARK`, `LIGHT`, `ConfigApi module` (+85 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 378 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **35 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `ТЗ: платформа-маркетплейс услуг` connect `ТЗ маркетплейса v0.6` to `Отклики, карта и маршрут`, `README и OpenAPI`, `Процесс сделки`, `Монетизация`?**
  _High betweenness centrality (0.106) - this node is a cross-community bridge._
- **What connects `Loading`, `ALL`, `DARK` to the rest of the system?**
  _90 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Сервер API: HTTP и транзакции` be split into smaller, more focused modules?**
  _Cohesion score 0.06460959003331884 - nodes in this community are weakly interconnected._
- **Why does `План этапа Э4 — экраны клиента` connect `План Э4 — экраны клиента` to `Время по Алматы и таблицы`, `Сервер API: HTTP и транзакции`, `Экран сделки A/B/C`, `Экран запроса клиента`, `Экран баллов`, `Р-4, итог Э5 и следующий этап`, `DealsScreen()`, `Шаг 3: основа приложения — core/time, Po`, `Центр уведомлений`, `Главная: модель`?**
  _High betweenness centrality (0.103) - this node is a cross-community bridge._
- **Should `Входящие запросы поставщика` be split into smaller, more focused modules?**
  _Cohesion score 0.06886338393187708 - nodes in this community are weakly interconnected._
- **Why does `Процесс сделки` connect `Процесс сделки` to `Смещение точки до согласования approx_of`, `ТЗ маркетплейса v0.6`?**
  _High betweenness centrality (0.093) - this node is a cross-community bridge._
- **Should `Экран сделки A/B/C` be split into smaller, more focused modules?**
  _Cohesion score 0.08367271380970011 - nodes in this community are weakly interconnected._