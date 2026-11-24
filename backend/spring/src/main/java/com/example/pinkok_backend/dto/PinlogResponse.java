package com.example.pinkok_backend.dto;

import com.example.pinkok_backend.entity.Pinlog;
import com.example.pinkok_backend.entity.PinlogClip;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
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

    private static final DateTimeFormatter FILE_NAME_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    /** 파일 이름의 날짜는 사용자가 보는 날짜여야 한다. 서버 시각(UTC)을 그대로 쓰면 한국 오전 9시 전엔 전날이 찍힌다. */
    private static final ZoneId FILE_NAME_ZONE = ZoneId.of("Asia/Seoul");

    private final Long id;
    private final Long tripId;
    private final String title;
    /** QUEUED / PROCESSING / DONE / FAILED */
    private final String status;
    private final String videoUrl;
    /**
     * 갤러리에 저장할 때 쓸 파일 이름 (완성됐을 때만).
     *
     * <p>다운로드 응답 헤더에도 같은 이름이 들어가지만, 한글 이름은 헤더에 그대로 담을 수 없어
     * 인코딩된 형태로 나간다. 그걸 읽지 못하는 클라이언트가 있어서(포스트맨이 그렇다) 앱이
     * 바로 쓸 수 있도록 여기에도 담아준다.
     */
    private final String downloadFileName;
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
        this.downloadFileName = pinlog.getVideoUrl() == null ? null : downloadFileNameOf(pinlog);
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

    /**
     * 갤러리에서 알아볼 수 있는 파일 이름을 만든다. 예: {@code PinKok_제주도_여행_20261020.mp4}
     *
     * <p>UUID 이름으로 저장되면 나중에 뭐가 뭔지 찾을 수 없다. 제목을 안 적었으면 여행 이름을 쓰고,
     * 파일 이름에 쓸 수 없는 글자(\ / : * ? " &lt; &gt; |)는 빼고 공백은 밑줄로 바꾼다.
     */
    public static String downloadFileNameOf(Pinlog pinlog) {
        String base = pinlog.getTitle() == null || pinlog.getTitle().isBlank()
                ? pinlog.getTrip().getTitle()
                : pinlog.getTitle();
        String safe = base.replaceAll("[\\\\/:*?\"<>|]", "").replaceAll("\\s+", "_").trim();
        LocalDateTime when = pinlog.getCompletedAt() == null ? pinlog.getCreatedAt() : pinlog.getCompletedAt();
        String date = when.atOffset(ZoneOffset.UTC).atZoneSameInstant(FILE_NAME_ZONE).format(FILE_NAME_DATE);
        return "PinKok_" + safe + "_" + date + ".mp4";
    }
}
