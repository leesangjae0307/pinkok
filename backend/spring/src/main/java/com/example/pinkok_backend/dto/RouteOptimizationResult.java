package com.example.pinkok_backend.dto;

import java.util.List;

/** route_optimizations.result_json 에 저장되는 내용 (그대로 응답에도 쓴다). */
public record RouteOptimizationResult(
        boolean redistribute,
        int beforeDistanceM,
        List<Day> days,
        List<Long> untouchedItemIds
) {
    public record Day(Long dayId, int dayNumber, int totalDistanceM, int totalDurationMin, List<Stop> stops) {
    }

    /** 직전 장소에서 이 장소로 오는 이동 정보 (그날 첫 장소는 0 / null) */
    public record Stop(Long itemId, Long placeId, String placeName, int order,
                       String transportMode, int distanceFromPreviousM, int durationFromPreviousMin) {
    }
}
