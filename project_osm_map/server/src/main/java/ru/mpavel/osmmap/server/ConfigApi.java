package ru.mpavel.osmmap.server;

import ru.mpavel.osmmap.server.Http.ApiException;
import ru.mpavel.osmmap.server.Http.Request;
import ru.mpavel.osmmap.server.Http.Response;

import java.sql.SQLException;
import java.util.regex.Matcher;

/** Вход, профиль, конфигурация интерфейса, справочники и поиск услуг (ТЗ 4.1-4.3). */
final class ConfigApi {

    private final Db db;
    private final Auth auth;
    private final boolean devLogin;

    ConfigApi(Db db, Auth auth, boolean devLogin) {
        this.db = db;
        this.auth = auth;
        this.devLogin = devLogin;
    }

    // ---------------------------------------------------------------- вход

    /** POST /api/session {login, password} или, при MP_DEV_LOGIN=1, {agent_id}. */
    Response login(Request r, Matcher m) throws SQLException {
        Long agentId = db.tx(null, c -> {
            String[] row = Db.rows(c,
                "SELECT agent_login_check(b->>'login', b->>'password'), (b->>'agent_id')::numeric"
                + " FROM (SELECT ?::jsonb b) x", r.body).get(0);
            if (row[0] != null) {
                return Long.valueOf(row[0]);
            }
            if (devLogin && row[1] != null) {
                return Db.cellLong(c, "SELECT agent_id FROM agent WHERE agent_id = ? AND is_active = 1",
                                   Long.valueOf(row[1]));
            }
            return null;
        });
        if (agentId == null) {
            throw new ApiException(401, "LOGIN_FAILED", "Неверный логин или пароль");
        }
        String token = auth.open(agentId);
        r.agentId = agentId;
        return Response.ok("{\"token\":\"" + token + "\",\"agent\":" + meJson(agentId, r.lang) + "}");
    }

    /** DELETE /api/session */
    Response logout(Request r, Matcher m) {
        auth.close(r.header("Authorization"));
        return Response.ok("{\"status\":\"ok\"}");
    }

    /** GET /api/dev/agents - только при MP_DEV_LOGIN=1: список агентов для входа без пароля. */
    Response devAgents(Request r, Matcher m) throws SQLException {
        if (!devLogin) {
            throw new ApiException(404, "NOT_FOUND", "Вход без пароля выключен");
        }
        return Response.ok(db.tx(null, c -> Db.cell(c,
            "SELECT coalesce(json_agg(x ORDER BY x.agent_id), '[]') FROM ("
            + " SELECT a.agent_id, agent_pseudonym(a.agent_id) AS pseudonym, a.is_testing = 1 AS is_testing,"
            + "        (SELECT json_agg(d.dict_code ORDER BY d.sort_order) FROM agent_role ar"
            + "         JOIN dictionary d ON d.dict_id = ar.role_id WHERE ar.agent_id = a.agent_id AND ar.is_active = 1) AS roles,"
            + "        (SELECT login FROM agent_login l WHERE l.agent_id = a.agent_id) AS login"
            + " FROM agent a WHERE a.is_active = 1) x")));
    }

    // ---------------------------------------------------------------- профиль

    /** GET /api/me */
    Response me(Request r, Matcher m) throws SQLException {
        return Response.ok(meJson(r.agent(), r.lang));
    }

    String meJson(long agentId, int lang) throws SQLException {
        return db.tx(agentId, c -> Db.cell(c,
            "SELECT json_build_object("
            + " 'agent_id', a.agent_id, 'pseudonym', agent_pseudonym(a.agent_id),"
            + " 'name', (SELECT aa.attribute_value FROM agent_attribute aa WHERE aa.agent_id = a.agent_id"
            + "          AND aa.attribute_id = 2 AND aa.is_active = 1 ORDER BY aa.date_start DESC LIMIT 1),"
            + " 'is_testing', a.is_testing = 1, 'lang', l.language_code,"
            + " 'roles', (SELECT json_agg(d.dict_code ORDER BY d.sort_order) FROM agent_role ar"
            + "           JOIN dictionary d ON d.dict_id = ar.role_id WHERE ar.agent_id = a.agent_id AND ar.is_active = 1),"
            + " 'pending_reviews', agent_pending_reviews(a.agent_id),"
            + " 'services', (SELECT coalesce(json_agg(json_build_object("
            + "        'service_id', s.service_id, 'name', gt.text_value, 'node_type', sv.node_type,"
            + "        'price_unit', get_setting(s.service_id, 'price_unit'), 'geo_mode', get_setting(s.service_id, 'geo_mode'),"
            + "        'lat', s.geo_lat, 'lon', s.geo_lon, 'zone_radius_m', s.zone_radius_m,"
            + "        'work_time_from', s.work_time_from, 'work_time_to', s.work_time_to,"
            + "        'price_min', s.price_min, 'price_max', s.price_max, 'unit_price_min', s.unit_price_min,"
            + "        'capacity', s.capacity, 'client_rating_min', s.client_rating_min, 'is_auto', s.is_auto = 1,"
            + "        'rating', agent_rating(a.agent_id, 'PROVIDER', s.service_id),"
            + "        'newbie', NOT agent_has_rating(a.agent_id, 'PROVIDER', s.service_id))"
            + "        ORDER BY s.service_id), '[]')"
            + "     FROM agent_service s JOIN service sv ON sv.service_id = s.service_id"
            + "     CROSS JOIN LATERAL (SELECT gloss(sv.glossary_id, ?::numeric) AS text_value) gt"
            + "     WHERE s.agent_id = a.agent_id AND s.is_active = 1 AND sv.node_type <> 'system'))"
            + " FROM agent a JOIN interface_lang l ON l.interface_lang_id = a.interface_lang_id"
            + " WHERE a.agent_id = ?", lang, agentId));
    }

    // ---------------------------------------------------------------- конфигурация интерфейса

    private static final String SQL_CONFIG =
        "WITH cfg AS (SELECT coalesce((SELECT config_json FROM ui_config WHERE agent_id = ?),"
        + "  '{\"lang\":\"RUS\",\"template\":\"A\",\"scheme\":\"A_LIGHT\",\"map\":\"OSM\",\"version\":7}'::jsonb) AS c),"
        + " lang AS (SELECT coalesce((SELECT interface_lang_id FROM interface_lang, cfg WHERE language_code = c->>'lang'), 1) AS id),"
        + " tx AS ("  // текст термина на языке, без перевода - RUS
        + "  SELECT g.glossary_id, g.glossary_code, coalesce(t.text_value, r.text_value) AS txt"
        + "  FROM glossary g JOIN glossary_text r ON r.glossary_id = g.glossary_id AND r.interface_lang_id = 1"
        + "  LEFT JOIN glossary_text t ON t.glossary_id = g.glossary_id AND t.interface_lang_id = (SELECT id FROM lang)),"
        + " p AS (SELECT jsonb_build_object("
        + "  'config', c,"
        + "  'template', (SELECT value_json FROM dictionary WHERE dict_type = 'UI_TEMPLATE' AND dict_code = c->>'template'),"
        + "  'scheme', (SELECT value_json FROM dictionary WHERE dict_type = 'UI_SCHEME' AND dict_code = c->>'scheme'),"
        + "  'texts', (SELECT jsonb_object_agg(glossary_code, txt) FROM tx WHERE glossary_code LIKE 'ui.%'),"
        + "  'notify_texts', (SELECT jsonb_object_agg(d.dict_code, tx.txt) FROM dictionary d JOIN tx USING (glossary_id)"
        + "                   WHERE d.dict_type = 'NOTIFY_TYPE'),"
        + "  'statuses', (SELECT jsonb_object_agg(d.dict_code, tx.txt) FROM dictionary d JOIN tx USING (glossary_id)"
        + "               WHERE d.dict_type = 'DEAL_STATUS'),"
        + "  'cancel_reasons', (SELECT jsonb_agg(jsonb_build_object('id', d.dict_id, 'code', d.dict_code, 'text', tx.txt)"
        + "                     ORDER BY d.sort_order) FROM dictionary d JOIN tx USING (glossary_id)"
        + "                     WHERE d.dict_type = 'CANCEL_REASON' AND d.is_active = 1),"
        + "  'icons', (SELECT jsonb_agg(jsonb_build_object('icon_id', icon_id, 'code', icon_code, 'svg', icon_svg)"
        + "            ORDER BY icon_id) FROM icon WHERE is_active = 1),"
        + "  'cursors', (SELECT jsonb_object_agg(cursor_code, icon_svg) FROM v_map_cursor),"
        + "  'base_layer', (SELECT jsonb_build_object('code', d.dict_code, 'url', d.value_text, 'version', d.value_num,"
        + "                 'attribution', tx.txt) FROM dictionary d JOIN tx USING (glossary_id)"
        + "                 WHERE d.dict_type = 'MAP_BASE_LAYER' AND d.dict_code = coalesce(c->>'map', 'OSM'))"
        + " ) AS j FROM cfg)"
        + " SELECT md5(j::text), j::text FROM p";

    /** GET /api/config?checksum= - 304, если конфигурация не изменилась (F-10). */
    Response config(Request r, Matcher m) throws SQLException {
        long me = r.agent();
        String[] row = db.tx(me, c -> Db.rows(c, SQL_CONFIG, me).get(0));
        if (row[0].equals(r.param("checksum", ""))) {
            return new Response(304, "");
        }
        return Response.ok("{\"checksum\":\"" + row[0] + "\"," + row[1].substring(1));
    }

    /** PUT /api/config {template, scheme, lang} - применяется сразу. */
    Response saveConfig(Request r, Matcher m) throws SQLException {
        long me = r.agent();
        db.tx(me, c -> {
            String[] v = Db.rows(c,
                "WITH b AS (SELECT ?::jsonb AS b),"
                + " old AS (SELECT coalesce((SELECT config_json FROM ui_config WHERE agent_id = ?),"
                + "   '{\"lang\":\"RUS\",\"template\":\"A\",\"scheme\":\"A_LIGHT\",\"map\":\"OSM\",\"version\":7}'::jsonb) AS o),"
                + " n AS (SELECT o || jsonb_strip_nulls(jsonb_build_object('template', b->>'template',"
                + "        'scheme', b->>'scheme', 'lang', b->>'lang')) AS cfg FROM old, b)"
                + " SELECT cfg::text,"
                + "   EXISTS (SELECT 1 FROM dictionary s JOIN dictionary t ON t.dict_id = s.parent_dict_id"
                + "           WHERE s.dict_type = 'UI_SCHEME' AND s.dict_code = cfg->>'scheme' AND t.dict_code = cfg->>'template'),"
                + "   (SELECT interface_lang_id FROM interface_lang WHERE language_code = cfg->>'lang')"
                + " FROM n", r.body, me).get(0);
            if (!"t".equals(v[1])) {
                throw new ApiException(400, "BAD_SCHEME", "Цветовая схема не относится к выбранному шаблону");
            }
            if (v[2] == null) {
                throw new ApiException(400, "BAD_LANG", "Нет такого языка интерфейса");
            }
            Db.update(c, "INSERT INTO ui_config (agent_id, config_json, checksum) VALUES (?, ?::jsonb, md5(?::jsonb::text))"
                + " ON CONFLICT (agent_id) DO UPDATE SET config_json = excluded.config_json, checksum = excluded.checksum",
                me, v[0], v[0]);
            Db.update(c, "UPDATE agent SET interface_lang_id = ? WHERE agent_id = ?", Long.valueOf(v[2]), me);
            return null;
        });
        r.query.remove("checksum");
        return config(r, m);
    }

    /** GET /api/ui/options - варианты "Шаблон / Схема" и языки. */
    Response uiOptions(Request r, Matcher m) throws SQLException {
        return Response.ok(db.tx(r.agentId, c -> Db.cell(c,
            "SELECT json_build_object("
            + " 'options', (SELECT json_agg(json_build_object('template', template_code, 'scheme', scheme_code,"
            + "              'label', option_label, 'is_dark', is_dark) ORDER BY sort_order)"
            + "             FROM v_ui_options WHERE interface_lang_id = ?),"
            + " 'languages', (SELECT json_agg(json_build_object('code', language_code, 'name', language_name)"
            + "               ORDER BY interface_lang_id) FROM interface_lang WHERE is_active = 1))", r.lang)));
    }

    // ---------------------------------------------------------------- справочники и услуги

    /** GET /api/services?q= - поиск по названию, пути и синонимам. */
    Response services(Request r, Matcher m) throws SQLException {
        String q = r.param("q", "");
        return Response.ok(db.tx(r.agentId, c -> Db.cell(c,
            "SELECT coalesce(json_agg(s ORDER BY s.service_path), '[]') FROM ("
            + " SELECT v.service_id, v.node_type, v.parent_service_id, v.service_name, v.service_path, v.icon_id,"
            + "        i.icon_code, get_setting(v.service_id, 'geo_mode') AS geo_mode,"
            + "        get_setting(v.service_id, 'price_unit') AS price_unit"
            + " FROM v_service_search v LEFT JOIN icon i ON i.icon_id = v.icon_id"
            + " WHERE v.interface_lang_id = ? AND (? = '' OR v.search_text LIKE '%' || lower(?) || '%')) s",
            r.lang, q, q)));
    }

    /** GET /api/services/{id}/form?scope=request|offer|profile - поля с наследованием, типами, списками. */
    Response form(Request r, Matcher m) throws SQLException {
        long serviceId = Long.parseLong(m.group(1));
        String scope = r.param("scope", "request");
        return Response.ok(db.tx(r.agentId, c -> Db.cell(c,
            "SELECT coalesce(json_agg(f ORDER BY f.sort_order, f.attribute_id), '[]') FROM ("
            + " SELECT e.attribute_id, l.attribute_type AS type, l.label, l.icon_code, e.is_required = 1 AS required,"
            + "        e.show_stage, e.sort_order,"
            + "        (SELECT json_agg(json_build_object('attribute_id', o.attribute_id, 'label', o.label) ORDER BY o.sort_order)"
            + "         FROM v_attribute_label o WHERE o.parent_attribute_id = e.attribute_id"
            + "           AND o.interface_lang_id = l.interface_lang_id) AS options"
            + " FROM v_service_attribute_effective e"
            + " JOIN v_attribute_label l ON l.attribute_id = e.attribute_id AND l.interface_lang_id = ?"
            + " WHERE e.service_id = ? AND e.attr_scope = ?) f", r.lang, serviceId, scope)));
    }

    /** GET /api/services/{id}/settings - эффективные настройки узла. */
    Response settings(Request r, Matcher m) throws SQLException {
        long serviceId = Long.parseLong(m.group(1));
        return Response.ok(db.tx(r.agentId, c -> Db.cell(c,
            "SELECT json_object_agg(code, get_setting(?, code)) FROM unnest(ARRAY["
            + " 'geo_mode','place_mode','price_unit','offer_sort_1','offer_sort_2','offer_sort_3','near_radius_m',"
            + " 'slot_required','max_parallel_deals','close_method','close_qr_ttl_s','close_key_ttl_s','close_code_len',"
            + " 'cancel_free_min','currency','forecast_window_min','forecast_grid_m']) code", serviceId)));
    }

    /** GET /api/map/base-layers */
    Response baseLayers(Request r, Matcher m) throws SQLException {
        return Response.ok(db.tx(r.agentId, c -> Db.cell(c,
            "SELECT coalesce(json_agg(b ORDER BY b.sort_order), '[]') FROM ("
            + " SELECT d.dict_code AS code, d.sort_order, d.value_num AS version, d.value_text AS url,"
            + "        gt.text_value AS attribution"
            + " FROM dictionary d CROSS JOIN LATERAL (SELECT gloss(d.glossary_id, ?::numeric) AS text_value) gt"
            + " WHERE d.dict_type = 'MAP_BASE_LAYER' AND d.is_active = 1) b", r.lang)));
    }

    /** GET /api/icons */
    Response icons(Request r, Matcher m) throws SQLException {
        return Response.ok(db.tx(r.agentId, c -> Db.cell(c,
            "SELECT coalesce(json_agg(i ORDER BY i.icon_id), '[]') FROM ("
            + " SELECT icon_id, icon_code, icon_svg FROM icon WHERE is_active = 1) i")));
    }

    /**
     * PUT /api/me/position {lat, lon, source, accuracy_m}; manual - только в режиме тестирования (триггер).
     * Сразу проверяет сближение сторон (В-Э6-1): в contact - сделки, перешедшие в CONTACT.
     */
    Response position(Request r, Matcher m) throws SQLException {
        long me = r.agent();
        String contact = db.tx(me, c -> {
            Db.update(c,
                "INSERT INTO agent_position (agent_id, geo_lat, geo_lon, source, accuracy_m)"
                + " SELECT ?, (b->>'lat')::numeric, (b->>'lon')::numeric, coalesce(b->>'source', 'gps'),"
                + "        (b->>'accuracy_m')::numeric FROM (SELECT ?::jsonb b) x"
                + " ON CONFLICT (agent_id) DO UPDATE SET geo_lat = excluded.geo_lat, geo_lon = excluded.geo_lon,"
                + "   source = excluded.source, accuracy_m = excluded.accuracy_m", me, r.body);
            // переход в CONTACT выполняет система: функция сама очищает и восстанавливает mp.agent_id
            return Db.cell(c, "SELECT to_json(deal_contact_check(?))", me);
        });
        return Response.ok("{\"status\":\"ok\",\"contact\":" + contact + "}");
    }
}
