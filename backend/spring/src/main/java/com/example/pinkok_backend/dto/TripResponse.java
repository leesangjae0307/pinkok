package com.example.pinkok_backend.dto;

import com.example.pinkok_backend.entity.Trip;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
public class TripResponse {

    private final Long id;
    private final String title;
    private final String region;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final String companionType;
    private final String status;
    private final String coverImageUrl;
    private final Boolean isPublic;
    /** 요청한 사람이 이 여행에서 OWNER 인지 MEMBER 인지. 화면에서 수정 버튼 노출 여부에 씀. */
    private final String myRole;
    /** 이 여행에 꽂힌 핀 개수 (목록·바텀시트의 "장소 N개") */
    private final int placesCount;
    private final LocalDateTime createdAt;

    private TripResponse(Trip trip, String myRole, int placesCount) {
        this.id = trip.getId();
        this.title = trip.getTitle();
        this.region = trip.getRegion();
        this.startDate = trip.getStartDate();
        this.endDate = trip.getEndDate();
        this.companionType = trip.getCompanionType();
        this.status = trip.getStatus();
        this.coverImageUrl = trip.getCoverImageUrl();
        this.isPublic = trip.getIsPublic();
        this.myRole = myRole;
        this.placesCount = placesCount;
        this.createdAt = trip.getCreatedAt();
    }

    public static TripResponse of(Trip trip, String myRole, int placesCount) {
        return new TripResponse(trip, myRole, placesCount);
    }
}
