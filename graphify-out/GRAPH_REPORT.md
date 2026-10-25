# Graph Report - github_repo  (2026-10-25)

## Corpus Check
- 4 files · ~40,833 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 400 nodes · 562 edges · 21 communities (12 shown, 9 thin omitted)
- Extraction: 90% EXTRACTED · 10% INFERRED · 0% AMBIGUOUS · INFERRED: 54 edges (avg confidence: 0.88)
- Token cost: 86,718 input · 0 output

## Community Hubs (Navigation)
- CLAUDE.md · adb reverse tcp:8080 + http: · Сессия API задаёт
- Автоотклик (is_auto) на все  · Отмена: CANCEL_FREE / CANCEL 
- Activity · build · Bundle
- Антифрод и agent_sanction (э · Смещение точки до согласован 
- TZ_marketplace.md · Антифрод · Жизненный цикл подписки
- com.sun.net.httpserver.HttpE · drivermanager · executors
- 6) Антифрод: BYPASS_GEO, NEA · Сроки: EXPIRED, «Услуга оказ 
- Сценарии приёмки S-1…S-16 · Роли агентов: CLIENT, PROVID · К
- Exception · httpurlconnection · jsonarray
- card_token вместо номера кар · Триггер check_dict_type() (О 
- Режимы сервиса: geo_mode, pl · Правило зоны по place_mode ( 
- Справочник UI_TEMPLATE (A, B · Таблица ui_config (config_js 
- Алгоритм формирования сделки · Журнал решений В-1…В-13, Н-1 
- Справочник NOTIFY_TYPE (NEW_ · Таблица notification · GET /n
- Отложено: прогноз по спросу, · Дорожная карта: прототип v0.
- Рабочий контур (балансировщи

## God Nodes (most connected - your core abstractions)
1. `ApiClient` - 22 edges
2. `MainActivity` - 21 edges
3. `ТЗ: платформа-маркетплейс услуг` - 16 edges
4. `План этапа Э1 — БД v0.7` - 15 edges
5. `ApiServer` - 13 edges
6. `Шаг 3: функции правил сделки` - 12 edges
7. `provider_can_offer(deal_id, agent_id) → (ok, reason)` - 11 edges
8. `Проверка связности плана Э1` - 9 edges
9. `2) Подбор поставщиков (MATCHING)` - 7 edges
10. `Триггер trg_deal_status` - 7 edges

## Surprising Connections (you probably didn't know these)
- `Тестовые пользователи user_testing_1…5 (агенты 7–11)` --semantically_similar_to--> `Шаг 12а: тестовые пользователи (агенты 7–11, Тестер_1…5)`  [INFERRED] [semantically similar]
  CLAUDE.md → project_osm_map/docs/v0.7/plans/E1_db.md
- `Итог Э1: PostgreSQL 17.6, 30 таблиц, 14 представлений, 35 функций, 17 триггеров` --semantically_similar_to--> `Этап Э1 — БД v0.7 (выполнен)`  [INFERRED] [semantically similar]
  project_osm_map/docs/v0.7/plans/E1_db.md → CLAUDE.md
- `Каталог планов docs/v0.7/plans` --references--> `План этапа Э1 — БД v0.7`  [INFERRED]
  CLAUDE.md → project_osm_map/docs/v0.7/plans/E1_db.md
- `Функции схемы с SET search_path = mp_service` --conceptually_related_to--> `Шаг 3: функции правил сделки`  [INFERRED]
  CLAUDE.md → project_osm_map/docs/v0.7/plans/E1_db.md
- `Сессия API задаёт mp.agent_id и mp.geo_secret` --shares_data_with--> `Шаг 3: функции правил сделки`  [INFERRED]
  CLAUDE.md → project_osm_map/docs/v0.7/plans/E1_db.md

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Конфигурация сервиса данными (дерево, настройки, поля, глоссарий)** — project_osm_map_docs_tz_marketplace_service_tree, project_osm_map_docs_tz_marketplace_service_setting, project_osm_map_docs_tz_marketplace_service_attribute, project_osm_map_docs_tz_marketplace_glossary, project_osm_map_docs_tz_marketplace_get_setting [EXTRACTED 1.00]
- **Правила сделки, проверяемые БД (переходы, подбор, отзывы, один selected)** — project_osm_map_docs_v0_7_02_db_architecture_trg_deal_status, project_osm_map_docs_v0_7_02_db_architecture_trg_deal_offer_check, project_osm_map_docs_v0_7_02_db_architecture_provider_can_offer, project_osm_map_docs_v0_7_02_db_architecture_trg_deal_review, project_osm_map_docs_v0_7_02_db_architecture_uq_deal_offer_selected, project_osm_map_docs_v0_7_02_db_architecture_dict_deal_transition [EXTRACTED 1.00]
- **Этапы алгоритма сделки: инициация → подбор → отклик → согласование → закрытие** — project_osm_map_docs_v0_7_01_deal_algorithm_stage_initiation, project_osm_map_docs_v0_7_01_deal_algorithm_stage_matching, project_osm_map_docs_v0_7_01_deal_algorithm_stage_offer, project_osm_map_docs_v0_7_01_deal_algorithm_stage_agreement, project_osm_map_docs_v0_7_01_deal_algorithm_stage_closing [EXTRACTED 1.00]
- **Поток экрана сделки: приложение → API → представления mp_service** — project_osm_map_readme_android_app, project_osm_map_readme_local_api_server, project_osm_map_readme_api_offers, project_osm_map_docs_tz_marketplace_v_deal_offer_list, project_osm_map_readme_api_agree, project_osm_map_readme_api_routes, project_osm_map_docs_tz_marketplace_v_deal_offer_route [EXTRACTED 1.00]
- **Правила сделки в БД: функции, триггеры, переходы, плановое задание** — project_osm_map_docs_v0_7_plans_e1_db_provider_can_offer, project_osm_map_docs_v0_7_plans_e1_db_deal_agree, project_osm_map_docs_v0_7_plans_e1_db_deal_cancel, project_osm_map_docs_v0_7_plans_e1_db_run_deal_jobs, project_osm_map_docs_v0_7_plans_e1_db_trg_deal_status, project_osm_map_docs_v0_7_plans_e1_db_deal_transition [EXTRACTED 1.00]
- **Этапы реализации Э0–Э8** — project_osm_map_docs_v0_7_03_implementation_spec_stage_e0, project_osm_map_docs_v0_7_03_implementation_spec_stage_e1, project_osm_map_docs_v0_7_03_implementation_spec_stage_e2, project_osm_map_docs_v0_7_03_implementation_spec_stage_e3, project_osm_map_docs_v0_7_03_implementation_spec_stage_e4, project_osm_map_docs_v0_7_03_implementation_spec_stage_e5, project_osm_map_docs_v0_7_03_implementation_spec_stage_e6, project_osm_map_docs_v0_7_03_implementation_spec_stage_e7, project_osm_map_docs_v0_7_03_implementation_spec_stage_e8 [EXTRACTED 1.00]
- **Вход тестовых пользователей по хешу пароля** — claude_test_users, project_osm_map_docs_v0_7_plans_e1_db_step_12a_test_users, project_osm_map_docs_v0_7_plans_e1_db_agent_login, project_osm_map_docs_v0_7_plans_e1_db_agent_login_check [EXTRACTED 1.00]
- **Меры против обхода платформы до согласования** — project_osm_map_docs_tz_marketplace_approx_offset, project_osm_map_docs_tz_marketplace_pseudonym, project_osm_map_docs_tz_marketplace_show_stage_near, project_osm_map_docs_tz_marketplace_antifraud [INFERRED 0.85]
- **Процесс «план → проверка связности → реализация» по этапам Э0–Э8** — claude_rule_plan_first, claude_plans_dir, project_osm_map_docs_v0_7_plans_e1_db_plan, project_osm_map_docs_v0_7_plans_e1_db_coherence_check, claude_stage_e2 [INFERRED 0.85]

## Communities (21 total, 9 thin omitted)

### Community 0 - "CLAUDE.md · adb reverse tcp:8080 + http: · Сессия API задаёт"
Cohesion: 0.06
Nodes (43): adb reverse tcp:8080 + http://127.0.0.1:8080, Сессия API задаёт mp.agent_id и mp.geo_secret, graphify-out — граф ведения проектов, server/ — локальный API на Java (HttpServer + JDBC), Схема mp_service (db/mp_service.sql), Офлайн-карта: растровые MBTiles (osmdroid), kazakhstan-latest.osm.pbf (Geofabrik) для маршрутов такси, Каталог планов docs/v0.7/plans (+35 more)

### Community 1 - "Автоотклик (is_auto) на все  · Отмена: CANCEL_FREE / CANCEL "
Cohesion: 0.05
Nodes (38): Публикация (окно) поставщика (Н-2), Сценарий: парковка — публикация места, Сценарий: частная парковка (capacity), 4) Согласование (OFFERS → AGREED → CONTACT), 5) Закрытие сделки, 1) Инициация — клиент создаёт запрос (INIT → MATCHING), 2) Подбор поставщиков (MATCHING), 3) Выбор — поставщик откликается (MATCHING → OFFERS) (+30 more)

### Community 3 - "Антифрод и agent_sanction (э · Смещение точки до согласован "
Cohesion: 0.05
Nodes (43): Антифрод и agent_sanction (эскалация санкций), Кейс A/B/C/D (сделка 6, порядок B, D, C), Сделка deal, Отмена сделки (CANCEL_FREE/CLIENT/PROVIDER, EXPIRED), Отклик deal_offer, Статусы сделки INIT→MATCHING→OFFERS→AGREED→CONTACT→CLOSED, Отчёты по выручке (v_agent_finance_daily, v_company_finance_daily), geo_distance_m() (гаверсинус, ОВ-15/ОВ-19) (+35 more)

### Community 4 - "TZ_marketplace.md · Антифрод · Жизненный цикл подписки"
Cohesion: 0.06
Nodes (33): Антифрод, Жизненный цикл подписки, Изменения относительно исходной модели, Как добавить сервис, Карта в приложении, Кейс A/B/C/D, Модель данных, Монетизация (+25 more)

### Community 6 - "6) Антифрод: BYPASS_GEO, NEA · Сроки: EXPIRED, «Услуга оказ "
Cohesion: 0.09
Nodes (22): 6) Антифрод: BYPASS_GEO, NEAR_CANCEL, FAKE_REFERRALS, блокировки, Статус CONTACT (сближение ≤ near_radius_m), Статус EXPIRED, Статус NOT_CLOSED, Плановые задания (раз в минуту): истечение, CLOSING_ASK, NOT_CLOSED, сближение, публикации, Таблица agent_sanction (warning / rating_down / block), Структура модулей приложения ru.mpavel.osmmap (core/*, feature/*, ui/template, ui/map), API v0.7 (http://127.0.0.1:8080/api) (+14 more)

### Community 7 - "Сценарии приёмки S-1…S-16 · Роли агентов: CLIENT, PROVID · К"
Cohesion: 0.10
Nodes (22): Сценарии приёмки S-1…S-16, Роли агентов: CLIENT, PROVIDER, SUBAGENT, COMPANY, ADMIN, Сценарий: онлайн-услуга (без CONTACT, ключ KEY), Режим тестирования администратора, Справочник MAP_CURSOR (капля / капля с шестерёнкой), Миграция v0.6 → v0.7, Таблица agent (is_testing), Таблица agent_position [new] (+14 more)

### Community 8 - "Exception · httpurlconnection · jsonarray"
Cohesion: 0.17
Nodes (6): ApiClient, ApiException, BaseLayer, Deal, Offer, RouteLink

### Community 9 - "card_token вместо номера кар · Триггер check_dict_type() (О "
Cohesion: 0.11
Nodes (19): Триггер check_dict_type() (ОВ-22), Основной документ ТЗ в Claude Docs, Таблица dictionary (общий справочник, dict_type), Справочник MAP_BASE_LAYER / OSM_OFFLINE_ALMATY (ОВ-20), Ссылки на маршрут MAP_ROUTE_LINK (Яндекс, 2ГИС, Google), Схема mp_service (27 таблиц, 10 представлений, 6 функций), Открытые вопросы ОВ-1…ОВ-24, Подпись «© участники OpenStreetMap» (+11 more)

### Community 10 - "Режимы сервиса: geo_mode, pl · Правило зоны по place_mode ( "
Cohesion: 0.22
Nodes (8): Режимы сервиса: geo_mode, place_mode, price_unit, max_parallel_deals, slot_required, close_method, get_setting(service_id, code, rating), Таблица glossary_text (тексты на языке агента), Таблица service (дерево категория → сервис → вариант), Таблица service_setting (настройки по дереву и рейтингу), Представление v_service_search, GET /services, /services/{id}/form, /services/{id}/settings, Модель конфигурации «новый сервис — только данными» (пример «Эвакуатор»)

### Community 11 - "Справочник UI_TEMPLATE (A, B · Таблица ui_config (config_js "
Cohesion: 0.25
Nodes (9): Справочник UI_TEMPLATE (A, B, C) и UI_SCHEME (LIGHT/DARK), Таблица ui_config (config_json, checksum md5), Представление v_ui_options, GET/PUT /config, GET /ui/options, Загрузка конфигурации интерфейса по контрольной сумме (304), Взаимодействие: загрузка конфигурации интерфейса, Шаблон A «Карта в центре» (Manrope), Шаблон B «Карточки» (Golos Text) (+1 more)

### Community 12 - "Алгоритм формирования сделки · Журнал решений В-1…В-13, Н-1 "
Cohesion: 0.54
Nodes (8): Алгоритм формирования сделки v0.7, Журнал решений В-1…В-13, Н-1…Н-12, П-1…П-6, Архитектура БД mp_service v0.7 (29 таблиц), ТЗ на реализацию прототипа v0.7 (для Claude), Общая архитектура продукта v0.7, Комплект документации project_osm_map v0.7, ТЗ v0.6 (docs/TZ_marketplace.md), TZ_project_WIN_SERVICE.txt (исходный алгоритм)

### Community 14 - "Справочник NOTIFY_TYPE (NEW_ · Таблица notification · GET /n"
Cohesion: 0.67
Nodes (3): Справочник NOTIFY_TYPE (NEW_REQUEST, NEW_OFFER, AGREED, NEAR, CLOSE_REQUEST, CLOSE_CODE, …), Таблица notification, GET /notifications?since (опрос 2–5 с), GET /me

## Knowledge Gaps
- **53 isolated node(s):** `geo_distance_m() (гаверсинус, ОВ-15/ОВ-19)`, `Оплата сделки DIRECT / PLATFORM (ОВ-8)`, `ТОП-10 сервисов (парковка, такси, курьер … репетитор)`, `ui_config (конфигурация интерфейса, контрольная сумма)`, `v_subscription_charge` (+48 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 138 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **9 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Триггер trg_deal_status` connect `Автоотклик (is_auto) на все  · Отмена: CANCEL_FREE / CANCEL ` to `Антифрод и agent_sanction (э · Смещение точки до согласован `?**
  _High betweenness centrality (0.480) - this node is a cross-community bridge._
- **Are the 2 inferred relationships involving `План этапа Э1 — БД v0.7` (e.g. with `Каталог планов docs/v0.7/plans` and `Договорённость: детальный план + проверка логической связанности перед реализацией`) actually correct?**
  _`План этапа Э1 — БД v0.7` has 2 INFERRED edges - model-reasoned connections that need verification._
- **What connects `geo_distance_m() (гаверсинус, ОВ-15/ОВ-19)`, `Оплата сделки DIRECT / PLATFORM (ОВ-8)`, `ТОП-10 сервисов (парковка, такси, курьер … репетитор)` to the rest of the system?**
  _53 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `CLAUDE.md · adb reverse tcp:8080 + http: · Сессия API задаёт` be split into smaller, more focused modules?**
  _Cohesion score 0.055523085914669784 - nodes in this community are weakly interconnected._
- **Should `Автоотклик (is_auto) на все  · Отмена: CANCEL_FREE / CANCEL ` be split into smaller, more focused modules?**
  _Cohesion score 0.052244897959183675 - nodes in this community are weakly interconnected._
- **Should `Activity · build · Bundle` be split into smaller, more focused modules?**
  _Cohesion score 0.06666666666666667 - nodes in this community are weakly interconnected._
- **Should `Антифрод и agent_sanction (э · Смещение точки до согласован ` be split into smaller, more focused modules?**
  _Cohesion score 0.05454545454545454 - nodes in this community are weakly interconnected._