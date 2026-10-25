package ru.mpavel.osmmap.server;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

/**
 * Динамический код закрытия сделки (п. 5.1.2 алгоритма): HMAC-SHA256(секрет сделки, "deal:интервал"),
 * 31 бит -> остаток от 10^длины. Интервал = unix_time / ttl; принимается текущий и предыдущий.
 */
final class CloseCode {

    private CloseCode() {
    }

    static long interval(long epochSeconds, int ttlSeconds) {
        return epochSeconds / ttlSeconds;
    }

    static String code(String secret, long dealId, long interval, int length) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] h = mac.doFinal((dealId + ":" + interval).getBytes(StandardCharsets.UTF_8));
            int offset = h[h.length - 1] & 0x0f;
            int bin = ((h[offset] & 0x7f) << 24) | ((h[offset + 1] & 0xff) << 16)
                    | ((h[offset + 2] & 0xff) << 8) | (h[offset + 3] & 0xff);
            int mod = (int) Math.pow(10, length);
            return String.format("%0" + length + "d", bin % mod);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    /** 0 - код действует, 1 - устарел (до 10 интервалов назад), -1 - неверный. */
    static int check(String secret, long dealId, String code, int ttlSeconds, int length, long epochSeconds) {
        long now = interval(epochSeconds, ttlSeconds);
        if (code.equals(code(secret, dealId, now, length)) || code.equals(code(secret, dealId, now - 1, length))) {
            return 0;
        }
        for (long i = now - 2; i >= now - 10; i--) {
            if (code.equals(code(secret, dealId, i, length))) {
                return 1;
            }
        }
        return -1;
    }
}
