-- =====================================================================
-- Маркетплейс услуг: схема mp_service, структура БД + тестовые данные
-- Версия модели 0.6 (упрощенная). Требуется PostgreSQL 15+ (UNIQUE NULLS NOT DISTINCT).
-- Функции, читающие таблицы, закреплены за схемой (SET search_path = mp_service):
-- представления работают из любой сессии, без SET search_path у клиента.
-- Соглашения:
--   DIM  - справочник, LIST - реестр, LINK - связка, FAKT - факты/события
--   Общие поля версионности: date_creation, date_start, date_end (NULL = бессрочно), is_active
--   Новый сервис добавляется только данными: узел в service, тексты в glossary,
--   поля в service_attribute, настройки в service_setting, включение в пакет.
-- ВНИМАНИЕ: скрипт удаляет схему mp_service целиком.
-- =====================================================================

DROP SCHEMA IF EXISTS mp_service CASCADE;
CREATE SCHEMA mp_service;
SET search_path TO mp_service;

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
-- dict_type: ROLE, DEAL_STATUS, CANCEL_REASON, MAP_BASE_LAYER, MAP_ROUTE_LINK,
--            REFERRAL_LEVEL, PAYMENT_METHOD, NOTIFY_TYPE, SANCTION_REASON
CREATE TABLE dictionary (
    dict_id            numeric      PRIMARY KEY,
    dict_type          text         NOT NULL,
    dict_code          text         NOT NULL,
    parent_dict_id     numeric      REFERENCES dictionary,
    glossary_id        numeric      NOT NULL REFERENCES glossary,
    sort_order         numeric      NOT NULL DEFAULT 1,
    value_num          numeric,                           -- % уровня, признак финального статуса ...
    value_text         text,                              -- шаблон ссылки, URL тайлов ...
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

-- LINK Сервисы агента + настройки поставщика (для клиента настройки пустые)
CREATE TABLE agent_service (
    agent_id           numeric      NOT NULL REFERENCES agent,
    service_id         numeric      NOT NULL REFERENCES service,
    geo_lat            numeric(9,6),
    geo_lon            numeric(9,6),
    zone_radius_m      numeric,
    work_time_from     time,
    work_time_to       time,
    price_min          numeric(12,2),
    price_max          numeric(12,2),
    client_rating_min  numeric(3,2),
    is_auto            smallint     NOT NULL DEFAULT 0 CHECK (is_auto IN (0,1)),
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

-- FAKT Сделка. service_id - любой узел дерева (обычно вариант услуги).
CREATE TABLE deal (
    deal_id              numeric     PRIMARY KEY,
    service_id           numeric     NOT NULL REFERENCES service,
    client_agent_id      numeric     NOT NULL REFERENCES agent,
    provider_agent_id    numeric     REFERENCES agent,
    deal_status_id       numeric     NOT NULL REFERENCES dictionary,
    payment_method_id    numeric     NOT NULL REFERENCES dictionary,  -- сейчас DIRECT
    geo_lat              numeric(9,6),                      -- точка клиента / точка А
    geo_lon              numeric(9,6),
    zone_radius_m        numeric,
    geo_to_lat           numeric(9,6),                      -- точка Б (geo_mode = route)
    geo_to_lon           numeric(9,6),
    period_start         timestamp   NOT NULL,
    period_end           timestamp   NOT NULL,
    price_forecast_min   numeric(12,2),
    price_forecast_max   numeric(12,2),
    provider_rating_min  numeric(3,2),
    price_provider       numeric(12,2),
    fee_amount           numeric(12,2),
    price_total          numeric(12,2),
    qr_token             text,
    date_creation        timestamp   NOT NULL DEFAULT now(),
    date_close           timestamp,
    CHECK (period_end > period_start)
);

-- FAKT Отклик поставщика
CREATE TABLE deal_offer (
    deal_offer_id      numeric      PRIMARY KEY,
    deal_id            numeric      NOT NULL REFERENCES deal,
    provider_agent_id  numeric      NOT NULL REFERENCES agent,
    offer_price        numeric(12,2) NOT NULL,
    offer_mode         text         NOT NULL CHECK (offer_mode IN ('auto','manual')),
    geo_lat            numeric(9,6),                      -- точка предложения
    geo_lon            numeric(9,6),
    is_selected        smallint     NOT NULL DEFAULT 0 CHECK (is_selected IN (0,1)),
    date_creation      timestamp    NOT NULL DEFAULT now(),
    UNIQUE (deal_id, provider_agent_id)
);

-- FAKT Поля запроса (deal_offer_id NULL) и отклика - по service_attribute.attr_scope
CREATE TABLE deal_attribute (
    deal_attribute_id  numeric      PRIMARY KEY,
    deal_id            numeric      NOT NULL REFERENCES deal,
    deal_offer_id      numeric      REFERENCES deal_offer,
    attribute_id       numeric      NOT NULL REFERENCES attribute,
    attribute_value    text         NOT NULL,
    date_creation      timestamp    NOT NULL DEFAULT now(),
    UNIQUE NULLS NOT DISTINCT (deal_id, deal_offer_id, attribute_id)
);

-- FAKT История статусов сделки
CREATE TABLE deal_status_log (
    deal_status_log_id numeric      PRIMARY KEY,
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
    notification_id    numeric      PRIMARY KEY,
    agent_id           numeric      NOT NULL REFERENCES agent,
    deal_id            numeric      REFERENCES deal,
    notify_type_id     numeric      NOT NULL REFERENCES dictionary,
    channel            text         NOT NULL CHECK (channel IN ('push','sms','email','in_app')),
    payload            jsonb,
    date_creation      timestamp    NOT NULL DEFAULT now(),
    date_sent          timestamp,
    date_read          timestamp
);

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
    sanction_id        numeric      PRIMARY KEY,
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
    SELECT dict_id FROM dictionary WHERE dict_type = p_type AND dict_code = p_code
$$;

-- Расстояние между точками в метрах (гаверсинус). При переходе на PostGIS - ST_Distance.
CREATE FUNCTION geo_distance_m(lat1 numeric, lon1 numeric, lat2 numeric, lon2 numeric)
RETURNS numeric LANGUAGE sql IMMUTABLE STRICT AS $$
    SELECT round((2 * 6371000 * asin(sqrt(
               power(sin(radians(lat2 - lat1) / 2), 2) +
               cos(radians(lat1)) * cos(radians(lat2)) *
               power(sin(radians(lon2 - lon1) / 2), 2))))::numeric, 1)
$$;

-- Ключ сортировки отклика по спецификации 'distance:asc' | 'rating:desc' | 'price:asc'
CREATE FUNCTION offer_sort_key(p_spec text, p_distance numeric, p_rating numeric, p_price numeric)
RETURNS numeric LANGUAGE sql IMMUTABLE AS $$
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

-- Сервисы, доступные агенту по действующей подписке (категория в пакете = все узлы под ней)
CREATE VIEW v_agent_available_service AS
SELECT DISTINCT sub.agent_id, t.service_id
FROM agent_subscription sub
JOIN package_service ps ON ps.package_id = sub.package_id AND ps.is_active = 1
JOIN v_service_tree t   ON t.ancestor_id = ps.service_id
WHERE sub.is_active = 1 AND now() BETWEEN sub.date_start AND sub.date_end;

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

-- Отклики в порядке вывода клиенту (список и точки на карте).
-- До согласования точка смещается на approx_offset_m (зависит от сервиса и рейтинга поставщика),
-- направление смещения постоянное для отклика; точная точка - после согласования выбранного отклика.
CREATE VIEW v_deal_offer_list AS
WITH base AS (
    SELECT o.deal_offer_id, o.deal_id, o.provider_agent_id, o.offer_price, o.is_selected,
           o.geo_lat, o.geo_lon, d.service_id, d.zone_radius_m, ds.dict_code AS deal_status,
           geo_distance_m(d.geo_lat, d.geo_lon, o.geo_lat, o.geo_lon) AS distance_m,
           (SELECT rh.total_rating FROM rating_history rh
            WHERE rh.agent_id = o.provider_agent_id
              AND rh.role_id = dict_id('ROLE','PROVIDER')
              AND rh.service_id = service_base_id(d.service_id)
              AND rh.rating_date <= d.date_creation::date
            ORDER BY rh.rating_date DESC LIMIT 1) AS rating,
           (SELECT aa.attribute_value FROM agent_attribute aa
            WHERE aa.agent_id = o.provider_agent_id AND aa.attribute_id = 16 AND aa.is_active = 1
            ORDER BY aa.date_start DESC LIMIT 1) AS pseudonym,
           get_setting(d.service_id, 'offer_sort_1') AS s1,
           get_setting(d.service_id, 'offer_sort_2') AS s2,
           get_setting(d.service_id, 'offer_sort_3') AS s3
    FROM deal_offer o
    JOIN deal d        ON d.deal_id = o.deal_id
    JOIN dictionary ds ON ds.dict_id = d.deal_status_id
), shown AS (
    SELECT b.*,
           (b.is_selected = 1 AND b.deal_status IN ('AGREED','CONTACT','CLOSED')) AS is_exact,
           coalesce(get_setting(b.service_id, 'approx_offset_m', b.rating)::numeric, 0) AS offset_m,
           radians((('x' || substr(md5(b.deal_offer_id::text), 1, 6))::bit(24)::int % 360)) AS angle
    FROM base b
)
SELECT deal_id, service_id,
       row_number() OVER (PARTITION BY deal_id
           ORDER BY offer_sort_key(s1, distance_m, rating, offer_price) NULLS LAST,
                    offer_sort_key(s2, distance_m, rating, offer_price) NULLS LAST,
                    offer_sort_key(s3, distance_m, rating, offer_price) NULLS LAST,
                    deal_offer_id) AS position,
       deal_offer_id, provider_agent_id, pseudonym, rating, offer_price, is_exact,
       CASE WHEN is_exact OR offset_m = 0 THEN geo_lat
            ELSE round((geo_lat + offset_m * cos(angle) / 111195)::numeric, 6) END AS show_lat,
       CASE WHEN is_exact OR offset_m = 0 THEN geo_lon
            ELSE round((geo_lon + offset_m * sin(angle) / (111195 * cos(radians(geo_lat))))::numeric, 6) END AS show_lon,
       CASE WHEN is_exact OR offset_m = 0 THEN distance_m ELSE round(distance_m / 10) * 10 END AS distance_m,
       concat_ws(' · ', pseudonym, '★ ' || rating, round(offer_price) || ' тг') AS marker_label
FROM shown
WHERE distance_m IS NULL OR distance_m <= zone_radius_m;

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

-- =====================================================================
-- ТЕСТОВЫЕ ДАННЫЕ
-- =====================================================================

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
 (27,'people',   '<circle cx="9" cy="8" r="3"/><circle cx="17" cy="9" r="2.5"/><path d="M3 20a6 6 0 0 1 12 0M15 20a4 4 0 0 1 6-3.5"/>')
) v(id, code, body);

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
(40005,'PKG_COMPANY_10',   27,'Компания + 10 субагентов','Company + 10 subagents',NULL);

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
(85,'SANCTION_REASON','REVENUE_DROP',  30085,5,NULL,NULL,1);

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
(1101,110,'variant', 11101,1),
(1102,110,'variant', 11102,2),
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
(72,180,'price_unit',     NULL,NULL,'hour');

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
(6,2,NULL);

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

INSERT INTO ui_config (agent_id, config_json, checksum)
SELECT agent_id, cfg, md5(cfg::text)
FROM (VALUES
  (1, '{"lang":"RUS","theme":"dark","map":"OSM_OFFLINE_ALMATY","services":[100,110,101],"version":6}'::jsonb),
  (2, '{"lang":"RUS","theme":"light","map":"OSM_OFFLINE_ALMATY","services":[100],"version":6}'::jsonb)
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

-- Сделки (оплата напрямую между агентами, сбор платформы = 0)
INSERT INTO deal VALUES
(1,1001,1,2,16,61,43.238000,76.889000,100,NULL,NULL,'2026-10-24 18:00','2026-10-24 18:15',
 800.00,2000.00,4.00,1000.00,0.00,1000.00,'qr_7f3a9c','2026-10-24 17:40','2026-10-24 18:16'),
(2,1011,1,4,19,61,43.250000,76.900000,1000,43.260000,76.920000,'2026-10-25 12:00','2026-10-25 13:00',
 1500.00,3000.00,NULL,1800.00,0.00,1800.00,NULL,'2026-10-25 10:00','2026-10-25 11:20'),
(3,1001,5,4,16,61,43.240000,76.890000,200,NULL,NULL,'2026-10-22 09:00','2026-10-22 10:00',
 900.00,1800.00,NULL,1200.00,0.00,1200.00,'qr_1a2b3c','2026-10-22 08:30','2026-10-22 10:02'),
(4,1001,1,4,16,61,43.241000,76.892000,150,NULL,NULL,'2026-10-23 19:00','2026-10-23 19:30',
 700.00,1500.00,NULL, 900.00,0.00, 900.00,'qr_4d5e6f','2026-10-23 18:40','2026-10-23 19:31'),
(5,1001,5,2,16,61,43.239000,76.889500,100,NULL,NULL,'2026-10-23 20:00','2026-10-23 20:30',
 700.00,1400.00,NULL, 800.00,0.00, 800.00,'qr_7g8h9i','2026-10-23 19:45','2026-10-23 20:33'),
-- Сделка 6: кейс A/B/C/D, ждет выбора клиента
(6,1001,1,NULL,13,61,43.238000,76.889000,100,NULL,NULL,'2026-10-24 21:00','2026-10-24 22:00',
 700.00,1500.00,NULL,NULL,NULL,NULL,NULL,'2026-10-24 20:30',NULL),
-- Сделка 7: такси "Эконом" из точки А в точку Б
(7,1101,1,NULL,13,61,43.238000,76.889000,1500,43.256000,76.945000,'2026-10-24 22:30','2026-10-24 22:45',
 1200.00,1800.00,4.00,NULL,NULL,NULL,NULL,'2026-10-24 22:20',NULL);

INSERT INTO deal_offer VALUES
(1,1,2,1000.00,'auto',  43.238764,76.889000,1,'2026-10-24 17:41'),  -- 85 м
(2,1,4,1200.00,'manual',43.238000,76.890173,0,'2026-10-24 17:45'),  -- 95 м
(3,2,4,1800.00,'manual',NULL,     NULL,     1,'2026-10-25 10:10'),
(4,6,2,1000.00,'auto',  43.238270,76.889000,0,'2026-10-24 20:31'),  -- точка B, 30 м
(5,6,5, 900.00,'auto',  43.237370,76.889000,0,'2026-10-24 20:32'),  -- точка C, 70 м
(6,6,4,1100.00,'manual',43.238000,76.889617,0,'2026-10-24 20:35'),  -- точка D, 50 м
(7,6,6, 950.00,'manual',43.239170,76.889000,0,'2026-10-24 20:36'),  -- точка E, 130 м, вне зоны
(8,7,6,1500.00,'auto',  43.245000,76.900000,0,'2026-10-24 22:21');  -- машина в 1.2 км

-- Поля запроса и отклика: пассажиры (запрос такси), время подачи (отклик), описание (доставка)
INSERT INTO deal_attribute VALUES
(1,7,NULL,22,'2','2026-10-24 22:20'),
(2,7,8,   25,'6','2026-10-24 22:21'),
(3,2,NULL,15,'ул. Абая 150, кв. 12','2026-10-25 10:00'),
(4,2,3,   25,'40','2026-10-25 10:10');

INSERT INTO deal_status_log VALUES
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
(12,6,11,1,   NULL,NULL,'2026-10-24 20:30'),
(13,6,12,NULL,NULL,NULL,'2026-10-24 20:30'),
(14,6,13,NULL,NULL,NULL,'2026-10-24 20:31'),
(15,7,11,1,   NULL,NULL,'2026-10-24 22:20'),
(16,7,12,NULL,NULL,NULL,'2026-10-24 22:20'),
(17,7,13,NULL,NULL,NULL,'2026-10-24 22:21');

INSERT INTO deal_review VALUES
(1,1,2,2,5.0,'Место рядом со входом','2026-10-24 18:17'),
(1,2,1,1,4.0,NULL,'2026-10-24 18:18'),
(2,1,4,2,2.0,'Отменил за час до доставки','2026-10-25 11:25');

INSERT INTO notification VALUES
(1,1,6,   71,'push',  '{"deal_offer_id":4}','2026-10-24 20:31','2026-10-24 20:31',NULL),
(2,1,6,   71,'push',  '{"deal_offer_id":5}','2026-10-24 20:32','2026-10-24 20:32','2026-10-24 20:33'),
(3,1,2,   73,'push',  '{"by":"provider","reason":"LATE"}','2026-10-25 11:20','2026-10-25 11:20','2026-10-25 11:22'),
(4,1,7,   71,'push',  '{"deal_offer_id":8}','2026-10-24 22:21','2026-10-24 22:21',NULL),
(5,2,NULL,75,'in_app','{"subscription_id":4,"date_end":"2026-10-31"}','2026-10-24 09:00','2026-10-24 09:00',NULL);

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

-- Санкции агента 4: предупреждение, понижение рейтинга, блокировка на неделю
INSERT INTO agent_sanction VALUES
(1,4,NULL,81,'warning',    NULL,'2026-10-10 12:00',NULL,NULL,1),
(2,4,NULL,82,'rating_down',NULL,'2026-10-15 12:00',NULL,NULL,1),
(3,4,NULL,83,'block',      1,   '2026-10-26 12:00','2026-10-26 12:00','2026-11-02 12:00',1);
