package com.example.pinkok_backend.dto;

import com.example.pinkok_backend.entity.DiaryMedia;
import com.example.pinkok_backend.entity.ItineraryItem;
import lombok.Getter;

import java.util.List;

/**
 * 핀 하나에서 고를 수 있는 클립들.
 *
 * <p>여행을 팀원이 함께 쓰므로 한 장소에 여러 사람의 영상이 있을 수 있다. PinLog 를 만들 때
 * 핀마다 <b>내 영상 / 팀원 영상</b> 중에서 고르는 화면이 이 응답으로 그려진다.
 * 세 명이 갔는데 한 명이 깜빡한 장소도 다른 사람 영상으로 채울 수 있다.
 */
@Getter
public class ClipOptionResponse {

    private final Long itineraryItemId;
    private final Integer dayNumber;
    private final Integer visitOrder;
    private final String placeName;
    private final List<Option> options;

    private ClipOptionResponse(ItineraryItem item, List<DiaryMedia> media, Long currentUserId) {
        this.itineraryItemId = item.getId();
        this.dayNumber = item.getDay() == null ? null : item.getDay().getDayNumber();
        this.visitOrder = item.getVisitOrder();
        this.placeName = item.getPlace().getName();
        this.options = media.stream().map(m -> new Option(m, currentUserId)).toList();
    }

    public static ClipOptionResponse of(ItineraryItem item, List<DiaryMedia> media, Long currentUserId) {
        return new ClipOptionResponse(item, media, currentUserId);
    }

    @Getter
    public static class Option {

        private final Long diaryMediaId;
        /** PHOTO / VIDEO */
        private final String mediaType;
        private final String fileUrl;
        private final String thumbnailUrl;
        private final Integer durationMs;
        private final DiaryAuthorResponse author;
        /** 내가 찍은 것인지 (앱에서 "내 영상"으로 묶어 보여줄 때) */
        private final boolean mine;

        private Option(DiaryMedia media, Long currentUserId) {
            this.diaryMediaId = media.getId();
            this.mediaType = media.getMediaType();
            this.fileUrl = media.getFileUrl();
            this.thumbnailUrl = media.getThumbnailUrl();
            this.durationMs = media.getDurationMs();
            this.author = DiaryAuthorResponse.from(media.getDiary().getUser());
            this.mine = media.getDiary().getUser().getId().equals(currentUserId);
        }
    }
}
