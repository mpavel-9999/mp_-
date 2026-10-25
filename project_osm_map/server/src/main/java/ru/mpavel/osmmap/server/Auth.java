package ru.mpavel.osmmap.server;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Сессии API: случайный токен 32 байта в памяти сервера, 12 часов.
 * Пароль проверяет БД (agent_login_check, хеш bcrypt); сервер пароли не хранит.
 */
final class Auth {

    private record Session(long agentId, Instant expires) {
    }

    private static final Duration TTL = Duration.ofHours(12);

    private final SecureRandom random = new SecureRandom();
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    String open(long agentId) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        sessions.put(token, new Session(agentId, Instant.now().plus(TTL)));
        return token;
    }

    /** Агент по заголовку Authorization: Bearer; null - нет действующей сессии. */
    Long agent(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        Session s = sessions.get(authorization.substring(7).trim());
        if (s == null || s.expires().isBefore(Instant.now())) {
            return null;
        }
        return s.agentId();
    }

    void close(String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            sessions.remove(authorization.substring(7).trim());
        }
    }

    void purgeExpired() {
        Instant now = Instant.now();
        sessions.values().removeIf(s -> s.expires().isBefore(now));
    }
}
