package com.example.pinkok_backend.service;

import com.example.pinkok_backend.repository.RouteOptimizationRepository;
import org.springframework.stereotype.Service;

@Service
public class RouteOptimizationService {

    private final RouteOptimizationRepository routeOptimizationRepository;

    public RouteOptimizationService(RouteOptimizationRepository routeOptimizationRepository) {
        this.routeOptimizationRepository = routeOptimizationRepository;
    }

    // TODO: 비즈니스 로직 작성
}
