package com.example.pinkok_backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PinlogCreateRequest {

    @NotNull
    private Long tripId;

    @Size(max = 100)
    private String title;

    /** TRAVEL_DIARY / CINEMATIC / EMOTIONAL (지금은 저장만 한다) */
    @Size(max = 30)
    private String templateCode;

    /** BRIGHT / CALM / RETRO (지금은 저장만 한다) */
    @Size(max = 30)
    private String styleCode;

    /**
     * 쓸 클립을 고른 순서대로. <b>비워두면</b> 서버가 동선 순서대로 자동으로 고른다
     * (핀마다 내 영상 우선, 없으면 팀원 영상).
     */
    @Valid
    private List<PinlogClipRequest> clips;

    /**
     * 자동 선택에 사진까지 포함할지. 기본은 영상만(false) — 앱의 클립 선택 화면이 영상만 다루기 때문.
     * 켜면 영상이 없는 핀을 사진 한 장(2초 정지 장면)으로 채운다.
     */
    private Boolean includePhotos;
}
