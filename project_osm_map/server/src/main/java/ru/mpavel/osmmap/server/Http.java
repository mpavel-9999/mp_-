package ru.mpavel.osmmap.server;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/** Запрос, ответ и ошибки API. Ошибка: {"error": "текст", "code": "КОД"}. */
final class Http {

    private Http() {
    }

    /** Ошибка, которую обработчик отдает клиенту как есть. */
    static final class ApiException extends RuntimeException {
        final int status;
        final String code;

        ApiException(int status, String code, String message) {
            super(message);
            this.status = status;
            this.code = code;
        }
    }

    /** Разобранный запрос: метод, путь, параметры, тело, агент сессии, язык. */
    static final class Request {
        final HttpExchange exchange;
        final String method;
        final String path;
        final Map<String, String> query;
        final String body;
        Long agentId;
        int lang = 1;

        Request(HttpExchange exchange) throws IOException {
            this.exchange = exchange;
            this.method = exchange.getRequestMethod();
            this.path = exchange.getRequestURI().getPath();
            this.query = parseQuery(exchange.getRequestURI().getRawQuery());
            try (InputStream in = exchange.getRequestBody()) {
                String text = new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
                this.body = text.isEmpty() ? "{}" : text;
            }
        }

        String header(String name) {
            return exchange.getRequestHeaders().getFirst(name);
        }

        String param(String name, String def) {
            String v = query.get(name);
            return v == null || v.isEmpty() ? def : v;
        }

        /** Агент сессии; без входа - 401. */
        long agent() {
            if (agentId == null) {
                throw new ApiException(401, "UNAUTHORIZED", "Нужен вход: заголовок Authorization: Bearer <токен>");
            }
            return agentId;
        }
    }

    /** Ответ обработчика: код и JSON. */
    record Response(int status, String json) {
        static Response ok(String json) {
            return new Response(200, json == null ? "null" : json);
        }
    }

    static Response error(int status, String code, String message) {
        return new Response(status, "{\"error\":\"" + escape(message) + "\",\"code\":\"" + escape(code) + "\"}");
    }

    static void send(HttpExchange ex, Response r) throws IOException {
        if (r.status() == 304) {
            ex.sendResponseHeaders(304, -1);
            ex.close();
            return;
        }
        byte[] bytes = r.json().getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(r.status(), bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    static String escape(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder b = new StringBuilder(s.length() + 8);
        for (char ch : s.toCharArray()) {
            switch (ch) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                case '\n' -> b.append("\\n");
                case '\r' -> b.append("\\r");
                case '\t' -> b.append("\\t");
                default -> {
                    if (ch < 0x20) {
                        b.append(String.format("\\u%04x", (int) ch));
                    } else {
                        b.append(ch);
                    }
                }
            }
        }
        return b.toString();
    }

    private static Map<String, String> parseQuery(String raw) {
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
}
