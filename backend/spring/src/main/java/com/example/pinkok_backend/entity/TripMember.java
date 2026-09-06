package com.example.pinkok_backend.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "trip_members")
@Getter
@Setter
public class TripMember {

    @EmbeddedId
    private TripMemberId id;

    @MapsId("tripId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // OWNER(만든 사람) / MEMBER
    @Column(nullable = false, length = 10)
    private String role;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Embeddable
    @Getter
    @Setter
    @EqualsAndHashCode
    public static class TripMemberId implements Serializable {

        @Column(name = "trip_id")
        private Long tripId;

        @Column(name = "user_id")
        private Long userId;
    }
}
