package com.example.pinkok_backend.service;

import com.example.pinkok_backend.dto.AiRequestCreateRequest;
import com.example.pinkok_backend.dto.AiRequestResponse;
import com.example.pinkok_backend.dto.ExtractedPlace;
import com.example.pinkok_backend.entity.AiRequest;
import com.example.pinkok_backend.entity.AiRequestImage;
import com.example.pinkok_backend.entity.Trip;
import com.example.pinkok_backend.entity.User;
import com.example.pinkok_backend.gemini.GeminiCallException;
import com.example.pinkok_backend.gemini.GeminiClient;
import com.example.pinkok_backend.gemini.GeminiGenerateRequest;
import com.example.pinkok_backend.gemini.GeminiGenerateResponse;
import com.example.pinkok_backend.gemini.GeminiPricing;
import com.example.pinkok_backend.gemini.PlaceExtractionPromptBuilder;
import com.example.pinkok_backend.repository.AiRequestImageRepository;
import com.example.pinkok_backend.repository.AiRequestRepository;
import com.example.pinkok_backend.repository.TripRepository;
import com.example.pinkok_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

/**
 * AI 장소 추출 (정훈 담당 — Gemini 호출 쪽).
 *
 * <p>여기서 만드는 건 "링크/글/스크린샷을 Gemini에 보내서 장소 이름 후보를 뽑아내는 것"까지다.
 * 그 후보를 카카오맵 좌표로 바꾸고 place_candidates 테이블에 저장하는 건 지은 담당
 * (PlaceCandidateService) — 이 클래스가 만든 AiRequest.status=SUCCESS + rawResponse(JSON)를
 * 읽어가서 처리하면 된다.
 */
@Service
public class AiRequestService {

    private static final String REQUEST_TYPE_PLACE_EXTRACT = "PLACE_EXTRACT";

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";

    private static final String INPUT_LINK = "LINK";
    private static final String INPUT_TEXT = "TEXT";
    private static final String INPUT_IMAGE = "IMAGE";

    private static final int MAX_IMAGES = 10;

    private final AiRequestRepository aiRequestRepository;
    private final AiRequestImageRepository aiRequestImageRepository;
    private final TripRepository tripRepository;
    private final UserRepository userRepository;
    private final GeminiClient geminiClient;
    private final FileService fileService;
    private final TaskExecutor aiTaskExecutor;
    private final ObjectMapper objectMapper;
    private final int maxRetries;

    public AiRequestService(AiRequestRepository aiRequestRepository,
                            AiRequestImageRepository aiRequestImageRepository,
                            TripRepository tripRepository,
                            UserRepository userRepository,
                            GeminiClient geminiClient,
                            FileService fileService,
                            TaskExecutor aiTaskExecutor,
                            ObjectMapper objectMapper,
                            @Value("${gemini.max-retries}") int maxRetries) {
        this.aiRequestRepository = aiRequestRepository;
        this.aiRequestImageRepository = aiRequestImageRepository;
        this.tripRepository = tripRepository;
        this.userRepository = userRepository;
        this.geminiClient = geminiClient;
        this.fileService = fileService;
        this.aiTaskExecutor = aiTaskExecutor;
        this.objectMapper = objectMapper;
        this.maxRetries = maxRetries;
    }

    /** 요청을 접수만 하고 바로 응답한다 (PENDING). 실제 Gemini 호출은 별도 스레드에서 비동기로 진행된다. */
    @Transactional
    public AiRequestResponse create(Long userId, AiRequestCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        Trip trip = null;
        if (request.getTripId() != null) {
            trip = tripRepository.findByIdAndDeletedAtIsNull(request.getTripId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "여행을 찾을 수 없습니다."));
        }

        String inputType = validateAndNormalizeInputType(request);

        AiRequest aiRequest = new AiRequest();
        aiRequest.setUser(user);
        aiRequest.setTrip(trip);
        aiRequest.setRequestType(REQUEST_TYPE_PLACE_EXTRACT);
        aiRequest.setInputType(inputType);
        aiRequest.setStatus(STATUS_PENDING);
        aiRequest.setRetryCount(0);
        aiRequest.setRequestedAt(LocalDateTime.now());

        if (inputType.equals(INPUT_LINK)) {
            aiRequest.setSourceUrl(request.getSourceUrl());
            aiRequest.setSourcePlatform(detectPlatform(request.getSourceUrl()));
        } else if (inputType.equals(INPUT_TEXT)) {
            aiRequest.setSourceText(request.getSourceText());
        }

        aiRequest = aiRequestRepository.save(aiRequest);

        if (inputType.equals(INPUT_IMAGE)) {
            saveImages(aiRequest, request.getImageUrls());
        }

        Long aiRequestId = aiRequest.getId();
        aiTaskExecutor.execute(() -> process(aiRequestId));

        // 테스트처럼 동기 실행 환경이면 위 execute() 가 이미 끝나 있을 수 있어 최신 상태를 다시 읽는다.
        AiRequest latest = aiRequestRepository.findById(aiRequestId).orElse(aiRequest);
        return AiRequestResponse.of(latest, parsePlaces(latest));
    }

    @Transactional(readOnly = true)
    public AiRequestResponse get(Long id, Long userId) {
        AiRequest aiRequest = getOwnedOrThrow(id, userId);
        return AiRequestResponse.of(aiRequest, parsePlaces(aiRequest));
    }

    @Transactional(readOnly = true)
    public List<AiRequestResponse> list(Long userId, Long tripId) {
        List<AiRequest> requests = tripId == null
                ? aiRequestRepository.findAllByUser_IdOrderByRequestedAtDesc(userId)
                : aiRequestRepository.findAllByUser_IdAndTrip_IdOrderByRequestedAtDesc(userId, tripId);
        return requests.stream()
                .map(r -> AiRequestResponse.of(r, parsePlaces(r)))
                .toList();
    }

    // ------------------------------------------------------------------
    // Gemini 호출 (비동기로 실행됨)
    // ------------------------------------------------------------------

    void process(Long aiRequestId) {
        AiRequest aiRequest = aiRequestRepository.findById(aiRequestId).orElse(null);
        if (aiRequest == null) {
            return;
        }

        aiRequest.setStatus(STATUS_PROCESSING);
        aiRequestRepository.save(aiRequest);

        String prompt = buildPrompt(aiRequest);
        aiRequest.setPromptText(prompt);

        GeminiGenerateRequest.Content content = new GeminiGenerateRequest.Content();
        content.setParts(buildParts(aiRequest, prompt));
        GeminiGenerateRequest geminiRequest = GeminiGenerateRequest.of(content, new GeminiGenerateRequest.GenerationConfig());

        int attempt = 0;
        while (true) {
            try {
                GeminiGenerateResponse response = geminiClient.generate(geminiRequest);
                onSuccess(aiRequest, response, attempt);
                return;
            } catch (GeminiCallException e) {
                if (attempt >= maxRetries) {
                    onFailure(aiRequest, attempt, "GEMINI_CALL_FAILED: " + rootMessage(e));
                    return;
                }
                attempt++;
            } catch (ResponseStatusException e) {
                // 키 미설정 등 재시도해도 소용없는 경우
                onFailure(aiRequest, attempt, e.getStatusCode() + " " + e.getReason());
                return;
            } catch (RuntimeException e) {
                // 응답 파싱 실패 등 예상 못한 오류 - 재시도 대상은 아님
                onFailure(aiRequest, attempt, "UNEXPECTED_ERROR: " + rootMessage(e));
                return;
            }
        }
    }

    private void onSuccess(AiRequest aiRequest, GeminiGenerateResponse response, int attempt) {
        GeminiGenerateResponse.UsageMetadata usage = response.getUsageMetadata();

        aiRequest.setModelName(geminiClient.modelName());
        aiRequest.setRetryCount(attempt);
        aiRequest.setRawResponse(response.firstText());
        if (usage != null) {
            aiRequest.setInputTokens(usage.getPromptTokenCount());
            aiRequest.setOutputTokens(usage.getCandidatesTokenCount());
            aiRequest.setCostUsd(GeminiPricing.estimateUsd(usage.getPromptTokenCount(), usage.getCandidatesTokenCount()));
        }
        aiRequest.setStatus(STATUS_SUCCESS);
        aiRequest.setCompletedAt(LocalDateTime.now());
        aiRequestRepository.save(aiRequest);
    }

    private void onFailure(AiRequest aiRequest, int attempt, String errorCode) {
        aiRequest.setModelName(geminiClient.modelName());
        aiRequest.setRetryCount(attempt);
        aiRequest.setStatus(STATUS_FAILED);
        aiRequest.setErrorCode(truncate(errorCode, 50));
        aiRequest.setCompletedAt(LocalDateTime.now());
        aiRequestRepository.save(aiRequest);
    }

    private String buildPrompt(AiRequest aiRequest) {
        return switch (aiRequest.getInputType()) {
            case INPUT_LINK -> PlaceExtractionPromptBuilder.forLink(aiRequest.getSourceUrl());
            case INPUT_TEXT -> PlaceExtractionPromptBuilder.forText(aiRequest.getSourceText());
            case INPUT_IMAGE -> PlaceExtractionPromptBuilder.forImages(
                    aiRequestImageRepository.findAllByAiRequest_IdOrderByDisplayOrderAsc(aiRequest.getId()).size());
            default -> throw new IllegalStateException("알 수 없는 inputType: " + aiRequest.getInputType());
        };
    }

    private List<GeminiGenerateRequest.Part> buildParts(AiRequest aiRequest, String prompt) {
        if (!INPUT_IMAGE.equals(aiRequest.getInputType())) {
            return List.of(GeminiGenerateRequest.Part.ofText(prompt));
        }

        List<AiRequestImage> images =
                aiRequestImageRepository.findAllByAiRequest_IdOrderByDisplayOrderAsc(aiRequest.getId());

        java.util.ArrayList<GeminiGenerateRequest.Part> parts = new java.util.ArrayList<>();
        parts.add(GeminiGenerateRequest.Part.ofText(prompt));
        for (AiRequestImage image : images) {
            FileService.ImageBytes bytes = fileService.readImageBytes(image.getFileUrl());
            String base64 = Base64.getEncoder().encodeToString(bytes.bytes());
            parts.add(GeminiGenerateRequest.Part.ofImage(bytes.mimeType(), base64));
        }
        return parts;
    }

    private List<ExtractedPlace> parsePlaces(AiRequest aiRequest) {
        if (!STATUS_SUCCESS.equals(aiRequest.getStatus()) || aiRequest.getRawResponse() == null) {
            return null;
        }
        try {
            return objectMapper.readValue(aiRequest.getRawResponse(), objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, ExtractedPlace.class));
        } catch (Exception e) {
            return List.of();
        }
    }

    // ------------------------------------------------------------------
    // 입력 검증 · 잡일
    // ------------------------------------------------------------------

    private String validateAndNormalizeInputType(AiRequestCreateRequest request) {
        String inputType = request.getInputType() == null ? "" : request.getInputType().toUpperCase(Locale.ROOT);

        switch (inputType) {
            case INPUT_LINK -> {
                if (!StringUtils.hasText(request.getSourceUrl())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sourceUrl 을 입력하세요.");
                }
            }
            case INPUT_TEXT -> {
                if (!StringUtils.hasText(request.getSourceText())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sourceText 를 입력하세요.");
                }
            }
            case INPUT_IMAGE -> {
                if (request.getImageUrls() == null || request.getImageUrls().isEmpty()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "imageUrls 를 1개 이상 입력하세요.");
                }
                if (request.getImageUrls().size() > MAX_IMAGES) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "스크린샷은 " + MAX_IMAGES + "장까지 보낼 수 있습니다.");
                }
            }
            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "inputType 은 LINK / TEXT / IMAGE 중 하나여야 합니다.");
        }
        return inputType;
    }

    private void saveImages(AiRequest aiRequest, List<String> imageUrls) {
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < imageUrls.size(); i++) {
            AiRequestImage image = new AiRequestImage();
            image.setAiRequest(aiRequest);
            image.setFileUrl(imageUrls.get(i));
            image.setDisplayOrder(i + 1);
            image.setCreatedAt(now);
            aiRequestImageRepository.save(image);
        }
    }

    private String detectPlatform(String url) {
        String lower = url.toLowerCase(Locale.ROOT);
        if (lower.contains("youtube.com") || lower.contains("youtu.be")) {
            return "YOUTUBE";
        }
        if (lower.contains("instagram.com")) {
            return "INSTAGRAM";
        }
        return "OTHER";
    }

    private AiRequest getOwnedOrThrow(Long id, Long userId) {
        AiRequest aiRequest = aiRequestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "AI 요청을 찾을 수 없습니다."));
        if (!aiRequest.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인의 AI 요청만 조회할 수 있습니다.");
        }
        return aiRequest;
    }

    private String rootMessage(Throwable e) {
        Throwable cause = e;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
    }

    private String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
