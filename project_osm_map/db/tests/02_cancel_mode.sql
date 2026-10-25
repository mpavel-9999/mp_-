-- =====================================================================
-- Тесты режима отмены deal_cancel_mode (алгоритм п. 5.2-5.4, план Э4 шаг 1).
-- Каждый тест выполняется и откатывается: данные БД не меняются.
-- Запуск: psql -h localhost -U postgres -d postgres -v ON_ERROR_STOP=1 -f db/tests/02_cancel_mode.sql
-- Ожидание: expected = ok (тело прошло все проверки) или фрагмент текста ошибки, которую должна дать БД.
-- =====================================================================
SET search_path TO mp_service, public;

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

-- Сделка 6 (кейс A/B/C/D): клиент - агент 1, отклик 4 - поставщик агент 2; период начинается через 2 ч после загрузки,
-- поэтому "опоздание другой стороны" в тестах не срабатывает.

SELECT pg_temp.run_test('C-1 До согласования: бесплатно клиенту, постороннему - нельзя', 'ok', $t$
    PERFORM pg_temp.check(mp_service.deal_cancel_mode(6, 1) = 'CANCEL_FREE', 'клиент до согласования');
    PERFORM pg_temp.check(mp_service.deal_cancel_mode(6, 9) IS NULL, 'посторонний агент');
    PERFORM pg_temp.check(mp_service.deal_cancel_mode(6, NULL) IS NULL, 'без агента');
    PERFORM pg_temp.check(mp_service.deal_cancel_mode(-1, 1) IS NULL, 'нет сделки');
$t$);

SELECT pg_temp.run_test('C-2 AGREED в окне cancel_free_min: бесплатно обеим сторонам', 'ok', $t$
    PERFORM set_config('mp.agent_id', '1', true);
    PERFORM mp_service.deal_agree(6, 4);
    PERFORM pg_temp.check(mp_service.deal_cancel_mode(6, 1) = 'CANCEL_FREE', 'клиент в окне');
    PERFORM pg_temp.check(mp_service.deal_cancel_mode(6, 2) = 'CANCEL_FREE', 'поставщик в окне');
$t$);

SELECT pg_temp.run_test('C-3 AGREED после окна: штраф стороне-инициатору', 'ok', $t$
    PERFORM set_config('mp.agent_id', '1', true);
    PERFORM mp_service.deal_agree(6, 4);
    UPDATE mp_service.deal SET date_agreed = now() - interval '10 minutes' WHERE deal_id = 6;
    PERFORM pg_temp.check(mp_service.deal_cancel_mode(6, 1) = 'CANCEL_CLIENT', 'клиент после окна');
    PERFORM pg_temp.check(mp_service.deal_cancel_mode(6, 2) = 'CANCEL_PROVIDER', 'поставщик после окна');
$t$);

SELECT pg_temp.run_test('C-4 deal_cancel даёт тот же итог, что deal_cancel_mode', 'ok', $t$
    DECLARE m text; s text;
    BEGIN
        PERFORM set_config('mp.agent_id', '1', true);
        PERFORM mp_service.deal_agree(6, 4);
        UPDATE mp_service.deal SET date_agreed = now() - interval '10 minutes' WHERE deal_id = 6;
        m := mp_service.deal_cancel_mode(6, 1);
        s := mp_service.deal_cancel(6, mp_service.dict_id('CANCEL_REASON','LATE'));
        PERFORM pg_temp.check(s = m, 'отмена ' || s || ', режим ' || m);
        PERFORM pg_temp.check(mp_service.deal_cancel_mode(6, 1) IS NULL, 'после отмены - нельзя');
    END;
$t$);

SELECT pg_temp.run_test('C-5 Закрытая сделка: режим NULL, отмена отклоняется', 'отменить нельзя', $t$
    DECLARE c numeric;
    BEGIN
        SELECT client_agent_id INTO c FROM mp_service.deal d JOIN mp_service.dictionary s ON s.dict_id = d.deal_status_id
        WHERE s.dict_code = 'CLOSED' ORDER BY d.deal_id LIMIT 1;
        PERFORM pg_temp.check(mp_service.deal_cancel_mode((SELECT d.deal_id FROM mp_service.deal d
            JOIN mp_service.dictionary s ON s.dict_id = d.deal_status_id WHERE s.dict_code = 'CLOSED' ORDER BY d.deal_id LIMIT 1), c) IS NULL,
            'закрытая сделка');
        PERFORM set_config('mp.agent_id', c::text, true);
        PERFORM mp_service.deal_cancel((SELECT d.deal_id FROM mp_service.deal d
            JOIN mp_service.dictionary s ON s.dict_id = d.deal_status_id WHERE s.dict_code = 'CLOSED' ORDER BY d.deal_id LIMIT 1));
    END;
$t$);

-- ---------------------------------------------------------------------
SELECT n, CASE WHEN passed THEN 'OK' ELSE 'FAIL' END AS status, test, CASE WHEN passed THEN NULL ELSE result END AS details
FROM t_result ORDER BY n;

SELECT count(*) FILTER (WHERE passed) AS passed, count(*) FILTER (WHERE NOT passed) AS failed FROM t_result;
