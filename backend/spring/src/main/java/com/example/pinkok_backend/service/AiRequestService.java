package com.example.pinkok_backend.service;

import com.example.pinkok_backend.dto.AiExtractionResult;
import com.example.pinkok_backend.dto.AiRequestCreateRequest;
import com.example.pinkok_backend.dto.AiRequestResponse;
import com.example.pinkok_backend.entity.AiRequest;
import com.example.pinkok_backend.entity.AiRequestImage;
import com.example.pinkok_backend.entity.Trip;
import com.example.pinkok_backend.entity.User;
import com.example.pinkok_backend.gemini.GeminiGenerateRequest;
import com.example.pinkok_backend.gemini.PlaceExtractionPromptBuilder;
import com.example.pinkok_backend.repository.AiRequestImageRepository;
import com.example.pinkok_backend.repository.AiRequestRepository;
import com.example.pinkok_backend.repository.TripRepository;
import com.example.pinkok_backend.repository.UserRepository;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
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

    private static final String STATUS_PENDING = GeminiJobRunner.STATUS_PENDING;
    private static final String STATUS_SUCCESS = GeminiJobRunner.STATUS_SUCCESS;

    private static final String INPUT_LINK = "LINK";
    private static final String INPUT_TEXT = "TEXT";
    private static final String INPUT_IMAGE = "IMAGE";

    private static final String PLATFORM_YOUTUBE = "YOUTUBE";
    private static final String PLATFORM_INSTAGRAM = "INSTAGRAM";
    private static final String PLATFORM_OTHER = "OTHER";

    /** 유튜브는 Gemini가 영상을 직접 분석할 수 있어서(fileData) mp4로 취급한다. */
    private static final String YOUTUBE_MIME_TYPE = "video/mp4";

    private static final int MAX_IMAGES = 10;

    private final AiRequestRepository aiRequestRepository;
    private final AiRequestImageRepository aiRequestImageRepository;
    private final TripRepository tripRepository;
    private final UserRepository userRepository;
    private final GeminiJobRunner geminiJobRunner;
    private final FileService fileService;
    private final TaskExecutor aiTaskExecutor;
    private final ObjectMapper objectMapper;

    public AiRequestService(AiRequestRepository aiRequestRepository,
                            AiRequestImageRepository aiRequestImageRepository,
                            TripRepository tripRepository,
                            UserRepository userRepository,
                            GeminiJobRunner geminiJobRunner,
                            FileService fileService,
                            TaskExecutor aiTaskExecutor,
                            ObjectMapper objectMapper) {
        this.aiRequestRepository = aiRequestRepository;
        this.aiRequestImageRepository = aiRequestImageRepository;
        this.tripRepository = tripRepository;
        this.userRepository = userRepository;
        this.geminiJobRunner = geminiJobRunner;
        this.fileService = fileService;
        this.aiTaskExecutor = aiTaskExecutor;
        this.objectMapper = objectMapper;
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
        return AiRequestResponse.of(latest, parseResult(latest));
    }

    @Transactional(readOnly = true)
    public AiRequestResponse get(Long id, Long userId) {
        AiRequest aiRequest = getOwnedOrThrow(id, userId);
        return AiRequestResponse.of(aiRequest, parseResult(aiRequest));
    }

    @Transactional(readOnly = true)
    public List<AiRequestResponse> list(Long userId, Long tripId) {
        List<AiRequest> requests = tripId == null
                ? aiRequestRepository.findAllByUser_IdOrderByRequestedAtDesc(userId)
                : aiRequestRepository.findAllByUser_IdAndTrip_IdOrderByRequestedAtDesc(userId, tripId);
        return requests.stream()
                .map(r -> AiRequestResponse.of(r, parseResult(r)))
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

        List<GeminiGenerateRequest.Part> parts;
        try {
            parts = buildParts(aiRequest);
        } catch (RuntimeException e) {
            // 업로드한 이미지를 못 읽는 등 Gemini 호출 전에 실패한 경우에도 PENDING 에 멈추지 않고 FAILED 로 끝낸다
            geminiJobRunner.markFailed(aiRequest, 0, "INPUT_ERROR: " + e.getMessage());
            return;
        }

        geminiJobRunner.run(aiRequest, parts);
    }

    /**
     * 입력 방식별로 Gemini에 보낼 parts를 만든다. 프롬프트 문구 자체는
     * {@link PlaceExtractionPromptBuilder}의 검증된 문구를 그대로 쓴다.
     */
    private List<GeminiGenerateRequest.Part> buildParts(AiRequest aiRequest) {
        return switch (aiRequest.getInputType()) {
            case INPUT_LINK -> buildLinkParts(aiRequest);
            case INPUT_TEXT -> List.of(GeminiGenerateRequest.Part.ofText(
                    PlaceExtractionPromptBuilder.forText(aiRequest.getSourceText())));
            case INPUT_IMAGE -> buildImageParts(aiRequest);
            default -> throw new IllegalStateException("알 수 없는 inputType: " + aiRequest.getInputType());
        };
    }

    /**
     * 유튜브면 링크를 텍스트로 설명만 하는 게 아니라, fileData로 Gemini에게 영상을 직접 보여준다
     * (Gemini의 유튜브 URL 지원 기능 — 검증된 프로토타입 방식). 그 외 링크(인스타 등)는
     * Gemini가 열어볼 수 없으므로 URL 문자열만으로 추정하게 한다 - 정확도가 낮을 수 있음.
     */
    private List<GeminiGenerateRequest.Part> buildLinkParts(AiRequest aiRequest) {
        if (PLATFORM_YOUTUBE.equals(aiRequest.getSourcePlatform())) {
            return List.of(
                    GeminiGenerateRequest.Part.ofFileUri(aiRequest.getSourceUrl(), YOUTUBE_MIME_TYPE),
                    GeminiGenerateRequest.Part.ofText(PlaceExtractionPromptBuilder.EXTRACTION_PROMPT));
        }
        return List.of(GeminiGenerateRequest.Part.ofText(
                PlaceExtractionPromptBuilder.forNonYoutubeLink(aiRequest.getSourceUrl())));
    }

    private List<GeminiGenerateRequest.Part> buildImageParts(AiRequest aiRequest) {
        List<AiRequestImage> images =
                aiRequestImageRepository.findAllByAiRequest_IdOrderByDisplayOrderAsc(aiRequest.getId());

        List<GeminiGenerateRequest.Part> parts = new ArrayList<>();
        for (AiRequestImage image : images) {
            FileService.ImageBytes bytes = fileService.readImageBytes(image.getFileUrl());
            String base64 = Base64.getEncoder().encodeToString(bytes.bytes());
            parts.add(GeminiGenerateRequest.Part.ofImage(bytes.mimeType(), base64));
        }
        parts.add(GeminiGenerateRequest.Part.ofText(PlaceExtractionPromptBuilder.EXTRACTION_PROMPT));
        return parts;
    }

    private AiExtractionResult parseResult(AiRequest aiRequest) {
        // 추천(RECOMMEND) 같은 다른 종류 요청은 rawResponse 모양이 달라서 장소 추출 결과로 읽으면 안 된다
        if (!REQUEST_TYPE_PLACE_EXTRACT.equals(aiRequest.getRequestType())
                || !STATUS_SUCCESS.equals(aiRequest.getStatus())
                || aiRequest.getRawResponse() == null) {
            return null;
        }
        try {
            return objectMapper.readValue(aiRequest.getRawResponse(), AiExtractionResult.class);
        } catch (Exception e) {
            return AiExtractionResult.empty();
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
            return PLATFORM_YOUTUBE;
        }
        if (lower.contains("instagram.com")) {
            return PLATFORM_INSTAGRAM;
        }
        return PLATFORM_OTHER;
    }

    private AiRequest getOwnedOrThrow(Long id, Long userId) {
        AiRequest aiRequest = aiRequestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "AI 요청을 찾을 수 없습니다."));
        if (!aiRequest.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인의 AI 요청만 조회할 수 있습니다.");
        }
        return aiRequest;
    }
}
