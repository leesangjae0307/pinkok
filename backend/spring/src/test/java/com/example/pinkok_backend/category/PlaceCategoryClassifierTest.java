package com.example.pinkok_backend.category;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.example.pinkok_backend.category.PlaceCategoryClassifier.classify;
import static org.assertj.core.api.Assertions.assertThat;

/** 장소 종류 분류 규칙. DB 없이 (코드, 카카오 경로) 만 넣어서 확인한다. */
class PlaceCategoryClassifierTest {

    @Test
    @DisplayName("카카오 큰 분류 코드가 있으면 그대로 (맛집·카페·숙소·쇼핑·관광명소·문화시설)")
    void byKakaoCode() {
        assertThat(classify("FD6", "음식점 > 한식")).isEqualTo(PlaceCategory.FOOD);
        assertThat(classify("CE7", "음식점 > 카페")).isEqualTo(PlaceCategory.CAFE);
        assertThat(classify("AD5", "숙박 > 호텔")).isEqualTo(PlaceCategory.STAY);
        assertThat(classify("MT1", "가정,생활 > 대형마트")).isEqualTo(PlaceCategory.SHOPPING);
        assertThat(classify("AT4", "여행 > 관광,명소 > 테마파크")).isEqualTo(PlaceCategory.SIGHT);
        assertThat(classify("CT1", "문화,예술 > 문화시설 > 박물관")).isEqualTo(PlaceCategory.CULTURE);
    }

    @Test
    @DisplayName("바다·자연은 관광명소 코드보다 먼저 본다, 단 맛집·카페 코드는 이름에 바다가 있어도 그대로")
    void seaAndNatureFirst() {
        assertThat(classify("AT4", "여행 > 관광,명소 > 해수욕장")).isEqualTo(PlaceCategory.SEA);
        assertThat(classify("AT4", "여행 > 관광,명소 > 오름")).isEqualTo(PlaceCategory.NATURE);
        assertThat(classify(null, "여행 > 섬 > 섬(내륙)")).isEqualTo(PlaceCategory.SEA);
        assertThat(classify(null, "여행 > 관광,명소 > 산")).isEqualTo(PlaceCategory.NATURE);
        assertThat(classify("CE7", "음식점 > 카페 > 오션뷰카페")).isEqualTo(PlaceCategory.CAFE);
        assertThat(classify(null, "음식점 > 한식 > 해물,생선")).isEqualTo(PlaceCategory.FOOD);
        assertThat(classify("AG2", "부동산 > 중개업소")).isEqualTo(PlaceCategory.ETC); // 부동산 ≠ 산
    }

    @Test
    @DisplayName("코드가 없으면 카카오 경로 첫 단계로 (감천문화마을 '여행 > 관광,명소' 등)")
    void byPathWhenNoCode() {
        assertThat(classify(null, "여행 > 관광,명소")).isEqualTo(PlaceCategory.SIGHT);
        assertThat(classify("", "문화,예술 > 문화시설 > 미술관")).isEqualTo(PlaceCategory.CULTURE);
        assertThat(classify(null, "음식점 > 카페 > 커피전문점")).isEqualTo(PlaceCategory.CAFE);
        assertThat(classify(null, "숙박 > 펜션")).isEqualTo(PlaceCategory.STAY);
        assertThat(classify(null, "가정,생활 > 시장 > 전통시장")).isEqualTo(PlaceCategory.SHOPPING);
        assertThat(classify(null, "서비스,산업 > 세탁소")).isEqualTo(PlaceCategory.ETC);
    }

    @Test
    @DisplayName("직접 추가한 장소: 사용자가 고른 우리 코드(바다·자연·기타)와 카카오 코드(숙소 등)를 그대로 인식")
    void userPickedCodes() {
        assertThat(classify("SEA", null)).isEqualTo(PlaceCategory.SEA);
        assertThat(classify("NATURE", null)).isEqualTo(PlaceCategory.NATURE);
        assertThat(classify("ETC", null)).isEqualTo(PlaceCategory.ETC);
        assertThat(classify("AD5", null)).isEqualTo(PlaceCategory.STAY);
        assertThat(classify("AT4", null)).isEqualTo(PlaceCategory.SIGHT);
        assertThat(classify(" SEA ", null)).isEqualTo(PlaceCategory.SEA);
    }

    @Test
    @DisplayName("분류 정보가 아예 없으면 기타")
    void nothing() {
        assertThat(classify(null, null)).isEqualTo(PlaceCategory.ETC);
        assertThat(classify("", " ")).isEqualTo(PlaceCategory.ETC);
    }

    @Test
    @DisplayName("숙소는 목록에서는 '숙소', 취향에서는 '숙박'. 순서는 맛집이 처음, 기타가 마지막")
    void labelsAndOrder() {
        assertThat(PlaceCategory.STAY.label()).isEqualTo("숙소");
        assertThat(PlaceCategory.STAY.tasteLabel()).isEqualTo("숙박");
        assertThat(PlaceCategory.FOOD.order()).isZero();
        assertThat(PlaceCategory.ETC.order()).isEqualTo(PlaceCategory.values().length - 1);
    }
}
