package com.example.pinkok_backend.kakao;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

/**
 * 카카오 Local API(키워드 장소 검색, 주소 검색) 호출만 담당하는 얇은 클라이언트.
 * REST API 키는 서버 대 서버 호출이라 플랫폼(도메인) 등록 없이 헤더만으로 동작한다.
 */
@Component
public class KakaoLocalApiClient {

    private static final String KEYWORD_SEARCH_PATH = "/v2/local/search/keyword.json";
    private static final String ADDRESS_SEARCH_PATH = "/v2/local/search/address.json";

    private final RestClient restClient;
    private final String restApiKey;

    public KakaoLocalApiClient(
            @Value("${kakao.local-api-base-url}") String baseUrl,
            @Value("${kakao.rest-api-key}") String restApiKey,
            @Value("${kakao.connect-timeout-ms:3000}") long connectTimeoutMs,
            @Value("${kakao.read-timeout-ms:10000}") long readTimeoutMs) {
        // 장소 후보는 장소마다 카카오를 부르므로, 한 번이 멈추면 추출 전체가 PROCESSING 에 멈춘다
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
        this.restApiKey = restApiKey;
    }

    public KakaoKeywordSearchResponse searchByKeyword(String keyword, int page, int size) {
        return get(KEYWORD_SEARCH_PATH, keyword, page, size, KakaoKeywordSearchResponse.class, "카카오 장소 검색");
    }

    /**
     * 주소로 좌표 찾기. 도로명("강릉시 임영로207번길 6-1")·지번 주소 모두 된다.
     * 카카오맵에 가게로 등록되지 않은 곳(에어비앤비 숙소 등)을 일정에 넣을 때 쓴다.
     */
    public KakaoAddressSearchResponse searchByAddress(String address, int page, int size) {
        return get(ADDRESS_SEARCH_PATH, address, page, size, KakaoAddressSearchResponse.class, "카카오 주소 검색");
    }

    private <T> T get(String path, String query, int page, int size, Class<T> type, String what) {
        if (!StringUtils.hasText(restApiKey)) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "카카오 API 키가 설정되지 않았습니다. KAKAO_REST_API_KEY 환경변수를 확인하세요.");
        }

        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(path)
                            .queryParam("query", query)
                            .queryParam("page", page)
                            .queryParam("size", size)
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + restApiKey)
                    .retrieve()
                    .body(type);
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, what + " 호출에 실패했습니다.", e);
        }
    }
}
