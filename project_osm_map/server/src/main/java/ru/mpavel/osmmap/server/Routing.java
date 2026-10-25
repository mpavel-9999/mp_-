package ru.mpavel.osmmap.server;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.ResponsePath;
import com.graphhopper.config.Profile;
import com.graphhopper.util.GHUtility;
import com.graphhopper.util.PointList;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Маршруты по дорогам OSM (GraphHopper, профиль car): длина и линия маршрута, проверка "по пути" для такси (В-8).
 * Граф строится из server/data/kazakhstan-latest.osm.pbf при первом запуске (несколько минут), дальше читается из кэша.
 * Пока граф не готов или файла нет - методы маршрутизации отвечают ROUTING_UNAVAILABLE, остальной API работает.
 */
final class Routing {

    /** Маршрут: длина в метрах и точки [lat, lon]. */
    record Route(double lengthM, List<double[]> points) {
    }

    /** Проекция точки на линию: расстояние до линии и путь от начала линии до проекции, м. */
    record Projection(double distanceM, double alongM) {
    }

    private volatile GraphHopper hopper;
    private volatile String state = "не запущен";

    /** Импорт или загрузка графа в фоне. */
    void start(File pbf, File cache) {
        if (!pbf.isFile()) {
            state = "нет файла " + pbf;
            System.err.println("Маршруты недоступны: " + state);
            return;
        }
        Thread t = new Thread(() -> {
            try {
                state = "строится граф дорог";
                long t0 = System.currentTimeMillis();
                GraphHopper gh = new GraphHopper();
                gh.setOSMFile(pbf.getPath());
                gh.setGraphHopperLocation(cache.getPath());
                gh.setEncodedValuesString("car_access, car_average_speed");
                gh.setProfiles(new Profile("car").setCustomModel(GHUtility.loadCustomModelFromJar("car.json")));
                gh.importOrLoad();
                hopper = gh;
                state = "готов";
                System.out.println("Граф дорог готов за " + (System.currentTimeMillis() - t0) / 1000 + " с");
            } catch (Exception e) {
                state = "ошибка: " + e.getMessage();
                System.err.println("Маршруты недоступны: " + state);
            }
        }, "routing-import");
        t.setDaemon(true);
        t.start();
    }

    String state() {
        return state;
    }

    Route route(double lat1, double lon1, double lat2, double lon2) {
        GraphHopper gh = hopper;
        if (gh == null) {
            throw new Http.ApiException(503, "ROUTING_UNAVAILABLE", "Маршруты недоступны: " + state);
        }
        GHResponse rsp = gh.route(new GHRequest(lat1, lon1, lat2, lon2).setProfile("car"));
        if (rsp.hasErrors()) {
            throw new Http.ApiException(422, "NO_ROUTE", "Маршрут не построен: " + rsp.getErrors().get(0).getMessage());
        }
        ResponsePath path = rsp.getBest();
        PointList pl = path.getPoints();
        List<double[]> pts = new ArrayList<>(pl.size());
        for (int i = 0; i < pl.size(); i++) {
            pts.add(new double[]{pl.getLat(i), pl.getLon(i)});
        }
        return new Route(path.getDistance(), pts);
    }

    /**
     * Запрос такси по пути водителя (п. 2.1.1): подача - в коридоре маршрута ±rA; назначение - в коридоре ±rA
     * или в радиусе rB вокруг Б; подача раньше назначения по ходу движения. Без точки Б - подача в радиусе rA вокруг А.
     */
    static boolean onTheWay(List<double[]> line, double aLat, double aLon, Double bLat, Double bLon,
                            double rA, double rB, double pLat, double pLon, double dLat, double dLon) {
        if (bLat == null || line == null || line.size() < 2) {
            return distance(aLat, aLon, pLat, pLon) <= rA;
        }
        Projection p = project(line, pLat, pLon);
        if (p.distanceM() > rA) {
            return false;
        }
        Projection d = project(line, dLat, dLon);
        double dAlong;
        if (d.distanceM() <= rA) {
            dAlong = d.alongM();
        } else if (distance(bLat, bLon, dLat, dLon) <= rB) {
            dAlong = length(line);
        } else {
            return false;
        }
        return p.alongM() < dAlong;
    }

    static Projection project(List<double[]> line, double lat, double lon) {
        double best = Double.MAX_VALUE;
        double bestAlong = 0;
        double along = 0;
        for (int i = 0; i + 1 < line.size(); i++) {
            double[] a = line.get(i);
            double[] b = line.get(i + 1);
            // локальная плоская проекция в метрах вокруг точки a
            double kx = 111320 * Math.cos(Math.toRadians(a[0]));
            double ky = 110540;
            double bx = (b[1] - a[1]) * kx;
            double by = (b[0] - a[0]) * ky;
            double px = (lon - a[1]) * kx;
            double py = (lat - a[0]) * ky;
            double seg2 = bx * bx + by * by;
            double t = seg2 == 0 ? 0 : Math.max(0, Math.min(1, (px * bx + py * by) / seg2));
            double dx = px - t * bx;
            double dy = py - t * by;
            double dist = Math.sqrt(dx * dx + dy * dy);
            double segLen = Math.sqrt(seg2);
            if (dist < best) {
                best = dist;
                bestAlong = along + t * segLen;
            }
            along += segLen;
        }
        return new Projection(best, bestAlong);
    }

    static double length(List<double[]> line) {
        double s = 0;
        for (int i = 0; i + 1 < line.size(); i++) {
            s += distance(line.get(i)[0], line.get(i)[1], line.get(i + 1)[0], line.get(i + 1)[1]);
        }
        return s;
    }

    /** Расстояние по прямой (гаверсинус), м - как geo_distance_m в БД. */
    static double distance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double h = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.pow(Math.sin(dLon / 2), 2);
        return 2 * 6371000 * Math.asin(Math.sqrt(h));
    }

    /** Линия в формате encoded polyline (точность 1e-5), как в Google и OSRM. */
    static String encode(List<double[]> points) {
        StringBuilder b = new StringBuilder();
        long lastLat = 0;
        long lastLon = 0;
        for (double[] p : points) {
            long lat = Math.round(p[0] * 1e5);
            long lon = Math.round(p[1] * 1e5);
            encodeValue(lat - lastLat, b);
            encodeValue(lon - lastLon, b);
            lastLat = lat;
            lastLon = lon;
        }
        return b.toString();
    }

    static List<double[]> decode(String encoded) {
        List<double[]> pts = new ArrayList<>();
        if (encoded == null || encoded.isEmpty()) {
            return pts;
        }
        int i = 0;
        long lat = 0;
        long lon = 0;
        while (i < encoded.length()) {
            long[] r = decodeValue(encoded, i);
            lat += r[0];
            r = decodeValue(encoded, (int) r[1]);
            lon += r[0];
            i = (int) r[1];
            pts.add(new double[]{lat / 1e5, lon / 1e5});
        }
        return pts;
    }

    private static void encodeValue(long v, StringBuilder b) {
        v = v < 0 ? ~(v << 1) : (v << 1);
        while (v >= 0x20) {
            b.append((char) ((0x20 | (v & 0x1f)) + 63));
            v >>= 5;
        }
        b.append((char) (v + 63));
    }

    private static long[] decodeValue(String s, int i) {
        long result = 0;
        int shift = 0;
        int ch;
        do {
            ch = s.charAt(i++) - 63;
            result |= (long) (ch & 0x1f) << shift;
            shift += 5;
        } while (ch >= 0x20);
        long value = (result & 1) != 0 ? ~(result >> 1) : (result >> 1);
        return new long[]{value, i};
    }
}
