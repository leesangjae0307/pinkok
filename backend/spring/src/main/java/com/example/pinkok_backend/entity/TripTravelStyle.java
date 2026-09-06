package com.example.pinkok_backend.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Entity
@Table(name = "trip_travel_styles")
@Getter
@Setter
public class TripTravelStyle {

    @EmbeddedId
    private TripTravelStyleId id;

    @MapsId("tripId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @MapsId("styleId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "style_id", nullable = false)
    private TravelStyle travelStyle;

    @Embeddable
    @Getter
    @Setter
    @EqualsAndHashCode
    public static class TripTravelStyleId implements Serializable {

        @Column(name = "trip_id")
        private Long tripId;

        @Column(name = "style_id")
        private Long styleId;
    }
}
