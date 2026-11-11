package com.example.pinkok_backend.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 프로필 화면의 내 여행 집계. 기준은 모두 "내가 참여 중이고 삭제되지 않은 여행"이다.
 *
 * @param visitedCount 방문한 장소 수 (= visited 개수)
 * @param savedCount   저장한 장소 수 (= saved 개수)
 * @param regionCount  다녀온 지역 수 (= regions 개수)
 * @param visited      내가 기록(다이어리)을 남긴 핀. 최근에 쓴 순
 * @param saved        내 여행 일정에 담긴 핀. 최근에 담은 순
 * @param regions      여행 지역별 여행 수. 많은 순, 같으면 가나다순
 * @param tastes       내 여행들이 고른 테마별 횟수와 비율(%). 많은 순
 */
public record UserStatsResponse(
        int visitedCount,
        int savedCount,
        int regionCount,
        List<PlaceEntry> visited,
        List<PlaceEntry> saved,
        List<RegionEntry> regions,
        List<TasteEntry> tastes
) {

    /**
     * 방문·저장 목록의 한 줄.
     *
     * @param recordedAt visited 에서는 기록을 쓴 시각, saved 에서는 핀을 담은 시각
     */
    public record PlaceEntry(
            Long itineraryItemId,
            Long tripId,
            String tripTitle,
            String placeName,
            String roadAddress,
            String lotAddress,
            String categoryName,
            LocalDateTime recordedAt
    ) {
    }

    public record RegionEntry(String name, int tripCount) {
    }

    public record TasteEntry(String code, String name, int count, int percent) {
    }
}
