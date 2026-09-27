package com.example.pinkok_backend.dto;

/**
 * Gemini가 뽑아낸 장소 후보 하나. 아직 places/place_candidates 테이블에 저장되기 전 단계 —
 * 카카오맵 좌표 변환 + 후보 저장(place_candidates)은 이 결과를 받아서 처리한다.
 */
public record ExtractedPlace(String name, String address, String category, Double confidence) {
}
