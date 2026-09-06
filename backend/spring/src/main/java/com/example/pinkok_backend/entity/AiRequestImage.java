package com.example.pinkok_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_request_images")
@Getter
@Setter
public class AiRequestImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_request_id", nullable = false)
    private AiRequest aiRequest;

    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    @Column(name = "display_order")
    private Integer displayOrder;

    private Integer width;

    private Integer height;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
