package com.example.pinkok_backend.controller;

import com.example.pinkok_backend.service.RouteOptimizationService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/route-optimizations")
public class RouteOptimizationController {

    private final RouteOptimizationService routeOptimizationService;

    public RouteOptimizationController(RouteOptimizationService routeOptimizationService) {
        this.routeOptimizationService = routeOptimizationService;
    }

    // TODO: API 엔드포인트 작성
}
