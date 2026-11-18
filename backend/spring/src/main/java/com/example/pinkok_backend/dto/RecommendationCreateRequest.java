package com.example.pinkok_backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RecommendationCreateRequest {

    @NotNull
    private Long tripId;

    /** SOLO / COUPLE / FAMILY / FRIEND. 비우면 여행 -> 내 기본 설정 순으로 찾는다. */
    private String companionType;

    /** 추천받을 지역(예: 제주). 비우면 여행에 저장된 지역을 쓴다. 최대 100자. */
    private String region;

    /** GET /travel-styles 의 code (FOOD, CAFE ...). 비우면 여행 -> 내 취향 순으로 찾는다. */
    private List<String> styleCodes;

    /** 추천 개수 1~10, 비우면 5. */
    private Integer count;
}
