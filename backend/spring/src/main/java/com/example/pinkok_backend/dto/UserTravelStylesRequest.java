package com.example.pinkok_backend.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 내 여행 취향을 통째로 바꾼다. 여기 있는 것만 남고 나머지는 해제된다 (빈 배열이면 전부 해제).
 *
 * @param styleCodes GET /travel-styles 의 code 목록 (예: ["FOOD", "CAFE"])
 */
public record UserTravelStylesRequest(
        @NotNull(message = "styleCodes 는 필수입니다. 비우려면 빈 배열([])을 보내세요.")
        List<String> styleCodes
) {
}
