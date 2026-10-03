package com.example.pinkok_backend.repository;

import com.example.pinkok_backend.entity.RouteOptimization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RouteOptimizationRepository extends JpaRepository<RouteOptimization, Long> {

    List<RouteOptimization> findAllByTrip_IdOrderByCreatedAtDescIdDesc(Long tripId);
}
