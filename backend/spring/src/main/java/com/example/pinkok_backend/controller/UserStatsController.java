package com.example.pinkok_backend.controller;

import com.example.pinkok_backend.dto.UserStatsResponse;
import com.example.pinkok_backend.security.CurrentUserId;
import com.example.pinkok_backend.service.UserStatsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserStatsController {

    private final UserStatsService userStatsService;

    public UserStatsController(UserStatsService userStatsService) {
        this.userStatsService = userStatsService;
    }

    /** 프로필 화면의 집계 (방문·저장 장소, 다녀온 지역, 여행 취향). 내 완성 PinLog 는 GET /pinlogs?mine=true. */
    @GetMapping("/users/me/stats")
    public UserStatsResponse stats(@CurrentUserId Long userId) {
        return userStatsService.statsOf(userId);
    }
}
