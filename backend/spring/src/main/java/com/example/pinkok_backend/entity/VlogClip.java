package com.example.pinkok_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "vlog_clips",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_vlog_clips_order",
                columnNames = {"vlog_id", "clip_order"}
        )
)
@Getter
@Setter
public class VlogClip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vlog_id", nullable = false)
    private Vlog vlog;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diary_media_id", nullable = false)
    private DiaryMedia diaryMedia;

    @Column(name = "clip_order", nullable = false)
    private Integer clipOrder;

    @Column(name = "start_ms", nullable = false)
    private Integer startMs;

    @Column(name = "end_ms")
    private Integer endMs;
}
