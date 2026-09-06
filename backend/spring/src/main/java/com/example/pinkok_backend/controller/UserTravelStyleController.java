package com.example.pinkok_backend.controller;

import com.example.pinkok_backend.service.UserTravelStyleService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user-travel-styles")
public class UserTravelStyleController {

    private final UserTravelStyleService userTravelStyleService;

    public UserTravelStyleController(UserTravelStyleService userTravelStyleService) {
        this.userTravelStyleService = userTravelStyleService;
    }

    // TODO: API 엔드포인트 작성
}
