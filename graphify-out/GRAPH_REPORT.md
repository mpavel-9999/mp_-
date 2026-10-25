# Graph Report - github_repo  (2026-10-25)

## Corpus Check
- 6 files · ~30,213 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 362 nodes · 498 edges · 18 communities (11 shown, 7 thin omitted)
- Extraction: 91% EXTRACTED · 9% INFERRED · 0% AMBIGUOUS · INFERRED: 45 edges (avg confidence: 0.91)
- Token cost: 169,249 input · 0 output

## Community Hubs (Navigation)
- Экран сделки MainActivity
- Сделки и отклики (ТЗ v0.6)
- Алгоритм v0.7: подбор и сроки
- Алгоритм v0.7: выбор и закрытие
- Разделы ТЗ маркетплейса
- Окружение и запуск (Э0)
- Java API-сервер
- Контекст репозитория и договорённости
- Клиент API (ApiClient)
- Справочники и модель данных
- Режим тестирования и закрытие кодом
- Дорожная карта
- Уведомления

## God Nodes (most connected - your core abstractions)
1. `ApiClient` - 22 edges
2. `MainActivity` - 21 edges
3. `ТЗ: платформа-маркетплейс услуг` - 16 edges
4. `ApiServer` - 13 edges
5. `provider_can_offer(deal_id, agent_id) → (ok, reason)` - 11 edges
6. `Э1. БД v0.7` - 8 edges
7. `Комплект документации project_osm_map v0.7` - 7 edges
8. `Алгоритм формирования сделки v0.7` - 7 edges
9. `2) Подбор поставщиков (MATCHING)` - 7 edges
10. `Триггер trg_deal_status` - 7 edges

## Surprising Connections (you probably didn't know these)
- `Локальный API на Java (HttpServer + JDBC) над mp_service` --semantically_similar_to--> `Модуль: API-сервер (Java, HttpServer + JDBC)`  [INFERRED] [semantically similar]
  CLAUDE.md → project_osm_map/docs/v0.7/04_product_architecture.md
- `Договорённость: сначала план, потом выполнение` --semantically_similar_to--> `Правила работы исполнителя (план → согласование, вопросы, разрешение на скачивание)`  [INFERRED] [semantically similar]
  CLAUDE.md → project_osm_map/docs/v0.7/03_implementation_spec.md
- `Э7. Тестирование на двух эмуляторах` --references--> `Эмуляторы Medium_Phone_API_37.0 (клиент, 5554) и Medium_Phone_B (поставщик, 5556)`  [INFERRED]
  project_osm_map/docs/v0.7/03_implementation_spec.md → CLAUDE.md
- `Жизненный цикл сделки (stateDiagram)` --semantically_similar_to--> `Статусы сделки INIT→MATCHING→OFFERS→AGREED→CONTACT→CLOSED`  [INFERRED] [semantically similar]
  project_osm_map/docs/v0.7/04_product_architecture.md → project_osm_map/docs/TZ_marketplace.md
- `Офлайн-карта MBTiles (скачивание с CDN не сделано)` --conceptually_related_to--> `Отложено: прогноз по спросу, оплата PLATFORM, сканер камерой, MBTiles с CDN`  [INFERRED]
  CLAUDE.md → project_osm_map/docs/v0.7/01_deal_algorithm.md

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Конфигурация сервиса данными (дерево, настройки, поля, глоссарий)** — project_osm_map_docs_tz_marketplace_service_tree, project_osm_map_docs_tz_marketplace_service_setting, project_osm_map_docs_tz_marketplace_service_attribute, project_osm_map_docs_tz_marketplace_glossary, project_osm_map_docs_tz_marketplace_get_setting [EXTRACTED 1.00]
- **Поток экрана сделки: приложение → API → представления mp_service** — project_osm_map_readme_android_app, project_osm_map_readme_local_api_server, project_osm_map_readme_api_offers, project_osm_map_docs_tz_marketplace_v_deal_offer_list, project_osm_map_readme_api_agree, project_osm_map_readme_api_routes, project_osm_map_docs_tz_marketplace_v_deal_offer_route [EXTRACTED 1.00]
- **Меры против обхода платформы до согласования** — project_osm_map_docs_tz_marketplace_approx_offset, project_osm_map_docs_tz_marketplace_pseudonym, project_osm_map_docs_tz_marketplace_show_stage_near, project_osm_map_docs_tz_marketplace_antifraud [INFERRED 0.85]
- **Этапы алгоритма сделки: инициация → подбор → отклик → согласование → закрытие** — project_osm_map_docs_v0_7_01_deal_algorithm_stage_initiation, project_osm_map_docs_v0_7_01_deal_algorithm_stage_matching, project_osm_map_docs_v0_7_01_deal_algorithm_stage_offer, project_osm_map_docs_v0_7_01_deal_algorithm_stage_agreement, project_osm_map_docs_v0_7_01_deal_algorithm_stage_closing [EXTRACTED 1.00]
- **Этапы реализации Э0–Э8** — project_osm_map_docs_v0_7_03_implementation_spec_stage_e0, project_osm_map_docs_v0_7_03_implementation_spec_stage_e1, project_osm_map_docs_v0_7_03_implementation_spec_stage_e2, project_osm_map_docs_v0_7_03_implementation_spec_stage_e3, project_osm_map_docs_v0_7_03_implementation_spec_stage_e4, project_osm_map_docs_v0_7_03_implementation_spec_stage_e5, project_osm_map_docs_v0_7_03_implementation_spec_stage_e6, project_osm_map_docs_v0_7_03_implementation_spec_stage_e7, project_osm_map_docs_v0_7_03_implementation_spec_stage_e8 [EXTRACTED 1.00]
- **Правила сделки, проверяемые БД (переходы, подбор, отзывы, один selected)** — project_osm_map_docs_v0_7_02_db_architecture_trg_deal_status, project_osm_map_docs_v0_7_02_db_architecture_trg_deal_offer_check, project_osm_map_docs_v0_7_02_db_architecture_provider_can_offer, project_osm_map_docs_v0_7_02_db_architecture_trg_deal_review, project_osm_map_docs_v0_7_02_db_architecture_uq_deal_offer_selected, project_osm_map_docs_v0_7_02_db_architecture_dict_deal_transition [EXTRACTED 1.00]

## Communities (18 total, 7 thin omitted)

### Community 1 - "Сделки и отклики (ТЗ v0.6)"
Cohesion: 0.06
Nodes (40): Антифрод и agent_sanction (эскалация санкций), Кейс A/B/C/D (сделка 6, порядок B, D, C), Сделка deal, Отмена сделки (CANCEL_FREE/CLIENT/PROVIDER, EXPIRED), Отклик deal_offer, Статусы сделки INIT→MATCHING→OFFERS→AGREED→CONTACT→CLOSED, Отчёты по выручке (v_agent_finance_daily, v_company_finance_daily), geo_distance_m() (гаверсинус, ОВ-15/ОВ-19) (+32 more)

### Community 2 - "Алгоритм v0.7: подбор и сроки"
Cohesion: 0.06
Nodes (30): 6) Антифрод: BYPASS_GEO, NEAR_CANCEL, FAKE_REFERRALS, блокировки, Сценарий: частная парковка (capacity), Режимы сервиса: geo_mode, place_mode, price_unit, max_parallel_deals, slot_required, close_method, 2) Подбор поставщиков (MATCHING), Статус EXPIRED, Статус NOT_CLOSED, agent_rating(agent, role, service, on_date), get_setting(service_id, code, rating) (+22 more)

### Community 3 - "Алгоритм v0.7: выбор и закрытие"
Cohesion: 0.06
Nodes (30): Публикация (окно) поставщика (Н-2), Сценарий: парковка — публикация места, 4) Согласование (OFFERS → AGREED → CONTACT), 5) Закрытие сделки, 1) Инициация — клиент создаёт запрос (INIT → MATCHING), 3) Выбор — поставщик откликается (MATCHING → OFFERS), Статус CONTACT (сближение ≤ near_radius_m), agent_pending_reviews(agent_id) (+22 more)

### Community 4 - "Разделы ТЗ маркетплейса"
Cohesion: 0.06
Nodes (33): Антифрод, Жизненный цикл подписки, Изменения относительно исходной модели, Как добавить сервис, Карта в приложении, Кейс A/B/C/D, Модель данных, Монетизация (+25 more)

### Community 5 - "Окружение и запуск (Э0)"
Cohesion: 0.09
Nodes (24): Текущее состояние: Э0 выполнен, следующий шаг Э1 — БД v0.7, Эмуляторы Medium_Phone_API_37.0 (клиент, 5554) и Medium_Phone_B (поставщик, 5556), kazakhstan-latest.osm.pbf (Geofabrik, 214 МБ) — ждёт разрешения, Локальный API на Java (HttpServer + JDBC) над mp_service, db/mp_service.sql — схема маркетплейса и тестовые данные, project_osm_map — Android-приложение с картой OSM (osmdroid), Проверка на тестовых данных 25.10.2026 (расхождения v0.6), Миграция v0.6 → v0.7 (+16 more)

### Community 7 - "Контекст репозитория и договорённости"
Cohesion: 0.12
Nodes (20): Ветка feature/osm-map-offers, Холст эскизов интерфейса (шаблоны A/B/C), Договорённости, О проекте, Структура, Текущее состояние и решения, Сценарии приёмки S-1…S-16, Алгоритм формирования сделки v0.7 (+12 more)

### Community 8 - "Клиент API (ApiClient)"
Cohesion: 0.17
Nodes (6): ApiClient, ApiException, BaseLayer, Deal, Offer, RouteLink

### Community 9 - "Справочники и модель данных"
Cohesion: 0.11
Nodes (19): Триггер check_dict_type() (ОВ-22), Основной документ ТЗ в Claude Docs, Таблица dictionary (общий справочник, dict_type), Справочник MAP_BASE_LAYER / OSM_OFFLINE_ALMATY (ОВ-20), Ссылки на маршрут MAP_ROUTE_LINK (Яндекс, 2ГИС, Google), Схема mp_service (27 таблиц, 10 представлений, 6 функций), Открытые вопросы ОВ-1…ОВ-24, Подпись «© участники OpenStreetMap» (+11 more)

### Community 10 - "Режим тестирования и закрытие кодом"
Cohesion: 0.12
Nodes (19): Роли агентов: CLIENT, PROVIDER, SUBAGENT, COMPANY, ADMIN, Сценарий: онлайн-услуга (без CONTACT, ключ KEY), Режим тестирования администратора, Справочник MAP_CURSOR (капля / капля с шестерёнкой), Таблица agent (is_testing), Таблица agent_position [new], Триггер trg_agent_position (manual только при is_testing), Триггер trg_agent_testing (is_testing только при ADMIN) (+11 more)

### Community 11 - "Дорожная карта"
Cohesion: 0.50
Nodes (4): Офлайн-карта MBTiles (скачивание с CDN не сделано), Отложено: прогноз по спросу, оплата PLATFORM, сканер камерой, MBTiles с CDN, Рабочий контур (балансировщик, API ×N, реплика, CDN MBTiles, push, платежи), Дорожная карта: прототип v0.7 → следующий → далее

### Community 13 - "Уведомления"
Cohesion: 0.67
Nodes (3): Справочник NOTIFY_TYPE (NEW_REQUEST, NEW_OFFER, AGREED, NEAR, CLOSE_REQUEST, CLOSE_CODE, …), Таблица notification, GET /notifications?since (опрос 2–5 с), GET /me

## Knowledge Gaps
- **47 isolated node(s):** `Триггер check_dict_type() (ОВ-22)`, `Открытые вопросы ОВ-1…ОВ-24`, `ТОП-10 сервисов (парковка, такси, курьер … репетитор)`, `GET /api/map/base-layers`, `GET /api/deals/{id}/routes` (+42 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 131 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **7 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `ТЗ: платформа-маркетплейс услуг` connect `Разделы ТЗ маркетплейса` to `Сделки и отклики (ТЗ v0.6)`?**
  _High betweenness centrality (0.486) - this node is a cross-community bridge._
- **Are the 5 inferred relationships involving `provider_can_offer(deal_id, agent_id) → (ok, reason)` (e.g. with `Периоды как tsrange (П-6)` and `agent_rating(agent, role, service, on_date)`) actually correct?**
  _`provider_can_offer(deal_id, agent_id) → (ok, reason)` has 5 INFERRED edges - model-reasoned connections that need verification._
- **What connects `Триггер check_dict_type() (ОВ-22)`, `Открытые вопросы ОВ-1…ОВ-24`, `ТОП-10 сервисов (парковка, такси, курьер … репетитор)` to the rest of the system?**
  _47 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Экран сделки MainActivity` be split into smaller, more focused modules?**
  _Cohesion score 0.06666666666666667 - nodes in this community are weakly interconnected._
- **Why does `Триггер trg_deal_status` connect `Алгоритм v0.7: выбор и закрытие` to `Алгоритм v0.7: подбор и сроки`, `Окружение и запуск (Э0)`?**
  _High betweenness centrality (0.462) - this node is a cross-community bridge._
- **Should `Сделки и отклики (ТЗ v0.6)` be split into smaller, more focused modules?**
  _Cohesion score 0.059233449477351915 - nodes in this community are weakly interconnected._
- **Should `Алгоритм v0.7: подбор и сроки` be split into smaller, more focused modules?**
  _Cohesion score 0.06456456456456457 - nodes in this community are weakly interconnected._