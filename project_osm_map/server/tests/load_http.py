"""
HTTP-замер горячих запросов на нагрузочном стенде (план М1, шаг 2). Только стандартная библиотека Python 3.

Подготовка: БД со стендом (db/load/mp_load.sql, SELECT mp_load.generate()), сервер с MP_DEV_LOGIN=1
(вход по agent_id - паролей не нужно). Параметры стенда берутся из mp_load.http_params():
    psql ... -Atc "SELECT mp_load.http_params()" > params.json
    python server/tests/load_http.py --params params.json --label before --out bench_http.sql
    psql ... -f bench_http.sql          # результаты -> mp_load.bench_result (method = http)

Каждый запрос Q1-Q3, Q5-Q8 гоняется --seconds секунд при 1, 20 и 50 параллельных клиентах (--clients);
у каждого клиента свой агент и токен. Новый запрос клиента (Q4) и плановое задание (Q9) по HTTP не
замеряются: Q4 меняет стенд и упирается в подбор (его время видно в mp_load.bench), Q9 - не HTTP.
"""
import argparse
import json
import os
import statistics
import threading
import time
import urllib.error
import urllib.request

BASE = os.environ.get('MP_API', 'http://127.0.0.1:8080/api')


def call(method, path, token=None, body=None):
    req = urllib.request.Request(BASE + path, method=method,
                                 data=None if body is None else json.dumps(body).encode('utf-8'))
    req.add_header('Content-Type', 'application/json')
    if token:
        req.add_header('Authorization', 'Bearer ' + token)
    try:
        with urllib.request.urlopen(req, timeout=120) as r:
            return r.status, r.read()
    except urllib.error.HTTPError as e:
        return e.code, e.read()


def login(agent_id):
    status, text = call('POST', '/session', body={'agent_id': agent_id})
    if status != 200:
        raise RuntimeError(f'вход агента {agent_id}: {status} {text[:200]}')
    return json.loads(text)['token']


def requests_for(code, p, k):
    """Агент и запрос k-го клиента для кода Q."""
    if code == 'Q1':
        return p['notif_agents'][k % len(p['notif_agents'])], 'GET', f"/notifications?since={p['since']}", None
    if code == 'Q2':
        return p['agreed'][k % len(p['agreed'])][1], 'PUT', '/me/position', {'lat': 43.24, 'lon': 76.89, 'source': 'gps'}
    if code == 'Q3':
        return p['providers'][k % len(p['providers'])], 'GET', '/requests', None
    if code == 'Q5':
        return p['clients'][k % len(p['clients'])], 'GET', '/deals', None
    if code == 'Q6':
        deal, client = p['agreed'][k % len(p['agreed'])]
        return client, 'GET', f'/deals/{deal}', None
    if code == 'Q7':
        deal, client = p['offers'][k % len(p['offers'])]
        return client, 'GET', f'/deals/{deal}/offers', None
    if code == 'Q8':
        return p['clients'][k % len(p['clients'])], 'GET', '/map/slots?lat=43.24&lon=76.89&radius=1000', None
    raise ValueError(code)


def run(code, clients, seconds, p, tokens):
    lat, errors = [], [0]
    lock = threading.Lock()
    stop = time.time() + seconds

    def worker(k):
        agent, method, path, body = requests_for(code, p, k)
        token = tokens[agent]
        mine, bad = [], 0
        while time.time() < stop:
            t0 = time.perf_counter()
            status, _ = call(method, path, token, body)
            ms = (time.perf_counter() - t0) * 1000
            if status == 200:
                mine.append(ms)
            else:
                bad += 1
        with lock:
            lat.extend(mine)
            errors[0] += bad

    t0 = time.time()
    threads = [threading.Thread(target=worker, args=(k,)) for k in range(clients)]
    for t in threads:
        t.start()
    for t in threads:
        t.join()
    elapsed = time.time() - t0
    if not lat:
        return {'calls': 0, 'errors': errors[0], 'p50': None, 'p95': None, 'max': None, 'rps': 0}
    lat.sort()
    return {'calls': len(lat), 'errors': errors[0], 'p50': round(statistics.median(lat), 2),
            'p95': round(lat[min(len(lat) - 1, int(len(lat) * 0.95))], 2), 'max': round(lat[-1], 2),
            'rps': round(len(lat) / elapsed, 1)}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--params', required=True)
    ap.add_argument('--label', required=True)
    ap.add_argument('--out', required=True, help='файл INSERT в mp_load.bench_result')
    ap.add_argument('--clients', default='1,20,50')
    ap.add_argument('--seconds', type=int, default=60)
    ap.add_argument('--queries', default='Q1,Q2,Q3,Q5,Q6,Q7,Q8')
    a = ap.parse_args()
    with open(a.params, encoding='utf-8') as f:
        p = json.load(f)
    levels = [int(x) for x in a.clients.split(',')]
    codes = a.queries.split(',')

    agents = {requests_for(c, p, k)[0] for c in codes for k in range(max(levels))}
    tokens = {ag: login(ag) for ag in sorted(agents)}
    print(f'вошли {len(tokens)} агентов')

    rows = []
    for code in codes:
        for n in levels:
            r = run(code, n, a.seconds, p, tokens)
            print(f"{code} x{n:>2}: {r['calls']:>6} вызовов, ошибок {r['errors']}, p50 {r['p50']} мс,"
                  f" p95 {r['p95']} мс, max {r['max']} мс, {r['rps']} в с", flush=True)
            rows.append((code, n, r))

    def num(v):
        return 'NULL' if v is None else str(v)
    with open(a.out, 'w', encoding='utf-8') as f:
        for code, n, r in rows:
            f.write("INSERT INTO mp_load.bench_result (label, query_code, method, clients, calls, errors,"
                    " ms_p50, ms_p95, ms_max, rps) VALUES "
                    f"('{a.label}', '{code}', 'http', {n}, {r['calls']}, {r['errors']}, {num(r['p50'])},"
                    f" {num(r['p95'])}, {num(r['max'])}, {r['rps']});\n")


if __name__ == '__main__':
    main()
