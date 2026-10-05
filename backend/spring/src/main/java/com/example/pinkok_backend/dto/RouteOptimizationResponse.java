package com.example.pinkok_backend.dto;

import com.example.pinkok_backend.entity.RouteOptimization;

import java.time.LocalDateTime;
import java.util.List;

public record RouteOptimizationResponse(
        Long id,
        Long tripId,
        boolean redistribute,
        int totalDistanceM,
        int totalDurationMin,
        int beforeDistanceM,
        boolean applied,
        LocalDateTime createdAt,
        List<RouteOptimizationResult.Day> days,
        List<Long> untouchedItemIds
) {
    public static RouteOptimizationResponse of(RouteOptimization entity, RouteOptimizationResult result) {
        return new RouteOptimizationResponse(
                entity.getId(), entity.getTrip().getId(), result.redistribute(),
                entity.getTotalDistanceM(), entity.getTotalDurationMin(), result.beforeDistanceM(),
                Boolean.TRUE.equals(entity.getIsApplied()), entity.getCreatedAt(),
                result.days(), result.untouchedItemIds());
    }
}
