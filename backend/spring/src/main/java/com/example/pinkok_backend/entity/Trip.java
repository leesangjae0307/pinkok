package com.example.pinkok_backend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.BatchSize;

@Entity
@Table(name = "trips")
@Getter
@Setter
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 100)
    private String region;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "companion_type", length = 20)
    private String companionType;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

    @Column(name = "is_public", nullable = false)
    private Boolean isPublic;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * 자유 해시태그 (# 없이, 보낸 순서대로). trip_tags 테이블에 저장된다.
     * 여행 목록에서 여행마다 따로 읽지 않도록 여러 여행의 태그를 한 번에 묶어 읽는다(BatchSize).
     */
    @ElementCollection
    @CollectionTable(name = "trip_tags", joinColumns = @JoinColumn(name = "trip_id"))
    @OrderColumn(name = "sort_order")
    @Column(name = "tag", nullable = false, length = 20)
    @BatchSize(size = 100)
    @Setter(AccessLevel.NONE)
    private List<String> tags = new ArrayList<>();

    /** 태그를 통째로 바꾼다 (목록 객체는 그대로 두고 내용만 바꿔야 JPA 가 변경을 제대로 추적한다) */
    public void replaceTags(List<String> newTags) {
        tags.clear();
        tags.addAll(newTags);
    }
}
