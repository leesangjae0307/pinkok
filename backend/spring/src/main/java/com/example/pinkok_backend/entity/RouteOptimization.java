package com.example.pinkok_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "route_optimizations")
@Getter
@Setter
public class RouteOptimization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_request_id")
    private AiRequest aiRequest;

    @Column(name = "total_distance_m")
    private Integer totalDistanceM;

    @Column(name = "total_duration_min")
    private Integer totalDurationMin;

    @Column(name = "result_json", columnDefinition = "json")
    private String resultJson;

    @Column(name = "is_applied", nullable = false)
    private Boolean isApplied;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
