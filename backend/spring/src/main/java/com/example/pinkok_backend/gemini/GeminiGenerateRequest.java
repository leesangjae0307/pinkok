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
        private FileData fileData;

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

        /** 유튜브 링크처럼, 파일을 올리지 않고 URI만으로 Gemini에게 직접 보여줄 때. */
        public static Part ofFileUri(String fileUri, String mimeType) {
            Part part = new Part();
            FileData data = new FileData();
            data.setFileUri(fileUri);
            data.setMimeType(mimeType);
            part.setFileData(data);
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
    public static class FileData {
        private String mimeType;
        private String fileUri;
    }

    @Getter
    @Setter
    public static class GenerationConfig {
        private String responseMimeType = "application/json";
        private Double temperature = 0.1;
    }
}
