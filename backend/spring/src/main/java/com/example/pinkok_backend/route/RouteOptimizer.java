package com.example.pinkok_backend.route;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 동선 최적화 알고리즘 (LLM 아님 - 같은 입력이면 항상 같은 결과, 비용 없음).
 *
 * <p>1) 모든 핀을 지나는 가장 짧은 "열린 경로"를 찾는다 (각 핀을 시작점으로 최근접 이웃 + 2-opt 개선, 가장 짧은 것 채택).
 * 2) 그 경로를 날짜 수만큼 연속 구간으로 자른다 → 같은 날 가는 곳끼리 가깝게 모인다.
 */
public final class RouteOptimizer {

    /** 직선거리 → 실제 도로거리 보정 계수 */
    static final double ROAD_FACTOR = 1.3;
    /** 이 거리(m) 이하면 걸어서, 넘으면 차로 이동한다고 본다 */
    static final int WALK_MAX_M = 1000;
    private static final double WALK_M_PER_MIN = 75;    // 4.5km/h
    private static final double CAR_M_PER_MIN = 667;    // 40km/h (시내·국도 평균)
    /** 핀이 이만큼 이하일 때만 모든 시작점을 시도한다 */
    private static final int ALL_STARTS_LIMIT = 40;

    private RouteOptimizer() {
    }

    public record Point(long id, double lat, double lng) {
    }

    public static double haversineM(Point a, Point b) {
        double r = 6_371_000;
        double dLat = Math.toRadians(b.lat() - a.lat());
        double dLng = Math.toRadians(b.lng() - a.lng());
        double h = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(a.lat())) * Math.cos(Math.toRadians(b.lat()))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * r * Math.asin(Math.sqrt(h));
    }

    /** 두 지점 사이 예상 이동거리(m) */
    public static int roadDistanceM(Point a, Point b) {
        return (int) Math.round(haversineM(a, b) * ROAD_FACTOR);
    }

    public static String transportModeFor(int distanceM) {
        return distanceM <= WALK_MAX_M ? "WALK" : "CAR";
    }

    public static int durationMin(int distanceM, String mode) {
        double speed = "WALK".equals(mode) ? WALK_M_PER_MIN : CAR_M_PER_MIN;
        return (int) Math.max(1, Math.round(distanceM / speed));
    }

    /** 주어진 순서대로 이동할 때의 총 예상거리(m). */
    public static int pathLengthM(List<Point> path) {
        int sum = 0;
        for (int i = 1; i < path.size(); i++) {
            sum += roadDistanceM(path.get(i - 1), path.get(i));
        }
        return sum;
    }

    /** 모든 점을 한 번씩 지나는 짧은 열린 경로. 입력 순서와 무관하게 결정적(id 순으로 정렬해서 시작). */
    public static List<Point> shortestPath(List<Point> points) {
        if (points.size() <= 2) {
            return new ArrayList<>(points);
        }
        List<Point> sorted = new ArrayList<>(points);
        sorted.sort((a, b) -> Long.compare(a.id(), b.id()));

        List<Point> best = null;
        double bestLen = Double.MAX_VALUE;
        int starts = sorted.size() <= ALL_STARTS_LIMIT ? sorted.size() : 1;
        for (int s = 0; s < starts; s++) {
            List<Point> path = twoOpt(nearestNeighbor(sorted, s));
            double len = length(path);
            if (len < bestLen - 1e-9) {
                bestLen = len;
                best = path;
            }
        }
        return best;
    }

    /** 경로를 days 개의 연속 구간으로 자른다. 크기는 최대한 균등(앞 날짜가 하나씩 더 많음). 빈 날짜는 만들지 않는다. */
    public static List<List<Point>> splitIntoDays(List<Point> path, int days) {
        int d = Math.max(1, Math.min(days, path.size()));
        List<List<Point>> result = new ArrayList<>();
        int base = path.size() / d;
        int extra = path.size() % d;
        int from = 0;
        for (int i = 0; i < d; i++) {
            int size = base + (i < extra ? 1 : 0);
            result.add(new ArrayList<>(path.subList(from, from + size)));
            from += size;
        }
        return result;
    }

    private static List<Point> nearestNeighbor(List<Point> sorted, int startIndex) {
        List<Point> remaining = new ArrayList<>(sorted);
        List<Point> path = new ArrayList<>();
        Point current = remaining.remove(startIndex);
        path.add(current);
        while (!remaining.isEmpty()) {
            int nearest = 0;
            double nearestDist = Double.MAX_VALUE;
            for (int i = 0; i < remaining.size(); i++) {
                double dist = haversineM(current, remaining.get(i));
                if (dist < nearestDist - 1e-9) {
                    nearestDist = dist;
                    nearest = i;
                }
            }
            current = remaining.remove(nearest);
            path.add(current);
        }
        return path;
    }

    /** 열린 경로용 2-opt: 구간을 뒤집어서 더 짧아지면 채택, 더 못 줄일 때까지 반복. */
    private static List<Point> twoOpt(List<Point> start) {
        List<Point> path = new ArrayList<>(start);
        int n = path.size();
        boolean improved = true;
        while (improved) {
            improved = false;
            for (int i = 0; i < n - 1; i++) {
                for (int j = i + 1; j < n; j++) {
                    double before = 0;
                    double after = 0;
                    if (i > 0) {
                        before += haversineM(path.get(i - 1), path.get(i));
                        after += haversineM(path.get(i - 1), path.get(j));
                    }
                    if (j < n - 1) {
                        before += haversineM(path.get(j), path.get(j + 1));
                        after += haversineM(path.get(i), path.get(j + 1));
                    }
                    if (after < before - 1e-6) {
                        Collections.reverse(path.subList(i, j + 1));
                        improved = true;
                    }
                }
            }
        }
        return path;
    }

    private static double length(List<Point> path) {
        double sum = 0;
        for (int i = 1; i < path.size(); i++) {
            sum += haversineM(path.get(i - 1), path.get(i));
        }
        return sum;
    }
}
