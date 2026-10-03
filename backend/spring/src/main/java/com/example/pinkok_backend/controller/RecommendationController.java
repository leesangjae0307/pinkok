package com.example.pinkok_backend.controller;

import com.example.pinkok_backend.dto.AiRequestResponse;
import com.example.pinkok_backend.dto.RecommendationAcceptRequest;
import com.example.pinkok_backend.dto.RecommendationCreateRequest;
import com.example.pinkok_backend.dto.RecommendationResponse;
import com.example.pinkok_backend.security.CurrentUserId;
import com.example.pinkok_backend.service.RecommendationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    /**
     * 맞춤 추천 요청. 바로 202 로 응답하고, 실제 추천은 뒤에서 비동기로 만든다.
     * 응답의 id 로 GET /ai-requests/{id} 를 폴링해서 SUCCESS 가 되면 GET /recommendations?tripId= 로 목록을 읽는다.
     */
    @PostMapping
    public ResponseEntity<AiRequestResponse> create(@CurrentUserId Long userId,
                                                    @Valid @RequestBody RecommendationCreateRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(recommendationService.create(userId, request));
    }

    /** 이 여행의 추천 목록 (최신순, 수락·거절한 것 포함). 여행 팀원만 조회 가능. */
    @GetMapping
    public List<RecommendationResponse> list(@CurrentUserId Long userId, @RequestParam Long tripId) {
        return recommendationService.list(tripId, userId);
    }

    /** 추천 수락 - 카카오맵에서 좌표를 찾아 일정에 핀으로 추가한다. 지도에서 못 찾으면 422. */
    @PostMapping("/{id}/accept")
    public RecommendationResponse accept(@CurrentUserId Long userId,
                                         @PathVariable Long id,
                                         @RequestBody(required = false) RecommendationAcceptRequest request) {
        Long dayId = request == null ? null : request.getDayId();
        return recommendationService.accept(id, userId, dayId);
    }

    @PostMapping("/{id}/reject")
    public RecommendationResponse reject(@CurrentUserId Long userId, @PathVariable Long id) {
        return recommendationService.reject(id, userId);
    }
}
