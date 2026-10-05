package com.example.pinkok_backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

/** PinLog 에 넣을 클립 하나. 핀마다 "내 영상 / 팀원 영상" 중 고른 것을 보낸다. */
@Getter
@Setter
public class PinlogClipRequest {

    /** 장소 기록에 붙어 있는 사진·영상(diary_media) 의 ID */
    @NotNull
    private Long diaryMediaId;

    /** 영상에서 잘라낼 시작 지점(ms). 안 보내면 처음부터. */
    @PositiveOrZero
    private Integer startMs;

    /** 잘라낼 끝 지점(ms). 안 보내면 끝까지 (클립당 최대 5초까지만 쓰인다). */
    @PositiveOrZero
    private Integer endMs;
}
