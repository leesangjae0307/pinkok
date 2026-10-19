package com.example.pinkok_backend.controller;

import com.example.pinkok_backend.dto.TravelStyleResponse;
import com.example.pinkok_backend.dto.TripTravelStylesRequest;
import com.example.pinkok_backend.security.CurrentUserId;
import com.example.pinkok_backend.service.TripTravelStyleService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/trip-travel-styles")
public class TripTravelStyleController {

    private final TripTravelStyleService tripTravelStyleService;

    public TripTravelStyleController(TripTravelStyleService tripTravelStyleService) {
        this.tripTravelStyleService = tripTravelStyleService;
    }

    /** 이 여행에 저장된 취향 (팀원만). */
    @GetMapping
    public List<TravelStyleResponse> list(@CurrentUserId Long userId, @RequestParam Long tripId) {
        return tripTravelStyleService.list(tripId, userId);
    }

    /** 이 여행의 취향을 통째로 바꾼다 (보낸 것만 남고 나머지는 해제, 빈 배열이면 전부 해제). */
    @PutMapping
    public List<TravelStyleResponse> replace(@CurrentUserId Long userId,
                                             @Valid @RequestBody TripTravelStylesRequest request) {
        return tripTravelStyleService.replace(userId, request.tripId(), request.styleCodes());
    }
}
