package com.example.pinkok_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_requests")
@Getter
@Setter
public class AiRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id")
    private Trip trip;

    @Column(name = "request_type", nullable = false, length = 20)
    private String requestType;

    // 사용자가 넣은 방식 (LINK / IMAGE / TEXT)
    @Column(name = "input_type", nullable = false, length = 20)
    private String inputType;

    // 출처 (YOUTUBE / INSTAGRAM / BLOG / OTHER)
    @Column(name = "source_platform", length = 20)
    private String sourcePlatform;

    // LINK 일 때만 채워짐
    @Column(name = "source_url", length = 1000)
    private String sourceUrl;

    // TEXT 일 때만 채워짐 — 사용자가 붙여넣은 글
    @Column(name = "source_text", columnDefinition = "TEXT")
    private String sourceText;

    @Column(name = "prompt_text", columnDefinition = "TEXT")
    private String promptText;

    @Column(name = "model_name", length = 50)
    private String modelName;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "error_code", length = 50)
    private String errorCode;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;

    @Column(name = "input_tokens")
    private Integer inputTokens;

    @Column(name = "output_tokens")
    private Integer outputTokens;

    @Column(name = "cost_usd", precision = 10, scale = 6)
    private BigDecimal costUsd;

    @Column(name = "raw_response", columnDefinition = "json")
    private String rawResponse;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
