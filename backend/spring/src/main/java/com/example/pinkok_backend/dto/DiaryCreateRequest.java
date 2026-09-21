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
}
