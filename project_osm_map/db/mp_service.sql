-- =====================================================================
-- Маркетплейс услуг: схема mp_service, структура БД + тестовые данные
-- Версия модели 0.7. Требуется PostgreSQL 15+ (UNIQUE NULLS NOT DISTINCT).
-- Описание: docs/v0.7/01_deal_algorithm.md (правила), docs/v0.7/02_db_architecture.md (схема).
-- Функции, читающие таблицы, закреплены за схемой (SET search_path = mp_service):
-- представления работают из любой сессии, без SET search_path у клиента.
-- Правила процесса сделки проверяет БД (раздел 9): переходы статусов, допустимость отклика, отзывы.
-- Параметры сессии, которые задает API-сервер:
--   mp.agent_id   - агент, от имени которого выполняется действие (пусто = система)
--   mp.geo_secret - секрет смещения точек до согласования (переменная окружения MP_GEO_SECRET)
--   mp.load_mode  - 'on' только при загрузке тестовых данных: триггеры процесса не проверяют и не логируют
-- Соглашения:
--   DIM  - справочник, LIST - реестр, LINK - связка, FAKT - факты/события
--   Общие поля версионности: date_creation, date_start, date_end (NULL = бессрочно), is_active
--   Новый сервис добавляется только данными: узел в service, тексты в glossary,
--   поля в service_attribute, настройки в service_setting, включение в пакет.
-- ВНИМАНИЕ: скрипт удаляет схему mp_service целиком.
-- =====================================================================

DROP SCHEMA IF EXISTS mp_service CASCADE;
CREATE SCHEMA mp_service;
-- pgcrypto: хеш пароля входа (crypt / gen_salt); ставится в public, функции схемы видят его через search_path
CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;
SET search_path TO mp_service, public;

-- ---------------------------------------------------------------------
-- 1. ЯЗЫКИ, ПИКТОГРАММЫ, ГЛОССАРИЙ, ОБЩИЙ СПРАВОЧНИК
-- ---------------------------------------------------------------------

-- DIM Язык интерфейса
CREATE TABLE interface_lang (
    interface_lang_id  numeric      PRIMARY KEY,
    language_code      text         NOT NULL UNIQUE,      -- RUS, ENG, KAZ, DEU
    language_name      text         NOT NULL,
    date_creation      timestamp    NOT NULL DEFAULT now(),
    date_start         timestamp    NOT NULL DEFAULT now(),
    date_end           timestamp,
    is_active          smallint     NOT NULL DEFAULT 1 CHECK (is_active IN (0,1))
);

-- DIM Пиктограммы. SVG рисуется цветом currentColor, поэтому подходит для любой темы.
CREATE TABLE icon (
    icon_id            numeric      PRIMARY KEY,
    icon_code          text         NOT NULL UNIQUE,      -- parking, car, phone ...
    icon_format        text         NOT NULL CHECK (icon_format IN ('svg','png')),
    icon_svg           text,                              -- inline SVG 24x24
    icon_url           text,                              -- или ссылка на файл (CDN)
    date_creation      timestamp    NOT NULL DEFAULT now(),
    date_start         timestamp    NOT NULL DEFAULT now(),
    date_end           timestamp,
    is_active          smallint     NOT NULL DEFAULT 1 CHECK (is_active IN (0,1)),
    CHECK (icon_svg IS NOT NULL OR icon_url IS NOT NULL)
);

-- DIM Термин глоссария (не зависит от языка): код и пиктограмма.
-- На термин ссылаются сервисы, атрибуты, справочник, пакеты - настоящими FK.
CREATE TABLE glossary (
    glossary_id        numeric      PRIMARY KEY,
    glossary_code      text         NOT NULL UNIQUE,
    icon_id            numeric      REFERENCES icon,
    date_creation      timestamp    NOT NULL DEFAULT now(),
    date_start         timestamp    NOT NULL DEFAULT now(),
    date_end           timestamp,
    is_active          smallint     NOT NULL DEFAULT 1 CHECK (is_active IN (0,1))
);

-- DIM Перевод термина + синонимы для поиска
CREATE TABLE glossary_text (
    glossary_id        numeric      NOT NULL REFERENCES glossary,
    interface_lang_id  numeric      NOT NULL REFERENCES interface_lang,
    text_value         text         NOT NULL,
    search_text        text,                              -- синонимы через пробел
    date_creation      timestamp    NOT NULL DEFAULT now(),
    date_start         timestamp    NOT NULL DEFAULT now(),
    date_end           timestamp,
    is_active          smallint     NOT NULL DEFAULT 1 CHECK (is_active IN (0,1)),
    PRIMARY KEY (glossary_id, interface_lang_id)
);

-- DIM Общий справочник для коротких списков значений.
-- dict_type: ROLE, DEAL_STATUS, DEAL_TRANSITION, CANCEL_REASON, MAP_BASE_LAYER, MAP_ROUTE_LINK,
--            MAP_CURSOR, REFERRAL_LEVEL, PAYMENT_METHOD, NOTIFY_TYPE, SANCTION_REASON,
--            UI_TEMPLATE, UI_SCHEME
CREATE TABLE dictionary (
    dict_id            numeric      PRIMARY KEY,
    dict_type          text         NOT NULL,
    dict_code          text         NOT NULL,
    parent_dict_id     numeric      REFERENCES dictionary,
    glossary_id        numeric      NOT NULL REFERENCES glossary,
    sort_order         numeric      NOT NULL DEFAULT 1,
    value_num          numeric,                           -- % уровня, признак финального статуса ...
    value_text         text,                              -- шаблон ссылки, URL тайлов ...
    value_json         jsonb,                             -- раскладка шаблона интерфейса, палитра
    date_creation      timestamp    NOT NULL DEFAULT now(),
    date_start         timestamp    NOT NULL DEFAULT now(),
    date_end           timestamp,
    is_active          smallint     NOT NULL DEFAULT 1 CHECK (is_active IN (0,1)),
    UNIQUE (dict_type, dict_code)
);

-- ---------------------------------------------------------------------
-- 2. СЕРВИСЫ: ИЕРАРХИЯ, НАСТРОЙКИ, АТРИБУТЫ, ПАКЕТЫ
-- ---------------------------------------------------------------------

-- DIM Дерево услуг: категория -> сервис -> вариант услуги.
-- Настройки и атрибуты наследуются вниз по дереву (ближайший узел побеждает).
CREATE TABLE service (
    service_id         numeric      PRIMARY KEY,
    parent_service_id  numeric      REFERENCES service,
    node_type          text         NOT NULL CHECK (node_type IN ('system','category','service','variant')),
    glossary_id        numeric      NOT NULL REFERENCES glossary,
    sort_order         numeric      NOT NULL DEFAULT 1,
    date_creation      timestamp    NOT NULL DEFAULT now(),
    date_start         timestamp    NOT NULL DEFAULT now(),
    date_end           timestamp,
    is_active          smallint     NOT NULL DEFAULT 1 CHECK (is_active IN (0,1))
);

-- DIM Настройки: service_id NULL = вся платформа; rating_min/max - настройка зависит от рейтинга.
-- Коды: geo_mode (point|route|none), place_mode (provider|client|online), price_unit (fixed|hour|km),
--   offer_sort_1..3 ('distance:asc'), near_radius_m, approx_offset_m, deal_fee_percent,
--   forecast_window_min, forecast_grid_m, rating_weight_system, rating_weight_review, currency
CREATE TABLE service_setting (
    setting_id         numeric      PRIMARY KEY,
    service_id         numeric      REFERENCES service,
    setting_code       text         NOT NULL,
    rating_min         numeric(3,2),                      -- включительно
    rating_max         numeric(3,2),                      -- не включительно
    setting_value      text         NOT NULL,
    date_creation      timestamp    NOT NULL DEFAULT now(),
    date_start         timestamp    NOT NULL DEFAULT now(),
    date_end           timestamp,
    is_active          smallint     NOT NULL DEFAULT 1 CHECK (is_active IN (0,1)),
    UNIQUE NULLS NOT DISTINCT (service_id, setting_code, rating_min)
);

-- DIM Атрибуты: поля карточек, группы, списки выбора (dim) и их значения (par)
CREATE TABLE attribute (
    attribute_id        numeric     PRIMARY KEY,
    parent_attribute_id numeric     REFERENCES attribute,
    attribute_type      text        NOT NULL CHECK (attribute_type IN
                        ('group','text','number','mobile','email','date','datetime','card_token',
                         'card_exp','iin','car_number','address','photo','dim','par')),
    glossary_id         numeric     NOT NULL REFERENCES glossary,
    sort_order          numeric     NOT NULL DEFAULT 1,
    date_creation       timestamp   NOT NULL DEFAULT now(),
    date_start          timestamp   NOT NULL DEFAULT now(),
    date_end            timestamp,
    is_active           smallint    NOT NULL DEFAULT 1 CHECK (is_active IN (0,1))
);

-- LINK Атрибуты узла дерева услуг
--   attr_scope: profile - карточка агента, request - заполняет клиент в сделке,
--               offer - заполняет поставщик в отклике
--   show_stage: когда значение видно другой стороне сделки
CREATE TABLE service_attribute (
    service_id         numeric      NOT NULL REFERENCES service,
    attribute_id       numeric      NOT NULL REFERENCES attribute,
    attr_scope         text         NOT NULL CHECK (attr_scope IN ('profile','request','offer')),
    is_required        smallint     NOT NULL DEFAULT 0 CHECK (is_required IN (0,1)),
    show_stage         text         NOT NULL DEFAULT 'never'
                       CHECK (show_stage IN ('always','never','agreed','near')),
    sort_order         numeric      NOT NULL DEFAULT 1,
    date_creation      timestamp    NOT NULL DEFAULT now(),
    date_start         timestamp    NOT NULL DEFAULT now(),
    date_end           timestamp,
    is_active          smallint     NOT NULL DEFAULT 1 CHECK (is_active IN (0,1)),
    PRIMARY KEY (service_id, attribute_id)
);

-- DIM Пакет (абонплата). Лимита субагентов нет: included_subagents входят в цену,
-- каждый следующий стоит extra_subagent_price.
CREATE TABLE service_package (
    package_id           numeric      PRIMARY KEY,
    glossary_id          numeric      NOT NULL REFERENCES glossary,
    role_id              numeric      NOT NULL REFERENCES dictionary,
    price                numeric(12,2) NOT NULL,
    price_period         text         NOT NULL DEFAULT 'month' CHECK (price_period IN ('month','year')),
    included_subagents   numeric      NOT NULL DEFAULT 0,
    extra_subagent_price numeric(12,2) NOT NULL DEFAULT 0,
    trial_days           numeric      NOT NULL DEFAULT 0,
    currency_code        text         NOT NULL DEFAULT 'KZT',
    date_creation        timestamp    NOT NULL DEFAULT now(),
    date_start           timestamp    NOT NULL DEFAULT now(),
    date_end             timestamp,
    is_active            smallint     NOT NULL DEFAULT 1 CHECK (is_active IN (0,1))
);

-- LINK Состав пакета. Узел-категория включает все сервисы под ней.
CREATE TABLE package_service (
    package_id         numeric      NOT NULL REFERENCES service_package,
    service_id         numeric      NOT NULL REFERENCES service,
    date_creation      timestamp    NOT NULL DEFAULT now(),
    date_start         timestamp    NOT NULL DEFAULT now(),
    date_end           timestamp,
    is_active          smallint     NOT NULL DEFAULT 1 CHECK (is_active IN (0,1)),
    PRIMARY KEY (package_id, service_id)
);

-- ---------------------------------------------------------------------
-- 3. АГЕНТЫ
-- ---------------------------------------------------------------------

-- LIST Агент
CREATE TABLE agent (
    agent_id           numeric      PRIMARY KEY,
    interface_lang_id  numeric      NOT NULL REFERENCES interface_lang,
    referrer_agent_id  numeric      REFERENCES agent,     -- кто пригласил
    is_testing         smallint     NOT NULL DEFAULT 0 CHECK (is_testing IN (0,1)),  -- режим тестирования (только ADMIN)
    date_creation      timestamp    NOT NULL DEFAULT now(),
    date_start         timestamp    NOT NULL DEFAULT now(),
    date_end           timestamp,
    is_active          smallint     NOT NULL DEFAULT 1 CHECK (is_active IN (0,1))
);

-- LINK Роли агента; parent_agent_id - компания субагента
CREATE TABLE agent_role (
    agent_id           numeric      NOT NULL REFERENCES agent,
    role_id            numeric      NOT NULL REFERENCES dictionary,
    parent_agent_id    numeric      REFERENCES agent,
    date_creation      timestamp    NOT NULL DEFAULT now(),
    date_start         timestamp    NOT NULL DEFAULT now(),
    date_end           timestamp,
    is_active          smallint     NOT NULL DEFAULT 1 CHECK (is_active IN (0,1)),
    PRIMARY KEY (agent_id, role_id)
);

-- LINK Сервисы агента + настройки поставщика (для клиента настройки пустые).
-- Субагенту сервис назначает компания (assigned_by_agent_id) на период date_start..date_end.
CREATE TABLE agent_service (
    agent_id             numeric      NOT NULL REFERENCES agent,
    service_id           numeric      NOT NULL REFERENCES service,
    geo_lat              numeric(9,6),                    -- точка и зона обслуживания по умолчанию
    geo_lon              numeric(9,6),
    zone_radius_m        numeric,
    work_time_from       time,                            -- для сервисов без публикаций (slot_required = 0)
    work_time_to         time,
    price_min            numeric(12,2),                   -- такси: порог минимальной стоимости поездки
    price_max            numeric(12,2),
    unit_price_min       numeric(12,2),                   -- минимальная цена за единицу price_unit (км, час)
    capacity             numeric,                         -- емкость (частная парковка); NULL - max_parallel_deals
    client_rating_min    numeric(3,2),
    is_auto              smallint     NOT NULL DEFAULT 0 CHECK (is_auto IN (0,1)),
    assigned_by_agent_id numeric      REFERENCES agent,   -- компания, назначившая сервис субагенту
    date_creation      timestamp    NOT NULL DEFAULT now(),
    date_start         timestamp    NOT NULL DEFAULT now(),
    date_end           timestamp,
    is_active          smallint     NOT NULL DEFAULT 1 CHECK (is_active IN (0,1)),
    PRIMARY KEY (agent_id, service_id)
);

-- FAKT Значения атрибутов профиля агента (история через date_start/date_end)
CREATE TABLE agent_attribute (
    agent_id           numeric      NOT NULL REFERENCES agent,
    attribute_id       numeric      NOT NULL REFERENCES attribute,
    attribute_value    text         NOT NULL,
    date_creation      timestamp    NOT NULL DEFAULT now(),
    date_start         timestamp    NOT NULL DEFAULT now(),
    date_end           timestamp,
    is_active          smallint     NOT NULL DEFAULT 1 CHECK (is_active IN (0,1)),
    PRIMARY KEY (agent_id, attribute_id, date_start)
);

-- LIST Конфигурация интерфейса + контрольная сумма
CREATE TABLE ui_config (
    agent_id           numeric      PRIMARY KEY REFERENCES agent,
    config_json        jsonb        NOT NULL,
    checksum           text         NOT NULL,             -- md5(config_json::text)
    date_creation      timestamp    NOT NULL DEFAULT now(),
    date_start         timestamp    NOT NULL DEFAULT now(),
    date_end           timestamp,
    is_active          smallint     NOT NULL DEFAULT 1 CHECK (is_active IN (0,1))
);

-- LIST Вход агента по логину и паролю. Хранится только хеш bcrypt (pgcrypto), пароль - никогда.
CREATE TABLE agent_login (
    login              text         PRIMARY KEY,
    agent_id           numeric      NOT NULL UNIQUE REFERENCES agent,
    password_hash      text         NOT NULL,
    date_last_login    timestamp,
    date_creation      timestamp    NOT NULL DEFAULT now(),
    date_start         timestamp    NOT NULL DEFAULT now(),
    date_end           timestamp,
    is_active          smallint     NOT NULL DEFAULT 1 CHECK (is_active IN (0,1)),
    CHECK (login = lower(login))
);

-- FAKT Последнее положение агента: GPS или курсор на карте (manual - только в режиме тестирования)
CREATE TABLE agent_position (
    agent_id           numeric      PRIMARY KEY REFERENCES agent,
    geo_lat            numeric(9,6) NOT NULL,
    geo_lon            numeric(9,6) NOT NULL,
    source             text         NOT NULL CHECK (source IN ('gps','manual')),
    accuracy_m         numeric,
    date_update        timestamp    NOT NULL DEFAULT now()
);
CREATE INDEX ix_agent_position_update ON agent_position (date_update);

-- FAKT Публикация доступности поставщика: свободное место на время, маршрут водителя А -> Б.
-- Для маршрута сервер считает route_polyline и route_length_m по дорогам OSM (GraphHopper).
CREATE SEQUENCE provider_slot_seq START 100;
CREATE TABLE provider_slot (
    slot_id            numeric      PRIMARY KEY DEFAULT nextval('provider_slot_seq'),
    agent_id           numeric      NOT NULL REFERENCES agent,
    service_id         numeric      NOT NULL REFERENCES service,
    geo_lat            numeric(9,6) NOT NULL,             -- точка места / точка А маршрута
    geo_lon            numeric(9,6) NOT NULL,
    radius_m           numeric,                           -- радиус точки / ширина коридора rA
    geo_to_lat         numeric(9,6),                      -- точка Б (такси)
    geo_to_lon         numeric(9,6),
    radius_to_m        numeric,                           -- радиус rB вокруг Б
    route_polyline     text,                              -- encoded polyline маршрута А -> Б
    route_length_m     numeric,
    period_start       timestamp    NOT NULL,
    period_end         timestamp    NOT NULL,
    price_min          numeric(12,2),
    slot_status        text         NOT NULL DEFAULT 'active' CHECK (slot_status IN ('active','closed','cancelled')),
    date_creation      timestamp    NOT NULL DEFAULT now(),
    CHECK (period_end > period_start),
    CHECK ((geo_to_lat IS NULL) = (geo_to_lon IS NULL))
);
CREATE INDEX ix_provider_slot_search ON provider_slot (service_id, slot_status, period_start);

-- ---------------------------------------------------------------------
-- 4. ПОДПИСКИ И РЕФЕРАЛЫ
-- ---------------------------------------------------------------------

-- FAKT Период подписки (пробный или платный)
CREATE TABLE agent_subscription (
    subscription_id    numeric      PRIMARY KEY,
    agent_id           numeric      NOT NULL REFERENCES agent,
    package_id         numeric      NOT NULL REFERENCES service_package,
    is_trial           smallint     NOT NULL DEFAULT 0 CHECK (is_trial IN (0,1)),
    date_creation      timestamp    NOT NULL DEFAULT now(),
    date_start         timestamp    NOT NULL,
    date_end           timestamp    NOT NULL,
    is_active          smallint     NOT NULL DEFAULT 1 CHECK (is_active IN (0,1))
);

-- Ознакомительный период - один на агента
CREATE UNIQUE INDEX ux_agent_subscription_trial ON agent_subscription (agent_id) WHERE is_trial = 1;

-- FAKT Оплаты абонплаты
CREATE TABLE subscription_payment (
    payment_id         numeric      PRIMARY KEY,
    subscription_id    numeric      NOT NULL REFERENCES agent_subscription,
    agent_id           numeric      NOT NULL REFERENCES agent,
    amount             numeric(12,2) NOT NULL,
    payment_status     text         NOT NULL CHECK (payment_status IN ('paid','failed','refund')),
    date_creation      timestamp    NOT NULL DEFAULT now()
);

-- FAKT Реферальное вознаграждение (оформляется как оплата рекламных услуг агента).
-- Начисляется после окончания оплаченного периода: pending -> accrued -> paid_out (выплачено на карту
-- агента со счета платформы); при возврате оплаты до конца периода - cancelled.
CREATE TABLE referral_reward (
    referral_reward_id   numeric     PRIMARY KEY,
    payment_id           numeric     NOT NULL REFERENCES subscription_payment,
    beneficiary_agent_id numeric     NOT NULL REFERENCES agent,
    source_agent_id      numeric     NOT NULL REFERENCES agent,
    referral_level_id    numeric     NOT NULL REFERENCES dictionary,
    reward_percent       numeric(5,2) NOT NULL,
    amount               numeric(12,2) NOT NULL,
    reward_status        text        NOT NULL CHECK (reward_status IN ('pending','accrued','paid_out','cancelled')),
    date_accrual         date        NOT NULL,            -- окончание оплаченного периода
    date_creation        timestamp   NOT NULL DEFAULT now(),
    date_payout          timestamp,                       -- когда выплачено на карту агента
    payout_ref           text,                            -- id операции выплаты у платежного провайдера
    UNIQUE (payment_id, beneficiary_agent_id),
    CHECK ((reward_status = 'paid_out') = (date_payout IS NOT NULL))
);

-- ---------------------------------------------------------------------
-- 5. СДЕЛКИ И УВЕДОМЛЕНИЯ
-- ---------------------------------------------------------------------

-- Номера фактов выдают последовательности (П-3): два клиента API не получат один номер.
CREATE SEQUENCE deal_seq START 100;
CREATE SEQUENCE deal_offer_seq START 100;
CREATE SEQUENCE deal_attribute_seq START 100;
CREATE SEQUENCE deal_status_log_seq START 100;
CREATE SEQUENCE notification_seq START 100;
CREATE SEQUENCE agent_sanction_seq START 100;

-- FAKT Сделка (запрос клиента). service_id - любой узел дерева (обычно вариант услуги).
-- Статус меняется только по таблице переходов DEAL_TRANSITION (триггер trg_deal_status).
CREATE TABLE deal (
    deal_id              numeric     PRIMARY KEY DEFAULT nextval('deal_seq'),
    service_id           numeric     NOT NULL REFERENCES service,
    client_agent_id      numeric     NOT NULL REFERENCES agent,
    provider_agent_id    numeric     REFERENCES agent,
    slot_id              numeric     REFERENCES provider_slot,  -- запрос создан из публикации поставщика
    deal_status_id       numeric     NOT NULL REFERENCES dictionary,
    payment_method_id    numeric     NOT NULL REFERENCES dictionary,  -- сейчас DIRECT
    geo_lat              numeric(9,6),                      -- точка клиента / точка подачи П
    geo_lon              numeric(9,6),
    zone_radius_m        numeric,
    geo_to_lat           numeric(9,6),                      -- точка назначения Н (geo_mode = route)
    geo_to_lon           numeric(9,6),
    route_length_m       numeric,                           -- длина П -> Н по дорогам (считает сервер)
    period_start         timestamp   NOT NULL,
    period_end           timestamp   NOT NULL,
    price_forecast_min   numeric(12,2),
    price_forecast_max   numeric(12,2),
    client_price         numeric(12,2),                     -- "Моя цена" клиента
    provider_rating_min  numeric(3,2),
    price_provider       numeric(12,2),
    fee_amount           numeric(12,2),
    price_total          numeric(12,2),
    close_secret         text,                              -- секрет кода закрытия, создается при AGREED
    date_creation        timestamp   NOT NULL DEFAULT now(),
    date_agreed          timestamp,
    date_close           timestamp,
    CHECK (period_end > period_start),
    CHECK (date_close IS NULL OR date_close >= date_creation)
);
CREATE INDEX ix_deal_status_period ON deal (deal_status_id, period_start);

-- FAKT Отклик поставщика. offer_status: active - ждет выбора, selected - выбран клиентом,
-- declined - выбран другой, withdrawn - снят (место занято другой сделкой), expired - сделка истекла
CREATE TABLE deal_offer (
    deal_offer_id      numeric      PRIMARY KEY DEFAULT nextval('deal_offer_seq'),
    deal_id            numeric      NOT NULL REFERENCES deal,
    provider_agent_id  numeric      NOT NULL REFERENCES agent,
    offer_price        numeric(12,2) NOT NULL,
    offer_mode         text         NOT NULL CHECK (offer_mode IN ('auto','manual')),
    geo_lat            numeric(9,6),                      -- точка предложения (место, машина)
    geo_lon            numeric(9,6),
    offer_status       text         NOT NULL DEFAULT 'active'
                       CHECK (offer_status IN ('active','selected','declined','withdrawn','expired')),
    spot_number        text,                              -- номер места (частная парковка)
    date_creation      timestamp    NOT NULL DEFAULT now(),
    UNIQUE (deal_id, provider_agent_id)
);
CREATE UNIQUE INDEX uq_deal_offer_selected ON deal_offer (deal_id) WHERE offer_status = 'selected';
CREATE INDEX ix_deal_offer_provider ON deal_offer (provider_agent_id, offer_status);

-- FAKT Поля запроса (deal_offer_id NULL) и отклика - по service_attribute.attr_scope
CREATE TABLE deal_attribute (
    deal_attribute_id  numeric      PRIMARY KEY DEFAULT nextval('deal_attribute_seq'),
    deal_id            numeric      NOT NULL REFERENCES deal,
    deal_offer_id      numeric      REFERENCES deal_offer,
    attribute_id       numeric      NOT NULL REFERENCES attribute,
    attribute_value    text         NOT NULL,
    date_creation      timestamp    NOT NULL DEFAULT now(),
    UNIQUE NULLS NOT DISTINCT (deal_id, deal_offer_id, attribute_id)
);

-- FAKT История статусов сделки. Пишет только триггер trg_deal_status_log.
CREATE TABLE deal_status_log (
    deal_status_log_id numeric      PRIMARY KEY DEFAULT nextval('deal_status_log_seq'),
    deal_id            numeric      NOT NULL REFERENCES deal,
    deal_status_id     numeric      NOT NULL REFERENCES dictionary,
    agent_id           numeric      REFERENCES agent,      -- NULL = система
    cancel_reason_id   numeric      REFERENCES dictionary,
    comment            text,
    date_creation      timestamp    NOT NULL DEFAULT now()
);

-- FAKT Баллы и отзывы сторон
CREATE TABLE deal_review (
    deal_id            numeric      NOT NULL REFERENCES deal,
    from_agent_id      numeric      NOT NULL REFERENCES agent,
    to_agent_id        numeric      NOT NULL REFERENCES agent,
    to_role_id         numeric      NOT NULL REFERENCES dictionary,
    score              numeric(2,1) NOT NULL CHECK (score BETWEEN 1 AND 5),
    comment            text,
    date_creation      timestamp    NOT NULL DEFAULT now(),
    PRIMARY KEY (deal_id, from_agent_id)
);

-- FAKT Уведомления с историей отправки и прочтения
CREATE TABLE notification (
    notification_id    numeric      PRIMARY KEY DEFAULT nextval('notification_seq'),
    agent_id           numeric      NOT NULL REFERENCES agent,
    deal_id            numeric      REFERENCES deal,
    notify_type_id     numeric      NOT NULL REFERENCES dictionary,
    channel            text         NOT NULL CHECK (channel IN ('push','sms','email','in_app')),
    payload            jsonb,
    date_creation      timestamp    NOT NULL DEFAULT now(),
    date_sent          timestamp,
    date_read          timestamp
);
CREATE INDEX ix_notification_agent ON notification (agent_id, notification_id);

-- ---------------------------------------------------------------------
-- 6. РЕЙТИНГ И САНКЦИИ
-- ---------------------------------------------------------------------

-- FAKT Рейтинг по дням в разрезе роли и сервиса (узел node_type = 'service')
CREATE TABLE rating_history (
    agent_id           numeric      NOT NULL REFERENCES agent,
    role_id            numeric      NOT NULL REFERENCES dictionary,
    service_id         numeric      NOT NULL REFERENCES service,
    rating_date        date         NOT NULL,
    system_rating      numeric(3,2) NOT NULL,
    review_rating      numeric(3,2) NOT NULL,
    total_rating       numeric(3,2) NOT NULL,             -- веса: rating_weight_* в service_setting
    deals_closed       numeric      NOT NULL DEFAULT 0,
    deals_cancelled    numeric      NOT NULL DEFAULT 0,
    PRIMARY KEY (agent_id, role_id, service_id, rating_date)
);

-- FAKT Санкции за фрод: предупреждение, понижение рейтинга, блокировка (1 неделя, 2 месяц, 3 год)
CREATE TABLE agent_sanction (
    sanction_id        numeric      PRIMARY KEY DEFAULT nextval('agent_sanction_seq'),
    agent_id           numeric      NOT NULL REFERENCES agent,
    deal_id            numeric      REFERENCES deal,
    reason_id          numeric      NOT NULL REFERENCES dictionary,
    action             text         NOT NULL CHECK (action IN ('warning','rating_down','block')),
    block_level        smallint     CHECK (block_level IN (1,2,3)),
    date_creation      timestamp    NOT NULL DEFAULT now(),
    date_start         timestamp,
    date_end           timestamp,
    is_active          smallint     NOT NULL DEFAULT 1 CHECK (is_active IN (0,1)),
    CHECK (action <> 'block' OR (block_level IS NOT NULL AND date_start IS NOT NULL AND date_end IS NOT NULL))
);

-- ---------------------------------------------------------------------
-- 7. ФУНКЦИИ
-- ---------------------------------------------------------------------

-- Проверка типа значения общего справочника (ОВ-22): внешний ключ на dictionary проверяет
-- только наличие строки, триггер - что dict_type совпадает с ожидаемым для колонки.
-- Аргументы триггера - пары: имя колонки, ожидаемый dict_type.
CREATE FUNCTION check_dict_type()
RETURNS trigger LANGUAGE plpgsql SET search_path = mp_service AS $$
    -- Что делает: Триггер: проверяет, что значение общего справочника в колонке имеет ожидаемый dict_type (ОВ-22).
    -- Методика: Аргументы триггера - пары (колонка, тип); значение колонки берется из to_jsonb(NEW), тип ищется в dictionary; несовпадение - исключение.
DECLARE
    i        int;
    v_value  text;
    v_type   text;
BEGIN
    FOR i IN 0 .. TG_NARGS - 1 BY 2 LOOP
        v_value := to_jsonb(NEW) ->> TG_ARGV[i];
        IF v_value IS NOT NULL THEN
            SELECT dict_type INTO v_type FROM dictionary WHERE dict_id = v_value::numeric;
            IF v_type IS DISTINCT FROM TG_ARGV[i + 1] THEN
                RAISE EXCEPTION '%.%: значение % имеет тип %, ожидается %',
                    TG_TABLE_NAME, TG_ARGV[i], v_value, coalesce(v_type, '-'), TG_ARGV[i + 1];
            END IF;
        END IF;
    END LOOP;
    RETURN NEW;
END
$$;

CREATE TRIGGER trg_service_package_dict BEFORE INSERT OR UPDATE ON service_package
    FOR EACH ROW EXECUTE FUNCTION check_dict_type('role_id','ROLE');
CREATE TRIGGER trg_agent_role_dict BEFORE INSERT OR UPDATE ON agent_role
    FOR EACH ROW EXECUTE FUNCTION check_dict_type('role_id','ROLE');
CREATE TRIGGER trg_referral_reward_dict BEFORE INSERT OR UPDATE ON referral_reward
    FOR EACH ROW EXECUTE FUNCTION check_dict_type('referral_level_id','REFERRAL_LEVEL');
CREATE TRIGGER trg_deal_dict BEFORE INSERT OR UPDATE ON deal
    FOR EACH ROW EXECUTE FUNCTION check_dict_type('deal_status_id','DEAL_STATUS','payment_method_id','PAYMENT_METHOD');
CREATE TRIGGER trg_deal_status_log_dict BEFORE INSERT OR UPDATE ON deal_status_log
    FOR EACH ROW EXECUTE FUNCTION check_dict_type('deal_status_id','DEAL_STATUS','cancel_reason_id','CANCEL_REASON');
CREATE TRIGGER trg_deal_review_dict BEFORE INSERT OR UPDATE ON deal_review
    FOR EACH ROW EXECUTE FUNCTION check_dict_type('to_role_id','ROLE');
CREATE TRIGGER trg_notification_dict BEFORE INSERT OR UPDATE ON notification
    FOR EACH ROW EXECUTE FUNCTION check_dict_type('notify_type_id','NOTIFY_TYPE');
CREATE TRIGGER trg_rating_history_dict BEFORE INSERT OR UPDATE ON rating_history
    FOR EACH ROW EXECUTE FUNCTION check_dict_type('role_id','ROLE');
CREATE TRIGGER trg_agent_sanction_dict BEFORE INSERT OR UPDATE ON agent_sanction
    FOR EACH ROW EXECUTE FUNCTION check_dict_type('reason_id','SANCTION_REASON');

-- Id значения общего справочника по типу и коду
CREATE FUNCTION dict_id(p_type text, p_code text)
RETURNS numeric LANGUAGE sql STABLE SET search_path = mp_service AS $$
    -- Что делает: Возвращает id значения общего справочника по типу и коду.
    -- Методика: Прямой поиск по уникальной паре (dict_type, dict_code).
    SELECT dict_id FROM dictionary WHERE dict_type = p_type AND dict_code = p_code
$$;

-- Текст глоссария на языке агента; без перевода - русский (алгоритм п. 0.5)
CREATE FUNCTION gloss(p_glossary_id numeric, p_lang numeric)
RETURNS text LANGUAGE sql STABLE SET search_path = mp_service AS $$
    -- Что делает: Текст термина глоссария на языке интерфейса агента; если перевода нет - на русском (п. 0.5).
    -- Методика: Ищет glossary_text на языке p_lang, иначе на RUS (interface_lang_id = 1); так строка с непереведённым названием не выпадает из выборки.
    SELECT coalesce(
        (SELECT text_value FROM glossary_text WHERE glossary_id = p_glossary_id AND interface_lang_id = p_lang),
        (SELECT text_value FROM glossary_text WHERE glossary_id = p_glossary_id AND interface_lang_id = 1))
$$;

-- Расстояние между точками в метрах (гаверсинус). При переходе на PostGIS - ST_Distance.
CREATE FUNCTION geo_distance_m(lat1 numeric, lon1 numeric, lat2 numeric, lon2 numeric)
RETURNS numeric LANGUAGE sql IMMUTABLE STRICT AS $$
    -- Что делает: Расстояние между двумя точками по прямой, м.
    -- Методика: Формула гаверсинуса на сфере радиусом 6 371 км, округление до 0,1 м; при переходе на PostGIS - ST_Distance.
    SELECT round((2 * 6371000 * asin(sqrt(
               power(sin(radians(lat2 - lat1) / 2), 2) +
               cos(radians(lat1)) * cos(radians(lat2)) *
               power(sin(radians(lon2 - lon1) / 2), 2))))::numeric, 1)
$$;

-- Ключ сортировки отклика по спецификации 'distance:asc' | 'rating:desc' | 'price:asc'
CREATE FUNCTION offer_sort_key(p_spec text, p_distance numeric, p_rating numeric, p_price numeric)
RETURNS numeric LANGUAGE sql IMMUTABLE AS $$
    -- Что делает: Ключ сортировки отклика по спецификации "поле:направление" (distance/rating/price, asc/desc).
    -- Методика: Берет значение нужного поля и умножает на -1 для desc, чтобы все ключи сортировать по возрастанию.
    SELECT (CASE split_part(p_spec, ':', 1)
                WHEN 'distance' THEN p_distance
                WHEN 'rating'   THEN p_rating
                WHEN 'price'    THEN p_price
            END) * CASE split_part(p_spec, ':', 2) WHEN 'desc' THEN -1 ELSE 1 END
$$;

-- ---------------------------------------------------------------------
-- 8. ПРЕДСТАВЛЕНИЯ
-- ---------------------------------------------------------------------

-- Дерево услуг: каждый узел со всеми предками (depth 0 = сам узел)
CREATE VIEW v_service_tree AS
WITH RECURSIVE t AS (
    SELECT service_id, service_id AS ancestor_id, 0 AS depth FROM service
    UNION ALL
    SELECT t.service_id, s.parent_service_id, t.depth + 1
    FROM t JOIN service s ON s.service_id = t.ancestor_id
    WHERE s.parent_service_id IS NOT NULL
)
SELECT service_id, ancestor_id, depth FROM t;

-- Значение настройки для узла: ближайший узел вверх по дереву, затем платформа.
-- Если передан рейтинг - сначала строки с подходящим диапазоном рейтинга.
CREATE FUNCTION get_setting(p_service_id numeric, p_code text, p_rating numeric DEFAULT NULL)
RETURNS text LANGUAGE sql STABLE SET search_path = mp_service AS $$
    -- Что делает: Значение настройки сервиса для узла дерева с учетом наследования и рейтинга поставщика.
    -- Методика: Ищет строки настройки на узле и его предках, затем на платформе; ближайший узел побеждает, при переданном рейтинге сначала строки с подходящим диапазоном.
    SELECT ss.setting_value
    FROM service_setting ss
    LEFT JOIN v_service_tree t ON t.service_id = p_service_id AND t.ancestor_id = ss.service_id
    WHERE ss.setting_code = p_code
      AND ss.is_active = 1
      AND (ss.service_id IS NULL OR t.ancestor_id IS NOT NULL)
      AND CASE WHEN p_rating IS NULL THEN ss.rating_min IS NULL AND ss.rating_max IS NULL
               ELSE (ss.rating_min IS NULL OR p_rating >= ss.rating_min)
                AND (ss.rating_max IS NULL OR p_rating <  ss.rating_max) END
    ORDER BY coalesce(t.depth, 1000), (ss.rating_min IS NULL AND ss.rating_max IS NULL)
    LIMIT 1
$$;

-- Узел уровня "сервис" для любого узла (рейтинг и отчеты ведутся по нему)
CREATE FUNCTION service_base_id(p_service_id numeric)
RETURNS numeric LANGUAGE sql STABLE SET search_path = mp_service AS $$
    -- Что делает: Узел уровня "сервис" для любого узла дерева (по нему ведутся рейтинг и отчеты).
    -- Методика: Поднимается по v_service_tree до ближайшего предка типа service или system.
    SELECT t.ancestor_id
    FROM v_service_tree t JOIN service s ON s.service_id = t.ancestor_id
    WHERE t.service_id = p_service_id AND s.node_type IN ('service','system')
    ORDER BY t.depth
    LIMIT 1
$$;

-- Поиск услуг: название, путь в дереве, синонимы, пиктограмма (по языку)
CREATE VIEW v_service_search AS
SELECT s.service_id,
       s.node_type,
       s.parent_service_id,
       gt.interface_lang_id,
       gt.text_value AS service_name,
       p.service_path,
       lower(concat_ws(' ', p.service_path, gt.search_text)) AS search_text,
       coalesce(g.icon_id, pi.icon_id) AS icon_id     -- у варианта без иконки берется иконка предка
FROM service s
JOIN glossary g       ON g.glossary_id = s.glossary_id
JOIN glossary_text gt ON gt.glossary_id = s.glossary_id AND gt.is_active = 1
CROSS JOIN LATERAL (
    SELECT string_agg(at.text_value, ' / ' ORDER BY t.depth DESC) AS service_path
    FROM v_service_tree t
    JOIN service a        ON a.service_id = t.ancestor_id
    JOIN glossary_text at ON at.glossary_id = a.glossary_id AND at.interface_lang_id = gt.interface_lang_id
    WHERE t.service_id = s.service_id
) p
LEFT JOIN LATERAL (
    SELECT ag.icon_id
    FROM v_service_tree t
    JOIN service a  ON a.service_id = t.ancestor_id
    JOIN glossary ag ON ag.glossary_id = a.glossary_id
    WHERE t.service_id = s.service_id AND t.depth > 0 AND ag.icon_id IS NOT NULL
    ORDER BY t.depth
    LIMIT 1
) pi ON true
WHERE s.node_type <> 'system' AND s.is_active = 1;

-- Атрибуты узла с учетом наследования (ближайший узел побеждает)
CREATE VIEW v_service_attribute_effective AS
SELECT DISTINCT ON (t.service_id, sa.attribute_id)
       t.service_id, sa.attribute_id, sa.attr_scope, sa.is_required, sa.show_stage, sa.sort_order,
       t.ancestor_id AS defined_at_service_id
FROM v_service_tree t
JOIN service_attribute sa ON sa.service_id = t.ancestor_id AND sa.is_active = 1
ORDER BY t.service_id, sa.attribute_id, t.depth;

-- Атрибуты с подписью и пиктограммой на языке (для вывода карточек и форм)
CREATE VIEW v_attribute_label AS
SELECT a.attribute_id, a.attribute_type, a.parent_attribute_id, a.sort_order,
       gt.interface_lang_id, gt.text_value AS label, g.icon_id, i.icon_code
FROM attribute a
JOIN glossary g       ON g.glossary_id = a.glossary_id
JOIN glossary_text gt ON gt.glossary_id = a.glossary_id
LEFT JOIN icon i      ON i.icon_id = g.icon_id;

-- Сервисы, доступные агенту по действующей подписке (категория в пакете = все узлы под ней).
-- Субагент: сервисы, назначенные компанией на действующий период, и только из пакета компании.
CREATE VIEW v_agent_available_service AS
WITH own AS (
    SELECT DISTINCT sub.agent_id, t.service_id
    FROM agent_subscription sub
    JOIN package_service ps ON ps.package_id = sub.package_id AND ps.is_active = 1
    JOIN v_service_tree t   ON t.ancestor_id = ps.service_id
    WHERE sub.is_active = 1 AND now() BETWEEN sub.date_start AND sub.date_end
)
SELECT agent_id, service_id FROM own
UNION
SELECT ags.agent_id, t.service_id
FROM agent_service ags
JOIN agent_role ar    ON ar.agent_id = ags.agent_id AND ar.role_id = dict_id('ROLE','SUBAGENT')
                     AND ar.is_active = 1 AND ar.parent_agent_id = ags.assigned_by_agent_id
JOIN v_service_tree t ON t.ancestor_id = ags.service_id
JOIN own c            ON c.agent_id = ar.parent_agent_id AND c.service_id = t.service_id
WHERE ags.is_active = 1 AND now() >= ags.date_start AND now() <= coalesce(ags.date_end, 'infinity');

-- Начисление абонплаты за период: цена пакета + субагенты сверх включенных
CREATE VIEW v_subscription_charge AS
SELECT sub.subscription_id, sub.agent_id, sub.package_id,
       sp.price, sp.included_subagents, sp.extra_subagent_price,
       coalesce(sa.cnt, 0) AS subagents,
       sp.price + greatest(coalesce(sa.cnt, 0) - sp.included_subagents, 0) * sp.extra_subagent_price AS charge
FROM agent_subscription sub
JOIN service_package sp ON sp.package_id = sub.package_id
LEFT JOIN LATERAL (
    SELECT count(*) AS cnt FROM agent_role ar
    WHERE ar.parent_agent_id = sub.agent_id AND ar.role_id = dict_id('ROLE','SUBAGENT') AND ar.is_active = 1
) sa ON true
WHERE sub.is_trial = 0;

-- Доходы и расходы агента по дням в разрезе роли, сервиса и источника
--   income : закрытые сделки поставщика, начисленные рефералы
--   expense: закрытые сделки клиента, оплаты абонплаты
CREATE VIEW v_agent_finance_daily AS
SELECT d.provider_agent_id AS agent_id, dict_id('ROLE','PROVIDER') AS role_id,
       service_base_id(d.service_id) AS service_id, 'deal'::text AS source, 'income'::text AS direction,
       d.date_close::date AS fin_date, count(*) AS cnt, sum(d.price_provider) AS amount
FROM deal d WHERE d.deal_status_id = dict_id('DEAL_STATUS','CLOSED')
GROUP BY 1, 2, 3, 6
UNION ALL
SELECT d.client_agent_id, dict_id('ROLE','CLIENT'), service_base_id(d.service_id), 'deal', 'expense',
       d.date_close::date, count(*), sum(d.price_total)
FROM deal d WHERE d.deal_status_id = dict_id('DEAL_STATUS','CLOSED')
GROUP BY 1, 2, 3, 6
UNION ALL
SELECT rr.beneficiary_agent_id, NULL, NULL, 'referral', 'income', rr.date_accrual, count(*), sum(rr.amount)
FROM referral_reward rr WHERE rr.reward_status IN ('accrued','paid_out')
GROUP BY 1, 6
UNION ALL
SELECT p.agent_id, NULL, NULL, 'subscription', 'expense', p.date_creation::date, count(*), sum(p.amount)
FROM subscription_payment p WHERE p.payment_status = 'paid'
GROUP BY 1, 6;

-- То же для компании в разрезе субагентов
CREATE VIEW v_company_finance_daily AS
SELECT ar.parent_agent_id AS company_agent_id, f.agent_id AS subagent_id,
       f.role_id, f.service_id, f.source, f.direction, f.fin_date, f.cnt, f.amount
FROM v_agent_finance_daily f
JOIN agent_role ar ON ar.agent_id = f.agent_id
                  AND ar.role_id = dict_id('ROLE','SUBAGENT')
                  AND ar.is_active = 1;

-- Отклики и ссылки на маршрут (v_deal_offer_list, v_deal_offer_route) - в разделе 9:
-- они используют функции правил сделки.

-- ---------------------------------------------------------------------
-- 9. ПРАВИЛА СДЕЛКИ (v0.7): подбор, переходы статусов, отмена, антифрод, сроки
-- Пункты в комментариях - нумерация docs/v0.7/01_deal_algorithm.md
-- ---------------------------------------------------------------------

-- Загрузка тестовых данных: триггеры процесса не проверяют и не логируют
CREATE FUNCTION is_load_mode()
RETURNS boolean LANGUAGE sql STABLE AS $$
    -- Что делает: Признак загрузки тестовых данных (mp.load_mode = on).
    -- Методика: Читает параметр сессии; триггеры процесса в этом режиме не проверяют и не логируют.
    SELECT coalesce(current_setting('mp.load_mode', true), '') = 'on'
$$;

-- Агент текущей сессии API; NULL - система (плановые задания)
CREATE FUNCTION session_agent_id()
RETURNS numeric LANGUAGE sql STABLE AS $$
    -- Что делает: Агент, от имени которого действует текущая сессия API; NULL - система.
    -- Методика: Читает параметр сессии mp.agent_id, который задает сервер на каждый запрос.
    SELECT nullif(current_setting('mp.agent_id', true), '')::numeric
$$;

-- Вход по логину и паролю: id агента или NULL. Логин без учета регистра; фиксирует дату входа.
CREATE FUNCTION agent_login_check(p_login text, p_password text)
RETURNS numeric LANGUAGE plpgsql SET search_path = mp_service AS $$
    -- Что делает: Проверка логина и пароля: возвращает id агента или NULL, фиксирует дату входа.
    -- Методика: Логин приводится к нижнему регистру; пароль сравнивается с хешем bcrypt через crypt() из pgcrypto; учитываются только активные вход и агент.
DECLARE
    v_agent numeric;
BEGIN
    SELECT l.agent_id INTO v_agent
    FROM agent_login l
    JOIN agent a ON a.agent_id = l.agent_id AND a.is_active = 1
    WHERE l.login = lower(trim(p_login)) AND l.is_active = 1
      AND l.password_hash = public.crypt(p_password, l.password_hash);
    IF v_agent IS NOT NULL THEN
        UPDATE agent_login SET date_last_login = now() WHERE agent_id = v_agent;
    END IF;
    RETURN v_agent;
END
$$;

-- Псевдоним агента (атрибут 16, виден всем)
CREATE FUNCTION agent_pseudonym(p_agent_id numeric)
RETURNS text LANGUAGE sql STABLE SET search_path = mp_service AS $$
    -- Что делает: Псевдоним агента (атрибут 16, виден всем).
    -- Методика: Последнее активное значение атрибута из истории agent_attribute.
    SELECT aa.attribute_value FROM agent_attribute aa
    WHERE aa.agent_id = p_agent_id AND aa.attribute_id = 16 AND aa.is_active = 1
    ORDER BY aa.date_start DESC LIMIT 1
$$;

-- Последний итоговый рейтинг агента в роли по сервису на дату.
-- Нет истории по сервису - rating_start (В-4), в интерфейсе пометка "новичок" (agent_has_rating).
CREATE FUNCTION agent_rating(p_agent_id numeric, p_role_code text, p_service_id numeric,
                             p_on date DEFAULT current_date)
RETURNS numeric LANGUAGE sql STABLE SET search_path = mp_service AS $$
    -- Что делает: Итоговый рейтинг агента в роли по сервису на дату; без истории - rating_start (В-4).
    -- Методика: Последняя запись rating_history по узлу уровня "сервис" не позже даты; если ее нет - настройка rating_start.
    SELECT coalesce(
        (SELECT rh.total_rating FROM rating_history rh
         WHERE rh.agent_id = p_agent_id AND rh.role_id = dict_id('ROLE', p_role_code)
           AND rh.service_id = service_base_id(p_service_id) AND rh.rating_date <= p_on
         ORDER BY rh.rating_date DESC LIMIT 1),
        get_setting(p_service_id, 'rating_start')::numeric)
$$;

CREATE FUNCTION agent_has_rating(p_agent_id numeric, p_role_code text, p_service_id numeric)
RETURNS boolean LANGUAGE sql STABLE SET search_path = mp_service AS $$
    -- Что делает: Есть ли у агента история рейтинга в роли по сервису (иначе пометка "новичок").
    -- Методика: Проверка существования записи rating_history по узлу уровня "сервис".
    SELECT EXISTS (SELECT 1 FROM rating_history rh
                   WHERE rh.agent_id = p_agent_id AND rh.role_id = dict_id('ROLE', p_role_code)
                     AND rh.service_id = service_base_id(p_service_id))
$$;

-- Подключенный узел поставщика покрывает узел сделки: предок по дереву
-- или вариант из списка принимаемых классов (accepts_variants: Комфорт берет Эконом)
CREATE FUNCTION service_covers(p_provider_service_id numeric, p_deal_service_id numeric)
RETURNS boolean LANGUAGE sql STABLE SET search_path = mp_service AS $$
    -- Что делает: Подключенный узел поставщика покрывает узел сделки.
    -- Методика: Да, если узел поставщика - предок узла сделки по дереву, или узел сделки входит в настройку accepts_variants (классы авто).
    SELECT EXISTS (SELECT 1 FROM v_service_tree t
                   WHERE t.service_id = p_deal_service_id AND t.ancestor_id = p_provider_service_id)
        OR p_deal_service_id::text = ANY (string_to_array(
               coalesce(get_setting(p_provider_service_id, 'accepts_variants'), ''), ','))
$$;

-- Период сделки внутри рабочего времени; окно может переходить через полночь (П-6)
CREATE FUNCTION period_in_work_time(p_from time, p_to time, p_start timestamp, p_end timestamp)
RETURNS boolean LANGUAGE sql IMMUTABLE AS $$
    -- Что делает: Период сделки помещается в рабочее время; окно может переходить через полночь (П-6).
    -- Методика: Строит интервалы рабочего окна на день начала периода и на предыдущий день (для ночных окон) и проверяет вхождение tsrange.
    SELECT p_from IS NULL OR p_to IS NULL OR EXISTS (
        SELECT 1 FROM (VALUES (0), (-1)) d(shift)
        WHERE tsrange(p_start, p_end, '[]') <@ tsrange(
                  (p_start::date + d.shift) + p_from,
                  (p_start::date + d.shift) + p_to
                      + CASE WHEN p_to <= p_from THEN interval '1 day' ELSE interval '0' END,
                  '[]'))
$$;

-- Сделки, по которым агент еще не выставил баллы: после закрытия - обе стороны,
-- после односторонней отмены - пострадавшая сторона (п. 5.6, В-11)
CREATE FUNCTION agent_pending_reviews(p_agent_id numeric)
RETURNS bigint LANGUAGE sql STABLE SET search_path = mp_service AS $$
    -- Что делает: Число сделок, по которым агент еще не выставил баллы (п. 5.6, В-11).
    -- Методика: Закрытые сделки - для обеих сторон, отмены со штрафом - для пострадавшей стороны; без записи deal_review от агента.
    SELECT count(*)
    FROM deal d
    JOIN dictionary s ON s.dict_id = d.deal_status_id
    WHERE d.provider_agent_id IS NOT NULL
      AND p_agent_id IN (d.client_agent_id, d.provider_agent_id)
      AND (s.dict_code = 'CLOSED'
           OR (s.dict_code = 'CANCEL_CLIENT'   AND p_agent_id = d.provider_agent_id)
           OR (s.dict_code = 'CANCEL_PROVIDER' AND p_agent_id = d.client_agent_id))
      AND NOT EXISTS (SELECT 1 FROM deal_review r WHERE r.deal_id = d.deal_id AND r.from_agent_id = p_agent_id)
$$;

-- Сколько сделок поставщик может держать одновременно по сервису (п. 2.8)
CREATE FUNCTION provider_limit(p_agent_id numeric, p_service_id numeric)
RETURNS numeric LANGUAGE sql STABLE SET search_path = mp_service AS $$
    -- Что делает: Сколько сделок поставщик может держать одновременно по сервису (п. 2.8).
    -- Методика: Емкость из agent_service, если задана, иначе настройка max_parallel_deals, иначе 1.
    SELECT coalesce(
        (SELECT ags.capacity FROM agent_service ags
         WHERE ags.agent_id = p_agent_id AND ags.is_active = 1 AND service_covers(ags.service_id, p_service_id)
           AND ags.capacity IS NOT NULL
         ORDER BY ags.capacity DESC LIMIT 1),
        get_setting(p_service_id, 'max_parallel_deals')::numeric, 1)
$$;

-- Сколько согласованных сделок поставщика того же сервиса пересекается с периодом
CREATE FUNCTION provider_busy(p_agent_id numeric, p_service_id numeric, p_start timestamp, p_end timestamp,
                              p_exclude_deal_id numeric DEFAULT NULL)
RETURNS bigint LANGUAGE sql STABLE SET search_path = mp_service AS $$
    -- Что делает: Сколько согласованных сделок поставщика того же сервиса пересекается с периодом.
    -- Методика: Считает сделки в AGREED/CONTACT с тем же узлом уровня "сервис" и пересечением tsrange, кроме исключаемой.
    SELECT count(*)
    FROM deal x JOIN dictionary s ON s.dict_id = x.deal_status_id
    WHERE x.provider_agent_id = p_agent_id AND s.dict_code IN ('AGREED','CONTACT')
      AND x.deal_id IS DISTINCT FROM p_exclude_deal_id
      AND service_base_id(x.service_id) = service_base_id(p_service_id)
      AND tsrange(x.period_start, x.period_end) && tsrange(p_start, p_end)
$$;

-- Может ли поставщик откликнуться на сделку: условия п. 2.1-2.9 алгоритма.
-- p_lat/p_lon - точка предложения, если уже известна (иначе - публикация или точка из agent_service).
-- Маршрут такси (коридор ±rA, радиус rB, направление) проверяет API-сервер по дорогам OSM.
-- reason: NO_DEAL, DEAL_NOT_OPEN, OWN_DEAL, REVIEW_REQUIRED, NO_SERVICE_ACCESS, BLOCKED, PROVIDER_RATING,
--         CLIENT_RATING, NO_SLOT, WORK_TIME, OUT_OF_CLIENT_ZONE, OUT_OF_PROVIDER_ZONE, PRICE_RANGE, BUSY
CREATE FUNCTION provider_can_offer(p_deal_id numeric, p_agent_id numeric,
                                   p_lat numeric DEFAULT NULL, p_lon numeric DEFAULT NULL,
                                   OUT ok boolean, OUT reason text)
LANGUAGE plpgsql STABLE SET search_path = mp_service AS $$
    -- Что делает: Может ли поставщик откликнуться на сделку: все условия п. 2.1-2.9 алгоритма; возвращает ok и код причины.
    -- Методика: Последовательно проверяет: статус сделки, не сам клиент, баллы, доступ к сервису (дерево, классы, подписка), блокировку, рейтинги, окно публикации или рабочее время, гео по place_mode, цену, лимит одновременных сделок; первая невыполненная проверка дает reason. Маршрут такси проверяет сервер.
DECLARE
    d        deal%ROWTYPE;
    v_status text;
    v_svc    agent_service%ROWTYPE;
    v_slot   provider_slot%ROWTYPE;
    v_place  text;
    v_geo    text;
    v_lat    numeric;
    v_lon    numeric;
    v_hi     numeric;
    v_lo     numeric;
BEGIN
    ok := false;
    SELECT * INTO d FROM deal WHERE deal_id = p_deal_id;
    IF NOT FOUND THEN reason := 'NO_DEAL'; RETURN; END IF;
    SELECT dict_code INTO v_status FROM dictionary WHERE dict_id = d.deal_status_id;
    IF v_status NOT IN ('MATCHING','OFFERS') THEN reason := 'DEAL_NOT_OPEN'; RETURN; END IF;
    -- 2.9 не сам клиент
    IF p_agent_id = d.client_agent_id THEN reason := 'OWN_DEAL'; RETURN; END IF;
    -- 5.6 баллы по прошлым сделкам
    IF agent_pending_reviews(p_agent_id) > 0 THEN reason := 'REVIEW_REQUIRED'; RETURN; END IF;

    -- 2.6 доступ: сервис подключен (2.1.2 с учетом классов авто) и входит в подписку
    SELECT ags.* INTO v_svc
    FROM agent_service ags
    WHERE ags.agent_id = p_agent_id AND ags.is_active = 1
      AND service_covers(ags.service_id, d.service_id)
      AND EXISTS (SELECT 1 FROM v_agent_available_service a
                  WHERE a.agent_id = p_agent_id AND a.service_id = d.service_id)
    ORDER BY (ags.service_id = d.service_id) DESC, ags.service_id DESC
    LIMIT 1;
    IF NOT FOUND THEN reason := 'NO_SERVICE_ACCESS'; RETURN; END IF;

    -- 2.7 блокировка на период сделки
    IF EXISTS (SELECT 1 FROM agent_sanction s
               WHERE s.agent_id = p_agent_id AND s.action = 'block' AND s.is_active = 1
                 AND tsrange(s.date_start, s.date_end) && tsrange(d.period_start, d.period_end)) THEN
        reason := 'BLOCKED'; RETURN;
    END IF;

    -- 2.4, 2.5 рейтинги
    IF d.provider_rating_min IS NOT NULL
       AND agent_rating(p_agent_id, 'PROVIDER', d.service_id) < d.provider_rating_min THEN
        reason := 'PROVIDER_RATING'; RETURN;
    END IF;
    IF v_svc.client_rating_min IS NOT NULL
       AND agent_rating(d.client_agent_id, 'CLIENT', d.service_id) < v_svc.client_rating_min THEN
        reason := 'CLIENT_RATING'; RETURN;
    END IF;

    v_place := get_setting(d.service_id, 'place_mode');
    v_geo   := get_setting(d.service_id, 'geo_mode');

    -- 2.2 время: окно публикации (запрос из публикации - только к ее поставщику) или рабочее время
    IF coalesce(get_setting(d.service_id, 'slot_required'), '0') = '1' OR d.slot_id IS NOT NULL THEN
        SELECT ps.* INTO v_slot
        FROM provider_slot ps
        WHERE ps.agent_id = p_agent_id AND ps.slot_status = 'active'
          AND service_covers(ps.service_id, d.service_id)
          AND tsrange(d.period_start, d.period_end, '[]') <@ tsrange(ps.period_start, ps.period_end, '[]')
          AND (d.slot_id IS NULL OR d.slot_id = ps.slot_id)
        ORDER BY ps.slot_id DESC
        LIMIT 1;
        IF NOT FOUND THEN reason := 'NO_SLOT'; RETURN; END IF;
    ELSIF NOT period_in_work_time(v_svc.work_time_from, v_svc.work_time_to, d.period_start, d.period_end) THEN
        reason := 'WORK_TIME'; RETURN;
    END IF;

    -- 2.1 гео по place_mode (В-1)
    IF v_geo = 'point' AND v_place <> 'online' AND d.geo_lat IS NOT NULL THEN
        IF v_place = 'provider' THEN
            v_lat := coalesce(p_lat, v_slot.geo_lat, v_svc.geo_lat);
            v_lon := coalesce(p_lon, v_slot.geo_lon, v_svc.geo_lon);
            IF v_lat IS NULL OR geo_distance_m(d.geo_lat, d.geo_lon, v_lat, v_lon) > d.zone_radius_m THEN
                reason := 'OUT_OF_CLIENT_ZONE'; RETURN;
            END IF;
        ELSIF v_svc.geo_lat IS NOT NULL AND v_svc.zone_radius_m IS NOT NULL
              AND geo_distance_m(d.geo_lat, d.geo_lon, v_svc.geo_lat, v_svc.geo_lon) > v_svc.zone_radius_m THEN
            reason := 'OUT_OF_PROVIDER_ZONE'; RETURN;
        END IF;
    END IF;

    -- 2.3 цена: прогноз или цена клиента пересекается с диапазоном поставщика (км - на сервере)
    IF get_setting(d.service_id, 'price_unit') <> 'km' THEN
        v_hi := greatest(d.price_forecast_max, d.client_price);
        v_lo := least(d.price_forecast_min, d.client_price);
        IF (v_svc.price_min IS NOT NULL AND v_hi < v_svc.price_min)
           OR (v_svc.price_max IS NOT NULL AND v_lo > v_svc.price_max) THEN
            reason := 'PRICE_RANGE'; RETURN;
        END IF;
    END IF;

    -- 2.8 лимит одновременных сделок
    IF provider_busy(p_agent_id, d.service_id, d.period_start, d.period_end, d.deal_id)
       >= provider_limit(p_agent_id, d.service_id) THEN
        reason := 'BUSY'; RETURN;
    END IF;

    ok := true;
END
$$;

-- Уведомление агенту в приложение (тексты - в глоссарии типа уведомления)
CREATE FUNCTION notify_agent(p_agent_id numeric, p_deal_id numeric, p_type text, p_payload jsonb DEFAULT '{}')
RETURNS void LANGUAGE sql SET search_path = mp_service AS $$
    -- Что делает: Создает уведомление агенту в приложение.
    -- Методика: Вставка в notification с типом из справочника NOTIFY_TYPE, каналом in_app и временем отправки; пустой получатель пропускается.
    INSERT INTO notification (agent_id, deal_id, notify_type_id, channel, payload, date_sent)
    SELECT p_agent_id, p_deal_id, dict_id('NOTIFY_TYPE', p_type), 'in_app', coalesce(p_payload, '{}'), now()
    WHERE p_agent_id IS NOT NULL
$$;

-- Эпизод фрода и эскалация санкций (п. 6): 1 - предупреждение, 2 - понижение рейтинга,
-- больше двух при подозрительных отзывах И падении выручки - блокировка: неделя, месяц, год.
-- Агенту и компании субагента - уведомление SANCTION.
CREATE FUNCTION register_fraud_episode(p_agent_id numeric, p_reason_code text, p_deal_id numeric DEFAULT NULL,
                                       p_reviews_suspicious boolean DEFAULT false,
                                       p_revenue_drop boolean DEFAULT false)
RETURNS text LANGUAGE plpgsql SET search_path = mp_service AS $$
    -- Что делает: Регистрирует эпизод фрода и применяет санкцию по эскалации (п. 6).
    -- Методика: По числу активных санкций: первый эпизод - предупреждение, далее - понижение рейтинга; больше двух при подозрительных отзывах И падении выручки - блокировка следующего уровня (неделя, месяц, год). Уведомляет агента и компанию субагента.
DECLARE
    v_cnt     bigint;
    v_blocks  bigint;
    v_action  text;
    v_level   smallint;
    v_start   timestamp;
    v_end     timestamp;
    v_company numeric;
    v_payload jsonb;
BEGIN
    SELECT count(*), count(*) FILTER (WHERE action = 'block')
    INTO v_cnt, v_blocks
    FROM agent_sanction WHERE agent_id = p_agent_id AND is_active = 1;

    IF v_cnt = 0 THEN
        v_action := 'warning';
    ELSIF v_cnt >= 2 AND p_reviews_suspicious AND p_revenue_drop THEN
        v_action := 'block';
        v_level  := least(v_blocks + 1, 3);
        v_start  := now();
        v_end    := now() + CASE v_level WHEN 1 THEN interval '7 days'
                                         WHEN 2 THEN interval '1 month'
                                         ELSE interval '1 year' END;
    ELSE
        v_action := 'rating_down';
    END IF;

    INSERT INTO agent_sanction (agent_id, deal_id, reason_id, action, block_level, date_start, date_end)
    VALUES (p_agent_id, p_deal_id, dict_id('SANCTION_REASON', p_reason_code), v_action, v_level, v_start, v_end);

    v_payload := jsonb_build_object('action', v_action, 'reason', p_reason_code, 'block_level', v_level,
                                    'date_end', v_end, 'agent_id', p_agent_id);
    PERFORM notify_agent(p_agent_id, p_deal_id, 'SANCTION', v_payload);

    SELECT ar.parent_agent_id INTO v_company FROM agent_role ar
    WHERE ar.agent_id = p_agent_id AND ar.role_id = dict_id('ROLE','SUBAGENT') AND ar.is_active = 1;
    PERFORM notify_agent(v_company, p_deal_id, 'SANCTION', v_payload);
    RETURN v_action;
END
$$;

-- Проверка перехода статуса по справочнику DEAL_TRANSITION (П-1) и служебные поля.
-- value_text перехода - кто выполняет: client, provider, party (любая сторона), system, any.
-- Причина отмены и комментарий - параметры транзакции mp.cancel_reason_id и mp.status_comment.
CREATE FUNCTION trg_deal_status_check()
RETURNS trigger LANGUAGE plpgsql SET search_path = mp_service AS $$
    -- Что делает: Триггер BEFORE на deal: допустимость перехода статуса и служебные поля (П-1).
    -- Методика: Новая сделка - только INIT; переход ищется в DEAL_TRANSITION, исполнитель сверяется с mp.agent_id; закрытие гео-услуги - только из CONTACT; причина обязательна для отмен со штрафом; при AGREED - дата и секрет кода закрытия; финальный статус - дата закрытия.
DECLARE
    v_from    text;
    v_to      text;
    v_actor   text;
    v_agent   numeric := session_agent_id();
    v_reason  numeric := nullif(current_setting('mp.cancel_reason_id', true), '')::numeric;
    v_comment text    := nullif(current_setting('mp.status_comment', true), '');
BEGIN
    IF is_load_mode() THEN RETURN NEW; END IF;
    SELECT dict_code INTO v_to FROM dictionary WHERE dict_id = NEW.deal_status_id;

    IF TG_OP = 'INSERT' THEN
        IF v_to <> 'INIT' THEN
            RAISE EXCEPTION 'Новая сделка создается в статусе INIT, получен %', v_to;
        END IF;
        RETURN NEW;
    END IF;
    IF NEW.deal_status_id = OLD.deal_status_id THEN RETURN NEW; END IF;

    SELECT dict_code INTO v_from FROM dictionary WHERE dict_id = OLD.deal_status_id;
    SELECT value_text INTO v_actor FROM dictionary
    WHERE dict_type = 'DEAL_TRANSITION' AND is_active = 1
      AND parent_dict_id = OLD.deal_status_id AND value_num = NEW.deal_status_id;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Переход % -> % не разрешен', v_from, v_to;
    END IF;

    IF (v_actor = 'system'   AND v_agent IS NOT NULL)
    OR (v_actor = 'client'   AND v_agent IS DISTINCT FROM OLD.client_agent_id)
    OR (v_actor = 'provider' AND v_agent IS DISTINCT FROM OLD.provider_agent_id)
    OR (v_actor = 'party'    AND (v_agent IS NULL
                                  OR v_agent NOT IN (OLD.client_agent_id, coalesce(OLD.provider_agent_id, -1)))) THEN
        RAISE EXCEPTION 'Переход % -> % не может выполнить агент % (выполняет: %)',
            v_from, v_to, coalesce(v_agent::text, 'система'), v_actor;
    END IF;

    -- 5.1.3 гео-услугу закрывают только после сближения; online - из AGREED
    IF v_from = 'AGREED' AND v_to = 'CLOSED' AND get_setting(NEW.service_id, 'place_mode') <> 'online' THEN
        RAISE EXCEPTION 'Закрыть сделку можно только после сближения сторон (CONTACT)'
            USING HINT = 'CONTACT_REQUIRED';
    END IF;
    -- 5.3, 5.4 причина обязательна при отмене со штрафом
    IF v_to IN ('CANCEL_CLIENT','CANCEL_PROVIDER') AND v_reason IS NULL AND v_comment IS NULL THEN
        RAISE EXCEPTION 'Для отмены % нужна причина', v_to;
    END IF;

    IF v_to = 'AGREED' THEN
        IF NEW.provider_agent_id IS NULL THEN
            RAISE EXCEPTION 'Согласование без выбранного поставщика';
        END IF;
        NEW.date_agreed  := now();
        NEW.close_secret := replace(gen_random_uuid()::text || gen_random_uuid()::text, '-', '');
    END IF;
    IF (SELECT value_num FROM dictionary WHERE dict_id = NEW.deal_status_id) = 1 THEN
        NEW.date_close := now();
    END IF;
    RETURN NEW;
END
$$;

-- История статусов и уведомления сторонам после смены статуса
CREATE FUNCTION trg_deal_status_log()
RETURNS trigger LANGUAGE plpgsql SET search_path = mp_service AS $$
    -- Что делает: Триггер AFTER на deal: история статусов и уведомления сторонам.
    -- Методика: Пишет deal_status_log с автором и причиной из параметров сессии; по новому статусу рассылает уведомления (запрос кандидатам, согласование, сближение, баллы, истечение, отмена).
DECLARE
    v_code    text;
    v_agent   numeric := session_agent_id();
    v_reason  numeric := nullif(current_setting('mp.cancel_reason_id', true), '')::numeric;
    v_comment text    := nullif(current_setting('mp.status_comment', true), '');
    v_other   numeric;
    r         record;
BEGIN
    IF is_load_mode() THEN RETURN NULL; END IF;
    IF TG_OP = 'UPDATE' AND NEW.deal_status_id = OLD.deal_status_id THEN RETURN NULL; END IF;
    SELECT dict_code INTO v_code FROM dictionary WHERE dict_id = NEW.deal_status_id;

    INSERT INTO deal_status_log (deal_id, deal_status_id, agent_id, cancel_reason_id, comment)
    VALUES (NEW.deal_id, NEW.deal_status_id, v_agent,
            CASE WHEN v_code LIKE 'CANCEL%' THEN v_reason END, v_comment);

    v_other := CASE WHEN v_agent = NEW.client_agent_id THEN NEW.provider_agent_id ELSE NEW.client_agent_id END;
    CASE v_code
        WHEN 'MATCHING' THEN
            -- 2.10 запрос подходящим поставщикам; маршрутные сервисы рассылает сервер после проверки маршрута
            IF get_setting(NEW.service_id, 'geo_mode') <> 'route' THEN
                FOR r IN SELECT c.provider_agent_id FROM v_deal_candidates c WHERE c.deal_id = NEW.deal_id LOOP
                    PERFORM notify_agent(r.provider_agent_id, NEW.deal_id, 'NEW_REQUEST');
                END LOOP;
            END IF;
        WHEN 'AGREED' THEN
            PERFORM notify_agent(NEW.provider_agent_id, NEW.deal_id, 'AGREED');
        WHEN 'CONTACT' THEN
            PERFORM notify_agent(NEW.client_agent_id,   NEW.deal_id, 'NEAR');
            PERFORM notify_agent(NEW.provider_agent_id, NEW.deal_id, 'NEAR');
        WHEN 'CLOSED' THEN
            PERFORM notify_agent(NEW.client_agent_id,   NEW.deal_id, 'REVIEW_REQUIRED');
            PERFORM notify_agent(NEW.provider_agent_id, NEW.deal_id, 'REVIEW_REQUIRED');
        WHEN 'EXPIRED' THEN
            PERFORM notify_agent(NEW.client_agent_id, NEW.deal_id, 'EXPIRED');
        WHEN 'CANCEL_FREE', 'CANCEL_CLIENT', 'CANCEL_PROVIDER' THEN
            PERFORM notify_agent(v_other, NEW.deal_id, 'CANCELLED',
                jsonb_build_object('status', v_code, 'by', v_agent, 'reason_id', v_reason, 'comment', v_comment));
            IF v_code <> 'CANCEL_FREE' THEN
                PERFORM notify_agent(v_other, NEW.deal_id, 'REVIEW_REQUIRED');
            END IF;
        ELSE NULL;
    END CASE;
    RETURN NULL;
END
$$;

CREATE TRIGGER trg_deal_status BEFORE INSERT OR UPDATE OF deal_status_id ON deal
    FOR EACH ROW EXECUTE FUNCTION trg_deal_status_check();
CREATE TRIGGER trg_deal_status_after AFTER INSERT OR UPDATE OF deal_status_id ON deal
    FOR EACH ROW EXECUTE FUNCTION trg_deal_status_log();

-- Отклик: повторная проверка условий подбора (П-2); первый отклик переводит сделку в OFFERS
CREATE FUNCTION trg_deal_offer_check()
RETURNS trigger LANGUAGE plpgsql SET search_path = mp_service AS $$
    -- Что делает: Триггер BEFORE на deal_offer: повторная проверка условий подбора и цены отклика (П-2).
    -- Методика: Вызывает provider_can_offer с точкой отклика; проверяет цену в пределах настроек поставщика (для км - не ниже порога); отклик получает статус active.
DECLARE
    v_ok     boolean;
    v_reason text;
BEGIN
    IF is_load_mode() THEN RETURN NEW; END IF;
    SELECT ok, reason INTO v_ok, v_reason
    FROM provider_can_offer(NEW.deal_id, NEW.provider_agent_id, NEW.geo_lat, NEW.geo_lon);
    IF NOT v_ok THEN
        RAISE EXCEPTION 'Отклик невозможен: %', v_reason USING HINT = v_reason;
    END IF;
    -- цена отклика - в пределах настроек поставщика (для км - не ниже порога поездки)
    IF EXISTS (
        SELECT 1 FROM deal d
        JOIN agent_service ags ON ags.agent_id = NEW.provider_agent_id AND ags.is_active = 1
                              AND service_covers(ags.service_id, d.service_id)
        WHERE d.deal_id = NEW.deal_id
          AND (NEW.offer_price < coalesce(ags.price_min, 0)
               OR (get_setting(d.service_id, 'price_unit') <> 'km' AND NEW.offer_price > coalesce(ags.price_max, NEW.offer_price)))) THEN
        RAISE EXCEPTION 'Отклик невозможен: PRICE_OUT_OF_RANGE' USING HINT = 'PRICE_OUT_OF_RANGE';
    END IF;
    NEW.offer_status := 'active';
    RETURN NEW;
END
$$;

CREATE FUNCTION trg_deal_offer_after()
RETURNS trigger LANGUAGE plpgsql SET search_path = mp_service AS $$
    -- Что делает: Триггер AFTER на deal_offer: первый отклик переводит сделку в OFFERS, клиенту уведомление.
    -- Методика: Обновляет статус MATCHING -> OFFERS и создает уведомление NEW_OFFER.
DECLARE
    v_client numeric;
BEGIN
    IF is_load_mode() THEN RETURN NULL; END IF;
    UPDATE deal SET deal_status_id = dict_id('DEAL_STATUS','OFFERS')
    WHERE deal_id = NEW.deal_id AND deal_status_id = dict_id('DEAL_STATUS','MATCHING');
    SELECT client_agent_id INTO v_client FROM deal WHERE deal_id = NEW.deal_id;
    PERFORM notify_agent(v_client, NEW.deal_id, 'NEW_OFFER', jsonb_build_object('deal_offer_id', NEW.deal_offer_id));
    RETURN NULL;
END
$$;

-- Период запроса и публикации: не в прошлом, конец позже начала (Р-4)
CREATE FUNCTION trg_period_check()
RETURNS trigger LANGUAGE plpgsql SET search_path = mp_service AS $$
    -- Что делает: Триггер BEFORE INSERT на deal и provider_slot: период, который создает агент, не в прошлом (Р-4).
    -- Методика: Проверяются только вставки от агента (mp.agent_id задан, не загрузка данных); now() - в поясе сессии Asia/Almaty, как и время API.
    -- Запрос (deal) - начало не раньше now() - 1 мин (запас на задержку между формой и отправкой), иначе он сразу истек бы (п. 5.5);
    -- публикация (provider_slot) - окно еще не закончилось: место может быть свободно уже сейчас (п. 4.1).
    -- Конец не позже начала - PERIOD_INVALID (раньше ограничения CHECK, чтобы код был понятен приложению).
BEGIN
    IF is_load_mode() OR session_agent_id() IS NULL THEN RETURN NEW; END IF;
    IF NEW.period_end <= NEW.period_start THEN
        RAISE EXCEPTION 'Окончание периода должно быть позже начала' USING HINT = 'PERIOD_INVALID';
    END IF;
    IF TG_TABLE_NAME = 'deal' AND NEW.period_start < now()::timestamp - interval '1 minute' THEN
        RAISE EXCEPTION 'Начало периода % уже прошло (сейчас %)', NEW.period_start, date_trunc('minute', now()::timestamp)
            USING HINT = 'PERIOD_IN_PAST';
    END IF;
    IF TG_TABLE_NAME = 'provider_slot' AND NEW.period_end <= now()::timestamp THEN
        RAISE EXCEPTION 'Окно % уже закончилось (сейчас %)', NEW.period_end, date_trunc('minute', now()::timestamp)
            USING HINT = 'PERIOD_IN_PAST';
    END IF;
    RETURN NEW;
END
$$;

CREATE TRIGGER trg_deal_period BEFORE INSERT ON deal
    FOR EACH ROW EXECUTE FUNCTION trg_period_check();
CREATE TRIGGER trg_provider_slot_period BEFORE INSERT ON provider_slot
    FOR EACH ROW EXECUTE FUNCTION trg_period_check();

CREATE TRIGGER trg_deal_offer_check BEFORE INSERT ON deal_offer
    FOR EACH ROW EXECUTE FUNCTION trg_deal_offer_check();
CREATE TRIGGER trg_deal_offer_after AFTER INSERT ON deal_offer
    FOR EACH ROW EXECUTE FUNCTION trg_deal_offer_after();

-- Отзыв: только стороны сделки друг другу и только после закрытия или отмены
CREATE FUNCTION trg_deal_review_check()
RETURNS trigger LANGUAGE plpgsql SET search_path = mp_service AS $$
    -- Что делает: Триггер BEFORE на deal_review: баллы только сторонами друг другу и только после закрытия или отмены.
    -- Методика: Проверяет статус сделки и участников, роль оцениваемого проставляет сам.
DECLARE
    d        deal%ROWTYPE;
    v_status text;
BEGIN
    IF is_load_mode() THEN RETURN NEW; END IF;
    SELECT * INTO d FROM deal WHERE deal_id = NEW.deal_id;
    SELECT dict_code INTO v_status FROM dictionary WHERE dict_id = d.deal_status_id;
    IF v_status NOT IN ('CLOSED','CANCEL_FREE','CANCEL_CLIENT','CANCEL_PROVIDER') THEN
        RAISE EXCEPTION 'Баллы ставятся после закрытия или отмены сделки (сейчас %)', v_status;
    END IF;
    IF d.provider_agent_id IS NULL
       OR NEW.from_agent_id NOT IN (d.client_agent_id, d.provider_agent_id)
       OR NEW.to_agent_id   NOT IN (d.client_agent_id, d.provider_agent_id)
       OR NEW.from_agent_id = NEW.to_agent_id THEN
        RAISE EXCEPTION 'Баллы ставят друг другу только стороны сделки %', NEW.deal_id;
    END IF;
    NEW.to_role_id := CASE WHEN NEW.to_agent_id = d.client_agent_id
                           THEN dict_id('ROLE','CLIENT') ELSE dict_id('ROLE','PROVIDER') END;
    RETURN NEW;
END
$$;

CREATE TRIGGER trg_deal_review BEFORE INSERT OR UPDATE ON deal_review
    FOR EACH ROW EXECUTE FUNCTION trg_deal_review_check();

-- Режим тестирования - только администратору
CREATE FUNCTION trg_agent_testing_check()
RETURNS trigger LANGUAGE plpgsql SET search_path = mp_service AS $$
    -- Что делает: Триггер на agent: режим тестирования только для роли ADMIN.
    -- Методика: При is_testing = 1 проверяет наличие активной роли ADMIN.
BEGIN
    IF NEW.is_testing = 1 AND NOT EXISTS (
        SELECT 1 FROM agent_role ar
        WHERE ar.agent_id = NEW.agent_id AND ar.role_id = dict_id('ROLE','ADMIN') AND ar.is_active = 1) THEN
        RAISE EXCEPTION 'Режим тестирования доступен только администратору (агент %)', NEW.agent_id;
    END IF;
    RETURN NEW;
END
$$;

CREATE TRIGGER trg_agent_testing BEFORE INSERT OR UPDATE OF is_testing ON agent
    FOR EACH ROW EXECUTE FUNCTION trg_agent_testing_check();

-- Положение курсором (manual) - только в режиме тестирования
CREATE FUNCTION trg_agent_position_check()
RETURNS trigger LANGUAGE plpgsql SET search_path = mp_service AS $$
    -- Что делает: Триггер на agent_position: положение курсором только в режиме тестирования.
    -- Методика: Для source = manual требует agent.is_testing = 1; ставит время обновления.
BEGIN
    IF NEW.source = 'manual' AND NOT EXISTS (
        SELECT 1 FROM agent a WHERE a.agent_id = NEW.agent_id AND a.is_testing = 1) THEN
        RAISE EXCEPTION 'Положение курсором задается только в режиме тестирования (агент %)', NEW.agent_id;
    END IF;
    NEW.date_update := now();
    RETURN NEW;
END
$$;

CREATE TRIGGER trg_agent_position BEFORE INSERT OR UPDATE ON agent_position
    FOR EACH ROW EXECUTE FUNCTION trg_agent_position_check();

-- Сервис субагенту назначает компания: только из ее пакета и не дольше ее подписки (В-12, Н-9)
CREATE FUNCTION trg_agent_service_assign_check()
RETURNS trigger LANGUAGE plpgsql SET search_path = mp_service AS $$
    -- Что делает: Триггер на agent_service: сервис субагенту назначает только его компания, из своего пакета и не дольше своей подписки (В-12).
    -- Методика: Проверяет связь субагент-компания и наличие сервиса в подписке компании; срок назначения обрезается окончанием подписки.
DECLARE
    v_until timestamp;
BEGIN
    IF NEW.assigned_by_agent_id IS NULL THEN RETURN NEW; END IF;
    IF NOT EXISTS (SELECT 1 FROM agent_role ar
                   WHERE ar.agent_id = NEW.agent_id AND ar.role_id = dict_id('ROLE','SUBAGENT')
                     AND ar.parent_agent_id = NEW.assigned_by_agent_id AND ar.is_active = 1) THEN
        RAISE EXCEPTION 'Агент % не субагент компании %', NEW.agent_id, NEW.assigned_by_agent_id;
    END IF;
    SELECT max(sub.date_end) INTO v_until
    FROM agent_subscription sub
    JOIN package_service ps ON ps.package_id = sub.package_id AND ps.is_active = 1
    JOIN v_service_tree t   ON t.ancestor_id = ps.service_id AND t.service_id = NEW.service_id
    WHERE sub.agent_id = NEW.assigned_by_agent_id AND sub.is_active = 1;
    IF v_until IS NULL THEN
        RAISE EXCEPTION 'Сервиса % нет в пакете компании %', NEW.service_id, NEW.assigned_by_agent_id;
    END IF;
    IF NEW.date_end IS NULL OR NEW.date_end > v_until THEN
        NEW.date_end := v_until;
    END IF;
    RETURN NEW;
END
$$;

CREATE TRIGGER trg_agent_service_assign BEFORE INSERT OR UPDATE ON agent_service
    FOR EACH ROW EXECUTE FUNCTION trg_agent_service_assign_check();

-- Отклики в порядке вывода клиенту (список и точки на карте), п. 4.1-4.3.
-- До согласования точка смещена на approx_offset_m (зависит от сервиса и рейтинга поставщика);
-- направление - md5 номера отклика с секретом сервера (П-4), расстояние - интервалом.
-- Точная точка и расстояние - после согласования выбранного отклика.
CREATE VIEW v_deal_offer_list AS
WITH base AS (
    SELECT o.deal_offer_id, o.deal_id, o.provider_agent_id, o.offer_price, o.offer_status, o.offer_mode,
           o.spot_number, o.geo_lat, o.geo_lon, o.date_creation,
           d.service_id, d.zone_radius_m, ds.dict_code AS deal_status,
           geo_distance_m(d.geo_lat, d.geo_lon, o.geo_lat, o.geo_lon) AS real_distance_m,
           agent_rating(o.provider_agent_id, 'PROVIDER', d.service_id, d.date_creation::date) AS rating,
           agent_pseudonym(o.provider_agent_id) AS pseudonym,
           get_setting(d.service_id, 'geo_mode') AS geo_mode,
           get_setting(d.service_id, 'offer_sort_1') AS s1,
           get_setting(d.service_id, 'offer_sort_2') AS s2,
           get_setting(d.service_id, 'offer_sort_3') AS s3
    FROM deal_offer o
    JOIN deal d        ON d.deal_id = o.deal_id
    JOIN dictionary ds ON ds.dict_id = d.deal_status_id
    WHERE o.offer_status IN ('active','selected')
), shown AS (
    SELECT b.*,
           (b.offer_status = 'selected' AND b.deal_status IN ('AGREED','CONTACT','CLOSED','NOT_CLOSED')) AS is_exact,
           coalesce(get_setting(b.service_id, 'approx_offset_m', b.rating)::numeric, 0) AS offset_m,
           radians((('x' || substr(md5(b.deal_offer_id::text || coalesce(current_setting('mp.geo_secret', true), '')),
                                   1, 6))::bit(24)::int % 360)) AS angle
    FROM base b
)
SELECT deal_id, service_id,
       row_number() OVER (PARTITION BY deal_id
           ORDER BY offer_sort_key(s1, real_distance_m, rating, offer_price) NULLS LAST,
                    offer_sort_key(s2, real_distance_m, rating, offer_price) NULLS LAST,
                    offer_sort_key(s3, real_distance_m, rating, offer_price) NULLS LAST,
                    date_creation, deal_offer_id) AS position,
       deal_offer_id, provider_agent_id, pseudonym, rating, offer_price, offer_status, offer_mode, spot_number,
       is_exact,
       CASE WHEN is_exact OR offset_m = 0 THEN geo_lat
            ELSE round((geo_lat + offset_m * cos(angle) / 111195)::numeric, 6) END AS show_lat,
       CASE WHEN is_exact OR offset_m = 0 THEN geo_lon
            ELSE round((geo_lon + offset_m * sin(angle) / (111195 * cos(radians(geo_lat))))::numeric, 6) END AS show_lon,
       CASE WHEN is_exact OR offset_m = 0 THEN real_distance_m END AS distance_m,
       CASE WHEN real_distance_m IS NULL OR is_exact OR offset_m = 0 THEN NULL
            WHEN real_distance_m <= 50  THEN '0-50'
            WHEN real_distance_m <= 100 THEN '50-100'
            WHEN real_distance_m <= 200 THEN '100-200'
            ELSE '200+' END AS distance_band,
       concat_ws(' · ', pseudonym, '★ ' || rating, round(offer_price) || ' тг') AS marker_label
FROM shown
WHERE real_distance_m IS NULL OR real_distance_m <= zone_radius_m OR geo_mode = 'route';

-- Ссылки на маршрут во внешних картах - только для точной точки (после согласования)
CREATE VIEW v_deal_offer_route AS
SELECT l.deal_id, l.deal_offer_id, m.dict_code AS provider_code, m.sort_order,
       replace(replace(replace(replace(m.value_text,
           '{from_lat}', d.geo_lat::text), '{from_lon}', d.geo_lon::text),
           '{to_lat}',   l.show_lat::text), '{to_lon}',   l.show_lon::text) AS route_url
FROM v_deal_offer_list l
JOIN deal d ON d.deal_id = l.deal_id
JOIN dictionary m ON m.dict_type = 'MAP_ROUTE_LINK' AND m.is_active = 1
WHERE l.is_exact AND d.geo_lat IS NOT NULL AND l.show_lat IS NOT NULL;

-- Поставщики, которым подходит открытая сделка и которые еще не откликнулись (источник входящих запросов)
CREATE VIEW v_deal_candidates AS
SELECT d.deal_id, p.agent_id AS provider_agent_id
FROM deal d
JOIN dictionary s ON s.dict_id = d.deal_status_id AND s.dict_code IN ('MATCHING','OFFERS')
JOIN (SELECT DISTINCT ar.agent_id FROM agent_role ar
      WHERE ar.is_active = 1 AND ar.role_id IN (dict_id('ROLE','PROVIDER'), dict_id('ROLE','SUBAGENT'))) p ON true
CROSS JOIN LATERAL provider_can_offer(d.deal_id, p.agent_id) c
WHERE c.ok
  AND NOT EXISTS (SELECT 1 FROM deal_offer o WHERE o.deal_id = d.deal_id AND o.provider_agent_id = p.agent_id);

-- Активные публикации для карты клиента: точка смещена так же, как у откликов (Н-2)
CREATE VIEW v_provider_slot_map AS
WITH b AS (
    SELECT ps.*, a.is_testing,
           agent_pseudonym(ps.agent_id) AS pseudonym,
           agent_rating(ps.agent_id, 'PROVIDER', ps.service_id) AS rating
    FROM provider_slot ps
    JOIN agent a ON a.agent_id = ps.agent_id
    WHERE ps.slot_status = 'active' AND ps.period_end > now()
), s AS (
    SELECT b.*,
           coalesce(get_setting(b.service_id, 'approx_offset_m', b.rating)::numeric, 0) AS offset_m,
           radians((('x' || substr(md5('slot' || b.slot_id::text || coalesce(current_setting('mp.geo_secret', true), '')),
                                   1, 6))::bit(24)::int % 360)) AS angle
    FROM b
)
SELECT slot_id, agent_id, service_id, pseudonym, rating, is_testing, price_min, period_start, period_end,
       CASE WHEN offset_m = 0 THEN geo_lat
            ELSE round((geo_lat + offset_m * cos(angle) / 111195)::numeric, 6) END AS show_lat,
       CASE WHEN offset_m = 0 THEN geo_lon
            ELSE round((geo_lon + offset_m * sin(angle) / (111195 * cos(radians(geo_lat))))::numeric, 6) END AS show_lon,
       radius_m, geo_to_lat, geo_to_lon, radius_to_m, route_polyline, route_length_m,
       concat_ws(' · ', pseudonym, '★ ' || rating, 'от ' || round(price_min) || ' тг') AS marker_label
FROM s;

-- Варианты интерфейса для выпадающего списка: "Шаблон A / Светлая" (фильтр - is_dark)
CREATE VIEW v_ui_options AS
SELECT t.dict_code AS template_code, sc.dict_code AS scheme_code,
       coalesce((sc.value_json ->> 'dark')::boolean, false) AS is_dark,
       tt.interface_lang_id, tt.text_value AS template_name, st.text_value AS scheme_name,
       tt.text_value || ' / ' || st.text_value AS option_label,
       t.sort_order * 10 + sc.sort_order AS sort_order
FROM dictionary sc
JOIN dictionary t       ON t.dict_id = sc.parent_dict_id AND t.dict_type = 'UI_TEMPLATE' AND t.is_active = 1
JOIN glossary_text tt   ON tt.glossary_id = t.glossary_id
JOIN glossary_text st   ON st.glossary_id = sc.glossary_id AND st.interface_lang_id = tt.interface_lang_id
WHERE sc.dict_type = 'UI_SCHEME' AND sc.is_active = 1;

-- Курсоры карты: роль x режим тестирования -> SVG (цвет зашит в пиктограмму)
CREATE VIEW v_map_cursor AS
SELECT c.dict_code AS cursor_code, i.icon_code, i.icon_svg
FROM dictionary c
JOIN glossary g ON g.glossary_id = c.glossary_id
JOIN icon i     ON i.icon_id = g.icon_id
WHERE c.dict_type = 'MAP_CURSOR' AND c.is_active = 1;

-- Прогноз цены (П-5): нижняя граница - минимальная цена поставщиков рядом, верхняя - медиана их максимумов,
-- медиана - по закрытым сделкам сервиса в ячейке forecast_grid_m за 90 дней.
-- Для price_unit = km прогноз поездки считает сервер по длине маршрута и тарифам водителей.
CREATE FUNCTION price_forecast(p_service_id numeric, p_lat numeric, p_lon numeric,
                               OUT price_min numeric, OUT price_max numeric, OUT price_median numeric,
                               OUT providers bigint)
LANGUAGE plpgsql STABLE SET search_path = mp_service AS $$
    -- Что делает: Прогноз цены услуги в точке (П-5).
    -- Методика: Нижняя граница - минимальная цена поставщиков рядом, верхняя - медиана их максимумов, медиана - по закрытым сделкам сервиса в ячейке forecast_grid_m за 90 дней (иначе середина диапазона).
DECLARE
    v_grid numeric := coalesce(get_setting(p_service_id, 'forecast_grid_m')::numeric, 500);
BEGIN
    SELECT min(ags.price_min),
           percentile_cont(0.5) WITHIN GROUP (ORDER BY ags.price_max),
           count(DISTINCT ags.agent_id)
    INTO price_min, price_max, providers
    FROM agent_service ags
    WHERE ags.is_active = 1 AND service_covers(ags.service_id, p_service_id)
      AND ags.price_min IS NOT NULL
      AND (p_lat IS NULL OR ags.geo_lat IS NULL
           OR geo_distance_m(p_lat, p_lon, ags.geo_lat, ags.geo_lon)
              <= greatest(coalesce(ags.zone_radius_m, 0), v_grid));

    SELECT percentile_cont(0.5) WITHIN GROUP (ORDER BY d.price_provider)
    INTO price_median
    FROM deal d
    WHERE d.deal_status_id = dict_id('DEAL_STATUS','CLOSED')
      AND service_base_id(d.service_id) = service_base_id(p_service_id)
      AND d.date_close >= now() - interval '90 days'
      AND (p_lat IS NULL OR d.geo_lat IS NULL OR geo_distance_m(p_lat, p_lon, d.geo_lat, d.geo_lon) <= v_grid);

    price_median := round(coalesce(price_median, (price_min + price_max) / 2));
END
$$;

-- Цена автоотклика (В-5): цена клиента, если она в диапазоне поставщика,
-- иначе медиана прогноза в пределах диапазона, округленная до шага
CREATE FUNCTION auto_offer_price(p_client_price numeric, p_median numeric,
                                 p_min numeric, p_max numeric, p_round numeric)
RETURNS numeric LANGUAGE sql IMMUTABLE AS $$
    -- Что делает: Цена автоотклика (В-5).
    -- Методика: Цена клиента, если она в диапазоне поставщика; иначе медиана прогноза, ограниченная диапазоном и округленная до шага.
    SELECT CASE
        WHEN p_client_price IS NOT NULL
             AND p_client_price >= coalesce(p_min, p_client_price)
             AND p_client_price <= coalesce(p_max, p_client_price) THEN p_client_price
        ELSE round(least(greatest(coalesce(p_median, p_min, p_max), coalesce(p_min, 0)),
                         coalesce(p_max, 1e12)) / p_round) * p_round
    END
$$;

-- Автоотклики на сделку (п. 3.1): все поставщики с is_auto = 1, которым подходит запрос.
-- Маршрутные сервисы (такси) - на сервере: там проверяется маршрут и считается цена по тарифу.
CREATE FUNCTION make_auto_offers(p_deal_id numeric)
RETURNS int LANGUAGE plpgsql SET search_path = mp_service AS $$
    -- Что делает: Создает автоотклики на сделку всех подходящих поставщиков с is_auto = 1 (п. 3.1).
    -- Методика: Берет кандидатов из v_deal_candidates, цену считает auto_offer_price, точку - из публикации или настроек; маршрутные сервисы пропускает (их ведет сервер).
DECLARE
    d       deal%ROWTYPE;
    r       record;
    v_svc   agent_service%ROWTYPE;
    v_lat   numeric;
    v_lon   numeric;
    v_round numeric;
    v_n     int := 0;
BEGIN
    SELECT * INTO d FROM deal WHERE deal_id = p_deal_id;
    IF get_setting(d.service_id, 'geo_mode') = 'route' THEN RETURN 0; END IF;
    v_round := coalesce(get_setting(d.service_id, 'auto_price_round')::numeric, 50);

    FOR r IN SELECT c.provider_agent_id FROM v_deal_candidates c WHERE c.deal_id = p_deal_id LOOP
        SELECT ags.* INTO v_svc FROM agent_service ags
        WHERE ags.agent_id = r.provider_agent_id AND ags.is_active = 1 AND service_covers(ags.service_id, d.service_id)
        ORDER BY (ags.service_id = d.service_id) DESC, ags.service_id DESC LIMIT 1;
        CONTINUE WHEN v_svc.is_auto <> 1;

        SELECT ps.geo_lat, ps.geo_lon INTO v_lat, v_lon FROM provider_slot ps
        WHERE ps.agent_id = r.provider_agent_id AND ps.slot_status = 'active'
          AND service_covers(ps.service_id, d.service_id)
          AND tsrange(d.period_start, d.period_end, '[]') <@ tsrange(ps.period_start, ps.period_end, '[]')
        ORDER BY ps.slot_id DESC LIMIT 1;
        IF NOT FOUND THEN v_lat := v_svc.geo_lat; v_lon := v_svc.geo_lon; END IF;

        INSERT INTO deal_offer (deal_id, provider_agent_id, offer_price, offer_mode, geo_lat, geo_lon)
        VALUES (p_deal_id, r.provider_agent_id,
                auto_offer_price(d.client_price, (d.price_forecast_min + d.price_forecast_max) / 2,
                                 v_svc.price_min, v_svc.price_max, v_round),
                'auto', v_lat, v_lon);
        v_n := v_n + 1;
    END LOOP;
    RETURN v_n;
END
$$;

-- Согласование отклика клиентом (п. 4.4) в одной транзакции
CREATE FUNCTION deal_agree(p_deal_id numeric, p_offer_id numeric)
RETURNS void LANGUAGE plpgsql SET search_path = mp_service AS $$
    -- Что делает: Согласование отклика клиентом в одной транзакции (п. 4.4).
    -- Методика: Блокирует сделку, проверяет клиента, активность отклика и занятость поставщика; выбранный отклик - selected, остальные - declined с уведомлением OFFER_DECLINED их поставщикам; сделке поставщик, цена и статус AGREED; при исчерпании лимита снимает другие отклики поставщика на это время и уведомляет их клиентов.
DECLARE
    d     deal%ROWTYPE;
    o     deal_offer%ROWTYPE;
    v_fee numeric;
    r     record;
BEGIN
    SELECT * INTO d FROM deal WHERE deal_id = p_deal_id FOR UPDATE;
    IF NOT FOUND THEN RAISE EXCEPTION 'Нет сделки %', p_deal_id; END IF;
    IF session_agent_id() IS DISTINCT FROM d.client_agent_id THEN
        RAISE EXCEPTION 'Согласовать может только клиент сделки';
    END IF;
    SELECT * INTO o FROM deal_offer WHERE deal_offer_id = p_offer_id AND deal_id = p_deal_id FOR UPDATE;
    IF NOT FOUND OR o.offer_status <> 'active' THEN
        RAISE EXCEPTION 'Отклик % уже не активен', p_offer_id USING HINT = 'OFFER_NOT_ACTIVE';
    END IF;
    -- место могло быть занято другой сделкой после отклика
    IF provider_busy(o.provider_agent_id, d.service_id, d.period_start, d.period_end, d.deal_id)
       >= provider_limit(o.provider_agent_id, d.service_id) THEN
        UPDATE deal_offer SET offer_status = 'withdrawn' WHERE deal_offer_id = p_offer_id;
        RAISE EXCEPTION 'Поставщик уже занят на это время' USING HINT = 'BUSY';
    END IF;

    UPDATE deal_offer
    SET offer_status = CASE WHEN deal_offer_id = p_offer_id THEN 'selected' ELSE 'declined' END
    WHERE deal_id = p_deal_id AND offer_status = 'active';

    -- невыбранным поставщикам - уведомление: сделка пропадает из их списков, иначе об отказе не узнать (Э5 шаг 8)
    FOR r IN SELECT deal_offer_id, provider_agent_id FROM deal_offer WHERE deal_id = p_deal_id AND offer_status = 'declined' LOOP
        PERFORM notify_agent(r.provider_agent_id, p_deal_id, 'OFFER_DECLINED', jsonb_build_object('deal_offer_id', r.deal_offer_id));
    END LOOP;

    v_fee := round(o.offer_price * coalesce(get_setting(d.service_id, 'deal_fee_percent')::numeric, 0) / 100, 2);
    UPDATE deal
    SET provider_agent_id = o.provider_agent_id, price_provider = o.offer_price,
        fee_amount = v_fee, price_total = o.offer_price + v_fee,
        deal_status_id = dict_id('DEAL_STATUS','AGREED')
    WHERE deal_id = p_deal_id;

    -- лимит исчерпан: снять другие активные отклики поставщика на пересекающееся время (Н-3)
    IF provider_busy(o.provider_agent_id, d.service_id, d.period_start, d.period_end)
       >= provider_limit(o.provider_agent_id, d.service_id) THEN
        FOR r IN
            SELECT o2.deal_offer_id, x.deal_id, x.client_agent_id
            FROM deal_offer o2 JOIN deal x ON x.deal_id = o2.deal_id
            WHERE o2.provider_agent_id = o.provider_agent_id AND o2.offer_status = 'active'
              AND x.deal_id <> p_deal_id
              AND service_base_id(x.service_id) = service_base_id(d.service_id)
              AND tsrange(x.period_start, x.period_end) && tsrange(d.period_start, d.period_end)
        LOOP
            UPDATE deal_offer SET offer_status = 'withdrawn' WHERE deal_offer_id = r.deal_offer_id;
            PERFORM notify_agent(r.client_agent_id, r.deal_id, 'OFFER_WITHDRAWN',
                                 jsonb_build_object('deal_offer_id', r.deal_offer_id));
        END LOOP;
    END IF;
END
$$;

-- Другая сторона опаздывает: период начался, а ее положение дальше near_radius_m от места встречи (В-3)
CREATE FUNCTION deal_other_side_late(p_deal_id numeric, p_agent_id numeric)
RETURNS boolean LANGUAGE sql STABLE SET search_path = mp_service AS $$
    -- Что делает: Опаздывает ли другая сторона сделки (для бесплатной отмены, В-3).
    -- Методика: Период начался, а свежее положение другой стороны дальше near_radius_m от места встречи (точка отклика для place_mode = provider, иначе точка клиента).
    SELECT coalesce((
        SELECT now() > d.period_start
           AND geo_distance_m(pos.geo_lat, pos.geo_lon,
                   CASE WHEN get_setting(d.service_id, 'place_mode') = 'provider' THEN coalesce(o.geo_lat, d.geo_lat) ELSE d.geo_lat END,
                   CASE WHEN get_setting(d.service_id, 'place_mode') = 'provider' THEN coalesce(o.geo_lon, d.geo_lon) ELSE d.geo_lon END)
               > get_setting(d.service_id, 'near_radius_m')::numeric
        FROM deal d
        JOIN agent_position pos ON pos.agent_id = CASE WHEN p_agent_id = d.client_agent_id
                                                       THEN d.provider_agent_id ELSE d.client_agent_id END
        LEFT JOIN deal_offer o ON o.deal_id = d.deal_id AND o.offer_status = 'selected'
        WHERE d.deal_id = p_deal_id
          AND pos.date_update >= now() - make_interval(secs => get_setting(d.service_id, 'position_ttl_s')::int)
    ), false)
$$;

-- Отмена стороной сделки (п. 5.2-5.4); итоговый статус решает БД. Возвращает код статуса.
CREATE FUNCTION deal_cancel_mode(p_deal_id numeric, p_agent_id numeric)
RETURNS text LANGUAGE plpgsql STABLE SET search_path = mp_service AS $$
    -- Что делает: Каким будет итог отмены сделки агентом прямо сейчас (п. 5.2-5.4); NULL - отменить нельзя.
    -- Методика: Только сторона сделки. До согласования - CANCEL_FREE; AGREED в окне cancel_free_min или при опоздании другой стороны - CANCEL_FREE; AGREED/CONTACT в остальных случаях - CANCEL_CLIENT или CANCEL_PROVIDER по стороне; финальные статусы - NULL. Одно правило для deal_cancel и для предупреждения в приложении.
DECLARE
    d        deal%ROWTYPE;
    v_status text;
BEGIN
    SELECT * INTO d FROM deal WHERE deal_id = p_deal_id;
    IF NOT FOUND OR p_agent_id IS NULL OR p_agent_id NOT IN (d.client_agent_id, coalesce(d.provider_agent_id, -1)) THEN
        RETURN NULL;
    END IF;
    SELECT dict_code INTO v_status FROM dictionary WHERE dict_id = d.deal_status_id;

    IF v_status IN ('INIT','MATCHING','OFFERS') THEN
        RETURN 'CANCEL_FREE';
    ELSIF v_status = 'AGREED'
          AND (now() <= d.date_agreed + make_interval(mins => get_setting(d.service_id, 'cancel_free_min')::int)
               OR deal_other_side_late(p_deal_id, p_agent_id)) THEN
        RETURN 'CANCEL_FREE';
    ELSIF v_status IN ('AGREED','CONTACT') THEN
        RETURN CASE WHEN p_agent_id = d.client_agent_id THEN 'CANCEL_CLIENT' ELSE 'CANCEL_PROVIDER' END;
    END IF;
    RETURN NULL;
END
$$;

CREATE FUNCTION deal_cancel(p_deal_id numeric, p_reason_id numeric DEFAULT NULL, p_comment text DEFAULT NULL)
RETURNS text LANGUAGE plpgsql SET search_path = mp_service AS $$
    -- Что делает: Отмена сделки стороной; итоговый статус решает БД (п. 5.2-5.4).
    -- Методика: До согласования, в окне cancel_free_min до CONTACT или при опоздании другой стороны - CANCEL_FREE; иначе CANCEL_CLIENT / CANCEL_PROVIDER с обязательной причиной; активные отклики истекают; отмена после CONTACT - эпизод фрода NEAR_CANCEL.
DECLARE
    d        deal%ROWTYPE;
    v_agent  numeric := session_agent_id();
    v_status text;
    v_new    text;
BEGIN
    SELECT * INTO d FROM deal WHERE deal_id = p_deal_id FOR UPDATE;
    IF NOT FOUND THEN RAISE EXCEPTION 'Нет сделки %', p_deal_id; END IF;
    IF v_agent IS NULL OR v_agent NOT IN (d.client_agent_id, coalesce(d.provider_agent_id, -1)) THEN
        RAISE EXCEPTION 'Отменить может только сторона сделки';
    END IF;
    SELECT dict_code INTO v_status FROM dictionary WHERE dict_id = d.deal_status_id;

    -- итог отмены - по тем же правилам, что видит приложение до нажатия (deal_cancel_mode)
    v_new := deal_cancel_mode(p_deal_id, v_agent);
    IF v_new IS NULL THEN
        RAISE EXCEPTION 'Сделку в статусе % отменить нельзя', v_status;
    END IF;

    PERFORM set_config('mp.cancel_reason_id', coalesce(p_reason_id::text, ''), true);
    PERFORM set_config('mp.status_comment', coalesce(p_comment, ''), true);
    UPDATE deal SET deal_status_id = dict_id('DEAL_STATUS', v_new) WHERE deal_id = p_deal_id;
    UPDATE deal_offer SET offer_status = 'expired' WHERE deal_id = p_deal_id AND offer_status = 'active';

    -- 6.1 отмена после показа гос. номеров вблизи - эпизод фрода
    IF v_status = 'CONTACT' THEN
        PERFORM register_fraud_episode(v_agent, 'NEAR_CANCEL', p_deal_id);
    END IF;
    RETURN v_new;
END
$$;

-- Сближение сторон (п. 4, AGREED -> CONTACT): после обновления положения и в плановом задании (Э6, В-Э6-1)
CREATE FUNCTION deal_contact_check(p_agent_id numeric DEFAULT NULL)
RETURNS numeric[] LANGUAGE plpgsql SET search_path = mp_service AS $$
    -- Что делает: Переводит в CONTACT согласованные гео-сделки, стороны которых сблизились; возвращает номера переведенных сделок.
    -- Методика: Сделки AGREED агента (p_agent_id - клиент или поставщик) или все (NULL), кроме place_mode = online; свежие положения обеих сторон (не старше position_ttl_s) ближе near_radius_m. Переход выполняет система: mp.agent_id на время перевода очищается и затем восстанавливается, чтобы вызов из транзакции агента не менял ее автора.
DECLARE
    r       record;
    v_ids   numeric[] := '{}';
    v_agent text := current_setting('mp.agent_id', true);
BEGIN
    PERFORM set_config('mp.agent_id', '', true);
    FOR r IN SELECT d.deal_id FROM deal d
             JOIN agent_position pc ON pc.agent_id = d.client_agent_id
             JOIN agent_position pp ON pp.agent_id = d.provider_agent_id
             WHERE d.deal_status_id = dict_id('DEAL_STATUS','AGREED')
               AND (p_agent_id IS NULL OR p_agent_id IN (d.client_agent_id, d.provider_agent_id))
               AND get_setting(d.service_id, 'place_mode') <> 'online'
               AND pc.date_update >= now() - make_interval(secs => get_setting(d.service_id, 'position_ttl_s')::int)
               AND pp.date_update >= now() - make_interval(secs => get_setting(d.service_id, 'position_ttl_s')::int)
               AND geo_distance_m(pc.geo_lat, pc.geo_lon, pp.geo_lat, pp.geo_lon)
                   <= get_setting(d.service_id, 'near_radius_m')::numeric
             FOR UPDATE OF d LOOP
        UPDATE deal SET deal_status_id = dict_id('DEAL_STATUS','CONTACT') WHERE deal_id = r.deal_id;
        v_ids := v_ids || r.deal_id;
    END LOOP;
    PERFORM set_config('mp.agent_id', coalesce(v_agent, ''), true);
    RETURN v_ids;
END
$$;

-- Плановые задания (п. 5.5, 4.6), вызывает сервер раз в минуту от имени системы
CREATE FUNCTION run_deal_jobs()
RETURNS jsonb LANGUAGE plpgsql SET search_path = mp_service AS $$
    -- Что делает: Плановые задания по сделкам, вызывает сервер раз в минуту от имени системы (п. 4.6, 5.5).
    -- Методика: Истечение открытых сделок к началу периода; CONTACT при сближении сторон (deal_contact_check по всем сделкам - запасной путь к проверке после PUT /me/position); вопрос "Услуга оказана?" после периода; NOT_CLOSED через сутки с сигналом антифроду, если стороны встречались; закрытие прошедших публикаций. Возвращает счетчики.
DECLARE
    r          record;
    v_expired  int := 0;
    v_contact  int := 0;
    v_ask      int := 0;
    v_notclose int := 0;
    v_slots    int := 0;
BEGIN
    PERFORM set_config('mp.agent_id', '', true);

    -- истечение: нет согласования к началу периода
    FOR r IN SELECT d.deal_id FROM deal d
             WHERE d.deal_status_id IN (dict_id('DEAL_STATUS','MATCHING'), dict_id('DEAL_STATUS','OFFERS'))
               AND d.period_start <= now() LOOP
        UPDATE deal SET deal_status_id = dict_id('DEAL_STATUS','EXPIRED') WHERE deal_id = r.deal_id;
        UPDATE deal_offer SET offer_status = 'expired' WHERE deal_id = r.deal_id AND offer_status = 'active';
        v_expired := v_expired + 1;
    END LOOP;

    -- сближение сторон (запасной путь: обычно CONTACT ставит PUT /me/position)
    v_contact := cardinality(deal_contact_check(NULL));

    -- вопрос "Услуга оказана?" после окончания периода
    FOR r IN SELECT d.deal_id, d.client_agent_id, d.provider_agent_id FROM deal d
             WHERE d.deal_status_id IN (dict_id('DEAL_STATUS','AGREED'), dict_id('DEAL_STATUS','CONTACT'))
               AND d.period_end + make_interval(mins => get_setting(d.service_id, 'closing_ask_after_min')::int) <= now()
               AND NOT EXISTS (SELECT 1 FROM notification n
                               WHERE n.deal_id = d.deal_id AND n.notify_type_id = dict_id('NOTIFY_TYPE','CLOSING_ASK')) LOOP
        PERFORM notify_agent(r.client_agent_id,   r.deal_id, 'CLOSING_ASK');
        PERFORM notify_agent(r.provider_agent_id, r.deal_id, 'CLOSING_ASK');
        v_ask := v_ask + 1;
    END LOOP;

    -- не закрыта: стороны встречались - сигнал антифроду (обход платформы)
    FOR r IN SELECT d.deal_id, d.client_agent_id, d.provider_agent_id,
                    d.deal_status_id = dict_id('DEAL_STATUS','CONTACT') AS met
             FROM deal d
             WHERE d.deal_status_id IN (dict_id('DEAL_STATUS','AGREED'), dict_id('DEAL_STATUS','CONTACT'))
               AND d.period_end + make_interval(hours => get_setting(d.service_id, 'not_closed_after_h')::int) <= now() LOOP
        UPDATE deal SET deal_status_id = dict_id('DEAL_STATUS','NOT_CLOSED') WHERE deal_id = r.deal_id;
        IF r.met THEN
            PERFORM register_fraud_episode(r.client_agent_id,   'BYPASS_GEO', r.deal_id);
            PERFORM register_fraud_episode(r.provider_agent_id, 'BYPASS_GEO', r.deal_id);
        END IF;
        v_notclose := v_notclose + 1;
    END LOOP;

    UPDATE provider_slot SET slot_status = 'closed' WHERE slot_status = 'active' AND period_end <= now();
    GET DIAGNOSTICS v_slots = ROW_COUNT;

    RETURN jsonb_build_object('expired', v_expired, 'contact', v_contact, 'closing_ask', v_ask,
                              'not_closed', v_notclose, 'slots_closed', v_slots);
END
$$;

-- ---------------------------------------------------------------------
-- 10. ОПИСАНИЯ ТАБЛИЦ, ПОЛЕЙ И ПРЕДСТАВЛЕНИЙ (COMMENT ON)
-- ---------------------------------------------------------------------

-- Общие поля версионности - одним циклом для всех таблиц, где они есть; ниже уточняются, если смысл другой
DO $$
DECLARE
    r record;
BEGIN
    FOR r IN
        SELECT c.table_name, c.column_name,
               CASE c.column_name
                   WHEN 'date_creation' THEN 'Дата и время создания записи'
                   WHEN 'date_start'    THEN 'Начало периода действия записи'
                   WHEN 'date_end'      THEN 'Окончание периода действия записи (NULL - бессрочно)'
                   WHEN 'is_active'     THEN 'Признак активности: 1 - активна, 0 - нет'
                   WHEN 'sort_order'    THEN 'Порядок сортировки при выводе'
               END AS descr
        FROM information_schema.columns c
        JOIN information_schema.tables t ON t.table_schema = c.table_schema AND t.table_name = c.table_name
        WHERE c.table_schema = 'mp_service' AND t.table_type = 'BASE TABLE'
          AND c.column_name IN ('date_creation','date_start','date_end','is_active','sort_order')
    LOOP
        EXECUTE format('COMMENT ON COLUMN mp_service.%I.%I IS %L', r.table_name, r.column_name, r.descr);
    END LOOP;
END
$$;

-- 1. Языки, пиктограммы, глоссарий, справочник
COMMENT ON TABLE  interface_lang IS 'DIM. Языки интерфейса';
COMMENT ON COLUMN interface_lang.interface_lang_id IS 'Ключ языка интерфейса';
COMMENT ON COLUMN interface_lang.language_code     IS 'Код языка: RUS, ENG, KAZ, DEU';
COMMENT ON COLUMN interface_lang.language_name     IS 'Название языка на самом языке';

COMMENT ON TABLE  icon IS 'DIM. Пиктограммы: SVG (currentColor - под любую тему; у курсоров карты цвет зашит) или ссылка на файл';
COMMENT ON COLUMN icon.icon_id     IS 'Ключ пиктограммы';
COMMENT ON COLUMN icon.icon_code   IS 'Код пиктограммы: parking, car, cursor_client_test ...';
COMMENT ON COLUMN icon.icon_format IS 'Формат: svg или png';
COMMENT ON COLUMN icon.icon_svg    IS 'Встроенный SVG 24x24';
COMMENT ON COLUMN icon.icon_url    IS 'Ссылка на файл пиктограммы (CDN), если не встроенный SVG';

COMMENT ON TABLE  glossary IS 'DIM. Термин глоссария, не зависит от языка: код и пиктограмма. На термин ссылаются сервисы, атрибуты, справочник, пакеты';
COMMENT ON COLUMN glossary.glossary_id   IS 'Ключ термина. Диапазоны: 10000+ сервисы, 20000+ атрибуты, 30000+ справочник, 40000+ пакеты, 50000+ тексты интерфейса';
COMMENT ON COLUMN glossary.glossary_code IS 'Уникальный код термина (тексты интерфейса - ui.*)';
COMMENT ON COLUMN glossary.icon_id       IS 'Пиктограмма термина, одна на все языки';

COMMENT ON TABLE  glossary_text IS 'DIM. Перевод термина на язык интерфейса и синонимы для поиска';
COMMENT ON COLUMN glossary_text.glossary_id       IS 'Термин';
COMMENT ON COLUMN glossary_text.interface_lang_id IS 'Язык перевода';
COMMENT ON COLUMN glossary_text.text_value        IS 'Текст на языке';
COMMENT ON COLUMN glossary_text.search_text       IS 'Синонимы для поиска через пробел';

COMMENT ON TABLE  dictionary IS 'DIM. Общий справочник коротких списков значений: тип списка в dict_type (ROLE, DEAL_STATUS, DEAL_TRANSITION, NOTIFY_TYPE, UI_TEMPLATE ...)';
COMMENT ON COLUMN dictionary.dict_id        IS 'Ключ значения справочника';
COMMENT ON COLUMN dictionary.dict_type      IS 'Тип списка: ROLE, DEAL_STATUS, DEAL_TRANSITION, CANCEL_REASON, MAP_BASE_LAYER, MAP_ROUTE_LINK, MAP_CURSOR, REFERRAL_LEVEL, PAYMENT_METHOD, NOTIFY_TYPE, SANCTION_REASON, UI_TEMPLATE, UI_SCHEME';
COMMENT ON COLUMN dictionary.dict_code      IS 'Код значения внутри типа';
COMMENT ON COLUMN dictionary.parent_dict_id IS 'Родительское значение: шаблон для цветовой схемы, статус "из" для перехода';
COMMENT ON COLUMN dictionary.glossary_id    IS 'Подпись и пиктограмма значения';
COMMENT ON COLUMN dictionary.value_num      IS 'Числовой параметр: процент уровня реферала, признак финального статуса (1), статус "в" для перехода, версия карты';
COMMENT ON COLUMN dictionary.value_text     IS 'Текстовый параметр: шаблон ссылки, URL тайлов, исполнитель перехода (client, provider, party, system, any)';
COMMENT ON COLUMN dictionary.value_json     IS 'JSON-параметр: раскладка шаблона интерфейса, палитра цветовой схемы';

-- 2. Сервисы
COMMENT ON TABLE  service IS 'DIM. Дерево услуг: категория -> сервис -> вариант. Настройки и поля наследуются вниз по дереву';
COMMENT ON COLUMN service.service_id        IS 'Ключ узла дерева услуг';
COMMENT ON COLUMN service.parent_service_id IS 'Родительский узел (NULL - корень)';
COMMENT ON COLUMN service.node_type         IS 'Тип узла: system (базовый профиль), category, service, variant';
COMMENT ON COLUMN service.glossary_id       IS 'Название, синонимы и пиктограмма узла';

COMMENT ON TABLE  service_setting IS 'DIM. Настройки узла дерева или платформы (service_id NULL); значение ищется вверх по дереву, может зависеть от рейтинга поставщика';
COMMENT ON COLUMN service_setting.setting_id    IS 'Ключ настройки';
COMMENT ON COLUMN service_setting.service_id    IS 'Узел дерева; NULL - вся платформа';
COMMENT ON COLUMN service_setting.setting_code  IS 'Код настройки: geo_mode, place_mode, price_unit, offer_sort_1..3, near_radius_m, approx_offset_m, slot_required, close_method ...';
COMMENT ON COLUMN service_setting.rating_min    IS 'Нижняя граница рейтинга поставщика, включительно (NULL - без ограничения)';
COMMENT ON COLUMN service_setting.rating_max    IS 'Верхняя граница рейтинга поставщика, не включительно';
COMMENT ON COLUMN service_setting.setting_value IS 'Значение настройки (текстом)';

COMMENT ON TABLE  attribute IS 'DIM. Поля карточек, запросов и откликов; группы, списки выбора (dim) и их значения (par)';
COMMENT ON COLUMN attribute.attribute_id        IS 'Ключ атрибута';
COMMENT ON COLUMN attribute.parent_attribute_id IS 'Группа или список, в который входит атрибут';
COMMENT ON COLUMN attribute.attribute_type      IS 'Тип и формат значения: text, number, mobile, email, date, car_number, address, photo, card_token, group, dim, par ...';
COMMENT ON COLUMN attribute.glossary_id         IS 'Подпись и пиктограмма поля';

COMMENT ON TABLE  service_attribute IS 'LINK. Поля узла дерева услуг: профиль, запрос, отклик; обязательность и когда значение видно другой стороне';
COMMENT ON COLUMN service_attribute.service_id   IS 'Узел дерева';
COMMENT ON COLUMN service_attribute.attribute_id IS 'Атрибут';
COMMENT ON COLUMN service_attribute.attr_scope   IS 'Где заполняется: profile - карточка агента, request - запрос клиента, offer - отклик поставщика';
COMMENT ON COLUMN service_attribute.is_required  IS 'Обязательное поле: 1 - да';
COMMENT ON COLUMN service_attribute.show_stage   IS 'Когда видно другой стороне: always, never, agreed (после согласования), near (при сближении)';

COMMENT ON TABLE  service_package IS 'DIM. Пакет сервисов (абонплата) для роли: цена, субагенты в пакете и сверх него, пробный период';
COMMENT ON COLUMN service_package.package_id           IS 'Ключ пакета';
COMMENT ON COLUMN service_package.glossary_id          IS 'Название пакета';
COMMENT ON COLUMN service_package.role_id              IS 'Роль, для которой пакет (справочник ROLE)';
COMMENT ON COLUMN service_package.price                IS 'Цена пакета за период';
COMMENT ON COLUMN service_package.price_period         IS 'Период оплаты: month или year';
COMMENT ON COLUMN service_package.included_subagents   IS 'Сколько субагентов входит в цену';
COMMENT ON COLUMN service_package.extra_subagent_price IS 'Цена каждого субагента сверх включенных';
COMMENT ON COLUMN service_package.trial_days           IS 'Длительность пробного периода, дней';
COMMENT ON COLUMN service_package.currency_code        IS 'Валюта цены';

COMMENT ON TABLE  package_service IS 'LINK. Состав пакета: узел-категория включает все сервисы под ней';
COMMENT ON COLUMN package_service.package_id IS 'Пакет';
COMMENT ON COLUMN package_service.service_id IS 'Узел дерева услуг, входящий в пакет';

-- 3. Агенты
COMMENT ON TABLE  agent IS 'LIST. Агенты (пользователи платформы): одна учетная запись - несколько ролей и сервисов';
COMMENT ON COLUMN agent.agent_id          IS 'Ключ агента';
COMMENT ON COLUMN agent.interface_lang_id IS 'Язык интерфейса агента';
COMMENT ON COLUMN agent.referrer_agent_id IS 'Кто пригласил агента (реферальная программа)';
COMMENT ON COLUMN agent.is_testing        IS 'Режим тестирования: положение курсором, QR без камеры, шестеренка на курсоре. Только для роли ADMIN';

COMMENT ON TABLE  agent_login IS 'LIST. Вход агента по логину и паролю; хранится только хеш bcrypt (pgcrypto)';
COMMENT ON COLUMN agent_login.login           IS 'Логин в нижнем регистре';
COMMENT ON COLUMN agent_login.agent_id        IS 'Агент, под которым выполняется вход';
COMMENT ON COLUMN agent_login.password_hash   IS 'Хеш пароля bcrypt; проверка - функция agent_login_check';
COMMENT ON COLUMN agent_login.date_last_login IS 'Дата и время последнего успешного входа';

COMMENT ON TABLE  agent_role IS 'LINK. Роли агента: CLIENT, PROVIDER, COMPANY, SUBAGENT, ADMIN';
COMMENT ON COLUMN agent_role.agent_id        IS 'Агент';
COMMENT ON COLUMN agent_role.role_id         IS 'Роль (справочник ROLE)';
COMMENT ON COLUMN agent_role.parent_agent_id IS 'Компания, в которую входит субагент';

COMMENT ON TABLE  agent_service IS 'LINK. Подключенные сервисы агента и настройки поставщика: зона, время, цены, тариф, автоотклик';
COMMENT ON COLUMN agent_service.agent_id             IS 'Агент';
COMMENT ON COLUMN agent_service.service_id           IS 'Узел дерева услуг (сервис или вариант, например класс авто)';
COMMENT ON COLUMN agent_service.geo_lat              IS 'Широта точки обслуживания по умолчанию';
COMMENT ON COLUMN agent_service.geo_lon              IS 'Долгота точки обслуживания по умолчанию';
COMMENT ON COLUMN agent_service.zone_radius_m        IS 'Радиус зоны обслуживания поставщика, м';
COMMENT ON COLUMN agent_service.work_time_from       IS 'Начало рабочего времени (для сервисов без публикаций)';
COMMENT ON COLUMN agent_service.work_time_to         IS 'Окончание рабочего времени; меньше начала - окно через полночь';
COMMENT ON COLUMN agent_service.price_min            IS 'Минимальная цена; для такси - порог стоимости поездки';
COMMENT ON COLUMN agent_service.price_max            IS 'Максимальная цена';
COMMENT ON COLUMN agent_service.unit_price_min       IS 'Минимальная цена за единицу price_unit (км, час)';
COMMENT ON COLUMN agent_service.capacity             IS 'Емкость (частная парковка): сколько сделок одновременно; NULL - настройка max_parallel_deals';
COMMENT ON COLUMN agent_service.client_rating_min    IS 'Минимальный рейтинг клиента, с которым поставщик работает';
COMMENT ON COLUMN agent_service.is_auto              IS 'Автоотклик на подходящие запросы: 1 - включен';
COMMENT ON COLUMN agent_service.assigned_by_agent_id IS 'Компания, назначившая сервис субагенту (период - date_start..date_end, не дольше подписки компании)';

COMMENT ON TABLE  agent_attribute IS 'FAKT. Значения полей профиля агента с историей изменений';
COMMENT ON COLUMN agent_attribute.agent_id        IS 'Агент';
COMMENT ON COLUMN agent_attribute.attribute_id    IS 'Атрибут профиля (16 - псевдоним, виден всем)';
COMMENT ON COLUMN agent_attribute.attribute_value IS 'Значение в формате attribute_type';
COMMENT ON COLUMN agent_attribute.date_start      IS 'С какого момента действует значение (часть ключа истории)';

COMMENT ON TABLE  ui_config IS 'LIST. Конфигурация интерфейса агента: язык, шаблон, цветовая схема; приложение перезагружает ее при смене контрольной суммы';
COMMENT ON COLUMN ui_config.agent_id    IS 'Агент';
COMMENT ON COLUMN ui_config.config_json IS 'Конфигурация: lang, template (A/B/C), scheme (A_LIGHT ...), map, version';
COMMENT ON COLUMN ui_config.checksum    IS 'Контрольная сумма md5(config_json)';

COMMENT ON TABLE  agent_position IS 'FAKT. Последнее положение агента: GPS или курсор на карте (manual - только в режиме тестирования)';
COMMENT ON COLUMN agent_position.agent_id    IS 'Агент';
COMMENT ON COLUMN agent_position.geo_lat     IS 'Широта';
COMMENT ON COLUMN agent_position.geo_lon     IS 'Долгота';
COMMENT ON COLUMN agent_position.source      IS 'Источник: gps или manual (курсор)';
COMMENT ON COLUMN agent_position.accuracy_m  IS 'Точность GPS, м';
COMMENT ON COLUMN agent_position.date_update IS 'Когда положение обновлено; старше position_ttl_s считается неизвестным';

COMMENT ON TABLE  provider_slot IS 'FAKT. Публикация доступности поставщика: свободное место на время или маршрут водителя А -> Б';
COMMENT ON COLUMN provider_slot.slot_id        IS 'Ключ публикации';
COMMENT ON COLUMN provider_slot.agent_id       IS 'Поставщик';
COMMENT ON COLUMN provider_slot.service_id     IS 'Узел дерева услуг';
COMMENT ON COLUMN provider_slot.geo_lat        IS 'Широта места или точки А маршрута';
COMMENT ON COLUMN provider_slot.geo_lon        IS 'Долгота места или точки А маршрута';
COMMENT ON COLUMN provider_slot.radius_m       IS 'Радиус точки или ширина коридора маршрута rA, м';
COMMENT ON COLUMN provider_slot.geo_to_lat     IS 'Широта точки Б (такси)';
COMMENT ON COLUMN provider_slot.geo_to_lon     IS 'Долгота точки Б (такси)';
COMMENT ON COLUMN provider_slot.radius_to_m    IS 'Радиус rB вокруг точки Б, м';
COMMENT ON COLUMN provider_slot.route_polyline IS 'Маршрут А -> Б по дорогам OSM (encoded polyline, считает сервер)';
COMMENT ON COLUMN provider_slot.route_length_m IS 'Длина маршрута, м';
COMMENT ON COLUMN provider_slot.period_start   IS 'Начало окна доступности';
COMMENT ON COLUMN provider_slot.period_end     IS 'Окончание окна доступности';
COMMENT ON COLUMN provider_slot.price_min      IS 'Минимальная цена по публикации';
COMMENT ON COLUMN provider_slot.slot_status    IS 'Статус: active, closed (окно прошло), cancelled (снята поставщиком)';

-- 4. Подписки и рефералы
COMMENT ON TABLE  agent_subscription IS 'FAKT. Период подписки агента на пакет: пробный (один раз на агента) или платный';
COMMENT ON COLUMN agent_subscription.subscription_id IS 'Ключ подписки';
COMMENT ON COLUMN agent_subscription.agent_id        IS 'Агент';
COMMENT ON COLUMN agent_subscription.package_id      IS 'Пакет';
COMMENT ON COLUMN agent_subscription.is_trial        IS 'Пробный период: 1 - да';
COMMENT ON COLUMN agent_subscription.date_start      IS 'Начало периода подписки';
COMMENT ON COLUMN agent_subscription.date_end        IS 'Окончание периода подписки';

COMMENT ON TABLE  subscription_payment IS 'FAKT. Оплаты абонплаты';
COMMENT ON COLUMN subscription_payment.payment_id      IS 'Ключ оплаты';
COMMENT ON COLUMN subscription_payment.subscription_id IS 'Оплаченный период подписки';
COMMENT ON COLUMN subscription_payment.agent_id        IS 'Плательщик';
COMMENT ON COLUMN subscription_payment.amount          IS 'Сумма оплаты';
COMMENT ON COLUMN subscription_payment.payment_status  IS 'Статус: paid, failed, refund';

COMMENT ON TABLE  referral_reward IS 'FAKT. Реферальное вознаграждение (оплата рекламных услуг агента): pending -> accrued -> paid_out; cancelled при возврате';
COMMENT ON COLUMN referral_reward.referral_reward_id   IS 'Ключ начисления';
COMMENT ON COLUMN referral_reward.payment_id           IS 'Оплата, с которой начислено';
COMMENT ON COLUMN referral_reward.beneficiary_agent_id IS 'Получатель вознаграждения';
COMMENT ON COLUMN referral_reward.source_agent_id      IS 'Приглашенный агент, который заплатил';
COMMENT ON COLUMN referral_reward.referral_level_id    IS 'Уровень вложенности (справочник REFERRAL_LEVEL)';
COMMENT ON COLUMN referral_reward.reward_percent       IS 'Процент вознаграждения';
COMMENT ON COLUMN referral_reward.amount               IS 'Сумма вознаграждения';
COMMENT ON COLUMN referral_reward.reward_status        IS 'Статус: pending, accrued, paid_out, cancelled';
COMMENT ON COLUMN referral_reward.date_accrual         IS 'Дата начисления - окончание оплаченного периода';
COMMENT ON COLUMN referral_reward.date_payout          IS 'Когда выплачено на карту агента';
COMMENT ON COLUMN referral_reward.payout_ref           IS 'Номер операции выплаты у платежного провайдера';

-- 5. Сделки и уведомления
COMMENT ON TABLE  deal IS 'FAKT. Сделка - запрос клиента на услугу; статус меняется только по таблице переходов DEAL_TRANSITION';
COMMENT ON COLUMN deal.deal_id             IS 'Ключ сделки';
COMMENT ON COLUMN deal.service_id          IS 'Узел дерева услуг (обычно вариант)';
COMMENT ON COLUMN deal.client_agent_id     IS 'Клиент';
COMMENT ON COLUMN deal.provider_agent_id   IS 'Поставщик - после согласования';
COMMENT ON COLUMN deal.slot_id             IS 'Публикация поставщика, из которой создан запрос';
COMMENT ON COLUMN deal.deal_status_id      IS 'Статус сделки (справочник DEAL_STATUS)';
COMMENT ON COLUMN deal.payment_method_id   IS 'Способ оплаты (справочник PAYMENT_METHOD), сейчас DIRECT';
COMMENT ON COLUMN deal.geo_lat             IS 'Широта точки клиента / точки подачи';
COMMENT ON COLUMN deal.geo_lon             IS 'Долгота точки клиента / точки подачи';
COMMENT ON COLUMN deal.zone_radius_m       IS 'Радиус зоны клиента, м';
COMMENT ON COLUMN deal.geo_to_lat          IS 'Широта точки назначения (geo_mode = route)';
COMMENT ON COLUMN deal.geo_to_lon          IS 'Долгота точки назначения';
COMMENT ON COLUMN deal.route_length_m      IS 'Длина маршрута подача -> назначение по дорогам, м';
COMMENT ON COLUMN deal.period_start        IS 'Начало периода, когда нужна услуга';
COMMENT ON COLUMN deal.period_end          IS 'Окончание периода';
COMMENT ON COLUMN deal.price_forecast_min  IS 'Прогноз цены: нижняя граница';
COMMENT ON COLUMN deal.price_forecast_max  IS 'Прогноз цены: верхняя граница';
COMMENT ON COLUMN deal.client_price        IS 'Цена, которую предлагает клиент ("Моя цена")';
COMMENT ON COLUMN deal.provider_rating_min IS 'Минимальный рейтинг поставщика';
COMMENT ON COLUMN deal.price_provider      IS 'Цена поставщика, фиксируется при согласовании';
COMMENT ON COLUMN deal.fee_amount          IS 'Сбор платформы (deal_fee_percent, сейчас 0)';
COMMENT ON COLUMN deal.price_total         IS 'Итог для клиента: цена поставщика + сбор';
COMMENT ON COLUMN deal.close_secret        IS 'Секрет кода закрытия (HMAC), создается при AGREED, клиенту не отдается';
COMMENT ON COLUMN deal.date_creation       IS 'Когда клиент создал запрос';
COMMENT ON COLUMN deal.date_agreed         IS 'Когда клиент согласовал отклик (окно бесплатной отмены)';
COMMENT ON COLUMN deal.date_close          IS 'Когда сделка перешла в финальный статус';

COMMENT ON TABLE  deal_offer IS 'FAKT. Отклик поставщика на сделку: цена и точка предложения';
COMMENT ON COLUMN deal_offer.deal_offer_id     IS 'Ключ отклика';
COMMENT ON COLUMN deal_offer.deal_id           IS 'Сделка';
COMMENT ON COLUMN deal_offer.provider_agent_id IS 'Поставщик';
COMMENT ON COLUMN deal_offer.offer_price       IS 'Цена предложения';
COMMENT ON COLUMN deal_offer.offer_mode        IS 'Режим: auto (автоотклик) или manual';
COMMENT ON COLUMN deal_offer.geo_lat           IS 'Широта точки предложения (место, машина)';
COMMENT ON COLUMN deal_offer.geo_lon           IS 'Долгота точки предложения';
COMMENT ON COLUMN deal_offer.offer_status      IS 'Статус: active, selected, declined, withdrawn (поставщик занят), expired';
COMMENT ON COLUMN deal_offer.spot_number       IS 'Номер места (частная парковка), выдается при согласовании';
COMMENT ON COLUMN deal_offer.date_creation     IS 'Когда поставщик откликнулся; при равенстве ключей раньше - выше';

COMMENT ON TABLE  deal_attribute IS 'FAKT. Значения полей запроса клиента (deal_offer_id NULL) и отклика поставщика';
COMMENT ON COLUMN deal_attribute.deal_attribute_id IS 'Ключ значения';
COMMENT ON COLUMN deal_attribute.deal_id           IS 'Сделка';
COMMENT ON COLUMN deal_attribute.deal_offer_id     IS 'Отклик; NULL - поле запроса клиента';
COMMENT ON COLUMN deal_attribute.attribute_id      IS 'Атрибут';
COMMENT ON COLUMN deal_attribute.attribute_value   IS 'Значение';
COMMENT ON COLUMN deal_attribute.date_creation     IS 'Когда заполнено';

COMMENT ON TABLE  deal_status_log IS 'FAKT. История статусов сделки; пишет только триггер trg_deal_status_after';
COMMENT ON COLUMN deal_status_log.deal_status_log_id IS 'Ключ записи истории';
COMMENT ON COLUMN deal_status_log.deal_id            IS 'Сделка';
COMMENT ON COLUMN deal_status_log.deal_status_id     IS 'Новый статус';
COMMENT ON COLUMN deal_status_log.agent_id           IS 'Кто изменил статус; NULL - система';
COMMENT ON COLUMN deal_status_log.cancel_reason_id   IS 'Причина отмены (справочник CANCEL_REASON)';
COMMENT ON COLUMN deal_status_log.comment            IS 'Комментарий';
COMMENT ON COLUMN deal_status_log.date_creation      IS 'Когда изменен статус';

COMMENT ON TABLE  deal_review IS 'FAKT. Баллы и отзывы сторон сделки друг другу';
COMMENT ON COLUMN deal_review.deal_id       IS 'Сделка';
COMMENT ON COLUMN deal_review.from_agent_id IS 'Кто оценивает';
COMMENT ON COLUMN deal_review.to_agent_id   IS 'Кого оценивают';
COMMENT ON COLUMN deal_review.to_role_id    IS 'Роль оцениваемого в сделке (ставится триггером)';
COMMENT ON COLUMN deal_review.score         IS 'Балл от 1 до 5';
COMMENT ON COLUMN deal_review.comment       IS 'Отзыв';
COMMENT ON COLUMN deal_review.date_creation IS 'Когда оценено';

COMMENT ON TABLE  notification IS 'FAKT. Уведомления агентам с историей отправки и прочтения';
COMMENT ON COLUMN notification.notification_id IS 'Ключ уведомления';
COMMENT ON COLUMN notification.agent_id        IS 'Получатель';
COMMENT ON COLUMN notification.deal_id         IS 'Сделка, к которой относится уведомление';
COMMENT ON COLUMN notification.notify_type_id  IS 'Тип уведомления (справочник NOTIFY_TYPE), текст - в глоссарии типа';
COMMENT ON COLUMN notification.channel         IS 'Канал: push, sms, email, in_app';
COMMENT ON COLUMN notification.payload         IS 'Параметры уведомления (JSON)';
COMMENT ON COLUMN notification.date_creation   IS 'Когда создано';
COMMENT ON COLUMN notification.date_sent       IS 'Когда отправлено';
COMMENT ON COLUMN notification.date_read       IS 'Когда прочитано';

-- 6. Рейтинг и санкции
COMMENT ON TABLE  rating_history IS 'FAKT. Рейтинг агента по дням в разрезе роли и сервиса: итог = 0,5 x системный + 0,5 x по отзывам';
COMMENT ON COLUMN rating_history.agent_id        IS 'Агент';
COMMENT ON COLUMN rating_history.role_id         IS 'Роль (справочник ROLE)';
COMMENT ON COLUMN rating_history.service_id      IS 'Узел уровня "сервис"';
COMMENT ON COLUMN rating_history.rating_date     IS 'Дата рейтинга';
COMMENT ON COLUMN rating_history.system_rating   IS 'Автоматический рейтинг системы';
COMMENT ON COLUMN rating_history.review_rating   IS 'Рейтинг по отзывам';
COMMENT ON COLUMN rating_history.total_rating    IS 'Итоговый рейтинг (веса - настройки rating_weight_*)';
COMMENT ON COLUMN rating_history.deals_closed    IS 'Закрыто сделок';
COMMENT ON COLUMN rating_history.deals_cancelled IS 'Отменено сделок';

COMMENT ON TABLE  agent_sanction IS 'FAKT. Санкции антифрод-системы: предупреждение, понижение рейтинга, блокировка (неделя, месяц, год)';
COMMENT ON COLUMN agent_sanction.sanction_id   IS 'Ключ санкции';
COMMENT ON COLUMN agent_sanction.agent_id      IS 'Агент';
COMMENT ON COLUMN agent_sanction.deal_id       IS 'Сделка, в которой выявлен эпизод';
COMMENT ON COLUMN agent_sanction.reason_id     IS 'Причина (справочник SANCTION_REASON)';
COMMENT ON COLUMN agent_sanction.action        IS 'Действие: warning, rating_down, block';
COMMENT ON COLUMN agent_sanction.block_level   IS 'Уровень блокировки: 1 - неделя, 2 - месяц, 3 - год';
COMMENT ON COLUMN agent_sanction.date_creation IS 'Когда применена санкция';
COMMENT ON COLUMN agent_sanction.date_start    IS 'Начало блокировки';
COMMENT ON COLUMN agent_sanction.date_end      IS 'Окончание блокировки';

-- Представления
COMMENT ON VIEW v_service_tree                IS 'Каждый узел дерева услуг со всеми предками (depth 0 - сам узел)';
COMMENT ON VIEW v_service_search              IS 'Поиск услуг: название, путь в дереве, синонимы, пиктограмма на языке';
COMMENT ON VIEW v_service_attribute_effective IS 'Поля узла с учетом наследования (ближайший узел побеждает)';
COMMENT ON VIEW v_attribute_label             IS 'Подписи полей и пиктограммы на языке';
COMMENT ON VIEW v_agent_available_service     IS 'Сервисы, доступные агенту по подписке; субагенту - назначенные компанией из ее пакета';
COMMENT ON VIEW v_subscription_charge         IS 'Абонплата за период с субагентами сверх пакета';
COMMENT ON VIEW v_agent_finance_daily         IS 'Доходы и расходы агента по дням в разрезе роли, сервиса и источника';
COMMENT ON VIEW v_company_finance_daily       IS 'Доходы и расходы компании по субагентам';
COMMENT ON VIEW v_deal_offer_list             IS 'Отклики в порядке вывода клиенту; до согласования точка смещена, расстояние - интервалом';
COMMENT ON VIEW v_deal_offer_route            IS 'Ссылки на маршрут во внешних картах - только после согласования';
COMMENT ON VIEW v_deal_candidates             IS 'Поставщики, которым подходит открытая сделка и которые еще не откликнулись';
COMMENT ON VIEW v_provider_slot_map           IS 'Активные публикации поставщиков для карты клиента (точка смещена)';
COMMENT ON VIEW v_ui_options                  IS 'Варианты интерфейса для выпадающего списка: "Шаблон A / Светлая"';
COMMENT ON VIEW v_map_cursor                  IS 'Курсоры карты по роли и режиму тестирования (SVG)';

-- Поля представлений
COMMENT ON COLUMN v_service_tree.service_id  IS 'Узел дерева услуг';
COMMENT ON COLUMN v_service_tree.ancestor_id IS 'Предок узла (включая сам узел)';
COMMENT ON COLUMN v_service_tree.depth       IS 'Расстояние до предка: 0 - сам узел, 1 - родитель ...';

COMMENT ON COLUMN v_service_search.service_id        IS 'Узел дерева услуг';
COMMENT ON COLUMN v_service_search.node_type         IS 'Тип узла: category, service, variant';
COMMENT ON COLUMN v_service_search.parent_service_id IS 'Родительский узел';
COMMENT ON COLUMN v_service_search.interface_lang_id IS 'Язык текстов';
COMMENT ON COLUMN v_service_search.service_name      IS 'Название узла на языке';
COMMENT ON COLUMN v_service_search.service_path      IS 'Путь в дереве: Категория / Сервис / Вариант';
COMMENT ON COLUMN v_service_search.search_text       IS 'Текст для поиска в нижнем регистре: путь и синонимы';
COMMENT ON COLUMN v_service_search.icon_id           IS 'Пиктограмма узла или ближайшего предка';

COMMENT ON COLUMN v_service_attribute_effective.service_id            IS 'Узел дерева, для которого действует поле';
COMMENT ON COLUMN v_service_attribute_effective.attribute_id          IS 'Атрибут';
COMMENT ON COLUMN v_service_attribute_effective.attr_scope            IS 'Где заполняется: profile, request, offer';
COMMENT ON COLUMN v_service_attribute_effective.is_required           IS 'Обязательное поле: 1 - да';
COMMENT ON COLUMN v_service_attribute_effective.show_stage            IS 'Когда видно другой стороне: always, never, agreed, near';
COMMENT ON COLUMN v_service_attribute_effective.sort_order            IS 'Порядок вывода';
COMMENT ON COLUMN v_service_attribute_effective.defined_at_service_id IS 'Узел, на котором поле задано (сам узел или предок)';

COMMENT ON COLUMN v_attribute_label.attribute_id        IS 'Атрибут';
COMMENT ON COLUMN v_attribute_label.attribute_type      IS 'Тип и формат значения';
COMMENT ON COLUMN v_attribute_label.parent_attribute_id IS 'Группа или список, в который входит атрибут';
COMMENT ON COLUMN v_attribute_label.sort_order          IS 'Порядок вывода';
COMMENT ON COLUMN v_attribute_label.interface_lang_id   IS 'Язык подписи';
COMMENT ON COLUMN v_attribute_label.label               IS 'Подпись поля на языке';
COMMENT ON COLUMN v_attribute_label.icon_id             IS 'Пиктограмма поля';
COMMENT ON COLUMN v_attribute_label.icon_code           IS 'Код пиктограммы';

COMMENT ON COLUMN v_agent_available_service.agent_id   IS 'Агент';
COMMENT ON COLUMN v_agent_available_service.service_id IS 'Доступный узел дерева услуг';

COMMENT ON COLUMN v_subscription_charge.subscription_id      IS 'Платный период подписки';
COMMENT ON COLUMN v_subscription_charge.agent_id             IS 'Агент (компания)';
COMMENT ON COLUMN v_subscription_charge.package_id           IS 'Пакет';
COMMENT ON COLUMN v_subscription_charge.price                IS 'Цена пакета';
COMMENT ON COLUMN v_subscription_charge.included_subagents   IS 'Субагентов в цене пакета';
COMMENT ON COLUMN v_subscription_charge.extra_subagent_price IS 'Цена субагента сверх пакета';
COMMENT ON COLUMN v_subscription_charge.subagents            IS 'Активных субагентов у компании';
COMMENT ON COLUMN v_subscription_charge.charge               IS 'Начисление: цена пакета + субагенты сверх включенных';

COMMENT ON COLUMN v_agent_finance_daily.agent_id  IS 'Агент';
COMMENT ON COLUMN v_agent_finance_daily.role_id   IS 'Роль (для сделок); NULL - рефералы и абонплата';
COMMENT ON COLUMN v_agent_finance_daily.service_id IS 'Узел уровня "сервис" (для сделок)';
COMMENT ON COLUMN v_agent_finance_daily.source    IS 'Источник: deal, referral, subscription';
COMMENT ON COLUMN v_agent_finance_daily.direction IS 'Направление: income или expense';
COMMENT ON COLUMN v_agent_finance_daily.fin_date  IS 'Дата операции';
COMMENT ON COLUMN v_agent_finance_daily.cnt       IS 'Число операций';
COMMENT ON COLUMN v_agent_finance_daily.amount    IS 'Сумма операций';

COMMENT ON COLUMN v_company_finance_daily.company_agent_id IS 'Компания';
COMMENT ON COLUMN v_company_finance_daily.subagent_id      IS 'Субагент компании';
COMMENT ON COLUMN v_company_finance_daily.role_id          IS 'Роль (для сделок)';
COMMENT ON COLUMN v_company_finance_daily.service_id       IS 'Узел уровня "сервис"';
COMMENT ON COLUMN v_company_finance_daily.source           IS 'Источник: deal, referral, subscription';
COMMENT ON COLUMN v_company_finance_daily.direction        IS 'Направление: income или expense';
COMMENT ON COLUMN v_company_finance_daily.fin_date         IS 'Дата операции';
COMMENT ON COLUMN v_company_finance_daily.cnt              IS 'Число операций';
COMMENT ON COLUMN v_company_finance_daily.amount           IS 'Сумма операций';

COMMENT ON COLUMN v_deal_offer_list.deal_id           IS 'Сделка';
COMMENT ON COLUMN v_deal_offer_list.service_id        IS 'Узел дерева услуг сделки';
COMMENT ON COLUMN v_deal_offer_list.position          IS 'Позиция в списке клиента по offer_sort_1..3';
COMMENT ON COLUMN v_deal_offer_list.deal_offer_id     IS 'Отклик';
COMMENT ON COLUMN v_deal_offer_list.provider_agent_id IS 'Поставщик';
COMMENT ON COLUMN v_deal_offer_list.pseudonym         IS 'Псевдоним поставщика (имя - после согласования)';
COMMENT ON COLUMN v_deal_offer_list.rating            IS 'Рейтинг поставщика по сервису на дату сделки';
COMMENT ON COLUMN v_deal_offer_list.offer_price       IS 'Цена предложения';
COMMENT ON COLUMN v_deal_offer_list.offer_status      IS 'Статус отклика: active или selected';
COMMENT ON COLUMN v_deal_offer_list.offer_mode        IS 'Режим: auto или manual';
COMMENT ON COLUMN v_deal_offer_list.spot_number       IS 'Номер места (частная парковка)';
COMMENT ON COLUMN v_deal_offer_list.is_exact          IS 'Точка точная (выбранный отклик согласованной сделки)';
COMMENT ON COLUMN v_deal_offer_list.show_lat          IS 'Широта для карты: смещенная до согласования, точная после';
COMMENT ON COLUMN v_deal_offer_list.show_lon          IS 'Долгота для карты';
COMMENT ON COLUMN v_deal_offer_list.distance_m        IS 'Точное расстояние до точки клиента, м - только когда точка точная';
COMMENT ON COLUMN v_deal_offer_list.distance_band     IS 'Интервал расстояния до согласования: 0-50, 50-100, 100-200, 200+';
COMMENT ON COLUMN v_deal_offer_list.marker_label      IS 'Подпись точки на карте: псевдоним · рейтинг · цена';

COMMENT ON COLUMN v_deal_offer_route.deal_id       IS 'Сделка';
COMMENT ON COLUMN v_deal_offer_route.deal_offer_id IS 'Выбранный отклик';
COMMENT ON COLUMN v_deal_offer_route.provider_code IS 'Сервис карт: YANDEX, 2GIS, GOOGLE';
COMMENT ON COLUMN v_deal_offer_route.sort_order    IS 'Порядок кнопок';
COMMENT ON COLUMN v_deal_offer_route.route_url     IS 'Ссылка на маршрут от точки клиента до точного места';

COMMENT ON COLUMN v_deal_candidates.deal_id           IS 'Открытая сделка (MATCHING или OFFERS)';
COMMENT ON COLUMN v_deal_candidates.provider_agent_id IS 'Поставщик, которому подходит сделка и который еще не откликнулся';

COMMENT ON COLUMN v_provider_slot_map.slot_id        IS 'Публикация';
COMMENT ON COLUMN v_provider_slot_map.agent_id       IS 'Поставщик';
COMMENT ON COLUMN v_provider_slot_map.service_id     IS 'Узел дерева услуг';
COMMENT ON COLUMN v_provider_slot_map.pseudonym      IS 'Псевдоним поставщика';
COMMENT ON COLUMN v_provider_slot_map.rating         IS 'Рейтинг поставщика по сервису';
COMMENT ON COLUMN v_provider_slot_map.is_testing     IS 'Поставщик в режиме тестирования (курсор с шестеренкой)';
COMMENT ON COLUMN v_provider_slot_map.price_min      IS 'Минимальная цена по публикации';
COMMENT ON COLUMN v_provider_slot_map.period_start   IS 'Начало окна доступности';
COMMENT ON COLUMN v_provider_slot_map.period_end     IS 'Окончание окна доступности';
COMMENT ON COLUMN v_provider_slot_map.show_lat       IS 'Широта для карты (смещена на approx_offset_m)';
COMMENT ON COLUMN v_provider_slot_map.show_lon       IS 'Долгота для карты (смещена)';
COMMENT ON COLUMN v_provider_slot_map.radius_m       IS 'Радиус точки или коридор маршрута rA, м';
COMMENT ON COLUMN v_provider_slot_map.geo_to_lat     IS 'Широта точки Б маршрута';
COMMENT ON COLUMN v_provider_slot_map.geo_to_lon     IS 'Долгота точки Б маршрута';
COMMENT ON COLUMN v_provider_slot_map.radius_to_m    IS 'Радиус rB вокруг точки Б, м';
COMMENT ON COLUMN v_provider_slot_map.route_polyline IS 'Маршрут А -> Б (encoded polyline)';
COMMENT ON COLUMN v_provider_slot_map.route_length_m IS 'Длина маршрута, м';
COMMENT ON COLUMN v_provider_slot_map.marker_label   IS 'Подпись точки: псевдоним · рейтинг · цена от';

COMMENT ON COLUMN v_ui_options.template_code     IS 'Шаблон интерфейса: A, B, C';
COMMENT ON COLUMN v_ui_options.scheme_code       IS 'Цветовая схема: A_LIGHT, A_DARK ...';
COMMENT ON COLUMN v_ui_options.is_dark           IS 'Темная схема (фильтр "Светлые / Темные")';
COMMENT ON COLUMN v_ui_options.interface_lang_id IS 'Язык подписей';
COMMENT ON COLUMN v_ui_options.template_name     IS 'Название шаблона на языке';
COMMENT ON COLUMN v_ui_options.scheme_name       IS 'Название схемы на языке';
COMMENT ON COLUMN v_ui_options.option_label      IS 'Подпись в выпадающем списке: "Шаблон A / Светлая"';
COMMENT ON COLUMN v_ui_options.sort_order        IS 'Порядок в списке';

COMMENT ON COLUMN v_map_cursor.cursor_code IS 'Код курсора: CLIENT, PROVIDER, CLIENT_TEST, PROVIDER_TEST';
COMMENT ON COLUMN v_map_cursor.icon_code   IS 'Код пиктограммы';
COMMENT ON COLUMN v_map_cursor.icon_svg    IS 'SVG курсора (цвет зашит)';

-- Функции: описание (что делает и методика; то же - комментарием в теле функции)
DO $$
DECLARE
    r record;
BEGIN
    FOR r IN
        SELECT p.oid::regprocedure AS fn, v.descr
        FROM (VALUES
    ('check_dict_type', 'Триггер: проверяет, что значение общего справочника в колонке имеет ожидаемый dict_type (ОВ-22). Аргументы триггера - пары (колонка, тип); значение колонки берется из to_jsonb(NEW), тип ищется в dictionary; несовпадение - исключение.'),
    ('dict_id', 'Возвращает id значения общего справочника по типу и коду. Прямой поиск по уникальной паре (dict_type, dict_code).'),
    ('gloss', 'Текст термина глоссария на языке интерфейса агента; без перевода - на русском (п. 0.5). Ищет glossary_text на языке p_lang, иначе на RUS; строка с непереведённым названием не выпадает из выборки (найдено в Э5: агент на KAZ не видел входящих запросов).'),
    ('geo_distance_m', 'Расстояние между двумя точками по прямой, м. Формула гаверсинуса на сфере радиусом 6 371 км, округление до 0,1 м; при переходе на PostGIS - ST_Distance.'),
    ('offer_sort_key', 'Ключ сортировки отклика по спецификации "поле:направление" (distance/rating/price, asc/desc). Берет значение нужного поля и умножает на -1 для desc, чтобы все ключи сортировать по возрастанию.'),
    ('get_setting', 'Значение настройки сервиса для узла дерева с учетом наследования и рейтинга поставщика. Ищет строки настройки на узле и его предках, затем на платформе; ближайший узел побеждает, при переданном рейтинге сначала строки с подходящим диапазоном.'),
    ('service_base_id', 'Узел уровня "сервис" для любого узла дерева (по нему ведутся рейтинг и отчеты). Поднимается по v_service_tree до ближайшего предка типа service или system.'),
    ('is_load_mode', 'Признак загрузки тестовых данных (mp.load_mode = on). Читает параметр сессии; триггеры процесса в этом режиме не проверяют и не логируют.'),
    ('session_agent_id', 'Агент, от имени которого действует текущая сессия API; NULL - система. Читает параметр сессии mp.agent_id, который задает сервер на каждый запрос.'),
    ('agent_login_check', 'Проверка логина и пароля: возвращает id агента или NULL, фиксирует дату входа. Логин приводится к нижнему регистру; пароль сравнивается с хешем bcrypt через crypt() из pgcrypto; учитываются только активные вход и агент.'),
    ('agent_pseudonym', 'Псевдоним агента (атрибут 16, виден всем). Последнее активное значение атрибута из истории agent_attribute.'),
    ('agent_rating', 'Итоговый рейтинг агента в роли по сервису на дату; без истории - rating_start (В-4). Последняя запись rating_history по узлу уровня "сервис" не позже даты; если ее нет - настройка rating_start.'),
    ('agent_has_rating', 'Есть ли у агента история рейтинга в роли по сервису (иначе пометка "новичок"). Проверка существования записи rating_history по узлу уровня "сервис".'),
    ('service_covers', 'Подключенный узел поставщика покрывает узел сделки. Да, если узел поставщика - предок узла сделки по дереву, или узел сделки входит в настройку accepts_variants (классы авто).'),
    ('period_in_work_time', 'Период сделки помещается в рабочее время; окно может переходить через полночь (П-6). Строит интервалы рабочего окна на день начала периода и на предыдущий день (для ночных окон) и проверяет вхождение tsrange.'),
    ('agent_pending_reviews', 'Число сделок, по которым агент еще не выставил баллы (п. 5.6, В-11). Закрытые сделки - для обеих сторон, отмены со штрафом - для пострадавшей стороны; без записи deal_review от агента.'),
    ('provider_limit', 'Сколько сделок поставщик может держать одновременно по сервису (п. 2.8). Емкость из agent_service, если задана, иначе настройка max_parallel_deals, иначе 1.'),
    ('provider_busy', 'Сколько согласованных сделок поставщика того же сервиса пересекается с периодом. Считает сделки в AGREED/CONTACT с тем же узлом уровня "сервис" и пересечением tsrange, кроме исключаемой.'),
    ('provider_can_offer', 'Может ли поставщик откликнуться на сделку: все условия п. 2.1-2.9 алгоритма; возвращает ok и код причины. Последовательно проверяет: статус сделки, не сам клиент, баллы, доступ к сервису (дерево, классы, подписка), блокировку, рейтинги, окно публикации или рабочее время, гео по place_mode, цену, лимит одновременных сделок; первая невыполненная проверка дает reason. Маршрут такси проверяет сервер.'),
    ('notify_agent', 'Создает уведомление агенту в приложение. Вставка в notification с типом из справочника NOTIFY_TYPE, каналом in_app и временем отправки; пустой получатель пропускается.'),
    ('register_fraud_episode', 'Регистрирует эпизод фрода и применяет санкцию по эскалации (п. 6). По числу активных санкций: первый эпизод - предупреждение, далее - понижение рейтинга; больше двух при подозрительных отзывах И падении выручки - блокировка следующего уровня (неделя, месяц, год). Уведомляет агента и компанию субагента.'),
    ('trg_deal_status_check', 'Триггер BEFORE на deal: допустимость перехода статуса и служебные поля (П-1). Новая сделка - только INIT; переход ищется в DEAL_TRANSITION, исполнитель сверяется с mp.agent_id; закрытие гео-услуги - только из CONTACT; причина обязательна для отмен со штрафом; при AGREED - дата и секрет кода закрытия; финальный статус - дата закрытия.'),
    ('trg_deal_status_log', 'Триггер AFTER на deal: история статусов и уведомления сторонам. Пишет deal_status_log с автором и причиной из параметров сессии; по новому статусу рассылает уведомления (запрос кандидатам, согласование, сближение, баллы, истечение, отмена).'),
    ('trg_deal_offer_check', 'Триггер BEFORE на deal_offer: повторная проверка условий подбора и цены отклика (П-2). Вызывает provider_can_offer с точкой отклика; проверяет цену в пределах настроек поставщика (для км - не ниже порога); отклик получает статус active.'),
    ('trg_period_check', 'Триггер BEFORE INSERT на deal и provider_slot: период, который создает агент, не в прошлом (Р-4). Проверяются только вставки от агента (mp.agent_id задан, не загрузка данных); now() - в поясе сессии Asia/Almaty, как и время API. Запрос - начало не раньше now() - 1 мин, иначе PERIOD_IN_PAST; публикация - окно еще не закончилось (место может быть свободно уже сейчас), иначе PERIOD_IN_PAST; конец не позже начала - PERIOD_INVALID.'),
    ('trg_deal_offer_after', 'Триггер AFTER на deal_offer: первый отклик переводит сделку в OFFERS, клиенту уведомление. Обновляет статус MATCHING -> OFFERS и создает уведомление NEW_OFFER.'),
    ('trg_deal_review_check', 'Триггер BEFORE на deal_review: баллы только сторонами друг другу и только после закрытия или отмены. Проверяет статус сделки и участников, роль оцениваемого проставляет сам.'),
    ('trg_agent_testing_check', 'Триггер на agent: режим тестирования только для роли ADMIN. При is_testing = 1 проверяет наличие активной роли ADMIN.'),
    ('trg_agent_position_check', 'Триггер на agent_position: положение курсором только в режиме тестирования. Для source = manual требует agent.is_testing = 1; ставит время обновления.'),
    ('trg_agent_service_assign_check', 'Триггер на agent_service: сервис субагенту назначает только его компания, из своего пакета и не дольше своей подписки (В-12). Проверяет связь субагент-компания и наличие сервиса в подписке компании; срок назначения обрезается окончанием подписки.'),
    ('price_forecast', 'Прогноз цены услуги в точке (П-5). Нижняя граница - минимальная цена поставщиков рядом, верхняя - медиана их максимумов, медиана - по закрытым сделкам сервиса в ячейке forecast_grid_m за 90 дней (иначе середина диапазона).'),
    ('auto_offer_price', 'Цена автоотклика (В-5). Цена клиента, если она в диапазоне поставщика; иначе медиана прогноза, ограниченная диапазоном и округленная до шага.'),
    ('make_auto_offers', 'Создает автоотклики на сделку всех подходящих поставщиков с is_auto = 1 (п. 3.1). Берет кандидатов из v_deal_candidates, цену считает auto_offer_price, точку - из публикации или настроек; маршрутные сервисы пропускает (их ведет сервер).'),
    ('deal_agree', 'Согласование отклика клиентом в одной транзакции (п. 4.4). Блокирует сделку, проверяет клиента, активность отклика и занятость поставщика; выбранный отклик - selected, остальные - declined с уведомлением OFFER_DECLINED их поставщикам; сделке поставщик, цена и статус AGREED; при исчерпании лимита снимает другие отклики поставщика на это время и уведомляет их клиентов.'),
    ('deal_other_side_late', 'Опаздывает ли другая сторона сделки (для бесплатной отмены, В-3). Период начался, а свежее положение другой стороны дальше near_radius_m от места встречи (точка отклика для place_mode = provider, иначе точка клиента).'),
    ('deal_cancel_mode', 'Каким будет итог отмены сделки агентом прямо сейчас (п. 5.2-5.4): CANCEL_FREE, CANCEL_CLIENT, CANCEL_PROVIDER или NULL - отменить нельзя. До согласования, в окне cancel_free_min до CONTACT или при опоздании другой стороны - бесплатно; иначе штраф стороне-инициатору. Одно правило для deal_cancel и предупреждения в приложении (GET /deals/{id}, поле cancel_mode).'),
    ('deal_cancel', 'Отмена сделки стороной; итоговый статус решает БД (п. 5.2-5.4). Итог берется из deal_cancel_mode (бесплатно или штраф инициатору), для отмен со штрафом причина обязательна; активные отклики истекают; отмена после CONTACT - эпизод фрода NEAR_CANCEL.'),
    ('deal_contact_check', 'Переводит в CONTACT согласованные гео-сделки, стороны которых сблизились (п. 4, В-Э6-1); возвращает номера переведенных сделок. Сделки AGREED агента (клиент или поставщик) или все при NULL, кроме place_mode = online; свежие положения обеих сторон (не старше position_ttl_s) ближе near_radius_m. Переход выполняет система: mp.agent_id на время перевода очищается и восстанавливается. Вызывают сервер после PUT /me/position и run_deal_jobs.'),
    ('run_deal_jobs', 'Плановые задания по сделкам, вызывает сервер раз в минуту от имени системы (п. 4.6, 5.5). Истечение открытых сделок к началу периода; CONTACT при сближении сторон (deal_contact_check по всем сделкам - запасной путь к проверке после PUT /me/position); вопрос "Услуга оказана?" после периода; NOT_CLOSED через сутки с сигналом антифроду, если стороны встречались; закрытие прошедших публикаций. Возвращает счетчики.')
        ) v(name, descr)
        JOIN pg_proc p ON p.proname = v.name
        JOIN pg_namespace n ON n.oid = p.pronamespace AND n.nspname = 'mp_service'
    LOOP
        EXECUTE format('COMMENT ON FUNCTION %s IS %L', r.fn, r.descr);
    END LOOP;
END
$$;

-- =====================================================================
-- ТЕСТОВЫЕ ДАННЫЕ
-- Исторические сделки - на фиксированные даты октября 2026; открытые сделки 6 и 7 и публикации
-- поставщиков - относительно момента загрузки, чтобы сценарии работали в любой день.
-- =====================================================================

SET mp.load_mode = 'on';

INSERT INTO interface_lang (interface_lang_id, language_code, language_name) VALUES
(1,'RUS','Русский'), (2,'ENG','English'), (3,'KAZ','Қазақ'), (4,'DEU','Deutsch');

-- Пиктограммы: контурные 24x24, stroke = currentColor
INSERT INTO icon (icon_id, icon_code, icon_format, icon_svg)
SELECT id, code, 'svg',
       '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" '
       || 'stroke-width="2" stroke-linecap="round" stroke-linejoin="round">' || body || '</svg>'
FROM (VALUES
 (1, 'parking',  '<rect x="4" y="3" width="16" height="18" rx="3"/><path d="M10 17V7h3a3 3 0 0 1 0 6h-3"/>'),
 (2, 'car',      '<path d="M3 13l2-5h14l2 5v4H3z"/><circle cx="7.5" cy="17" r="1.5"/><circle cx="16.5" cy="17" r="1.5"/>'),
 (3, 'box',      '<path d="M3 7l9-4 9 4v10l-9 4-9-4z"/><path d="M3 7l9 4 9-4M12 11v10"/>'),
 (4, 'truck',    '<path d="M2 6h11v10H2zM13 10h5l3 3v3h-8"/><circle cx="6" cy="18" r="2"/><circle cx="17" cy="18" r="2"/>'),
 (5, 'hammer',   '<path d="M14 4l6 6-3 3-6-6zM11 7l-8 8 3 3 8-8"/>'),
 (6, 'wrench',   '<path d="M14.5 5.5a4 4 0 0 0 5 5L11 19a2 2 0 0 1-3-3l8.5-8.5"/>'),
 (7, 'sparkles', '<path d="M12 3v4M12 17v4M3 12h4M17 12h4M6 6l2 2M16 16l2 2M6 18l2-2M16 8l2-2"/>'),
 (8, 'scissors', '<circle cx="6" cy="6" r="3"/><circle cx="6" cy="18" r="3"/><path d="M8.5 8.5L20 20M8.5 15.5L20 4"/>'),
 (9, 'brush',    '<path d="M18 3l3 3-9 9-3-3zM9 12l-4 4a2 2 0 0 0 3 3l4-4"/>'),
 (10,'book',     '<path d="M4 4h6a3 3 0 0 1 3 3v13a2 2 0 0 0-2-2H4zM20 4h-5v16h5z"/>'),
 (11,'home',     '<path d="M3 11l9-8 9 8M5 9v12h14V9"/>'),
 (12,'star4',    '<path d="M12 3l2.5 6.5L21 12l-6.5 2.5L12 21l-2.5-6.5L3 12l6.5-2.5z"/>'),
 (13,'phone',    '<rect x="7" y="2" width="10" height="20" rx="2"/><path d="M11 18h2"/>'),
 (14,'calendar', '<rect x="3" y="5" width="18" height="16" rx="2"/><path d="M3 10h18M8 3v4M16 3v4"/>'),
 (15,'user',     '<circle cx="12" cy="8" r="4"/><path d="M4 21a8 8 0 0 1 16 0"/>'),
 (16,'plate',    '<rect x="2" y="7" width="20" height="10" rx="2"/><path d="M6 12h2M10 12h4M16 12h2"/>'),
 (17,'pin',      '<path d="M12 21s-7-6-7-12a7 7 0 0 1 14 0c0 6-7 12-7 12z"/><circle cx="12" cy="9" r="2.5"/>'),
 (18,'star',     '<path d="M12 3l2.8 5.7 6.2.9-4.5 4.4 1 6.2-5.5-2.9-5.5 2.9 1-6.2L3 9.6l6.2-.9z"/>'),
 (19,'sun',      '<circle cx="12" cy="12" r="4"/><path d="M12 2v2M12 20v2M2 12h2M20 12h2M5 5l1.5 1.5M17.5 17.5L19 19M5 19l1.5-1.5M17.5 6.5L19 5"/>'),
 (20,'moon',     '<path d="M20 14a8 8 0 1 1-10-10 6 6 0 0 0 10 10z"/>'),
 (21,'half',     '<circle cx="12" cy="12" r="9"/><path d="M12 3v18"/>'),
 (22,'card',     '<rect x="2" y="5" width="20" height="14" rx="2"/><path d="M2 10h20"/>'),
 (23,'note',     '<path d="M5 4h14v16H5zM8 8h8M8 12h8M8 16h5"/>'),
 (24,'photo',    '<rect x="3" y="5" width="18" height="14" rx="2"/><circle cx="9" cy="10" r="2"/><path d="M21 17l-5-5-8 8"/>'),
 (25,'clock',    '<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/>'),
 (26,'weight',   '<path d="M6 8h12l2 12H4zM9 8a3 3 0 0 1 6 0"/>'),
 (27,'people',   '<circle cx="9" cy="8" r="3"/><circle cx="17" cy="9" r="2.5"/><path d="M3 20a6 6 0 0 1 12 0M15 20a4 4 0 0 1 6-3.5"/>'),
 (32,'shield',   '<path d="M12 3l8 3v6c0 5-3.5 8-8 9-4.5-1-8-4-8-9V6z"/>'),
 (33,'qr',       '<rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><path d="M14 14h3v3h-3zM20 14v.01M14 20h.01M17 20h4v-3"/>'),
 (34,'gear',     '<circle cx="12" cy="12" r="3"/><path d="M12 2v3M12 19v3M2 12h3M19 12h3M4.9 4.9l2.1 2.1M17 17l2.1 2.1M4.9 19.1L7 17M17 7l2.1-2.1"/>'),
 (35,'layout',   '<rect x="3" y="3" width="18" height="18" rx="2"/><path d="M3 9h18M9 21V9"/>')
) v(id, code, body);

-- Курсоры карты: "капля" и "капля с шестеренкой" (режим тестирования). Цвет зашит в SVG:
-- клиент - синий, поставщик - оранжевый. Сменить курсор = загрузить новую пиктограмму.
INSERT INTO icon (icon_id, icon_code, icon_format, icon_svg)
SELECT id, code, 'svg',
       '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 32">'
       || '<path d="M12 31s-10-10.2-10-18.5a10 10 0 0 1 20 0C22 20.8 12 31 12 31z" fill="' || color
       || '" stroke="#FFFFFF" stroke-width="1.6"/>'
       || CASE WHEN gear THEN
              '<g transform="translate(12 12.5)" fill="#FFFFFF">'
              || '<circle r="4"/>'
              || '<rect x="-1.2" y="-6.3" width="2.4" height="3" rx="0.6"/>'
              || '<rect x="-1.2" y="-6.3" width="2.4" height="3" rx="0.6" transform="rotate(60)"/>'
              || '<rect x="-1.2" y="-6.3" width="2.4" height="3" rx="0.6" transform="rotate(120)"/>'
              || '<rect x="-1.2" y="-6.3" width="2.4" height="3" rx="0.6" transform="rotate(180)"/>'
              || '<rect x="-1.2" y="-6.3" width="2.4" height="3" rx="0.6" transform="rotate(240)"/>'
              || '<rect x="-1.2" y="-6.3" width="2.4" height="3" rx="0.6" transform="rotate(300)"/>'
              || '<circle r="1.7" fill="' || color || '"/></g>'
          ELSE '<circle cx="12" cy="12.5" r="4" fill="#FFFFFF"/>' END
       || '</svg>'
FROM (VALUES
 (28,'cursor_client',        '#2563EB', false),
 (29,'cursor_provider',      '#EA580C', false),
 (30,'cursor_client_test',   '#2563EB', true),
 (31,'cursor_provider_test', '#EA580C', true)
) v(id, code, color, gear);

-- Глоссарий. Диапазоны glossary_id: 10000+ сервисы (10000 + service_id), 20000+ атрибуты,
-- 30000+ общий справочник (30000 + dict_id), 40000+ пакеты.
CREATE TEMP TABLE g_src (id numeric, code text, icon numeric, rus text, eng text, syn text);
INSERT INTO g_src VALUES
-- дерево услуг
(10001,'SVC_PROFILE',   15,'Базовый профиль','Base profile',NULL),
(10010,'CAT_TRANSPORT',  2,'Транспорт и авто','Transport and cars','машина авто'),
(10020,'CAT_DELIVERY',   3,'Доставка и перевозки','Delivery and moving','отправить перевезти'),
(10030,'CAT_HOME',      11,'Дом и ремонт','Home and repair','квартира дом'),
(10040,'CAT_BEAUTY',    12,'Красота','Beauty','салон'),
(10050,'CAT_EDU',       10,'Обучение','Education','учеба'),
(10100,'SVC_PARKING',    1,'Парковка','Parking','парковочное место стоянка припарковаться'),
(10110,'SVC_TAXI',       2,'Такси','Taxi','подвезти поездка водитель'),
(10101,'SVC_COURIER',    3,'Курьер / доставка','Courier / delivery','доставить посылка'),
(10120,'SVC_CARGO',      4,'Грузоперевозки','Freight','переезд грузовое такси газель'),
(10130,'SVC_HANDYMAN',   5,'Разнорабочий','Handyman','грузчик подсобник помощник'),
(10140,'SVC_MASTER',     6,'Мастер на час','Repairs','ремонт починить'),
(10150,'SVC_CLEANING',   7,'Уборка','Cleaning','клининг мытье'),
(10160,'SVC_HAIR',       8,'Парикмахерская','Hairdresser','стрижка парикмахер барбер прическа'),
(10170,'SVC_NAILS',      9,'Маникюр','Manicure','ногти педикюр'),
(10180,'SVC_TUTOR',     10,'Репетитор','Tutor','учитель занятия уроки'),
(11001,'VAR_PARKING_HOUR', NULL,'Почасовая парковка','Hourly parking',NULL),
(11002,'VAR_PARKING_NIGHT',NULL,'Парковка на ночь','Overnight parking',NULL),
(11101,'VAR_TAXI_ECONOMY', NULL,'Эконом','Economy',NULL),
(11102,'VAR_TAXI_COMFORT', NULL,'Комфорт','Comfort',NULL),
(11103,'VAR_TAXI_COMFORT_PLUS',NULL,'Комфорт+','Comfort+',NULL),
(11104,'VAR_TAXI_BUSINESS',NULL,'Бизнес','Business',NULL),
(10105,'SVC_PRIVATE_PARKING',1,'Частная парковка','Private parking','паркинг стоянка платная'),
(11051,'VAR_PRIVATE_HOUR', NULL,'Место на час','Space per hour',NULL),
(11011,'VAR_COURIER_DOCS', NULL,'Документы и мелкие посылки','Documents and small parcels',NULL),
(11012,'VAR_COURIER_FOOD', NULL,'Продукты из магазина','Groceries',NULL),
(11201,'VAR_CARGO_MOVE',   NULL,'Переезд квартиры','Apartment move',NULL),
(11301,'VAR_HANDY_LOAD',   NULL,'Погрузка и разгрузка','Loading and unloading',NULL),
(11401,'VAR_MASTER_ELEC',  NULL,'Электрик','Electrician','розетка проводка свет'),
(11402,'VAR_MASTER_PLUMB', NULL,'Сантехник','Plumber','кран труба засор'),
(11501,'VAR_CLEAN_FLAT',   NULL,'Уборка квартиры','Apartment cleaning',NULL),
(11601,'VAR_HAIR_MEN',     NULL,'Мужская стрижка','Men''s haircut',NULL),
(11602,'VAR_HAIR_WOMEN',   NULL,'Женская стрижка','Women''s haircut',NULL),
(11701,'VAR_NAILS_GEL',    NULL,'Маникюр с покрытием','Gel manicure',NULL),
(11801,'VAR_TUTOR_MATH',   NULL,'Математика','Mathematics',NULL),
(11802,'VAR_TUTOR_ENG',    NULL,'Английский язык','English',NULL),
-- атрибуты
(20001,'ATR_SURNAME',     15,'Фамилия','Surname',NULL),
(20002,'ATR_NAME',        15,'Имя','Name',NULL),
(20003,'ATR_PATRONYMIC',  15,'Отчество','Patronymic',NULL),
(20004,'ATR_MOBILE',      13,'Моб. тел.','Mobile',NULL),
(20005,'ATR_BIRTHDATE',   14,'Дата рождения','Date of birth',NULL),
(20006,'ATR_CARD',        22,'Банковская карта','Bank card',NULL),
(20007,'ATR_CARD_EXP',    14,'Срок действия карты','Card expiry',NULL),
(20008,'ATR_THEME',     NULL,'Стиль оформления','Theme',NULL),
(20009,'ATR_THEME_LIGHT', 19,'Светлый','Light',NULL),
(20010,'ATR_THEME_DARK',  20,'Темный','Dark',NULL),
(20011,'ATR_THEME_GRAY',  21,'Серый','Gray',NULL),
(20012,'ATR_GRP_CLIENT',  15,'Карточка клиента','Client card',NULL),
(20013,'ATR_GRP_CARDS',   22,'Карты оплаты','Payment cards',NULL),
(20014,'ATR_CAR_NUMBER',  16,'Гос. номер авто','Licence plate',NULL),
(20015,'ATR_ADDRESS',     17,'Адрес','Address',NULL),
(20016,'ATR_PSEUDONYM',   15,'Псевдоним','Nickname',NULL),
(20017,'ATR_TASK',        23,'Описание задачи','Task description',NULL),
(20018,'ATR_CARGO_KG',    26,'Вес груза, кг','Cargo weight, kg',NULL),
(20019,'ATR_LOADERS',     27,'Количество грузчиков','Number of loaders',NULL),
(20020,'ATR_AREA_M2',     11,'Площадь, м²','Area, m²',NULL),
(20021,'ATR_CAR_MODEL',    2,'Марка и цвет авто','Car model and colour',NULL),
(20022,'ATR_PASSENGERS',  27,'Количество пассажиров','Passengers',NULL),
(20023,'ATR_LESSON_TOPIC',10,'Тема занятия','Lesson topic',NULL),
(20024,'ATR_WORK_PHOTO',  24,'Фото работ','Portfolio photos',NULL),
(20025,'ATR_ETA_MIN',     25,'Время подачи, мин','Arrival time, min',NULL),
(20026,'ATR_EXPERIENCE',  18,'Опыт работы','Experience',NULL),
-- общий справочник
(30001,'ROLE_CLIENT',   15,'Клиент','Client',NULL),
(30002,'ROLE_PROVIDER', 18,'Поставщик услуг','Service provider',NULL),
(30003,'ROLE_COMPANY',  27,'Компания','Company',NULL),
(30004,'ROLE_SUBAGENT', 15,'Субагент','Subagent',NULL),
(30005,'ROLE_ADMIN',    32,'Администратор','Administrator',NULL),
(30021,'ST_NOT_CLOSED', NULL,'Не закрыта','Not closed',NULL),
(30091,'NT_NEW_REQUEST',    NULL,'Новый запрос рядом','New request nearby',NULL),
(30092,'NT_OFFER_WITHDRAWN',NULL,'Отклик снят: поставщик занят','Offer withdrawn: provider is busy',NULL),
(30093,'NT_CLOSE_REQUEST',  33,'Клиент закрывает сделку - покажите код','The client is closing the deal - show the code',NULL),
(30094,'NT_CLOSE_CODE',     33,'Код закрытия получен','Closing code received',NULL),
(30095,'NT_CLOSING_ASK',    NULL,'Услуга оказана? Закройте сделку','Was the service provided? Close the deal',NULL),
(30096,'NT_REVIEW_REQUIRED',18,'Поставьте баллы второй стороне','Rate the other side',NULL),
(30097,'NT_EXPIRED',        NULL,'Сделка истекла без согласования','The deal expired without agreement',NULL),
(30098,'NT_SANCTION',       32,'Санкция антифрод-системы','Anti-fraud sanction',NULL),
(30099,'NT_OFFER_DECLINED', NULL,'Клиент выбрал другого поставщика','The client chose another provider',NULL),
(30101,'CUR_CLIENT',        28,'Курсор клиента','Client cursor',NULL),
(30102,'CUR_PROVIDER',      29,'Курсор поставщика','Provider cursor',NULL),
(30103,'CUR_CLIENT_TEST',   30,'Курсор клиента (тест)','Client cursor (test)',NULL),
(30104,'CUR_PROVIDER_TEST', 31,'Курсор поставщика (тест)','Provider cursor (test)',NULL),
(30111,'UI_TPL_A',          35,'Шаблон A','Template A',NULL),
(30112,'UI_TPL_B',          35,'Шаблон B','Template B',NULL),
(30113,'UI_TPL_C',          35,'Шаблон C','Template C',NULL),
(30121,'UI_SCHEME_LIGHT',   19,'Светлая','Light',NULL),
(30122,'UI_SCHEME_DARK',    20,'Тёмная','Dark',NULL),
(30200,'DEAL_TRANSITION', NULL,'Переход статуса сделки','Deal status transition',NULL),
(30011,'ST_INIT',     NULL,'Инициирована','Initiated',NULL),
(30012,'ST_MATCHING', NULL,'Подбор','Matching',NULL),
(30013,'ST_OFFERS',   NULL,'Есть отклики','Offers received',NULL),
(30014,'ST_AGREED',   NULL,'Согласована','Agreed',NULL),
(30015,'ST_CONTACT',  NULL,'Контакт','Contact',NULL),
(30016,'ST_CLOSED',   NULL,'Закрыта','Closed',NULL),
(30017,'ST_CANCEL_FREE',    NULL,'Отменена без штрафа','Cancelled, no penalty',NULL),
(30018,'ST_CANCEL_CLIENT',  NULL,'Отменена клиентом','Cancelled by client',NULL),
(30019,'ST_CANCEL_PROVIDER',NULL,'Отменена поставщиком','Cancelled by provider',NULL),
(30020,'ST_EXPIRED',  NULL,'Истекла','Expired',NULL),
(30031,'CR_PLANS',    NULL,'Изменились планы','Plans changed',NULL),
(30032,'CR_LATE',     NULL,'Не могу оказать услугу в срок','Cannot make it in time',NULL),
(30033,'CR_SILENT',   NULL,'Другая сторона не выходит на связь','The other side does not respond',NULL),
(30034,'CR_OTHER',    NULL,'Другое (комментарий)','Other (comment)',NULL),
(30041,'MAP_OSM',     17,'© участники OpenStreetMap','© OpenStreetMap contributors',NULL),
(30045,'MAP_OSM_OFFLINE_ALMATY',17,'Офлайн-карта Алматы · © участники OpenStreetMap','Almaty offline map · © OpenStreetMap contributors',NULL),
(30042,'MAP_YANDEX',  17,'Яндекс Карты','Yandex Maps',NULL),
(30043,'MAP_2GIS',    17,'2ГИС','2GIS',NULL),
(30044,'MAP_GOOGLE',  17,'Google Maps','Google Maps',NULL),
(30051,'REF_L1',      NULL,'Уровень 1','Level 1',NULL),
(30052,'REF_L2',      NULL,'Уровень 2','Level 2',NULL),
(30053,'REF_L3',      NULL,'Уровень 3','Level 3',NULL),
(30061,'PAY_DIRECT',  22,'Напрямую между агентами','Directly between agents',NULL),
(30062,'PAY_PLATFORM',22,'Через счет платформы','Via platform account',NULL),
(30071,'NT_NEW_OFFER',NULL,'Новый отклик','New offer',NULL),
(30072,'NT_AGREED',   NULL,'Сделка согласована','Deal agreed',NULL),
(30073,'NT_CANCELLED',NULL,'Сделка отменена','Deal cancelled',NULL),
(30074,'NT_NEAR',     NULL,'Вторая сторона рядом','The other side is near',NULL),
(30075,'NT_SUB_END',  NULL,'Заканчивается подписка','Subscription ends soon',NULL),
(30081,'SR_BYPASS_GEO',  NULL,'Встреча по гео-меткам без закрытой сделки','Meeting by geo without a closed deal',NULL),
(30082,'SR_NEAR_CANCEL', NULL,'Отмена после показа гос. номеров вблизи','Cancel after plates shown nearby',NULL),
(30083,'SR_FAKE_REVIEWS',NULL,'Подозрительно положительные отзывы','Suspicious positive reviews',NULL),
(30084,'SR_FAKE_REFERRALS',NULL,'Фиктивные рефералы','Fake referrals',NULL),
(30085,'SR_REVENUE_DROP',NULL,'Недополученная выручка больше принесенной','Lost revenue exceeds earned',NULL),
-- пакеты
(40001,'PKG_CLIENT',       15,'Клиент: бесплатно','Client: free',NULL),
(40002,'PKG_PROVIDER_MOVE', 2,'Поставщик: транспорт и доставка','Provider: transport and delivery',NULL),
(40003,'PKG_PROVIDER_ALL', 18,'Поставщик: все услуги','Provider: all services',NULL),
(40004,'PKG_COMPANY_5',    27,'Компания + 5 субагентов','Company + 5 subagents',NULL),
(40005,'PKG_COMPANY_10',   27,'Компания + 10 субагентов','Company + 10 subagents',NULL),
-- тексты интерфейса приложения (ключ ui.*); {n}, {s} - подстановки
(50001,'ui.app_name',         NULL,'Маркетплейс услуг','Services marketplace',NULL),
(50002,'ui.login_title',      NULL,'Выберите тестового агента','Choose a test agent',NULL),
(50003,'ui.login_hint',       NULL,'Вход без пароля - только для разработки','Password-free login - development only',NULL),
(50004,'ui.settings',         34,'Настройки','Settings',NULL),
(50005,'ui.interface',        35,'Интерфейс','Interface',NULL),
(50006,'ui.scheme_filter',    NULL,'Схема','Scheme',NULL),
(50007,'ui.scheme_all',       NULL,'Все','All',NULL),
(50008,'ui.scheme_light',     19,'Светлые','Light',NULL),
(50009,'ui.scheme_dark',      20,'Тёмные','Dark',NULL),
(50010,'ui.language',         NULL,'Язык','Language',NULL),
(50011,'ui.search_hint',      NULL,'Какая услуга нужна?','What service do you need?',NULL),
(50012,'ui.categories',       NULL,'Категории','Categories',NULL),
(50013,'ui.popular',          NULL,'Популярное','Popular',NULL),
(50014,'ui.active_deals',     NULL,'Мои сделки','My deals',NULL),
(50015,'ui.no_deals',         NULL,'Активных сделок нет','No active deals',NULL),
(50016,'ui.free_places',      17,'Свободно рядом','Available nearby',NULL),
(50017,'ui.new_request',      NULL,'Новый запрос','New request',NULL),
(50018,'ui.where_when',       NULL,'Где и когда','Where and when',NULL),
(50019,'ui.point',            17,'Точка','Point',NULL),
(50020,'ui.pickup',           17,'Откуда','From',NULL),
(50021,'ui.destination',      17,'Куда','To',NULL),
(50022,'ui.period_from',      25,'С','From',NULL),
(50023,'ui.period_to',        25,'По','Until',NULL),
(50024,'ui.radius',           NULL,'Радиус зоны','Zone radius',NULL),
(50025,'ui.provider_rating_min',18,'Рейтинг поставщика от','Provider rating from',NULL),
(50026,'ui.my_price',         NULL,'Моя цена','My price',NULL),
(50027,'ui.forecast',         NULL,'Прогноз цены','Price forecast',NULL),
(50028,'ui.estimate',         NULL,'Оценка поездки','Trip estimate',NULL),
(50029,'ui.find_providers',   NULL,'Найти поставщиков','Find providers',NULL),
(50030,'ui.next',             NULL,'Далее','Next',NULL),
(50031,'ui.back',             NULL,'Назад','Back',NULL),
(50032,'ui.offers',           NULL,'Отклики','Offers',NULL),
(50033,'ui.offers_count',     NULL,'Откликов: {n}','Offers: {n}',NULL),
(50034,'ui.list',             NULL,'Список','List',NULL),
(50035,'ui.map',              17,'Карта','Map',NULL),
(50036,'ui.agree',            NULL,'Согласовать','Agree',NULL),
(50037,'ui.approx_point',     NULL,'До согласования место показано примерно','The place is approximate until agreed',NULL),
(50038,'ui.newbie',           NULL,'новичок','newcomer',NULL),
(50039,'ui.deal',             NULL,'Сделка','Deal',NULL),
(50040,'ui.route',            17,'Маршрут','Route',NULL),
(50041,'ui.contacts_near',    NULL,'Откроется, когда вы будете рядом','Opens when you are close',NULL),
(50042,'ui.close_deal',       33,'Закрыть сделку','Close the deal',NULL),
(50043,'ui.cancel_deal',      NULL,'Отменить','Cancel',NULL),
(50044,'ui.cancel_reason',    NULL,'Причина отмены','Cancellation reason',NULL),
(50045,'ui.cancel_free',      NULL,'Отмена без штрафа','Free cancellation',NULL),
(50046,'ui.cancel_penalty',   NULL,'Отмена снизит ваш рейтинг','Cancelling will lower your rating',NULL),
(50047,'ui.waiting_code',     NULL,'Ждём код от поставщика','Waiting for the provider''s code',NULL),
(50048,'ui.show_code',        33,'Покажите код клиенту','Show the code to the client',NULL),
(50049,'ui.code_expires',     NULL,'Код обновится через {s} с','The code refreshes in {s} s',NULL),
(50050,'ui.scan_qr',          33,'Сканировать QR','Scan QR',NULL),
(50051,'ui.enter_key',        NULL,'Ввести ключ','Enter the key',NULL),
(50052,'ui.provide_qr',       33,'Предоставить QR','Provide QR',NULL),
(50053,'ui.review_title',     18,'Оцените вторую сторону','Rate the other side',NULL),
(50054,'ui.review_comment',   23,'Комментарий','Comment',NULL),
(50055,'ui.send',             NULL,'Отправить','Send',NULL),
(50056,'ui.role_client',      15,'Клиент','Client',NULL),
(50057,'ui.role_provider',    18,'Поставщик','Provider',NULL),
(50058,'ui.my_slots',         NULL,'Мои публикации','My listings',NULL),
(50059,'ui.publish',          NULL,'Опубликовать','Publish',NULL),
(50060,'ui.slot_window',      25,'Окно доступности','Availability window',NULL),
(50061,'ui.price_min',        NULL,'Минимальная цена','Minimum price',NULL),
(50062,'ui.incoming',         NULL,'Запросы','Requests',NULL),
(50063,'ui.respond',          NULL,'Откликнуться','Respond',NULL),
(50064,'ui.skip',             NULL,'Пропустить','Skip',NULL),
(50065,'ui.recommended_price',NULL,'Рекомендуемая цена','Recommended price',NULL),
(50066,'ui.auto_offer',       NULL,'Автоотклик','Auto-response',NULL),
(50067,'ui.service_settings', 34,'Настройки сервиса','Service settings',NULL),
(50068,'ui.min_fare',         NULL,'Порог стоимости','Minimum fare',NULL),
(50069,'ui.price_per_km',     NULL,'Цена за км','Price per km',NULL),
(50070,'ui.profile',          15,'Профиль','Profile',NULL),
(50071,'ui.rating',           18,'Рейтинг','Rating',NULL),
(50072,'ui.testing_mode',     34,'Режим тестирования','Testing mode',NULL),
(50073,'ui.testing_hint',     NULL,'Перетащите свой курсор, чтобы изменить положение','Drag your cursor to change your position',NULL),
(50074,'ui.error_network',    NULL,'Нет связи с сервером','No connection to the server',NULL),
(50075,'ui.retry',            NULL,'Повторить','Retry',NULL),
(50076,'ui.notifications',    NULL,'Уведомления','Notifications',NULL),
(50077,'ui.status',           NULL,'Статус','Status',NULL),
(50078,'ui.unit_min',         NULL,'мин','min',NULL),
(50079,'ui.unit_m',           NULL,'м','m',NULL),
(50080,'ui.unit_km',          NULL,'км','km',NULL),
(50081,'ui.currency',         NULL,'тг','KZT',NULL),
(50082,'ui.per_hour',         NULL,'за час','per hour',NULL),
(50083,'ui.per_km',           NULL,'за км','per km',NULL),
(50084,'ui.service_done',     NULL,'Услуга оказана?','Service provided?',NULL),
(50085,'ui.logout',           NULL,'Сменить агента','Switch agent',NULL),
(50086,'ui.choose_deal',      NULL,'Выберите сделку','Choose a deal',NULL),
(50087,'ui.closed_ok',        NULL,'Сделка закрыта','Deal closed',NULL),
(50088,'ui.exact_point',      17,'Точное место','Exact place',NULL),
(50089,'ui.class',            2,'Класс','Class',NULL),
(50090,'ui.spot_number',      1,'Номер места','Space number',NULL),
-- Э4: экраны клиента
(50091,'ui.home',             NULL,'Главная','Home',NULL),
(50092,'ui.role_admin',       NULL,'Администратор','Administrator',NULL),
(50093,'ui.role_company',     NULL,'Компания','Company',NULL),
(50094,'ui.role_subagent',    NULL,'Субагент','Subagent',NULL),
(50095,'ui.waiting_offers',   NULL,'Ждём отклики поставщиков','Waiting for provider offers',NULL),
(50096,'ui.service',          NULL,'Услуга','Service',NULL),
(50097,'ui.variant',          NULL,'Вариант','Option',NULL),
(50098,'ui.choose_point',     NULL,'Нажмите на карту, чтобы выбрать место','Tap the map to choose the place',NULL),
(50099,'ui.any',              NULL,'Любой','Any',NULL),
(50100,'ui.price',            NULL,'Цена','Price',NULL),
(50101,'ui.distance',         NULL,'Расстояние','Distance',NULL),
(50102,'ui.pending_reviews',  NULL,'Оцените завершённые сделки','Rate your finished deals',NULL),
(50103,'ui.counterpart',      NULL,'Вторая сторона','Other side',NULL),
(50104,'ui.request_fields',   NULL,'Детали запроса','Request details',NULL),
(50105,'ui.open_route',       NULL,'Открыть маршрут','Open route',NULL),
(50106,'ui.score',            NULL,'Оценка','Score',NULL),
(50107,'ui.err.review_required',     NULL,'Сначала оцените прошлые сделки','Rate your previous deals first',NULL),
(50108,'ui.err.no_service_access',   NULL,'Услуга недоступна по вашей подписке','This service is not available in your subscription',NULL),
(50109,'ui.err.required_fields',     NULL,'Заполните обязательные поля','Fill in the required fields',NULL),
(50110,'ui.err.offer_not_active',    NULL,'Отклик уже недоступен','This offer is no longer available',NULL),
(50111,'ui.err.busy',                NULL,'Поставщик уже занят на это время','The provider is busy at this time',NULL),
(50112,'ui.err.routing_unavailable', NULL,'Маршруты временно недоступны','Routing is temporarily unavailable',NULL),
(50113,'ui.err.no_route',            NULL,'Не удалось построить маршрут','Could not build a route',NULL),
(50114,'ui.err.rule',                NULL,'Действие сейчас недоступно','This action is not available now',NULL),
-- Э5: время по Алматы (Р-4)
(50115,'ui.err.period_in_past',      NULL,'Начало периода уже прошло - выберите время позже','The start time has already passed - choose a later time',NULL),
(50116,'ui.err.period_invalid',      NULL,'Окончание должно быть позже начала','The end must be after the start',NULL),
(50117,'ui.time_almaty',             25,'Время - по Алматы','Times are in Almaty time',NULL),
-- Э5: экраны поставщика
(50118,'ui.provider',          NULL,'Поставщик','Provider',NULL),
(50119,'ui.my_services',       NULL,'Мои сервисы','My services',NULL),
(50120,'ui.no_slots',          NULL,'Активных публикаций нет','No active listings',NULL),
(50121,'ui.no_requests',       NULL,'Подходящих запросов пока нет','No matching requests yet',NULL),
(50122,'ui.slot_point',        NULL,'Место','Place',NULL),
(50123,'ui.radius_a',          NULL,'Радиус у точки А','Radius at point A',NULL),
(50124,'ui.radius_b',          NULL,'Радиус у точки Б','Radius at point B',NULL),
(50125,'ui.remove_slot',       NULL,'Снять','Withdraw',NULL),
(50126,'ui.offer_sent',        NULL,'Отклик отправлен','Offer sent',NULL),
(50127,'ui.waiting_client',    NULL,'Ждём решения клиента','Waiting for the client''s decision',NULL),
(50128,'ui.offer_declined',    NULL,'Клиент выбрал другого поставщика','The client chose another provider',NULL),
(50129,'ui.work_time',         25,'Рабочее время','Working hours',NULL),
(50130,'ui.price_max',         NULL,'Максимальная цена','Maximum price',NULL),
(50131,'ui.client_rating_min', 18,'Рейтинг клиента от','Client rating from',NULL),
(50132,'ui.capacity',          NULL,'Мест','Spaces',NULL),
(50133,'ui.on_the_way',        NULL,'По пути','On your way',NULL),
(50134,'ui.saved',             NULL,'Сохранено','Saved',NULL),
(50135,'ui.err.not_on_the_way',      NULL,'Поездка не по вашему маршруту','This trip is not on your route',NULL),
(50136,'ui.err.cannot_offer',        NULL,'Откликнуться на этот запрос нельзя','You cannot respond to this request',NULL),
(50137,'ui.err.not_found',           NULL,'Не найдено - возможно, уже снято','Not found - it may have been removed',NULL),
(50138,'ui.err.price_out_of_range',  NULL,'Цена вне ваших настроек сервиса','The price is outside your service settings',NULL),
-- Э5 шаг 6: статусы публикации (provider_slot.slot_status) и форма
(50139,'ui.slot_closed',       NULL,'Завершена','Ended',NULL),
(50140,'ui.slot_cancelled',    NULL,'Снята','Withdrawn',NULL),
(50141,'ui.route_preview',     NULL,'Линия маршрута построится по дорогам после публикации','The road route is built after publishing',NULL),
-- Э5 шаг 8: отклик снят (offer_status = withdrawn / expired) - сделка открыта, но своего активного отклика нет
(50142,'ui.offer_withdrawn',   NULL,'Отклик снят: время занято другой сделкой или истекло','Offer withdrawn: the time is taken by another deal or has expired',NULL),
-- Э5 шаг 9: настройки сервиса поставщика
(50143,'ui.save',               NULL,'Сохранить','Save',NULL),
(50144,'ui.service_point',      17,'Точка обслуживания','Service point',NULL),
(50145,'ui.trip_min',           NULL,'Порог стоимости поездки','Minimum trip fare',NULL),
(50146,'ui.price_per_hour',     NULL,'Цена за час','Price per hour',NULL),
(50147,'ui.no_changes',         NULL,'Изменений нет','No changes',NULL),
(50148,'ui.err.check_fields',   NULL,'Проверьте выделенные поля: время - ЧЧ:ММ, рейтинг - от 0 до 5','Check the highlighted fields: time as HH:MM, rating from 0 to 5',NULL),
(50149,'ui.err.price_min_max',  NULL,'Минимальная цена больше максимальной','The minimum price is above the maximum',NULL),
-- Э6: положение и закрытие сделки (термины 50042-50052, 50073, 50087 - с Э1)
(50150,'ui.open_code',          33,'Показать код','Show code',NULL),
(50151,'ui.scan_camera',        33,'Сканировать камерой','Scan with camera',NULL),
(50152,'ui.code_sent',          NULL,'Код отправлен клиенту','Code sent to the client',NULL),
(50153,'ui.code_hint',          NULL,'Введите цифры с экрана поставщика','Enter the digits from the provider''s screen',NULL),
(50154,'ui.confirm_code',       NULL,'Подтвердить','Confirm',NULL),
(50155,'ui.distance_to_other',  NULL,'До другой стороны {m} м, контакт - ближе {r} м','To the other side {m} m, contact within {r} m',NULL),
(50156,'ui.my_position',        NULL,'Моё положение','My position',NULL),
(50157,'ui.gps_needed',         NULL,'Разрешите доступ к геолокации, чтобы отметить встречу','Allow location access to register the meeting',NULL),
(50158,'ui.gps_denied',         NULL,'Геолокация запрещена - положение не отправляется','Location access denied - position is not sent',NULL),
(50159,'ui.position_unknown',   NULL,'Положение неизвестно','Position unknown',NULL),
(50160,'ui.err.code_invalid',   NULL,'Неверный код','Invalid code',NULL),
(50161,'ui.err.code_expired',   NULL,'Код устарел - попросите показать новый','The code has expired - ask for a new one',NULL),
(50162,'ui.err.contact_required',NULL,'Закрыть сделку можно, когда вы рядом с другой стороной','You can close the deal when you are near the other side',NULL),
(50163,'ui.err.deal_not_closable',NULL,'Закрыть можно только согласованную сделку','Only an agreed deal can be closed',NULL),
(50164,'ui.err.not_testing',    NULL,'Доступно только в режиме тестирования','Available in testing mode only',NULL),
(50165,'ui.err.scanner_unavailable',NULL,'Сканер недоступен - введите цифры','Scanner unavailable - enter the digits',NULL);

INSERT INTO glossary (glossary_id, glossary_code, icon_id) SELECT id, code, icon FROM g_src;
INSERT INTO glossary_text (glossary_id, interface_lang_id, text_value, search_text)
SELECT id, 1, rus, syn FROM g_src
UNION ALL
SELECT id, 2, eng, NULL FROM g_src WHERE eng IS NOT NULL;
INSERT INTO glossary_text (glossary_id, interface_lang_id, text_value) VALUES
(10010,3,'Көлік'), (10020,3,'Жеткізу'), (10030,3,'Үй және жөндеу'), (10040,3,'Сұлулық'), (10050,3,'Білім'),
(10100,3,'Тұрақ'), (10101,3,'Жеткізу'), (20001,3,'Тегі'), (20001,4,'Nachname'), (10100,4,'Parken'),
(10101,4,'Lieferung');
DROP TABLE g_src;

INSERT INTO dictionary (dict_id, dict_type, dict_code, glossary_id, sort_order, value_num, value_text, is_active) VALUES
(1, 'ROLE','CLIENT',   30001,1,NULL,NULL,1),
(2, 'ROLE','PROVIDER', 30002,2,NULL,NULL,1),
(3, 'ROLE','COMPANY',  30003,3,NULL,NULL,1),
(4, 'ROLE','SUBAGENT', 30004,4,NULL,NULL,1),
(11,'DEAL_STATUS','INIT',           30011,1, 0,NULL,1),
(12,'DEAL_STATUS','MATCHING',       30012,2, 0,NULL,1),
(13,'DEAL_STATUS','OFFERS',         30013,3, 0,NULL,1),
(14,'DEAL_STATUS','AGREED',         30014,4, 0,NULL,1),
(15,'DEAL_STATUS','CONTACT',        30015,5, 0,NULL,1),
(16,'DEAL_STATUS','CLOSED',         30016,6, 1,NULL,1),   -- value_num 1 = финальный
(17,'DEAL_STATUS','CANCEL_FREE',    30017,7, 1,NULL,1),
(18,'DEAL_STATUS','CANCEL_CLIENT',  30018,8, 1,NULL,1),
(19,'DEAL_STATUS','CANCEL_PROVIDER',30019,9, 1,NULL,1),
(20,'DEAL_STATUS','EXPIRED',        30020,10,1,NULL,1),
(31,'CANCEL_REASON','PLANS',  30031,1,NULL,NULL,1),
(32,'CANCEL_REASON','LATE',   30032,2,NULL,NULL,1),
(33,'CANCEL_REASON','SILENT', 30033,3,NULL,NULL,1),
(34,'CANCEL_REASON','OTHER',  30034,4,NULL,NULL,1),
-- подложка карты на данных OpenStreetMap (атрибуция обязательна - текст в глоссарии).
-- Рабочая: офлайн-пакет MBTiles (растровые тайлы - их читает osmdroid), собранный платформой из данных OSM
--   и раздаваемый со своего CDN;
--   приложение скачивает его на устройство один раз и обновляет по map_pack_refresh_days.
--   value_num = версия пакета (ГГГГММ). Адрес CDN - заглушка, заменить при запуске.
-- Тестовая: тайлы tile.openstreetmap.org (массовая предзагрузка с них запрещена их политикой).
(45,'MAP_BASE_LAYER','OSM_OFFLINE_ALMATY',30045,1,202610,'https://cdn.example.com/maps/osm-kz-almaty-202610.mbtiles',1),
(41,'MAP_BASE_LAYER','OSM',   30041,2,NULL,'https://tile.openstreetmap.org/{z}/{x}/{y}.png',1),
-- ссылки на маршрут, проверены в браузере 24.10.2026
(42,'MAP_ROUTE_LINK','YANDEX',30042,1,NULL,'https://yandex.kz/maps/?rtext={from_lat},{from_lon}~{to_lat},{to_lon}&rtt=auto',1),
(43,'MAP_ROUTE_LINK','2GIS',  30043,2,NULL,'https://2gis.kz/directions/points/{from_lon},{from_lat}%7C{to_lon},{to_lat}',1),
(44,'MAP_ROUTE_LINK','GOOGLE',30044,3,NULL,'https://www.google.com/maps/dir/?api=1&origin={from_lat},{from_lon}&destination={to_lat},{to_lon}&travelmode=driving',1),
(51,'REFERRAL_LEVEL','1',     30051,1,10.00,NULL,1),
(52,'REFERRAL_LEVEL','2',     30052,2, 5.00,NULL,1),
(53,'REFERRAL_LEVEL','3',     30053,3, 2.00,NULL,1),
(61,'PAYMENT_METHOD','DIRECT',  30061,1,NULL,NULL,1),
(62,'PAYMENT_METHOD','PLATFORM',30062,2,NULL,NULL,0),  -- заложено на будущее
(71,'NOTIFY_TYPE','NEW_OFFER', 30071,1,NULL,NULL,1),
(72,'NOTIFY_TYPE','AGREED',    30072,2,NULL,NULL,1),
(73,'NOTIFY_TYPE','CANCELLED', 30073,3,NULL,NULL,1),
(74,'NOTIFY_TYPE','NEAR',      30074,4,NULL,NULL,1),
(75,'NOTIFY_TYPE','SUB_END',   30075,5,NULL,NULL,1),
(81,'SANCTION_REASON','BYPASS_GEO',    30081,1,NULL,NULL,1),
(82,'SANCTION_REASON','NEAR_CANCEL',   30082,2,NULL,NULL,1),
(83,'SANCTION_REASON','FAKE_REVIEWS',  30083,3,NULL,NULL,1),
(84,'SANCTION_REASON','FAKE_REFERRALS',30084,4,NULL,NULL,1),
(85,'SANCTION_REASON','REVENUE_DROP',  30085,5,NULL,NULL,1),
(5, 'ROLE','ADMIN',    30005,5,NULL,NULL,1),
(21,'DEAL_STATUS','NOT_CLOSED',     30021,11,1,NULL,1),
(91,'NOTIFY_TYPE','NEW_REQUEST',    30091,6, NULL,NULL,1),
(92,'NOTIFY_TYPE','OFFER_WITHDRAWN',30092,7, NULL,NULL,1),
(93,'NOTIFY_TYPE','CLOSE_REQUEST',  30093,8, NULL,NULL,1),
(94,'NOTIFY_TYPE','CLOSE_CODE',     30094,9, NULL,NULL,1),
(95,'NOTIFY_TYPE','CLOSING_ASK',    30095,10,NULL,NULL,1),
(96,'NOTIFY_TYPE','REVIEW_REQUIRED',30096,11,NULL,NULL,1),
(97,'NOTIFY_TYPE','EXPIRED',        30097,12,NULL,NULL,1),
(98,'NOTIFY_TYPE','SANCTION',       30098,13,NULL,NULL,1),
(99,'NOTIFY_TYPE','OFFER_DECLINED', 30099,14,NULL,NULL,1),
-- курсоры карты: роль x режим тестирования
(101,'MAP_CURSOR','CLIENT',        30101,1,NULL,NULL,1),
(102,'MAP_CURSOR','PROVIDER',      30102,2,NULL,NULL,1),
(103,'MAP_CURSOR','CLIENT_TEST',   30103,3,NULL,NULL,1),
(104,'MAP_CURSOR','PROVIDER_TEST', 30104,4,NULL,NULL,1);

-- Шаблоны интерфейса: одна логика экранов, разные раскладки (эскизы - холст "Эскизы маркетплейса услуг")
INSERT INTO dictionary (dict_id, dict_type, dict_code, glossary_id, sort_order, value_json) VALUES
(111,'UI_TEMPLATE','A',30111,1,'{"code":"A","title":"Карта в центре","font":{"family":"Manrope","mono":null},
  "shape":{"corner":28,"chip":20,"button":28},
  "screens":{"home":"map_sheet","deal_form":"sheet","offers":"map_sheet","deal":"sheet_timeline","provider_request":"sheet","slot_form":"sheet"},
  "navigation":"top_actions","map":{"position":"full","sheet_peek":360}}'),
(112,'UI_TEMPLATE','B',30112,2,'{"code":"B","title":"Карточки","font":{"family":"Golos Text","mono":null},
  "shape":{"corner":20,"chip":12,"button":18},
  "screens":{"home":"tiles","deal_form":"wizard","offers":"cards_toggle","deal":"cards_timeline","provider_request":"cards","slot_form":"wizard"},
  "navigation":"bottom_tabs","map":{"position":"card","sheet_peek":0}}'),
(113,'UI_TEMPLATE','C',30113,3,'{"code":"C","title":"Диспетчер","font":{"family":"Onest","mono":"JetBrains Mono"},
  "shape":{"corner":14,"chip":8,"button":14},
  "screens":{"home":"dense_list","deal_form":"form_rows","offers":"map_table","deal":"dense_rows","provider_request":"dense","slot_form":"form_rows"},
  "navigation":"fab","map":{"position":"top","sheet_peek":0}}');

-- Цветовые схемы: parent_dict_id - шаблон; контраст текста к фону не ниже 4.5:1
INSERT INTO dictionary (dict_id, dict_type, dict_code, parent_dict_id, glossary_id, sort_order, value_json) VALUES
(121,'UI_SCHEME','A_LIGHT',111,30121,1,'{"code":"A_LIGHT","dark":false,"colors":{
  "background":"#EEF1EC","surface":"#FFFFFF","surfaceAlt":"#F4F6F5","text":"#12201D","textMuted":"#4B5A56",
  "border":"#E3E8E6","accent":"#0F766E","onAccent":"#FFFFFF","accentSoft":"#F0F7F5",
  "danger":"#B91C1C","warning":"#B45309","success":"#166534",
  "mapLand":"#EEF1EC","mapRoad":"#FFFFFF","mapWater":"#CFE3F0","mapPark":"#D7EBD3","zone":"#0F766E"}}'),
(122,'UI_SCHEME','A_DARK',111,30122,2,'{"code":"A_DARK","dark":true,"colors":{
  "background":"#0F1715","surface":"#17211E","surfaceAlt":"#1F2B27","text":"#E6EFEC","textMuted":"#9DB0AA",
  "border":"#2A3833","accent":"#2DD4BF","onAccent":"#042F2E","accentSoft":"#123A35",
  "danger":"#F87171","warning":"#FBBF24","success":"#4ADE80",
  "mapLand":"#1A2421","mapRoad":"#2A3632","mapWater":"#16303A","mapPark":"#1C3326","zone":"#2DD4BF"}}'),
(123,'UI_SCHEME','B_LIGHT',112,30121,1,'{"code":"B_LIGHT","dark":false,"colors":{
  "background":"#F2F4F8","surface":"#FFFFFF","surfaceAlt":"#F1F5F9","text":"#0F172A","textMuted":"#475569",
  "border":"#E2E8F0","accent":"#1D4ED8","onAccent":"#FFFFFF","accentSoft":"#DBEAFE",
  "danger":"#B91C1C","warning":"#C2410C","success":"#166534",
  "mapLand":"#E9EEF3","mapRoad":"#FFFFFF","mapWater":"#CFE0F2","mapPark":"#D9EBD4","zone":"#1D4ED8"}}'),
(124,'UI_SCHEME','B_DARK',112,30122,2,'{"code":"B_DARK","dark":true,"colors":{
  "background":"#0B1220","surface":"#131C2E","surfaceAlt":"#1B2740","text":"#E2E8F0","textMuted":"#94A3B8",
  "border":"#24324D","accent":"#60A5FA","onAccent":"#0B1220","accentSoft":"#1E3A8A",
  "danger":"#F87171","warning":"#FB923C","success":"#4ADE80",
  "mapLand":"#121A2A","mapRoad":"#22304A","mapWater":"#102844","mapPark":"#15302A","zone":"#60A5FA"}}'),
(125,'UI_SCHEME','C_LIGHT',113,30121,1,'{"code":"C_LIGHT","dark":false,"colors":{
  "background":"#F5F6F2","surface":"#FFFFFF","surfaceAlt":"#EEF0EA","text":"#14171C","textMuted":"#545B66",
  "border":"#D9DDD3","accent":"#4D7C0F","onAccent":"#FFFFFF","accentSoft":"#ECF5D9",
  "danger":"#B91C1C","warning":"#B45309","success":"#166534",
  "mapLand":"#ECEEE8","mapRoad":"#FFFFFF","mapWater":"#D3E2EA","mapPark":"#DCE9D2","zone":"#4D7C0F"}}'),
(126,'UI_SCHEME','C_DARK',113,30122,2,'{"code":"C_DARK","dark":true,"colors":{
  "background":"#0E1116","surface":"#171B22","surfaceAlt":"#1C212A","text":"#E6E9EE","textMuted":"#9AA4B2",
  "border":"#2B323D","accent":"#C8F04A","onAccent":"#0E1116","accentSoft":"#1C2416",
  "danger":"#F87171","warning":"#FFB347","success":"#4ADE80",
  "mapLand":"#141922","mapRoad":"#232A35","mapWater":"#13202C","mapPark":"#17261C","zone":"#C8F04A"}}');

-- Допустимые переходы статуса сделки (П-1): parent_dict_id - из, value_num - в,
-- value_text - кто выполняет: client, provider, party (любая сторона), system, any
INSERT INTO dictionary (dict_id, dict_type, dict_code, parent_dict_id, glossary_id, sort_order, value_num, value_text)
SELECT 200 + row_number() OVER (), 'DEAL_TRANSITION', f.dict_code || '__' || t.dict_code,
       f.dict_id, 30200, row_number() OVER (), t.dict_id, v.actor
FROM (VALUES
    ('INIT','MATCHING','any'),         ('INIT','CANCEL_FREE','party'),
    ('MATCHING','OFFERS','any'),       ('MATCHING','EXPIRED','system'),   ('MATCHING','CANCEL_FREE','party'),
    ('OFFERS','AGREED','client'),      ('OFFERS','EXPIRED','system'),     ('OFFERS','CANCEL_FREE','party'),
    ('AGREED','CONTACT','system'),     ('AGREED','CLOSED','client'),      ('AGREED','CANCEL_FREE','party'),
    ('AGREED','CANCEL_CLIENT','client'), ('AGREED','CANCEL_PROVIDER','provider'), ('AGREED','NOT_CLOSED','system'),
    ('CONTACT','CLOSED','client'),     ('CONTACT','CANCEL_CLIENT','client'), ('CONTACT','CANCEL_PROVIDER','provider'),
    ('CONTACT','NOT_CLOSED','system')
) v(from_code, to_code, actor)
JOIN dictionary f ON f.dict_type = 'DEAL_STATUS' AND f.dict_code = v.from_code
JOIN dictionary t ON t.dict_type = 'DEAL_STATUS' AND t.dict_code = v.to_code;

-- Дерево услуг: 5 категорий, ТОП-10 сервисов, варианты
INSERT INTO service (service_id, parent_service_id, node_type, glossary_id, sort_order) VALUES
(1,   NULL,'system',  10001,0),
(10,  NULL,'category',10010,1),
(20,  NULL,'category',10020,2),
(30,  NULL,'category',10030,3),
(40,  NULL,'category',10040,4),
(50,  NULL,'category',10050,5),
(100, 10, 'service', 10100,1),   -- 1. Парковка
(110, 10, 'service', 10110,2),   -- 2. Такси
(101, 20, 'service', 10101,1),   -- 3. Курьер / доставка
(120, 20, 'service', 10120,2),   -- 4. Грузоперевозки
(130, 30, 'service', 10130,1),   -- 5. Разнорабочий
(140, 30, 'service', 10140,2),   -- 6. Мастер на час
(150, 30, 'service', 10150,3),   -- 7. Уборка
(160, 40, 'service', 10160,1),   -- 8. Парикмахерская
(170, 40, 'service', 10170,2),   -- 9. Маникюр
(180, 50, 'service', 10180,1),   -- 10. Репетитор
(1001,100,'variant', 11001,1),
(1002,100,'variant', 11002,2),
(105, 10, 'service', 10105,3),   -- Частная парковка (емкость мест)
(1051,105,'variant', 11051,1),
(1101,110,'variant', 11101,1),   -- классы авто такси (Н-6)
(1102,110,'variant', 11102,2),
(1103,110,'variant', 11103,3),
(1104,110,'variant', 11104,4),
(1011,101,'variant', 11011,1),
(1012,101,'variant', 11012,2),
(1201,120,'variant', 11201,1),
(1301,130,'variant', 11301,1),
(1401,140,'variant', 11401,1),
(1402,140,'variant', 11402,2),
(1501,150,'variant', 11501,1),
(1601,160,'variant', 11601,1),
(1602,160,'variant', 11602,2),
(1701,170,'variant', 11701,1),
(1801,180,'variant', 11801,1),
(1802,180,'variant', 11802,2);

-- Настройки: платформа (service_id NULL), категории, сервисы. Варианты наследуют.
INSERT INTO service_setting (setting_id, service_id, setting_code, rating_min, rating_max, setting_value) VALUES
(1, NULL,'rating_weight_system', NULL,NULL,'0.5'),
(2, NULL,'rating_weight_review', NULL,NULL,'0.5'),
(3, NULL,'offer_sort_1',         NULL,NULL,'rating:desc'),
(4, NULL,'offer_sort_2',         NULL,NULL,'price:asc'),
(5, NULL,'offer_sort_3',         NULL,NULL,'distance:asc'),
(6, NULL,'geo_mode',             NULL,NULL,'point'),
(7, NULL,'place_mode',           NULL,NULL,'provider'),
(8, NULL,'price_unit',           NULL,NULL,'fixed'),
(9, NULL,'near_radius_m',        NULL,NULL,'300'),
(10,NULL,'approx_offset_m',      NULL,NULL,'0'),
(11,NULL,'deal_fee_percent',     NULL,NULL,'0'),
(12,NULL,'forecast_window_min',  NULL,NULL,'30'),
(13,NULL,'forecast_grid_m',      NULL,NULL,'500'),
(14,NULL,'currency',             NULL,NULL,'KZT'),
(15,NULL,'map_pack_refresh_days',NULL,NULL,'30'),   -- как часто приложение обновляет офлайн-карту
-- Парковка: ближе -> выше рейтинг -> дешевле; смещение точки зависит от рейтинга поставщика
(20,100,'price_unit',     NULL,NULL,'hour'),
(21,100,'offer_sort_1',   NULL,NULL,'distance:asc'),
(22,100,'offer_sort_2',   NULL,NULL,'rating:desc'),
(23,100,'offer_sort_3',   NULL,NULL,'price:asc'),
(24,100,'approx_offset_m',NULL,4.00,'100'),
(25,100,'approx_offset_m',4.00,4.50,'75'),
(26,100,'approx_offset_m',4.50,NULL,'50'),
(27,100,'forecast_window_min',NULL,NULL,'15'),
-- Такси: маршрут А -> Б, поставщик едет к клиенту, цена за км
(30,110,'geo_mode',       NULL,NULL,'route'),
(31,110,'place_mode',     NULL,NULL,'client'),
(32,110,'price_unit',     NULL,NULL,'km'),
(33,110,'offer_sort_1',   NULL,NULL,'distance:asc'),
(34,110,'offer_sort_2',   NULL,NULL,'rating:desc'),
(35,110,'near_radius_m',  NULL,NULL,'200'),
-- Категория "Доставка и перевозки"
(40,20, 'geo_mode',       NULL,NULL,'route'),
(41,20, 'place_mode',     NULL,NULL,'client'),
(42,120,'price_unit',     NULL,NULL,'hour'),
-- Категория "Дом и ремонт": мастер приходит к клиенту, почасово; "Мастер на час" - фикс
(50,30, 'place_mode',     NULL,NULL,'client'),
(51,30, 'price_unit',     NULL,NULL,'hour'),
(52,140,'price_unit',     NULL,NULL,'fixed'),
-- Категория "Красота": клиент идет в салон, сначала рейтинг, затем расстояние
(60,40, 'place_mode',     NULL,NULL,'provider'),
(61,40, 'offer_sort_1',   NULL,NULL,'rating:desc'),
(62,40, 'offer_sort_2',   NULL,NULL,'distance:asc'),
-- Репетитор: онлайн, без точки
(70,180,'geo_mode',       NULL,NULL,'none'),
(71,180,'place_mode',     NULL,NULL,'online'),
(72,180,'price_unit',     NULL,NULL,'hour'),
-- v0.7: платформа
(80,NULL,'slot_required',        NULL,NULL,'0'),
(81,NULL,'max_parallel_deals',   NULL,NULL,'1'),
(82,NULL,'close_method',         NULL,NULL,'QR'),
(83,NULL,'close_qr_ttl_s',       NULL,NULL,'10'),
(84,NULL,'close_key_ttl_s',      NULL,NULL,'60'),
(85,NULL,'close_code_len',       NULL,NULL,'6'),
(86,NULL,'cancel_free_min',      NULL,NULL,'5'),
(87,NULL,'closing_ask_after_min',NULL,NULL,'30'),
(88,NULL,'not_closed_after_h',   NULL,NULL,'24'),
(89,NULL,'rating_start',         NULL,NULL,'4.0'),
(90,NULL,'auto_price_round',     NULL,NULL,'50'),
(91,NULL,'position_ttl_s',       NULL,NULL,'120'),
-- v0.7: парковка и такси работают по публикациям; такси - отмена без штрафа 2 мин, цена третьим ключом
(92,100,'slot_required',  NULL,NULL,'1'),
(93,110,'slot_required',  NULL,NULL,'1'),
(94,110,'cancel_free_min',NULL,NULL,'2'),
(95,110,'offer_sort_3',   NULL,NULL,'price:asc'),
-- частная парковка: публикации, место на час, лимит = емкость поставщика (agent_service.capacity)
(96,105,'slot_required',  NULL,NULL,'1'),
(97,105,'price_unit',     NULL,NULL,'hour'),
(98,105,'max_parallel_deals',NULL,NULL,'1'),
-- классы авто: машина высшего класса берет заказы низших
(99, 1102,'accepts_variants',NULL,NULL,'1101'),
(100,1103,'accepts_variants',NULL,NULL,'1101,1102'),
(101,1104,'accepts_variants',NULL,NULL,'1101,1102,1103'),
-- репетитор: закрытие ключом (онлайн)
(102,180,'close_method',  NULL,NULL,'KEY');

-- Атрибуты
INSERT INTO attribute (attribute_id, parent_attribute_id, attribute_type, glossary_id, sort_order) VALUES
(12,NULL,'group',     20012,1),   -- Карточка клиента
(13,NULL,'group',     20013,2),   -- Карты оплаты
(1, 12,  'text',      20001,1),
(2, 12,  'text',      20002,2),
(3, 12,  'text',      20003,3),
(4, 12,  'mobile',    20004,4),
(5, 12,  'date',      20005,5),
(16,12,  'text',      20016,6),   -- Псевдоним
(6, 13,  'card_token',20006,1),   -- токен карты, не номер
(7, 13,  'card_exp',  20007,2),
(8, NULL,'dim',       20008,3),   -- список: стиль оформления
(9, 8,   'par',       20009,3),
(10,8,   'par',       20010,2),
(11,8,   'par',       20011,1),
(14,NULL,'car_number',20014,1),
(15,NULL,'address',   20015,1),
(17,NULL,'text',      20017,1),
(18,NULL,'number',    20018,2),
(19,NULL,'number',    20019,3),
(20,NULL,'number',    20020,2),
(21,NULL,'text',      20021,2),
(22,NULL,'number',    20022,1),
(23,NULL,'text',      20023,1),
(24,NULL,'photo',     20024,3),
(25,NULL,'number',    20025,1),
(26,NULL,'text',      20026,4);

-- Атрибуты узлов дерева; варианты и сервисы наследуют атрибуты категорий
INSERT INTO service_attribute (service_id, attribute_id, attr_scope, is_required, show_stage, sort_order) VALUES
(1,  1, 'profile',1,'never', 1),
(1,  2, 'profile',1,'agreed',2),
(1,  3, 'profile',0,'never', 3),
(1,  4, 'profile',1,'never', 4),
(1,  5, 'profile',1,'never', 5),
(1,  16,'profile',1,'always',6),
(1,  6, 'profile',1,'never', 7),
(1,  7, 'profile',1,'never', 8),
(1,  8, 'profile',0,'never', 9),
(100,14,'profile',1,'near',  1),   -- парковка: гос. номер виден в радиусе near_radius_m
(105,14,'profile',1,'near',  1),   -- частная парковка: то же
(110,14,'profile',1,'near',  1),   -- такси: номер машины
(110,21,'profile',1,'agreed',2),
(110,22,'request',1,'always',1),
(110,25,'offer',  1,'always',1),
(101,15,'request',1,'agreed',1),
(101,4, 'profile',1,'near',  2),
(101,25,'offer',  0,'always',1),
(120,18,'request',1,'always',1),
(120,19,'request',0,'always',2),
(120,15,'request',1,'agreed',3),
(30, 17,'request',1,'always',1),   -- категория "Дом и ремонт": описание задачи для всех ее сервисов
(30, 26,'profile',0,'always',1),
(150,20,'request',1,'always',2),
(40, 24,'profile',0,'always',1),   -- категория "Красота": фото работ
(40, 26,'profile',0,'always',2),
(180,23,'request',1,'always',1),
(180,26,'profile',1,'always',1);

-- Пакеты: пакетом дешевле (1 000 и 800 тг за субагента), каждый следующий - 1 200 тг
INSERT INTO service_package (package_id, glossary_id, role_id, price, price_period,
                             included_subagents, extra_subagent_price, trial_days) VALUES
(1,40001,1,   0.00,'month', 0,   0.00, 0),
(2,40002,2,2000.00,'month', 0,   0.00,90),
(3,40003,2,3500.00,'month', 0,   0.00,90),
(4,40004,3,5000.00,'month', 5,1200.00,90),
(5,40005,3,8000.00,'month',10,1200.00,90);

INSERT INTO package_service (package_id, service_id) VALUES
(1,10),(1,20),(1,30),(1,40),(1,50),
(2,10),(2,20),
(3,10),(3,20),(3,30),(3,40),(3,50),
(4,10),(4,20),(4,30),(4,40),(4,50),
(5,10),(5,20),(5,30),(5,40),(5,50);

-- Агенты: 1 клиент, 2 поставщик, 3 компания, 4 субагент компании 3, 5 и 6 поставщики.
-- Реферальное дерево: 2 -> 3 -> (4, 5); 2 -> 6
INSERT INTO agent (agent_id, interface_lang_id, referrer_agent_id, date_creation, date_start) VALUES
(1,1,NULL,'2026-10-01','2026-10-01'),
(2,1,NULL,'2026-06-01','2026-06-01'),
(3,1,2,   '2026-07-01','2026-07-01'),
(4,3,3,   '2026-07-15','2026-07-15'),
(5,2,3,   '2026-08-01','2026-08-01'),
(6,1,2,   '2026-10-20','2026-10-20');

INSERT INTO agent_role (agent_id, role_id, parent_agent_id) VALUES
(1,1,NULL),
(2,2,NULL), (2,1,NULL),
(3,3,NULL),
(4,4,3), (4,2,3),
(5,2,NULL),
(6,2,NULL),
(1,5,NULL), (2,5,NULL);   -- администраторы: им доступен режим тестирования

-- Режим тестирования - после назначения роли ADMIN (проверяет триггер trg_agent_testing)
UPDATE agent SET is_testing = 1 WHERE agent_id IN (1, 2);

-- Сервисы агентов + настройки поставщика
INSERT INTO agent_service (agent_id, service_id, geo_lat, geo_lon, zone_radius_m, work_time_from, work_time_to,
                           price_min, price_max, client_rating_min, is_auto, is_active) VALUES
(1,1,  NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0,1),
(1,100,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0,1),
(1,101,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0,1),
(1,110,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0,1),
(2,1,  NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0,1),
(2,100,43.238900,76.889700,300,'18:00','23:00', 800.00,1500.00,3.50,1,1),
(2,101,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0,0),      -- доставка отключена
(3,1,  NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0,1),
(4,1,  NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0,1),
(4,100,43.240100,76.891200,500,'09:00','21:00',1000.00,2500.00,4.00,0,1),
(5,1,  NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0,1),
(5,100,43.237400,76.889100,200,'08:00','22:00', 700.00,1500.00,3.00,1,1),
(5,101,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0,1),
(6,1,  NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0,1),
(6,100,43.239170,76.889000,150,'00:00','23:59', 900.00,1200.00,3.00,0,1),
(6,110,43.245000,76.900000,3000,'18:00','23:59',1000.00,2500.00,3.50,1,1);

-- Тариф водителя (Н-7): порог стоимости поездки 700 тг и минимум 150 тг за км
UPDATE agent_service SET price_min = 700.00, unit_price_min = 150.00 WHERE agent_id = 6 AND service_id = 110;

INSERT INTO agent_attribute (agent_id, attribute_id, attribute_value, date_start) VALUES
(1,1,'Иванов','2026-10-01'), (1,2,'Иван','2026-10-01'), (1,3,'Иванович','2026-10-01'),
(1,4,'+77011234567','2026-10-01'), (1,5,'1990-05-15','2026-10-01'), (1,16,'Ivan_A','2026-10-01'),
(1,6,'tok_test_4f9a21c7','2026-10-01'), (1,7,'12/28','2026-10-01'), (1,8,'10','2026-10-01'),
(1,14,'777ABC02','2026-10-01'),
(2,1,'Петров','2026-06-01'), (2,2,'Петр','2026-06-01'), (2,4,'+77029876543','2026-06-01'),
(2,5,'1985-11-02','2026-06-01'), (2,16,'Петр_Парковка','2026-06-01'),
(2,6,'tok_test_8b3d55e0','2026-06-01'), (2,7,'03/27','2026-06-01'), (2,8,'9','2026-06-01'),
(4,1,'Сериков','2026-07-15'), (4,2,'Ерлан','2026-07-15'), (4,4,'+77475550011','2026-07-15'),
(4,5,'1993-02-20','2026-07-15'), (4,16,'ErlanPark','2026-07-15'),
(4,6,'tok_test_1c7e90aa','2026-07-15'), (4,7,'08/29','2026-07-15'),
(5,1,'Ахметова','2026-08-01'), (5,2,'Айгерим','2026-08-01'), (5,4,'+77051112233','2026-08-01'),
(5,5,'1996-07-09','2026-08-01'), (5,16,'Айгерим','2026-08-01'),
(5,6,'tok_test_5d2b77f1','2026-08-01'), (5,7,'11/28','2026-08-01'),
(6,1,'Ким','2026-10-20'), (6,2,'Виктор','2026-10-20'), (6,4,'+77013334455','2026-10-20'),
(6,5,'1988-03-30','2026-10-20'), (6,16,'ViktorK','2026-10-20'),
(6,6,'tok_test_9e4c12b8','2026-10-20'), (6,7,'05/29','2026-10-20'),
(6,14,'123KZA02','2026-10-20'), (6,21,'Toyota Camry, белая','2026-10-20');

-- Конфигурация интерфейса: шаблон, цветовая схема, язык. Офлайн-пакет карты пока заглушка - подложка OSM.
INSERT INTO ui_config (agent_id, config_json, checksum)
SELECT agent_id, cfg, md5(cfg::text)
FROM (VALUES
  (1, '{"lang":"RUS","template":"A","scheme":"A_LIGHT","map":"OSM","version":7}'::jsonb),
  (2, '{"lang":"RUS","template":"C","scheme":"C_DARK","map":"OSM","version":7}'::jsonb),
  (3, '{"lang":"RUS","template":"B","scheme":"B_LIGHT","map":"OSM","version":7}'::jsonb),
  (4, '{"lang":"KAZ","template":"A","scheme":"A_LIGHT","map":"OSM","version":7}'::jsonb),
  (5, '{"lang":"ENG","template":"B","scheme":"B_DARK","map":"OSM","version":7}'::jsonb),
  (6, '{"lang":"RUS","template":"A","scheme":"A_DARK","map":"OSM","version":7}'::jsonb)
) v(agent_id, cfg);

-- Подписки: пробный период, затем платные месяцы
INSERT INTO agent_subscription (subscription_id, agent_id, package_id, is_trial, date_start, date_end) VALUES
(1, 1,1,0,'2026-10-01','2099-12-31'),
(2, 2,2,1,'2026-06-01','2026-08-31'),
(3, 2,2,0,'2026-09-01','2026-09-30 23:59:59'),
(4, 2,2,0,'2026-10-01','2026-10-31 23:59:59'),
(5, 3,4,1,'2026-07-01','2026-09-30'),
(6, 3,4,0,'2026-10-01','2026-10-31 23:59:59'),
(7, 5,2,1,'2026-08-01','2026-08-31'),
(8, 5,2,0,'2026-09-01','2026-09-30 23:59:59'),
(9, 5,2,0,'2026-10-01','2026-10-31 23:59:59'),
(10,6,3,1,'2026-10-20','2027-01-18');

-- Тестовое продление без оплат: поставщики и компания сохраняют доступ к сервисам после октября
INSERT INTO agent_subscription (subscription_id, agent_id, package_id, is_trial, date_start, date_end)
SELECT id, agent_id, package_id, 0, '2026-11-01', greatest(now(), '2026-11-01') + interval '1 year'
FROM (VALUES (11, 2, 2), (12, 3, 4), (13, 5, 2)) v(id, agent_id, package_id);

-- Субагенту 4 парковку назначает компания 3 (триггер ограничит срок подпиской компании)
UPDATE agent_service SET assigned_by_agent_id = 3 WHERE agent_id = 4 AND service_id = 100;

INSERT INTO subscription_payment VALUES
(1,3,2,2000.00,'paid','2026-09-01 10:00'),
(2,4,2,2000.00,'paid','2026-10-01 10:00'),
(3,6,3,5000.00,'paid','2026-10-01 11:00'),
(4,8,5,2000.00,'paid','2026-09-01 12:00'),
(5,9,5,2000.00,'paid','2026-10-01 12:00');

-- Реферальные начисления: вверх по referrer_agent_id до последнего уровня.
-- Статус accrued - если оплаченный период закончился на тестовую дату 24.10.2026.
INSERT INTO referral_reward
WITH RECURSIVE chain AS (
    SELECT p.payment_id, p.agent_id AS source_agent_id, a.referrer_agent_id AS beneficiary_agent_id,
           1 AS lvl, p.amount, s.date_end
    FROM subscription_payment p
    JOIN agent a              ON a.agent_id = p.agent_id
    JOIN agent_subscription s ON s.subscription_id = p.subscription_id
    WHERE p.payment_status = 'paid' AND a.referrer_agent_id IS NOT NULL
    UNION ALL
    SELECT c.payment_id, c.source_agent_id, a.referrer_agent_id, c.lvl + 1, c.amount, c.date_end
    FROM chain c
    JOIN agent a ON a.agent_id = c.beneficiary_agent_id
    WHERE a.referrer_agent_id IS NOT NULL
      AND c.lvl < (SELECT count(*) FROM dictionary WHERE dict_type = 'REFERRAL_LEVEL' AND is_active = 1)
)
SELECT row_number() OVER (ORDER BY c.payment_id, c.lvl),
       c.payment_id, c.beneficiary_agent_id, c.source_agent_id, r.dict_id,
       r.value_num, round(c.amount * r.value_num / 100, 2),
       CASE WHEN c.date_end < '2026-10-24' THEN 'accrued' ELSE 'pending' END,
       c.date_end::date,
       now()
FROM chain c
JOIN dictionary r ON r.dict_type = 'REFERRAL_LEVEL' AND r.dict_code = c.lvl::text AND r.is_active = 1;

-- Сентябрьское вознаграждение агента 3 уже выплачено на карту
UPDATE referral_reward
SET reward_status = 'paid_out', date_payout = '2026-10-05 12:00', payout_ref = 'payout_test_0001'
WHERE payment_id = 4 AND beneficiary_agent_id = 3;

-- Публикации поставщиков (Н-2), окно - от часа назад до 8 часов вперед от момента загрузки.
-- Места B, C, D, E вокруг точки клиента A (43.238000, 76.889000); E в 130 м - вне зоны 100 м.
-- Водитель 6 едет из А' в Б: коридор ±300 м, радиус 1 000 м вокруг Б (В-8).
INSERT INTO provider_slot (slot_id, agent_id, service_id, geo_lat, geo_lon, radius_m,
                           geo_to_lat, geo_to_lon, radius_to_m, period_start, period_end, price_min)
SELECT id, agent_id, service_id, lat, lon, radius, to_lat, to_lon, radius_to,
       date_trunc('minute', now()) - interval '1 hour', date_trunc('minute', now()) + interval '8 hours', price_min
FROM (VALUES
  (1, 2, 100, 43.238270, 76.889000, NULL::numeric, NULL::numeric, NULL::numeric, NULL::numeric,  800.00),  -- B
  (2, 5, 100, 43.237370, 76.889000, NULL, NULL, NULL, NULL,  700.00),                                   -- C
  (3, 4, 100, 43.238000, 76.889617, NULL, NULL, NULL, NULL, 1000.00),                                   -- D
  (4, 6, 100, 43.239170, 76.889000, NULL, NULL, NULL, NULL,  900.00),                                   -- E
  (5, 6, 110, 43.245000, 76.900000, 300,  43.256000, 76.945000, 1000, 700.00)                           -- такси А' -> Б
) v(id, agent_id, service_id, lat, lon, radius, to_lat, to_lon, radius_to, price_min);

-- Сделки (оплата напрямую между агентами, сбор платформы = 0)
INSERT INTO deal (deal_id, service_id, client_agent_id, provider_agent_id, deal_status_id, payment_method_id,
                  geo_lat, geo_lon, zone_radius_m, geo_to_lat, geo_to_lon, route_length_m,
                  period_start, period_end, price_forecast_min, price_forecast_max, client_price, provider_rating_min,
                  price_provider, fee_amount, price_total, close_secret, date_creation, date_agreed, date_close) VALUES
(1,1001,1,2,16,61,43.238000,76.889000,100,NULL,NULL,NULL,'2026-10-24 18:00','2026-10-24 18:15',
 800.00,2000.00,NULL,4.00,1000.00,0.00,1000.00,NULL,'2026-10-24 17:40','2026-10-24 17:47','2026-10-24 18:16'),
(2,1011,1,4,19,61,43.250000,76.900000,1000,43.260000,76.920000,NULL,'2026-10-25 12:00','2026-10-25 13:00',
 1500.00,3000.00,NULL,NULL,1800.00,0.00,1800.00,NULL,'2026-10-25 10:00','2026-10-25 10:15','2026-10-25 11:20'),
(3,1001,5,4,16,61,43.240000,76.890000,200,NULL,NULL,NULL,'2026-10-22 09:00','2026-10-22 10:00',
 900.00,1800.00,NULL,NULL,1200.00,0.00,1200.00,NULL,'2026-10-22 08:30','2026-10-22 08:40','2026-10-22 10:02'),
(4,1001,1,4,16,61,43.241000,76.892000,150,NULL,NULL,NULL,'2026-10-23 19:00','2026-10-23 19:30',
 700.00,1500.00,NULL,NULL, 900.00,0.00, 900.00,NULL,'2026-10-23 18:40','2026-10-23 18:45','2026-10-23 19:31'),
(5,1001,5,2,16,61,43.239000,76.889500,100,NULL,NULL,NULL,'2026-10-23 20:00','2026-10-23 20:30',
 700.00,1400.00,NULL,NULL, 800.00,0.00, 800.00,NULL,'2026-10-23 19:45','2026-10-23 19:50','2026-10-23 20:33');

-- Сделка 6: кейс A/B/C/D, ждет выбора клиента; сделка 7: такси "Эконом" из А в Б, 6,4 км по дорогам
INSERT INTO deal (deal_id, service_id, client_agent_id, deal_status_id, payment_method_id,
                  geo_lat, geo_lon, zone_radius_m, geo_to_lat, geo_to_lon, route_length_m,
                  period_start, period_end, price_forecast_min, price_forecast_max, provider_rating_min, date_creation)
SELECT id, service_id, 1, 13, 61, 43.238000, 76.889000, radius, to_lat, to_lon, route_m,
       date_trunc('minute', now()) + start_after, date_trunc('minute', now()) + start_after + duration,
       fmin, fmax, prmin, now() - interval '10 minutes'
FROM (VALUES
  (6, 1001, 100,  NULL::numeric, NULL::numeric, NULL::numeric, interval '2 hours', interval '1 hour',    700.00,  1500.00, NULL::numeric),
  (7, 1101, 1500, 43.256000,     76.945000,     6400,          interval '2 hours', interval '15 minutes', 700.00, 1300.00, 4.00)
) v(id, service_id, radius, to_lat, to_lon, route_m, start_after, duration, fmin, fmax, prmin);

INSERT INTO deal_offer (deal_offer_id, deal_id, provider_agent_id, offer_price, offer_mode, geo_lat, geo_lon,
                        offer_status, date_creation) VALUES
(1,1,2,1000.00,'auto',  43.238764,76.889000,'selected','2026-10-24 17:41'),  -- 85 м
(2,1,4,1200.00,'manual',43.238000,76.890173,'declined','2026-10-24 17:45'),  -- 95 м
(3,2,4,1800.00,'manual',NULL,     NULL,     'selected','2026-10-25 10:10'),
(9,3,4,1200.00,'manual',43.240300,76.890200,'selected','2026-10-22 08:35'),
(10,4,4,900.00,'manual',43.241500,76.892300,'selected','2026-10-23 18:42'),
(11,5,2,800.00,'auto',  43.239200,76.889600,'selected','2026-10-23 19:46');

-- Отклики сделок 6 и 7 - сразу после создания сделок; E (агент 6, 130 м) не откликается: вне зоны (В-1)
INSERT INTO deal_offer (deal_offer_id, deal_id, provider_agent_id, offer_price, offer_mode, geo_lat, geo_lon, date_creation)
SELECT id, deal_id, agent_id, price, mode, lat, lon, now() - interval '9 minutes' + after
FROM (VALUES
  (4, 6, 2, 1000.00, 'auto',   43.238270, 76.889000, interval '0'),          -- B, 30 м
  (5, 6, 5,  900.00, 'auto',   43.237370, 76.889000, interval '1 minute'),   -- C, 70 м
  (6, 6, 4, 1100.00, 'manual', 43.238000, 76.889617, interval '4 minutes'),  -- D, 50 м
  (8, 7, 6,  960.00, 'auto',   43.245000, 76.900000, interval '1 minute')    -- машина в 1,2 км: max(700, 150 x 6,4)
) v(id, deal_id, agent_id, price, mode, lat, lon, after);

-- Поля запроса и отклика: пассажиры (запрос такси), время подачи (отклик), описание (доставка)
INSERT INTO deal_attribute VALUES
(1,7,NULL,22,'2','2026-10-24 22:20'),
(2,7,8,   25,'6','2026-10-24 22:21'),
(3,2,NULL,15,'ул. Абая 150, кв. 12','2026-10-25 10:00'),
(4,2,3,   25,'40','2026-10-25 10:10');


-- История статусов (в работе ее пишет триггер; здесь - явно, режим загрузки)
INSERT INTO deal_status_log (deal_status_log_id, deal_id, deal_status_id, agent_id, cancel_reason_id, comment, date_creation) VALUES
(1, 1,11,1,   NULL,NULL,'2026-10-24 17:40'),
(2, 1,12,NULL,NULL,NULL,'2026-10-24 17:40'),
(3, 1,13,NULL,NULL,NULL,'2026-10-24 17:41'),
(4, 1,14,1,   NULL,NULL,'2026-10-24 17:47'),
(5, 1,15,NULL,NULL,'Стороны в 50 м друг от друга','2026-10-24 17:58'),
(6, 1,16,1,   NULL,'QR отсканирован','2026-10-24 18:16'),
(7, 2,11,1,   NULL,NULL,'2026-10-25 10:00'),
(8, 2,12,NULL,NULL,NULL,'2026-10-25 10:00'),
(9, 2,13,NULL,NULL,NULL,'2026-10-25 10:10'),
(10,2,14,1,   NULL,NULL,'2026-10-25 10:15'),
(11,2,19,4,   32,  'Сломалась машина','2026-10-25 11:20'),
(18,3,11,5,   NULL,NULL,'2026-10-22 08:30'),
(19,3,12,NULL,NULL,NULL,'2026-10-22 08:30'),
(20,3,13,NULL,NULL,NULL,'2026-10-22 08:35'),
(21,3,14,5,   NULL,NULL,'2026-10-22 08:40'),
(22,3,15,NULL,NULL,NULL,'2026-10-22 08:58'),
(23,3,16,5,   NULL,'QR отсканирован','2026-10-22 10:02'),
(24,4,11,1,   NULL,NULL,'2026-10-23 18:40'),
(25,4,12,NULL,NULL,NULL,'2026-10-23 18:40'),
(26,4,13,NULL,NULL,NULL,'2026-10-23 18:42'),
(27,4,14,1,   NULL,NULL,'2026-10-23 18:45'),
(28,4,15,NULL,NULL,NULL,'2026-10-23 18:57'),
(29,4,16,1,   NULL,'QR отсканирован','2026-10-23 19:31'),
(30,5,11,5,   NULL,NULL,'2026-10-23 19:45'),
(31,5,12,NULL,NULL,NULL,'2026-10-23 19:45'),
(32,5,13,NULL,NULL,NULL,'2026-10-23 19:46'),
(33,5,14,5,   NULL,NULL,'2026-10-23 19:50'),
(34,5,15,NULL,NULL,NULL,'2026-10-23 19:58'),
(35,5,16,5,   NULL,'QR отсканирован','2026-10-23 20:33');

-- Открытые сделки 6 и 7: INIT -> MATCHING -> OFFERS в момент загрузки
INSERT INTO deal_status_log (deal_status_log_id, deal_id, deal_status_id, agent_id, date_creation)
SELECT 36 + row_number() OVER (ORDER BY d.deal_id, s.ord), d.deal_id, s.status_id,
       CASE WHEN s.status_id = 11 THEN d.client_agent_id END, d.date_creation + s.after
FROM deal d
CROSS JOIN (VALUES (1, 11, interval '0'), (2, 12, interval '0'), (3, 13, interval '1 minute')) s(ord, status_id, after)
WHERE d.deal_id IN (6, 7);

-- Баллы: у закрытых сделок - обе стороны, у отмены поставщиком - пострадавший клиент
INSERT INTO deal_review (deal_id, from_agent_id, to_agent_id, to_role_id, score, comment, date_creation) VALUES
(1,1,2,2,5.0,'Место рядом со входом','2026-10-24 18:17'),
(1,2,1,1,4.0,NULL,'2026-10-24 18:18'),
(2,1,4,2,2.0,'Отменил за час до доставки','2026-10-25 11:25'),
(3,5,4,2,4.0,NULL,'2026-10-22 10:05'),
(3,4,5,1,5.0,NULL,'2026-10-22 10:06'),
(4,1,4,2,4.0,NULL,'2026-10-23 19:33'),
(4,4,1,1,4.0,NULL,'2026-10-23 19:34'),
(5,5,2,2,5.0,'Быстро нашла место','2026-10-23 20:35'),
(5,2,5,1,5.0,NULL,'2026-10-23 20:36');

INSERT INTO notification (notification_id, agent_id, deal_id, notify_type_id, channel, payload, date_creation, date_sent, date_read) VALUES
(1,1,6,   71,'push',  '{"deal_offer_id":4}',now() - interval '9 minutes',now() - interval '9 minutes',NULL),
(2,1,6,   71,'push',  '{"deal_offer_id":5}',now() - interval '8 minutes',now() - interval '8 minutes',now() - interval '7 minutes'),
(3,1,2,   73,'push',  '{"by":"provider","reason":"LATE"}','2026-10-25 11:20','2026-10-25 11:20','2026-10-25 11:22'),
(4,1,7,   71,'push',  '{"deal_offer_id":8}',now() - interval '8 minutes',now() - interval '8 minutes',NULL),
(5,2,NULL,75,'in_app','{"subscription_id":4,"date_end":"2026-10-31"}','2026-10-24 09:00','2026-10-24 09:00',NULL),
(6,1,6,   71,'push',  '{"deal_offer_id":6}',now() - interval '5 minutes',now() - interval '5 minutes',NULL);

-- Рейтинг по узлам уровня "сервис"; total = 0.5 * system + 0.5 * review
INSERT INTO rating_history VALUES
(2,2,100,'2026-10-22',4.10,4.60,4.35,12,1),
(2,2,100,'2026-10-23',4.15,4.62,4.39,13,1),
(2,2,100,'2026-10-24',4.20,4.65,4.43,14,1),
(1,1,100,'2026-10-24',4.00,4.00,4.00, 1,0),
(4,2,100,'2026-10-24',3.80,4.10,3.95, 5,0),
(4,2,101,'2026-10-25',3.40,2.00,2.70, 0,1),
(5,2,100,'2026-10-24',4.50,4.80,4.65, 8,0),
(6,2,100,'2026-10-24',3.00,3.00,3.00, 0,0),
(6,2,110,'2026-10-24',4.20,4.50,4.35,30,1);

-- Санкции агента 4: предупреждение, понижение рейтинга, блокировка на неделю (уже закончилась)
INSERT INTO agent_sanction (sanction_id, agent_id, deal_id, reason_id, action, block_level, date_creation, date_start, date_end, is_active) VALUES
(1,4,NULL,81,'warning',    NULL,'2026-10-01 12:00',NULL,NULL,1),
(2,4,NULL,82,'rating_down',NULL,'2026-10-05 12:00',NULL,NULL,1),
(3,4,NULL,83,'block',      1,   '2026-10-10 12:00','2026-10-10 12:00','2026-10-17 12:00',1);

-- ---------------------------------------------------------------------
-- Тестовые пользователи user_testing_1..5 (агенты 7-11): вход по общему паролю, заданному владельцем.
-- В скрипте только хеш bcrypt. Клиент + поставщик + администратор, режим тестирования включен.
-- ---------------------------------------------------------------------
INSERT INTO agent (agent_id, interface_lang_id, date_creation, date_start)
SELECT 6 + n, 1, now(), now() FROM generate_series(1, 5) n;

INSERT INTO agent_role (agent_id, role_id)
SELECT 6 + n, r FROM generate_series(1, 5) n CROSS JOIN (VALUES (1), (2), (5)) v(r);

UPDATE agent SET is_testing = 1 WHERE agent_id BETWEEN 7 AND 11;

INSERT INTO agent_login (login, agent_id, password_hash)
SELECT 'user_testing_' || n, 6 + n, '$2a$10$u1iId.yJzUmMObuonUC1f.meuDSquozF7R.GHEnOEZlAKQkugzFme' FROM generate_series(1, 5) n;

INSERT INTO agent_attribute (agent_id, attribute_id, attribute_value, date_start)
SELECT 6 + n, a.attribute_id,
       CASE a.attribute_id
           WHEN 1  THEN 'Тестовый'
           WHEN 2  THEN 'Пользователь ' || n
           WHEN 4  THEN '+7700000000' || n
           WHEN 5  THEN '1990-01-0' || n
           WHEN 16 THEN 'Тестер_' || n
           WHEN 14 THEN n || '00TST02'
           WHEN 21 THEN 'Тестовое авто ' || n
       END,
       now()
FROM generate_series(1, 5) n
CROSS JOIN (VALUES (1), (2), (4), (5), (16), (14), (21)) a(attribute_id);

-- Подписка "Поставщик: все услуги" на год (без пробного периода и оплат)
INSERT INTO agent_subscription (subscription_id, agent_id, package_id, is_trial, date_start, date_end)
SELECT 13 + n, 6 + n, 3, 0, now() - interval '1 day', now() + interval '1 year' FROM generate_series(1, 5) n;

-- Сервисы поставщика: точка и зона обслуживания вокруг тестовой точки A, без автоотклика
INSERT INTO agent_service (agent_id, service_id, geo_lat, geo_lon, zone_radius_m, work_time_from, work_time_to,
                           price_min, price_max, unit_price_min, client_rating_min, is_auto)
SELECT 6 + n, s.service_id, 43.238000 + n * 0.0005, 76.889000, s.zone, NULL, NULL,
       s.pmin, s.pmax, s.unit, NULL, 0
FROM generate_series(1, 5) n
CROSS JOIN (VALUES
    (1,   NULL::numeric, NULL::numeric, NULL::numeric, NULL::numeric),
    (100, 500,  500.00, 2000.00, NULL),          -- парковка
    (1101,5000, 700.00, 5000.00, 150.00),        -- такси Эконом: порог 700 тг, 150 тг за км
    (140, 5000, 3000.00,15000.00,NULL),          -- мастер на час
    (180, NULL, 2000.00, 6000.00, NULL)          -- репетитор (онлайн)
) s(service_id, zone, pmin, pmax, unit);

INSERT INTO ui_config (agent_id, config_json, checksum)
SELECT 6 + n, cfg, md5(cfg::text)
FROM generate_series(1, 5) n
CROSS JOIN LATERAL (SELECT jsonb_build_object('lang','RUS','template','A','scheme','A_LIGHT','map','OSM','version',7) AS cfg) c;

-- ---------------------------------------------------------------------
-- Завершение загрузки: триггеры процесса снова проверяют и логируют; номера фактов - после тестовых
-- ---------------------------------------------------------------------
RESET mp.load_mode;

SELECT setval('deal_seq',            (SELECT max(deal_id)            FROM deal)::bigint);
SELECT setval('deal_offer_seq',      (SELECT max(deal_offer_id)      FROM deal_offer)::bigint);
SELECT setval('deal_attribute_seq',  (SELECT max(deal_attribute_id)  FROM deal_attribute)::bigint);
SELECT setval('deal_status_log_seq', (SELECT max(deal_status_log_id) FROM deal_status_log)::bigint);
SELECT setval('notification_seq',    (SELECT max(notification_id)    FROM notification)::bigint);
SELECT setval('agent_sanction_seq',  (SELECT max(sanction_id)        FROM agent_sanction)::bigint);
SELECT setval('provider_slot_seq',   (SELECT max(slot_id)            FROM provider_slot)::bigint);
