package com.example.pinkok_backend.gemini;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class GeminiGenerateResponse {

    private List<Candidate> candidates;
    private UsageMetadata usageMetadata;

    /** 모델이 실제로 답한 텍스트(JSON 문자열)만 뽑아준다. 답이 없으면 null. */
    public String firstText() {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        Content content = candidates.get(0).getContent();
        if (content == null || content.getParts() == null || content.getParts().isEmpty()) {
            return null;
        }
        return content.getParts().get(0).getText();
    }

    @Getter
    @Setter
    public static class Candidate {
        private Content content;
        private String finishReason;
    }

    @Getter
    @Setter
    public static class Content {
        private List<Part> parts;
    }

    @Getter
    @Setter
    public static class Part {
        private String text;
    }

    @Getter
    @Setter
    public static class UsageMetadata {
        private Integer promptTokenCount;
        private Integer candidatesTokenCount;
        private Integer totalTokenCount;
    }
}
