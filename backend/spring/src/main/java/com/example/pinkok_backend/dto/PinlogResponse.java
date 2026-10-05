package com.example.pinkok_backend.dto;

import com.example.pinkok_backend.entity.Pinlog;
import com.example.pinkok_backend.entity.PinlogClip;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * PinLog 한 편.
 *
 * <p>영상 합성은 오래 걸려서 요청하자마자 결과가 나오지 않는다. 앱은 이 응답의 {@code status} 가
 * QUEUED → PROCESSING → DONE 으로 바뀔 때까지 다시 조회하고, DONE 이 되면 {@code videoUrl} 을 재생한다.
 * 실패하면 FAILED 와 함께 {@code errorMessage} 에 이유가 담긴다.
 */
@Getter
public class PinlogResponse {

    private final Long id;
    private final Long tripId;
    private final String title;
    /** QUEUED / PROCESSING / DONE / FAILED */
    private final String status;
    private final String videoUrl;
    private final String thumbnailUrl;
    private final Integer durationMs;
    private final String resolution;
    private final String errorMessage;
    private final String templateCode;
    private final String styleCode;
    private final String creatorNickname;
    /** 내가 만든 PinLog 인지 (삭제 버튼을 보여줄지 판단) */
    private final boolean mine;
    private final List<PinlogClipResponse> clips;
    private final LocalDateTime createdAt;
    private final LocalDateTime completedAt;

    private PinlogResponse(Pinlog pinlog, List<PinlogClip> clips, Long currentUserId) {
        this.id = pinlog.getId();
        this.tripId = pinlog.getTrip().getId();
        this.title = pinlog.getTitle();
        this.status = pinlog.getStatus();
        this.videoUrl = pinlog.getVideoUrl();
        this.thumbnailUrl = pinlog.getThumbnailUrl();
        this.durationMs = pinlog.getDurationMs();
        this.resolution = pinlog.getResolution();
        this.errorMessage = pinlog.getErrorMessage();
        this.templateCode = pinlog.getTemplateCode();
        this.styleCode = pinlog.getStyleCode();
        this.creatorNickname = pinlog.getUser().getNickname();
        this.mine = pinlog.getUser().getId().equals(currentUserId);
        this.clips = clips.stream().map(PinlogClipResponse::from).toList();
        this.createdAt = pinlog.getCreatedAt();
        this.completedAt = pinlog.getCompletedAt();
    }

    public static PinlogResponse of(Pinlog pinlog, List<PinlogClip> clips, Long currentUserId) {
        return new PinlogResponse(pinlog, clips, currentUserId);
    }
}
