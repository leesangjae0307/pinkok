package com.example.pinkok_backend.dto;

import com.example.pinkok_backend.entity.ItineraryItem;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
public class ItineraryItemResponse {

    private final Long id;
    private final Long tripId;
    private final Long dayId;
    private final Integer dayNumber;
    private final PlaceResponse place;
    private final Integer visitOrder;
    private final LocalTime plannedArrivalTime;
    private final Integer stayMinutes;
    private final String transportMode;
    private final String memo;
    private final String addedBy;
    /** 이 핀에 달린 팀원들의 기록 별점 평균 (소수 첫째 자리). 별점이 하나도 없으면 null */
    private final Double rating;
    /** 이 핀에 달린 기록(다이어리) 개수 */
    private final int reviewsCount;
    /** 이 핀의 첫 번째 기록 사진·영상 썸네일 주소. 없으면 null (앱이 기본 이미지를 보여준다) */
    private final String thumbnailUrl;
    private final LocalDateTime createdAt;

    private ItineraryItemResponse(ItineraryItem item, Stats stats) {
        this.id = item.getId();
        this.tripId = item.getTrip().getId();
        this.dayId = item.getDay() == null ? null : item.getDay().getId();
        this.dayNumber = item.getDay() == null ? null : item.getDay().getDayNumber();
        this.place = PlaceResponse.from(item.getPlace());
        this.visitOrder = item.getVisitOrder();
        this.plannedArrivalTime = item.getPlannedArrivalTime();
        this.stayMinutes = item.getStayMinutes();
        this.transportMode = item.getTransportMode();
        this.memo = item.getMemo();
        this.addedBy = item.getAddedBy();
        this.rating = stats.rating();
        this.reviewsCount = stats.reviewsCount();
        this.thumbnailUrl = stats.thumbnailUrl();
        this.createdAt = item.getCreatedAt();
    }

    /** 방금 만든 핀처럼 아직 기록이 없을 수밖에 없을 때 */
    public static ItineraryItemResponse from(ItineraryItem item) {
        return new ItineraryItemResponse(item, Stats.NONE);
    }

    public static ItineraryItemResponse from(ItineraryItem item, Stats stats) {
        return new ItineraryItemResponse(item, stats == null ? Stats.NONE : stats);
    }

    /** 핀에 달린 기록에서 계산한 값 (별점 평균, 기록 수, 썸네일) */
    public record Stats(Double rating, int reviewsCount, String thumbnailUrl) {
        public static final Stats NONE = new Stats(null, 0, null);
    }
}
