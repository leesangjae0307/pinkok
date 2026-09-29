package com.example.pinkok_backend.dto;

import java.util.List;

/**
 * Gemini 응답 그대로의 모양. {@code AiRequest.rawResponse} 에는 이 객체를 JSON으로 저장한다.
 */
public record AiExtractionResult(String title, List<ExtractedPlace> places) {

    public static AiExtractionResult empty() {
        return new AiExtractionResult(null, List.of());
    }
}
