package com.example.pinkok_backend.dto;

import com.example.pinkok_backend.entity.AiRequest;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class AiRequestResponse {

    private final Long id;
    private final Long tripId;
    private final String inputType;
    /** PENDING / PROCESSING / SUCCESS / FAILED */
    private final String status;
    private final String errorCode;
    private final Integer retryCount;
    private final LocalDateTime requestedAt;
    private final LocalDateTime completedAt;
    /** status=SUCCESS 일 때만 채워짐 (Gemini가 붙인 콘텐츠 제목). */
    private final String title;
    /** status=SUCCESS 일 때만 채워짐. */
    private final List<ExtractedPlace> places;
    /**
     * 장소 후보를 볼 수 있는 상태인지. 장소 추출이 SUCCESS 일 때만 채워진다.
     * false 면 AI 는 장소를 찾았는데 후보 저장이 실패한 것(카카오맵 장애 등)이라,
     * POST /place-candidates?aiRequestId= 로 다시 만들어야 한다. true 인데 후보가 없으면 진짜 0곳이다.
     */
    private final Boolean candidatesReady;

    private AiRequestResponse(AiRequest request, AiExtractionResult result, Boolean candidatesReady) {
        this.id = request.getId();
        this.tripId = request.getTrip() == null ? null : request.getTrip().getId();
        this.inputType = request.getInputType();
        this.status = request.getStatus();
        this.errorCode = request.getErrorCode();
        this.retryCount = request.getRetryCount();
        this.requestedAt = request.getRequestedAt();
        this.completedAt = request.getCompletedAt();
        this.title = result == null ? null : result.title();
        this.places = result == null ? null : result.places();
        this.candidatesReady = candidatesReady;
    }

    public static AiRequestResponse of(AiRequest request, AiExtractionResult result) {
        return new AiRequestResponse(request, result, null);
    }

    public static AiRequestResponse of(AiRequest request, AiExtractionResult result, boolean candidatesReady) {
        return new AiRequestResponse(request, result, candidatesReady);
    }
}
