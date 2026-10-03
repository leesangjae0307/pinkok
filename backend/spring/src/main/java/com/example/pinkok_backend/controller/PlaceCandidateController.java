package com.example.pinkok_backend.controller;

import com.example.pinkok_backend.dto.AddCandidatesToTripRequest;
import com.example.pinkok_backend.dto.AddCandidatesToTripResponse;
import com.example.pinkok_backend.dto.PlaceCandidateResponse;
import com.example.pinkok_backend.security.CurrentUserId;
import com.example.pinkok_backend.service.PlaceCandidateService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * AI가 뽑은 장소 후보(검수용 체크리스트)와, 체크한 것을 여행에 핀으로 추가하는 API.
 * 후보는 AI 추출이 성공하면 서버가 자동으로 만들어 두므로, 보통은 GET 만 부르면 된다.
 */
@RestController
@RequestMapping("/place-candidates")
public class PlaceCandidateController {

    private final PlaceCandidateService placeCandidateService;

    public PlaceCandidateController(PlaceCandidateService placeCandidateService) {
        this.placeCandidateService = placeCandidateService;
    }

    /** 후보 목록. 좌표를 못 찾은 후보는 mapped=false 로 온다. */
    @GetMapping
    public List<PlaceCandidateResponse> list(@CurrentUserId Long userId, @RequestParam Long aiRequestId) {
        return placeCandidateService.list(aiRequestId, userId);
    }

    /**
     * 후보 만들기 재시도. 자동 생성이 실패해서(카카오맵 장애 등) 목록이 비어 있을 때 쓴다.
     * 이미 후보가 있으면 새로 만들지 않고 그대로 돌려준다.
     */
    @PostMapping
    public List<PlaceCandidateResponse> create(@CurrentUserId Long userId, @RequestParam Long aiRequestId) {
        return placeCandidateService.createAndList(aiRequestId, userId);
    }

    /** 체크한 후보들을 여행 지도에 핀으로 추가한다 (날짜 미배정). */
    @PostMapping("/add-to-trip")
    public AddCandidatesToTripResponse addToTrip(@CurrentUserId Long userId,
                                                 @Valid @RequestBody AddCandidatesToTripRequest request) {
        return placeCandidateService.addToTrip(userId, request);
    }
}
