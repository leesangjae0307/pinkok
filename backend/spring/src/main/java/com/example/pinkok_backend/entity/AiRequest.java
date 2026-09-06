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

    @Column(name = "source_type", length = 20)
    private String sourceType;

    @Column(name = "source_url", length = 1000)
    private String sourceUrl;

    @Column(name = "source_file_url", length = 500)
    private String sourceFileUrl;

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
