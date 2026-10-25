package ru.mpavel.osmmap.server;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Плановые задания (раз в минуту): run_deal_jobs() в БД (истечение, сближение, вопрос о закрытии, NOT_CLOSED)
 * и подбор такси по пути: БД не знает дорог, поэтому для маршрутных сервисов сервер сам проверяет маршрут,
 * рассылает NEW_REQUEST и создает автоотклики по тарифу водителя.
 */
final class Jobs {

    private final Db db;
    private final Auth auth;
    private volatile Routing routing;

    // кандидаты на маршрутные сделки: сделка, поставщик, его публикация и тариф
    private static final String SQL_ROUTE_CANDIDATES =
        "SELECT d.deal_id, c.provider_agent_id, d.geo_lat, d.geo_lon, d.geo_to_lat, d.geo_to_lon, d.route_length_m,"
        + " ps.geo_lat, ps.geo_lon, ps.geo_to_lat, ps.geo_to_lon, coalesce(ps.radius_m, 300), coalesce(ps.radius_to_m, 1000),"
        + " ps.route_polyline, ags.price_min, ags.unit_price_min, ags.is_auto,"
        + " EXISTS (SELECT 1 FROM notification n WHERE n.deal_id = d.deal_id AND n.agent_id = c.provider_agent_id"
        + "         AND n.notify_type_id = dict_id('NOTIFY_TYPE','NEW_REQUEST'))"
        + " FROM deal d"
        + " JOIN dictionary st ON st.dict_id = d.deal_status_id AND st.dict_code IN ('MATCHING','OFFERS')"
        + " JOIN v_deal_candidates c ON c.deal_id = d.deal_id"
        + " JOIN LATERAL (SELECT * FROM provider_slot p WHERE p.agent_id = c.provider_agent_id AND p.slot_status = 'active'"
        + "      AND service_covers(p.service_id, d.service_id)"
        + "      AND tsrange(d.period_start, d.period_end, '[]') <@ tsrange(p.period_start, p.period_end, '[]')"
        + "      ORDER BY p.slot_id DESC LIMIT 1) ps ON true"
        + " JOIN LATERAL (SELECT * FROM agent_service a WHERE a.agent_id = c.provider_agent_id AND a.is_active = 1"
        + "      AND service_covers(a.service_id, d.service_id) ORDER BY (a.service_id = d.service_id) DESC LIMIT 1) ags ON true"
        + " WHERE get_setting(d.service_id, 'geo_mode') = 'route'"
        + "   AND (?::numeric IS NULL OR d.deal_id = ?) AND (?::numeric IS NULL OR c.provider_agent_id = ?)";

    Jobs(Db db, Auth auth) {
        this.db = db;
        this.auth = auth;
    }

    void setRouting(Routing routing) {
        this.routing = routing;
    }

    void start() {
        ScheduledExecutorService ex = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "deal-jobs");
            t.setDaemon(true);
            return t;
        });
        ex.scheduleWithFixedDelay(this::tick, 10, 60, TimeUnit.SECONDS);
    }

    private void tick() {
        try {
            String result = db.tx(null, c -> Db.cell(c, "SELECT run_deal_jobs()::text"));
            if (result != null && result.matches(".*: [1-9].*")) {   // печатать только когда что-то изменилось
                System.out.println("Задания: " + result);
            }
            processRouteDeals(null);
            auth.purgeExpired();
        } catch (Exception e) {
            System.err.println("Задания: " + e.getMessage());
        }
    }

    /** Подбор такси по пути для одной сделки или всех открытых: уведомления и автоотклики. */
    void processRouteDeals(Long dealId) {
        if (routing == null) {
            return;
        }
        try {
            db.tx(null, c -> {
                for (String[] row : Db.rows(c, SQL_ROUTE_CANDIDATES, dealId, dealId, null, null)) {
                    if (!onTheWay(row)) {
                        continue;
                    }
                    long deal = Long.parseLong(row[0]);
                    long provider = Long.parseLong(row[1]);
                    if (!"t".equals(row[17])) {
                        Db.cell(c, "SELECT notify_agent(?, ?, 'NEW_REQUEST')", provider, deal);
                    }
                    if ("1".equals(row[16])) {
                        autoOffer(c, deal, provider, row);
                    }
                }
                return null;
            });
        } catch (SQLException e) {
            System.err.println("Подбор такси: " + e.getMessage());
        }
    }

    /** Открытые сделки, на которые поставщик может откликнуться (такси - только по пути). */
    List<Long> matchingDealsFor(long providerId) throws SQLException {
        return db.tx(providerId, c -> {
            List<Long> ids = new ArrayList<>();
            for (String[] row : Db.rows(c,
                    "SELECT c.deal_id FROM v_deal_candidates c JOIN deal d ON d.deal_id = c.deal_id"
                    + " WHERE c.provider_agent_id = ? AND get_setting(d.service_id, 'geo_mode') <> 'route'", providerId)) {
                ids.add(Long.valueOf(row[0]));
            }
            for (String[] row : Db.rows(c, SQL_ROUTE_CANDIDATES, null, null, providerId, providerId)) {
                if (onTheWay(row)) {
                    ids.add(Long.valueOf(row[0]));
                }
            }
            return ids;
        });
    }

    private boolean onTheWay(String[] row) {
        if (row[2] == null || row[4] == null) {
            return false;
        }
        return Routing.onTheWay(Routing.decode(row[13]),
            d(row[7]), d(row[8]), row[9] == null ? null : d(row[9]), row[10] == null ? null : d(row[10]),
            d(row[11]), d(row[12]), d(row[2]), d(row[3]), d(row[4]), d(row[5]));
    }

    /** Автоотклик водителя: цена max(порог, цена за км x длина), точка - начало его маршрута. */
    private static void autoOffer(Connection c, long deal, long provider, String[] row) throws SQLException {
        double km = row[6] == null ? 0 : Double.parseDouble(row[6]) / 1000;
        double price = Math.max(row[14] == null ? 0 : d(row[14]), (row[15] == null ? 0 : d(row[15])) * km);
        price = Math.ceil(price / 10) * 10;
        try {
            Db.update(c, "SAVEPOINT auto_offer");
            Db.update(c, "INSERT INTO deal_offer (deal_id, provider_agent_id, offer_price, offer_mode, geo_lat, geo_lon)"
                + " VALUES (?, ?, ?, 'auto', ?::numeric, ?::numeric)", deal, provider, price, row[7], row[8]);
            Db.update(c, "RELEASE SAVEPOINT auto_offer");
        } catch (SQLException e) {
            Db.update(c, "ROLLBACK TO SAVEPOINT auto_offer");
            System.err.println("Автоотклик такси " + deal + "/" + provider + ": " + e.getMessage());
        }
    }

    private static double d(String s) {
        return Double.parseDouble(s);
    }
}
