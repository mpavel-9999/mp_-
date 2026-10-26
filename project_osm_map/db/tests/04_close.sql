-- =====================================================================
-- Тесты сближения и закрытия: deal_contact_check, HINT CONTACT_REQUIRED (план Э6 шаг 1).
-- Каждый тест выполняется и откатывается: данные БД не меняются.
-- Запуск: psql -h localhost -U postgres -d postgres -v ON_ERROR_STOP=1 -f db/tests/04_close.sql
-- Ожидание: expected = ok (тело прошло все проверки) или фрагмент текста ошибки, которую должна дать БД.
-- =====================================================================
SET search_path TO mp_service, public;
SET TimeZone = 'Asia/Almaty';

CREATE TEMP TABLE t_result (n serial, test text, expected text, result text, passed boolean);

CREATE FUNCTION pg_temp.run_test(p_name text, p_expected text, p_body text)
RETURNS void LANGUAGE plpgsql AS $$
DECLARE
    v_res text;
BEGIN
    BEGIN
        EXECUTE 'DO $body$ BEGIN ' || p_body || ' END $body$';
        RAISE EXCEPTION 'rollback_marker';
    EXCEPTION WHEN OTHERS THEN
        v_res := CASE WHEN SQLERRM = 'rollback_marker' THEN 'ok' ELSE 'error: ' || SQLERRM END;
    END;
    INSERT INTO t_result (test, expected, result, passed)
    VALUES (p_name, p_expected, v_res,
            CASE WHEN p_expected = 'ok' THEN v_res = 'ok'
                 ELSE v_res LIKE 'error: %' || p_expected || '%' END);
END
$$;

-- Утверждение внутри теста
CREATE FUNCTION pg_temp.check(p_cond boolean, p_msg text)
RETURNS void LANGUAGE plpgsql AS $$
BEGIN
    IF p_cond IS NOT TRUE THEN RAISE EXCEPTION 'assert: %', p_msg; END IF;
END
$$;

-- Согласованная сделка клиента 1 с поставщиком 2 (загрузкой, без проверок процесса); возвращает номер
CREATE FUNCTION pg_temp.agreed_deal(p_service numeric DEFAULT 1001)
RETURNS numeric LANGUAGE plpgsql AS $$
DECLARE
    v_id numeric;
BEGIN
    PERFORM set_config('mp.load_mode', 'on', true);
    INSERT INTO mp_service.deal (deal_id, service_id, client_agent_id, provider_agent_id, deal_status_id, payment_method_id,
                                 geo_lat, geo_lon, zone_radius_m, period_start, period_end, date_agreed)
    VALUES ((SELECT max(deal_id) + 1 FROM mp_service.deal), p_service, 1, 2,
            mp_service.dict_id('DEAL_STATUS','AGREED'), 61, 43.238000, 76.889000, 100,
            date_trunc('minute', now()::timestamp) + interval '10 minutes',
            date_trunc('minute', now()::timestamp) + interval '40 minutes', now())
    RETURNING deal_id INTO v_id;
    PERFORM set_config('mp.load_mode', 'off', true);
    RETURN v_id;
END
$$;

-- Положение агента по GPS: сдвиг на север от точки сделки в метрах
CREATE FUNCTION pg_temp.put_position(p_agent numeric, p_north_m numeric)
RETURNS void LANGUAGE sql AS $$
    INSERT INTO mp_service.agent_position (agent_id, geo_lat, geo_lon, source)
    VALUES (p_agent, round(43.238000 + p_north_m / 111195.0, 6), 76.889000, 'gps')
    ON CONFLICT (agent_id) DO UPDATE SET geo_lat = excluded.geo_lat, geo_lon = excluded.geo_lon, source = excluded.source
$$;

CREATE FUNCTION pg_temp.status(p_deal numeric)
RETURNS text LANGUAGE sql AS $$
    SELECT s.dict_code FROM mp_service.deal d JOIN mp_service.dictionary s ON s.dict_id = d.deal_status_id
    WHERE d.deal_id = p_deal
$$;

-- Попытка клиента закрыть сделку; возвращает код из HINT, текст ошибки или 'ok'
CREATE FUNCTION pg_temp.try_close(p_deal numeric)
RETURNS text LANGUAGE plpgsql AS $$
DECLARE
    v_hint text;
BEGIN
    PERFORM set_config('mp.agent_id', '1', true);
    UPDATE mp_service.deal SET deal_status_id = mp_service.dict_id('DEAL_STATUS','CLOSED') WHERE deal_id = p_deal;
    RETURN 'ok';
EXCEPTION WHEN OTHERS THEN
    GET STACKED DIAGNOSTICS v_hint = PG_EXCEPTION_HINT;
    RETURN coalesce(nullif(v_hint, ''), SQLERRM);
END
$$;

SELECT pg_temp.run_test('C-1 Стороны ближе near_radius_m (300 м) - CONTACT, автор - система, агент сессии восстановлен', 'ok', $t$
    DECLARE v_deal numeric := pg_temp.agreed_deal(); v_ids numeric[];
    BEGIN
        PERFORM pg_temp.put_position(1, 0);
        PERFORM pg_temp.put_position(2, 120);
        PERFORM set_config('mp.agent_id', '1', true);
        v_ids := mp_service.deal_contact_check(1);
        PERFORM pg_temp.check(v_ids = ARRAY[v_deal], 'возвращена сделка: ' || v_ids::text);
        PERFORM pg_temp.check(pg_temp.status(v_deal) = 'CONTACT', 'статус ' || pg_temp.status(v_deal));
        PERFORM pg_temp.check(current_setting('mp.agent_id', true) = '1', 'агент сессии не восстановлен');
        PERFORM pg_temp.check(EXISTS (SELECT 1 FROM mp_service.notification n WHERE n.deal_id = v_deal
                                      AND n.notify_type_id = mp_service.dict_id('NOTIFY_TYPE','NEAR')), 'нет уведомления NEAR');
    END;
$t$);

SELECT pg_temp.run_test('C-2 Стороны дальше near_radius_m - сделка остаётся AGREED', 'ok', $t$
    DECLARE v_deal numeric := pg_temp.agreed_deal();
    BEGIN
        PERFORM pg_temp.put_position(1, 0);
        PERFORM pg_temp.put_position(2, 450);
        PERFORM pg_temp.check(cardinality(mp_service.deal_contact_check(1)) = 0, 'переведена дальняя сделка');
        PERFORM pg_temp.check(pg_temp.status(v_deal) = 'AGREED', 'статус ' || pg_temp.status(v_deal));
    END;
$t$);

SELECT pg_temp.run_test('C-3 Положение старше position_ttl_s (120 с) не учитывается', 'ok', $t$
    DECLARE v_deal numeric := pg_temp.agreed_deal();
    BEGIN
        PERFORM pg_temp.put_position(1, 0);
        PERFORM pg_temp.put_position(2, 50);
        ALTER TABLE mp_service.agent_position DISABLE TRIGGER trg_agent_position;
        UPDATE mp_service.agent_position SET date_update = now() - interval '5 minutes' WHERE agent_id = 2;
        ALTER TABLE mp_service.agent_position ENABLE TRIGGER trg_agent_position;
        PERFORM pg_temp.check(cardinality(mp_service.deal_contact_check(1)) = 0, 'учтено устаревшее положение');
        PERFORM pg_temp.check(pg_temp.status(v_deal) = 'AGREED', 'статус ' || pg_temp.status(v_deal));
    END;
$t$);

SELECT pg_temp.run_test('C-4 Вызов по другому агенту не трогает чужие сделки; без агента - все', 'ok', $t$
    DECLARE v_deal numeric := pg_temp.agreed_deal();
    BEGIN
        PERFORM pg_temp.put_position(1, 0);
        PERFORM pg_temp.put_position(2, 100);
        PERFORM pg_temp.check(NOT (v_deal = ANY (mp_service.deal_contact_check(5))), 'переведена чужая сделка');
        PERFORM pg_temp.check(pg_temp.status(v_deal) = 'AGREED', 'статус после чужого вызова ' || pg_temp.status(v_deal));
        PERFORM pg_temp.check(v_deal = ANY (mp_service.deal_contact_check(NULL)), 'общий вызов не перевёл сделку');
    END;
$t$);

SELECT pg_temp.run_test('C-5 Онлайн-услуга (1801) в CONTACT не переводится', 'ok', $t$
    DECLARE v_deal numeric := pg_temp.agreed_deal(1801);
    BEGIN
        PERFORM pg_temp.put_position(1, 0);
        PERFORM pg_temp.put_position(2, 10);
        PERFORM pg_temp.check(NOT (v_deal = ANY (mp_service.deal_contact_check(1))), 'онлайн-сделка в CONTACT');
        PERFORM pg_temp.check(pg_temp.status(v_deal) = 'AGREED', 'статус ' || pg_temp.status(v_deal));
    END;
$t$);

SELECT pg_temp.run_test('C-6 Закрытие гео-сделки из AGREED - HINT CONTACT_REQUIRED; из CONTACT - CLOSED', 'ok', $t$
    DECLARE v_deal numeric := pg_temp.agreed_deal();
    BEGIN
        PERFORM pg_temp.check(pg_temp.try_close(v_deal) = 'CONTACT_REQUIRED', 'закрытие из AGREED: ' || pg_temp.try_close(v_deal));
        PERFORM pg_temp.put_position(1, 0);
        PERFORM pg_temp.put_position(2, 100);
        PERFORM mp_service.deal_contact_check(1);
        PERFORM pg_temp.check(pg_temp.try_close(v_deal) = 'ok', 'закрытие из CONTACT');
        PERFORM pg_temp.check(pg_temp.status(v_deal) = 'CLOSED', 'статус ' || pg_temp.status(v_deal));
    END;
$t$);

SELECT pg_temp.run_test('C-7 Онлайн-услуга закрывается из AGREED', 'ok', $t$
    DECLARE v_deal numeric := pg_temp.agreed_deal(1801);
    BEGIN
        PERFORM pg_temp.check(pg_temp.try_close(v_deal) = 'ok', 'закрытие онлайн-сделки: ' || pg_temp.try_close(v_deal));
        PERFORM pg_temp.check(pg_temp.status(v_deal) = 'CLOSED', 'статус ' || pg_temp.status(v_deal));
    END;
$t$);

SELECT pg_temp.run_test('C-8 run_deal_jobs считает сближение через deal_contact_check', 'ok', $t$
    DECLARE v_deal numeric := pg_temp.agreed_deal(); v_res jsonb;
    BEGIN
        PERFORM pg_temp.put_position(1, 0);
        PERFORM pg_temp.put_position(2, 200);
        v_res := mp_service.run_deal_jobs();
        PERFORM pg_temp.check((v_res->>'contact')::int >= 1, 'contact = ' || (v_res->>'contact'));
        PERFORM pg_temp.check(pg_temp.status(v_deal) = 'CONTACT', 'статус ' || pg_temp.status(v_deal));
    END;
$t$);

-- ---------------------------------------------------------------------
SELECT n, CASE WHEN passed THEN 'OK' ELSE 'FAIL' END AS status, test, CASE WHEN passed THEN NULL ELSE result END AS details
FROM t_result ORDER BY n;

SELECT count(*) FILTER (WHERE passed) AS passed, count(*) FILTER (WHERE NOT passed) AS failed FROM t_result;
