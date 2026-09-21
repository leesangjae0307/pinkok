package com.example.pinkok_backend.dto;

import com.example.pinkok_backend.entity.DiaryMedia;
import lombok.Getter;

@Getter
public class DiaryMediaResponse {

    private final Long id;
    /** PHOTO / VIDEO */
    private final String mediaType;
    private final String fileUrl;
    private final String thumbnailUrl;
    private final Integer durationMs;
    private final Integer displayOrder;

    private DiaryMediaResponse(DiaryMedia media) {
        this.id = media.getId();
        this.mediaType = media.getMediaType();
        this.fileUrl = media.getFileUrl();
        this.thumbnailUrl = media.getThumbnailUrl();
        this.durationMs = media.getDurationMs();
        this.displayOrder = media.getDisplayOrder();
    }

    public static DiaryMediaResponse from(DiaryMedia media) {
        return new DiaryMediaResponse(media);
    }
}
