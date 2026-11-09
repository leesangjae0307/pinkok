package com.example.pinkok_backend.gemini;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

/** Gemini generateContent API 호출만 담당하는 얇은 클라이언트. */
@Component
public class GeminiClient {

    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    /**
     * @param readTimeoutMs 응답을 기다리는 최대 시간. Gemini 는 답을 한 번에 돌려주므로 "생성이 끝날 때까지"가
     *                      곧 이 시간이다. 정하지 않으면 응답이 안 올 때 요청이 PROCESSING 에 영원히 멈춘다.
     *                      무료 키는 2분이 넘게 걸릴 때가 있어서 기본값을 넉넉히 잡았다.
     */
    public GeminiClient(
            @Value("${gemini.base-url}") String baseUrl,
            @Value("${gemini.api-key}") String apiKey,
            @Value("${gemini.model}") String model,
            @Value("${gemini.connect-timeout-ms:10000}") long connectTimeoutMs,
            @Value("${gemini.read-timeout-ms:180000}") long readTimeoutMs) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
        this.apiKey = apiKey;
        this.model = model;
    }

    public GeminiGenerateResponse generate(GeminiGenerateRequest request) {
        if (!StringUtils.hasText(apiKey)) {
            throw new ResponseStatusException(
                    SERVICE_UNAVAILABLE, "Gemini API 키가 설정되지 않았습니다. GEMINI_API_KEY 환경변수를 확인하세요.");
        }

        try {
            return restClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1beta/models/{model}:generateContent")
                            .queryParam("key", apiKey)
                            .build(model))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(GeminiGenerateResponse.class);
        } catch (RestClientException e) {
            throw new GeminiCallException("Gemini 호출에 실패했습니다.", e);
        }
    }

    public String modelName() {
        return model;
    }
}
