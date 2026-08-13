package com.example.pinkok_backend.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Entity
@Table(name = "user_travel_styles")
@Getter
@Setter
public class UserTravelStyle {

    @EmbeddedId
    private UserTravelStyleId id;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @MapsId("styleId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "style_id", nullable = false)
    private TravelStyle travelStyle;

    @Embeddable
    @Getter
    @Setter
    @EqualsAndHashCode
    public static class UserTravelStyleId implements Serializable {

        @Column(name = "user_id")
        private Long userId;

        @Column(name = "style_id")
        private Long styleId;
    }
}
