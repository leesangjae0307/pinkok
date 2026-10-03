package com.example.pinkok_backend.support;

import com.example.pinkok_backend.gemini.GeminiGenerateResponse;

import java.util.List;

/** 테스트에서 Gemini 응답을 흉내 낼 때 쓰는 도우미. */
public final class GeminiTestSupport {

    private GeminiTestSupport() {
    }

    /** 모델이 {@code json} 텍스트로 답하고 토큰을 이만큼 썼다고 치는 성공 응답. */
    public static GeminiGenerateResponse successResponse(String json, int promptTokens, int outputTokens) {
        GeminiGenerateResponse.Part part = new GeminiGenerateResponse.Part();
        part.setText(json);
        GeminiGenerateResponse.Content content = new GeminiGenerateResponse.Content();
        content.setParts(List.of(part));
        GeminiGenerateResponse.Candidate candidate = new GeminiGenerateResponse.Candidate();
        candidate.setContent(content);
        candidate.setFinishReason("STOP");

        GeminiGenerateResponse.UsageMetadata usage = new GeminiGenerateResponse.UsageMetadata();
        usage.setPromptTokenCount(promptTokens);
        usage.setCandidatesTokenCount(outputTokens);
        usage.setTotalTokenCount(promptTokens + outputTokens);

        GeminiGenerateResponse response = new GeminiGenerateResponse();
        response.setCandidates(List.of(candidate));
        response.setUsageMetadata(usage);
        return response;
    }
}
