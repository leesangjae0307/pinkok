package com.example.pinkok_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "recommendations")
@Getter
@Setter
public class Recommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_request_id")
    private AiRequest aiRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id")
    private Place place;

    @Column(name = "suggested_name", nullable = false, length = 200)
    private String suggestedName;

    // AI가 준 주소 - 수락할 때 카카오맵에서 좌표를 찾는 힌트로 쓴다
    @Column(name = "suggested_address", length = 300)
    private String suggestedAddress;

    // AI가 분류한 카테고리 (맛집/카페/관광지 ...) - 앱에서 아이콘 표시용
    @Column(name = "category_text", length = 100)
    private String categoryText;

    @Column(length = 500)
    private String reason;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
