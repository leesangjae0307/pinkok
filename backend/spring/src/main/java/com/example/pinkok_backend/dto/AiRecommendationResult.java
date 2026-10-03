package com.example.pinkok_backend.dto;

import java.util.List;

/** Gemini 추천 응답 그대로의 모양. AiRequest.rawResponse 에 이 JSON 이 저장된다. */
public record AiRecommendationResult(List<RecommendedPlace> recommendations) {

    public record RecommendedPlace(String name, String address, String category, String reason) {
    }
}
