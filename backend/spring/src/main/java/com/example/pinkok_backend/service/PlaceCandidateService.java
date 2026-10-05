package com.example.pinkok_backend.service;

import com.example.pinkok_backend.dto.AddCandidatesToTripRequest;
import com.example.pinkok_backend.dto.AddCandidatesToTripResponse;
import com.example.pinkok_backend.dto.AiExtractionResult;
import com.example.pinkok_backend.dto.ExtractedPlace;
import com.example.pinkok_backend.dto.ItineraryItemResponse;
import com.example.pinkok_backend.dto.PlaceCandidateResponse;
import com.example.pinkok_backend.dto.PlaceInput;
import com.example.pinkok_backend.entity.AiRequest;
import com.example.pinkok_backend.entity.AiRequestImage;
import com.example.pinkok_backend.entity.ItineraryItem;
import com.example.pinkok_backend.entity.Place;
import com.example.pinkok_backend.entity.PlaceCandidate;
import com.example.pinkok_backend.entity.Trip;
import com.example.pinkok_backend.kakao.KakaoKeywordSearchResponse;
import com.example.pinkok_backend.kakao.KakaoLocalApiClient;
import com.example.pinkok_backend.repository.AiRequestImageRepository;
import com.example.pinkok_backend.repository.AiRequestRepository;
import com.example.pinkok_backend.repository.ItineraryItemRepository;
import com.example.pinkok_backend.repository.PlaceCandidateRepository;
import com.example.pinkok_backend.repository.TripRepository;
import com.example.pinkok_backend.security.TripAccessGuard;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * AI가 뽑은 장소 이름을 실제 좌표로 바꿔서 후보로 저장하고, 사용자가 고른 것만 여행 지도에 핀으로 꽂는다.
 *
 * <p>정훈이 만든 {@link AiRequestService}는 "링크/글/스크린샷 → 장소 이름"까지만 한다.
 * 그 결과({@code AiRequest.rawResponse})에는 좌표가 없어서 지도에 찍을 수 없으므로,
 * 여기서 카카오맵으로 좌표를 찾아 {@code place_candidates}에 저장한다.
 *
 * <p>AI는 틀릴 수 있어서 바로 일정에 넣지 않고 <b>후보(검수 대상)</b>로 한 단계 거친다 —
 * 사용자가 체크한 것만 {@link #addToTrip}으로 실제 핀이 된다.
 */
@Service
public class PlaceCandidateService {

    private static final Logger log = LoggerFactory.getLogger(PlaceCandidateService.class);

    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String ADDED_BY_AI = "AI";

    /** 후보 하나를 찾으려고 카카오에 물어볼 때 받아올 결과 수 (첫 번째만 쓴다). */
    private static final int SEARCH_SIZE = 5;

    /** 이름이 얼마나 맞는지 (클수록 가깝다) */
    private static final int NAME_SCORE_EXACT = 3;
    private static final int NAME_SCORE_PARTIAL = 2;
    private static final int NAME_SCORE_LOOSE = 1;

    private static final BigDecimal CONFIDENCE_EXACT = new BigDecimal("1.000");
    private static final BigDecimal CONFIDENCE_PARTIAL = new BigDecimal("0.800");
    private static final BigDecimal CONFIDENCE_LOOSE = new BigDecimal("0.600");
    private static final BigDecimal CONFIDENCE_NOT_FOUND = new BigDecimal("0.300");

    private final PlaceCandidateRepository placeCandidateRepository;
    private final AiRequestRepository aiRequestRepository;
    private final AiRequestImageRepository aiRequestImageRepository;
    private final ItineraryItemRepository itineraryItemRepository;
    private final TripRepository tripRepository;
    private final PlaceService placeService;
    private final KakaoLocalApiClient kakaoLocalApiClient;
    private final TripAccessGuard tripAccessGuard;
    private final ObjectMapper objectMapper;

    public PlaceCandidateService(PlaceCandidateRepository placeCandidateRepository,
                                 AiRequestRepository aiRequestRepository,
                                 AiRequestImageRepository aiRequestImageRepository,
                                 ItineraryItemRepository itineraryItemRepository,
                                 TripRepository tripRepository,
                                 PlaceService placeService,
                                 KakaoLocalApiClient kakaoLocalApiClient,
                                 TripAccessGuard tripAccessGuard,
                                 ObjectMapper objectMapper) {
        this.placeCandidateRepository = placeCandidateRepository;
        this.aiRequestRepository = aiRequestRepository;
        this.aiRequestImageRepository = aiRequestImageRepository;
        this.itineraryItemRepository = itineraryItemRepository;
        this.tripRepository = tripRepository;
        this.placeService = placeService;
        this.kakaoLocalApiClient = kakaoLocalApiClient;
        this.tripAccessGuard = tripAccessGuard;
        this.objectMapper = objectMapper;
    }

    // ------------------------------------------------------------------
    // 후보 만들기 (AI 추출이 성공한 직후 자동으로 불린다)
    // ------------------------------------------------------------------

    /**
     * AI 추출 결과를 읽어 좌표를 붙인 후보로 저장한다. 이미 만들어 둔 요청이면 아무 것도 하지 않는다.
     *
     * <p><b>어떤 경우에도 예외를 밖으로 던지지 않는다.</b> 이 메서드는 Gemini 호출이 성공한 직후
     * 같은 흐름에서 불리는데, 카카오맵이 죽었다는 이유로 이미 돈을 내고 받아온 AI 결과까지
     * 실패 처리되면 안 되기 때문이다. 실패하면 후보가 비어 있을 뿐이고,
     * {@code POST /place-candidates?aiRequestId=} 로 다시 시도할 수 있다.
     */
    public void createFromQuietly(Long aiRequestId) {
        try {
            createFrom(aiRequestId);
        } catch (RuntimeException e) {
            log.warn("장소 후보 생성 실패 (aiRequestId={}). 후보 없이 넘어간다.", aiRequestId, e);
        }
    }

    /** 실제 후보 생성. 실패를 호출한 쪽에 알려야 하는 재시도 API에서 쓴다. */
    public void createFrom(Long aiRequestId) {
        if (placeCandidateRepository.existsByAiRequest_Id(aiRequestId)) {
            return;
        }

        AiRequest aiRequest = aiRequestRepository.findById(aiRequestId).orElse(null);
        if (aiRequest == null || !STATUS_SUCCESS.equals(aiRequest.getStatus())) {
            return;
        }

        List<ExtractedPlace> places = parsePlaces(aiRequest);
        if (places.isEmpty()) {
            return;
        }

        // Gemini 응답은 "어느 사진에서 나온 장소"인지 알려주지 않는다.
        // 그래서 스크린샷이 한 장뿐일 때만 출처를 확실히 알 수 있어 연결한다.
        List<AiRequestImage> images = aiRequestImageRepository.findAllByAiRequest_IdOrderByDisplayOrderAsc(aiRequestId);
        AiRequestImage sourceImage = images.size() == 1 ? images.get(0) : null;

        LocalDateTime now = LocalDateTime.now();
        List<PlaceCandidate> candidates = new ArrayList<>();
        for (int i = 0; i < places.size(); i++) {
            ExtractedPlace extracted = places.get(i);
            if (!StringUtils.hasText(extracted.name())) {
                continue;
            }

            // 카카오가 장애면 여기서 예외가 나가고, 후보를 하나도 저장하지 않는다.
            // "검색 결과가 없다"와 "카카오가 죽었다"를 구분하기 위해서다 — 전자는 진짜 없는 것이고,
            // 후자는 나중에 다시 시도하면 찾을 수 있는 것이라 반쪽짜리 목록을 남기면 안 된다.
            Matched matched = geocode(extracted);

            PlaceCandidate candidate = new PlaceCandidate();
            candidate.setAiRequest(aiRequest);
            candidate.setSourceImage(sourceImage);
            candidate.setPlace(matched == null ? null : matched.place());
            candidate.setRawName(truncate(extracted.name(), 200));
            candidate.setRawAddress(truncate(extracted.address(), 300));
            candidate.setCategoryText(truncate(extracted.category(), 100));
            candidate.setDescription(extracted.description());
            candidate.setConfidence(matched == null ? CONFIDENCE_NOT_FOUND : matched.confidence());
            candidate.setIsSelected(false);
            candidate.setDisplayOrder(i + 1);
            candidate.setCreatedAt(now);
            candidates.add(candidate);
        }

        placeCandidateRepository.saveAll(candidates);
    }

    /**
     * 후보 이름을 카카오맵에서 찾아 좌표가 있는 장소로 바꾼다.
     *
     * <p>"이름 + 주소"로 먼저 찾고, 안 나오면 이름만으로 한 번 더 찾는다.
     * AI가 준 주소가 부정확할 때가 있어서, 주소 때문에 못 찾는 경우를 구제하기 위한 것이다.
     *
     * <p><b>검색 1순위를 그냥 쓰면 안 된다.</b> 실제로 강릉 여행의 "동해바다열차"가 경기도 부천의
     * 여행사로 매칭된 적이 있다. 이름만 비슷하면 전혀 다른 지역 가게가 조용히 일정에 들어간다.
     * 그래서 결과 여러 개를 놓고 <b>지역이 맞는지</b> 먼저 보고, 그 안에서 이름이 가장 가까운 것을 고른다.
     */
    private Matched geocode(ExtractedPlace extracted) {
        String name = extracted.name().trim();
        String region = extractRegion(extracted.address());
        String withAddress = StringUtils.hasText(extracted.address())
                ? name + " " + extracted.address().trim()
                : name;

        Matched matched = bestMatch(name, region, search(withAddress));
        if (matched == null && !withAddress.equals(name)) {
            matched = bestMatch(name, region, search(name));
        }
        return matched;
    }

    /**
     * 검색 결과 중에서 쓸 만한 것을 고른다.
     *
     * <ul>
     *   <li>AI가 지역(시/군/구)을 알려줬으면 <b>그 지역에 있는 결과만</b> 쓴다. 하나도 없으면 "못 찾음"으로 둔다 —
     *       엉뚱한 지역을 넣느니 사용자가 직접 검색하게 하는 편이 낫다.</li>
     *   <li>지역을 모를 때는 검증할 방법이 없으므로 <b>이름이 확실히 비슷할 때만</b> 쓴다.</li>
     * </ul>
     */
    private Matched bestMatch(String name, String region, List<KakaoKeywordSearchResponse.Document> documents) {
        KakaoKeywordSearchResponse.Document best = null;
        int bestScore = 0;

        for (KakaoKeywordSearchResponse.Document document : documents) {
            if (region != null && !inRegion(document, region)) {
                continue;
            }
            int score = nameScore(name, document.getPlaceName());
            if (score > bestScore) {
                bestScore = score;
                best = document;
            }
        }

        if (best == null) {
            return null;
        }
        // 지역을 확인하지 못했는데 이름까지 애매하면 채택하지 않는다 (엉뚱한 곳이 들어오는 경로)
        if (region == null && bestScore <= NAME_SCORE_LOOSE) {
            return null;
        }
        return new Matched(placeService.findOrCreate(toPlaceInput(best)), confidenceOf(bestScore, region != null));
    }

    private List<KakaoKeywordSearchResponse.Document> search(String keyword) {
        KakaoKeywordSearchResponse response = kakaoLocalApiClient.searchByKeyword(keyword, 1, SEARCH_SIZE);
        if (response == null || response.getDocuments() == null) {
            return List.of();
        }
        return response.getDocuments();
    }

    /**
     * AI가 준 주소에서 시/군/구를 뽑아낸다. ("강원 강릉시 강릉역" → "강릉시")
     * 이게 매칭을 검증하는 기준이 된다.
     */
    private String extractRegion(String address) {
        if (!StringUtils.hasText(address)) {
            return null;
        }
        for (String token : address.trim().split("\\s+")) {
            if (token.length() >= 2 && (token.endsWith("시") || token.endsWith("군") || token.endsWith("구"))) {
                return token;
            }
        }
        return null;
    }

    /** 카카오 결과의 주소(지번·도로명)에 그 시/군/구가 들어 있는지. */
    private boolean inRegion(KakaoKeywordSearchResponse.Document document, String region) {
        return contains(document.getAddressName(), region) || contains(document.getRoadAddressName(), region);
    }

    private boolean contains(String address, String region) {
        return address != null && address.contains(region);
    }

    /**
     * 카카오에서 찾은 이름이 AI가 뽑은 이름과 얼마나 맞아떨어지는지. 클수록 가깝다.
     * 띄어쓰기·대소문자는 무시한다 ("협재 해수욕장" = "협재해수욕장").
     */
    private int nameScore(String rawName, String matchedName) {
        String a = normalize(rawName);
        String b = normalize(matchedName);
        if (a.isEmpty() || b.isEmpty()) {
            return NAME_SCORE_LOOSE;
        }
        if (a.equals(b)) {
            return NAME_SCORE_EXACT;
        }
        if (a.contains(b) || b.contains(a)) {
            return NAME_SCORE_PARTIAL;
        }
        return NAME_SCORE_LOOSE;
    }

    /**
     * 신뢰도. 이름이 얼마나 맞는지에 더해, <b>지역까지 확인됐는지</b>를 반영한다.
     * 지역을 확인하지 못한 매칭은 한 단계 낮춰서 앱이 "확인 필요"로 보여줄 수 있게 한다.
     */
    private BigDecimal confidenceOf(int nameScore, boolean regionVerified) {
        if (nameScore == NAME_SCORE_EXACT) {
            return regionVerified ? CONFIDENCE_EXACT : CONFIDENCE_PARTIAL;
        }
        if (nameScore == NAME_SCORE_PARTIAL) {
            return regionVerified ? CONFIDENCE_PARTIAL : CONFIDENCE_LOOSE;
        }
        return CONFIDENCE_LOOSE;
    }

    /** 띄어쓰기·대소문자 차이로 "다른 이름"이 되지 않게 맞춰준다. ("협재 해수욕장" = "협재해수욕장") */
    private String normalize(String value) {
        return value == null ? "" : value.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }

    private PlaceInput toPlaceInput(KakaoKeywordSearchResponse.Document document) {
        PlaceInput input = new PlaceInput();
        input.setKakaoPlaceId(document.getId());
        input.setName(document.getPlaceName());
        input.setRoadAddress(document.getRoadAddressName());
        input.setLotAddress(document.getAddressName());
        // 카카오는 x=경도, y=위도 로 준다 (반대로 넣으면 지구 반대편에 핀이 꽂힌다)
        input.setLatitude(new BigDecimal(document.getY()));
        input.setLongitude(new BigDecimal(document.getX()));
        input.setCategoryGroupCode(document.getCategoryGroupCode());
        input.setCategoryName(document.getCategoryName());
        input.setPhone(document.getPhone());
        input.setPlaceUrl(document.getPlaceUrl());
        return input;
    }

    // ------------------------------------------------------------------
    // 조회 · 재시도
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<PlaceCandidateResponse> list(Long aiRequestId, Long userId) {
        requireOwnedRequest(aiRequestId, userId);
        return placeCandidateRepository.findAllByAiRequestId(aiRequestId).stream()
                .map(PlaceCandidateResponse::from)
                .toList();
    }

    /**
     * 후보 만들기를 다시 시도한다 (자동 생성이 카카오 장애 등으로 실패했을 때).
     * 이미 후보가 있으면 그대로 돌려주기만 하고 새로 만들지 않는다.
     */
    public List<PlaceCandidateResponse> createAndList(Long aiRequestId, Long userId) {
        AiRequest aiRequest = requireOwnedRequest(aiRequestId, userId);
        if (!STATUS_SUCCESS.equals(aiRequest.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "AI 추출이 아직 끝나지 않았거나 실패한 요청입니다. (status=" + aiRequest.getStatus() + ")");
        }
        createFrom(aiRequestId);
        return list(aiRequestId, userId);
    }

    // ------------------------------------------------------------------
    // 여행에 핀으로 추가
    // ------------------------------------------------------------------

    /**
     * 체크한 후보들을 여행 지도에 핀으로 꽂는다.
     *
     * <p>핀은 <b>날짜 미배정</b>으로 들어간다. 어느 날에 갈지는 사용자가 일정 화면에서 정하는 것이고,
     * AI가 임의로 날짜를 정하면 오히려 되돌리는 일이 늘기 때문이다.
     */
    @Transactional
    public AddCandidatesToTripResponse addToTrip(Long userId, AddCandidatesToTripRequest request) {
        tripAccessGuard.requireMember(request.getTripId(), userId);
        Trip trip = tripRepository.findByIdAndDeletedAtIsNull(request.getTripId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "여행을 찾을 수 없습니다."));

        Set<Long> ids = new LinkedHashSet<>(request.getCandidateIds());
        List<PlaceCandidate> candidates = placeCandidateRepository.findAllByIdIn(ids);
        if (candidates.size() != ids.size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "후보를 찾을 수 없습니다.");
        }

        LocalDateTime now = LocalDateTime.now();
        List<ItineraryItemResponse> added = new ArrayList<>();
        List<Long> skippedDuplicate = new ArrayList<>();
        List<Long> skippedNoCoordinate = new ArrayList<>();
        Set<Long> addedPlaceIds = new HashSet<>();

        for (PlaceCandidate candidate : candidates) {
            if (!candidate.getAiRequest().getUser().getId().equals(userId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인의 AI 요청에서 나온 후보만 추가할 수 있습니다.");
            }

            Place place = candidate.getPlace();
            if (place == null) {
                skippedNoCoordinate.add(candidate.getId());
                continue;
            }
            // 같은 장소가 이미 이 여행에 있으면 건너뛴다 (같은 캡처를 두 번 올린 경우 등).
            // addedPlaceIds 는 이번 요청 안에서 같은 장소가 두 번 들어오는 경우까지 막는다.
            if (!addedPlaceIds.add(place.getId())
                    || itineraryItemRepository.existsByTrip_IdAndPlace_Id(trip.getId(), place.getId())) {
                skippedDuplicate.add(candidate.getId());
                continue;
            }

            ItineraryItem item = new ItineraryItem();
            item.setTrip(trip);
            item.setDay(null);              // 날짜 미배정 핀
            item.setPlace(place);
            item.setCandidate(candidate);   // 어느 AI 후보에서 왔는지 남겨둔다
            item.setVisitOrder(null);
            item.setAddedBy(ADDED_BY_AI);
            item.setCreatedAt(now);
            added.add(ItineraryItemResponse.from(itineraryItemRepository.save(item)));

            candidate.setIsSelected(true);
            placeCandidateRepository.save(candidate);
        }

        return new AddCandidatesToTripResponse(added, skippedDuplicate, skippedNoCoordinate);
    }

    // ------------------------------------------------------------------
    // 잡일
    // ------------------------------------------------------------------

    private AiRequest requireOwnedRequest(Long aiRequestId, Long userId) {
        AiRequest aiRequest = aiRequestRepository.findById(aiRequestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "AI 요청을 찾을 수 없습니다."));
        if (!aiRequest.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인의 AI 요청만 조회할 수 있습니다.");
        }
        return aiRequest;
    }

    private List<ExtractedPlace> parsePlaces(AiRequest aiRequest) {
        if (aiRequest.getRawResponse() == null) {
            return List.of();
        }
        try {
            AiExtractionResult result = objectMapper.readValue(unwrapJson(aiRequest.getRawResponse()), AiExtractionResult.class);
            return result.places() == null ? List.of() : result.places();
        } catch (Exception e) {
            log.warn("AI 응답을 읽지 못했다 (aiRequestId={})", aiRequest.getId(), e);
            return List.of();
        }
    }

    /**
     * raw_response 는 DB의 JSON 컬럼에 저장되는데, 넣었다가 다시 읽으면 JSON 전체가
     * 문자열 하나로 한 겹 더 감싸져 돌아오는 경우가 있다(H2가 그렇다).
     * 그대로 파싱하면 "문자열을 객체로 못 바꾼다"며 실패하므로, 따옴표로 시작하면 한 겹 벗겨낸 뒤 파싱한다.
     */
    private String unwrapJson(String raw) {
        String trimmed = raw.trim();
        if (trimmed.startsWith("\"")) {
            return objectMapper.readValue(trimmed, String.class);
        }
        return trimmed;
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }

    /** 카카오맵에서 찾아낸 장소와, 그 결과를 얼마나 믿을 수 있는지. */
    private record Matched(Place place, BigDecimal confidence) {
    }
}
