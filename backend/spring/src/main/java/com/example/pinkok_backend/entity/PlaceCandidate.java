package com.example.pinkok_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "place_candidates")
@Getter
@Setter
public class PlaceCandidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_request_id", nullable = false)
    private AiRequest aiRequest;

    // 이 후보가 어느 스크린샷에서 나왔는지 (썸네일 표시용, 이미지 입력일 때만)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_image_id")
    private AiRequestImage sourceImage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id")
    private Place place;

    @Column(name = "raw_name", nullable = false, length = 200)
    private String rawName;

    @Column(name = "raw_address", length = 300)
    private String rawAddress;

    @Column(name = "category_text", length = 100)
    private String categoryText;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(precision = 4, scale = 3)
    private BigDecimal confidence;

    @Column(name = "is_selected", nullable = false)
    private Boolean isSelected;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
