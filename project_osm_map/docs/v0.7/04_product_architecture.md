# Общая архитектура продукта

Версия 0.7 от 25.10.2026. Платформа-маркетплейс услуг: агенты предоставляют и получают услуги (10 сервисов в 5 категориях — от парковки до репетитора). Новый сервис добавляется данными, интерфейс собирается из конфигурации без выпуска новой версии приложения.

Детали: логика сделки — [01_deal_algorithm.md](01_deal_algorithm.md), БД — [02_db_architecture.md](02_db_architecture.md), реализация — [03_implementation_spec.md](03_implementation_spec.md). Схемы ниже — в нотации Mermaid (открываются в GitHub, VS Code, Obsidian).

## 1. Контекст системы

```mermaid
flowchart LR
    C["Клиент<br/>(Android)"] -- "HTTPS / JSON" --> API
    P["Поставщик / субагент<br/>(Android)"] -- "HTTPS / JSON" --> API
    CO["Компания<br/>(Android)"] -- "HTTPS / JSON" --> API
    AD["Администратор<br/>режим тестирования"] -- "HTTPS / JSON" --> API
    API["API-сервер<br/>(Java)"] -- "JDBC" --> DB[("PostgreSQL<br/>mp_service")]
    API -- "граф дорог" --> GH["GraphHopper<br/>данные OSM"]
    C -. "тайлы" .-> OSM["Подложка OSM<br/>(тест: tile.openstreetmap.org,<br/>рабочая: офлайн-пакет с CDN)"]
    P -. "тайлы" .-> OSM
    C -. "ссылка на маршрут" .-> EXT["Яндекс Карты · 2ГИС · Google Maps"]
    API -. "следующий этап" .-> PAY["Платёжный провайдер<br/>(абонплата, выплаты)"]
    API -. "следующий этап" .-> PUSH["Push-уведомления"]
```

## 2. Функциональные блоки

| Блок | Что делает | Где живёт |
| --- | --- | --- |
| Каталог услуг | Дерево «категория → сервис → вариант», поиск, синонимы, пиктограммы | БД: `service`, `glossary*`, `icon`; API `/services` |
| Конфигурация сервисов | Настройки по дереву и рейтингу, поля карточек/запросов/откликов | БД: `service_setting`, `attribute`, `service_attribute`, `get_setting()` |
| Агенты и роли | Клиент, поставщик, компания, субагент, администратор; профиль с историей | БД: `agent*`; API `/me` |
| Подписки и монетизация | Пакеты, пробный период, абонплата, субагенты сверх пакета | БД: `service_package`, `agent_subscription`, `subscription_payment` |
| Рефералы | 3 уровня 10/5/2%, начисление после периода, выплата | БД: `referral_reward` |
| Публикации поставщиков | Окна доступности: место, маршрут такси | БД: `provider_slot`; API `/slots`, `/map/slots` |
| Сделки | Запрос, подбор, отклики, согласование, закрытие, отмена, сроки | БД: `deal*`, триггеры; API `/deals`, `/requests` |
| Гео и карта | Положения, зоны, смещение точек, сближение, курсоры | БД: `agent_position`, `v_deal_offer_list`; приложение: osmdroid |
| Маршрутизация | Маршрут по дорогам, коридор, направление, длина | Сервер: GraphHopper |
| Закрытие сделки | Динамический QR / ключ, тестовое подтверждение | Сервер: HMAC; приложение: QR, сканер |
| Уведомления | Новые запросы и отклики, согласование, сближение, закрытие, сроки, санкции | БД: `notification`; API `/notifications` (опрос) |
| Рейтинг и отзывы | Баллы обязательны, рейтинг по дням (0,5 системный + 0,5 отзывы) | БД: `deal_review`, `rating_history` |
| Антифрод | Сигналы, предупреждения, понижение, блокировки, уведомления компании | БД: `agent_sanction`; сервер: плановые задания |
| Конфигурация интерфейса | Шаблоны A/B/C, схемы LIGHT/DARK, контрольная сумма | БД: `ui_config`, `dictionary` (`UI_TEMPLATE`, `UI_SCHEME`); API `/config` |
| Локализация | Тексты интерфейса и уведомлений на языке агента | БД: `glossary_text`; приложение: `core/i18n` |
| Отчёты | Доходы и расходы агента и компании по дням | БД: `v_agent_finance_daily`, `v_company_finance_daily` |
| Режим тестирования | Курсор вместо GPS, QR без камеры, шестерёнка на курсоре | БД: `agent.is_testing`; API, приложение |

## 3. Компоненты и модули

```mermaid
flowchart TB
    subgraph APP["Android-приложение (Kotlin, Jetpack Compose)"]
        direction TB
        UI["ui/template a · b · c<br/>раскладки экранов"] --> FEAT["feature/*<br/>login · settings · home · request · offers<br/>deal · provider · review"]
        FEAT --> VM["ViewModel + StateFlow"]
        VM --> REPO["data/ репозитории"]
        REPO --> NET["core/net"]
        VM --> CORE["core/config · theme · i18n · icons · location"]
        MAP["ui/map osmdroid<br/>зона · маркеры · курсоры"] --> FEAT
    end

    subgraph SRV["API-сервер (Java, HttpServer + JDBC)"]
        direction TB
        H["HTTP-обработчики /api/*"] --> SVC["Сервисы: сессии · сделки · публикации<br/>закрытие (HMAC) · уведомления"]
        SVC --> RT["Маршрутизация GraphHopper"]
        SVC --> JOB["Плановые задания (1 мин):<br/>EXPIRED · CLOSING_ASK · NOT_CLOSED · CONTACT"]
        SVC --> SQL["SQL-доступ<br/>SET mp.agent_id / mp.geo_secret"]
    end

    subgraph DBS["PostgreSQL mp_service"]
        direction TB
        T["29 таблиц"] --- F["Функции: get_setting · provider_can_offer<br/>price_forecast · agent_rating · geo_distance_m"]
        T --- TR["Триггеры: переходы статусов · проверка отклика<br/>отзывы · тестовый режим · тип справочника"]
        T --- V["Представления: v_deal_candidates · v_deal_offer_list<br/>v_provider_slot_map · v_ui_options · v_map_cursor …"]
    end

    NET -- "JSON, опрос 2–5 с" --> H
    SQL -- "JDBC" --> T
```

Принцип разделения:
- **БД** — источник истины по правилам: переходы статусов, допустимость отклика, отзывы, настройки. Обойти её из API или приложения нельзя.
- **API** — сессии, секреты (код закрытия, смещение точек), маршрутизация, плановые задания, сборка JSON.
- **Приложение** — отображение по шаблону, ввод, карта, положение; бизнес-правил не дублирует, берёт `actions` сделки из API.

## 4. Развёртывание

### 4.1 Прототип (локально)

```mermaid
flowchart LR
    subgraph PC["Компьютер разработчика (Windows)"]
        PG[("PostgreSQL 17<br/>:5432")]
        API2["API-сервер<br/>127.0.0.1:8080"]
        GH2["GraphHopper<br/>kazakhstan-latest.osm.pbf"]
        API2 --- PG
        API2 --- GH2
        subgraph EMU["Эмуляторы Android (API 37)"]
            E1["Эмулятор 1<br/>агент 1 · клиент · тест"]
            E2["Эмулятор 2<br/>агент 2 · поставщик · тест"]
        end
        E1 -- "adb reverse tcp:8080" --> API2
        E2 -- "adb reverse tcp:8080" --> API2
    end
    E1 -. "тайлы" .-> TILE["tile.openstreetmap.org"]
    E2 -. "тайлы" .-> TILE
```

### 4.2 Рабочий контур (целевой)

```mermaid
flowchart LR
    MOB["Мобильные клиенты"] -- "HTTPS" --> LB["Балансировщик"]
    LB --> A1["API ×N"]
    A1 --> PGM[("PostgreSQL<br/>основная + реплика")]
    A1 --> GHR["GraphHopper"]
    A1 --> PUSH2["Push"]
    A1 --> PAY2["Платёжный провайдер"]
    CDN["CDN платформы<br/>офлайн-пакеты OSM MBTiles,<br/>пиктограммы"] --> MOB
```

## 5. Жизненный цикл сделки

```mermaid
stateDiagram-v2
    [*] --> INIT : клиент создал запрос
    INIT --> MATCHING : запрос сохранён
    MATCHING --> OFFERS : первый отклик
    MATCHING --> EXPIRED : начало периода без откликов
    OFFERS --> AGREED : клиент «Согласовать»
    OFFERS --> EXPIRED : начало периода без согласования
    AGREED --> CONTACT : стороны ближе near_radius_m
    AGREED --> CLOSED : online, ключ KEY
    CONTACT --> CLOSED : код закрытия принят
    AGREED --> NOT_CLOSED : 24 ч без закрытия
    CONTACT --> NOT_CLOSED : 24 ч без закрытия
    INIT --> CANCEL_FREE
    MATCHING --> CANCEL_FREE
    OFFERS --> CANCEL_FREE
    AGREED --> CANCEL_FREE : до cancel_free_min и до CONTACT
    AGREED --> CANCEL_CLIENT
    AGREED --> CANCEL_PROVIDER
    CONTACT --> CANCEL_CLIENT
    CONTACT --> CANCEL_PROVIDER
    CLOSED --> [*]
    EXPIRED --> [*]
    NOT_CLOSED --> [*]
    CANCEL_FREE --> [*]
    CANCEL_CLIENT --> [*]
    CANCEL_PROVIDER --> [*]
```

## 6. Взаимодействия

### 6.1 Публикация, запрос, отклик, согласование

```mermaid
sequenceDiagram
    autonumber
    actor P as Поставщик
    actor C as Клиент
    participant A as API
    participant D as БД
    P->>A: POST /slots (место, окно 18:00–18:30, мин. цена)
    A->>D: INSERT provider_slot
    C->>A: GET /map/slots
    A->>D: v_provider_slot_map (точки смещены)
    A-->>C: свободные места
    C->>A: POST /forecast
    A->>D: price_forecast()
    A-->>C: 700–1500, медиана 950
    C->>A: POST /deals (slot_id, период, радиус, «моя цена»)
    A->>D: проверка баллов, INSERT deal → MATCHING (триггер логирует)
    A->>D: v_deal_candidates
    A->>D: INSERT notification NEW_REQUEST поставщикам
    A->>D: автоотклики (is_auto = 1)
    P->>A: GET /notifications → GET /requests
    P->>A: POST /deals/{id}/offers (цена)
    A->>D: trg_deal_offer_check: provider_can_offer → OFFERS
    A->>D: NEW_OFFER клиенту
    C->>A: GET /deals/{id}/offers
    A-->>C: B, D, C (смещённые точки, расстояние интервалом)
    C->>A: POST /offers/{id}/agree
    A->>D: selected / declined / withdrawn, AGREED, close_secret
    A->>D: AGREED поставщику, OFFER_WITHDRAWN другим клиентам
    A-->>C: точная точка, ссылки на маршрут
```

### 6.2 Сближение и закрытие

```mermaid
sequenceDiagram
    autonumber
    actor C as Клиент
    actor P as Поставщик
    participant A as API
    participant D as БД
    loop каждые 10 с (GPS) или при отпускании курсора (тест)
        C->>A: PUT /me/position
        P->>A: PUT /me/position
    end
    A->>D: задание «Сближение»: расстояние ≤ near_radius_m
    A->>D: AGREED → CONTACT, NEAR обеим сторонам
    C->>A: GET /deals/{id} → открыт гос. номер / телефон (show_stage = near)
    alt Клиент нажал «Закрыть сделку»
        C->>A: POST /close/request
        A->>D: CLOSE_REQUEST поставщику
        P->>A: GET /notifications → экран кода открывается сам
    else Поставщик нажал «Закрыть сделку»
        P->>A: открывает экран кода
    end
    loop каждые close_qr_ttl_s (10 с)
        P->>A: GET /close/code
        A-->>P: QR + цифры, expires_in_s
    end
    C->>A: POST /close {code}
    A->>A: HMAC(close_secret, интервал) — текущий или предыдущий
    A->>D: CONTACT → CLOSED, REVIEW_REQUIRED обеим
    C->>A: POST /review
    P->>A: POST /review
```

### 6.3 Режим тестирования: закрытие без камеры

```mermaid
sequenceDiagram
    autonumber
    actor P as Поставщик (тест)
    actor C as Клиент (тест)
    participant A as API
    alt «Предоставить QR»
        P->>A: POST /close/test-provide
        A->>C: уведомление CLOSE_CODE с кодом
        C->>A: POST /close {code} (автоматически)
    else «Сканировать QR»
        C->>A: выбирает сделку, POST /close/test-scan
        A->>A: подставляет действующий код
    end
    A-->>C: CLOSED → форма баллов
    A-->>P: CLOSED → форма баллов
```

### 6.4 Подбор такси по пути

```mermaid
flowchart TD
    S["Запрос такси: П, Н, класс"] --> K{"Класс водителя = класс заказа<br/>или в accepts_variants?"}
    K -- нет --> X["Не подходит"]
    K -- да --> B{"Водитель указал Б?"}
    B -- нет --> R0{"П в радиусе rA вокруг А?"}
    R0 -- нет --> X
    R0 -- да --> OK
    B -- да --> R1{"П в коридоре маршрута А→Б ±rA?<br/>(GraphHopper)"}
    R1 -- нет --> X
    R1 -- да --> R2{"Н в коридоре ±rA<br/>или в радиусе rB вокруг Б?"}
    R2 -- нет --> X
    R2 -- да --> R3{"П раньше Н<br/>по ходу маршрута?"}
    R3 -- нет --> X
    R3 -- да --> PC["provider_can_offer:<br/>время · цена · рейтинги · доступ · блокировка · лимит"]
    PC -- нет --> X
    PC -- да --> OK["Водитель получает запрос<br/>цена = max(порог, ₸/км × длина П→Н)"]
```

### 6.5 Загрузка конфигурации интерфейса

```mermaid
sequenceDiagram
    participant App as Приложение
    participant A as API
    participant D as БД
    App->>A: GET /config?checksum=<кэш>
    A->>D: ui_config.checksum
    alt совпадает
        A-->>App: 304 — используем кэш
    else отличается
        A->>D: шаблон, палитра, тексты, пиктограммы, курсоры
        A-->>App: 200 + новая контрольная сумма
        App->>App: сохранить, перестроить тему и раскладку
    end
    App->>A: PUT /config (Шаблон B / Тёмная, ENG)
    A->>D: ui_config.config_json, checksum = md5
    A-->>App: новая конфигурация
```

### 6.6 Антифрод

```mermaid
flowchart LR
    SIG["Сигналы:<br/>встреча без сделки (BYPASS_GEO)<br/>отмена после показа номеров (NEAR_CANCEL)<br/>фиктивные рефералы (FAKE_REFERRALS)"] --> CNT{"Эпизод №"}
    CNT -- "1" --> W["Предупреждение<br/>о понижении рейтинга"]
    CNT -- "2" --> RD["Понижение system_rating<br/>+ предупреждение о блокировке"]
    CNT -- ">2" --> CHK{"Подозрительные отзывы<br/>И падение выручки?"}
    CHK -- нет --> RD
    CHK -- да --> BL["Блокировка: неделя → месяц → год"]
    W --> N["Уведомление агенту<br/>и компании субагента"]
    RD --> N
    BL --> N
```

## 7. Модель конфигурации: «новый сервис — только данными»

```mermaid
flowchart LR
    G["glossary + glossary_text<br/>название, синонимы, пиктограмма"] --> S["service<br/>узел под категорией"]
    S --> SS["service_setting<br/>только отличия: geo_mode, place_mode,<br/>price_unit, close_method …"]
    S --> SA["service_attribute<br/>свои поля: profile / request / offer"]
    S --> PK["package_service<br/>если категории нет в пакете"]
    SS --> APPX["Приложение строит форму,<br/>карту и правила без обновления"]
    SA --> APPX
```

Пример: «Эвакуатор» в «Транспорт и авто» — 1 термин, 2 перевода, 1 узел, 3 настройки (`geo_mode = route`, `place_mode = client`, `price_unit = km`); поля «Гос. номер» и «Марка и цвет авто» уже есть.

## 8. Интерфейс: шаблоны и схемы

| Шаблон | Идея | Главная | Запрос | Отклики | Навигация | Шрифт |
| --- | --- | --- | --- | --- | --- | --- |
| A «Карта в центре» | Гео-сервисы: карта на весь экран, шторка снизу | карта + шторка | шторка | карта + шторка | кнопки сверху | Manrope |
| B «Карточки» | Супер-приложение: плитки, мастер по шагам | плитки | мастер | список / карта | нижнее меню | Golos Text |
| C «Диспетчер» | Плотные данные: карта + таблица, моноширинные цифры | плотный список | строки формы | карта + таблица | кнопка действия | Onest + JetBrains Mono |

Каждый шаблон — в схемах LIGHT и DARK; выбор «Шаблон / Схема» и язык — в настройках, применяются сразу. Логика и данные экранов общие, шаблон меняет только раскладку и оформление. Эскизы — холст «Эскизы маркетплейса услуг».

## 9. Нефункциональные требования

| Область | Требование |
| --- | --- |
| Безопасность | Секреты только в переменных окружения; точное место и имя поставщика до согласования не отдаются; код закрытия не выводится из данных клиента |
| Персональные данные | Поля другой стороны — по `show_stage` и статусу; номер карты не хранится (токен провайдера) |
| Карта | Подпись «© участники OpenStreetMap» всегда видна; массовая загрузка с tile.openstreetmap.org запрещена — рабочая подложка офлайн-пакетом |
| Доступность | Контраст текста ≥ 4.5:1, зоны нажатия ≥ 48 dp |
| Обновление интерфейса | Без выпуска приложения: шаблон, палитра, тексты, пиктограммы, поля и настройки сервисов — из БД |
| Масштабирование | Переход на PostGIS и push вместо опроса — при росте нагрузки |

## 10. Дорожная карта

| Этап | Содержание |
| --- | --- |
| Прототип v0.7 | Полный цикл сделки на двух эмуляторах, шаблоны A/B/C × LIGHT/DARK, режим тестирования, такси по пути |
| Следующий | Регистрация и профиль, финансовый кабинет, офлайн-пакет OSM с CDN, push, прогноз цены по спросу и предложению |
| Далее | Оплата через счёт платформы, выплаты рефералам, PostGIS, рабочий контур |
