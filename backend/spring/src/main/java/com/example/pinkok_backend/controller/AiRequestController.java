package com.example.pinkok_backend.controller;

import com.example.pinkok_backend.dto.AiRequestCreateRequest;
import com.example.pinkok_backend.dto.AiRequestResponse;
import com.example.pinkok_backend.security.CurrentUserId;
import com.example.pinkok_backend.service.AiRequestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ai-requests")
public class AiRequestController {

    private final AiRequestService aiRequestService;

    public AiRequestController(AiRequestService aiRequestService) {
        this.aiRequestService = aiRequestService;
    }

    /**
     * AI 장소 추출 요청. 링크(LINK)/붙여넣은 글(TEXT)/스크린샷(IMAGE, POST /files 로 먼저 업로드) 중 하나.
     * 바로 202로 응답하고, 실제 Gemini 호출은 뒤에서 비동기로 진행된다 — GET /ai-requests/{id} 로 진행 상태를 폴링한다.
     */
    @PostMapping
    public ResponseEntity<AiRequestResponse> create(@CurrentUserId Long userId,
                                                     @Valid @RequestBody AiRequestCreateRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(aiRequestService.create(userId, request));
    }

    /** 진행 상태 조회. status=SUCCESS 면 places 에 추출된 장소 후보가 담긴다. */
    @GetMapping("/{id}")
    public AiRequestResponse get(@CurrentUserId Long userId, @PathVariable Long id) {
        return aiRequestService.get(id, userId);
    }

    @GetMapping
    public List<AiRequestResponse> list(@CurrentUserId Long userId, @RequestParam(required = false) Long tripId) {
        return aiRequestService.list(userId, tripId);
    }
}
