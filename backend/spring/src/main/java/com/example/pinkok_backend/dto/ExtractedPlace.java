package com.example.pinkok_backend.dto;

/**
 * Gemini가 뽑아낸 장소 후보 하나. 아직 places/place_candidates 테이블에 저장되기 전 단계 —
 * 카카오맵 좌표 변환 + 후보 저장(place_candidates)은 이 결과를 받아서 처리한다.
 *
 * <p>lat/lng 는 Gemini가 스스로 추정한 좌표라 정확하지 않을 수 있다 — 카카오 지오코딩이
 * 실패했을 때 쓰는 폴백용으로만 쓸 것 (검증된 프로토타입의 geocodeService.js 방식과 동일).
 */
public record ExtractedPlace(
        String name,
        String address,
        String category,
        String description,
        Double lat,
        Double lng) {
}
