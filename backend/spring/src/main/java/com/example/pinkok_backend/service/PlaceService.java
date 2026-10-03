package com.example.pinkok_backend.service;

import com.example.pinkok_backend.dto.PlaceInput;
import com.example.pinkok_backend.dto.PlaceSearchPageResponse;
import com.example.pinkok_backend.dto.PlaceSearchResponse;
import com.example.pinkok_backend.entity.Place;
import com.example.pinkok_backend.kakao.KakaoKeywordSearchResponse;
import com.example.pinkok_backend.kakao.KakaoLocalApiClient;
import com.example.pinkok_backend.repository.PlaceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class PlaceService {

    private static final int MAX_PAGE_SIZE = 45; // 카카오 Local API 상한
    private static final int DEFAULT_PAGE_SIZE = 15;

    private final PlaceRepository placeRepository;
    private final KakaoLocalApiClient kakaoLocalApiClient;

    public PlaceService(PlaceRepository placeRepository, KakaoLocalApiClient kakaoLocalApiClient) {
        this.placeRepository = placeRepository;
        this.kakaoLocalApiClient = kakaoLocalApiClient;
    }

    /** 카카오맵 키워드 장소 검색. 결과를 그대로 저장하는 게 아니라, 골라서 핀으로 추가할 때 저장된다. */
    public PlaceSearchPageResponse search(String keyword, Integer page, Integer size) {
        if (!StringUtils.hasText(keyword)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "검색어를 입력하세요.");
        }

        int safePage = (page == null || page < 1) ? 1 : page;
        int safeSize = (size == null || size < 1) ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);

        KakaoKeywordSearchResponse response = kakaoLocalApiClient.searchByKeyword(keyword, safePage, safeSize);

        List<PlaceSearchResponse> places = response.getDocuments() == null
                ? Collections.emptyList()
                : response.getDocuments().stream().map(PlaceSearchResponse::from).toList();
        boolean hasMore = response.getMeta() != null && !response.getMeta().isEnd();

        return new PlaceSearchPageResponse(places, hasMore);
    }

    /** 이름 유사도가 이 값 미만이면 "다른 장소"로 보고 채택하지 않는다 (0~1). */
    private static final double MIN_NAME_SIMILARITY = 0.5;
    private static final int LOOKUP_RESULT_SIZE = 5;

    /** {@link #lookup} 결과. match 가 null 이면 못 찾은 것이고, closestCandidateName 은 그나마 가장 가까웠던 결과 이름(없을 수 있음). */
    public record PlaceLookup(PlaceInput match, String closestCandidateName) {
    }

    /**
     * AI 가 준 장소명을 카카오맵 장소로 바꾼다. 질의를 앞에서부터 차례로 시도해서, 결과 중 {@code expectedName} 과
     * 이름이 비슷한 첫 장소를 쓴다.
     *
     * <p>검색 1순위를 그냥 쓰면 안 된다: AI 추천은 폐업·상호 변경·환각이 흔해서, 같은 주소에 있는 전혀 다른 가게
     * (예: '카페 노티드 제주애월' -> '놀맨')가 조용히 일정에 들어가 버린다. 이름이 비슷하지 않으면 채택하지 않는다.
     * (저장은 하지 않는다 - 핀으로 추가할 때 findOrCreate 가 저장한다)
     *
     * @throws ResponseStatusException 카카오 호출 자체가 실패한 경우(키 문제 등)는 그대로 올라간다
     */
    public PlaceLookup lookup(String expectedName, String... queries) {
        String closestName = null;
        double closestScore = -1;

        for (String query : queries) {
            if (!StringUtils.hasText(query)) {
                continue;
            }
            KakaoKeywordSearchResponse response = kakaoLocalApiClient.searchByKeyword(query.trim(), 1, LOOKUP_RESULT_SIZE);
            if (response == null || response.getDocuments() == null) {
                continue;
            }
            // 이 질의의 결과 중 이름이 가장 가까운 것(동점이면 검색 순위가 빠른 것)을 고른다
            KakaoKeywordSearchResponse.Document best = null;
            double bestScore = -1;
            for (KakaoKeywordSearchResponse.Document doc : response.getDocuments()) {
                double score = nameSimilarity(expectedName, doc.getPlaceName());
                if (score > bestScore) {
                    bestScore = score;
                    best = doc;
                }
            }
            if (best != null && bestScore >= MIN_NAME_SIMILARITY) {
                return new PlaceLookup(toPlaceInput(best), best.getPlaceName());
            }
            if (best != null && bestScore > closestScore) {
                closestScore = bestScore;
                closestName = best.getPlaceName();
            }
        }
        return new PlaceLookup(null, closestName);
    }

    /**
     * 공백·기호·대소문자를 무시하고 두 이름이 얼마나 비슷한지(0~1).
     * 완전히 같으면 1, 한쪽이 다른 쪽을 포함하면(지점명·접두어 차이) 0.9, 아니면 글자 2개 단위(bigram) 겹침 비율(Dice 계수).
     * 같은 이름이 "성산일출봉" 과 "성산일출봉 매표소" 처럼 여러 개 나왔을 때 완전히 같은 쪽을 고르려고 구분했다.
     */
    static double nameSimilarity(String a, String b) {
        String x = normalizeForMatch(a);
        String y = normalizeForMatch(b);
        if (x.isEmpty() || y.isEmpty()) {
            return 0;
        }
        if (x.equals(y)) {
            return 1;
        }
        if (x.contains(y) || y.contains(x)) {
            return 0.9;
        }
        if (x.length() < 2 || y.length() < 2) {
            return 0;
        }
        java.util.Map<String, Integer> bigramsOfX = new java.util.HashMap<>();
        for (int i = 0; i < x.length() - 1; i++) {
            bigramsOfX.merge(x.substring(i, i + 2), 1, Integer::sum);
        }
        int common = 0;
        for (int i = 0; i < y.length() - 1; i++) {
            String bigram = y.substring(i, i + 2);
            Integer remaining = bigramsOfX.get(bigram);
            if (remaining != null && remaining > 0) {
                common++;
                bigramsOfX.put(bigram, remaining - 1);
            }
        }
        return 2.0 * common / ((x.length() - 1) + (y.length() - 1));
    }

    private static String normalizeForMatch(String name) {
        return name == null ? "" : name.replaceAll("[\\s\\p{Punct}]+", "").toLowerCase(java.util.Locale.ROOT);
    }

    private PlaceInput toPlaceInput(KakaoKeywordSearchResponse.Document doc) {
        PlaceInput input = new PlaceInput();
        input.setKakaoPlaceId(doc.getId());
        input.setName(doc.getPlaceName());
        input.setRoadAddress(doc.getRoadAddressName());
        input.setLotAddress(doc.getAddressName());
        input.setLatitude(new java.math.BigDecimal(doc.getY()));
        input.setLongitude(new java.math.BigDecimal(doc.getX()));
        input.setCategoryGroupCode(doc.getCategoryGroupCode());
        input.setCategoryName(doc.getCategoryName());
        input.setPhone(doc.getPhone());
        input.setPlaceUrl(doc.getPlaceUrl());
        return input;
    }

    /**
     * kakaoPlaceId 가 이미 있는 장소면 그걸 재사용하고, 없으면 새로 만든다.
     * 카카오맵 검색이 붙기 전까지는 프론트가 kakaoPlaceId 없이 직접 입력해서 써도 된다
     * (그 경우 매번 새 장소로 저장됨).
     */
    @Transactional
    public Place findOrCreate(PlaceInput input) {
        if (StringUtils.hasText(input.getKakaoPlaceId())) {
            return placeRepository.findByKakaoPlaceId(input.getKakaoPlaceId())
                    .orElseGet(() -> create(input));
        }
        return create(input);
    }

    private Place create(PlaceInput input) {
        LocalDateTime now = LocalDateTime.now();

        Place place = new Place();
        place.setKakaoPlaceId(input.getKakaoPlaceId());
        place.setName(input.getName());
        place.setRoadAddress(input.getRoadAddress());
        place.setLotAddress(input.getLotAddress());
        place.setLatitude(input.getLatitude());
        place.setLongitude(input.getLongitude());
        place.setCategoryGroupCode(input.getCategoryGroupCode());
        place.setCategoryName(input.getCategoryName());
        place.setPhone(input.getPhone());
        place.setPlaceUrl(input.getPlaceUrl());
        place.setCreatedAt(now);
        place.setUpdatedAt(now);

        return placeRepository.save(place);
    }
}
