package com.example.pinkok_backend.gemini;

/** Gemini 호출 자체가 실패했을 때(네트워크, 5xx 등) — 재시도 대상. */
public class GeminiCallException extends RuntimeException {

    public GeminiCallException(String message, Throwable cause) {
        super(message, cause);
    }

    public GeminiCallException(String message) {
        super(message);
    }
}
