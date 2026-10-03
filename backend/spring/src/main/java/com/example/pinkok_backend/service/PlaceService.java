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
import java.util.Optional;

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

    /**
     * 장소 이름(+지역 등)으로 카카오맵에서 가장 먼저 나오는 장소 1건을 찾는다.
     * 질의를 앞에서부터 차례로 시도해서 결과가 나오는 첫 질의의 첫 결과를 쓴다 - 정확한 이름이 안 먹히면
     * 다음 질의(예: 지역을 뺀 이름만)로 넘어가는 용도. AI가 준 장소명을 좌표로 바꿀 때 쓴다.
     * (저장은 하지 않는다 - 핀으로 추가할 때 findOrCreate 가 저장한다)
     *
     * @return 못 찾으면 empty. 카카오 호출 자체가 실패하면(키 문제 등) 예외가 그대로 올라간다.
     */
    public Optional<PlaceInput> findFirstByKeyword(String... queries) {
        for (String query : queries) {
            if (!StringUtils.hasText(query)) {
                continue;
            }
            KakaoKeywordSearchResponse response = kakaoLocalApiClient.searchByKeyword(query.trim(), 1, 1);
            if (response != null && response.getDocuments() != null && !response.getDocuments().isEmpty()) {
                return Optional.of(toPlaceInput(response.getDocuments().get(0)));
            }
        }
        return Optional.empty();
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
