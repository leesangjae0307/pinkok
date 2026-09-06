package com.example.pinkok_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(
        name = "itinerary_items",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_itinerary_items_order",
                columnNames = {"day_id", "visit_order"}
        )
)
@Getter
@Setter
public class ItineraryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "day_id")
    private ItineraryDay day;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id")
    private PlaceCandidate candidate;

    @Column(name = "visit_order")
    private Integer visitOrder;

    @Column(name = "planned_arrival_time")
    private LocalTime plannedArrivalTime;

    @Column(name = "stay_minutes")
    private Integer stayMinutes;

    // 직전 장소에서 이 장소로 올 때의 이동수단 (CAR / WALK / BUS / TRAIN)
    @Column(name = "transport_mode", length = 20)
    private String transportMode;

    @Column(length = 500)
    private String memo;

    @Column(name = "added_by", nullable = false, length = 10)
    private String addedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
