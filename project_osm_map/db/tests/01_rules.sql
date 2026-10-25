-- =====================================================================
-- Тесты правил сделки v0.7 (docs/v0.7/01_deal_algorithm.md, разделы 6-7).
-- Каждый тест выполняется и откатывается: данные БД не меняются.
-- Запуск: psql -h localhost -U postgres -d postgres -v ON_ERROR_STOP=1 -f db/tests/01_rules.sql
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

-- Новая парковочная сделка клиента у точки A на окне публикаций; возвращает id (статус MATCHING)
CREATE FUNCTION pg_temp.new_parking_deal(p_client numeric, p_start_after interval DEFAULT interval '3 hours')
RETURNS numeric LANGUAGE plpgsql AS $$
DECLARE
    v_id numeric;
BEGIN
    PERFORM set_config('mp.agent_id', p_client::text, true);
    INSERT INTO mp_service.deal (service_id, client_agent_id, deal_status_id, payment_method_id,
                                 geo_lat, geo_lon, zone_radius_m, period_start, period_end,
                                 price_forecast_min, price_forecast_max)
    VALUES (1001, p_client, mp_service.dict_id('DEAL_STATUS','INIT'), 61, 43.238000, 76.889000, 100,
            date_trunc('minute', now()) + p_start_after, date_trunc('minute', now()) + p_start_after + interval '30 minutes',
            700, 1500)
    RETURNING deal_id INTO v_id;
    UPDATE mp_service.deal SET deal_status_id = mp_service.dict_id('DEAL_STATUS','MATCHING') WHERE deal_id = v_id;
    RETURN v_id;
END
$$;

-- ---------------------------------------------------------------------
-- Проверки целостности (раздел 6 алгоритма)
-- ---------------------------------------------------------------------
SELECT pg_temp.run_test('T01 Переход OFFERS -> CLOSED напрямую', 'не разрешен', $t$
    PERFORM set_config('mp.agent_id', '1', true);
    UPDATE mp_service.deal SET deal_status_id = mp_service.dict_id('DEAL_STATUS','CLOSED') WHERE deal_id = 7;
$t$);

SELECT pg_temp.run_test('T02 Клиент откликается на свою сделку', 'OWN_DEAL', $t$
    INSERT INTO mp_service.deal_offer (deal_id, provider_agent_id, offer_price, offer_mode, geo_lat, geo_lon)
    VALUES (6, 1, 900, 'manual', 43.2381, 76.889);
$t$);

SELECT pg_temp.run_test('T03 Отклик на закрытую сделку', 'DEAL_NOT_OPEN', $t$
    INSERT INTO mp_service.deal_offer (deal_id, provider_agent_id, offer_price, offer_mode, geo_lat, geo_lon)
    VALUES (1, 5, 900, 'manual', 43.2381, 76.889);
$t$);

SELECT pg_temp.run_test('T04 Два выбранных отклика в одной сделке', 'uq_deal_offer_selected', $t$
    UPDATE mp_service.deal_offer SET offer_status = 'selected' WHERE deal_id = 6;
$t$);

SELECT pg_temp.run_test('T05 Отзыв по незакрытой сделке', 'после закрытия или отмены', $t$
    INSERT INTO mp_service.deal_review (deal_id, from_agent_id, to_agent_id, to_role_id, score)
    VALUES (7, 1, 6, 2, 5);
$t$);

SELECT pg_temp.run_test('T06 Отзыв от постороннего агента', 'только стороны сделки', $t$
    INSERT INTO mp_service.deal_review (deal_id, from_agent_id, to_agent_id, to_role_id, score)
    VALUES (1, 5, 2, 2, 5);
$t$);

SELECT pg_temp.run_test('T07 Отклик агента без сервиса и подписки (компания 3)', 'NO_SERVICE_ACCESS', $t$
    INSERT INTO mp_service.deal_offer (deal_id, provider_agent_id, offer_price, offer_mode, geo_lat, geo_lon)
    VALUES (6, 3, 900, 'manual', 43.2381, 76.889);
$t$);

SELECT pg_temp.run_test('T08 Отклик с ценой выше диапазона поставщика', 'PRICE_OUT_OF_RANGE', $t$
    DECLARE v numeric;
    BEGIN
        v := pg_temp.new_parking_deal(1);
        INSERT INTO mp_service.deal_offer (deal_id, provider_agent_id, offer_price, offer_mode, geo_lat, geo_lon)
        VALUES (v, 5, 9999, 'manual', 43.237370, 76.889000);
    END;
$t$);

SELECT pg_temp.run_test('T09 Режим тестирования не администратору', 'только администратору', $t$
    UPDATE mp_service.agent SET is_testing = 1 WHERE agent_id = 5;
$t$);

SELECT pg_temp.run_test('T10 Положение курсором без режима тестирования', 'только в режиме тестирования', $t$
    INSERT INTO mp_service.agent_position (agent_id, geo_lat, geo_lon, source) VALUES (5, 43.24, 76.89, 'manual');
$t$);

SELECT pg_temp.run_test('T11 Статус сделки в поле причины отмены', 'ожидается CANCEL_REASON', $t$
    INSERT INTO mp_service.deal_status_log (deal_id, deal_status_id, agent_id, cancel_reason_id) VALUES (6, 18, 1, 16);
$t$);

SELECT pg_temp.run_test('T12 Неверный пароль входа', 'ok', $t$
    PERFORM pg_temp.check(mp_service.agent_login_check('user_testing_1', 'неверный') IS NULL, 'вход с неверным паролем');
    PERFORM pg_temp.check(mp_service.agent_login_check('нет_такого', 'x') IS NULL, 'вход несуществующего логина');
$t$);

SELECT pg_temp.run_test('T13 У всех таблиц, представлений, их полей и функций есть описание (правило CLAUDE.md)', 'ok', $t$
    PERFORM pg_temp.check(NOT EXISTS (
        SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
        WHERE n.nspname = 'mp_service' AND c.relkind IN ('r','v','m') AND obj_description(c.oid, 'pg_class') IS NULL),
        'объект без описания');
    PERFORM pg_temp.check(NOT EXISTS (
        SELECT 1 FROM pg_attribute a JOIN pg_class c ON c.oid = a.attrelid JOIN pg_namespace n ON n.oid = c.relnamespace
        WHERE n.nspname = 'mp_service' AND c.relkind IN ('r','v','m') AND a.attnum > 0 AND NOT a.attisdropped
          AND col_description(c.oid, a.attnum) IS NULL),
        'поле без описания');
    PERFORM pg_temp.check(NOT EXISTS (
        SELECT 1 FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace
        WHERE n.nspname = 'mp_service'
          AND (obj_description(p.oid, 'pg_proc') IS NULL OR position('-- Что делает:' IN p.prosrc) = 0)),
        'функция без описания или без комментария в теле');
$t$);

-- ---------------------------------------------------------------------
-- Сценарии приемки (раздел 7 алгоритма)
-- ---------------------------------------------------------------------
SELECT pg_temp.run_test('S-1 Кейс A/B/C/D: порядок B, D, C; E не подходит; точки смещены', 'ok', $t$
    PERFORM pg_temp.check(
        (SELECT string_agg(deal_offer_id::text, ',' ORDER BY position) FROM mp_service.v_deal_offer_list WHERE deal_id = 6) = '4,6,5',
        'порядок откликов');
    PERFORM pg_temp.check((SELECT reason FROM mp_service.provider_can_offer(6, 6)) = 'OUT_OF_CLIENT_ZONE', 'E вне зоны');
    PERFORM pg_temp.check(NOT EXISTS (SELECT 1 FROM mp_service.v_deal_offer_list l JOIN mp_service.deal_offer o USING (deal_offer_id)
                                      WHERE l.deal_id = 6 AND (l.show_lat, l.show_lon) = (o.geo_lat, o.geo_lon)),
                          'точки до согласования смещены');
    PERFORM pg_temp.check(NOT EXISTS (SELECT 1 FROM mp_service.v_deal_offer_route WHERE deal_id = 6), 'маршрута до согласования нет');
$t$);

SELECT pg_temp.run_test('S-2 Согласование B: остальные declined, маршрут только к B', 'ok', $t$
    PERFORM set_config('mp.agent_id', '1', true);
    PERFORM mp_service.deal_agree(6, 4);
    PERFORM pg_temp.check((SELECT deal_status_id FROM mp_service.deal WHERE deal_id = 6) = mp_service.dict_id('DEAL_STATUS','AGREED'), 'статус AGREED');
    PERFORM pg_temp.check((SELECT close_secret IS NOT NULL AND date_agreed IS NOT NULL FROM mp_service.deal WHERE deal_id = 6), 'секрет и дата согласования');
    PERFORM pg_temp.check((SELECT string_agg(offer_status, ',' ORDER BY deal_offer_id) FROM mp_service.deal_offer WHERE deal_id = 6) = 'selected,declined,declined', 'статусы откликов');
    PERFORM pg_temp.check((SELECT count(*) FROM mp_service.v_deal_offer_route WHERE deal_id = 6 AND deal_offer_id = 4) = 3, '3 ссылки к B');
    PERFORM pg_temp.check((SELECT show_lat FROM mp_service.v_deal_offer_list WHERE deal_offer_id = 4) = 43.238270, 'точная точка B');
    PERFORM pg_temp.check(EXISTS (SELECT 1 FROM mp_service.deal_status_log WHERE deal_id = 6 AND agent_id = 1
                                  AND deal_status_id = mp_service.dict_id('DEAL_STATUS','AGREED')), 'история пишется триггером');
    PERFORM pg_temp.check(EXISTS (SELECT 1 FROM mp_service.notification WHERE agent_id = 2 AND deal_id = 6
                                  AND notify_type_id = mp_service.dict_id('NOTIFY_TYPE','AGREED')), 'уведомление поставщику');
$t$);

SELECT pg_temp.run_test('S-3 Поставщик занят: второй отклик на то же время снимается', 'ok', $t$
    DECLARE v numeric; o numeric;
    BEGIN
        v := pg_temp.new_parking_deal(7, interval '2 hours 10 minutes');   -- пересекается с периодом сделки 6
        PERFORM mp_service.make_auto_offers(v);
        SELECT deal_offer_id INTO o FROM mp_service.deal_offer WHERE deal_id = v AND provider_agent_id = 2;
        PERFORM pg_temp.check(o IS NOT NULL, 'автоотклик B на второй запрос');
        PERFORM set_config('mp.agent_id', '1', true);
        PERFORM mp_service.deal_agree(6, 4);
        PERFORM pg_temp.check((SELECT offer_status FROM mp_service.deal_offer WHERE deal_offer_id = o) = 'withdrawn', 'отклик снят');
        PERFORM pg_temp.check(EXISTS (SELECT 1 FROM mp_service.notification WHERE agent_id = 7 AND deal_id = v
                                      AND notify_type_id = mp_service.dict_id('NOTIFY_TYPE','OFFER_WITHDRAWN')), 'клиенту уведомление');
    END;
$t$);

SELECT pg_temp.run_test('S-4 Сближение курсорами: CONTACT', 'ok', $t$
    PERFORM set_config('mp.agent_id', '1', true);
    PERFORM mp_service.deal_agree(6, 4);
    INSERT INTO mp_service.agent_position (agent_id, geo_lat, geo_lon, source) VALUES (1, 43.238100, 76.889000, 'manual'),
                                                                                    (2, 43.238270, 76.889000, 'manual');
    PERFORM mp_service.run_deal_jobs();
    PERFORM pg_temp.check((SELECT deal_status_id FROM mp_service.deal WHERE deal_id = 6) = mp_service.dict_id('DEAL_STATUS','CONTACT'), 'статус CONTACT');
    PERFORM pg_temp.check((SELECT count(*) FROM mp_service.notification WHERE deal_id = 6
                           AND notify_type_id = mp_service.dict_id('NOTIFY_TYPE','NEAR')) = 2, 'уведомления NEAR обеим сторонам');
$t$);

SELECT pg_temp.run_test('S-6 Закрытие после CONTACT: CLOSED и баллы обеим сторонам', 'ok', $t$
    PERFORM set_config('mp.agent_id', '1', true);
    PERFORM mp_service.deal_agree(6, 4);
    PERFORM set_config('mp.agent_id', '', true);
    UPDATE mp_service.deal SET deal_status_id = mp_service.dict_id('DEAL_STATUS','CONTACT') WHERE deal_id = 6;
    PERFORM set_config('mp.agent_id', '1', true);
    UPDATE mp_service.deal SET deal_status_id = mp_service.dict_id('DEAL_STATUS','CLOSED') WHERE deal_id = 6;
    PERFORM pg_temp.check((SELECT date_close IS NOT NULL FROM mp_service.deal WHERE deal_id = 6), 'дата закрытия');
    PERFORM pg_temp.check((SELECT count(*) FROM mp_service.notification WHERE deal_id = 6
                           AND notify_type_id = mp_service.dict_id('NOTIFY_TYPE','REVIEW_REQUIRED')) = 2, 'запрос баллов обеим');
$t$);

SELECT pg_temp.run_test('S-6a Закрыть гео-сделку без сближения нельзя', 'после сближения', $t$
    PERFORM set_config('mp.agent_id', '1', true);
    PERFORM mp_service.deal_agree(6, 4);
    UPDATE mp_service.deal SET deal_status_id = mp_service.dict_id('DEAL_STATUS','CLOSED') WHERE deal_id = 6;
$t$);

SELECT pg_temp.run_test('S-6b Закрывает только клиент', 'не может выполнить агент 2', $t$
    PERFORM set_config('mp.agent_id', '1', true);
    PERFORM mp_service.deal_agree(6, 4);
    PERFORM set_config('mp.agent_id', '', true);
    UPDATE mp_service.deal SET deal_status_id = mp_service.dict_id('DEAL_STATUS','CONTACT') WHERE deal_id = 6;
    PERFORM set_config('mp.agent_id', '2', true);
    UPDATE mp_service.deal SET deal_status_id = mp_service.dict_id('DEAL_STATUS','CLOSED') WHERE deal_id = 6;
$t$);

SELECT pg_temp.run_test('S-8 Без баллов нельзя откликаться', 'ok', $t$
    PERFORM set_config('mp.agent_id', '1', true);
    PERFORM mp_service.deal_agree(6, 4);
    PERFORM set_config('mp.agent_id', '', true);
    UPDATE mp_service.deal SET deal_status_id = mp_service.dict_id('DEAL_STATUS','CONTACT') WHERE deal_id = 6;
    PERFORM set_config('mp.agent_id', '1', true);
    UPDATE mp_service.deal SET deal_status_id = mp_service.dict_id('DEAL_STATUS','CLOSED') WHERE deal_id = 6;
    PERFORM pg_temp.check(mp_service.agent_pending_reviews(2) = 1, 'у поставщика 1 сделка без баллов');
    DECLARE v numeric; BEGIN v := pg_temp.new_parking_deal(7); PERFORM pg_temp.check((SELECT reason FROM mp_service.provider_can_offer(v, 2)) = 'REVIEW_REQUIRED', 'отклик заблокирован'); END;
$t$);

SELECT pg_temp.run_test('S-9a Отмена поставщиком после окна без причины', 'нужна причина', $t$
    PERFORM set_config('mp.agent_id', '1', true);
    PERFORM mp_service.deal_agree(6, 4);
    UPDATE mp_service.deal SET date_agreed = now() - interval '10 minutes' WHERE deal_id = 6;
    PERFORM set_config('mp.agent_id', '2', true);
    PERFORM mp_service.deal_cancel(6);
$t$);

SELECT pg_temp.run_test('S-9 Отмена поставщиком после окна: CANCEL_PROVIDER, клиенту баллы', 'ok', $t$
    DECLARE s text;
    BEGIN
        PERFORM set_config('mp.agent_id', '1', true);
        PERFORM mp_service.deal_agree(6, 4);
        UPDATE mp_service.deal SET date_agreed = now() - interval '10 minutes' WHERE deal_id = 6;
        PERFORM set_config('mp.agent_id', '2', true);
        s := mp_service.deal_cancel(6, mp_service.dict_id('CANCEL_REASON','LATE'));
        PERFORM pg_temp.check(s = 'CANCEL_PROVIDER', 'статус ' || s);
        PERFORM pg_temp.check(mp_service.agent_pending_reviews(1) = 1, 'клиенту нужно поставить баллы');
        PERFORM pg_temp.check(mp_service.agent_pending_reviews(2) = 0, 'отменившему - нет');
    END;
$t$);

SELECT pg_temp.run_test('S-10 Отмена клиентом в окне 5 минут: CANCEL_FREE', 'ok', $t$
    DECLARE s text;
    BEGIN
        PERFORM set_config('mp.agent_id', '1', true);
        PERFORM mp_service.deal_agree(6, 4);
        s := mp_service.deal_cancel(6);
        PERFORM pg_temp.check(s = 'CANCEL_FREE', 'статус ' || s);
    END;
$t$);

SELECT pg_temp.run_test('S-11 Нет согласования к началу периода: EXPIRED', 'ok', $t$
    UPDATE mp_service.deal SET period_start = now() - interval '1 minute' WHERE deal_id = 6;
    PERFORM mp_service.run_deal_jobs();
    PERFORM pg_temp.check((SELECT deal_status_id FROM mp_service.deal WHERE deal_id = 6) = mp_service.dict_id('DEAL_STATUS','EXPIRED'), 'статус EXPIRED');
    PERFORM pg_temp.check(NOT EXISTS (SELECT 1 FROM mp_service.deal_offer WHERE deal_id = 6 AND offer_status = 'active'), 'отклики истекли');
$t$);

SELECT pg_temp.run_test('S-14 Субагент без назначенного сервиса', 'ok', $t$
    UPDATE mp_service.agent_service SET assigned_by_agent_id = NULL WHERE agent_id = 4 AND service_id = 100;
    DECLARE v numeric; BEGIN v := pg_temp.new_parking_deal(7); PERFORM pg_temp.check((SELECT reason FROM mp_service.provider_can_offer(v, 4)) = 'NO_SERVICE_ACCESS', 'нет доступа'); END;
$t$);

SELECT pg_temp.run_test('S-15 Заблокированный агент', 'ok', $t$
    INSERT INTO mp_service.agent_sanction (agent_id, reason_id, action, block_level, date_start, date_end)
    VALUES (5, 81, 'block', 1, now(), now() + interval '7 days');
    DECLARE v numeric; BEGIN v := pg_temp.new_parking_deal(7); PERFORM pg_temp.check((SELECT reason FROM mp_service.provider_can_offer(v, 5)) = 'BLOCKED', 'отклик запрещен'); END;
$t$);

SELECT pg_temp.run_test('S-16 Третий эпизод при отзывах и падении выручки: блокировка, компании уведомление', 'ok', $t$
    DECLARE a text;
    BEGIN
        -- у агента 4 уже 3 санкции; новая блокировка - уровень 2 (месяц)
        a := mp_service.register_fraud_episode(4, 'FAKE_REVIEWS', NULL, true, true);
        PERFORM pg_temp.check(a = 'block', 'действие ' || a);
        PERFORM pg_temp.check((SELECT block_level FROM mp_service.agent_sanction WHERE agent_id = 4 ORDER BY sanction_id DESC LIMIT 1) = 2, 'уровень 2');
        PERFORM pg_temp.check(EXISTS (SELECT 1 FROM mp_service.notification WHERE agent_id = 3
                                      AND notify_type_id = mp_service.dict_id('NOTIFY_TYPE','SANCTION')), 'уведомление компании');
        a := mp_service.register_fraud_episode(4, 'BYPASS_GEO');
        PERFORM pg_temp.check(a = 'rating_down', 'без признаков отзывов и выручки - понижение');
    END;
$t$);

SELECT pg_temp.run_test('S-17 Время через полночь (П-6)', 'ok', $t$
    PERFORM pg_temp.check(mp_service.period_in_work_time('22:00', '07:00', '2026-10-25 23:00', '2026-10-26 06:00'), 'ночь внутри окна');
    PERFORM pg_temp.check(NOT mp_service.period_in_work_time('09:00', '21:00', '2026-10-25 21:00', '2026-10-25 22:00'), 'вне окна');
    PERFORM pg_temp.check(mp_service.period_in_work_time('22:00', '07:00', '2026-10-26 01:00', '2026-10-26 02:00'), 'после полуночи');
$t$);

SELECT pg_temp.run_test('S-18 Субагент получает сервис по подписке компании', 'ok', $t$
    PERFORM pg_temp.check(EXISTS (SELECT 1 FROM mp_service.v_agent_available_service WHERE agent_id = 4 AND service_id = 1001), 'доступ к парковке');
    PERFORM pg_temp.check(NOT EXISTS (SELECT 1 FROM mp_service.v_agent_available_service WHERE agent_id = 4 AND service_id = 1101), 'такси не назначено');
$t$);

-- ---------------------------------------------------------------------
SELECT n, CASE WHEN passed THEN 'OK' ELSE 'FAIL' END AS status, test, CASE WHEN passed THEN NULL ELSE result END AS details
FROM t_result ORDER BY n;

SELECT count(*) FILTER (WHERE passed) AS passed, count(*) FILTER (WHERE NOT passed) AS failed FROM t_result;
