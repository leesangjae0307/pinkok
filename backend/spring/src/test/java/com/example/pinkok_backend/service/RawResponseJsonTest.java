package com.example.pinkok_backend.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RawResponseJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("평범한 JSON 은 그대로 둔다")
    void plainJson_unchanged() {
        assertEquals("{\"a\":1}", RawResponseJson.unwrap(objectMapper, " {\"a\":1} "));
    }

    @Test
    @DisplayName("JSON 컬럼에서 한 겹 더 감싸져 돌아온 문자열은 벗겨낸다")
    void wrappedJson_isUnwrapped() {
        assertEquals("{\"a\":\"b\"}", RawResponseJson.unwrap(objectMapper, "\"{\\\"a\\\":\\\"b\\\"}\""));
    }
}
