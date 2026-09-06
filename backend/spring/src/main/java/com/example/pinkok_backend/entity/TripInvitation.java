package com.example.pinkok_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "trip_invitations")
@Getter
@Setter
public class TripInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inviter_id", nullable = false)
    private User inviter;

    // @아이디로 특정 사람을 지목한 경우에만 채워짐
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invitee_id")
    private User invitee;

    // 링크로 초대한 경우에만 채워짐
    @Column(name = "invite_code", unique = true, length = 64)
    private String inviteCode;

    // PENDING / ACCEPTED / REJECTED / EXPIRED
    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;
}
