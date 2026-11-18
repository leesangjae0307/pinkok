package com.example.pinkok_backend.category;

import java.util.Arrays;
import java.util.Optional;

/**
 * 장소 종류. 앱 전체에서 이 9개로만 장소를 나눈다.
 *
 * <ul>
 *   <li>여행의 "전체" 탭에서 장소를 종류별로 묶어 보여줄 때 ({@link #label})</li>
 *   <li>프로필 "내 여행 취향" 비율을 낼 때 ({@link #tasteLabel})</li>
 *   <li>카카오맵에 없는 장소를 직접 추가할 때 사용자가 고르는 종류 ({@link #code} 를 categoryGroupCode 로 보낸다)</li>
 * </ul>
 *
 * <p>코드는 카카오 큰 분류 코드(FD6·CE7·AD5·AT4·CT1·MT1)를 그대로 쓰고, 카카오에 없는 바다·자연·기타만 우리가 만든 코드다.
 * 선언 순서가 곧 "전체" 탭에서 보여줄 순서다.
 */
public enum PlaceCategory {

    FOOD("FD6", "맛집", "맛집"),
    CAFE("CE7", "카페", "카페"),
    STAY("AD5", "숙소", "숙박"),
    SIGHT("AT4", "관광명소", "관광명소"),
    NATURE("NATURE", "자연", "자연"),
    SEA("SEA", "바다", "바다"),
    CULTURE("CT1", "문화시설", "문화시설"),
    SHOPPING("MT1", "쇼핑", "쇼핑"),
    ETC("ETC", "기타", "기타");

    private final String code;
    /** 장소 목록("전체" 탭)에 보이는 이름 */
    private final String label;
    /** 프로필 취향에 보이는 이름 (숙소만 "숙박"으로 다르다) */
    private final String tasteLabel;

    PlaceCategory(String code, String label, String tasteLabel) {
        this.code = code;
        this.label = label;
        this.tasteLabel = tasteLabel;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    public String tasteLabel() {
        return tasteLabel;
    }

    /** "전체" 탭에서 보여줄 순서 (0 부터) */
    public int order() {
        return ordinal();
    }

    public static Optional<PlaceCategory> ofCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        String trimmed = code.trim();
        return Arrays.stream(values()).filter(category -> category.code.equals(trimmed)).findFirst();
    }
}
