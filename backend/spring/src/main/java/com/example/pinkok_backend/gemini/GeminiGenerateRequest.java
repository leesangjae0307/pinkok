package com.example.pinkok_backend.gemini;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Gemini generateContent 요청 바디. 필드명이 그대로 camelCase JSON으로 나가므로
 * (카카오 응답과 달리) 별도 @JsonProperty 매핑이 필요 없다.
 */
@Getter
@Setter
public class GeminiGenerateRequest {

    private List<Content> contents;
    private GenerationConfig generationConfig;

    public static GeminiGenerateRequest of(Content content, GenerationConfig config) {
        GeminiGenerateRequest request = new GeminiGenerateRequest();
        request.setContents(List.of(content));
        request.setGenerationConfig(config);
        return request;
    }

    @Getter
    @Setter
    public static class Content {
        private String role = "user";
        private List<Part> parts;
    }

    @Getter
    @Setter
    public static class Part {
        private String text;
        private InlineData inlineData;

        public static Part ofText(String text) {
            Part part = new Part();
            part.setText(text);
            return part;
        }

        public static Part ofImage(String mimeType, String base64Data) {
            Part part = new Part();
            InlineData data = new InlineData();
            data.setMimeType(mimeType);
            data.setData(base64Data);
            part.setInlineData(data);
            return part;
        }
    }

    @Getter
    @Setter
    public static class InlineData {
        private String mimeType;
        private String data;
    }

    @Getter
    @Setter
    public static class GenerationConfig {
        private String responseMimeType = "application/json";
        private Double temperature = 0.2;
    }
}
