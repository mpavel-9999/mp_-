"""
Сквозная проверка API v0.7 по HTTP (план Э2, шаг 11). Только стандартная библиотека Python 3.

Запуск (сервер уже работает на 127.0.0.1:8080, БД загружена из db/mp_service.sql):
    MP_TEST_PASSWORD=<общий пароль тестовых пользователей> python server/tests/api_smoke.py

Пароль в репозитории не хранится - только в переменной окружения.
Сценарии меняют данные БД (создают сделки тестовых пользователей); перед приемкой БД можно перезагрузить.
Ожидание сближения сторон - до 70 с: CONTACT ставит плановое задание сервера раз в минуту.
"""
import json
import os
import sys
import time
import urllib.error
import urllib.request
from datetime import datetime, timedelta, timezone

BASE = os.environ.get('MP_API', 'http://127.0.0.1:8080/api')
PASSWORD = os.environ.get('MP_TEST_PASSWORD')
results = []


def call(method, path, token=None, body=None, lang=None, expect=200):
    req = urllib.request.Request(BASE + path, method=method,
                                 data=None if body is None else json.dumps(body).encode('utf-8'))
    req.add_header('Content-Type', 'application/json')
    if token:
        req.add_header('Authorization', 'Bearer ' + token)
    if lang:
        req.add_header('Accept-Language', lang)
    try:
        with urllib.request.urlopen(req) as r:
            status, text = r.status, r.read().decode('utf-8')
    except urllib.error.HTTPError as e:
        status, text = e.code, e.read().decode('utf-8')
    data = json.loads(text) if text else None
    if status != expect:
        raise AssertionError(f'{method} {path}: ожидался {expect}, получен {status}: {text[:300]}')
    return data


def step(name, fn):
    try:
        info = fn()
        results.append((True, name, info or ''))
    except Exception as e:  # noqa: BLE001 - отчет по каждому шагу
        results.append((False, name, str(e)))


ALMATY = timezone(timedelta(hours=5))  # время API - по Алматы (Р-4), независимо от пояса компьютера


def ts(minutes):
    return (datetime.now(ALMATY) + timedelta(minutes=minutes)).strftime('%Y-%m-%dT%H:%M:00')


def wait_status(token, deal_id, status, seconds=75):
    end = time.time() + seconds
    while time.time() < end:
        d = call('GET', f'/deals/{deal_id}', token)
        if d['status'] == status:
            return d
        time.sleep(3)
    raise AssertionError(f'сделка {deal_id} не перешла в {status} за {seconds} с')


def check(cond, msg):
    if not cond:
        raise AssertionError(msg)


def main():
    if not PASSWORD:
        sys.exit('Задайте MP_TEST_PASSWORD - общий пароль тестовых пользователей')
    s = {}

    # --- вход и конфигурация
    step('health и маршруты готовы', lambda: check(call('GET', '/health')['routing'] == 'готов', 'маршруты не готовы'))
    step('неверный пароль -> 401', lambda: call('POST', '/session', body={'login': 'user_testing_1', 'password': 'x'}, expect=401))

    def login():
        for n in (1, 2, 3):
            s[n] = call('POST', '/session', body={'login': f'user_testing_{n}', 'password': PASSWORD})['token']
        return 'клиент user_testing_1, поставщик user_testing_2, третий user_testing_3'
    step('вход тестовыми пользователями', login)
    if 1 not in s:
        return

    def config():
        c = call('GET', '/config', s[1])
        check(c['template']['code'] and c['scheme']['colors']['accent'], 'нет шаблона или палитры')
        check(c['texts']['ui.find_providers'] == 'Найти поставщиков', 'нет текстов интерфейса')
        check('CLIENT_TEST' in c['cursors'], 'нет курсоров')
        call('GET', f"/config?checksum={c['checksum']}", s[1], expect=304)
        c2 = call('PUT', '/config', s[1], {'template': 'B', 'scheme': 'B_DARK', 'lang': 'ENG'})
        check(c2['texts']['ui.find_providers'] == 'Find providers' and c2['scheme']['dark'], 'смена шаблона/языка')
        call('PUT', '/config', s[1], {'template': 'A', 'scheme': 'C_DARK'}, expect=400)
        call('PUT', '/config', s[1], {'template': 'A', 'scheme': 'A_LIGHT', 'lang': 'RUS'})
        opts = call('GET', '/ui/options', s[1])
        check(len(opts['options']) == 6, 'вариантов интерфейса не 6')
        return f"сумма {c['checksum'][:8]}…, 304 при совпадении, 6 вариантов"
    step('конфигурация интерфейса: 304, смена шаблона, схемы и языка', config)

    step('поиск услуг "стрижк"', lambda: check(len(call('GET', '/services?q=%D1%81%D1%82%D1%80%D0%B8%D0%B6%D0%BA', s[1])) >= 3, 'мало результатов'))

    # --- парковка: публикация -> запрос -> отклик -> согласование -> сближение -> код -> баллы
    P = (43.250000, 76.920000)

    def publish():
        slot = call('POST', '/slots', s[2], {'service_id': 100, 'lat': P[0] + 0.0004, 'lon': P[1],
                                              'period_start': ts(-10), 'period_end': ts(300), 'price_min': 600})
        s['slot'] = slot['slot_id']
        on_map = call('GET', f'/map/slots?lat={P[0]}&lon={P[1]}&radius=500&service_id=1001', s[1])
        check(any(x['slot_id'] == s['slot'] for x in on_map), 'публикация не видна на карте')
        mine = [x for x in on_map if x['slot_id'] == s['slot']][0]
        check(mine['is_testing'] and mine['pseudonym'] == 'Тестер_2', 'нет признака теста/псевдонима')
        return f"публикация {s['slot']}, точка на карте смещена: {mine['lat']}, {mine['lon']}"
    step('поставщик публикует место, клиент видит его на карте', publish)

    def create():
        f = call('POST', '/forecast', s[1], {'service_id': 1001, 'lat': P[0], 'lon': P[1]})
        d = call('POST', '/deals', s[1], {'service_id': 1001, 'slot_id': s.get('slot'), 'lat': P[0], 'lon': P[1],
                                          'zone_radius_m': 150, 'period_start': ts(60), 'period_end': ts(120),
                                          'client_price': 900})
        s['deal'] = d['deal_id']
        check(d['status'] == 'MATCHING' and 'cancel' in d['actions'], f"статус {d['status']}")
        return f"прогноз {f['price_min']}–{f['price_max']}, сделка {s['deal']}"
    step('клиент: прогноз и запрос из публикации', create)

    def period_in_past():
        r = call('POST', '/deals', s[1], {'service_id': 1001, 'lat': P[0], 'lon': P[1], 'zone_radius_m': 150,
                                          'period_start': ts(-10), 'period_end': ts(20), 'client_price': 900}, expect=422)
        check(r['code'] == 'PERIOD_IN_PAST', r['code'])
        r = call('POST', '/slots', s[2], {'service_id': 1001, 'lat': P[0], 'lon': P[1],
                                          'period_start': ts(-120), 'period_end': ts(-60), 'price_min': 600}, expect=422)
        check(r['code'] == 'PERIOD_IN_PAST', r['code'])
        return 'запрос с началом в прошлом и закончившееся окно отклонены'
    step('время по Алматы: прошлое -> 422 PERIOD_IN_PAST (Р-4)', period_in_past)

    def provider_sees():
        reqs = call('GET', '/requests', s[2])
        mine = [x for x in reqs if x['deal_id'] == s['deal']]
        check(mine, 'запрос не пришел поставщику')
        notes = call('GET', '/notifications', s[2])
        check(any(n['type'] == 'NEW_REQUEST' and n['deal_id'] == s['deal'] for n in notes), 'нет уведомления NEW_REQUEST')
        others = call('GET', '/requests', s[3])
        check(not any(x['deal_id'] == s['deal'] for x in others), 'запрос из публикации пришел чужому поставщику')
        return f"рекомендованная цена {mine[0]['recommended_price']}"
    step('поставщик получает запрос и уведомление; чужой поставщик - нет', provider_sees)

    step('отклик с ценой выше диапазона -> 409', lambda: call('POST', f"/deals/{s['deal']}/offers", s[2], {'price': 99999}, expect=409))

    def offer():
        call('POST', f"/deals/{s['deal']}/offers", s[2], {'price': 900})
        offers = call('GET', f"/deals/{s['deal']}/offers", s[1])
        check(len(offers) == 1 and offers[0]['distance_band'] and offers[0]['distance_m'] is None, 'точка не скрыта')
        s['offer'] = offers[0]['deal_offer_id']
        d = call('GET', f"/deals/{s['deal']}", s[1])
        check(d['status'] == 'OFFERS' and 'agree' in d['actions'], 'нет действия agree')
        return f"интервал расстояния {offers[0]['distance_band']} м"
    step('ручной отклик; клиент видит интервал, а не точное расстояние', offer)

    def my_offer():
        req = [x for x in call('GET', '/requests', s[2]) if x['deal_id'] == s['deal']]
        check(req, 'после отклика запрос пропал из списка поставщика')
        mo = req[0]['my_offer']
        check(mo and mo['status'] == 'active' and float(mo['price']) == 900, f'my_offer {mo}')
        check(req[0]['place_mode'] == 'provider', f"place_mode {req[0]['place_mode']}")
        return f"my_offer {mo['deal_offer_id']}: {mo['price']} тг, {mo['status']}"
    step('поставщик: запрос с откликом остаётся в списке (my_offer)', my_offer)

    def agree():
        call('GET', f"/deals/{s['deal']}/routes", s[1])
        d = call('POST', f"/deals/{s['deal']}/offers/{s['offer']}/agree", s[1])
        check(d['status'] == 'AGREED', d['status'])
        routes = call('GET', f"/deals/{s['deal']}/routes", s[1])
        check(len(routes) == 3, 'нет 3 ссылок на маршрут')
        names = [f['label'] for f in d['counterpart_fields']]
        check('Гос. номер авто' not in names, 'гос. номер открыт до сближения')
        call('GET', f"/deals/{s['deal']}/close/code", s[1], expect=403)
        return 'ссылки: ' + ', '.join(r['provider_name'] for r in routes) + '; открыто: ' + ', '.join(names)
    step('согласование: маршрут, имя открыто, номер скрыт', agree)

    def contact():
        call('PUT', '/me/position', s[1], {'lat': P[0] + 0.0003, 'lon': P[1], 'source': 'manual'})
        call('PUT', '/me/position', s[2], {'lat': P[0] + 0.0004, 'lon': P[1], 'source': 'manual'})
        d = wait_status(s[1], s['deal'], 'CONTACT')
        names = [f['label'] for f in d['counterpart_fields']]
        check('Гос. номер авто' in names, 'гос. номер не открылся')
        pos = call('GET', f"/deals/{s['deal']}/positions", s[1])
        check({p['cursor'] for p in pos} == {'CLIENT_TEST', 'PROVIDER_TEST'}, 'курсоры')
        return 'CONTACT, открыто: ' + ', '.join(names)
    step('сближение курсорами -> CONTACT, открывается гос. номер', contact)

    def close():
        call('POST', f"/deals/{s['deal']}/close/request", s[1])
        notes = call('GET', '/notifications', s[2])
        check(any(n['type'] == 'CLOSE_REQUEST' for n in notes), 'поставщику не пришло CLOSE_REQUEST')
        code = call('GET', f"/deals/{s['deal']}/close/code", s[2])
        check(code['method'] == 'QR' and code['ttl_s'] == 10, f"способ {code['method']} {code['ttl_s']}")
        call('POST', f"/deals/{s['deal']}/close", s[1], {'code': '000000' if code['code'] != '000000' else '111111'}, expect=422)
        code = call('GET', f"/deals/{s['deal']}/close/code", s[2])
        d = call('POST', f"/deals/{s['deal']}/close", s[1], {'code': code['qr_payload']})
        check(d['status'] == 'CLOSED' and 'review' in d['actions'], d['status'])
        call('POST', '/deals', s[1], {'service_id': 1001, 'lat': P[0], 'lon': P[1], 'period_start': ts(60),
                                      'period_end': ts(90)}, expect=409)
        call('POST', f"/deals/{s['deal']}/review", s[1], {'score': 5, 'comment': 'Отлично'})
        call('POST', f"/deals/{s['deal']}/review", s[2], {'score': 5})
        check(call('GET', '/me', s[1])['pending_reviews'] == 0, 'баллы не учтены')
        return 'QR 10 с, неверный код - 422, без баллов новый запрос - 409'
    step('закрытие по QR, обязательные баллы', close)

    # --- тестовый режим: "Предоставить QR" с автоподтверждением + бесплатная отмена
    def test_mode():
        d = call('POST', '/deals', s[1], {'service_id': 1001, 'lat': P[0], 'lon': P[1], 'zone_radius_m': 150,
                                          'period_start': ts(130), 'period_end': ts(160), 'client_price': 800})
        call('POST', f"/deals/{d['deal_id']}/offers", s[2], {'price': 800})
        offer_id = call('GET', f"/deals/{d['deal_id']}/offers", s[1])[0]['deal_offer_id']
        call('POST', f"/deals/{d['deal_id']}/offers/{offer_id}/agree", s[1])
        wait_status(s[1], d['deal_id'], 'CONTACT')
        provided = call('POST', f"/deals/{d['deal_id']}/close/test-provide", s[2])
        note = [n for n in call('GET', '/notifications', s[1]) if n['type'] == 'CLOSE_CODE' and n['deal_id'] == d['deal_id']]
        check(note and note[-1]['payload']['code'] == provided['code'], 'код не пришел клиенту')
        closed = call('POST', f"/deals/{d['deal_id']}/close", s[1], {'code': note[-1]['payload']['code']})
        check(closed['status'] == 'CLOSED', closed['status'])
        call('POST', f"/deals/{d['deal_id']}/review", s[1], {'score': 4})
        call('POST', f"/deals/{d['deal_id']}/review", s[2], {'score': 5})
        d2 = call('POST', '/deals', s[1], {'service_id': 1001, 'lat': P[0], 'lon': P[1], 'zone_radius_m': 150,
                                           'period_start': ts(170), 'period_end': ts(200), 'client_price': 800})
        call('POST', f"/deals/{d2['deal_id']}/offers", s[2], {'price': 800})
        oid = call('GET', f"/deals/{d2['deal_id']}/offers", s[1])[0]['deal_offer_id']
        call('POST', f"/deals/{d2['deal_id']}/offers/{oid}/agree", s[1])
        res = call('POST', f"/deals/{d2['deal_id']}/cancel", s[1], {})
        check(res['status'] == 'CANCEL_FREE', res['status'])
        return 'код через уведомление CLOSE_CODE, закрыто; отмена в окне - CANCEL_FREE'
    step('режим тестирования: "Предоставить QR", бесплатная отмена', test_mode)

    # --- такси по пути (В-8)
    def taxi():
        A, B = (43.245000, 76.900000), (43.256000, 76.945000)
        slot = call('POST', '/slots', s[2], {'service_id': 1101, 'lat': A[0], 'lon': A[1], 'radius_m': 300,
                                              'to_lat': B[0], 'to_lon': B[1], 'radius_to_m': 1000,
                                              'period_start': ts(-5), 'period_end': ts(480)})
        check(slot['route_length_m'] > 3000 and slot['route_polyline'], 'маршрут водителя не построен')
        line = slot['route_polyline']
        # подача - точка у начала маршрута, назначение - у точки Б
        on = call('POST', '/deals', s[3], {'service_id': 1101, 'lat': A[0] + 0.0005, 'lon': A[1] + 0.0005,
                                           'to_lat': B[0] - 0.003, 'to_lon': B[1] - 0.002, 'zone_radius_m': 1500,
                                           'period_start': ts(30), 'period_end': ts(45),
                                           'attributes': [{'attribute_id': 22, 'value': '2'}]})
        back = call('POST', '/deals', s[1], {'service_id': 1101, 'lat': B[0] - 0.003, 'lon': B[1] - 0.002,
                                             'to_lat': A[0] + 0.0005, 'to_lon': A[1] + 0.0005, 'zone_radius_m': 1500,
                                             'period_start': ts(30), 'period_end': ts(45),
                                             'attributes': [{'attribute_id': 22, 'value': '1'}]})
        reqs = {x['deal_id']: x for x in call('GET', '/requests', s[2])}
        check(on['deal_id'] in reqs, 'поездка по пути не пришла водителю')
        check(back['deal_id'] not in reqs, 'поездка против хода пришла водителю')
        call('POST', f"/deals/{back['deal_id']}/offers", s[2], {'price': 2000}, expect=409)
        price = reqs[on['deal_id']]['recommended_price']
        call('POST', f"/deals/{on['deal_id']}/offers", s[2], {'price': price})
        call('POST', f"/deals/{back['deal_id']}/cancel", s[1], {})
        call('POST', f"/deals/{on['deal_id']}/cancel", s[3], {})
        return (f"маршрут водителя {slot['route_length_m']} м (polyline {len(line)} симв.), поездка "
                f"{on['route_length_m']} м, цена {price} тг; против хода - отказ")
    step('такси по пути: коридор, направление, цена по тарифу', taxi)

    step('выход', lambda: call('DELETE', '/session', s[1]))

    ok = sum(1 for r in results if r[0])
    for passed, name, info in results:
        print(('OK   ' if passed else 'FAIL ') + name + ('' if not info else ' - ' + str(info)))
    print(f'\nИтого: {ok} из {len(results)}')
    sys.exit(0 if ok == len(results) else 1)


if __name__ == '__main__':
    main()
