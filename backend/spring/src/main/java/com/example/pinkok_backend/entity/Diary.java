package com.example.pinkok_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "diaries",
        // 한 핀에 한 사람은 기록 하나만 (팀원끼리는 각자 하나씩)
        uniqueConstraints = @UniqueConstraint(
                name = "uk_diaries_item_user",
                columnNames = {"itinerary_item_id", "user_id"}
        )
)
@Getter
@Setter
public class Diary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "itinerary_item_id", nullable = false)
    private ItineraryItem itineraryItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(columnDefinition = "TEXT")
    private String content;

    // SQL: TINYINT (만족도 1~5) -> 플래그가 아니므로 Integer 로 매핑
    @Column(columnDefinition = "TINYINT")
    private Integer rating;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 앱이 저장 요청마다 붙인 고유 값 (멱등 키). 응답을 못 받은 앱이 같은 요청을 다시 보내면
     * 이 값으로 "이미 만든 그 기록"임을 알아보고 새로 만들지 않고 그대로 돌려준다.
     */
    @Column(name = "client_request_id", length = 64)
    private String clientRequestId;
}
