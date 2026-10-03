package com.example.pinkok_backend.controller;

import com.example.pinkok_backend.dto.RouteOptimizationRequest;
import com.example.pinkok_backend.dto.RouteOptimizationResponse;
import com.example.pinkok_backend.security.CurrentUserId;
import com.example.pinkok_backend.service.RouteOptimizationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/route-optimizations")
public class RouteOptimizationController {

    private final RouteOptimizationService routeOptimizationService;

    public RouteOptimizationController(RouteOptimizationService routeOptimizationService) {
        this.routeOptimizationService = routeOptimizationService;
    }

    /** 동선 최적화 제안을 만든다 (일정은 아직 안 바뀜). 계산이라 바로 결과를 준다. */
    @PostMapping
    public ResponseEntity<RouteOptimizationResponse> create(@CurrentUserId Long userId,
                                                             @Valid @RequestBody RouteOptimizationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(routeOptimizationService.create(userId, request));
    }

    @GetMapping
    public List<RouteOptimizationResponse> list(@CurrentUserId Long userId, @RequestParam Long tripId) {
        return routeOptimizationService.list(tripId, userId);
    }

    @GetMapping("/{id}")
    public RouteOptimizationResponse get(@CurrentUserId Long userId, @PathVariable Long id) {
        return routeOptimizationService.get(id, userId);
    }

    /** 제안을 일정에 반영한다 (핀의 날짜·순서·이동수단이 바뀜). */
    @PostMapping("/{id}/apply")
    public RouteOptimizationResponse apply(@CurrentUserId Long userId, @PathVariable Long id) {
        return routeOptimizationService.apply(id, userId);
    }
}
