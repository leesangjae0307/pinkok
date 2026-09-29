package com.example.pinkok_backend.dto;

import com.example.pinkok_backend.entity.Diary;
import com.example.pinkok_backend.entity.DiaryMedia;
import com.example.pinkok_backend.entity.ItineraryItem;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 장소별 기록 한 개.
 *
 * <p>목록 화면이 "어느 핀의, 누구 기록인지"를 한 번에 그릴 수 있도록
 * 핀 정보(장소명·일차·순서)와 작성자 정보를 같이 담는다.
 * PinLog(4주차)에서 핀마다 내 영상/팀원 영상을 고르는 화면도 이 응답을 쓴다.
 */
@Getter
public class DiaryResponse {

    private final Long id;
    private final Long itineraryItemId;
    private final Long tripId;
    private final Integer dayNumber;
    private final Integer visitOrder;
    private final String placeName;
    private final DiaryAuthorResponse author;
    /** 지금 로그인한 사람이 쓴 기록인지 (앱에서 수정·삭제 버튼을 보여줄지 판단) */
    private final boolean mine;
    private final String content;
    private final Integer rating;
    private final List<DiaryMediaResponse> media;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private DiaryResponse(Diary diary, List<DiaryMedia> media, Long currentUserId) {
        ItineraryItem item = diary.getItineraryItem();

        this.id = diary.getId();
        this.itineraryItemId = item.getId();
        this.tripId = item.getTrip().getId();
        this.dayNumber = item.getDay() == null ? null : item.getDay().getDayNumber();
        this.visitOrder = item.getVisitOrder();
        this.placeName = item.getPlace().getName();
        this.author = DiaryAuthorResponse.from(diary.getUser());
        this.mine = diary.getUser().getId().equals(currentUserId);
        this.content = diary.getContent();
        this.rating = diary.getRating();
        this.media = media.stream().map(DiaryMediaResponse::from).toList();
        this.createdAt = diary.getCreatedAt();
        this.updatedAt = diary.getUpdatedAt();
    }

    public static DiaryResponse of(Diary diary, List<DiaryMedia> media, Long currentUserId) {
        return new DiaryResponse(diary, media, currentUserId);
    }
}
