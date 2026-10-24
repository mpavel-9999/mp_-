package ru.mpavel.osmmap.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Локальный API над схемой mp_service.
 * JSON собирает сам PostgreSQL (json_agg / row_to_json), сервер только передает его клиенту.
 * Пароль БД задается переменной окружения и в приложение не попадает.
 *
 * Переменные окружения:
 *   MP_DB_URL      jdbc:postgresql://localhost:5432/postgres
 *   MP_DB_USER     postgres
 *   MP_DB_PASSWORD обязательна
 *   MP_API_HOST    127.0.0.1 (эмулятор Android видит его как 10.0.2.2)
 *   MP_API_PORT    8080
 *
 * Авторизации нет: сервер только для локальной разработки, клиент сделки - агент из тестовых данных.
 */
public final class ApiServer {

    private static final Pattern DEAL = Pattern.compile("^/api/deals/(\\d+)$");
    private static final Pattern DEAL_OFFERS = Pattern.compile("^/api/deals/(\\d+)/offers$");
    private static final Pattern DEAL_ROUTES = Pattern.compile("^/api/deals/(\\d+)/routes$");
    private static final Pattern DEAL_AGREE = Pattern.compile("^/api/deals/(\\d+)/offers/(\\d+)/agree$");

    private static final String SQL_BASE_LAYERS =
        "SELECT coalesce(json_agg(b ORDER BY b.sort_order), '[]') FROM ("
        + " SELECT d.dict_code AS code, d.sort_order, d.value_num AS version, d.value_text AS url,"
        + "        gt.text_value AS attribution"
        + " FROM mp_service.dictionary d"
        + " JOIN mp_service.glossary_text gt ON gt.glossary_id = d.glossary_id AND gt.interface_lang_id = ?"
        + " WHERE d.dict_type = 'MAP_BASE_LAYER' AND d.is_active = 1) b";

    private static final String SQL_SERVICES =
        "SELECT coalesce(json_agg(s ORDER BY s.service_path), '[]') FROM ("
        + " SELECT service_id, node_type, parent_service_id, service_name, service_path, icon_id"
        + " FROM mp_service.v_service_search"
        + " WHERE interface_lang_id = ? AND (? = '' OR search_text LIKE '%' || lower(?) || '%')) s";

    private static final String SQL_ICONS =
        "SELECT coalesce(json_agg(i ORDER BY i.icon_id), '[]') FROM ("
        + " SELECT icon_id, icon_code, icon_svg FROM mp_service.icon WHERE is_active = 1) i";

    private static final String SQL_DEAL =
        "SELECT row_to_json(x) FROM ("
        + " SELECT d.deal_id, d.service_id, gt.text_value AS service_name, ds.dict_code AS status,"
        + "        d.client_agent_id, d.geo_lat, d.geo_lon, d.zone_radius_m, d.geo_to_lat, d.geo_to_lon,"
        + "        d.period_start, d.period_end, d.price_forecast_min, d.price_forecast_max,"
        + "        mp_service.get_setting(d.service_id, 'geo_mode') AS geo_mode,"
        + "        mp_service.get_setting(d.service_id, 'price_unit') AS price_unit"
        + " FROM mp_service.deal d"
        + " JOIN mp_service.service s ON s.service_id = d.service_id"
        + " JOIN mp_service.glossary_text gt ON gt.glossary_id = s.glossary_id AND gt.interface_lang_id = ?"
        + " JOIN mp_service.dictionary ds ON ds.dict_id = d.deal_status_id"
        + " WHERE d.deal_id = ?) x";

    private static final String SQL_OFFERS =
        "SELECT coalesce(json_agg(o ORDER BY o.position), '[]')"
        + " FROM mp_service.v_deal_offer_list o WHERE o.deal_id = ?";

    private static final String SQL_ROUTES =
        "SELECT coalesce(json_agg(r ORDER BY r.deal_offer_id, r.sort_order), '[]') FROM ("
        + " SELECT r.deal_offer_id, r.provider_code, r.sort_order, r.route_url, gt.text_value AS provider_name"
        + " FROM mp_service.v_deal_offer_route r"
        + " JOIN mp_service.dictionary m ON m.dict_type = 'MAP_ROUTE_LINK' AND m.dict_code = r.provider_code"
        + " JOIN mp_service.glossary_text gt ON gt.glossary_id = m.glossary_id AND gt.interface_lang_id = ?"
        + " WHERE r.deal_id = ?) r";

    // Согласование отклика клиентом: только из статуса OFFERS, в одной транзакции
    private static final String SQL_AGREE_SELECT =
        "UPDATE mp_service.deal_offer SET is_selected = CASE WHEN deal_offer_id = ? THEN 1 ELSE 0 END"
        + " WHERE deal_id = ?";
    private static final String SQL_AGREE_DEAL =
        "UPDATE mp_service.deal d"
        + " SET provider_agent_id = o.provider_agent_id, price_provider = o.offer_price,"
        + "     fee_amount = 0, price_total = o.offer_price,"
        + "     deal_status_id = mp_service.dict_id('DEAL_STATUS', 'AGREED')"
        + " FROM mp_service.deal_offer o"
        + " WHERE o.deal_offer_id = ? AND o.deal_id = d.deal_id AND d.deal_id = ?"
        + "   AND d.deal_status_id = mp_service.dict_id('DEAL_STATUS', 'OFFERS')";
    private static final String SQL_AGREE_LOG =
        "INSERT INTO mp_service.deal_status_log (deal_status_log_id, deal_id, deal_status_id, agent_id, comment)"
        + " SELECT coalesce(max(deal_status_log_id), 0) + 1, ?, mp_service.dict_id('DEAL_STATUS', 'AGREED'),"
        + "        (SELECT client_agent_id FROM mp_service.deal WHERE deal_id = ?), 'Согласовано в приложении'"
        + " FROM mp_service.deal_status_log";

    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;

    private ApiServer(String dbUrl, String dbUser, String dbPassword) {
        this.dbUrl = dbUrl;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
    }

    public static void main(String[] args) throws IOException {
        String password = System.getenv("MP_DB_PASSWORD");
        if (password == null || password.isEmpty()) {
            System.err.println("Задайте пароль БД в переменной окружения MP_DB_PASSWORD");
            System.exit(1);
        }
        String host = env("MP_API_HOST", "127.0.0.1");
        int port = Integer.parseInt(env("MP_API_PORT", "8080"));
        ApiServer api = new ApiServer(
            env("MP_DB_URL", "jdbc:postgresql://localhost:5432/postgres"),
            env("MP_DB_USER", "postgres"),
            password);

        HttpServer server = HttpServer.create(new InetSocketAddress(host, port), 0);
        server.createContext("/api/", api::handle);
        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();
        System.out.println("mp_service API: http://" + host + ":" + port + "/api/health");
    }

    private static String env(String name, String def) {
        String v = System.getenv(name);
        return v == null || v.isEmpty() ? def : v;
    }

    private void handle(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        String method = ex.getRequestMethod();
        Map<String, String> q = query(ex.getRequestURI().getRawQuery());
        try {
            int lang = Integer.parseInt(q.getOrDefault("lang", "1"));
            Matcher m;
            if (method.equals("GET") && path.equals("/api/health")) {
                send(ex, 200, "{\"status\":\"ok\"}");
            } else if (method.equals("GET") && path.equals("/api/map/base-layers")) {
                send(ex, 200, json(SQL_BASE_LAYERS, lang));
            } else if (method.equals("GET") && path.equals("/api/services")) {
                String text = q.getOrDefault("q", "");
                send(ex, 200, json(SQL_SERVICES, lang, text, text));
            } else if (method.equals("GET") && path.equals("/api/icons")) {
                send(ex, 200, json(SQL_ICONS));
            } else if (method.equals("GET") && (m = DEAL.matcher(path)).matches()) {
                String body = json(SQL_DEAL, lang, Long.parseLong(m.group(1)));
                if (body == null) {
                    send(ex, 404, error("Сделка не найдена"));
                } else {
                    send(ex, 200, body);
                }
            } else if (method.equals("GET") && (m = DEAL_OFFERS.matcher(path)).matches()) {
                send(ex, 200, json(SQL_OFFERS, Long.parseLong(m.group(1))));
            } else if (method.equals("GET") && (m = DEAL_ROUTES.matcher(path)).matches()) {
                send(ex, 200, json(SQL_ROUTES, lang, Long.parseLong(m.group(1))));
            } else if (method.equals("POST") && (m = DEAL_AGREE.matcher(path)).matches()) {
                long dealId = Long.parseLong(m.group(1));
                long offerId = Long.parseLong(m.group(2));
                if (agree(dealId, offerId)) {
                    send(ex, 200, json(SQL_OFFERS, dealId));
                } else {
                    send(ex, 409, error("Согласовать можно только отклик этой сделки в статусе OFFERS"));
                }
            } else {
                send(ex, 404, error("Нет такого метода: " + method + " " + path));
            }
        } catch (NumberFormatException e) {
            send(ex, 400, error("Неверный параметр: " + e.getMessage()));
        } catch (SQLException e) {
            System.err.println(method + " " + path + ": " + e.getMessage());
            send(ex, 500, error("Ошибка БД: " + e.getMessage()));
        }
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }

    /** Выполняет запрос, который возвращает одну ячейку с JSON; null - если строк нет. */
    private String json(String sql, Object... params) throws SQLException {
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    private boolean agree(long dealId, long offerId) throws SQLException {
        try (Connection c = connect()) {
            c.setAutoCommit(false);
            try {
                int updated;
                try (PreparedStatement ps = c.prepareStatement(SQL_AGREE_DEAL)) {
                    bind(ps, offerId, dealId);
                    updated = ps.executeUpdate();
                }
                if (updated != 1) {
                    c.rollback();
                    return false;
                }
                try (PreparedStatement ps = c.prepareStatement(SQL_AGREE_SELECT)) {
                    bind(ps, offerId, dealId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = c.prepareStatement(SQL_AGREE_LOG)) {
                    bind(ps, dealId, dealId);
                    ps.executeUpdate();
                }
                c.commit();
                return true;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    private static void bind(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
    }

    private static Map<String, String> query(String raw) {
        Map<String, String> map = new HashMap<>();
        if (raw == null) {
            return map;
        }
        for (String pair : raw.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0) {
                map.put(URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8),
                        URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8));
            }
        }
        return map;
    }

    private static String error(String message) {
        return "{\"error\":\"" + message.replace("\\", "\\\\").replace("\"", "\\\"") + "\"}";
    }

    private static void send(HttpExchange ex, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }
}
