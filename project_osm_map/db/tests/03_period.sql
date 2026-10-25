-- =====================================================================
-- Тесты проверки периода trg_period_check (Р-4, план Э5 шаг 1).
-- Каждый тест выполняется и откатывается: данные БД не меняются.
-- Запуск: psql -h localhost -U postgres -d postgres -v ON_ERROR_STOP=1 -f db/tests/03_period.sql
-- Ожидание: expected = ok (тело прошло все проверки) или фрагмент текста ошибки, которую должна дать БД.
-- =====================================================================
SET search_path TO mp_service, public;
-- как сессия API: время строк API - алматинское
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

-- Запрос на парковку от клиента (агент 1) с началом через p_start_after; возвращает код из HINT или 'ok'
CREATE FUNCTION pg_temp.try_deal(p_agent text, p_start_after interval, p_length interval DEFAULT interval '30 minutes')
RETURNS text LANGUAGE plpgsql AS $$
DECLARE
    v_hint text;
BEGIN
    PERFORM set_config('mp.agent_id', p_agent, true);
    INSERT INTO mp_service.deal (service_id, client_agent_id, deal_status_id, payment_method_id,
                                 geo_lat, geo_lon, zone_radius_m, period_start, period_end)
    VALUES (1001, 1, mp_service.dict_id('DEAL_STATUS','INIT'), 61, 43.238000, 76.889000, 100,
            date_trunc('minute', now()::timestamp) + p_start_after,
            date_trunc('minute', now()::timestamp) + p_start_after + p_length);
    RETURN 'ok';
EXCEPTION WHEN OTHERS THEN
    GET STACKED DIAGNOSTICS v_hint = PG_EXCEPTION_HINT;
    RETURN coalesce(nullif(v_hint, ''), SQLERRM);
END
$$;

-- Публикация места парковки от поставщика (агент 2) с началом через p_start_after
CREATE FUNCTION pg_temp.try_slot(p_agent text, p_start_after interval)
RETURNS text LANGUAGE plpgsql AS $$
DECLARE
    v_hint text;
BEGIN
    PERFORM set_config('mp.agent_id', p_agent, true);
    INSERT INTO mp_service.provider_slot (agent_id, service_id, geo_lat, geo_lon, period_start, period_end, price_min)
    VALUES (2, 1001, 43.238100, 76.889100,
            date_trunc('minute', now()::timestamp) + p_start_after,
            date_trunc('minute', now()::timestamp) + p_start_after + interval '30 minutes', 500);
    RETURN 'ok';
EXCEPTION WHEN OTHERS THEN
    GET STACKED DIAGNOSTICS v_hint = PG_EXCEPTION_HINT;
    RETURN coalesce(nullif(v_hint, ''), SQLERRM);
END
$$;

SELECT pg_temp.run_test('P-1 Запрос агента с началом 10 мин назад - PERIOD_IN_PAST', 'ok', $t$
    PERFORM pg_temp.check(pg_temp.try_deal('1', interval '-10 minutes') = 'PERIOD_IN_PAST', 'начало в прошлом');
$t$);

SELECT pg_temp.run_test('P-2 Запрос агента: конец не позже начала - PERIOD_INVALID', 'ok', $t$
    PERFORM pg_temp.check(pg_temp.try_deal('1', interval '1 hour', interval '0 minutes') = 'PERIOD_INVALID', 'конец = начало');
$t$);

SELECT pg_temp.run_test('P-3 Запрос агента: начало сейчас и через 30 мин - принимается', 'ok', $t$
    PERFORM pg_temp.check(pg_temp.try_deal('1', interval '0 minutes') = 'ok', 'начало сейчас (запас 1 мин)');
    PERFORM pg_temp.check(pg_temp.try_deal('1', interval '30 minutes') = 'ok', 'начало через 30 мин');
$t$);

SELECT pg_temp.run_test('P-4 Публикация агента: окно закончилось - PERIOD_IN_PAST; идёт сейчас или впереди - принимается', 'ok', $t$
    PERFORM pg_temp.check(pg_temp.try_slot('2', interval '-2 hours') = 'PERIOD_IN_PAST', 'окно закончилось');
    PERFORM pg_temp.check(pg_temp.try_slot('2', interval '-10 minutes') = 'ok', 'окно уже идёт (место свободно сейчас)');
    PERFORM pg_temp.check(pg_temp.try_slot('2', interval '15 minutes') = 'ok', 'окно впереди');
$t$);

SELECT pg_temp.run_test('P-5 Вставка без агента (система, данные) не проверяется', 'ok', $t$
    PERFORM pg_temp.check(pg_temp.try_deal('', interval '-1 day') = 'ok', 'запрос в прошлом от системы');
    PERFORM pg_temp.check(pg_temp.try_slot('', interval '-1 day') = 'ok', 'публикация в прошлом от системы');
$t$);

-- ---------------------------------------------------------------------
SELECT n, CASE WHEN passed THEN 'OK' ELSE 'FAIL' END AS status, test, CASE WHEN passed THEN NULL ELSE result END AS details
FROM t_result ORDER BY n;

SELECT count(*) FILTER (WHERE passed) AS passed, count(*) FILTER (WHERE NOT passed) AS failed FROM t_result;
