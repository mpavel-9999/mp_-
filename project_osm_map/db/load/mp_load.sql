-- =====================================================================
-- Нагрузочный стенд mp_load (этап М1, план docs/v0.7/plans/M1_db_base.md).
-- Загружается ПОСЛЕ db/mp_service.sql; схему mp_service не меняет.
-- Код генератора и результаты замеров - в схеме mp_load; сгенерированные строки - в таблицах mp_service
-- (замеряются настоящие запросы), номера агентов и фактов - с 1 000 000, кейс A/B/C/D не затрагивается.
--   psql ... -f db/load/mp_load.sql
--   SELECT mp_load.generate();            -- 100 000 агентов, 1 млн сделок (≈ 10-20 мин)
--   SELECT * FROM mp_load.check();        -- целостность: все violations = 0
-- Убрать стенд - перезагрузить db/mp_service.sql.
-- =====================================================================

DROP SCHEMA IF EXISTS mp_load CASCADE;
CREATE SCHEMA mp_load;
COMMENT ON SCHEMA mp_load IS 'Нагрузочный стенд М1: генератор данных в mp_service и результаты замеров';

-- LIST Запуски генератора
CREATE TABLE mp_load.gen_run (
    run_id        serial       PRIMARY KEY,
    p_agents      int          NOT NULL,
    p_deals       int          NOT NULL,
    p_notify_days int          NOT NULL,
    p_seed        float8       NOT NULL,
    date_start    timestamp    NOT NULL DEFAULT clock_timestamp(),
    date_end      timestamp,
    duration_s    numeric,
    row_counts    jsonb,
    table_mb      jsonb,
    checksum      text
);
COMMENT ON TABLE mp_load.gen_run IS 'Запуски генератора нагрузки: параметры, время, строк и размер по таблицам, контрольная сумма';
COMMENT ON COLUMN mp_load.gen_run.run_id IS 'Номер запуска';
COMMENT ON COLUMN mp_load.gen_run.p_agents IS 'Агентов (каждый четвертый - поставщик)';
COMMENT ON COLUMN mp_load.gen_run.p_deals IS 'Сделок всего (из них 2 300 открытых)';
COMMENT ON COLUMN mp_load.gen_run.p_notify_days IS 'За сколько последних суток создаются уведомления';
COMMENT ON COLUMN mp_load.gen_run.p_seed IS 'Зерно setseed - одинаковое зерно дает одинаковые данные';
COMMENT ON COLUMN mp_load.gen_run.date_start IS 'Начало генерации';
COMMENT ON COLUMN mp_load.gen_run.date_end IS 'Окончание генерации';
COMMENT ON COLUMN mp_load.gen_run.duration_s IS 'Длительность, с';
COMMENT ON COLUMN mp_load.gen_run.row_counts IS 'Вставлено строк по таблицам mp_service';
COMMENT ON COLUMN mp_load.gen_run.table_mb IS 'Размер таблиц mp_service с индексами после генерации, МБ';
COMMENT ON COLUMN mp_load.gen_run.checksum IS 'md5 ключевых полей сделок и откликов (без дат) - проверка повторяемости';

-- FAKT Результаты замеров горячих запросов
CREATE TABLE mp_load.bench_result (
    bench_id      serial       PRIMARY KEY,
    label         text         NOT NULL,
    query_code    text         NOT NULL,
    method        text         NOT NULL CHECK (method IN ('sql','http')),
    clients       int          NOT NULL DEFAULT 1,
    calls         int          NOT NULL,
    errors        int          NOT NULL DEFAULT 0,
    ms_p50        numeric,
    ms_p95        numeric,
    ms_max        numeric,
    rps           numeric,
    note          text,
    date_creation timestamp    NOT NULL DEFAULT clock_timestamp()
);
COMMENT ON TABLE mp_load.bench_result IS 'Результаты замеров горячих запросов Q1-Q9 по этапам М1 (как есть, после пула, индексов, bigint, параметров)';
COMMENT ON COLUMN mp_load.bench_result.bench_id IS 'Номер замера';
COMMENT ON COLUMN mp_load.bench_result.label IS 'Метка этапа: before, pool, indexes, bigint, settings, after';
COMMENT ON COLUMN mp_load.bench_result.query_code IS 'Код запроса Q1-Q9 (план М1, «Горячие запросы замера»)';
COMMENT ON COLUMN mp_load.bench_result.method IS 'sql - вызов в БД (mp_load.bench), http - через API (server/tests/load_http.py)';
COMMENT ON COLUMN mp_load.bench_result.clients IS 'Параллельных клиентов (для sql - 1)';
COMMENT ON COLUMN mp_load.bench_result.calls IS 'Выполнено вызовов';
COMMENT ON COLUMN mp_load.bench_result.errors IS 'Вызовов с ошибкой';
COMMENT ON COLUMN mp_load.bench_result.ms_p50 IS 'Медиана времени вызова, мс';
COMMENT ON COLUMN mp_load.bench_result.ms_p95 IS '95-й процентиль времени вызова, мс';
COMMENT ON COLUMN mp_load.bench_result.ms_max IS 'Максимум времени вызова, мс';
COMMENT ON COLUMN mp_load.bench_result.rps IS 'Вызовов в секунду (для http - всех клиентов вместе)';
COMMENT ON COLUMN mp_load.bench_result.note IS 'Примечание';
COMMENT ON COLUMN mp_load.bench_result.date_creation IS 'Когда замерено';


-- ---------------------------------------------------------------------
-- Генератор
-- ---------------------------------------------------------------------
CREATE FUNCTION mp_load.generate(p_agents int DEFAULT 100000, p_deals int DEFAULT 1000000,
                                 p_notify_days int DEFAULT 18, p_seed float8 DEFAULT 0.42)
RETURNS jsonb LANGUAGE plpgsql SET search_path = mp_service, public AS $$
    -- Что делает: заполняет mp_service нагрузочными данными по модели 05_db_scaling.md, раздел 2.
    -- Методика: строки вставляются напрямую в итоговых статусах при session_replication_role = replica
    -- (триггеры правил и проверки внешних ключей выключены на время транзакции - иначе каждый запрос
    -- шел бы через подбор). Целостность проверяет mp_load.check(). Повторяемость - setseed и запрет
    -- параллельного выполнения (порядок строк и вызовов random() один и тот же).
    -- Агенты 1 000 000 + n: каждый - клиент, каждый четвертый (n % 4 = 0) - еще и поставщик с 1-3 услугами.
    -- Сделки 1 000 000 + i: i <= 150 MATCHING, <= 300 OFFERS, <= 1 800 AGREED, <= 2 300 CONTACT,
    -- остальные - закрытые за ~50 суток назад (CLOSED 70 %, EXPIRED 12 %, отмены 16 %, NOT_CLOSED 2 %).
    -- Исполнитель и отклики берутся из поставщиков той же услуги (ps), уведомления - за p_notify_days суток.
DECLARE
    v_base   CONSTANT bigint := 1000000;
    v_run    int;
    v_nv     int;
    v_ns     int;
    v_n      bigint;
    v_rows   jsonb := '{}';
    v_mb     jsonb;
    v_sum    text;
BEGIN
    IF EXISTS (SELECT 1 FROM agent WHERE agent_id > v_base) THEN
        RAISE EXCEPTION 'Стенд уже сгенерирован - перезагрузите db/mp_service.sql и db/load/mp_load.sql';
    END IF;
    INSERT INTO mp_load.gen_run (p_agents, p_deals, p_notify_days, p_seed)
    VALUES (p_agents, p_deals, p_notify_days, p_seed) RETURNING run_id INTO v_run;

    PERFORM set_config('session_replication_role', 'replica', true);
    PERFORM set_config('max_parallel_workers_per_gather', '0', true);
    PERFORM setseed(p_seed);

    -- Варианты услуг (листья дерева, кроме системного узла) и их узел уровня service (для рейтинга)
    CREATE TEMP TABLE g_v ON COMMIT DROP AS
    WITH RECURSIVE up(v, s, depth) AS (
        SELECT service_id, service_id, 0 FROM service
        UNION ALL
        SELECT up.v, p.parent_service_id, up.depth + 1 FROM up JOIN service p ON p.service_id = up.s
        WHERE p.parent_service_id IS NOT NULL)
    SELECT (row_number() OVER (ORDER BY s.service_id) - 1)::int AS idx, s.service_id,
           get_setting(s.service_id, 'geo_mode') AS geo_mode,
           (SELECT up.s FROM up JOIN service n ON n.service_id = up.s
            WHERE up.v = s.service_id AND n.node_type = 'service' ORDER BY up.depth LIMIT 1) AS svc_node
    FROM service s
    WHERE s.node_type <> 'system'
      AND NOT EXISTS (SELECT 1 FROM service c WHERE c.parent_service_id = s.service_id);
    SELECT count(*) INTO v_nv FROM g_v;

    -- Агенты, роли, псевдоним, подписка, конфигурация интерфейса
    INSERT INTO agent (agent_id, interface_lang_id, date_creation, date_start)
    SELECT v_base + n, 1, now() - random() * interval '365 days', now() - interval '400 days'
    FROM generate_series(1, p_agents) n;
    GET DIAGNOSTICS v_n = ROW_COUNT; v_rows := v_rows || jsonb_build_object('agent', v_n);

    INSERT INTO agent_role (agent_id, role_id)
    SELECT v_base + n, 1 FROM generate_series(1, p_agents) n
    UNION ALL
    SELECT v_base + n, 2 FROM generate_series(4, p_agents, 4) n;
    GET DIAGNOSTICS v_n = ROW_COUNT; v_rows := v_rows || jsonb_build_object('agent_role', v_n);

    INSERT INTO agent_attribute (agent_id, attribute_id, attribute_value, date_start)
    SELECT v_base + n, 16, 'Load_' || n, now() - interval '400 days' FROM generate_series(1, p_agents) n;
    GET DIAGNOSTICS v_n = ROW_COUNT; v_rows := v_rows || jsonb_build_object('agent_attribute', v_n);

    INSERT INTO agent_subscription (subscription_id, agent_id, package_id, is_trial, date_start, date_end)
    SELECT v_base + n, v_base + n, CASE WHEN n % 4 = 0 THEN 3 ELSE 1 END, 0,
           now() - interval '60 days', now() + interval '1 year'
    FROM generate_series(1, p_agents) n;
    GET DIAGNOSTICS v_n = ROW_COUNT; v_rows := v_rows || jsonb_build_object('agent_subscription', v_n);

    INSERT INTO ui_config (agent_id, config_json, checksum)
    SELECT v_base + n, c.cfg, md5(c.cfg::text)
    FROM generate_series(1, p_agents) n
    CROSS JOIN (SELECT '{"lang":"RUS","template":"A","scheme":"A_LIGHT","map":"OSM","version":7}'::jsonb AS cfg) c;
    GET DIAGNOSTICS v_n = ROW_COUNT; v_rows := v_rows || jsonb_build_object('ui_config', v_n);

    -- Услуги поставщиков: 1-3 разных варианта, точка и зона - Алматы и окрестности (±13 км)
    INSERT INTO agent_service (agent_id, service_id, geo_lat, geo_lon, zone_radius_m,
                               price_min, price_max, unit_price_min, is_auto, date_start)
    SELECT v_base + n, v.service_id,
           CASE WHEN v.geo_mode <> 'none' THEN round((43.238 + (random() - 0.5) * 0.24)::numeric, 6) END,
           CASE WHEN v.geo_mode <> 'none' THEN round((76.889 + (random() - 0.5) * 0.34)::numeric, 6) END,
           CASE WHEN v.geo_mode <> 'none' THEN 2000 + floor(random() * 4) * 1000 END,
           500 + floor(random() * 4) * 250, 20000,
           CASE WHEN v.geo_mode = 'route' THEN 150 END,
           CASE WHEN random() < 0.1 THEN 1 ELSE 0 END, now() - interval '60 days'
    FROM generate_series(4, p_agents, 4) n
    CROSS JOIN generate_series(1, 1 + (n / 4) % 3) k
    JOIN g_v v ON v.idx = ((n / 4) * 7 + k * 5) % v_nv;
    GET DIAGNOSTICS v_n = ROW_COUNT; v_rows := v_rows || jsonb_build_object('agent_service', v_n);

    -- Поставщики по услугам: ps(service_id, rn) - выбор поставщика по номеру, psc - их число
    CREATE TEMP TABLE g_ps ON COMMIT DROP AS
    SELECT service_id, row_number() OVER (PARTITION BY service_id ORDER BY agent_id)::int AS rn, agent_id
    FROM agent_service WHERE agent_id > v_base;
    CREATE UNIQUE INDEX ON g_ps (service_id, rn);
    CREATE TEMP TABLE g_psc ON COMMIT DROP AS
    SELECT service_id, count(*)::int AS cnt FROM g_ps GROUP BY service_id;
    -- Услуги сделок - только те, у которых есть поставщики
    CREATE TEMP TABLE g_dv ON COMMIT DROP AS
    SELECT (row_number() OVER (ORDER BY v.service_id) - 1)::int AS idx, v.service_id, v.geo_mode, v.svc_node, c.cnt
    FROM g_v v JOIN g_psc c USING (service_id);
    SELECT count(*) INTO v_nv FROM g_dv;
    -- Услуги с публикациями (slot_required = 1)
    CREATE TEMP TABLE g_sl ON COMMIT DROP AS
    SELECT (row_number() OVER (ORDER BY service_id) - 1)::int AS idx, service_id, geo_mode, cnt
    FROM g_dv WHERE get_setting(service_id, 'slot_required') = '1';
    SELECT count(*) INTO v_ns FROM g_sl;

    -- Публикации: ~3 000 активных на ближайшие сутки, остальные - закрытые и отмененные за 50 суток
    INSERT INTO provider_slot (slot_id, agent_id, service_id, geo_lat, geo_lon, radius_m,
                               geo_to_lat, geo_to_lon, radius_to_m, route_length_m,
                               period_start, period_end, price_min, slot_status, date_creation)
    SELECT v_base + g.i, a.agent_id, a.service_id, a.geo_lat, a.geo_lon, 300,
           CASE WHEN s.geo_mode = 'route' THEN round((43.238 + (g.r2 - 0.5) * 0.24)::numeric, 6) END,
           CASE WHEN s.geo_mode = 'route' THEN round((76.889 + (g.r3 - 0.5) * 0.34)::numeric, 6) END,
           CASE WHEN s.geo_mode = 'route' THEN 500 END,
           CASE WHEN s.geo_mode = 'route' THEN 3000 + floor(g.r2 * 17000) END,
           g.ps, g.ps + (2 + floor(g.r3 * 5)) * interval '1 hour', 500,
           CASE WHEN g.i > p_deals / 2 - 3000 THEN 'active' WHEN g.r1 < 0.85 THEN 'closed' ELSE 'cancelled' END,
           g.ps - interval '1 hour'
    FROM (SELECT i, random() AS r1, random() AS r2, random() AS r3,
                 CASE WHEN i > p_deals / 2 - 3000 THEN now() + random() * interval '20 hours'
                      ELSE now() - interval '1 day' - (1 - i::float8 / (p_deals / 2)) * interval '50 days' END AS ps
          FROM generate_series(1, p_deals / 2) i) g
    JOIN g_sl s ON s.idx = g.i % v_ns
    JOIN g_ps p ON p.service_id = s.service_id AND p.rn = 1 + (g.i::bigint * 7919) % s.cnt
    JOIN agent_service a ON a.agent_id = p.agent_id AND a.service_id = p.service_id
    ORDER BY g.i;
    GET DIAGNOSTICS v_n = ROW_COUNT; v_rows := v_rows || jsonb_build_object('provider_slot', v_n);

    -- Сделки: черновик со всеми случайными значениями
    CREATE TEMP TABLE g_d ON COMMIT DROP AS
    SELECT i, v_base + i AS deal_id, v.service_id, v.geo_mode, v.svc_node, v.cnt,
           v_base + 4 * floor(random() * (p_agents / 4)) + 1 + floor(random() * 3) AS client_id,
           floor(random() * 1000000)::int AS base,
           CASE WHEN o <= 150 THEN 12 WHEN o <= 300 THEN 13 WHEN o <= 1800 THEN 14 WHEN o <= 2300 THEN 15
                WHEN r < 0.70 THEN 16 WHEN r < 0.82 THEN 20 WHEN r < 0.90 THEN 17 WHEN r < 0.95 THEN 18
                WHEN r < 0.98 THEN 19 ELSE 21 END AS status_id,
           CASE WHEN o <= 300 THEN now() - random() * interval '30 minutes'
                WHEN o <= 2300 THEN now() - interval '30 minutes' - random() * interval '90 minutes'
                ELSE now() - interval '1 day' - (1 - i::float8 / greatest(p_deals - 2300, 1)) * interval '50 days'
                     - random() * interval '1 hour' END AS dc,
           500 * (1 + floor(random() * 20)) AS price,
           random() AS rk,
           round((43.238 + (random() - 0.5) * 0.24)::numeric, 6) AS lat,
           round((76.889 + (random() - 0.5) * 0.34)::numeric, 6) AS lon,
           round((43.238 + (random() - 0.5) * 0.24)::numeric, 6) AS to_lat,
           round((76.889 + (random() - 0.5) * 0.34)::numeric, 6) AS to_lon
    FROM (SELECT i, p_deals - i + 1 AS o, random() AS r, floor(random() * v_nv)::int AS vi
          FROM generate_series(1, p_deals) i) s
    JOIN g_dv v ON v.idx = s.vi;
    ALTER TABLE g_d ADD COLUMN k int, ADD COLUMN has_provider boolean, ADD COLUMN provider_id bigint,
                    ADD COLUMN period_start timestamp, ADD COLUMN date_agreed timestamp, ADD COLUMN date_close timestamp;
    UPDATE g_d SET
        has_provider = status_id IN (14, 15, 16, 18, 19, 21),
        k = CASE WHEN status_id = 12 THEN 0
                 WHEN status_id = 13 THEN 1 + floor(rk * 4)
                 WHEN status_id = 20 THEN floor(rk * 3)
                 WHEN status_id = 17 THEN floor(rk * 2)
                 ELSE 1 + floor(rk * 5) END,
        period_start = CASE WHEN status_id IN (12, 13) THEN now() + interval '2 hours' + rk * interval '10 hours'
                            WHEN status_id IN (14, 15) THEN now() + interval '10 minutes' + rk * interval '3 hours'
                            ELSE dc + interval '30 minutes' + rk * interval '5 hours' END;
    UPDATE g_d d SET provider_id = p.agent_id,
                     date_agreed = d.dc + interval '5 minutes',
                     date_close = CASE WHEN d.status_id IN (16, 21) THEN d.period_start + interval '70 minutes'
                                       WHEN d.status_id IN (18, 19) THEN d.dc + interval '20 minutes' END
    FROM g_ps p WHERE d.has_provider AND p.service_id = d.service_id AND p.rn = d.base % d.cnt + 1;
    UPDATE g_d SET date_close = dc + interval '20 minutes' WHERE status_id = 17;
    UPDATE g_d SET date_close = period_start + interval '1 minute' WHERE status_id = 20;
    CREATE UNIQUE INDEX ON g_d (deal_id);

    INSERT INTO deal (deal_id, service_id, client_agent_id, provider_agent_id, deal_status_id, payment_method_id,
                      geo_lat, geo_lon, zone_radius_m, geo_to_lat, geo_to_lon, route_length_m,
                      period_start, period_end, price_forecast_min, price_forecast_max, client_price,
                      price_provider, fee_amount, price_total, close_secret, date_creation, date_agreed, date_close)
    SELECT deal_id, service_id, client_id, provider_id, status_id, 61,
           CASE WHEN geo_mode <> 'none' THEN lat END, CASE WHEN geo_mode <> 'none' THEN lon END,
           CASE WHEN geo_mode = 'point' THEN 3000 END,
           CASE WHEN geo_mode = 'route' THEN to_lat END, CASE WHEN geo_mode = 'route' THEN to_lon END,
           CASE WHEN geo_mode = 'route' THEN 3000 + floor(rk * 17000) END,
           period_start, period_start + interval '1 hour' + floor(rk * 3) * interval '1 hour',
           price * 0.8, price * 1.2, price,
           CASE WHEN has_provider THEN price END, CASE WHEN has_provider THEN 0 END,
           CASE WHEN has_provider THEN price END,
           CASE WHEN status_id IN (14, 15) THEN md5(deal_id::text || p_seed::text) END,
           dc, date_agreed, date_close
    FROM g_d ORDER BY deal_id;
    GET DIAGNOSTICS v_n = ROW_COUNT; v_rows := v_rows || jsonb_build_object('deal', v_n);

    -- Отклики: первый - исполнителя (selected), остальные - другие поставщики той же услуги
    PERFORM setval('deal_offer_seq', v_base);
    INSERT INTO deal_offer (deal_id, provider_agent_id, offer_price, offer_mode, geo_lat, geo_lon,
                            offer_status, date_creation)
    SELECT d.deal_id, p.agent_id, d.price * (0.9 + 0.05 * j), CASE WHEN j % 4 = 3 THEN 'auto' ELSE 'manual' END,
           d.lat, d.lon,
           CASE WHEN d.has_provider AND j = 0 THEN 'selected'
                WHEN d.status_id = 13 THEN 'active'
                WHEN d.has_provider THEN 'declined'
                WHEN d.status_id = 20 THEN 'expired'
                ELSE 'withdrawn' END,
           d.dc + (j + 1) * interval '1 minute'
    FROM g_d d
    CROSS JOIN generate_series(0, d.k - 1) j
    JOIN g_ps p ON p.service_id = d.service_id AND p.rn = (d.base + j * 37) % d.cnt + 1
    ORDER BY d.deal_id, j
    ON CONFLICT (deal_id, provider_agent_id) DO NOTHING;
    GET DIAGNOSTICS v_n = ROW_COUNT; v_rows := v_rows || jsonb_build_object('deal_offer', v_n);

    -- Поля запроса: до двух атрибутов запроса из узлов-предков услуги
    PERFORM setval('deal_attribute_seq', v_base);
    CREATE TEMP TABLE g_sa ON COMMIT DROP AS
    WITH RECURSIVE up(v, s) AS (
        SELECT service_id, service_id FROM g_dv
        UNION ALL
        SELECT up.v, p.parent_service_id FROM up JOIN service p ON p.service_id = up.s
        WHERE p.parent_service_id IS NOT NULL)
    SELECT v AS service_id, attribute_id, row_number() OVER (PARTITION BY v ORDER BY attribute_id) AS rn
    FROM (SELECT DISTINCT up.v, sa.attribute_id FROM up
          JOIN service_attribute sa ON sa.service_id = up.s AND sa.attr_scope = 'request') x;
    INSERT INTO deal_attribute (deal_id, attribute_id, attribute_value, date_creation)
    SELECT d.deal_id, a.attribute_id, 'load', d.dc
    FROM g_d d JOIN g_sa a ON a.service_id = d.service_id AND a.rn <= 2
    ORDER BY d.deal_id, a.attribute_id;
    GET DIAGNOSTICS v_n = ROW_COUNT; v_rows := v_rows || jsonb_build_object('deal_attribute', v_n);

    -- История статусов: MATCHING, OFFERS, AGREED, CONTACT (гео-услуги), итоговый статус
    PERFORM setval('deal_status_log_seq', v_base);
    INSERT INTO deal_status_log (deal_id, deal_status_id, agent_id, cancel_reason_id, date_creation)
    SELECT deal_id, st, ag, cr, dt FROM (
        SELECT deal_id, 12 AS st, client_id AS ag, NULL::int AS cr, dc AS dt, 1 AS o FROM g_d
        UNION ALL
        SELECT deal_id, 13, NULL, NULL, dc + interval '1 minute', 2 FROM g_d WHERE k > 0 AND status_id <> 12
        UNION ALL
        SELECT deal_id, 14, client_id, NULL, date_agreed, 3 FROM g_d WHERE has_provider
        UNION ALL
        SELECT deal_id, 15, NULL, NULL, period_start, 4 FROM g_d
        WHERE status_id IN (15, 16) AND geo_mode <> 'none'
        UNION ALL
        SELECT deal_id, status_id,
               CASE WHEN status_id IN (16, 17, 18) THEN client_id WHEN status_id = 19 THEN provider_id END,
               CASE WHEN status_id IN (17, 18, 19) THEN 31 END, date_close, 5
        FROM g_d WHERE status_id >= 16
    ) x ORDER BY deal_id, o;
    GET DIAGNOSTICS v_n = ROW_COUNT; v_rows := v_rows || jsonb_build_object('deal_status_log', v_n);

    -- Баллы: обе стороны закрытой сделки
    INSERT INTO deal_review (deal_id, from_agent_id, to_agent_id, to_role_id, score, date_creation)
    SELECT deal_id, client_id, provider_id, 2, 3 + floor(rk * 3), date_close + interval '1 minute'
    FROM g_d WHERE status_id = 16
    UNION ALL
    SELECT deal_id, provider_id, client_id, 1, 5 - floor(rk * 2), date_close + interval '2 minutes'
    FROM g_d WHERE status_id = 16;
    GET DIAGNOSTICS v_n = ROW_COUNT; v_rows := v_rows || jsonb_build_object('deal_review', v_n);

    -- Уведомления за последние p_notify_days суток: 20 NEW_REQUEST поставщикам услуги + 8 по ходу сделки
    PERFORM setval('notification_seq', v_base);
    INSERT INTO notification (agent_id, deal_id, notify_type_id, channel, payload, date_creation, date_sent, date_read)
    SELECT x.agent_id, x.deal_id, x.nt, 'in_app', jsonb_build_object('deal_id', x.deal_id),
           x.dt, x.dt, CASE WHEN x.dt < now() - interval '1 day' THEN x.dt + interval '2 minutes' END
    FROM (
        SELECT d.deal_id, d.dc + j * interval '30 seconds' AS dt,
               CASE WHEN j <= 20 THEN p.agent_id WHEN j IN (21, 22, 24, 27) THEN d.client_id ELSE d.provider_id END AS agent_id,
               CASE WHEN j <= 20 THEN 91 WHEN j = 21 THEN 71 WHEN j IN (22, 23) THEN 72 WHEN j IN (24, 25) THEN 74
                    WHEN j = 26 THEN 93 ELSE 96 END AS nt
        FROM g_d d
        CROSS JOIN generate_series(1, 28) j
        LEFT JOIN g_ps p ON j <= 20 AND p.service_id = d.service_id AND p.rn = (d.base + j * 101) % d.cnt + 1
        WHERE d.dc > now() - p_notify_days * interval '1 day'
          AND (j <= 20 OR (j = 21 AND d.k > 0) OR (j > 21 AND d.has_provider))
    ) x WHERE x.agent_id IS NOT NULL
    ORDER BY x.dt, x.deal_id;   -- номера уведомлений растут со временем, как при настоящей работе
    GET DIAGNOSTICS v_n = ROW_COUNT; v_rows := v_rows || jsonb_build_object('notification', v_n);

    -- Рейтинг поставщиков: по узлу service, за 4 последних дня
    INSERT INTO rating_history (agent_id, role_id, service_id, rating_date, system_rating, review_rating,
                                total_rating, deals_closed, deals_cancelled)
    SELECT agent_id, 2, svc_node, current_date - dd, 4.00, r, round((4.00 + r) / 2, 2), 10, 1
    FROM (SELECT x.agent_id, x.svc_node, dd, round((3 + random() * 2)::numeric, 2) AS r
          FROM (SELECT DISTINCT a.agent_id, v.svc_node FROM agent_service a JOIN g_v v USING (service_id)
                WHERE a.agent_id > v_base ORDER BY 1, 2) x
          CROSS JOIN generate_series(0, 3) dd ORDER BY 1, 2, 3) y;
    GET DIAGNOSTICS v_n = ROW_COUNT; v_rows := v_rows || jsonb_build_object('rating_history', v_n);

    -- Положения: стороны согласованных сделок (свежие) и часть остальных агентов (до 30 000)
    INSERT INTO agent_position (agent_id, geo_lat, geo_lon, source, accuracy_m, date_update)
    SELECT agent_id, round((43.238 + (random() - 0.5) * 0.24)::numeric, 6),
           round((76.889 + (random() - 0.5) * 0.34)::numeric, 6), 'gps', 10, dt
    FROM (
        SELECT DISTINCT ON (agent_id) agent_id, dt FROM (
            SELECT client_id AS agent_id, now() - random() * interval '60 seconds' AS dt, 1 AS o
            FROM g_d WHERE status_id IN (14, 15)
            UNION ALL
            SELECT provider_id, now() - random() * interval '60 seconds', 1 FROM g_d WHERE status_id IN (14, 15)
            UNION ALL
            SELECT v_base + n, now() - random() * interval '1 day', 2
            FROM generate_series(1, p_agents, greatest(p_agents / 26000, 1)) n
        ) u ORDER BY agent_id, o
    ) y;
    GET DIAGNOSTICS v_n = ROW_COUNT; v_rows := v_rows || jsonb_build_object('agent_position', v_n);

    -- Последовательности - после сгенерированных номеров
    PERFORM setval('deal_seq', (SELECT max(deal_id) FROM deal)::bigint);
    PERFORM setval('provider_slot_seq', (SELECT max(slot_id) FROM provider_slot)::bigint);

    SELECT md5(string_agg(concat_ws(':', deal_id, service_id, client_agent_id, provider_agent_id, deal_status_id,
                                    client_price), ',' ORDER BY deal_id))
           || '/' || (SELECT md5(string_agg(concat_ws(':', deal_id, provider_agent_id, offer_status), ','
                                            ORDER BY deal_offer_id)) FROM deal_offer WHERE deal_id > v_base)
    INTO v_sum FROM deal WHERE deal_id > v_base;

    SELECT jsonb_object_agg(c.relname, round(pg_total_relation_size(c.oid) / 1048576.0, 1) ORDER BY c.relname)
    INTO v_mb FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
    WHERE n.nspname = 'mp_service' AND c.relkind = 'r';

    UPDATE mp_load.gen_run SET date_end = clock_timestamp(),
           duration_s = round(extract(epoch FROM clock_timestamp() - date_start)::numeric, 1),
           row_counts = v_rows, table_mb = v_mb, checksum = v_sum
    WHERE run_id = v_run;
    RETURN v_rows || jsonb_build_object('checksum', v_sum);
END $$;
COMMENT ON FUNCTION mp_load.generate(int, int, int, float8) IS
    'Генератор нагрузки М1: агенты, услуги поставщиков, публикации, сделки, отклики, история, баллы, уведомления, рейтинг, положения в mp_service (номера с 1 000 000); после - ANALYZE и mp_load.check()';


-- ---------------------------------------------------------------------
-- Проверка целостности после генерации
-- ---------------------------------------------------------------------
CREATE FUNCTION mp_load.check()
RETURNS TABLE (check_name text, violations bigint) LANGUAGE plpgsql SET search_path = mp_service, public AS $$
    -- Что делает: проверяет то, что генератор обошел (триггеры и внешние ключи были выключены).
    -- Методика: (1) для каждого внешнего ключа mp_service - число строк-сирот (значения есть, родителя нет);
    -- (2) для каждого триггера check_dict_type - число значений справочника с чужим dict_type
    -- (пары «колонка, тип» берутся из аргументов триггера); (3) правила сделки, которые проверяют триггеры:
    -- исполнитель у AGREED / CONTACT / CLOSED, ровно один выбранный отклик у сделки с исполнителем,
    -- выбранный отклик - от исполнителя.
DECLARE
    r       record;
    v_cols  text;
    v_join  text;
    v_args  text[];
    i       int;
BEGIN
    FOR r IN
        SELECT c.conname, c.conrelid::regclass AS child, c.confrelid::regclass AS parent, c.conkey, c.confkey, c.conrelid, c.confrelid
        FROM pg_constraint c JOIN pg_namespace n ON n.oid = c.connamespace
        WHERE n.nspname = 'mp_service' AND c.contype = 'f' ORDER BY c.conname
    LOOP
        SELECT string_agg(format('x.%I IS NOT NULL', a.attname), ' AND '),
               string_agg(format('p.%I = x.%I', pa.attname, a.attname), ' AND ')
        INTO v_cols, v_join
        FROM unnest(r.conkey, r.confkey) k(ck, pk)
        JOIN pg_attribute a ON a.attrelid = r.conrelid AND a.attnum = k.ck
        JOIN pg_attribute pa ON pa.attrelid = r.confrelid AND pa.attnum = k.pk;
        EXECUTE format('SELECT count(*) FROM %s x WHERE %s AND NOT EXISTS (SELECT 1 FROM %s p WHERE %s)',
                       r.child, v_cols, r.parent, v_join) INTO violations;
        check_name := 'fk ' || r.conname;
        RETURN NEXT;
    END LOOP;

    FOR r IN
        SELECT t.tgname, t.tgrelid::regclass AS tbl, t.tgnargs, t.tgargs
        FROM pg_trigger t JOIN pg_proc p ON p.oid = t.tgfoid JOIN pg_namespace n ON n.oid = p.pronamespace
        WHERE n.nspname = 'mp_service' AND p.proname = 'check_dict_type' ORDER BY t.tgname
    LOOP
        v_args := string_to_array(rtrim(encode(r.tgargs, 'escape'), '\000'), '\000');
        FOR i IN 1 .. array_length(v_args, 1) BY 2 LOOP
            EXECUTE format('SELECT count(*) FROM %s x LEFT JOIN dictionary d ON d.dict_id = x.%I '
                           'WHERE x.%I IS NOT NULL AND d.dict_type IS DISTINCT FROM %L',
                           r.tbl, v_args[i], v_args[i], v_args[i + 1]) INTO violations;
            check_name := format('dict %s.%s = %s', r.tbl, v_args[i], v_args[i + 1]);
            RETURN NEXT;
        END LOOP;
    END LOOP;

    check_name := 'deal: исполнитель у AGREED/CONTACT/CLOSED';
    SELECT count(*) INTO violations FROM deal WHERE deal_status_id IN (14, 15, 16) AND provider_agent_id IS NULL;
    RETURN NEXT;
    check_name := 'deal: один выбранный отклик у сделки с исполнителем';
    SELECT count(*) INTO violations FROM deal d
    WHERE d.provider_agent_id IS NOT NULL
      AND (SELECT count(*) FROM deal_offer o WHERE o.deal_id = d.deal_id AND o.offer_status = 'selected') <> 1;
    RETURN NEXT;
    check_name := 'deal_offer: выбранный отклик - от исполнителя';
    SELECT count(*) INTO violations FROM deal_offer o JOIN deal d USING (deal_id)
    WHERE o.offer_status = 'selected' AND o.provider_agent_id IS DISTINCT FROM d.provider_agent_id;
    RETURN NEXT;
END $$;
COMMENT ON FUNCTION mp_load.check() IS
    'Целостность после генератора: сироты по каждому внешнему ключу mp_service, типы справочника по триггерам check_dict_type, исполнитель и выбранный отклик сделки; ожидается 0 нарушений везде';


-- ---------------------------------------------------------------------
-- Замер горячих запросов в БД (Q1-Q9, план М1)
-- ---------------------------------------------------------------------
CREATE FUNCTION mp_load.bench_sql(p_code text)
RETURNS text LANGUAGE sql IMMUTABLE AS $$
    -- Что делает: текст горячего запроса по коду Q1-Q9 - те же условия и функции, что в запросах сервера.
    -- Методика: параметры едины для всех запросов: $1 - агент, $2 - сделка или услуга, $3 - язык,
    -- $4 - номер уведомления «с»; неиспользуемые параметры игнорируются. Запросы, которые сервер делает
    -- на Java (маршруты такси через GraphHopper), сюда не входят - их видит HTTP-замер.
    SELECT CASE p_code
    WHEN 'Q1' THEN  -- GET /notifications?since= (DealApi.notifications)
        'SELECT coalesce(json_agg(x ORDER BY x.notification_id), ''[]'')::text FROM ('
        || ' SELECT n.notification_id, n.deal_id, t.dict_code AS type, coalesce(tl.text_value, tr.text_value) AS text,'
        || '        n.payload, n.date_creation, n.date_read'
        || ' FROM notification n JOIN dictionary t ON t.dict_id = n.notify_type_id'
        || ' JOIN glossary_text tr ON tr.glossary_id = t.glossary_id AND tr.interface_lang_id = 1'
        || ' LEFT JOIN glossary_text tl ON tl.glossary_id = t.glossary_id AND tl.interface_lang_id = $3'
        || ' WHERE n.agent_id = $1 AND n.notification_id > $4 ORDER BY n.notification_id LIMIT 200) x'
    WHEN 'Q2' THEN  -- PUT /me/position (ConfigApi.position)
        'WITH u AS (INSERT INTO agent_position (agent_id, geo_lat, geo_lon, source, accuracy_m)'
        || ' VALUES ($1, 43.24, 76.89, ''gps'', 10) ON CONFLICT (agent_id) DO UPDATE SET geo_lat = excluded.geo_lat,'
        || ' geo_lon = excluded.geo_lon, source = excluded.source, accuracy_m = excluded.accuracy_m'
        || ' RETURNING 1) SELECT (SELECT count(*) FROM u)::text || to_json(deal_contact_check($1))::text'
    WHEN 'Q3' THEN  -- GET /requests: кандидатские сделки поставщика (Jobs.matchingDealsFor, без такси)
        'SELECT count(*)::text FROM v_deal_candidates c JOIN deal d ON d.deal_id = c.deal_id'
        || ' WHERE c.provider_agent_id = $1 AND get_setting(d.service_id, ''geo_mode'') <> ''route'''
    WHEN 'Q4' THEN  -- POST /deals: создание (затем MATCHING и make_auto_offers - в mp_load.bench)
        'INSERT INTO deal (service_id, client_agent_id, deal_status_id, payment_method_id, geo_lat, geo_lon,'
        || ' zone_radius_m, period_start, period_end, client_price)'
        || ' VALUES ($2, $1, dict_id(''DEAL_STATUS'',''INIT''), dict_id(''PAYMENT_METHOD'',''DIRECT''), 43.24, 76.89, 3000,'
        || ' now() + interval ''3 hours'', now() + interval ''5 hours'', 3000) RETURNING deal_id::text'
    WHEN 'Q5' THEN  -- GET /deals «Мои сделки», role=all, status=active (DealApi.list)
        'SELECT count(*)::text FROM (SELECT d.deal_id, st.dict_code AS status,'
        || '  agent_pseudonym(CASE WHEN d.client_agent_id = $1 THEN d.provider_agent_id ELSE d.client_agent_id END) AS cp,'
        || '  get_setting(d.service_id, ''price_unit'') AS pu,'
        || '  (SELECT count(*) FROM deal_offer o WHERE o.deal_id = d.deal_id AND o.offer_status = ''active'') AS oa,'
        || '  (d.provider_agent_id IS NOT NULL AND st.dict_code IN (''CLOSED'',''CANCEL_CLIENT'',''CANCEL_PROVIDER'')'
        || '   AND NOT EXISTS (SELECT 1 FROM deal_review rv WHERE rv.deal_id = d.deal_id AND rv.from_agent_id = $1)) AS rp'
        || ' FROM deal d JOIN dictionary st ON st.dict_id = d.deal_status_id'
        || ' CROSS JOIN LATERAL (SELECT gloss(st.glossary_id, $3) AS t) stt'
        || ' JOIN service s ON s.service_id = d.service_id CROSS JOIN LATERAL (SELECT gloss(s.glossary_id, $3) AS t) sn'
        || ' WHERE d.client_agent_id = $1 OR d.provider_agent_id = $1 OR EXISTS (SELECT 1 FROM deal_offer o'
        || '   WHERE o.deal_id = d.deal_id AND o.provider_agent_id = $1 AND o.offer_status = ''active'')) x'
        || ' WHERE x.status NOT IN (''CLOSED'',''CANCEL_FREE'',''CANCEL_CLIENT'',''CANCEL_PROVIDER'',''EXPIRED'',''NOT_CLOSED'') OR x.rp'
    WHEN 'Q6' THEN  -- GET /deals/{id}: роль агента (DealApi.roleIn) и признаки этапа из SQL_DEAL
        'SELECT (SELECT CASE WHEN d.client_agent_id = $1 THEN ''client'' WHEN d.provider_agent_id = $1 THEN ''provider'''
        || '  WHEN EXISTS (SELECT 1 FROM deal_offer o WHERE o.deal_id = d.deal_id AND o.provider_agent_id = $1) THEN ''offerer'''
        || '  WHEN (SELECT ok FROM provider_can_offer(d.deal_id, $1)) THEN ''candidate'' ELSE ''none'' END)'
        || ' || (EXISTS (SELECT 1 FROM deal_status_log l WHERE l.deal_id = d.deal_id'
        || '     AND l.deal_status_id = dict_id(''DEAL_STATUS'',''CONTACT'')))::text'
        || ' || get_setting(d.service_id, ''place_mode'') FROM deal d WHERE d.deal_id = $2'
    WHEN 'Q7' THEN  -- GET /deals/{id}/offers глазами клиента (DealApi.offers)
        'SELECT coalesce(json_agg(o), ''[]'')::text FROM (SELECT l.*, a.is_testing = 1 AS is_testing,'
        || ' NOT agent_has_rating(l.provider_agent_id, ''PROVIDER'', l.service_id) AS newbie'
        || ' FROM v_deal_offer_list l JOIN agent a ON a.agent_id = l.provider_agent_id WHERE l.deal_id = $2) o'
    WHEN 'Q8' THEN  -- GET /map/slots: публикации в радиусе 1 км, все услуги (DealApi.slots)
        'SELECT count(*)::text FROM v_provider_slot_map v WHERE v.agent_id <> $1'
        || ' AND geo_distance_m(43.24, 76.89, v.show_lat, v.show_lon) <= 1000'
    WHEN 'Q9' THEN  -- плановое задание раз в минуту (Jobs.run)
        'SELECT run_deal_jobs()::text'
    END
$$;
COMMENT ON FUNCTION mp_load.bench_sql(text) IS 'Текст горячего запроса Q1-Q9 для замера (параметры: $1 агент, $2 сделка или услуга, $3 язык, $4 уведомление «с»)';

CREATE FUNCTION mp_load.bench(p_label text, p_calls int DEFAULT 50, p_heavy_calls int DEFAULT 3,
                              p_codes text[] DEFAULT ARRAY['Q1','Q2','Q3','Q4','Q5','Q6','Q7','Q8','Q9'])
RETURNS TABLE (query_code text, calls int, ms_p50 numeric, ms_p95 numeric, ms_max numeric)
LANGUAGE plpgsql SET search_path = mp_service, public AS $$
    -- Что делает: замеряет горячие запросы Q1-Q9 на стенде и пишет результат в mp_load.bench_result (method = sql).
    -- Методика: каждый вызов - в подтранзакции, которая откатывается (стенд не меняется, повторы сопоставимы);
    -- время - clock_timestamp() до и после EXECUTE; параметры - случайные из стенда при setseed(0.5):
    -- Q1 - получатель недавних уведомлений, «с» = последний номер - 50 000 (≈ 2 часа);
    -- Q2 - клиент согласованной сделки; Q3 - поставщик; Q4 - клиент и услуга-вариант (после вставки -
    -- переход в MATCHING и make_auto_offers, как в DealApi.create); Q5 - клиент недавней сделки;
    -- Q6 - клиент своей согласованной сделки; Q7 - сделка в OFFERS; Q8 - клиент; Q9 - без параметров.
    -- Q3, Q4, Q9 тяжелые - выполняются p_heavy_calls раз, остальные - p_calls раз.
    -- Сессия - как у сервера: mp.agent_id = агент вызова, mp.geo_secret задан, пояс Алматы.
DECLARE
    v_code   text;
    v_sql    text;
    v_n      int;
    v_k      int;
    i        int;
    v_me     bigint;
    v_deal   bigint;
    v_since  bigint;
    v_t0     timestamp;
    v_ms     float8[];
    v_out    text;
    v_err    int;
    a_notif  bigint[];
    a_pos    bigint[];
    a_prov   bigint[];
    a_cli    bigint[];
    a_agr    bigint[];
    a_agrc   bigint[];
    a_off    bigint[];
    a_svc    bigint[];
BEGIN
    PERFORM setseed(0.5);
    PERFORM set_config('TimeZone', 'Asia/Almaty', true);
    PERFORM set_config('mp.geo_secret', 'load', true);
    SELECT max(notification_id) - 50000 INTO v_since FROM notification;
    SELECT array_agg(agent_id) INTO a_notif FROM (SELECT DISTINCT agent_id FROM notification
        WHERE notification_id > v_since ORDER BY 1 LIMIT 500) x;
    SELECT array_agg(agent_id ORDER BY agent_id) INTO a_pos FROM (SELECT client_agent_id AS agent_id FROM deal
        WHERE deal_status_id IN (14, 15) AND deal_id > 1000000 ORDER BY deal_id LIMIT 500) x;
    SELECT array_agg(agent_id ORDER BY agent_id) INTO a_prov FROM (SELECT DISTINCT agent_id FROM agent_service
        WHERE agent_id > 1000000 ORDER BY 1 LIMIT 2000) x;
    SELECT array_agg(client_agent_id ORDER BY deal_id) INTO a_cli FROM (SELECT deal_id, client_agent_id FROM deal
        WHERE deal_id > 1000000 ORDER BY deal_id DESC LIMIT 500) x;
    SELECT array_agg(deal_id ORDER BY deal_id), array_agg(client_agent_id ORDER BY deal_id) INTO a_agr, a_agrc
        FROM deal WHERE deal_status_id IN (14, 15) AND deal_id > 1000000;
    SELECT array_agg(deal_id ORDER BY deal_id) INTO a_off FROM deal WHERE deal_status_id = 13 AND deal_id > 1000000;
    SELECT array_agg(DISTINCT service_id ORDER BY service_id) INTO a_svc FROM agent_service WHERE agent_id > 1000000;

    FOREACH v_code IN ARRAY p_codes LOOP
        v_sql := mp_load.bench_sql(v_code);
        v_n := CASE WHEN v_code IN ('Q3','Q4','Q9') THEN p_heavy_calls ELSE p_calls END;
        v_ms := '{}';
        v_err := 0;
        FOR i IN 1 .. v_n LOOP
            v_deal := NULL;
            v_me := CASE v_code
                WHEN 'Q1' THEN a_notif[1 + floor(random() * cardinality(a_notif))::int]
                WHEN 'Q2' THEN a_pos[1 + floor(random() * cardinality(a_pos))::int]
                WHEN 'Q3' THEN a_prov[1 + floor(random() * cardinality(a_prov))::int]
                WHEN 'Q9' THEN NULL
                ELSE a_cli[1 + floor(random() * cardinality(a_cli))::int] END;
            IF v_code = 'Q4' THEN
                v_deal := a_svc[1 + floor(random() * cardinality(a_svc))::int];
            ELSIF v_code = 'Q6' THEN
                v_k := 1 + floor(random() * cardinality(a_agr))::int;
                v_deal := a_agr[v_k]; v_me := a_agrc[v_k];
            ELSIF v_code = 'Q7' THEN
                v_deal := a_off[1 + floor(random() * cardinality(a_off))::int];
            END IF;
            BEGIN
                PERFORM set_config('mp.agent_id', coalesce(v_me::text, ''), true);
                v_t0 := clock_timestamp();
                EXECUTE v_sql INTO v_out USING v_me, v_deal, 1::bigint, v_since;
                IF v_code = 'Q4' THEN
                    UPDATE deal SET deal_status_id = dict_id('DEAL_STATUS', 'MATCHING') WHERE deal_id = v_out::bigint;
                    PERFORM make_auto_offers(v_out::bigint);
                END IF;
                v_ms := v_ms || extract(epoch FROM clock_timestamp() - v_t0)::float8 * 1000;
                RAISE EXCEPTION USING ERRCODE = 'MPRB0';
            EXCEPTION
                WHEN SQLSTATE 'MPRB0' THEN NULL;
                WHEN OTHERS THEN
                    v_err := v_err + 1;
                    RAISE NOTICE '% вызов %: %', v_code, i, SQLERRM;
            END;
        END LOOP;
        query_code := v_code;
        calls := cardinality(v_ms);
        SELECT round(percentile_cont(0.5) WITHIN GROUP (ORDER BY m)::numeric, 2),
               round(percentile_cont(0.95) WITHIN GROUP (ORDER BY m)::numeric, 2),
               round(max(m)::numeric, 2)
        INTO ms_p50, ms_p95, ms_max FROM unnest(v_ms) m;
        INSERT INTO mp_load.bench_result (label, query_code, method, clients, calls, errors, ms_p50, ms_p95, ms_max)
        VALUES (p_label, v_code, 'sql', 1, calls, v_err, ms_p50, ms_p95, ms_max);
        RETURN NEXT;
    END LOOP;
END $$;
COMMENT ON FUNCTION mp_load.bench(text, int, int, text[]) IS
    'Замер Q1-Q9 в БД: каждый вызов откатывается, медиана, p95 и максимум - в mp_load.bench_result с меткой p_label';

CREATE FUNCTION mp_load.explain(p_code text)
RETURNS SETOF text LANGUAGE plpgsql SET search_path = mp_service, public AS $$
    -- Что делает: план EXPLAIN (ANALYZE, BUFFERS) горячего запроса с типичными параметрами - для M1_bench/<метка>/.
    -- Методика: параметры - первые подходящие строки стенда (как в mp_load.bench, без случайности);
    -- выполнение в подтранзакции с откатом, строки плана копятся в массиве и отдаются после отката.
DECLARE
    v_me    bigint;
    v_deal  bigint;
    v_since bigint;
    v_lines text[] := '{}';
    r       record;
BEGIN
    PERFORM set_config('TimeZone', 'Asia/Almaty', true);
    PERFORM set_config('mp.geo_secret', 'load', true);
    SELECT max(notification_id) - 50000 INTO v_since FROM notification;
    IF p_code = 'Q1' THEN
        SELECT agent_id INTO v_me FROM notification WHERE notification_id > v_since ORDER BY notification_id LIMIT 1;
    ELSIF p_code IN ('Q2', 'Q6') THEN
        SELECT deal_id, client_agent_id INTO v_deal, v_me FROM deal
        WHERE deal_status_id IN (14, 15) AND deal_id > 1000000 ORDER BY deal_id LIMIT 1;
    ELSIF p_code = 'Q3' THEN
        SELECT min(agent_id) INTO v_me FROM agent_service WHERE agent_id > 1000000;
    ELSIF p_code = 'Q7' THEN
        SELECT deal_id, client_agent_id INTO v_deal, v_me FROM deal
        WHERE deal_status_id = 13 AND deal_id > 1000000 ORDER BY deal_id LIMIT 1;
    ELSE
        SELECT client_agent_id, service_id INTO v_me, v_deal FROM deal WHERE deal_id > 1000000 ORDER BY deal_id DESC LIMIT 1;
    END IF;
    BEGIN
        PERFORM set_config('mp.agent_id', coalesce(v_me::text, ''), true);
        FOR r IN EXECUTE 'EXPLAIN (ANALYZE, BUFFERS) ' || mp_load.bench_sql(p_code) USING v_me, v_deal, 1::bigint, v_since LOOP
            v_lines := v_lines || r."QUERY PLAN";
        END LOOP;
        RAISE EXCEPTION USING ERRCODE = 'MPRB0';
    EXCEPTION WHEN SQLSTATE 'MPRB0' THEN NULL;
    END;
    RETURN QUERY SELECT unnest(v_lines);
END $$;
COMMENT ON FUNCTION mp_load.explain(text) IS 'План EXPLAIN (ANALYZE, BUFFERS) горячего запроса Q1-Q9 на стенде (выполнение откатывается)';

CREATE FUNCTION mp_load.http_params()
RETURNS json LANGUAGE sql STABLE SET search_path = mp_service, public AS $$
    -- Что делает: параметры HTTP-замера (server/tests/load_http.py) - агенты и сделки стенда по ролям запросов.
    -- Методика: те же выборки, что в mp_load.bench, но детерминированно (по номерам): получатели свежих
    -- уведомлений и номер «с», клиенты согласованных сделок, поставщики, клиенты недавних сделок,
    -- согласованные сделки и сделки в OFFERS с их клиентами.
    WITH s AS (SELECT max(notification_id) - 50000 AS since FROM notification)
    SELECT json_build_object(
        'since', (SELECT since FROM s),
        'notif_agents', (SELECT json_agg(agent_id) FROM (SELECT DISTINCT agent_id FROM notification, s
                          WHERE notification_id > s.since ORDER BY 1 LIMIT 200) x),
        'providers', (SELECT json_agg(agent_id) FROM (SELECT DISTINCT agent_id FROM agent_service
                       WHERE agent_id > 1000000 ORDER BY 1 LIMIT 200) x),
        'clients', (SELECT json_agg(client_agent_id) FROM (SELECT client_agent_id FROM deal WHERE deal_id > 1000000
                     ORDER BY deal_id DESC LIMIT 200) x),
        'agreed', (SELECT json_agg(json_build_array(deal_id, client_agent_id) ORDER BY deal_id) FROM (
                    SELECT deal_id, client_agent_id FROM deal WHERE deal_status_id IN (14, 15) AND deal_id > 1000000
                    ORDER BY deal_id LIMIT 200) x),
        'offers', (SELECT json_agg(json_build_array(deal_id, client_agent_id) ORDER BY deal_id) FROM (
                    SELECT deal_id, client_agent_id FROM deal WHERE deal_status_id = 13 AND deal_id > 1000000
                    ORDER BY deal_id LIMIT 150) x))
$$;
COMMENT ON FUNCTION mp_load.http_params() IS 'Параметры HTTP-замера load_http.py: агенты и сделки стенда для Q1-Q8 (JSON)';
