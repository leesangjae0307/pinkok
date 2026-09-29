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

    private AiRequestResponse(AiRequest request, AiExtractionResult result) {
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
    }

    public static AiRequestResponse of(AiRequest request, AiExtractionResult result) {
        return new AiRequestResponse(request, result);
    }
}
