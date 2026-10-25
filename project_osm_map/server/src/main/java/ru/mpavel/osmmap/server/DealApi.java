package ru.mpavel.osmmap.server;

import ru.mpavel.osmmap.server.Http.ApiException;
import ru.mpavel.osmmap.server.Http.Request;
import ru.mpavel.osmmap.server.Http.Response;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.regex.Matcher;

/**
 * Сделка глазами клиента и обеих сторон: публикации на карте, прогноз, запрос, отклики, согласование, маршрут,
 * положения, закрытие кодом, отмена, баллы, уведомления (ТЗ 4.4, 4.6, 4.7).
 * Правила процесса проверяет БД; здесь - права доступа к сделке и код закрытия.
 */
final class DealApi {

    private final Db db;
    private final Routing routing;
    private final Jobs jobs;

    DealApi(Db db, Routing routing, Jobs jobs) {
        this.db = db;
        this.routing = routing;
        this.jobs = jobs;
    }

    // ---------------------------------------------------------------- карта и прогноз

    /** GET /api/map/slots?lat&lon&radius&service_id - публикации поставщиков рядом (точки смещены). */
    Response slots(Request r, Matcher m) throws SQLException {
        long me = r.agent();
        double lat = Double.parseDouble(r.param("lat", "43.238"));
        double lon = Double.parseDouble(r.param("lon", "76.889"));
        double radius = Double.parseDouble(r.param("radius", "1000"));
        String service = r.param("service_id", null);
        Long serviceId = service == null ? null : Long.valueOf(service);
        return Response.ok(db.tx(me, c -> Db.cell(c,
            "SELECT coalesce(json_agg(s ORDER BY s.distance_m), '[]') FROM ("
            + " SELECT v.slot_id, v.agent_id, v.service_id, gt.text_value AS service_name, v.pseudonym, v.rating,"
            + "        v.is_testing = 1 AS is_testing, v.price_min, v.period_start, v.period_end,"
            + "        v.show_lat AS lat, v.show_lon AS lon, v.geo_to_lat AS to_lat, v.geo_to_lon AS to_lon,"
            + "        v.radius_m, v.radius_to_m, v.route_polyline, v.marker_label,"
            + "        round(geo_distance_m(?::numeric, ?::numeric, v.show_lat, v.show_lon), -1) AS distance_m"
            + " FROM v_provider_slot_map v"
            + " JOIN service s ON s.service_id = v.service_id"
            + " JOIN glossary_text gt ON gt.glossary_id = s.glossary_id AND gt.interface_lang_id = ?"
            + " WHERE v.agent_id <> ?"
            + "   AND (?::numeric IS NULL OR service_covers(v.service_id, ?::numeric) OR service_covers(?::numeric, v.service_id))"
            + "   AND geo_distance_m(?::numeric, ?::numeric, v.show_lat, v.show_lon) <= ?) s",
            lat, lon, r.lang, me, serviceId, serviceId, serviceId, lat, lon, radius)));
    }

    /** POST /api/forecast {service_id, lat, lon, to_lat?, to_lon?} - прогноз; для такси - оценка поездки. */
    Response forecast(Request r, Matcher m) throws SQLException {
        long me = r.agent();
        return Response.ok(db.tx(me, c -> forecastJson(c, r.body)));
    }

    private String forecastJson(Connection c, String body) throws SQLException {
        String[] b = Db.rows(c,
            "SELECT (b->>'service_id')::numeric, (b->>'lat')::numeric, (b->>'lon')::numeric,"
            + " (b->>'to_lat')::numeric, (b->>'to_lon')::numeric, get_setting((b->>'service_id')::numeric, 'price_unit')"
            + " FROM (SELECT ?::jsonb b) x", body).get(0);
        if (b[0] == null) {
            throw new ApiException(400, "BAD_REQUEST", "Нужен service_id");
        }
        if ("km".equals(b[5])) {
            if (b[3] == null || b[1] == null) {
                throw new ApiException(400, "BAD_REQUEST", "Для поездки нужны точки откуда и куда");
            }
            Routing.Route route = routing.route(Double.parseDouble(b[1]), Double.parseDouble(b[2]),
                                                Double.parseDouble(b[3]), Double.parseDouble(b[4]));
            double km = route.lengthM() / 1000;
            return Db.cell(c,
                "SELECT json_build_object('price_min', min(p), 'price_max', max(p),"
                + " 'price_median', round(percentile_cont(0.5) WITHIN GROUP (ORDER BY p)), 'providers', count(*),"
                + " 'route_length_m', ?, 'route_polyline', ?, 'price_unit', 'km')"
                + " FROM (SELECT ceil(greatest(coalesce(ags.price_min, 0), coalesce(ags.unit_price_min, 0) * ?) / 10) * 10 AS p"
                + "       FROM agent_service ags WHERE ags.is_active = 1 AND ags.unit_price_min IS NOT NULL"
                + "         AND service_covers(ags.service_id, ?)) t",
                Math.round(route.lengthM()), Routing.encode(route.points()), km, Long.valueOf(b[0]));
        }
        return Db.cell(c,
            "SELECT json_build_object('price_min', f.price_min, 'price_max', f.price_max,"
            + " 'price_median', f.price_median, 'providers', f.providers, 'price_unit', get_setting(?, 'price_unit'))"
            + " FROM price_forecast(?, ?::numeric, ?::numeric) f",
            Long.valueOf(b[0]), Long.valueOf(b[0]), b[1], b[2]);
    }

    // ---------------------------------------------------------------- запрос клиента

    /**
     * POST /api/deals {service_id, slot_id?, lat, lon, to_lat?, to_lon?, zone_radius_m, period_start, period_end,
     * provider_rating_min?, client_price?, attributes: [{attribute_id, value}]}.
     * INIT -> поля -> MATCHING -> автоотклики; для такси - длина маршрута и подбор по пути на сервере.
     */
    Response create(Request r, Matcher m) throws SQLException {
        long me = r.agent();
        long dealId = db.tx(me, c -> {
            if (Db.cellLong(c, "SELECT agent_pending_reviews(?)", me) > 0) {
                throw new ApiException(409, "REVIEW_REQUIRED", "Сначала поставьте баллы по прошлым сделкам");
            }
            String[] b = Db.rows(c,
                "SELECT (b->>'service_id')::numeric, get_setting((b->>'service_id')::numeric, 'geo_mode'),"
                + " (b->>'lat')::numeric, (b->>'lon')::numeric, (b->>'to_lat')::numeric, (b->>'to_lon')::numeric,"
                + " EXISTS (SELECT 1 FROM v_agent_available_service a WHERE a.agent_id = ? AND a.service_id = (b->>'service_id')::numeric)"
                + " FROM (SELECT ?::jsonb b) x", me, r.body).get(0);
            if (b[0] == null) {
                throw new ApiException(400, "BAD_REQUEST", "Нужен service_id");
            }
            if (!"t".equals(b[6])) {
                throw new ApiException(403, "NO_SERVICE_ACCESS", "Сервис недоступен по вашей подписке");
            }
            Long routeM = null;
            if ("route".equals(b[1])) {
                if (b[4] == null || b[2] == null) {
                    throw new ApiException(400, "BAD_REQUEST", "Для поездки нужны точки откуда и куда");
                }
                routeM = Math.round(routing.route(Double.parseDouble(b[2]), Double.parseDouble(b[3]),
                                                  Double.parseDouble(b[4]), Double.parseDouble(b[5])).lengthM());
            }
            String forecast = b[2] == null && !"none".equals(b[1]) ? null : forecastJson(c, r.body);
            long id = Db.cellLong(c,
                "INSERT INTO deal (service_id, client_agent_id, slot_id, deal_status_id, payment_method_id,"
                + " geo_lat, geo_lon, zone_radius_m, geo_to_lat, geo_to_lon, route_length_m, period_start, period_end,"
                + " price_forecast_min, price_forecast_max, client_price, provider_rating_min)"
                + " SELECT (b->>'service_id')::numeric, ?, (b->>'slot_id')::numeric, dict_id('DEAL_STATUS','INIT'),"
                + "  dict_id('PAYMENT_METHOD','DIRECT'), (b->>'lat')::numeric, (b->>'lon')::numeric,"
                + "  coalesce((b->>'zone_radius_m')::numeric, 100), (b->>'to_lat')::numeric, (b->>'to_lon')::numeric, ?,"
                + "  (b->>'period_start')::timestamp, (b->>'period_end')::timestamp,"
                + "  (f->>'price_min')::numeric, (f->>'price_max')::numeric,"
                + "  (b->>'client_price')::numeric, (b->>'provider_rating_min')::numeric"
                + " FROM (SELECT ?::jsonb b, ?::jsonb f) x RETURNING deal_id",
                me, routeM, r.body, forecast == null ? "{}" : forecast);
            Db.update(c,
                "INSERT INTO deal_attribute (deal_id, attribute_id, attribute_value)"
                + " SELECT ?, (a->>'attribute_id')::numeric, a->>'value'"
                + " FROM jsonb_array_elements(coalesce(?::jsonb -> 'attributes', '[]')) a"
                + " WHERE coalesce(a->>'value', '') <> ''", id, r.body);
            String missing = Db.cell(c,
                "SELECT string_agg(l.label, ', ' ORDER BY e.sort_order)"
                + " FROM v_service_attribute_effective e"
                + " JOIN v_attribute_label l ON l.attribute_id = e.attribute_id AND l.interface_lang_id = ?"
                + " WHERE e.service_id = (SELECT service_id FROM deal WHERE deal_id = ?)"
                + "   AND e.attr_scope = 'request' AND e.is_required = 1"
                + "   AND NOT EXISTS (SELECT 1 FROM deal_attribute da WHERE da.deal_id = ? AND da.deal_offer_id IS NULL"
                + "                   AND da.attribute_id = e.attribute_id)", r.lang, id, id);
            if (missing != null) {
                throw new ApiException(400, "REQUIRED_FIELDS", "Заполните поля: " + missing);
            }
            Db.update(c, "UPDATE deal SET deal_status_id = dict_id('DEAL_STATUS','MATCHING') WHERE deal_id = ?", id);
            Db.cell(c, "SELECT make_auto_offers(?)", id);
            return id;
        });
        jobs.processRouteDeals(dealId);
        return Response.ok(dealJson(me, dealId, r.lang));
    }

    /** GET /api/deals?role=client|provider|all&status=active|all */
    Response list(Request r, Matcher m) throws SQLException {
        long me = r.agent();
        String role = r.param("role", "all");
        String status = r.param("status", "active");
        return Response.ok(db.tx(me, c -> Db.cell(c,
            "SELECT coalesce(json_agg(x ORDER BY x.period_start DESC), '[]') FROM ("
            + " SELECT d.deal_id, d.service_id, sn.text_value AS service_name, st.dict_code AS status,"
            + "        stt.text_value AS status_name,"
            + "        CASE WHEN d.client_agent_id = ? THEN 'client' ELSE 'provider' END AS my_role,"
            + "        d.period_start, d.period_end, d.price_total, d.client_price, d.price_forecast_min, d.price_forecast_max,"
            + "        get_setting(d.service_id, 'price_unit') AS price_unit, d.geo_lat AS lat, d.geo_lon AS lon,"
            + "        (SELECT count(*) FROM deal_offer o WHERE o.deal_id = d.deal_id AND o.offer_status = 'active') AS offers_active,"
            + "        agent_pseudonym(CASE WHEN d.client_agent_id = ? THEN d.provider_agent_id ELSE d.client_agent_id END) AS counterpart,"
            + "        (d.provider_agent_id IS NOT NULL AND st.dict_code IN ('CLOSED','CANCEL_CLIENT','CANCEL_PROVIDER')"
            + "         AND NOT EXISTS (SELECT 1 FROM deal_review rv WHERE rv.deal_id = d.deal_id AND rv.from_agent_id = ?)"
            + "         AND (st.dict_code = 'CLOSED'"
            + "              OR (st.dict_code = 'CANCEL_CLIENT' AND d.provider_agent_id = ?)"
            + "              OR (st.dict_code = 'CANCEL_PROVIDER' AND d.client_agent_id = ?))) AS review_pending"
            + " FROM deal d"
            + " JOIN dictionary st ON st.dict_id = d.deal_status_id"
            + " JOIN glossary_text stt ON stt.glossary_id = st.glossary_id AND stt.interface_lang_id = ?"
            + " JOIN service s ON s.service_id = d.service_id"
            + " JOIN glossary_text sn ON sn.glossary_id = s.glossary_id AND sn.interface_lang_id = ?"
            + " WHERE ((d.client_agent_id = ? AND ? IN ('client','all'))"
            + "     OR ((d.provider_agent_id = ? OR EXISTS (SELECT 1 FROM deal_offer o WHERE o.deal_id = d.deal_id"
            + "          AND o.provider_agent_id = ? AND o.offer_status = 'active')) AND ? IN ('provider','all')))"
            + ") x WHERE ? = 'all' OR x.status NOT IN ('CLOSED','CANCEL_FREE','CANCEL_CLIENT','CANCEL_PROVIDER','EXPIRED','NOT_CLOSED')"
            + "   OR x.review_pending",
            me, me, me, me, me, r.lang, r.lang, me, role, me, me, role, status)));
    }

    /** GET /api/deals/{id} - сделка, допустимые действия, открытые поля другой стороны. */
    Response get(Request r, Matcher m) throws SQLException {
        long me = r.agent();
        return Response.ok(dealJson(me, Long.parseLong(m.group(1)), r.lang));
    }

    /** Роль агента в сделке: client, provider, candidate (может откликнуться), offerer; иначе 403/404. */
    private static String roleIn(Connection c, long dealId, long me) throws SQLException {
        String role = Db.cell(c,
            "SELECT CASE WHEN d.client_agent_id = ? THEN 'client'"
            + "            WHEN d.provider_agent_id = ? THEN 'provider'"
            + "            WHEN EXISTS (SELECT 1 FROM deal_offer o WHERE o.deal_id = d.deal_id AND o.provider_agent_id = ?) THEN 'offerer'"
            + "            WHEN (SELECT ok FROM provider_can_offer(d.deal_id, ?)) THEN 'candidate'"
            + "            ELSE 'none' END FROM deal d WHERE d.deal_id = ?", me, me, me, me, dealId);
        if (role == null) {
            throw new ApiException(404, "NOT_FOUND", "Сделка не найдена");
        }
        if (role.equals("none")) {
            throw new ApiException(403, "FORBIDDEN", "Нет доступа к сделке");
        }
        return role;
    }

    String dealJson(long me, long dealId, int lang) throws SQLException {
        return db.tx(me, c -> {
            roleIn(c, dealId, me);
            return Db.cell(c, SQL_DEAL, dealId, me, lang);
        });
    }

    // Видимость полей другой стороны по show_stage: always; agreed - после согласования; near - после сближения
    private static final String SQL_DEAL =
        "WITH p AS (SELECT ?::numeric AS deal_id, ?::numeric AS me, ?::numeric AS lang),"
        + " d AS (SELECT d.*, st.dict_code AS status, stt.text_value AS status_name, p.me, p.lang,"
        + "        CASE WHEN d.client_agent_id = p.me THEN 'client' WHEN d.provider_agent_id = p.me THEN 'provider'"
        + "             WHEN EXISTS (SELECT 1 FROM deal_offer o WHERE o.deal_id = d.deal_id AND o.provider_agent_id = p.me) THEN 'offerer'"
        + "             ELSE 'candidate' END AS my_role,"
        + "        CASE WHEN d.client_agent_id = p.me THEN d.provider_agent_id ELSE d.client_agent_id END AS other_id,"
        + "        st.dict_code IN ('AGREED','CONTACT','CLOSED','NOT_CLOSED','CANCEL_CLIENT','CANCEL_PROVIDER')"
        + "          OR (st.dict_code = 'CANCEL_FREE' AND d.date_agreed IS NOT NULL) AS agreed_stage,"
        + "        st.dict_code IN ('CONTACT','CLOSED') OR EXISTS (SELECT 1 FROM deal_status_log l WHERE l.deal_id = d.deal_id"
        + "          AND l.deal_status_id = dict_id('DEAL_STATUS','CONTACT')) AS near_stage,"
        + "        get_setting(d.service_id, 'place_mode') AS place_mode,"
        + "        (SELECT is_testing = 1 FROM agent WHERE agent_id = p.me) AS me_testing"
        + "      FROM deal d CROSS JOIN p"
        + "      JOIN dictionary st ON st.dict_id = d.deal_status_id"
        + "      JOIN glossary_text stt ON stt.glossary_id = st.glossary_id AND stt.interface_lang_id = p.lang"
        + "      WHERE d.deal_id = p.deal_id),"
        + " x AS (SELECT d.*, (d.provider_agent_id IS NOT NULL AND d.my_role IN ('client','provider')"
        + "         AND NOT EXISTS (SELECT 1 FROM deal_review rv WHERE rv.deal_id = d.deal_id AND rv.from_agent_id = d.me)"
        + "         AND (d.status = 'CLOSED' OR (d.status = 'CANCEL_CLIENT' AND d.my_role = 'provider')"
        + "              OR (d.status = 'CANCEL_PROVIDER' AND d.my_role = 'client'))) AS review_pending,"
        + "        (d.my_role = 'candidate' AND (SELECT ok FROM provider_can_offer(d.deal_id, d.me))) AS can_offer"
        + "      FROM d)"
        + " SELECT json_build_object("
        + "  'deal_id', x.deal_id, 'service_id', x.service_id,"
        + "  'service_name', (SELECT gt.text_value FROM service s JOIN glossary_text gt ON gt.glossary_id = s.glossary_id"
        + "                   AND gt.interface_lang_id = x.lang WHERE s.service_id = x.service_id),"
        + "  'service_path', (SELECT service_path FROM v_service_search v WHERE v.service_id = x.service_id AND v.interface_lang_id = x.lang),"
        + "  'status', x.status, 'status_name', x.status_name, 'my_role', x.my_role,"
        + "  'geo_mode', get_setting(x.service_id, 'geo_mode'), 'place_mode', x.place_mode,"
        + "  'price_unit', get_setting(x.service_id, 'price_unit'), 'close_method', get_setting(x.service_id, 'close_method'),"
        + "  'near_radius_m', get_setting(x.service_id, 'near_radius_m'), 'currency', get_setting(x.service_id, 'currency'),"
        + "  'lat', x.geo_lat, 'lon', x.geo_lon, 'zone_radius_m', x.zone_radius_m, 'to_lat', x.geo_to_lat, 'to_lon', x.geo_to_lon,"
        + "  'route_length_m', x.route_length_m, 'period_start', x.period_start, 'period_end', x.period_end,"
        + "  'price_forecast_min', x.price_forecast_min, 'price_forecast_max', x.price_forecast_max,"
        + "  'client_price', x.client_price, 'provider_rating_min', x.provider_rating_min,"
        + "  'price_provider', x.price_provider, 'price_total', x.price_total,"
        + "  'date_creation', x.date_creation, 'date_agreed', x.date_agreed, 'date_close', x.date_close, 'slot_id', x.slot_id,"
        + "  'client', json_build_object('pseudonym', agent_pseudonym(x.client_agent_id),"
        + "      'rating', agent_rating(x.client_agent_id, 'CLIENT', x.service_id),"
        + "      'newbie', NOT agent_has_rating(x.client_agent_id, 'CLIENT', x.service_id),"
        + "      'is_testing', (SELECT is_testing = 1 FROM agent WHERE agent_id = x.client_agent_id)),"
        + "  'provider', CASE WHEN x.provider_agent_id IS NOT NULL THEN json_build_object("
        + "      'pseudonym', agent_pseudonym(x.provider_agent_id),"
        + "      'rating', agent_rating(x.provider_agent_id, 'PROVIDER', x.service_id),"
        + "      'is_testing', (SELECT is_testing = 1 FROM agent WHERE agent_id = x.provider_agent_id)) END,"
        + "  'counterpart_fields', (SELECT coalesce(json_agg(json_build_object('attribute_id', f.attribute_id, 'label', f.label,"
        + "        'icon_code', f.icon_code, 'value', f.value) ORDER BY f.sort_order), '[]') FROM ("
        + "      SELECT DISTINCT ON (e.attribute_id) e.attribute_id, e.sort_order, l.label, l.icon_code, v.attribute_value AS value"
        + "      FROM v_service_attribute_effective e"
        + "      JOIN v_attribute_label l ON l.attribute_id = e.attribute_id AND l.interface_lang_id = x.lang"
        + "      JOIN LATERAL (SELECT aa.attribute_value FROM agent_attribute aa WHERE aa.agent_id = x.other_id"
        + "           AND aa.attribute_id = e.attribute_id AND aa.is_active = 1 ORDER BY aa.date_start DESC LIMIT 1) v ON true"
        + "      WHERE e.service_id IN (x.service_id, 1) AND e.attr_scope = 'profile' AND x.other_id IS NOT NULL"
        + "        AND x.my_role IN ('client','provider') AND e.attribute_id <> 16"
        + "        AND (e.show_stage = 'always' OR (e.show_stage = 'agreed' AND x.agreed_stage)"
        + "             OR (e.show_stage = 'near' AND x.near_stage))"
        + "      ORDER BY e.attribute_id, (e.service_id = x.service_id) DESC) f),"
        + "  'request_fields', (SELECT coalesce(json_agg(json_build_object('attribute_id', da.attribute_id, 'label', l.label,"
        + "        'icon_code', l.icon_code, 'value', da.attribute_value) ORDER BY e.sort_order), '[]')"
        + "      FROM deal_attribute da"
        + "      JOIN v_service_attribute_effective e ON e.service_id = x.service_id AND e.attribute_id = da.attribute_id"
        + "      JOIN v_attribute_label l ON l.attribute_id = da.attribute_id AND l.interface_lang_id = x.lang"
        + "      WHERE da.deal_id = x.deal_id AND da.deal_offer_id IS NULL"
        + "        AND (x.my_role = 'client' OR e.show_stage = 'always' OR (e.show_stage = 'agreed' AND x.agreed_stage)"
        + "             OR (e.show_stage = 'near' AND x.near_stage))),"
        + "  'review_pending', x.review_pending,"
        // итог отмены, если нажать сейчас: правило в БД (deal_cancel_mode), экран лишь предупреждает
        + "  'cancel_mode', deal_cancel_mode(x.deal_id, x.me),"
        + "  'actions', array_remove(ARRAY["
        + "      CASE WHEN x.my_role = 'client' AND x.status = 'OFFERS' THEN 'agree' END,"
        + "      CASE WHEN x.my_role = 'client' AND x.status IN ('INIT','MATCHING','OFFERS','AGREED','CONTACT') THEN 'cancel' END,"
        + "      CASE WHEN x.my_role = 'provider' AND x.status IN ('AGREED','CONTACT') THEN 'cancel' END,"
        + "      CASE WHEN x.my_role IN ('client','provider') AND x.status IN ('AGREED','CONTACT') THEN 'close' END,"
        + "      CASE WHEN x.my_role = 'provider' AND x.status IN ('AGREED','CONTACT') THEN 'show_code' END,"
        + "      CASE WHEN x.my_role = 'client' AND (x.status = 'CONTACT' OR (x.status = 'AGREED' AND x.place_mode = 'online'))"
        + "           THEN 'enter_code' END,"
        + "      CASE WHEN x.my_role = 'client' AND x.me_testing AND (x.status = 'CONTACT'"
        + "           OR (x.status = 'AGREED' AND x.place_mode = 'online')) THEN 'test_scan' END,"
        + "      CASE WHEN x.my_role = 'provider' AND x.me_testing AND x.status IN ('AGREED','CONTACT') THEN 'test_provide' END,"
        + "      CASE WHEN x.my_role IN ('client','provider') AND x.status IN ('AGREED','CONTACT') THEN 'route' END,"
        + "      CASE WHEN x.can_offer THEN 'offer' END,"
        + "      CASE WHEN x.review_pending THEN 'review' END], NULL)"
        + " ) FROM x";

    // ---------------------------------------------------------------- отклики, согласование, маршрут

    /** GET /api/deals/{id}/offers - клиенту все отклики; поставщику - свой. */
    Response offers(Request r, Matcher m) throws SQLException {
        long me = r.agent();
        long dealId = Long.parseLong(m.group(1));
        return Response.ok(db.tx(me, c -> {
            String role = roleIn(c, dealId, me);
            return Db.cell(c,
                "SELECT coalesce(json_agg(o ORDER BY o.position), '[]') FROM ("
                + " SELECT l.*, a.is_testing = 1 AS is_testing,"
                + "        NOT agent_has_rating(l.provider_agent_id, 'PROVIDER', l.service_id) AS newbie"
                + " FROM v_deal_offer_list l JOIN agent a ON a.agent_id = l.provider_agent_id"
                + " WHERE l.deal_id = ? AND (? = 'client' OR l.provider_agent_id = ?)) o", dealId, role, me);
        }));
    }

    /** POST /api/deals/{id}/offers/{offerId}/agree */
    Response agree(Request r, Matcher m) throws SQLException {
        long me = r.agent();
        long dealId = Long.parseLong(m.group(1));
        long offerId = Long.parseLong(m.group(2));
        db.tx(me, c -> Db.cell(c, "SELECT deal_agree(?, ?)", dealId, offerId));
        return Response.ok(dealJson(me, dealId, r.lang));
    }

    /** GET /api/deals/{id}/routes - ссылки на маршрут к точному месту (после согласования). */
    Response routes(Request r, Matcher m) throws SQLException {
        long me = r.agent();
        long dealId = Long.parseLong(m.group(1));
        return Response.ok(db.tx(me, c -> {
            String role = roleIn(c, dealId, me);
            if (!role.equals("client") && !role.equals("provider")) {
                throw new ApiException(403, "FORBIDDEN", "Маршрут доступен сторонам сделки");
            }
            return Db.cell(c,
                "SELECT coalesce(json_agg(r ORDER BY r.sort_order), '[]') FROM ("
                + " SELECT r.deal_offer_id, r.provider_code, r.sort_order, r.route_url, gt.text_value AS provider_name"
                + " FROM v_deal_offer_route r"
                + " JOIN dictionary m ON m.dict_type = 'MAP_ROUTE_LINK' AND m.dict_code = r.provider_code"
                + " JOIN glossary_text gt ON gt.glossary_id = m.glossary_id AND gt.interface_lang_id = ?"
                + " WHERE r.deal_id = ?) r", r.lang, dealId);
        }));
    }

    /** GET /api/deals/{id}/positions - положения сторон согласованной сделки и код курсора. */
    Response positions(Request r, Matcher m) throws SQLException {
        long me = r.agent();
        long dealId = Long.parseLong(m.group(1));
        return Response.ok(db.tx(me, c -> {
            String role = roleIn(c, dealId, me);
            if (!role.equals("client") && !role.equals("provider")) {
                throw new ApiException(403, "FORBIDDEN", "Положения видят только стороны сделки");
            }
            return Db.cell(c,
                "SELECT coalesce(json_agg(json_build_object('agent_id', s.agent_id, 'role', s.role,"
                + "   'lat', p.geo_lat, 'lon', p.geo_lon, 'source', p.source, 'date_update', p.date_update,"
                + "   'cursor', upper(s.role) || CASE WHEN a.is_testing = 1 THEN '_TEST' ELSE '' END)), '[]')"
                + " FROM deal d"
                + " CROSS JOIN LATERAL (VALUES (d.client_agent_id, 'client'), (d.provider_agent_id, 'provider')) s(agent_id, role)"
                + " JOIN agent a ON a.agent_id = s.agent_id"
                + " JOIN agent_position p ON p.agent_id = s.agent_id"
                + "  AND p.date_update >= now() - make_interval(secs => get_setting(d.service_id, 'position_ttl_s')::int)"
                + " WHERE d.deal_id = ?", dealId);
        }));
    }

    // ---------------------------------------------------------------- закрытие кодом (п. 5.1)

    /** Секрет, длина и время жизни кода сделки; проверка статуса. */
    private record CodeParams(String secret, int length, int ttl, String method) {
    }

    private static CodeParams codeParams(Connection c, long dealId) throws SQLException {
        String[] v = Db.rows(c,
            "SELECT d.close_secret, get_setting(d.service_id, 'close_code_len'),"
            + " CASE get_setting(d.service_id, 'close_method') WHEN 'KEY' THEN get_setting(d.service_id, 'close_key_ttl_s')"
            + "      ELSE get_setting(d.service_id, 'close_qr_ttl_s') END, get_setting(d.service_id, 'close_method'),"
            + " st.dict_code"
            + " FROM deal d JOIN dictionary st ON st.dict_id = d.deal_status_id WHERE d.deal_id = ?", dealId).get(0);
        if (!"AGREED".equals(v[4]) && !"CONTACT".equals(v[4])) {
            throw new ApiException(409, "DEAL_NOT_CLOSABLE", "Закрыть можно только согласованную сделку");
        }
        return new CodeParams(v[0], Integer.parseInt(v[1]), Integer.parseInt(v[2]), v[3]);
    }

    private static String codeJson(long dealId, CodeParams p) {
        long now = System.currentTimeMillis() / 1000;
        long interval = CloseCode.interval(now, p.ttl());
        String code = CloseCode.code(p.secret(), dealId, interval, p.length());
        return "{\"method\":\"" + p.method() + "\",\"code\":\"" + code + "\",\"qr_payload\":\"mpclose:" + dealId + ":" + code
            + "\",\"ttl_s\":" + p.ttl() + ",\"expires_in_s\":" + ((interval + 1) * p.ttl() - now) + "}";
    }

    /** POST /api/deals/{id}/close/request - сторона нажала "Закрыть сделку"; поставщику открывается код. */
    Response closeRequest(Request r, Matcher m) throws SQLException {
        long me = r.agent();
        long dealId = Long.parseLong(m.group(1));
        db.tx(me, c -> {
            String role = roleIn(c, dealId, me);
            codeParams(c, dealId);
            if (role.equals("client")) {
                Db.cell(c, "SELECT notify_agent(provider_agent_id, deal_id, 'CLOSE_REQUEST') FROM deal WHERE deal_id = ?", dealId);
            } else if (!role.equals("provider")) {
                throw new ApiException(403, "FORBIDDEN", "Закрыть может только сторона сделки");
            }
            return null;
        });
        return Response.ok("{\"status\":\"ok\"}");
    }

    /** GET /api/deals/{id}/close/code - действующий код; только поставщику сделки. */
    Response closeCode(Request r, Matcher m) throws SQLException {
        long me = r.agent();
        long dealId = Long.parseLong(m.group(1));
        return Response.ok(db.tx(me, c -> {
            if (!"provider".equals(roleIn(c, dealId, me))) {
                throw new ApiException(403, "FORBIDDEN", "Код показывает только поставщик сделки");
            }
            return codeJson(dealId, codeParams(c, dealId));
        }));
    }

    /** POST /api/deals/{id}/close {code} - клиент подтверждает код; CLOSED (CONTACT проверяет БД). */
    Response close(Request r, Matcher m) throws SQLException {
        long me = r.agent();
        long dealId = Long.parseLong(m.group(1));
        db.tx(me, c -> {
            if (!"client".equals(roleIn(c, dealId, me))) {
                throw new ApiException(403, "FORBIDDEN", "Код подтверждает только клиент сделки");
            }
            CodeParams p = codeParams(c, dealId);
            String code = Db.cell(c, "SELECT ?::jsonb ->> 'code'", r.body);
            if (code != null && code.startsWith("mpclose:")) {
                String[] parts = code.split(":");
                if (parts.length != 3 || !parts[1].equals(Long.toString(dealId))) {
                    throw new ApiException(422, "CODE_INVALID", "Код от другой сделки");
                }
                code = parts[2];
            }
            int check = code == null ? -1 : CloseCode.check(p.secret(), dealId, code.trim(), p.ttl(), p.length(),
                                                            System.currentTimeMillis() / 1000);
            if (check == 1) {
                throw new ApiException(422, "CODE_EXPIRED", "Код устарел - попросите показать новый");
            }
            if (check != 0) {
                throw new ApiException(422, "CODE_INVALID", "Неверный код");
            }
            Db.update(c, "UPDATE deal SET deal_status_id = dict_id('DEAL_STATUS','CLOSED') WHERE deal_id = ?", dealId);
            return null;
        });
        return Response.ok(dealJson(me, dealId, r.lang));
    }

    /** POST /api/deals/{id}/close/test-scan - тестовый клиент: сервер подставляет действующий код. */
    Response testScan(Request r, Matcher m) throws SQLException {
        long me = r.agent();
        long dealId = Long.parseLong(m.group(1));
        db.tx(me, c -> {
            requireTesting(c, me);
            if (!"client".equals(roleIn(c, dealId, me))) {
                throw new ApiException(403, "FORBIDDEN", "Сканирует клиент сделки");
            }
            codeParams(c, dealId);
            Db.update(c, "UPDATE deal SET deal_status_id = dict_id('DEAL_STATUS','CLOSED') WHERE deal_id = ?", dealId);
            return null;
        });
        return Response.ok(dealJson(me, dealId, r.lang));
    }

    /** POST /api/deals/{id}/close/test-provide - тестовый поставщик: код уходит клиенту уведомлением CLOSE_CODE. */
    Response testProvide(Request r, Matcher m) throws SQLException {
        long me = r.agent();
        long dealId = Long.parseLong(m.group(1));
        return Response.ok(db.tx(me, c -> {
            requireTesting(c, me);
            if (!"provider".equals(roleIn(c, dealId, me))) {
                throw new ApiException(403, "FORBIDDEN", "Код предоставляет поставщик сделки");
            }
            String code = codeJson(dealId, codeParams(c, dealId));
            Db.cell(c, "SELECT notify_agent(client_agent_id, deal_id, 'CLOSE_CODE', ?::jsonb) FROM deal WHERE deal_id = ?",
                    code, dealId);
            return code;
        }));
    }

    private static void requireTesting(Connection c, long me) throws SQLException {
        if (!"1".equals(Db.cell(c, "SELECT is_testing FROM agent WHERE agent_id = ?", me))) {
            throw new ApiException(403, "NOT_TESTING", "Доступно только в режиме тестирования");
        }
    }

    // ---------------------------------------------------------------- отмена и баллы

    /** POST /api/deals/{id}/cancel {cancel_reason_id?, comment?} - итоговый статус решает БД (deal_cancel). */
    Response cancel(Request r, Matcher m) throws SQLException {
        long me = r.agent();
        long dealId = Long.parseLong(m.group(1));
        String status = db.tx(me, c -> Db.cell(c,
            "SELECT deal_cancel(?, (b->>'cancel_reason_id')::numeric, b->>'comment') FROM (SELECT ?::jsonb b) x",
            dealId, r.body));
        return Response.ok("{\"status\":\"" + status + "\",\"deal\":" + dealJson(me, dealId, r.lang) + "}");
    }

    /** POST /api/deals/{id}/review {score, comment?} - баллы другой стороне (проверяет триггер). */
    Response review(Request r, Matcher m) throws SQLException {
        long me = r.agent();
        long dealId = Long.parseLong(m.group(1));
        db.tx(me, c -> Db.update(c,
            "INSERT INTO deal_review (deal_id, from_agent_id, to_agent_id, to_role_id, score, comment)"
            + " SELECT d.deal_id, ?, CASE WHEN d.client_agent_id = ? THEN d.provider_agent_id ELSE d.client_agent_id END,"
            + "        dict_id('ROLE','CLIENT'), (b->>'score')::numeric, b->>'comment'"
            + " FROM deal d, (SELECT ?::jsonb b) x WHERE d.deal_id = ?", me, me, r.body, dealId));
        return Response.ok(dealJson(me, dealId, r.lang));
    }

    /** GET /api/me/pending-reviews */
    Response pendingReviews(Request r, Matcher m) throws SQLException {
        r.query.put("role", "all");
        r.query.put("status", "pending_reviews");
        long me = r.agent();
        return Response.ok(db.tx(me, c -> Db.cell(c,
            "SELECT coalesce(json_agg(json_build_object('deal_id', d.deal_id, 'counterpart',"
            + "  agent_pseudonym(CASE WHEN d.client_agent_id = ? THEN d.provider_agent_id ELSE d.client_agent_id END))"
            + "  ORDER BY d.deal_id), '[]')"
            + " FROM deal d JOIN dictionary s ON s.dict_id = d.deal_status_id"
            + " WHERE d.provider_agent_id IS NOT NULL AND ? IN (d.client_agent_id, d.provider_agent_id)"
            + "   AND (s.dict_code = 'CLOSED' OR (s.dict_code = 'CANCEL_CLIENT' AND d.provider_agent_id = ?)"
            + "        OR (s.dict_code = 'CANCEL_PROVIDER' AND d.client_agent_id = ?))"
            + "   AND NOT EXISTS (SELECT 1 FROM deal_review r WHERE r.deal_id = d.deal_id AND r.from_agent_id = ?)",
            me, me, me, me, me)));
    }

    // ---------------------------------------------------------------- уведомления

    /** GET /api/notifications?since=<id> */
    Response notifications(Request r, Matcher m) throws SQLException {
        long me = r.agent();
        long since = Long.parseLong(r.param("since", "0"));
        return Response.ok(db.tx(me, c -> Db.cell(c,
            "SELECT coalesce(json_agg(x ORDER BY x.notification_id), '[]') FROM ("
            + " SELECT n.notification_id, n.deal_id, t.dict_code AS type,"
            + "        coalesce(tl.text_value, tr.text_value) AS text, n.payload, n.date_creation, n.date_read"
            + " FROM notification n"
            + " JOIN dictionary t ON t.dict_id = n.notify_type_id"
            + " JOIN glossary_text tr ON tr.glossary_id = t.glossary_id AND tr.interface_lang_id = 1"
            + " LEFT JOIN glossary_text tl ON tl.glossary_id = t.glossary_id AND tl.interface_lang_id = ?"
            + " WHERE n.agent_id = ? AND n.notification_id > ?"
            + " ORDER BY n.notification_id LIMIT 200) x", r.lang, me, since)));
    }

    /** POST /api/notifications/{id}/read */
    Response read(Request r, Matcher m) throws SQLException {
        long me = r.agent();
        db.tx(me, c -> Db.update(c,
            "UPDATE notification SET date_read = coalesce(date_read, now()) WHERE notification_id = ? AND agent_id = ?",
            Long.parseLong(m.group(1)), me));
        return Response.ok("{\"status\":\"ok\"}");
    }
}
