package com.example.pinkok_backend.dto;

import com.example.pinkok_backend.entity.Recommendation;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class RecommendationResponse {

    private final Long id;
    private final Long tripId;
    private final String name;
    private final String address;
    private final String category;
    private final String reason;
    /** SUGGESTED / ACCEPTED / REJECTED */
    private final String status;
    /** 수락해서 지도 좌표를 찾은 뒤에만 채워짐. */
    private final Long placeId;
    /** 수락했을 때 만들어진 일정 핀 id (수락 응답에서만 채워짐). */
    private final Long itineraryItemId;
    private final LocalDateTime createdAt;

    private RecommendationResponse(Recommendation r, Long itineraryItemId) {
        this.id = r.getId();
        this.tripId = r.getTrip().getId();
        this.name = r.getSuggestedName();
        this.address = r.getSuggestedAddress();
        this.category = r.getCategoryText();
        this.reason = r.getReason();
        this.status = r.getStatus();
        this.placeId = r.getPlace() == null ? null : r.getPlace().getId();
        this.itineraryItemId = itineraryItemId;
        this.createdAt = r.getCreatedAt();
    }

    public static RecommendationResponse from(Recommendation r) {
        return new RecommendationResponse(r, null);
    }

    public static RecommendationResponse accepted(Recommendation r, Long itineraryItemId) {
        return new RecommendationResponse(r, itineraryItemId);
    }
}
