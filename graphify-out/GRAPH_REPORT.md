# Graph Report - github_repo  (2026-10-25)

## Corpus Check
- 18 files · ~63,165 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 614 nodes · 1280 edges · 47 communities (36 shown, 11 thin omitted)
- Extraction: 86% EXTRACTED · 14% INFERRED · 0% AMBIGUOUS · INFERRED: 179 edges (avg confidence: 0.86)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- Сервер: ConfigApi и обработка запросов
- Зависимости сервера Java
- ТЗ v0.6: принципы и антифрод
- Android: экран карты
- ApiServer: маршрутизатор
- ТЗ v0.6: разделы
- HTTP-тест api_smoke.py
- Android: ApiClient
- CLAUDE.md: этапы и решения
- Роли, тестовый режим, курсоры
- Отклики и правила provider_can_offer
- Функции правил сделки
- Код закрытия CloseCode
- План Э1 и тесты БД
- Сессии, Db и параметры сервера
- Переходы статусов сделки
- Вход и загрузка тестовых данных
- Структура проекта и схема БД
- Дерево услуг и настройки
- CLAUDE.md: договорённости
- Прогноз цены и баллы
- Архитектура API v0.7
- Шаблоны и конфигурация UI
- Комплект документации v0.7
- Подбор поставщиков и такси
- Смещение точек и безопасность
- Android-приложение v0.7 (Э3–Э6)
- API поставщика
- Этапы алгоритма сделки
- Задания и правила БД в API
- Антифрод и сроки
- API клиента
- Приёмка и тестирование
- Модель ошибок API
- README запуска
- Малая группа 35
- Малая группа 36
- Малая группа 38
- Малая группа 39
- Малая группа 40
- Малая группа 43
- Малая группа 44

## God Nodes (most connected - your core abstractions)
1. `Request` - 51 edges
2. `Response` - 49 edges
3. `DealApi` - 33 edges
4. `ApiClient` - 22 edges
5. `Routing` - 22 edges
6. `MainActivity` - 21 edges
7. `Db` - 19 edges
8. `ConfigApi` - 18 edges
9. `Jobs` - 17 edges
10. `ТЗ: платформа-маркетплейс услуг` - 16 edges

## Surprising Connections (you probably didn't know these)
- `Итог Э1: PostgreSQL 17.6, 30 таблиц, 14 представлений, 35 функций, 17 триггеров` --semantically_similar_to--> `Stage E1: DB mp_service v0.7 (rules in DB, tests 29/29)`  [INFERRED] [semantically similar]
  project_osm_map/docs/v0.7/plans/E1_db.md → CLAUDE.md
- `Сессия API задаёт mp.agent_id и mp.geo_secret` --shares_data_with--> `Шаг 3: функции правил сделки`  [INFERRED]
  CLAUDE.md → project_osm_map/docs/v0.7/plans/E1_db.md
- `Offline map: raster MBTiles only (CDN download stub)` --semantically_similar_to--> `Map base layer (MAP_BASE_LAYER, OSM_OFFLINE_ALMATY.mbtiles)`  [INFERRED] [semantically similar]
  CLAUDE.md → project_osm_map/README.md
- `Каталог планов docs/v0.7/plans` --references--> `План этапа Э1 — БД v0.7`  [INFERRED]
  CLAUDE.md → project_osm_map/docs/v0.7/plans/E1_db.md
- `Проверка связности плана Э1` --implements--> `Договорённость: детальный план + проверка логической связанности перед реализацией`  [INFERRED]
  project_osm_map/docs/v0.7/plans/E1_db.md → CLAUDE.md

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Конфигурация сервиса данными (дерево, настройки, поля, глоссарий)** — project_osm_map_docs_tz_marketplace_service_tree, project_osm_map_docs_tz_marketplace_service_setting, project_osm_map_docs_tz_marketplace_service_attribute, project_osm_map_docs_tz_marketplace_glossary, project_osm_map_docs_tz_marketplace_get_setting [EXTRACTED 1.00]
- **Правила сделки, проверяемые БД (переходы, подбор, отзывы, один selected)** — project_osm_map_docs_v0_7_02_db_architecture_trg_deal_status, project_osm_map_docs_v0_7_02_db_architecture_trg_deal_offer_check, project_osm_map_docs_v0_7_02_db_architecture_provider_can_offer, project_osm_map_docs_v0_7_02_db_architecture_trg_deal_review, project_osm_map_docs_v0_7_02_db_architecture_uq_deal_offer_selected, project_osm_map_docs_v0_7_02_db_architecture_dict_deal_transition [EXTRACTED 1.00]
- **Этапы алгоритма сделки: инициация → подбор → отклик → согласование → закрытие** — project_osm_map_docs_v0_7_01_deal_algorithm_stage_initiation, project_osm_map_docs_v0_7_01_deal_algorithm_stage_matching, project_osm_map_docs_v0_7_01_deal_algorithm_stage_offer, project_osm_map_docs_v0_7_01_deal_algorithm_stage_agreement, project_osm_map_docs_v0_7_01_deal_algorithm_stage_closing [EXTRACTED 1.00]
- **Правила сделки в БД: функции, триггеры, переходы, плановое задание** — project_osm_map_docs_v0_7_plans_e1_db_provider_can_offer, project_osm_map_docs_v0_7_plans_e1_db_deal_agree, project_osm_map_docs_v0_7_plans_e1_db_deal_cancel, project_osm_map_docs_v0_7_plans_e1_db_run_deal_jobs, project_osm_map_docs_v0_7_plans_e1_db_trg_deal_status, project_osm_map_docs_v0_7_plans_e1_db_deal_transition [EXTRACTED 1.00]
- **Этапы реализации Э0–Э8** — project_osm_map_docs_v0_7_03_implementation_spec_stage_e0, project_osm_map_docs_v0_7_03_implementation_spec_stage_e1, project_osm_map_docs_v0_7_03_implementation_spec_stage_e2, project_osm_map_docs_v0_7_03_implementation_spec_stage_e3, project_osm_map_docs_v0_7_03_implementation_spec_stage_e4, project_osm_map_docs_v0_7_03_implementation_spec_stage_e5, project_osm_map_docs_v0_7_03_implementation_spec_stage_e6, project_osm_map_docs_v0_7_03_implementation_spec_stage_e7, project_osm_map_docs_v0_7_03_implementation_spec_stage_e8 [EXTRACTED 1.00]
- **Вход тестовых пользователей по хешу пароля** — claude_test_users, project_osm_map_docs_v0_7_plans_e1_db_step_12a_test_users, project_osm_map_docs_v0_7_plans_e1_db_agent_login, project_osm_map_docs_v0_7_plans_e1_db_agent_login_check [EXTRACTED 1.00]
- **Меры против обхода платформы до согласования** — project_osm_map_docs_tz_marketplace_approx_offset, project_osm_map_docs_tz_marketplace_pseudonym, project_osm_map_docs_tz_marketplace_show_stage_near, project_osm_map_docs_tz_marketplace_antifraud [INFERRED 0.85]
- **E2 server module split (ApiServer, Db, Http, Auth, ConfigApi, DealApi, ProviderApi, Routing, CloseCode, Jobs)** — project_osm_map_docs_v0_7_plans_e2_api_apiserver, project_osm_map_docs_v0_7_plans_e2_api_db, project_osm_map_docs_v0_7_plans_e2_api_http, project_osm_map_docs_v0_7_plans_e2_api_auth, project_osm_map_docs_v0_7_plans_e2_api_configapi, project_osm_map_docs_v0_7_plans_e2_api_dealapi, project_osm_map_docs_v0_7_plans_e2_api_providerapi, project_osm_map_docs_v0_7_plans_e2_api_routing, project_osm_map_docs_v0_7_plans_e2_api_closecode, project_osm_map_docs_v0_7_plans_e2_api_jobs [EXTRACTED 1.00]
- **Deal lifecycle via API: request, offers, agree, close, review, cancel** — project_osm_map_server_openapi_deals, project_osm_map_server_openapi_deal_offers, project_osm_map_server_openapi_offer_agree, project_osm_map_server_openapi_close, project_osm_map_server_openapi_review, project_osm_map_server_openapi_cancel [INFERRED 0.85]
- **Taxi on-the-way matching (server-side routing, slots, requests, forecast)** — project_osm_map_docs_v0_7_plans_e2_api_routing, project_osm_map_docs_v0_7_plans_e2_api_graphhopper, project_osm_map_server_openapi_slots, project_osm_map_server_openapi_requests, project_osm_map_server_openapi_forecast [INFERRED 0.85]

## Communities (47 total, 11 thin omitted)

### Community 0 - "Сервер: ConfigApi и обработка запросов"
Cohesion: 0.15
Nodes (8): ConfigApi, Db, Work, DealApi, ApiException, Request, Response, ProviderApi

### Community 2 - "ТЗ v0.6: принципы и антифрод"
Cohesion: 0.05
Nodes (47): Антифрод и agent_sanction (эскалация санкций), Кейс A/B/C/D (сделка 6, порядок B, D, C), Триггер check_dict_type() (ОВ-22), Основной документ ТЗ в Claude Docs, Сделка deal, Отмена сделки (CANCEL_FREE/CLIENT/PROVIDER, EXPIRED), Отклик deal_offer, Статусы сделки INIT→MATCHING→OFFERS→AGREED→CONTACT→CLOSED (+39 more)

### Community 4 - "ApiServer: маршрутизатор"
Cohesion: 0.10
Nodes (9): ApiServer, Handler, Route, Auth, Session, Jobs, Projection, Route (+1 more)

### Community 5 - "ТЗ v0.6: разделы"
Cohesion: 0.06
Nodes (32): Антифрод, Жизненный цикл подписки, Изменения относительно исходной модели, Как добавить сервис, Карта в приложении, Кейс A/B/C/D, Модель данных, Монетизация (+24 more)

### Community 6 - "HTTP-тест api_smoke.py"
Cohesion: 0.18
Nodes (17): call(), check(), main(), agree(), close(), config(), contact(), create() (+9 more)

### Community 7 - "Android: ApiClient"
Cohesion: 0.17
Nodes (6): ApiClient, ApiException, BaseLayer, Deal, Offer, RouteLink

### Community 8 - "CLAUDE.md: этапы и решения"
Cohesion: 0.20
Nodes (10): Сессия API задаёт mp.agent_id и mp.geo_secret, kazakhstan-latest.osm.pbf (Geofabrik) для маршрутов такси, Stage E2: API v0.7 (39 methods, smoke 16/16), Stage E3: next step (app rewrite E3-E6), Такси: маршрут 6,4 км, цена max(700, 150×6,4) = 960 тг, E2 plan: API v0.7, api_smoke.py HTTP smoke test (16/16), 02_db_architecture.md (sections 6.3, 8) (+2 more)

### Community 9 - "Роли, тестовый режим, курсоры"
Cohesion: 0.16
Nodes (14): Роли агентов: CLIENT, PROVIDER, SUBAGENT, COMPANY, ADMIN, Сценарий: онлайн-услуга (без CONTACT, ключ KEY), Режим тестирования администратора, Справочник MAP_CURSOR (капля / капля с шестерёнкой), Таблица agent (is_testing), Триггер trg_agent_testing (is_testing только при ADMIN), Представление v_map_cursor, Закрытие: /close/request, /close/code, POST /close, /close/test-scan, /close/test-provide (+6 more)

### Community 10 - "Отклики и правила provider_can_offer"
Cohesion: 0.21
Nodes (12): agent_rating(agent, role, service, on_date), provider_can_offer(deal_id, agent_id) → (ok, reason), Таблица deal, Таблица deal_offer (offer_status), Таблица provider_slot [new], Триггер trg_deal_offer_check, Индекс uq_deal_offer_selected (один selected на сделку), Представление v_agent_available_service [chg] (+4 more)

### Community 11 - "Функции правил сделки"
Cohesion: 0.18
Nodes (9): Эскизы интерфейса (шаблоны A/B/C) — холст, deal_agree(), Справочник DEAL_TRANSITION (18 переходов), Шаги 6–8: справочные данные, глоссарий, дерево и настройки, Шаг 3: функции правил сделки, run_deal_jobs() — плановое задание раз в минуту, trg_deal_status / trg_deal_status_after, UI_TEMPLATE A/B/C, UI_SCHEME ×6, MAP_CURSOR, ui_config version 7 (+1 more)

### Community 13 - "План Э1 и тесты БД"
Cohesion: 0.29
Nodes (9): Каталог планов docs/v0.7/plans, Кейс A/B/C/D: порядок откликов B, D, C; E отсечён правилом В-1, План этапа Э1 — БД v0.7, provider_can_offer(), Итог Э1: PostgreSQL 17.6, 30 таблиц, 14 представлений, 35 функций, 17 триггеров, db/tests/01_rules.sql — SQL-тесты (29/29), Шаг 1: структура таблиц (agent_position, provider_slot, deal.close_secret, deal_offer.offer_status), Шаг 12: тестовые сделки 1–7 (парковка A/B/C/D, такси) (+1 more)

### Community 14 - "Сессии, Db и параметры сервера"
Cohesion: 0.18
Nodes (8): Test users user_testing_1..5 (agents 7-11), Auth module (in-memory bearer tokens 12h, dev login), Db module (connection, session params, transaction), Server env vars (MP_DB_URL, MP_DB_PASSWORD, MP_GEO_SECRET, MP_DEV_LOGIN, MP_OSM_FILE...), Bearer security scheme, /map/slots (offset provider points), /me/position (gps/manual, manual only testing), /session (login, logout)

### Community 15 - "Переходы статусов сделки"
Cohesion: 0.20
Nodes (9): Проверка на тестовых данных 25.10.2026 (расхождения v0.6), check_dict_type() + 9 триггеров, Справочник DEAL_TRANSITION [new], Таблица deal_status_log (пишет только триггер), Общий справочник dictionary (value_json), Триггер trg_deal_status, Э1. БД v0.7, Модуль: PostgreSQL mp_service (+1 more)

### Community 16 - "Вход и загрузка тестовых данных"
Cohesion: 0.24
Nodes (7): Таблица agent_login (хеш bcrypt через pgcrypto), agent_login_check(login, password) — crypt() pgcrypto, Режим загрузки mp.load_mode (is_load_mode, RESET), trg_agent_testing / trg_agent_service_assign / trg_agent_position, Этап Э8 — правка документации, Шаг 12а: тестовые пользователи (агенты 7–11, Тестер_1…5), Шаг 4: триггеры

### Community 17 - "Структура проекта и схема БД"
Cohesion: 0.22
Nodes (7): server/ — локальный API на Java (HttpServer + JDBC), PostgreSQL schema mp_service, Offline map: raster MBTiles only (CDN download stub), project_osm_map — Android-приложение с картой OSM, ConfigApi module, Map base layer (MAP_BASE_LAYER, OSM_OFFLINE_ALMATY.mbtiles), /config (GET checksum/304, PUT template/scheme/lang)

### Community 18 - "Дерево услуг и настройки"
Cohesion: 0.22
Nodes (8): Режимы сервиса: geo_mode, place_mode, price_unit, max_parallel_deals, slot_required, close_method, get_setting(service_id, code, rating), Таблица glossary_text (тексты на языке агента), Таблица service (дерево категория → сервис → вариант), Таблица service_setting (настройки по дереву и рейтингу), Представление v_service_search, GET /services, /services/{id}/form, /services/{id}/settings, Модель конфигурации «новый сервис — только данными» (пример «Эвакуатор»)

### Community 19 - "CLAUDE.md: договорённости"
Cohesion: 0.22
Nodes (5): graphify-out — граф ведения проектов, Договорённости, О проекте, Структура, Текущее состояние и решения

### Community 20 - "Прогноз цены и баллы"
Cohesion: 0.25
Nodes (7): 1) Инициация — клиент создаёт запрос (INIT → MATCHING), agent_pending_reviews(agent_id), price_forecast(service_id, lat, lon, period_start), Таблица deal_review, Триггер trg_deal_review, POST /deals/{id}/cancel, POST /deals/{id}/review, GET /me/pending-reviews, Клиент: /map/slots, POST /forecast, POST /deals, /deals/{id}/offers, /agree, /routes

### Community 21 - "Архитектура API v0.7"
Cohesion: 0.28
Nodes (8): Статус CONTACT (сближение ≤ near_radius_m), Плановые задания (раз в минуту): истечение, CLOSING_ASK, NOT_CLOSED, сближение, публикации, API v0.7 (http://127.0.0.1:8080/api), GraphHopper (маршруты по дорогам OSM), Библиотеки: Compose, OkHttp, kotlinx.serialization, osmdroid, Coil-svg, ZXing, Code Scanner, GraphHopper, Э2. API v0.7, Модуль: API-сервер (Java, HttpServer + JDBC), Контекст системы (клиенты Android, API, PostgreSQL, GraphHopper, OSM, внешние карты)

### Community 22 - "Шаблоны и конфигурация UI"
Cohesion: 0.25
Nodes (9): Справочник UI_TEMPLATE (A, B, C) и UI_SCHEME (LIGHT/DARK), Таблица ui_config (config_json, checksum md5), Представление v_ui_options, GET/PUT /config, GET /ui/options, Загрузка конфигурации интерфейса по контрольной сумме (304), Взаимодействие: загрузка конфигурации интерфейса, Шаблон A «Карта в центре» (Manrope), Шаблон B «Карточки» (Golos Text) (+1 more)

### Community 23 - "Комплект документации v0.7"
Cohesion: 0.54
Nodes (8): Алгоритм формирования сделки v0.7, Журнал решений В-1…В-13, Н-1…Н-12, П-1…П-6, Архитектура БД mp_service v0.7 (29 таблиц), ТЗ на реализацию прототипа v0.7 (для Claude), Общая архитектура продукта v0.7, Комплект документации project_osm_map v0.7, ТЗ v0.6 (docs/TZ_marketplace.md), TZ_project_WIN_SERVICE.txt (исходный алгоритм)

### Community 24 - "Подбор поставщиков и такси"
Cohesion: 0.25
Nodes (5): Сценарий: частная парковка (capacity), 2) Подбор поставщиков (MATCHING), Таблица agent_service (цены, тариф, capacity, назначение), Триггер trg_agent_service_assign (субагент), Схема подбора такси по пути

### Community 25 - "Смещение точек и безопасность"
Cohesion: 0.25
Nodes (5): Формат JSON шаблона интерфейса и палитры, Представление v_deal_offer_list (смещение, distance_band), GET /dev/agents, POST /session (dev-авторизация, MP_DEV_LOGIN), Э0. Подготовка (второй AVD, OSM-данные, MP_GEO_SECRET), Нефункциональные требования (безопасность, ПДн, карта, доступность, масштабирование)

### Community 26 - "Android-приложение v0.7 (Э3–Э6)"
Cohesion: 0.29
Nodes (7): Структура модулей приложения ru.mpavel.osmmap (core/*, feature/*, ui/template, ui/map), Открытые вопросы Р-1…Р-3, Экраны приложения (вход, главная, запрос, отклики, сделка, код, баллы, отмена, публикация…), Э3. Каркас Android-приложения (Compose), Э4. Экраны клиента, Э5. Экраны поставщика, Модуль: Android-приложение (Kotlin, Jetpack Compose)

### Community 27 - "API поставщика"
Cohesion: 0.29
Nodes (8): DB views (v_deal_offer_list, v_deal_candidates, v_provider_slot_map, v_ui_options, v_map_cursor), ProviderApi module, Routing module (GraphHopper 10.2, car profile, on-the-way check), /deals/{dealId}/offers (list, manual offer), Schema Offer (row of v_deal_offer_list), /requests (v_deal_candidates + recommended price), Schema Slot (row of provider_slot), /slots (publish place or route)

### Community 28 - "Этапы алгоритма сделки"
Cohesion: 0.29
Nodes (5): Публикация (окно) поставщика (Н-2), Сценарий: парковка — публикация места, 4) Согласование (OFFERS → AGREED → CONTACT), 5) Закрытие сделки, 3) Выбор — поставщик откликается (MATCHING → OFFERS)

### Community 29 - "Задания и правила БД в API"
Cohesion: 0.33
Nodes (6): Stage E1: DB mp_service v0.7 (rules in DB, tests 29/29), DB rule functions (provider_can_offer, deal_agree, deal_cancel, make_auto_offers, price_forecast, run_deal_jobs, agent_login_check), E1_db.md (E1 plan), Jobs module (run_deal_jobs every minute, taxi route requests), /deals/{dealId}/cancel (deal_cancel, CANCEL_FREE), /notifications (since, read)

### Community 30 - "Антифрод и сроки"
Cohesion: 0.33
Nodes (5): 6) Антифрод: BYPASS_GEO, NEAR_CANCEL, FAKE_REFERRALS, блокировки, Статус EXPIRED, Статус NOT_CLOSED, Таблица agent_sanction (warning / rating_down / block), Схема антифрода (эпизоды → предупреждение → понижение → блокировка)

### Community 31 - "API клиента"
Cohesion: 0.33
Nodes (6): DealApi module (client), Schema Deal (status, actions, counterpart_fields by show_stage), /deals (create request INIT->MATCHING, list), /forecast (price forecast, taxi by route length), /deals/{dealId}/offers/{offerId}/agree (deal_agree), /deals/{dealId}/review and /me/pending-reviews

### Community 32 - "Приёмка и тестирование"
Cohesion: 0.50
Nodes (5): Сценарии приёмки S-1…S-16, Определение готовности, Э7. Тестирование на двух эмуляторах, Э8. Документация и фиксация, Сценарии U-1…U-5 (шаблоны, язык, 304, недоступность сервера, курсоры)

### Community 33 - "Модель ошибок API"
Cohesion: 0.40
Nodes (3): ApiServer (routes, lang, DB errors to HTTP), Error model {error, code} with HTTP 400/401/403/404/409/422, Schema Error {error, code}

### Community 34 - "README запуска"
Cohesion: 0.40
Nodes (5): API, OSM Карта — экран сделки маркетплейса услуг, Запуск, Карта, Состав

### Community 35 - "Малая группа 35"
Cohesion: 0.50
Nodes (4): adb reverse tcp:8080 to 127.0.0.1, Stage E0: second emulator and OSM data, Развёртывание прототипа (локально, 2 эмулятора через adb reverse), GraphHopper + kazakhstan-latest.osm.pbf (gh-cache)

### Community 36 - "Малая группа 36"
Cohesion: 0.50
Nodes (3): Миграция v0.6 → v0.7, Таблица agent_position [new], Триггер trg_agent_position (manual только при is_testing)

### Community 38 - "Малая группа 38"
Cohesion: 0.67
Nodes (3): Справочник NOTIFY_TYPE (NEW_REQUEST, NEW_OFFER, AGREED, NEAR, CLOSE_REQUEST, CLOSE_CODE, …), Таблица notification, GET /notifications?since (опрос 2–5 с), GET /me

### Community 39 - "Малая группа 39"
Cohesion: 1.00
Nodes (3): CloseCode module (HMAC-SHA256, current+previous interval), Close endpoints (close/request, close/code, close, test-scan, test-provide), Schema CloseCode (QR/KEY, mpclose payload, ttl)

## Knowledge Gaps
- **57 isolated node(s):** `Развёртывание прототипа (локально, 2 эмулятора через adb reverse)`, `agent_rating(agent, role, service, on_date)`, `Индекс uq_deal_offer_selected (один selected на сделку)`, `Представление v_agent_available_service [chg]`, `GET /dev/agents, POST /session (dev-авторизация, MP_DEV_LOGIN)` (+52 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 167 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **11 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `ТЗ: платформа-маркетплейс услуг` connect `ТЗ v0.6: разделы` to `CLAUDE.md: этапы и решения`?**
  _High betweenness centrality (0.474) - this node is a cross-community bridge._
- **What connects `Развёртывание прототипа (локально, 2 эмулятора через adb reverse)`, `agent_rating(agent, role, service, on_date)`, `Индекс uq_deal_offer_selected (один selected на сделку)` to the rest of the system?**
  _57 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Сервер: ConfigApi и обработка запросов` be split into smaller, more focused modules?**
  _Cohesion score 0.14882882882882884 - nodes in this community are weakly interconnected._
- **Why does `ApiClient` connect `Android: ApiClient` to `Android: экран карты`?**
  _High betweenness centrality (0.426) - this node is a cross-community bridge._
- **Should `Зависимости сервера Java` be split into smaller, more focused modules?**
  _Cohesion score 0.05128205128205128 - nodes in this community are weakly interconnected._
- **Why does `MainActivity` connect `Android: экран карты` to `Android: ApiClient`?**
  _High betweenness centrality (0.412) - this node is a cross-community bridge._
- **Should `ТЗ v0.6: принципы и антифрод` be split into smaller, more focused modules?**
  _Cohesion score 0.05176470588235294 - nodes in this community are weakly interconnected._