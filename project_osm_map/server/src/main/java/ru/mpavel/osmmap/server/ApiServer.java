package ru.mpavel.osmmap.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import ru.mpavel.osmmap.server.Http.ApiException;
import ru.mpavel.osmmap.server.Http.Request;
import ru.mpavel.osmmap.server.Http.Response;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * API v0.7 над схемой mp_service (docs/v0.7/03_implementation_spec.md, раздел 4).
 * Правила процесса сделки проверяет БД; сервер: сессии, код закрытия, маршруты по дорогам, плановые задания.
 * JSON ответов собирает PostgreSQL, тело запроса разбирается в SQL как jsonb.
 *
 * Переменные окружения:
 *   MP_DB_URL      jdbc:postgresql://localhost:5432/postgres
 *   MP_DB_USER     postgres
 *   MP_DB_PASSWORD обязательна
 *   MP_API_HOST    127.0.0.1 (эмуляторы - через adb reverse tcp:8080 tcp:8080)
 *   MP_API_PORT    8080
 *   MP_GEO_SECRET  секрет смещения точек до согласования (без него - случайный на запуск)
 *   MP_DEV_LOGIN   1 - вход по agent_id без пароля и список агентов (только разработка)
 *   MP_OSM_FILE    data/kazakhstan-latest.osm.pbf (граф дорог для такси)
 */
public final class ApiServer {

    /** Обработчик метода API. */
    interface Handler {
        Response handle(Request r, Matcher m) throws SQLException;
    }

    private record Route(String method, Pattern pattern, Handler handler) {
    }

    private static final Map<String, Integer> LANGS = Map.of("RUS", 1, "ENG", 2, "KAZ", 3, "DEU", 4);

    private final List<Route> routes = new ArrayList<>();
    private final Db db;
    private final Auth auth;

    private ApiServer(Db db, Auth auth) {
        this.db = db;
        this.auth = auth;
    }

    public static void main(String[] args) throws IOException {
        String password = System.getenv("MP_DB_PASSWORD");
        if (password == null || password.isEmpty()) {
            System.err.println("Задайте пароль БД в переменной окружения MP_DB_PASSWORD");
            System.exit(1);
        }
        String geoSecret = env("MP_GEO_SECRET", null);
        if (geoSecret == null) {
            byte[] b = new byte[16];
            new SecureRandom().nextBytes(b);
            geoSecret = HexFormat.of().formatHex(b);
            System.out.println("MP_GEO_SECRET не задан - смещения точек меняются при каждом запуске");
        }
        boolean devLogin = "1".equals(env("MP_DEV_LOGIN", "0"));
        Db db = new Db(env("MP_DB_URL", "jdbc:postgresql://localhost:5432/postgres"), env("MP_DB_USER", "postgres"),
                       password, geoSecret);
        Auth auth = new Auth();
        Routing routing = new Routing();
        routing.start(new File(env("MP_OSM_FILE", "data/kazakhstan-latest.osm.pbf")), new File("data/gh-cache"));
        Jobs jobs = new Jobs(db, auth);
        jobs.setRouting(routing);

        ConfigApi config = new ConfigApi(db, auth, devLogin);
        DealApi deals = new DealApi(db, routing, jobs);
        ProviderApi provider = new ProviderApi(db, routing, jobs, deals);

        ApiServer api = new ApiServer(db, auth);
        api.get("/api/health", (r, m) -> Response.ok("{\"status\":\"ok\",\"routing\":\"" + Http.escape(routing.state()) + "\"}"));
        // 4.1 вход
        api.route("POST", "/api/session", config::login);
        api.route("DELETE", "/api/session", config::logout);
        api.get("/api/dev/agents", config::devAgents);
        // 4.2 конфигурация и справочники
        api.get("/api/me", config::me);
        api.get("/api/config", config::config);
        api.route("PUT", "/api/config", config::saveConfig);
        api.get("/api/ui/options", config::uiOptions);
        api.get("/api/services", config::services);
        api.get("/api/services/(\\d+)/form", config::form);
        api.get("/api/services/(\\d+)/settings", config::settings);
        api.get("/api/map/base-layers", config::baseLayers);
        api.get("/api/icons", config::icons);
        // 4.3 положение
        api.route("PUT", "/api/me/position", config::position);
        api.get("/api/deals/(\\d+)/positions", deals::positions);
        // 4.4 клиент
        api.get("/api/map/slots", deals::slots);
        api.route("POST", "/api/forecast", deals::forecast);
        api.route("POST", "/api/deals", deals::create);
        api.get("/api/deals", deals::list);
        api.get("/api/deals/(\\d+)", deals::get);
        api.get("/api/deals/(\\d+)/offers", deals::offers);
        api.route("POST", "/api/deals/(\\d+)/offers/(\\d+)/agree", deals::agree);
        api.get("/api/deals/(\\d+)/routes", deals::routes);
        // 4.5 поставщик
        api.route("POST", "/api/slots", provider::publish);
        api.get("/api/slots", provider::mySlots);
        api.route("DELETE", "/api/slots/(\\d+)", provider::removeSlot);
        api.get("/api/requests", provider::requests);
        api.route("POST", "/api/deals/(\\d+)/offers", provider::offer);
        api.route("PUT", "/api/me/services/(\\d+)", provider::saveService);
        // 4.6 закрытие, отмена, баллы
        api.route("POST", "/api/deals/(\\d+)/close/request", deals::closeRequest);
        api.get("/api/deals/(\\d+)/close/code", deals::closeCode);
        api.route("POST", "/api/deals/(\\d+)/close", deals::close);
        api.route("POST", "/api/deals/(\\d+)/close/test-scan", deals::testScan);
        api.route("POST", "/api/deals/(\\d+)/close/test-provide", deals::testProvide);
        api.route("POST", "/api/deals/(\\d+)/cancel", deals::cancel);
        api.route("POST", "/api/deals/(\\d+)/review", deals::review);
        api.get("/api/me/pending-reviews", deals::pendingReviews);
        // 4.7 уведомления
        api.get("/api/notifications", deals::notifications);
        api.route("POST", "/api/notifications/(\\d+)/read", deals::read);

        String host = env("MP_API_HOST", "127.0.0.1");
        int port = Integer.parseInt(env("MP_API_PORT", "8080"));
        HttpServer server = HttpServer.create(new InetSocketAddress(host, port), 0);
        server.createContext("/api/", api::handle);
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
        jobs.start();
        System.out.println("mp_service API v0.7: http://" + host + ":" + port + "/api/health"
                           + (devLogin ? " (вход без пароля включен)" : ""));
    }

    private void route(String method, String path, Handler h) {
        routes.add(new Route(method, Pattern.compile("^" + path + "$"), h));
    }

    private void get(String path, Handler h) {
        route("GET", path, h);
    }

    private static String env(String name, String def) {
        String v = System.getenv(name);
        return v == null || v.isEmpty() ? def : v;
    }

    private void handle(HttpExchange ex) throws IOException {
        Response response;
        Request r = null;
        try {
            r = new Request(ex);
            r.agentId = auth.agent(r.header("Authorization"));
            r.lang = lang(r);
            response = dispatch(r);
        } catch (ApiException e) {
            response = Http.error(e.status, e.code, e.getMessage());
        } catch (NumberFormatException e) {
            response = Http.error(400, "BAD_REQUEST", "Неверный параметр: " + e.getMessage());
        } catch (PSQLException e) {
            response = fromDb(e);
            if (response.status() >= 500) {
                System.err.println((r == null ? "" : r.method + " " + r.path + ": ") + e.getMessage());
            }
        } catch (SQLException | RuntimeException e) {
            System.err.println((r == null ? "" : r.method + " " + r.path + ": ") + e);
            response = Http.error(500, "INTERNAL", e.getMessage());
        }
        Http.send(ex, response);
    }

    private Response dispatch(Request r) throws SQLException {
        boolean pathKnown = false;
        for (Route route : routes) {
            Matcher m = route.pattern().matcher(r.path);
            if (m.matches()) {
                pathKnown = true;
                if (route.method().equals(r.method)) {
                    return route.handler().handle(r, m);
                }
            }
        }
        throw new ApiException(pathKnown ? 405 : 404, "NOT_FOUND", "Нет такого метода: " + r.method + " " + r.path);
    }

    /** Язык ответа: Accept-Language (RUS/ENG/...), иначе язык агента, иначе русский. */
    private int lang(Request r) throws SQLException {
        String h = r.header("Accept-Language");
        if (h != null && LANGS.containsKey(h.trim().toUpperCase())) {
            return LANGS.get(h.trim().toUpperCase());
        }
        if (r.agentId != null) {
            Long id = db.tx(r.agentId, c -> Db.cellLong(c, "SELECT interface_lang_id FROM agent WHERE agent_id = ?", r.agentId));
            return id == null ? 1 : id.intValue();
        }
        return 1;
    }

    /**
     * Ошибка БД -> HTTP. Правила процесса (RAISE EXCEPTION) -> 409 с кодом из HINT;
     * дубль -> 409; неверные данные и ограничения -> 400.
     */
    private static Response fromDb(PSQLException e) {
        ServerErrorMessage msg = e.getServerErrorMessage();
        String state = e.getSQLState() == null ? "" : e.getSQLState();
        String text = msg != null && msg.getMessage() != null ? msg.getMessage() : e.getMessage();
        String hint = msg == null ? null : msg.getHint();
        return switch (state) {
            case "P0001" -> Http.error(409, hint != null ? hint : "RULE", text);
            case "23505" -> Http.error(409, "DUPLICATE", text);
            case "23514", "23502", "23503", "22P02", "22007", "22008", "22003", "22023" -> Http.error(400, "BAD_REQUEST", text);
            default -> Http.error(500, "DB_ERROR", text);
        };
    }
}
