package com.example.pinkok_backend.route;

import com.example.pinkok_backend.route.RouteOptimizer.Point;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** 동선 최적화 알고리즘 순수 계산 테스트 (스프링 없음). */
class RouteOptimizerTest {

    /** 위도 한 줄로 늘어선 점들: 가장 짧은 경로는 순서대로 가는 것이다. */
    private static List<Point> line(int... ids) {
        List<Point> points = new ArrayList<>();
        for (int id : ids) {
            points.add(new Point(id, 33.0 + id * 0.01, 126.5));
        }
        return points;
    }

    @Test
    @DisplayName("뒤죽박죽 입력도 일직선으로 늘어선 점은 끝에서 끝으로 한 번에 지난다")
    void shortestPath_onALine_isMonotonic() {
        List<Point> path = RouteOptimizer.shortestPath(line(3, 1, 5, 2, 4));

        List<Long> ids = path.stream().map(Point::id).toList();
        assertTrue(ids.equals(List.of(1L, 2L, 3L, 4L, 5L)) || ids.equals(List.of(5L, 4L, 3L, 2L, 1L)), "경로: " + ids);
    }

    @Test
    @DisplayName("입력 순서가 달라도 결과 거리는 같다 (결정적)")
    void shortestPath_isDeterministicRegardlessOfInputOrder() {
        List<Point> points = new ArrayList<>(List.of(
                new Point(1, 33.46, 126.31), new Point(2, 33.46, 126.93), new Point(3, 33.47, 126.33),
                new Point(4, 33.45, 126.95), new Point(5, 33.50, 126.50), new Point(6, 33.465, 126.32)));
        List<Point> shuffled = new ArrayList<>(points);
        Collections.reverse(shuffled);

        assertEquals(
                RouteOptimizer.pathLengthM(RouteOptimizer.shortestPath(points)),
                RouteOptimizer.pathLengthM(RouteOptimizer.shortestPath(shuffled)));
        assertEquals(RouteOptimizer.shortestPath(points), RouteOptimizer.shortestPath(shuffled));
    }

    @Test
    @DisplayName("최적화한 경로는 입력 순서보다 길지 않다")
    void shortestPath_neverWorseThanInputOrder() {
        List<Point> zigzag = new ArrayList<>(List.of(
                new Point(1, 33.46, 126.31), new Point(2, 33.46, 126.93), new Point(3, 33.47, 126.33),
                new Point(4, 33.45, 126.95), new Point(5, 33.465, 126.32), new Point(6, 33.455, 126.94)));

        assertTrue(RouteOptimizer.pathLengthM(RouteOptimizer.shortestPath(zigzag)) < RouteOptimizer.pathLengthM(zigzag));
    }

    @Test
    @DisplayName("점이 0·1·2개여도 안전하다")
    void shortestPath_tinyInputs() {
        assertTrue(RouteOptimizer.shortestPath(List.of()).isEmpty());
        assertEquals(1, RouteOptimizer.shortestPath(line(1)).size());
        assertEquals(2, RouteOptimizer.shortestPath(line(2, 1)).size());
    }

    @Test
    @DisplayName("날짜 나누기 - 균등하게, 앞 날짜가 하나 더 많다")
    void splitIntoDays_balanced() {
        List<List<Point>> days = RouteOptimizer.splitIntoDays(line(1, 2, 3, 4, 5, 6, 7), 3);

        assertEquals(List.of(3, 2, 2), days.stream().map(List::size).toList());
    }

    @Test
    @DisplayName("날짜 나누기 - 핀보다 날짜가 많으면 빈 날짜를 만들지 않는다")
    void splitIntoDays_moreDaysThanPoints() {
        List<List<Point>> days = RouteOptimizer.splitIntoDays(line(1, 2), 5);

        assertEquals(2, days.size());
        assertTrue(days.stream().noneMatch(List::isEmpty));
    }

    @Test
    @DisplayName("이동수단·시간 추정 - 1km 이하는 도보, 넘으면 차")
    void transportAndDuration() {
        assertEquals("WALK", RouteOptimizer.transportModeFor(800));
        assertEquals("CAR", RouteOptimizer.transportModeFor(1500));
        assertEquals(11, RouteOptimizer.durationMin(800, "WALK"));
        assertEquals(15, RouteOptimizer.durationMin(10_000, "CAR"));
        assertEquals(1, RouteOptimizer.durationMin(10, "CAR"));
    }

    @Test
    @DisplayName("거리 계산 - 서울시청~부산시청은 약 325km(직선)")
    void haversine_knownDistance() {
        double m = RouteOptimizer.haversineM(new Point(1, 37.5663, 126.9779), new Point(2, 35.1796, 129.0756));
        assertEquals(325_000, m, 5_000);
    }
}
