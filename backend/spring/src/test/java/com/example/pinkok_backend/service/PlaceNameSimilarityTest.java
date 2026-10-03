package com.example.pinkok_backend.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** AI 가 준 장소명과 카카오맵 검색 결과가 "같은 곳인지" 가르는 이름 유사도 (스프링 없이 순수 계산). */
class PlaceNameSimilarityTest {

    private static final double THRESHOLD = 0.5;

    @Test
    @DisplayName("한쪽이 다른 쪽을 포함하면 같은 곳으로 본다 (지점명·접두어 차이) - 단, 완전히 같은 것보다는 낮다")
    void containment_isSamePlaceButRanksBelowExact() {
        assertEquals(0.9, PlaceService.nameSimilarity("동문시장", "제주동문시장"));
        assertEquals(0.9, PlaceService.nameSimilarity("애월 한담해변", "한담해변"));
        assertEquals(0.9, PlaceService.nameSimilarity("경복궁", "경복궁 광화문"));
        assertEquals(1.0, PlaceService.nameSimilarity("경복궁", "경복궁"));
    }

    @Test
    @DisplayName("공백·기호·대소문자는 무시한다")
    void ignoresWhitespacePunctuationAndCase() {
        assertEquals(1.0, PlaceService.nameSimilarity("아베베 베이커리", "아베베베이커리"));
        assertEquals(1.0, PlaceService.nameSimilarity("Cafe-Delmundo", "cafe delmundo"));
    }

    @Test
    @DisplayName("살짝 다르게 적힌 같은 가게는 기준을 넘긴다")
    void slightlyDifferentSpelling_passesThreshold() {
        assertTrue(PlaceService.nameSimilarity("스타벅스 강남역점", "스타벅스 강남R점") >= THRESHOLD);
        assertTrue(PlaceService.nameSimilarity("아베베 베이커리", "아베베베이커리 제주") >= THRESHOLD);
    }

    @Test
    @DisplayName("전혀 다른 가게는 기준에 못 미친다 (실제로 나온 '카페 노티드 제주애월' vs '놀맨')")
    void unrelatedPlaces_failThreshold() {
        assertTrue(PlaceService.nameSimilarity("카페 노티드 제주애월", "놀맨") < THRESHOLD);
        assertTrue(PlaceService.nameSimilarity("고집돌우럭 중문점", "스타벅스 중문점") < THRESHOLD);
    }

    @Test
    @DisplayName("비어 있거나 한 글자짜리는 안전하게 0")
    void emptyOrTooShort_isZero() {
        assertEquals(0.0, PlaceService.nameSimilarity(null, "경복궁"));
        assertEquals(0.0, PlaceService.nameSimilarity("경복궁", ""));
        assertEquals(0.0, PlaceService.nameSimilarity("가", "나"));
    }
}
