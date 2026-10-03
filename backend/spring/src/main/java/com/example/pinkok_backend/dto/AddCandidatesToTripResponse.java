package com.example.pinkok_backend.dto;

import java.util.List;

/**
 * 후보를 핀으로 추가한 결과.
 *
 * <p>건너뛴 것을 조용히 무시하면 사용자가 "왜 일부만 들어갔지?" 하고 헷갈리므로,
 * 무엇이 왜 빠졌는지 이유별로 나눠서 돌려준다.
 *
 * @param added               새로 꽂힌 핀 (날짜 미배정 상태)
 * @param skippedDuplicate    이미 이 여행에 있던 장소라 건너뛴 후보 ID
 * @param skippedNoCoordinate 카카오맵에서 좌표를 못 찾아 추가할 수 없는 후보 ID
 */
public record AddCandidatesToTripResponse(
        List<ItineraryItemResponse> added,
        List<Long> skippedDuplicate,
        List<Long> skippedNoCoordinate) {
}
