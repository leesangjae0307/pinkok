package com.example.pinkok_backend.service;

import tools.jackson.databind.ObjectMapper;

/**
 * ai_requests.raw_response 는 JSON 컬럼이라, 넣었다가 다시 읽으면 JSON 전체가 문자열 하나로 한 겹 더
 * 감싸져 돌아오는 경우가 있다(H2가 그렇다). 그대로 파싱하면 실패하므로 따옴표로 시작하면 한 겹 벗긴다.
 */
final class RawResponseJson {

    private RawResponseJson() {
    }

    static String unwrap(ObjectMapper objectMapper, String raw) {
        String trimmed = raw.trim();
        if (trimmed.startsWith("\"")) {
            return objectMapper.readValue(trimmed, String.class);
        }
        return trimmed;
    }
}
