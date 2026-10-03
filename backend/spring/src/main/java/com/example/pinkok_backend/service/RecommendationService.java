package com.example.pinkok_backend.service;

import com.example.pinkok_backend.dto.AiRecommendationResult;
import com.example.pinkok_backend.dto.AiRequestResponse;
import com.example.pinkok_backend.dto.ItineraryItemCreateRequest;
import com.example.pinkok_backend.dto.ItineraryItemResponse;
import com.example.pinkok_backend.dto.PlaceInput;
import com.example.pinkok_backend.dto.RecommendationCreateRequest;
import com.example.pinkok_backend.dto.RecommendationResponse;
import com.example.pinkok_backend.entity.AiRequest;
import com.example.pinkok_backend.entity.Recommendation;
import com.example.pinkok_backend.entity.Trip;
import com.example.pinkok_backend.entity.TravelStyle;
import com.example.pinkok_backend.entity.User;
import com.example.pinkok_backend.entity.UserSetting;
import com.example.pinkok_backend.gemini.GeminiGenerateRequest;
import com.example.pinkok_backend.gemini.RecommendationPromptBuilder;
import com.example.pinkok_backend.repository.AiRequestRepository;
import com.example.pinkok_backend.repository.ItineraryItemRepository;
import com.example.pinkok_backend.repository.PlaceRepository;
import com.example.pinkok_backend.repository.RecommendationRepository;
import com.example.pinkok_backend.repository.TravelStyleRepository;
import com.example.pinkok_backend.repository.TripRepository;
import com.example.pinkok_backend.repository.TripTravelStyleRepository;
import com.example.pinkok_backend.repository.UserRepository;
import com.example.pinkok_backend.repository.UserSettingRepository;
import com.example.pinkok_backend.repository.UserTravelStyleRepository;
import com.example.pinkok_backend.security.TripAccessGuard;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * AI 맞춤 추천 (정훈 담당 — 동반자·취향 기반).
 *
 * <p>흐름: 추천 요청(POST) -> AiRequest(RECOMMEND) 접수 후 즉시 응답 -> 비동기로 Gemini 호출 ->
 * 성공하면 recommendations 테이블에 SUGGESTED 로 저장 -> 사용자가 수락하면 카카오맵에서 좌표를 찾아
 * 일정에 핀으로 추가(ACCEPTED), 거절하면 REJECTED (같은 장소는 다음 추천에서 제외).
 * 진행 상태는 장소 추출과 같은 GET /ai-requests/{id} 로 폴링한다.
 */
@Service
public class RecommendationService {

    private static final String REQUEST_TYPE_RECOMMEND = "RECOMMEND";
    private static final String INPUT_TYPE_TEXT = "TEXT";

    private static final String STATUS_SUGGESTED = "SUGGESTED";
    private static final String STATUS_ACCEPTED = "ACCEPTED";
    private static final String STATUS_REJECTED = "REJECTED";

    private static final int DEFAULT_COUNT = 5;
    private static final int MAX_COUNT = 10;

    private static final Map<String, String> COMPANION_LABELS = Map.of(
            "SOLO", "혼자",
            "COUPLE", "커플",
            "FAMILY", "가족",
            "FRIEND", "친구");

    /** 요청마다 달라지는 조건. 비동기 스레드로 넘기려고 한데 묶었다. */
    private record Params(String companionType, List<String> styleCodes, int count) {
    }

    private final AiRequestRepository aiRequestRepository;
    private final TripRepository tripRepository;
    private final UserRepository userRepository;
    private final UserSettingRepository userSettingRepository;
    private final TripTravelStyleRepository tripTravelStyleRepository;
    private final UserTravelStyleRepository userTravelStyleRepository;
    private final TravelStyleRepository travelStyleRepository;
    private final ItineraryItemRepository itineraryItemRepository;
    private final RecommendationRepository recommendationRepository;
    private final PlaceRepository placeRepository;
    private final TripAccessGuard tripAccessGuard;
    private final GeminiJobRunner geminiJobRunner;
    private final PlaceService placeService;
    private final ItineraryItemService itineraryItemService;
    private final TaskExecutor aiTaskExecutor;
    private final ObjectMapper objectMapper;

    public RecommendationService(AiRequestRepository aiRequestRepository,
                                 TripRepository tripRepository,
                                 UserRepository userRepository,
                                 UserSettingRepository userSettingRepository,
                                 TripTravelStyleRepository tripTravelStyleRepository,
                                 UserTravelStyleRepository userTravelStyleRepository,
                                 TravelStyleRepository travelStyleRepository,
                                 ItineraryItemRepository itineraryItemRepository,
                                 RecommendationRepository recommendationRepository,
                                 PlaceRepository placeRepository,
                                 TripAccessGuard tripAccessGuard,
                                 GeminiJobRunner geminiJobRunner,
                                 PlaceService placeService,
                                 ItineraryItemService itineraryItemService,
                                 TaskExecutor aiTaskExecutor,
                                 ObjectMapper objectMapper) {
        this.aiRequestRepository = aiRequestRepository;
        this.tripRepository = tripRepository;
        this.userRepository = userRepository;
        this.userSettingRepository = userSettingRepository;
        this.tripTravelStyleRepository = tripTravelStyleRepository;
        this.userTravelStyleRepository = userTravelStyleRepository;
        this.travelStyleRepository = travelStyleRepository;
        this.itineraryItemRepository = itineraryItemRepository;
        this.recommendationRepository = recommendationRepository;
        this.placeRepository = placeRepository;
        this.tripAccessGuard = tripAccessGuard;
        this.geminiJobRunner = geminiJobRunner;
        this.placeService = placeService;
        this.itineraryItemService = itineraryItemService;
        this.aiTaskExecutor = aiTaskExecutor;
        this.objectMapper = objectMapper;
    }

    /**
     * 추천 요청을 접수하고 바로 응답한다 (PENDING).
     *
     * <p>AiRequestService.create 와 같은 이유로 일부러 @Transactional 을 안 붙였다 - 트랜잭션으로 감싸면
     * 비동기 스레드가 커밋 전에 먼저 돌아서 방금 만든 요청을 못 찾고 PENDING 에 멈춘다.
     */
    public AiRequestResponse create(Long userId, RecommendationCreateRequest request) {
        Long tripId = request.getTripId();
        tripAccessGuard.requireMember(tripId, userId);

        Trip trip = tripRepository.findByIdAndDeletedAtIsNull(tripId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "여행을 찾을 수 없습니다."));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        Params params = new Params(
                normalizeCompanion(request.getCompanionType()),
                normalizeStyleCodes(request.getStyleCodes()),
                normalizeCount(request.getCount()));

        AiRequest aiRequest = new AiRequest();
        aiRequest.setUser(user);
        aiRequest.setTrip(trip);
        aiRequest.setRequestType(REQUEST_TYPE_RECOMMEND);
        aiRequest.setInputType(INPUT_TYPE_TEXT);
        aiRequest.setSourceText("추천 요청 | 동행=" + orDash(params.companionType())
                + " | 스타일=" + (params.styleCodes().isEmpty() ? "-" : String.join(",", params.styleCodes()))
                + " | 개수=" + params.count());
        aiRequest.setStatus(GeminiJobRunner.STATUS_PENDING);
        aiRequest.setRetryCount(0);
        aiRequest.setRequestedAt(LocalDateTime.now());
        aiRequest = aiRequestRepository.save(aiRequest);

        Long aiRequestId = aiRequest.getId();
        aiTaskExecutor.execute(() -> process(aiRequestId, params));

        // 테스트처럼 동기 실행 환경이면 위 execute() 가 이미 끝나 있을 수 있어 최신 상태를 다시 읽는다.
        AiRequest latest = aiRequestRepository.findById(aiRequestId).orElse(aiRequest);
        return AiRequestResponse.of(latest, null);
    }

    @Transactional(readOnly = true)
    public List<RecommendationResponse> list(Long tripId, Long userId) {
        tripAccessGuard.requireMember(tripId, userId);
        return recommendationRepository.findAllByTrip_IdOrderByCreatedAtDescIdDesc(tripId).stream()
                .map(RecommendationResponse::from)
                .toList();
    }

    /**
     * 추천을 수락한다: 카카오맵에서 좌표를 찾아 일정에 핀으로 추가하고 ACCEPTED 로 바꾼다.
     * 지도에서 못 찾으면 422 로 거절하고 추천은 SUGGESTED 로 남는다 (다시 시도하거나 거절할 수 있게).
     */
    @Transactional
    public RecommendationResponse accept(Long id, Long userId, Long dayId) {
        Recommendation recommendation = getOrThrow(id);
        Trip trip = recommendation.getTrip();
        tripAccessGuard.requireMember(trip.getId(), userId);
        requireSuggested(recommendation);

        PlaceInput place = placeService.findFirstByKeyword(buildMapQueries(trip, recommendation))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_ENTITY, "카카오맵에서 '" + recommendation.getSuggestedName() + "' 을(를) 찾지 못했어요."));

        ItineraryItemCreateRequest itemRequest = new ItineraryItemCreateRequest();
        itemRequest.setTripId(trip.getId());
        itemRequest.setDayId(dayId);
        itemRequest.setPlace(place);
        ItineraryItemResponse item = itineraryItemService.create(userId, itemRequest, ItineraryItemService.ADDED_BY_AI);

        recommendation.setPlace(placeRepository.getReferenceById(item.getPlace().getId()));
        recommendation.setStatus(STATUS_ACCEPTED);
        recommendationRepository.save(recommendation);

        return RecommendationResponse.accepted(recommendation, item.getId());
    }

    @Transactional
    public RecommendationResponse reject(Long id, Long userId) {
        Recommendation recommendation = getOrThrow(id);
        tripAccessGuard.requireMember(recommendation.getTrip().getId(), userId);
        requireSuggested(recommendation);

        recommendation.setStatus(STATUS_REJECTED);
        return RecommendationResponse.from(recommendationRepository.save(recommendation));
    }

    // ------------------------------------------------------------------
    // Gemini 호출 (비동기로 실행됨)
    // ------------------------------------------------------------------

    void process(Long aiRequestId, Params params) {
        AiRequest aiRequest = aiRequestRepository.findById(aiRequestId).orElse(null);
        if (aiRequest == null) {
            return;
        }

        Long tripId;
        List<String> excludeNames;
        List<GeminiGenerateRequest.Part> parts;
        try {
            // 이 스레드는 트랜잭션 밖이라 lazy 연관(aiRequest.getTrip() 등)은 id 외엔 못 읽는다 - id 로 다시 조회한다
            tripId = aiRequest.getTrip().getId();
            Long userId = aiRequest.getUser().getId();
            Trip trip = tripRepository.findById(tripId)
                    .orElseThrow(() -> new IllegalStateException("여행을 찾을 수 없습니다."));

            String companion = firstText(
                    params.companionType(),
                    trip.getCompanionType(),
                    userSettingRepository.findById(userId).map(UserSetting::getDefaultCompanionType).orElse(null));

            excludeNames = Stream.concat(
                            itineraryItemRepository.findPlaceNamesByTripId(tripId).stream(),
                            recommendationRepository.findSuggestedNamesByTripId(tripId).stream())
                    .distinct()
                    .toList();

            String prompt = RecommendationPromptBuilder.build(
                    trip.getRegion(),
                    describePeriod(trip.getStartDate(), trip.getEndDate()),
                    companion == null ? null : COMPANION_LABELS.get(companion),
                    resolveStyleNames(params, tripId, userId),
                    excludeNames,
                    params.count());
            parts = List.of(GeminiGenerateRequest.Part.ofText(prompt));
        } catch (RuntimeException e) {
            geminiJobRunner.markFailed(aiRequest, 0, "INPUT_ERROR: " + e.getMessage());
            return;
        }

        if (!geminiJobRunner.run(aiRequest, parts)) {
            return;
        }

        try {
            saveRecommendations(aiRequest, tripId, params.count(), excludeNames);
        } catch (RuntimeException e) {
            // 호출은 성공했지만 응답이 약속한 JSON 형식이 아닌 경우 - 토큰·비용 기록은 남기고 실패로 끝낸다
            int retries = aiRequest.getRetryCount() == null ? 0 : aiRequest.getRetryCount();
            geminiJobRunner.markFailed(aiRequest, retries, "PARSE_ERROR: " + e.getMessage());
        }
    }

    private void saveRecommendations(AiRequest aiRequest, Long tripId, int count, List<String> excludeNames) {
        AiRecommendationResult result =
                objectMapper.readValue(stripCodeFence(aiRequest.getRawResponse()), AiRecommendationResult.class);
        if (result == null || result.recommendations() == null) {
            throw new IllegalStateException("추천 목록이 없는 응답입니다.");
        }

        Set<String> seen = new LinkedHashSet<>();
        excludeNames.forEach(name -> seen.add(normalizeName(name)));

        Trip tripRef = tripRepository.getReferenceById(tripId);
        AiRequest aiRequestRef = aiRequestRepository.getReferenceById(aiRequest.getId());
        LocalDateTime now = LocalDateTime.now();

        List<Recommendation> rows = new ArrayList<>();
        for (AiRecommendationResult.RecommendedPlace place : result.recommendations()) {
            if (rows.size() >= count) {
                break;
            }
            if (place == null || !StringUtils.hasText(place.name())) {
                continue;
            }
            // 프롬프트로 막아도 모델이 이미 있는 장소를 또 주거나 응답 안에서 중복될 수 있어 한 번 더 거른다
            if (!seen.add(normalizeName(place.name()))) {
                continue;
            }

            Recommendation row = new Recommendation();
            row.setTrip(tripRef);
            row.setAiRequest(aiRequestRef);
            row.setSuggestedName(truncate(place.name().trim(), 200));
            row.setSuggestedAddress(blankToNull(truncate(place.address(), 300)));
            row.setCategoryText(blankToNull(truncate(place.category(), 100)));
            row.setReason(blankToNull(truncate(place.reason(), 500)));
            row.setStatus(STATUS_SUGGESTED);
            row.setCreatedAt(now);
            rows.add(row);
        }
        recommendationRepository.saveAll(rows);
    }

    // ------------------------------------------------------------------
    // 조건 해석 · 검증
    // ------------------------------------------------------------------

    private String normalizeCompanion(String companionType) {
        if (!StringUtils.hasText(companionType)) {
            return null;
        }
        String upper = companionType.trim().toUpperCase(Locale.ROOT);
        if (!COMPANION_LABELS.containsKey(upper)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "companionType 은 SOLO / COUPLE / FAMILY / FRIEND 중 하나여야 합니다.");
        }
        return upper;
    }

    private List<String> normalizeStyleCodes(List<String> styleCodes) {
        if (styleCodes == null || styleCodes.isEmpty()) {
            return List.of();
        }
        List<String> codes = styleCodes.stream()
                .filter(StringUtils::hasText)
                .map(code -> code.trim().toUpperCase(Locale.ROOT))
                .distinct()
                .toList();
        if (travelStyleRepository.findAllByCodeIn(codes).size() != codes.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "알 수 없는 styleCodes 가 있습니다. GET /travel-styles 의 code 를 쓰세요.");
        }
        return codes;
    }

    private int normalizeCount(Integer count) {
        if (count == null) {
            return DEFAULT_COUNT;
        }
        if (count < 1 || count > MAX_COUNT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "count 는 1~" + MAX_COUNT + " 사이여야 합니다.");
        }
        return count;
    }

    /** 요청에 스타일이 있으면 그걸, 없으면 여행에 저장된 스타일, 그것도 없으면 내 취향을 쓴다. */
    private List<String> resolveStyleNames(Params params, Long tripId, Long userId) {
        if (!params.styleCodes().isEmpty()) {
            return travelStyleRepository.findAllByCodeIn(params.styleCodes()).stream()
                    .map(TravelStyle::getName)
                    .toList();
        }
        List<String> tripStyles = tripTravelStyleRepository.findStyleNamesByTripId(tripId);
        if (!tripStyles.isEmpty()) {
            return tripStyles;
        }
        return userTravelStyleRepository.findStyleNamesByUserId(userId);
    }

    private String describePeriod(LocalDate start, LocalDate end) {
        if (start == null || end == null || end.isBefore(start)) {
            return null;
        }
        long nights = ChronoUnit.DAYS.between(start, end);
        return nights == 0 ? "당일치기 (" + start + ")" : nights + "박 " + (nights + 1) + "일 (" + start + " ~ " + end + ")";
    }

    /** 카카오맵 검색 질의 후보: 지역+이름 -> 이름만 -> 주소 순으로 시도한다. */
    private String[] buildMapQueries(Trip trip, Recommendation recommendation) {
        List<String> queries = new ArrayList<>();
        String name = recommendation.getSuggestedName();
        if (StringUtils.hasText(trip.getRegion())) {
            queries.add(trip.getRegion().trim() + " " + name);
        }
        queries.add(name);
        if (StringUtils.hasText(recommendation.getSuggestedAddress())) {
            queries.add(recommendation.getSuggestedAddress());
        }
        return queries.toArray(new String[0]);
    }

    private Recommendation getOrThrow(Long id) {
        return recommendationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "추천을 찾을 수 없습니다."));
    }

    private void requireSuggested(Recommendation recommendation) {
        if (!STATUS_SUGGESTED.equals(recommendation.getStatus())) {
            throw new IllegalArgumentException("이미 처리된 추천입니다. (" + recommendation.getStatus() + ")");
        }
    }

    // ------------------------------------------------------------------
    // 문자열 잡일
    // ------------------------------------------------------------------

    private static String stripCodeFence(String text) {
        if (text == null) {
            return "";
        }
        return text.trim()
                .replaceFirst("^```(?:json)?\\s*", "")
                .replaceFirst("\\s*```$", "")
                .trim();
    }

    private static String normalizeName(String name) {
        return name == null ? "" : name.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }

    private static String firstText(String... candidates) {
        for (String candidate : candidates) {
            if (StringUtils.hasText(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static String orDash(String value) {
        return value == null ? "-" : value;
    }

    private static String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
