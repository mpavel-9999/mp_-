package ru.mpavel.osmmap.server;

import ru.mpavel.osmmap.server.Http.ApiException;
import ru.mpavel.osmmap.server.Http.Request;
import ru.mpavel.osmmap.server.Http.Response;

import java.sql.SQLException;
import java.util.List;
import java.util.regex.Matcher;

/** Поставщик: публикации, входящие запросы, отклик, настройки сервиса (ТЗ 4.5). */
final class ProviderApi {

    private final Db db;
    private final Routing routing;
    private final Jobs jobs;
    private final DealApi deals;

    ProviderApi(Db db, Routing routing, Jobs jobs, DealApi deals) {
        this.db = db;
        this.routing = routing;
        this.jobs = jobs;
        this.deals = deals;
    }

    /**
     * POST /api/slots {service_id, lat, lon, radius_m?, to_lat?, to_lon?, radius_to_m?, period_start, period_end, price_min?}
     * Публикация места или маршрута; для маршрута сервер строит линию по дорогам.
     * После публикации - уведомления и автоотклики по уже открытым подходящим запросам.
     */
    Response publish(Request r, Matcher m) throws SQLException {
        long me = r.agent();
        long slotId = db.tx(me, c -> {
            String[] b = Db.rows(c,
                "SELECT (b->>'service_id')::numeric, (b->>'lat')::numeric, (b->>'lon')::numeric,"
                + " (b->>'to_lat')::numeric, (b->>'to_lon')::numeric,"
                + " EXISTS (SELECT 1 FROM agent_service ags WHERE ags.agent_id = ? AND ags.is_active = 1"
                + "         AND service_covers(ags.service_id, (b->>'service_id')::numeric)"
                + "         AND EXISTS (SELECT 1 FROM v_agent_available_service a WHERE a.agent_id = ?"
                + "                     AND a.service_id = (b->>'service_id')::numeric))"
                + " FROM (SELECT ?::jsonb b) x", me, me, r.body).get(0);
            if (b[0] == null || b[1] == null) {
                throw new ApiException(400, "BAD_REQUEST", "Нужны service_id и точка");
            }
            if (!"t".equals(b[5])) {
                throw new ApiException(403, "NO_SERVICE_ACCESS", "Сервис не подключен или нет подписки");
            }
            String polyline = null;
            Long length = null;
            if (b[3] != null) {
                Routing.Route route = routing.route(Double.parseDouble(b[1]), Double.parseDouble(b[2]),
                                                    Double.parseDouble(b[3]), Double.parseDouble(b[4]));
                polyline = Routing.encode(route.points());
                length = Math.round(route.lengthM());
            }
            return Db.cellLong(c,
                "INSERT INTO provider_slot (agent_id, service_id, geo_lat, geo_lon, radius_m, geo_to_lat, geo_to_lon,"
                + " radius_to_m, route_polyline, route_length_m, period_start, period_end, price_min)"
                + " SELECT ?, (b->>'service_id')::numeric, (b->>'lat')::numeric, (b->>'lon')::numeric,"
                + "  (b->>'radius_m')::numeric, (b->>'to_lat')::numeric, (b->>'to_lon')::numeric,"
                + "  (b->>'radius_to_m')::numeric, ?, ?, (b->>'period_start')::timestamp, (b->>'period_end')::timestamp,"
                + "  (b->>'price_min')::numeric"
                + " FROM (SELECT ?::jsonb b) x RETURNING slot_id", me, polyline, length, r.body);
        });
        // открытые запросы, которые теперь подходят: точечные - рассылка и автоотклики в БД, маршрутные - на сервере
        db.tx(me, c -> Db.cell(c,
            "SELECT count(*) FROM (SELECT notify_agent(c.provider_agent_id, c.deal_id, 'NEW_REQUEST'), make_auto_offers(c.deal_id)"
            + " FROM v_deal_candidates c JOIN deal d ON d.deal_id = c.deal_id"
            + " WHERE c.provider_agent_id = ? AND get_setting(d.service_id, 'geo_mode') <> 'route') t", me));
        jobs.processRouteDeals(null);
        return Response.ok(db.tx(me, c -> Db.cell(c,
            "SELECT row_to_json(s) FROM provider_slot s WHERE slot_id = ?", slotId)));
    }

    /** GET /api/slots?mine=1 - мои публикации. */
    Response mySlots(Request r, Matcher m) throws SQLException {
        long me = r.agent();
        return Response.ok(db.tx(me, c -> Db.cell(c,
            "SELECT coalesce(json_agg(s ORDER BY s.period_start DESC), '[]') FROM ("
            + " SELECT ps.*, gt.text_value AS service_name FROM provider_slot ps"
            + " JOIN service sv ON sv.service_id = ps.service_id"
            + " JOIN glossary_text gt ON gt.glossary_id = sv.glossary_id AND gt.interface_lang_id = ?"
            + " WHERE ps.agent_id = ? AND (ps.slot_status = 'active' OR ps.period_end > now() - interval '1 day')) s",
            r.lang, me)));
    }

    /** DELETE /api/slots/{id} - снять публикацию. */
    Response removeSlot(Request r, Matcher m) throws SQLException {
        long me = r.agent();
        int n = db.tx(me, c -> Db.update(c,
            "UPDATE provider_slot SET slot_status = 'cancelled' WHERE slot_id = ? AND agent_id = ? AND slot_status = 'active'",
            Long.parseLong(m.group(1)), me));
        if (n == 0) {
            throw new ApiException(404, "NOT_FOUND", "Активная публикация не найдена");
        }
        return Response.ok("{\"status\":\"ok\"}");
    }

    /** GET /api/requests - подходящие открытые запросы с рекомендованной ценой; такси - только по пути. */
    Response requests(Request r, Matcher m) throws SQLException {
        long me = r.agent();
        List<Long> ids = jobs.matchingDealsFor(me);
        return Response.ok(db.tx(me, c -> Db.cell(c,
            "SELECT coalesce(json_agg(x ORDER BY x.period_start), '[]') FROM ("
            + " SELECT d.deal_id, d.service_id, gt.text_value AS service_name, d.period_start, d.period_end,"
            + "        d.geo_lat AS lat, d.geo_lon AS lon, d.zone_radius_m, d.geo_to_lat AS to_lat, d.geo_to_lon AS to_lon,"
            + "        d.route_length_m, d.client_price, d.price_forecast_min, d.price_forecast_max,"
            + "        get_setting(d.service_id, 'price_unit') AS price_unit, get_setting(d.service_id, 'geo_mode') AS geo_mode,"
            + "        agent_rating(d.client_agent_id, 'CLIENT', d.service_id) AS client_rating,"
            + "        NOT agent_has_rating(d.client_agent_id, 'CLIENT', d.service_id) AS client_newbie,"
            + "        (SELECT count(*) FROM deal_offer o WHERE o.deal_id = d.deal_id AND o.offer_status = 'active') AS offers_active,"
            + "        CASE WHEN get_setting(d.service_id, 'price_unit') = 'km'"
            + "             THEN ceil(greatest(coalesce(s.price_min, 0), coalesce(s.unit_price_min, 0) * d.route_length_m / 1000) / 10) * 10"
            + "             ELSE auto_offer_price(d.client_price, (d.price_forecast_min + d.price_forecast_max) / 2,"
            + "                  s.price_min, s.price_max, coalesce(get_setting(d.service_id, 'auto_price_round')::numeric, 50))"
            + "        END AS recommended_price"
            + " FROM deal d"
            + " JOIN service sv ON sv.service_id = d.service_id"
            + " JOIN glossary_text gt ON gt.glossary_id = sv.glossary_id AND gt.interface_lang_id = ?"
            + " JOIN LATERAL (SELECT * FROM agent_service a WHERE a.agent_id = ? AND a.is_active = 1"
            + "      AND service_covers(a.service_id, d.service_id) ORDER BY (a.service_id = d.service_id) DESC LIMIT 1) s ON true"
            + " WHERE d.deal_id = ANY (?::numeric[])) x",
            r.lang, me, ids.toArray(new Long[0]))));
    }

    /** POST /api/deals/{id}/offers {price, lat?, lon?, attributes} - ручной отклик; условия проверяет БД. */
    Response offer(Request r, Matcher m) throws SQLException {
        long me = r.agent();
        long dealId = Long.parseLong(m.group(1));
        if ("route".equals(db.tx(me, c -> Db.cell(c,
                "SELECT get_setting(service_id, 'geo_mode') FROM deal WHERE deal_id = ?", dealId)))
            && !jobs.matchingDealsFor(me).contains(dealId)) {
            throw new ApiException(409, "NOT_ON_THE_WAY", "Поездка не по вашему маршруту");
        }
        db.tx(me, c -> {
            // точка предложения: из запроса, иначе активная публикация, иначе точка обслуживания
            long offerId = Db.cellLong(c,
                "INSERT INTO deal_offer (deal_id, provider_agent_id, offer_price, offer_mode, geo_lat, geo_lon)"
                + " SELECT d.deal_id, ?, (b->>'price')::numeric, 'manual',"
                + "  coalesce((b->>'lat')::numeric, ps.geo_lat, ags.geo_lat), coalesce((b->>'lon')::numeric, ps.geo_lon, ags.geo_lon)"
                + " FROM deal d CROSS JOIN (SELECT ?::jsonb b) x"
                + " LEFT JOIN LATERAL (SELECT geo_lat, geo_lon FROM provider_slot p WHERE p.agent_id = ? AND p.slot_status = 'active'"
                + "      AND service_covers(p.service_id, d.service_id) ORDER BY p.slot_id DESC LIMIT 1) ps ON true"
                + " LEFT JOIN LATERAL (SELECT geo_lat, geo_lon FROM agent_service a WHERE a.agent_id = ? AND a.is_active = 1"
                + "      AND service_covers(a.service_id, d.service_id) LIMIT 1) ags ON true"
                + " WHERE d.deal_id = ? RETURNING deal_offer_id", me, r.body, me, me, dealId);
            Db.update(c,
                "INSERT INTO deal_attribute (deal_id, deal_offer_id, attribute_id, attribute_value)"
                + " SELECT ?, ?, (a->>'attribute_id')::numeric, a->>'value'"
                + " FROM jsonb_array_elements(coalesce(?::jsonb -> 'attributes', '[]')) a"
                + " WHERE coalesce(a->>'value', '') <> ''", dealId, offerId, r.body);
            return null;
        });
        return deals.offers(r, m);
    }

    /**
     * PUT /api/me/services/{serviceId} {lat, lon, zone_radius_m, work_time_from, work_time_to, price_min, price_max,
     * unit_price_min, client_rating_min, is_auto, capacity} - настройки поставщика; отсутствующие поля не меняются.
     */
    Response saveService(Request r, Matcher m) throws SQLException {
        long me = r.agent();
        long serviceId = Long.parseLong(m.group(1));
        int n = db.tx(me, c -> Db.update(c,
            "UPDATE agent_service s SET"
            + "  geo_lat = CASE WHEN jsonb_exists(b, 'lat') THEN (b->>'lat')::numeric ELSE s.geo_lat END,"
            + "  geo_lon = CASE WHEN jsonb_exists(b, 'lon') THEN (b->>'lon')::numeric ELSE s.geo_lon END,"
            + "  zone_radius_m = CASE WHEN jsonb_exists(b, 'zone_radius_m') THEN (b->>'zone_radius_m')::numeric ELSE s.zone_radius_m END,"
            + "  work_time_from = CASE WHEN jsonb_exists(b, 'work_time_from') THEN (b->>'work_time_from')::time ELSE s.work_time_from END,"
            + "  work_time_to = CASE WHEN jsonb_exists(b, 'work_time_to') THEN (b->>'work_time_to')::time ELSE s.work_time_to END,"
            + "  price_min = CASE WHEN jsonb_exists(b, 'price_min') THEN (b->>'price_min')::numeric ELSE s.price_min END,"
            + "  price_max = CASE WHEN jsonb_exists(b, 'price_max') THEN (b->>'price_max')::numeric ELSE s.price_max END,"
            + "  unit_price_min = CASE WHEN jsonb_exists(b, 'unit_price_min') THEN (b->>'unit_price_min')::numeric ELSE s.unit_price_min END,"
            + "  client_rating_min = CASE WHEN jsonb_exists(b, 'client_rating_min') THEN (b->>'client_rating_min')::numeric ELSE s.client_rating_min END,"
            + "  capacity = CASE WHEN jsonb_exists(b, 'capacity') THEN (b->>'capacity')::numeric ELSE s.capacity END,"
            + "  is_auto = CASE WHEN jsonb_exists(b, 'is_auto') THEN CASE WHEN (b->>'is_auto')::boolean THEN 1 ELSE 0 END ELSE s.is_auto END"
            + " FROM (SELECT ?::jsonb b) x WHERE s.agent_id = ? AND s.service_id = ?", r.body, me, serviceId));
        if (n == 0) {
            throw new ApiException(404, "NOT_FOUND", "Сервис не подключен");
        }
        return Response.ok("{\"status\":\"ok\"}");
    }
}
