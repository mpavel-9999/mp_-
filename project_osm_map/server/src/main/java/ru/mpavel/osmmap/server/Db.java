package ru.mpavel.osmmap.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Доступ к БД: соединение на запрос, параметры сессии для триггеров, транзакция.
 * mp.agent_id - кто действует (пусто = система), mp.geo_secret - секрет смещения точек до согласования.
 * TimeZone - пояс сессии: время в API алматинское без смещения в строке (Р-4), now() в БД - в том же поясе.
 */
final class Db {

    /** Пояс времени API и сессии БД (Р-4). */
    static final String TIME_ZONE = "Asia/Almaty";

    /** Работа внутри транзакции. */
    interface Work<T> {
        T run(Connection c) throws SQLException;
    }

    private final String url;
    private final String user;
    private final String password;
    private final String geoSecret;

    Db(String url, String user, String password, String geoSecret) {
        this.url = url;
        this.user = user;
        this.password = password;
        this.geoSecret = geoSecret;
    }

    /** Выполняет работу в одной транзакции от имени агента (null - система). */
    <T> T tx(Long agentId, Work<T> work) throws SQLException {
        try (Connection c = DriverManager.getConnection(url, user, password)) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT set_config('search_path', 'mp_service, public', true),"
                        + " set_config('TimeZone', '" + TIME_ZONE + "', true),"
                        + " set_config('mp.agent_id', ?, true), set_config('mp.geo_secret', ?, true)")) {
                    ps.setString(1, agentId == null ? "" : agentId.toString());
                    ps.setString(2, geoSecret);
                    ps.execute();
                }
                T result = work.run(c);
                c.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }

    /** Первая ячейка первой строки как текст; null - если строк нет. */
    static String cell(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    /** Первая ячейка как число; null - если строк нет или значение NULL. */
    static Long cellLong(Connection c, String sql, Object... params) throws SQLException {
        String v = cell(c, sql, params);
        return v == null ? null : Long.valueOf(v.contains(".") ? v.substring(0, v.indexOf('.')) : v);
    }

    /** Все строки как массивы текстовых значений. */
    static List<String[]> rows(Connection c, String sql, Object... params) throws SQLException {
        List<String[]> list = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                int n = rs.getMetaData().getColumnCount();
                while (rs.next()) {
                    String[] row = new String[n];
                    for (int i = 0; i < n; i++) {
                        row[i] = rs.getString(i + 1);
                    }
                    list.add(row);
                }
            }
        }
        return list;
    }

    static int update(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            return ps.executeUpdate();
        }
    }

    private static void bind(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
    }
}
