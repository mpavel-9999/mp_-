# Graph Report - github_repo  (2026-10-24)

## Corpus Check
- 12 files · ~17,875 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 218 nodes · 309 edges · 14 communities (6 shown, 8 thin omitted)
- Extraction: 96% EXTRACTED · 4% INFERRED · 0% AMBIGUOUS · INFERRED: 12 edges (avg confidence: 0.89)
- Token cost: 104,033 input · 0 output

## Community Hubs (Navigation)
- Контекст репозитория и решения
- Разделы ТЗ маркетплейса
- Java API-сервер
- Сделки, отклики и монетизация
- Клиент API (ApiClient)
- Экран сделки MainActivity
- README и документы
- Глоссарий, пиктограммы, поиск

## God Nodes (most connected - your core abstractions)
1. `ApiClient` - 22 edges
2. `MainActivity` - 21 edges
3. `ТЗ: платформа-маркетплейс услуг` - 16 edges
4. `ApiServer` - 13 edges
5. `Android-приложение app/ (osmdroid, экран сделки)` - 8 edges
6. `Локальный API-сервер server/ (Java HttpServer + JDBC)` - 7 edges
7. `Схема mp_service (27 таблиц, 10 представлений, 6 функций)` - 7 edges
8. `ТЗ: платформа-маркетплейс услуг v0.6` - 6 edges
9. `v_deal_offer_list` - 6 edges
10. `Смещение точки до согласования approx_offset_m (ОВ-13)` - 6 edges

## Surprising Connections (you probably didn't know these)
- `project_parking (проект «парковка»)` --conceptually_related_to--> `ТОП-10 сервисов (парковка, такси, курьер … репетитор)`  [AMBIGUOUS]
  CLAUDE.md → project_osm_map/docs/TZ_marketplace.md
- `Ветка feature/osm-map-offers` --references--> `Кейс A/B/C/D (сделка 6, порядок B, D, C)`  [INFERRED]
  CLAUDE.md → project_osm_map/docs/TZ_marketplace.md
- `Ветка feature/osm-map-offers` --references--> `Локальный API-сервер server/ (Java HttpServer + JDBC)`  [EXTRACTED]
  CLAUDE.md → project_osm_map/README.md
- `adb reverse tcp:8080 + http://127.0.0.1:8080` --rationale_for--> `Android-приложение app/ (osmdroid, экран сделки)`  [EXTRACTED]
  CLAUDE.md → project_osm_map/README.md
- `SET search_path = mp_service в функциях схемы` --conceptually_related_to--> `get_setting()`  [INFERRED]
  CLAUDE.md → project_osm_map/docs/TZ_marketplace.md

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Поток экрана сделки: приложение → API → представления mp_service** — project_osm_map_readme_android_app, project_osm_map_readme_local_api_server, project_osm_map_readme_api_offers, project_osm_map_docs_tz_marketplace_v_deal_offer_list, project_osm_map_readme_api_agree, project_osm_map_readme_api_routes, project_osm_map_docs_tz_marketplace_v_deal_offer_route [EXTRACTED 1.00]
- **Меры против обхода платформы до согласования** — project_osm_map_docs_tz_marketplace_approx_offset, project_osm_map_docs_tz_marketplace_pseudonym, project_osm_map_docs_tz_marketplace_show_stage_near, project_osm_map_docs_tz_marketplace_antifraud [INFERRED 0.85]
- **Конфигурация сервиса данными (дерево, настройки, поля, глоссарий)** — project_osm_map_docs_tz_marketplace_service_tree, project_osm_map_docs_tz_marketplace_service_setting, project_osm_map_docs_tz_marketplace_service_attribute, project_osm_map_docs_tz_marketplace_glossary, project_osm_map_docs_tz_marketplace_get_setting [EXTRACTED 1.00]

## Communities (14 total, 8 thin omitted)

### Community 0 - "Контекст репозитория и решения"
Cohesion: 0.06
Nodes (33): Ветка feature/osm-map-offers, graphify-out (граф ведения проектов), Ближайшие шаги: офлайн-пакет, авторизация агента в API, экран создания сделки, project_parking (проект «парковка»), Репозиторий mpavel-9999/mp_-, Договорённости, О проекте, Структура (+25 more)

### Community 1 - "Разделы ТЗ маркетплейса"
Cohesion: 0.06
Nodes (32): Антифрод, Жизненный цикл подписки, Изменения относительно исходной модели, Как добавить сервис, Карта в приложении, Кейс A/B/C/D, Модель данных, Монетизация (+24 more)

### Community 3 - "Сделки, отклики и монетизация"
Cohesion: 0.10
Nodes (24): Антифрод и agent_sanction (эскалация санкций), Сделка deal, Отмена сделки (CANCEL_FREE/CLIENT/PROVIDER, EXPIRED), Отклик deal_offer, Статусы сделки INIT→MATCHING→OFFERS→AGREED→CONTACT→CLOSED, Отчёты по выручке (v_agent_finance_daily, v_company_finance_daily), geo_distance_m() (гаверсинус, ОВ-15/ОВ-19), Монетизация: абонплата за пакеты (+16 more)

### Community 5 - "Клиент API (ApiClient)"
Cohesion: 0.17
Nodes (6): ApiClient, ApiException, BaseLayer, Deal, Offer, RouteLink

### Community 7 - "README и документы"
Cohesion: 0.29
Nodes (5): API, OSM Карта — экран сделки маркетплейса услуг, Запуск, Карта, Состав

### Community 8 - "Глоссарий, пиктограммы, поиск"
Cohesion: 0.33
Nodes (6): glossary / glossary_text (термины и переводы), Пиктограммы icon (SVG currentColor), ui_config (конфигурация интерфейса, контрольная сумма), v_service_search, GET /api/icons, GET /api/services (поиск услуг)

## Ambiguous Edges - Review These
- `project_parking (проект «парковка»)` → `ТОП-10 сервисов (парковка, такси, курьер … репетитор)`  [AMBIGUOUS]
  CLAUDE.md · relation: conceptually_related_to

## Knowledge Gaps
- **17 isolated node(s):** `Репозиторий mpavel-9999/mp_-`, `Переменные MP_DB_URL/MP_DB_USER/MP_DB_PASSWORD/MP_API_HOST/MP_API_PORT`, `GET /api/deals/{id}`, `GET /api/deals/{id}/offers`, `GET /api/deals/{id}/routes` (+12 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 95 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **8 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `ТЗ: платформа-маркетплейс услуг` connect `Разделы ТЗ маркетплейса` to `README и документы`?**
  _High betweenness centrality (0.577) - this node is a cross-community bridge._
- **What connects `Репозиторий mpavel-9999/mp_-`, `Переменные MP_DB_URL/MP_DB_USER/MP_DB_PASSWORD/MP_API_HOST/MP_API_PORT`, `GET /api/deals/{id}` to the rest of the system?**
  _17 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Контекст репозитория и решения` be split into smaller, more focused modules?**
  _Cohesion score 0.06463414634146342 - nodes in this community are weakly interconnected._
- **Why does `ApiClient` connect `Клиент API (ApiClient)` to `Экран сделки MainActivity`?**
  _High betweenness centrality (0.477) - this node is a cross-community bridge._
- **Should `Разделы ТЗ маркетплейса` be split into smaller, more focused modules?**
  _Cohesion score 0.0625 - nodes in this community are weakly interconnected._
- **Why does `Процесс сделки` connect `Разделы ТЗ маркетплейса` to `Сделки, отклики и монетизация`?**
  _High betweenness centrality (0.441) - this node is a cross-community bridge._

### Low-confidence Hints
_AMBIGUOUS edges — the extractor was unsure. Verify before acting on these._

- **What is the exact relationship between `project_parking (проект «парковка»)` and `ТОП-10 сервисов (парковка, такси, курьер … репетитор)`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._