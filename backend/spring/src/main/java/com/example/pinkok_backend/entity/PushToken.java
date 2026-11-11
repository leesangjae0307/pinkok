package com.example.pinkok_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 폰 푸시(FCM)를 보낼 기기 토큰. 한 사람이 폰을 여러 대 쓸 수 있어서 사용자당 여러 개다.
 *
 * <p>토큰은 기기(앱 설치)마다 하나라 전체에서 유일하다. 같은 폰에서 다른 계정으로 로그인하면
 * 토큰의 주인이 그 계정으로 바뀐다 — 안 그러면 로그아웃한 사람의 알림이 그 폰에 계속 간다.
 */
@Entity
@Table(
        name = "push_tokens",
        uniqueConstraints = @UniqueConstraint(name = "uk_push_tokens_token", columnNames = "token")
)
@Getter
@Setter
public class PushToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 512)
    private String token;

    // ANDROID / IOS
    @Column(nullable = false, length = 10)
    private String platform;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
