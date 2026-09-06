package com.example.pinkok_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "itinerary_days",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_itinerary_days_trip_day",
                columnNames = {"trip_id", "day_number"}
        )
)
@Getter
@Setter
public class ItineraryDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Column(name = "day_number", nullable = false)
    private Integer dayNumber;

    @Column(name = "visit_date")
    private LocalDate visitDate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
