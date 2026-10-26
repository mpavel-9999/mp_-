# Graph Report - github_repo  (2026-10-26)

## Corpus Check
- 14 files · ~132,128 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 1765 nodes · 4867 edges · 107 communities (74 shown, 33 thin omitted)
- Extraction: 97% EXTRACTED · 3% INFERRED · 0% AMBIGUOUS · INFERRED: 157 edges (avg confidence: 0.87)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- Публикация: раскладки
- Экран сделки A/B/C
- Тема и палитра
- HTTP-тест api_smoke
- План М1 и замеры стенда
- Сервер API: HTTP и транзакции (2)
- Схема БД и рейтинг
- Модели данных API
- План Э1 — БД и правила
- Входящие запросы поставщика (2)
- Авторизация сервера
- Центр уведомлений
- Карта osmdroid
- План Э3 — каркас приложения
- Экран сделки A/B/C (2)
- Сервер API: HTTP и транзакции
- Навигация App.kt
- Экран сделки A/B/C (2)
- Иконки SVG
- Настройки сервиса поставщика
- Сервер API: HTTP и транзакции (4)
- Публикация: модель
- Настройки: модель
- Экран запроса клиента
- Тесты поставщика
- Таблицы сделки
- Репозитории и граф зависимостей (2)
- LoginViewModel
- Главная и карта
- Экран баллов
- ТЗ на реализацию v0.7
- Сетевой клиент и ошибки
- План Э4 — экраны клиента
- Центр уведомлений (2)
- Р-4, итог Э5 и следующий этап
- Репозиторий сделок
- ТЗ маркетплейса v0.6
- Экран запроса клиента (2)
- Экран баллов (2)
- Экран сделки A/B/C (3)
- CLAUDE.md
- Nav
- OfferViewModel
- Время по Алматы и таблицы
- Каркас раскладок и поля даты
- Репозитории и граф зависимостей
- CLAUDE.md (2)
- Сервер API: HTTP и транзакции (5)
- ProviderViewModel
- CloseConfirmViewModel
- Профиль
- README и OpenAPI
- Маршрутизация GraphHopper
- Конфигурация интерфейса
- Смещение точки до согласования approx_of
- Авторизация сервера (2)
- agent_role
- Таблица dictionary (общий справочник, di
- JVM-тесты ядра
- service
- ТЗ на реализацию v0.7 (2)
- Карта osmdroid (2)
- AppViewModel
- Texts
- Экран запроса клиента (3)
- 04_close.sql
- decodeBody()
- Архитектура БД mp_service v0.7
- Входящие запросы поставщика (2) (2)
- agent
- glossary
- Сделка deal
- ProviderViewModel (2)
- Nav (2)
- Настройки сервиса поставщика (2)
- MainActivity.kt
- Модели данных API (2)
- Экран запроса клиента (4)
- 03_period.sql
- Дерево услуг service: категория → сервис
- Процесс сделки
- Карта osmdroid (2)
- 01_rules.sql
- Монетизация
- Отклики, карта и маршрут
- Отмена: CANCEL_FREE / CANCEL_CLIENT / CA
- README и OpenAPI (2)
- ApiClient
- deal_offer
- 02_cancel_mode.sql
- provider_can_offer(deal_id, agent_id) — 
- Db module (connection, session params, t
- Комплект документации project_osm_map v0
- Auth module (in-memory bearer tokens 12h
- Р-4, итог Э5 и следующий этап (2)
- ConfigApi module
- DB views (v_deal_offer_list, v_deal_cand
- Http module (body, responses, errors)
- Р-4, итог Э5 и следующий этап (3)
- Архитектура БД mp_service v0.7 (2)

## God Nodes (most connected - your core abstractions)
1. `AppGraph` - 42 edges
2. `LatLon` - 41 edges
3. `Nav` - 37 edges
4. `Deal` - 36 edges
5. `DealRepository` - 36 edges
6. `DealApi` - 34 edges
7. `DealState` - 31 edges
8. `DealViewModel` - 29 edges
9. `MpMap()` - 29 edges
10. `RequestState` - 24 edges

## Surprising Connections (you probably didn't know these)
- `Пароль и host БД не хранятся: запрашивать у владельца, только в переменных окружения (MP_DB_PASSWORD)` --semantically_similar_to--> `Приложение не подключается к БД напрямую: пароль только у сервера`  [INFERRED] [semantically similar]
  CLAUDE.md → project_osm_map/README.md
- `Модели и репозитории data/ (@Serializable по openapi.yaml, тонкие репозитории)` --implements--> `Offer`  [INFERRED]
  project_osm_map/docs/v0.7/plans/E3_app.md → project_osm_map/app/src/main/java/ru/mpavel/osmmap/data/Models.kt
- `Шаг 5: Запрос — гео point / route / none, поля формы, прогноз через 500 мс, «Моя цена» = медиана` --references--> `ReviewFirst`  [INFERRED]
  project_osm_map/docs/v0.7/plans/E4_client.md → project_osm_map/app/src/main/java/ru/mpavel/osmmap/feature/request/RequestViewModel.kt
- `Тема MpTheme из палитры → ColorScheme Material 3 + MpColors` --implements--> `MpColors`  [INFERRED]
  project_osm_map/docs/v0.7/plans/E3_app.md → project_osm_map/app/src/main/java/ru/mpavel/osmmap/core/theme/MpTheme.kt
- `Сетевой слой core/net/ (ApiClient, ApiError, NetworkError, SessionStore)` --implements--> `ApiClient`  [INFERRED]
  project_osm_map/docs/v0.7/plans/E3_app.md → project_osm_map/app/src/main/java/ru/mpavel/osmmap/core/net/ApiClient.kt

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Конфигурация сервиса данными (дерево, настройки, поля, глоссарий)** — project_osm_map_docs_tz_marketplace_service_tree, project_osm_map_docs_tz_marketplace_service_setting, project_osm_map_docs_tz_marketplace_service_attribute, project_osm_map_docs_tz_marketplace_glossary, project_osm_map_docs_tz_marketplace_get_setting [EXTRACTED 1.00]
- **Этапы масштабирования БД М1–М3** — project_osm_map_docs_v0_7_03_implementation_spec_m1, project_osm_map_docs_v0_7_03_implementation_spec_m2, project_osm_map_docs_v0_7_03_implementation_spec_m3, project_osm_map_docs_v0_7_05_db_scaling_two_phase_matching, project_osm_map_docs_v0_7_05_db_scaling_partitioning, project_osm_map_docs_v0_7_05_db_scaling_connection_pool [EXTRACTED 1.00]
- **Правила сделки в БД: функции, триггеры, переходы, плановое задание** — project_osm_map_docs_v0_7_plans_e1_db_provider_can_offer, project_osm_map_docs_v0_7_plans_e1_db_deal_agree, project_osm_map_docs_v0_7_plans_e1_db_deal_cancel, project_osm_map_docs_v0_7_plans_e1_db_run_deal_jobs, project_osm_map_docs_v0_7_plans_e1_db_trg_deal_status, project_osm_map_docs_v0_7_plans_e1_db_deal_transition [EXTRACTED 1.00]
- **E2 server module split (ApiServer, Db, Http, Auth, ConfigApi, DealApi, ProviderApi, Routing, CloseCode, Jobs)** — project_osm_map_docs_v0_7_plans_e2_api_apiserver, project_osm_map_docs_v0_7_plans_e2_api_db, project_osm_map_docs_v0_7_plans_e2_api_http, project_osm_map_docs_v0_7_plans_e2_api_auth, project_osm_map_docs_v0_7_plans_e2_api_configapi, project_osm_map_docs_v0_7_plans_e2_api_dealapi, project_osm_map_docs_v0_7_plans_e2_api_providerapi, project_osm_map_docs_v0_7_plans_e2_api_routing, project_osm_map_docs_v0_7_plans_e2_api_closecode, project_osm_map_docs_v0_7_plans_e2_api_jobs [EXTRACTED 1.00]
- **Интерфейс из конфигурации: кэш по сумме → тема, тексты, пиктограммы** — project_osm_map_docs_v0_7_plans_e3_app_config_checksum_cache, project_osm_map_docs_v0_7_plans_e3_app_theme_palette, project_osm_map_docs_v0_7_plans_e3_app_texts_glossary, project_osm_map_docs_v0_7_plans_e3_app_svg_icons, project_osm_map_docs_v0_7_plans_e3_app_settings_feature [EXTRACTED 1.00]
- **Одна логика — три раскладки откликов и сделки (A/B/C)** — project_osm_map_docs_v0_7_plans_e4_client_principle_three_layouts, project_osm_map_app_src_main_java_ru_mpavel_osmmap_feature_deal_dealviewmodel_dealviewmodel, project_osm_map_app_src_main_java_ru_mpavel_osmmap_ui_template_a_deallayoutsa_offersa, project_osm_map_app_src_main_java_ru_mpavel_osmmap_ui_template_b_deallayoutsb_offersb, project_osm_map_app_src_main_java_ru_mpavel_osmmap_ui_template_c_deallayoutsc_offersc, project_osm_map_app_src_main_java_ru_mpavel_osmmap_ui_template_a_deallayoutsa_deala, project_osm_map_app_src_main_java_ru_mpavel_osmmap_ui_template_b_deallayoutsb_dealb, project_osm_map_app_src_main_java_ru_mpavel_osmmap_ui_template_c_deallayoutsc_dealc [EXTRACTED 1.00]
- **Экраны поставщика Э5: раздел → публикация → входящие → сделка → настройки** — project_osm_map_docs_v0_7_plans_e5_provider_provider_screen, project_osm_map_docs_v0_7_plans_e5_provider_slot_screens, project_osm_map_docs_v0_7_plans_e5_provider_incoming, project_osm_map_docs_v0_7_plans_e5_provider_deal_roles, project_osm_map_docs_v0_7_plans_e5_provider_service_settings [EXTRACTED 1.00]
- **Р-4: время по Алматы — сервер, БД, приложение** — project_osm_map_docs_v0_7_plans_e5_provider_trg_period_check, project_osm_map_docs_v0_7_plans_e5_provider_now_mp [EXTRACTED 1.00]
- **Вход тестовых пользователей по хешу пароля** — project_osm_map_docs_v0_7_plans_e1_db_step_12a_test_users, project_osm_map_docs_v0_7_plans_e1_db_agent_login, project_osm_map_docs_v0_7_plans_e1_db_agent_login_check [EXTRACTED 1.00]
- **Меры против обхода платформы до согласования** — project_osm_map_docs_tz_marketplace_approx_offset, project_osm_map_docs_tz_marketplace_pseudonym, project_osm_map_docs_tz_marketplace_show_stage_near, project_osm_map_docs_tz_marketplace_antifraud [INFERRED 0.85]
- **Режим отмены: одно правило БД → поле API → диалог отмены** — project_osm_map_docs_v0_7_plans_e4_client_deal_cancel_mode, project_osm_map_app_src_main_java_ru_mpavel_osmmap_feature_deal_dealscreen_canceldialog, project_osm_map_docs_v0_7_plans_e4_client_test_02_cancel_mode [INFERRED 0.85]
- **Поток сближения и закрытия сделки (Э6)** — project_osm_map_docs_v0_7_plans_e6_close_position_sender, project_osm_map_server_openapi_me_position, project_osm_map_docs_v0_7_plans_e6_close_deal_contact_check, project_osm_map_server_openapi_close_endpoints, project_osm_map_docs_v0_7_plans_e6_close_feature_close, project_osm_map_docs_v0_7_03_implementation_spec_close_code [INFERRED 0.85]
- **Одна логика — три раскладки A/B/C** — project_osm_map_docs_v0_7_plans_e3_app_principle_one_logic_three_layouts, project_osm_map_app_src_main_java_ru_mpavel_osmmap_ui_template_a_homea_homea, project_osm_map_app_src_main_java_ru_mpavel_osmmap_ui_template_b_homeb_homeb, project_osm_map_app_src_main_java_ru_mpavel_osmmap_ui_template_c_homec_homec [INFERRED 0.85]
- **Taxi on-the-way matching (server-side routing, slots, requests, forecast)** — project_osm_map_docs_v0_7_plans_e2_api_routing, project_osm_map_docs_v0_7_plans_e2_api_graphhopper [INFERRED 0.85]
- **Время без часового пояса: истечение запросов и вопрос Р-4** — project_osm_map_docs_v0_7_plans_e4_client_timezone [INFERRED 0.95]

## Communities (107 total, 33 thin omitted)

### Community 0 - "Публикация: раскладки"
Cohesion: 0.05
Nodes (62): DealListItem, IncomingRequest, PendingReview, ServiceNode, CloseConfirmState, DealsSummary(), HomeActions, HomeScreen() (+54 more)

### Community 1 - "Экран сделки A/B/C"
Cohesion: 0.09
Nodes (44): CloseButton, CLOSE, ENTER_CODE, SHOW_CODE, TEST_PROVIDE, TEST_SCAN, closeButtons(), CancelDialog() (+36 more)

### Community 2 - "Тема и палитра"
Cohesion: 0.06
Nodes (19): findActivity(), googleFamily(), Mp, MpColors, MpFonts, MpShapes, MpTheme(), withFamily() (+11 more)

### Community 3 - "HTTP-тест api_smoke"
Cohesion: 0.11
Nodes (26): call(), check(), main(), agree(), close(), config(), contact(), create() (+18 more)

### Community 4 - "План М1 и замеры стенда"
Cohesion: 0.08
Nodes (26): Сейчас: М1 шаг 2 в работе, следующий — HTTP-замер load_http.py на сервере со стендом, Планы EXPLAIN (ANALYZE, BUFFERS) «как есть» Q1–Q9 — plans/M1_bench/before, План Q1 «как есть»: GET /notifications?since= — индекс по агенту, 0,3 мс, План Q2 «как есть»: PUT /me/position с deal_contact_check — 19 мс, План Q3 «как есть»: GET /requests: v_deal_candidates для поставщика — 23 с, План Q4 «как есть»: вставка сделки (без подбора) — 11 мс, План Q5 «как есть»: Мои сделки без индекса по клиенту и исполнителю — 272 мс, План Q6 «как есть»: карточка сделки: роль и поиск CONTACT в deal_status_log — 420 мс (+18 more)

### Community 6 - "Схема БД и рейтинг"
Cohesion: 0.07
Nodes (9): agent_has_rating(), agent_position, agent_rating(), deal_contact_check(), g_src, ix_agent_position_update, rating_history, trg_agent_position (+1 more)

### Community 7 - "Модели данных API"
Cohesion: 0.14
Nodes (26): BaseLayer, CancelReason, CancelRequest, CancelResult, DevLoginRequest, FieldOption, FontSpec, Forecast (+18 more)

### Community 8 - "План Э1 — БД и правила"
Cohesion: 0.10
Nodes (24): Таблица agent_login (хеш bcrypt через pgcrypto), agent_login_check(login, password) — crypt() pgcrypto, Кейс A/B/C/D: порядок откликов B, D, C; E отсечён правилом В-1, deal_agree(), Справочник DEAL_TRANSITION (18 переходов), Режим загрузки mp.load_mode (is_load_mode, RESET), trg_agent_testing / trg_agent_service_assign / trg_agent_position, План этапа Э1 — БД v0.7 (+16 more)

### Community 9 - "Входящие запросы поставщика (2)"
Cohesion: 0.11
Nodes (8): CloseCode, Notification, autoConfirm(), closeCodeFrom(), codeSecondsLeft(), qrDealId(), qrMatrix(), CloseFlowTest

### Community 10 - "Авторизация сервера"
Cohesion: 0.08
Nodes (5): CloseCode, ApiException, Http, Request, Response

### Community 12 - "Центр уведомлений"
Cohesion: 0.11
Nodes (3): Fix, PositionSender, positionUpdate()

### Community 14 - "План Э3 — каркас приложения"
Cohesion: 0.14
Nodes (18): NetworkError, E3_app.md — план этапа Э3, Конфигурация с кэшем по контрольной сумме (GET /config?checksum= → 304/200), Снимки экранов E3_screens/, Проверка на эмуляторе: сценарии U-1…U-4, Библиотеки и версии (вопрос Р-2): Compose BOM 2026.09.00, OkHttp 5.5.1, Coil 3.6.3, navigation 2.10.2 и др., Вход feature/login/ (GET /dev/agents при MP_DEV_LOGIN=1, иначе POST /session), Сетевой слой core/net/ (ApiClient, ApiError, NetworkError, SessionStore) (+10 more)

### Community 15 - "Экран сделки A/B/C (2)"
Cohesion: 0.14
Nodes (5): CloseCode, CloseConfirm, DealEvent, DealViewModel, Review

### Community 16 - "Сервер API: HTTP и транзакции"
Cohesion: 0.18
Nodes (4): Db, Work, Jobs, ProviderApi

### Community 17 - "Навигация App.kt"
Cohesion: 0.11
Nodes (7): HomeRoute, IncomingRoute, ProfileRoute, ProviderRoute, ServiceSettingsRoute, SettingsRoute, SlotRoute

### Community 18 - "Экран сделки A/B/C (2)"
Cohesion: 0.12
Nodes (4): CloseCodeScreen(), CloseCodeState, CloseCodeViewModel, QrImage()

### Community 19 - "Иконки SVG"
Cohesion: 0.12
Nodes (4): hex(), IconSet, MpIcon(), svgBitmap()

### Community 20 - "Настройки сервиса поставщика"
Cohesion: 0.19
Nodes (8): NumField(), ReadOnlyRow(), ServiceSettingsScreen(), ServiceSettingsViewModel, SettingsCheck, SettingsContent(), SettingsState, TextField()

### Community 21 - "Сервер API: HTTP и транзакции (4)"
Cohesion: 0.22
Nodes (3): Шаг 2: API — поле cancel_mode в DealApi.SQL_DEAL и схеме Deal, CodeParams, DealApi

### Community 22 - "Публикация: модель"
Cohesion: 0.13
Nodes (6): SlotCreate, buildSlotCreate(), SlotTarget, A, B, SlotViewModel

### Community 23 - "Настройки: модель"
Cohesion: 0.18
Nodes (9): ConfigUpdate, UiOption, SchemeFilter, ALL, DARK, LIGHT, SettingsScreen(), SettingsState (+1 more)

### Community 24 - "Экран запроса клиента"
Cohesion: 0.13
Nodes (4): RequestViewModel, RouteTarget, DEST, PICKUP

### Community 25 - "Тесты поставщика"
Cohesion: 0.20
Nodes (10): AttrValue, FormField, MyOffer, Offer, OfferCreate, offererNoteKey(), ownStartPoint(), buildOfferCreate() (+2 more)

### Community 26 - "Таблицы сделки"
Cohesion: 0.13
Nodes (21): agent_pending_reviews(), agent_sanction, check_dict_type(), deal, deal_review, deal_status_log, dictionary, ix_deal_status_period (+13 more)

### Community 27 - "Репозитории и граф зависимостей (2)"
Cohesion: 0.14
Nodes (4): StatusOk, NotificationRepository, ProviderRepository, SessionRepository

### Community 28 - "LoginViewModel"
Cohesion: 0.22
Nodes (7): ApiError, AppError, AgentList(), LoginScreen(), LoginState, LoginViewModel, PasswordForm()

### Community 30 - "Экран баллов"
Cohesion: 0.16
Nodes (5): ReviewRequest, ReviewScreen(), ReviewState, ReviewViewModel, Шаг 7: Баллы — оценка 1–5 обязательна, «Назад» не уводит

### Community 31 - "ТЗ на реализацию v0.7"
Cohesion: 0.14
Nodes (19): История этапов: Э0–Э5 (2026-10-25…26), Э6 закрыт 2026-10-26, Код закрытия (раздел 6): HMAC от close_secret, TTL сервиса, QR или цифры, Э6. Режим тестирования и закрытие (разделы 5.4, 6; S-4…S-8), Э7. Тестирование на двух эмуляторах (S-1…S-16), Тестирование на двух эмуляторах (раздел 8): запуск, сценарии, отчёт, Сближение и закрытие (6.2), Плановые задания раз в минуту: EXPIRED, CLOSING_ASK, NOT_CLOSED, CONTACT, Модель нагрузки: 100 000 агентов, 25 000 поставщиков, 20 000 сделок в день, 10 000 онлайн (+11 more)

### Community 32 - "Сетевой клиент и ошибки"
Cohesion: 0.15
Nodes (3): ApiClient, ErrorBody, HttpResult

### Community 33 - "План Э4 — экраны клиента"
Cohesion: 0.13
Nodes (15): План этапа Э4 — экраны клиента, Тестовые данные: кейс A/B/C/D (сделка 6 — парковка, сделка 7 — такси), Проверка связности плана Э4 (порядок, алгоритм, шаблоны, данные, доступ, безопасность), Функция БД deal_cancel_mode(deal_id, agent_id) → CANCEL_FREE / CLIENT / PROVIDER / NULL, coreLibraryDesugaring desugar_jdk_libs 2.1.5 (java.time при minSdk 24), Найдено и исправлено при проверке (карта под шторкой A, перецентровка, обновление главной, «Назад» на баллах), Цель Э4: путь «услуга → запрос → отклики → согласование → сделка → отмена или баллы» в A/B/C, Вопросы владельцу В-Э4-1…В-Э4-4 и решение 25.10.2026 (+7 more)

### Community 35 - "Р-4, итог Э5 и следующий этап"
Cohesion: 0.16
Nodes (17): Открытые вопросы: Р-4 часовой пояс (Asia/Almaty, реализован в Э5), tree-sitter-sql для графа, отправка ветки в GitHub, В-БД-1…5 (решены: этапы М1–М3), М1. Основа и замеры: генератор нагрузки mp_load, замер как есть, пул соединений, индексы, numeric → bigint, параметры PostgreSQL, pg_stat_statements, М2. Подбор поставщиков: PostGIS, GiST, плоское дерево услуг, текущий рейтинг, deal_candidate, М3. Журналы и статистика: партицирование notification, deal_status_log, rating_history; суточные агрегаты, Порядок этапов: Э0…Э6 → М1 → М2 → М3 → Э7 → Э8, Нефункциональные требования: секреты в переменных, подпись OSM, контраст, PostGIS и push при росте, Анализ БД: нагрузка 100 000 пользователей, статистика и партицирование, Суточные агрегаты: stat_agent_daily, stat_price_cell, stat_service_daily (+9 more)

### Community 36 - "Репозиторий сделок"
Cohesion: 0.19
Nodes (3): DealRepository, enc(), ServiceRepository

### Community 37 - "ТЗ маркетплейса v0.6"
Cohesion: 0.11
Nodes (18): Антифрод, Изменения относительно исходной модели, Как добавить сервис, Модель данных, Настройки сервисов, Основные понятия, Открытые вопросы, Пиктограммы (+10 more)

### Community 38 - "Экран запроса клиента (2)"
Cohesion: 0.18
Nodes (4): formatPeriod(), formatShort(), nowMp(), parseServerTime()

### Community 39 - "Экран баллов (2)"
Cohesion: 0.23
Nodes (7): apiErrorText(), ErrorBox(), errorKey(), errorMessage(), LoadingBox(), textOr(), Термины ошибок ui.err.* (review_required, no_service_access, required_fields, offer_not_active, busy, routing_unavailable, no_route, rule)

### Community 41 - "CLAUDE.md"
Cohesion: 0.15
Nodes (9): Сейчас (2026-10-26): этап М1 выполняется — план принят, шаги 0–1 выполнены (стенд mp_load: 100 000 агентов, 1 млн сделок, 3,2 ГБ), шаг 2 в работе — SQL-замер «как есть» сделан (Q3 подбор 22–478 с, Q4 новый запрос > 15 мин, Q8 карта 2,8 с), HTTP-замер не сделан; лимит 5 ч — 86 %, после шага 2 пауза; шаг 6 — перезапуск PostgreSQL владельцем, Окружение и запуск: JBR Android Studio, PostgreSQL 17.6, эмуляторы Medium_Phone_API_37.0 / Medium_Phone_B, adb reverse 8080, Ежечасный бэкап tools/backup/hourly_backup.ps1 (repo.bundle, refs/backup/<время>), Пользователи user_testing_1…5 (агенты 7–11, режим тестирования) и user_1…5 (агенты 12–16, обычные клиенты), Договорённости, О проекте, Структура, Текущее состояние и решения (+1 more)

### Community 42 - "Nav"
Cohesion: 0.17
Nodes (6): App(), CloseCodeRoute, CloseConfirmRoute, DealRoute, MainNav(), Nav

### Community 43 - "OfferViewModel"
Cohesion: 0.20
Nodes (6): OfferDialog(), OfferEvent, OfferViewModel, ReviewFirst, Sent, invalidFields()

### Community 44 - "Время по Алматы и таблицы"
Cohesion: 0.13
Nodes (14): Закрытие сделки кодом QR / KEY (HMAC по интервалу), Подбор поставщиков п. 2.1–2.10 (гео по place_mode, время, цена, рейтинг, доступ, лимит), Публикация (окно) поставщика, Такси по пути: маршрут А→Б, коридор ±rA, радиус rB, направление, Режим тестирования администратора (курсор, «Сканировать / Предоставить QR»), Таблица deal (сделка / запрос клиента), Таблица deal_offer (отклики, offer_status), provider_can_offer(deal_id, agent_id) — все условия подбора (+6 more)

### Community 47 - "CLAUDE.md (2)"
Cohesion: 0.14
Nodes (10): Общая архитектура продукта, Антифрод (6.6), Жизненный цикл сделки, Дорожная карта: прототип v0.7 → регистрация, финкабинет, push → оплата, PostGIS, рабочий контур, Подбор такси по пути (6.4, GraphHopper), Шаблоны интерфейса A «Карта в центре», B «Карточки», C «Диспетчер» × LIGHT/DARK, Режим тестирования: закрытие без камеры (6.3), Шаги Э6 0–9: подготовка, БД сближение, API, основа приложения, положение, код и подтверждение, режим тестирования, онлайн-услуга, проверка на двух эмуляторах, фиксация (+2 more)

### Community 48 - "Сервер API: HTTP и транзакции (5)"
Cohesion: 0.27
Nodes (3): ApiServer, Handler, Route

### Community 49 - "ProviderViewModel"
Cohesion: 0.31
Nodes (8): Slot, ProviderContent(), ProviderScreen(), ProviderState, ProviderViewModel, SectionTitle(), ServiceCard(), SlotRow()

### Community 50 - "CloseConfirmViewModel"
Cohesion: 0.17
Nodes (3): CloseConfirmScreen(), CloseConfirmViewModel, scannerUnavailable()

### Community 51 - "Профиль"
Cohesion: 0.25
Nodes (8): Failed, Loaded, Loading, ProfileContent(), ProfileScreen(), ProfileState, ProfileViewModel, roleKey()

### Community 52 - "README и OpenAPI"
Cohesion: 0.13
Nodes (14): Загрузка конфигурации интерфейса по контрольной сумме (6.5), E2 plan: API v0.7, api_smoke.py HTTP smoke test (16/16), ApiServer (routes, lang, DB errors to HTTP), 02_db_architecture.md (sections 6.3, 8), DB rule functions (provider_can_offer, deal_agree, deal_cancel, make_auto_offers, price_forecast, run_deal_jobs, agent_login_check), E1_db.md (E1 plan), Error model {error, code} with HTTP 400/401/403/404/409/422 (+6 more)

### Community 53 - "Маршрутизация GraphHopper"
Cohesion: 0.21
Nodes (3): Projection, Route, Routing

### Community 55 - "Смещение точки до согласования approx_of"
Cohesion: 0.19
Nodes (12): Антифрод и agent_sanction (эскалация санкций), Кейс A/B/C/D (сделка 6, порядок B, D, C), Отмена сделки (CANCEL_FREE/CLIENT/PROVIDER, EXPIRED), Статусы сделки INIT→MATCHING→OFFERS→AGREED→CONTACT→CLOSED, geo_distance_m() (гаверсинус, ОВ-15/ОВ-19), get_setting(), Порядок откликов offer_sort_1..3 / offer_sort_key(), Рейтинг total_rating = 0,5×system + 0,5×review (ОВ-11) (+4 more)

### Community 57 - "agent_role"
Cohesion: 0.19
Nodes (13): agent_role, agent_service, agent_subscription, package_service, provider_limit(), service_package, trg_agent_role_dict, trg_agent_service_assign (+5 more)

### Community 58 - "Таблица dictionary (общий справочник, di"
Cohesion: 0.15
Nodes (11): Триггер check_dict_type() (ОВ-22), Основной документ ТЗ в Claude Docs, Таблица dictionary (общий справочник, dict_type), Справочник MAP_BASE_LAYER / OSM_OFFLINE_ALMATY (ОВ-20), Ссылки на маршрут MAP_ROUTE_LINK (Яндекс, 2ГИС, Google), Схема mp_service (27 таблиц, 10 представлений, 6 функций), Открытые вопросы ОВ-1…ОВ-24, Подпись «© участники OpenStreetMap» (+3 more)

### Community 59 - "JVM-тесты ядра"
Cohesion: 0.27
Nodes (3): tintSvg(), parseHexColor(), CoreTest

### Community 60 - "service"
Cohesion: 0.23
Nodes (12): get_setting(), ix_provider_slot_search, provider_slot, service, service_attribute, service_base_id(), service_covers(), service_setting (+4 more)

### Community 61 - "ТЗ на реализацию v0.7 (2)"
Cohesion: 0.20
Nodes (12): Алгоритм формирования сделки v0.7 (источник истины), Сценарии приёмки S-1…S-16, Антифрод: блокировки и санкции, Статусы сделки и переходы INIT→MATCHING→OFFERS→AGREED→CONTACT→CLOSED, Журнал решений В-1…В-13, Н-1…Н-12, П-1…П-6, trg_deal_status — проверка переходов и deal_status_log, ТЗ на реализацию прототипа v0.7 (для Claude), API v0.7 (раздел 4): авторизация, конфигурация, положение, клиент, поставщик, закрытие, уведомления (+4 more)

### Community 62 - "Карта osmdroid (2)"
Cohesion: 0.27
Nodes (4): LatLon, MapLine, MapZone, MapEventsReceiver

### Community 63 - "AppViewModel"
Cohesion: 0.20
Nodes (6): AppState, AppViewModel, Failed, LoggedOut, Ready, Starting

### Community 64 - "Texts"
Cohesion: 0.22
Nodes (3): t(), Texts, Тексты глоссария core/i18n/ (t("ui.key"), нет ключа — показывается ключ)

### Community 66 - "04_close.sql"
Cohesion: 0.22
Nodes (3): pg_temp.run_test(), pg_temp.status(), t_result

### Community 67 - "decodeBody()"
Cohesion: 0.42
Nodes (4): decodeBody(), DecodeError, ApiModelsTest, Найдено и исправлено при проверке (pseudonym null, clipToBounds, coroutineScope, DecodeError)

### Community 68 - "Архитектура БД mp_service v0.7"
Cohesion: 0.22
Nodes (10): Автоотклик (п. 3.1, is_auto), Архитектура БД mp_service v0.7, Таблица agent_service (настройки сервиса поставщика), Глоссарий glossary / glossary_text (тексты ui.* на RUS, ENG), Настройки service_setting (наследование по дереву, get_setting), UI_TEMPLATE: ключи screens (deal_form, provider_request, slot_form) для A/B/C, v_deal_offer_list — смещённые точки (md5 с секретом), интервал расстояния, Функция БД gloss(glossary_id, lang) — текст с запасным русским (+2 more)

### Community 70 - "agent"
Cohesion: 0.31
Nodes (9): agent, agent_login, agent_login_check(), referral_reward, subscription_payment, trg_agent_testing, trg_referral_reward_dict, ui_config (+1 more)

### Community 71 - "glossary"
Cohesion: 0.31
Nodes (9): agent_attribute, agent_pseudonym(), attribute, gloss(), glossary, glossary_text, icon, interface_lang (+1 more)

### Community 72 - "Сделка deal"
Cohesion: 0.25
Nodes (9): Сделка deal, Отклик deal_offer, Отчёты по выручке (v_agent_finance_daily, v_company_finance_daily), Монетизация: абонплата за пакеты, Оплата сделки DIRECT / PLATFORM (ОВ-8), Реферальное вознаграждение 10/5/2% (pending→accrued→paid_out), Пакеты service_package / package_service, Жизненный цикл подписки (trial, paid, failed, refund) (+1 more)

### Community 74 - "Nav (2)"
Cohesion: 0.25
Nodes (5): DealsRoute, RequestRoute, ReviewRoute, decodePolyline(), Шаг 3: основа приложения — core/time, Polyline, onMapTap, NotificationCenter, маршруты навигации

### Community 75 - "Настройки сервиса поставщика (2)"
Cohesion: 0.39
Nodes (6): MyService, buildServiceUpdate(), fmt(), formOf(), parseNum(), SettingsForm

### Community 77 - "Модели данных API (2)"
Cohesion: 0.29
Nodes (5): CloseSubmit, Deal, DealCreate, FieldValue, Party

### Community 78 - "Экран запроса клиента (4)"
Cohesion: 0.40
Nodes (5): toServerTime(), buildDealCreate(), Created, RequestEvent, ReviewFirst

### Community 80 - "Дерево услуг service: категория → сервис"
Cohesion: 0.33
Nodes (6): glossary / glossary_text (термины и переводы), Пиктограммы icon (SVG currentColor), Дерево услуг service: категория → сервис → вариант, ТОП-10 сервисов (парковка, такси, курьер … репетитор), ui_config (конфигурация интерфейса, контрольная сумма), v_service_search

### Community 81 - "Процесс сделки"
Cohesion: 0.33
Nodes (6): Прогноз цены (forecast_window_min, forecast_grid_m, ОВ-10), Отмена, Процесс сделки, Условия подбора поставщика, Цена, Этапы

### Community 84 - "Монетизация"
Cohesion: 0.40
Nodes (5): Жизненный цикл подписки, Монетизация, Отчёты по выручке, Пакеты, Реферальное вознаграждение

### Community 85 - "Отклики, карта и маршрут"
Cohesion: 0.40
Nodes (5): Карта в приложении, Кейс A/B/C/D, Отклики, карта и маршрут, Правила, Ссылки на маршрут

### Community 86 - "Отмена: CANCEL_FREE / CANCEL_CLIENT / CA"
Cohesion: 0.40
Nodes (5): Согласование п. 4.4: selected / declined / withdrawn, Отмена: CANCEL_FREE / CANCEL_CLIENT / CANCEL_PROVIDER, Обязательные баллы (п. 5.6, В-11), Уведомление OFFER_DECLINED невыбранным поставщикам, Сделка глазами поставщика: роли candidate / offerer / provider

### Community 87 - "README и OpenAPI (2)"
Cohesion: 0.40
Nodes (5): API, OSM Карта — экран сделки маркетплейса услуг, Запуск, Карта, Состав

### Community 89 - "deal_offer"
Cohesion: 0.50
Nodes (4): deal_attribute, deal_offer, ix_deal_offer_provider, uq_deal_offer_selected

### Community 91 - "provider_can_offer(deal_id, agent_id) — "
Cohesion: 0.50
Nodes (4): DealApi module (client), GraphHopper + kazakhstan-latest.osm.pbf (gh-cache), ProviderApi module, Routing module (GraphHopper 10.2, car profile, on-the-way check)

### Community 95 - "Комплект документации project_osm_map v0"
Cohesion: 0.67
Nodes (3): Комплект документации project_osm_map v0.7, ТЗ v0.6 (docs/TZ_marketplace.md), TZ_project_WIN_SERVICE.txt (исходный алгоритм)

## Knowledge Gaps
- **94 isolated node(s):** `Sent`, `LoggedOut`, `Ready`, `Starting`, `Loading` (+89 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 419 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **33 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Карта ui/map/ MpMap (osmdroid, зона, маркеры, подпись © OSM)` connect `План Э3 — каркас приложения` to `Публикация: раскладки`, `README и OpenAPI (2)`?**
  _High betweenness centrality (0.143) - this node is a cross-community bridge._
- **What connects `Sent`, `LoggedOut`, `Ready` to the rest of the system?**
  _94 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Публикация: раскладки` be split into smaller, more focused modules?**
  _Cohesion score 0.053377144943410004 - nodes in this community are weakly interconnected._
- **Why does `OSM Карта — экран сделки маркетплейса услуг` connect `README и OpenAPI (2)` to `CLAUDE.md`?**
  _High betweenness centrality (0.134) - this node is a cross-community bridge._
- **Should `Экран сделки A/B/C` be split into smaller, more focused modules?**
  _Cohesion score 0.0861244019138756 - nodes in this community are weakly interconnected._
- **Why does `Карта` connect `README и OpenAPI (2)` to `План Э3 — каркас приложения`?**
  _High betweenness centrality (0.134) - this node is a cross-community bridge._
- **Should `Тема и палитра` be split into smaller, more focused modules?**
  _Cohesion score 0.06095791001451379 - nodes in this community are weakly interconnected._