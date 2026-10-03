package com.example.pinkok_backend.service;

import com.example.pinkok_backend.entity.AiRequest;
import com.example.pinkok_backend.gemini.GeminiCallException;
import com.example.pinkok_backend.gemini.GeminiClient;
import com.example.pinkok_backend.gemini.GeminiGenerateRequest;
import com.example.pinkok_backend.gemini.GeminiGenerateResponse;
import com.example.pinkok_backend.gemini.GeminiPricing;
import com.example.pinkok_backend.repository.AiRequestRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AiRequest 한 건을 "PROCESSING -> Gemini 호출(재시도) -> 결과·토큰·비용·실패 기록" 까지 처리하는 공통 실행기.
 *
 * <p>장소 추출(AiRequestService), 맞춤 추천(RecommendationService) 등 Gemini를 쓰는 기능이 전부 이걸 쓴다.
 * 호출 이력(AiRequest)을 같은 방식으로 남겨야 비용 집계와 오류 추적이 한 곳에서 된다.
 * 도메인별 후처리(추출 결과를 추천 row로 저장하는 등)는 {@link #run} 이 true 를 돌려줄 때 호출한 쪽이 한다.
 */
@Component
public class GeminiJobRunner {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";

    private final AiRequestRepository aiRequestRepository;
    private final GeminiClient geminiClient;
    private final int maxRetries;

    public GeminiJobRunner(AiRequestRepository aiRequestRepository,
                           GeminiClient geminiClient,
                           @Value("${gemini.max-retries}") int maxRetries) {
        this.aiRequestRepository = aiRequestRepository;
        this.geminiClient = geminiClient;
        this.maxRetries = maxRetries;
    }

    /**
     * @return Gemini 호출이 성공해서 aiRequest.rawResponse 가 채워졌으면 true, 최종 실패(FAILED)면 false
     */
    public boolean run(AiRequest aiRequest, List<GeminiGenerateRequest.Part> parts) {
        aiRequest.setStatus(STATUS_PROCESSING);
        aiRequest.setPromptText(describeParts(parts));
        aiRequestRepository.save(aiRequest);

        GeminiGenerateRequest.Content content = new GeminiGenerateRequest.Content();
        content.setParts(parts);
        GeminiGenerateRequest geminiRequest =
                GeminiGenerateRequest.of(content, new GeminiGenerateRequest.GenerationConfig());

        int attempt = 0;
        while (true) {
            try {
                GeminiGenerateResponse response = geminiClient.generate(geminiRequest);
                onSuccess(aiRequest, response, attempt);
                return true;
            } catch (GeminiCallException e) {
                if (attempt >= maxRetries) {
                    markFailed(aiRequest, attempt, "GEMINI_CALL_FAILED: " + rootMessage(e));
                    return false;
                }
                attempt++;
            } catch (ResponseStatusException e) {
                // 키 미설정 등 재시도해도 소용없는 경우
                markFailed(aiRequest, attempt, e.getStatusCode() + " " + e.getReason());
                return false;
            } catch (RuntimeException e) {
                // 응답 파싱 실패 등 예상 못한 오류 - 재시도 대상은 아님
                markFailed(aiRequest, attempt, "UNEXPECTED_ERROR: " + rootMessage(e));
                return false;
            }
        }
    }

    /** Gemini 호출 전에 실패한 경우(입력 이미지 읽기 실패 등)도 PENDING 에 멈추지 않고 FAILED 로 끝내기 위해 공개한다. */
    public void markFailed(AiRequest aiRequest, int attempt, String errorCode) {
        aiRequest.setModelName(geminiClient.modelName());
        aiRequest.setRetryCount(attempt);
        aiRequest.setStatus(STATUS_FAILED);
        aiRequest.setErrorCode(truncate(errorCode, 50));
        aiRequest.setCompletedAt(LocalDateTime.now());
        aiRequestRepository.save(aiRequest);
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

    /** 실제로 뭘 보냈는지 사람이 읽을 수 있게 기록한다 (AiRequest.promptText, 정확도 개선 분석용). */
    private String describeParts(List<GeminiGenerateRequest.Part> parts) {
        StringBuilder sb = new StringBuilder();
        for (GeminiGenerateRequest.Part part : parts) {
            if (part.getText() != null) {
                sb.append(part.getText()).append('\n');
            } else if (part.getFileData() != null) {
                sb.append("[영상 첨부: ").append(part.getFileData().getFileUri()).append("]\n");
            } else if (part.getInlineData() != null) {
                sb.append("[이미지 첨부]\n");
            }
        }
        return sb.toString();
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
