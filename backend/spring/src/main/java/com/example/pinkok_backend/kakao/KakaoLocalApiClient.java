package com.example.pinkok_backend.kakao;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

/**
 * 카카오 Local API(키워드 장소 검색) 호출만 담당하는 얇은 클라이언트.
 * REST API 키는 서버 대 서버 호출이라 플랫폼(도메인) 등록 없이 헤더만으로 동작한다.
 */
@Component
public class KakaoLocalApiClient {

    private static final String KEYWORD_SEARCH_PATH = "/v2/local/search/keyword.json";

    private final RestClient restClient;
    private final String restApiKey;

    public KakaoLocalApiClient(
            @Value("${kakao.local-api-base-url}") String baseUrl,
            @Value("${kakao.rest-api-key}") String restApiKey) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.restApiKey = restApiKey;
    }

    public KakaoKeywordSearchResponse searchByKeyword(String keyword, int page, int size) {
        if (!StringUtils.hasText(restApiKey)) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "카카오 API 키가 설정되지 않았습니다. KAKAO_REST_API_KEY 환경변수를 확인하세요.");
        }

        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(KEYWORD_SEARCH_PATH)
                            .queryParam("query", keyword)
                            .queryParam("page", page)
                            .queryParam("size", size)
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + restApiKey)
                    .retrieve()
                    .body(KakaoKeywordSearchResponse.class);
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "카카오 장소 검색 호출에 실패했습니다.", e);
        }
    }
}
