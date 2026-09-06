package com.example.pinkok_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_settings")
@Getter
@Setter
public class UserSetting {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "default_companion_type", length = 20)
    private String defaultCompanionType;

    @Column(name = "notification_enabled", nullable = false)
    private Boolean notificationEnabled;

    @Column(nullable = false, length = 30)
    private String theme;

    @Column(nullable = false, length = 10)
    private String language;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
