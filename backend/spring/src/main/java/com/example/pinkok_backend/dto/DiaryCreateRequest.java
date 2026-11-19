package com.example.pinkok_backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DiaryCreateRequest {

    /** 기록을 남길 핀(itinerary_items) ID */
    @NotNull
    private Long itineraryItemId;

    @Size(max = 2000)
    private String content;

    /** 만족도 1~5. 안 매겼으면 비워둔다. */
    @Min(1)
    @Max(5)
    private Integer rating;

    /** 사진 여러 장 + 영상 1개까지. 순서는 보낸 순서대로 저장된다. */
    @Valid
    private List<DiaryMediaRequest> media;

    /**
     * (선택) 저장 요청마다 앱이 만든 고유 값 (UUID 등, 64자 이하). 같은 값으로 다시 보내면(응답을 못 받아 재시도한 경우)
     * 이미 만든 기록을 그대로 돌려준다 — 409 를 받고 수정으로 우회하지 않아도 된다.
     */
    @Size(max = 64)
    private String clientRequestId;
}
