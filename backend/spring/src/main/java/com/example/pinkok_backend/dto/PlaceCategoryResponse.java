package com.example.pinkok_backend.dto;

import com.example.pinkok_backend.category.PlaceCategory;

/**
 * 장소 종류 한 개.
 *
 * @param code       종류 코드. 직접 추가하는 장소의 {@code place.categoryGroupCode} 로 그대로 보내면 된다
 * @param label      장소 목록("전체" 탭)에 보이는 이름 (맛집, 카페, 숙소 …)
 * @param tasteLabel 프로필 취향에 보이는 이름 (숙소만 "숙박")
 * @param order      "전체" 탭에서 보여줄 순서 (0 부터)
 */
public record PlaceCategoryResponse(String code, String label, String tasteLabel, int order) {

    public static PlaceCategoryResponse from(PlaceCategory category) {
        return new PlaceCategoryResponse(category.code(), category.label(), category.tasteLabel(), category.order());
    }
}
