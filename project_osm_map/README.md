# OSM Карта — экран сделки маркетплейса услуг

Android-приложение показывает сделку из схемы `mp_service` на карте OpenStreetMap: точку клиента,
зону поиска и отклики поставщиков в порядке, заданном настройками сервиса. Клиент согласует отклик,
после этого появляется точная точка и кнопки маршрута в Яндекс Картах, 2ГИС и Google Maps.

ТЗ: [docs/TZ_marketplace.md](docs/TZ_marketplace.md), основной документ — в Claude Docs.

## Состав

| Папка | Что внутри |
| --- | --- |
| `app/` | Android-приложение (osmdroid, без ключей API) |
| `server/` | Локальный API на Java: читает `mp_service` и отдаёт JSON |
| `db/mp_service.sql` | Структура БД и тестовые данные (PostgreSQL 15+) |
| `docs/` | ТЗ в Markdown |

Приложение не подключается к БД напрямую: пароль есть только у сервера.

## Запуск

1. Схема и тестовые данные. Скрипт удаляет схему `mp_service` целиком.

   ```bash
   psql -h localhost -U postgres -d postgres -v ON_ERROR_STOP=1 -f db/mp_service.sql
   ```

2. Сервер API (JDK из Android Studio подходит):

   ```powershell
   $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
   $env:MP_DB_PASSWORD = "<пароль БД>"
   .\gradlew.bat :server:run
   ```

   Переменные: `MP_DB_URL` (по умолчанию `jdbc:postgresql://localhost:5432/postgres`), `MP_DB_USER` (`postgres`),
   `MP_DB_PASSWORD` (обязательна), `MP_API_HOST` (`127.0.0.1`), `MP_API_PORT` (`8080`),
   `MP_GEO_SECRET` (секрет смещения точек; без него — случайный на каждый запуск),
   `MP_DEV_LOGIN=1` (вход по `agent_id` без пароля — только разработка),
   `MP_OSM_FILE` (`data/kazakhstan-latest.osm.pbf`).

   Маршруты такси строит GraphHopper по `server/data/kazakhstan-latest.osm.pbf` (Geofabrik). Первый запуск
   импортирует граф в `server/data/gh-cache` — несколько минут и 1–2 ГБ памяти; дальше граф берётся из кэша.
   Папка `server/data/` в git не попадает. Без файла остальные методы работают, маршрутные отвечают 503
   `ROUTING_UNAVAILABLE`. Готовность — `GET /api/health` → `"routing":"готов"`.

   Проверка по HTTP (сервер запущен, нужен Python 3; сценарии меняют данные БД):

   ```powershell
   $env:MP_TEST_PASSWORD = "<общий пароль тестовых пользователей>"
   python server/tests/api_smoke.py
   ```

3. Проброс порта на эмулятор или телефон по USB:

   ```bash
   adb reverse tcp:8080 tcp:8080
   ```

   Приложение ходит на `http://127.0.0.1:8080` (`app/src/main/res/values/strings.xml`). Адрес эмулятора
   `10.0.2.2` не подходит: при targetSdk 37 Android не пускает приложение в частную сеть без разрешения
   на доступ к локальной сети.

4. Приложение: `.\gradlew.bat :app:assembleDebug`, затем установить `app/build/outputs/apk/debug/app-debug.apk`.
   Номер сделки — `demo_deal_id` в `strings.xml` (6 — кейс A/B/C/D).

Вернуть тестовые данные после согласования — снова выполнить `db/mp_service.sql`.

## API

API v0.7 — 39 методов: вход, конфигурация интерфейса, справочники, положение, запрос клиента, отклики,
согласование, публикации поставщика, закрытие кодом, отмена, баллы, уведомления. Полное описание —
[server/openapi.yaml](server/openapi.yaml).

Вход: `POST /api/session {login, password}` → `token`, дальше заголовок `Authorization: Bearer <token>`.
Язык ответа — `Accept-Language: RUS|ENG|KAZ|DEU`. Ошибки — `{"error": "...", "code": "КОД"}`; правила сделки
проверяет БД, их нарушение — 409 с кодом причины.

Приложение v0.6 (`app/`) с API v0.7 не совместимо — переписывается в этапах Э3–Э6.

## Карта

Подложка выбирается по справочнику `MAP_BASE_LAYER`. Если на устройстве есть офлайн-пакет
`Android/data/ru.mpavel.osmmap/files/maps/OSM_OFFLINE_ALMATY.mbtiles`, приложение берёт карту из него
и не ходит в сеть за тайлами. Иначе — онлайн-тайлы tile.openstreetmap.org, только для тестов: массовая
предзагрузка с этого сервера запрещена его политикой. Пакет должен быть растровым MBTiles (osmdroid
не читает векторные форматы). Скачивание пакета с CDN платформы ещё не сделано: адрес в справочнике — заглушка.
Подпись «© участники OpenStreetMap» всегда видна поверх карты.
