package com.example.pinkok_backend.controller;

import com.example.pinkok_backend.dto.TravelStyleResponse;
import com.example.pinkok_backend.dto.UserTravelStylesRequest;
import com.example.pinkok_backend.security.CurrentUserId;
import com.example.pinkok_backend.service.UserTravelStyleService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/user-travel-styles")
public class UserTravelStyleController {

    private final UserTravelStyleService userTravelStyleService;

    public UserTravelStyleController(UserTravelStyleService userTravelStyleService) {
        this.userTravelStyleService = userTravelStyleService;
    }

    /** 내가 저장해 둔 여행 취향. */
    @GetMapping
    public List<TravelStyleResponse> list(@CurrentUserId Long userId) {
        return userTravelStyleService.list(userId);
    }

    /** 내 여행 취향을 통째로 바꾼다 (보낸 것만 남고 나머지는 해제, 빈 배열이면 전부 해제). */
    @PutMapping
    public List<TravelStyleResponse> replace(@CurrentUserId Long userId,
                                             @Valid @RequestBody UserTravelStylesRequest request) {
        return userTravelStyleService.replace(userId, request.styleCodes());
    }
}
