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
   `MP_DB_PASSWORD` (обязательна), `MP_API_HOST` (`127.0.0.1`), `MP_API_PORT` (`8080`).

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

| Метод | Ответ |
| --- | --- |
| `GET /api/health` | `{"status":"ok"}` |
| `GET /api/map/base-layers?lang=1` | Подложки карты из справочника `MAP_BASE_LAYER` |
| `GET /api/services?lang=1&q=стрижк` | Поиск по дереву услуг (`v_service_search`) |
| `GET /api/icons` | Пиктограммы (SVG) |
| `GET /api/deals/{id}?lang=1` | Сделка: услуга, статус, точка, радиус, настройки |
| `GET /api/deals/{id}/offers` | Отклики в порядке вывода (`v_deal_offer_list`) |
| `GET /api/deals/{id}/routes?lang=1` | Ссылки на маршрут после согласования (`v_deal_offer_route`) |
| `POST /api/deals/{id}/offers/{offerId}/agree` | Согласовать отклик (только из статуса `OFFERS`) |

Авторизации нет: сервер только для локальной разработки, клиент сделки — агент из тестовых данных.

## Карта

Подложка выбирается по справочнику `MAP_BASE_LAYER`. Если на устройстве есть офлайн-пакет
`Android/data/ru.mpavel.osmmap/files/maps/OSM_OFFLINE_ALMATY.mbtiles`, приложение берёт карту из него
и не ходит в сеть за тайлами. Иначе — онлайн-тайлы tile.openstreetmap.org, только для тестов: массовая
предзагрузка с этого сервера запрещена его политикой. Пакет должен быть растровым MBTiles (osmdroid
не читает векторные форматы). Скачивание пакета с CDN платформы ещё не сделано: адрес в справочнике — заглушка.
Подпись «© участники OpenStreetMap» всегда видна поверх карты.
