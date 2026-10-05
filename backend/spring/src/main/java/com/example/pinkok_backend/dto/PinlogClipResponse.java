package com.example.pinkok_backend.dto;

import com.example.pinkok_backend.entity.DiaryMedia;
import com.example.pinkok_backend.entity.PinlogClip;
import lombok.Getter;

/** PinLog 를 이루는 클립 하나 (어느 장소의, 누구 영상인지). */
@Getter
public class PinlogClipResponse {

    private final Long diaryMediaId;
    private final Integer clipOrder;
    private final String mediaType;
    private final String fileUrl;
    private final String thumbnailUrl;
    private final String placeName;
    private final String authorNickname;

    private PinlogClipResponse(PinlogClip clip) {
        DiaryMedia media = clip.getDiaryMedia();

        this.diaryMediaId = media.getId();
        this.clipOrder = clip.getClipOrder();
        this.mediaType = media.getMediaType();
        this.fileUrl = media.getFileUrl();
        this.thumbnailUrl = media.getThumbnailUrl();
        this.placeName = media.getDiary().getItineraryItem().getPlace().getName();
        this.authorNickname = media.getDiary().getUser().getNickname();
    }

    public static PinlogClipResponse from(PinlogClip clip) {
        return new PinlogClipResponse(clip);
    }
}
